package com.smartcart.servlet;

import com.smartcart.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    // CORS CONFIGURATION
    private void setCorsHeaders(
            HttpServletRequest request,
            HttpServletResponse response) {

        String origin = request.getHeader("Origin");

        if ("http://127.0.0.1:5500".equals(origin)
                || "http://localhost:5500".equals(origin)) {

            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Vary", "Origin");
        }
    }

    // HANDLE OPTIONS REQUEST
    @Override
    protected void doOptions(
            HttpServletRequest request,
            HttpServletResponse response) {

        setCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // VERIFY PASSWORD
    private boolean verifyPassword(
            String password,
            String storedPassword) throws Exception {

        if (storedPassword == null) {
            return false;
        }

        String[] parts = storedPassword.split("\\$");

        if (parts.length != 4 || !parts[0].equals("pbkdf2")) {
            return false;
        }

        int iterations = Integer.parseInt(parts[1]);

        if (iterations < 100000 || iterations > 1000000) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] originalHash = Base64.getDecoder().decode(parts[3]);

        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                originalHash.length * 8
        );

        try {
            SecretKeyFactory factory =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] newHash = factory.generateSecret(spec).getEncoded();

            if (newHash.length != originalHash.length) {
                return false;
            }

            int difference = 0;

            for (int i = 0; i < newHash.length; i++) {
                difference |= newHash[i] ^ originalHash[i];
            }

            return difference == 0;

        } finally {
            spec.clearPassword();
        }
    }

    // SEND JSON RESPONSE
    private void sendResponse(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        PrintWriter out = response.getWriter();

        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        out.print("{\"message\":\"" + safeMessage + "\"}");
        out.flush();
    }

    // LOGIN
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        setCorsHeaders(request, response);

        request.setCharacterEncoding("UTF-8");

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        // VALIDATE INPUT
        if (email == null || password == null
                || email.trim().isEmpty()
                || password.isEmpty()) {

            sendResponse(response, 400,
                    "Email and password are required.");
            return;
        }

        email = email.trim().toLowerCase();

        try (Connection con = DBConnection.getConnection()) {

            // GET CUSTOMER DETAILS FROM DATABASE
            String sql = "SELECT user_id, name, email, password, phone, role "
                    + "FROM users WHERE email = ?";

            try (PreparedStatement stmt = con.prepareStatement(sql)) {

                stmt.setString(1, email);

                try (ResultSet rs = stmt.executeQuery()) {

                    if (!rs.next()) {

                        sendResponse(response, 401,
                                "Invalid email or password.");
                        return;
                    }

                    String storedPassword = rs.getString("password");

                    // VERIFY PASSWORD
                    if (!verifyPassword(password, storedPassword)) {

                        sendResponse(response, 401,
                                "Invalid email or password.");
                        return;
                    }

                    // GET USER DETAILS
                    int userId = rs.getInt("user_id");

                    String userName = rs.getString("name");
                    String userEmail = rs.getString("email");
                    String userPhone = rs.getString("phone");
                    String userRole = rs.getString("role");

                    // INVALIDATE OLD SESSION
                    HttpSession oldSession = request.getSession(false);

                    if (oldSession != null) {
                        oldSession.invalidate();
                    }

                    // CREATE NEW SESSION
                    HttpSession session = request.getSession(true);

                    session.setAttribute("userId", userId);
                    session.setAttribute("userName", userName);
                    session.setAttribute("userEmail", userEmail);
                    session.setAttribute("userPhone", userPhone);
                    session.setAttribute("userRole", userRole);

                    session.setMaxInactiveInterval(30 * 60);

                    // SUCCESS RESPONSE
                    sendResponse(response, 200,
                            "Login successful!");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(response, 500,
                    "Server error. Please try again.");
        }
    }
}