package com.smartcart.servlet;

import com.smartcart.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final int ITERATIONS = 600000;
    private static final int KEY_LENGTH = 256;

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
            response.setHeader("Vary", "Origin");
        }
    }

    // HANDLE PREFLIGHT REQUEST
    @Override
    protected void doOptions(
            HttpServletRequest request,
            HttpServletResponse response) {

        setCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    // PASSWORD HASHING
    private String hashPassword(String password) throws Exception {

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);

        KeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                ITERATIONS,
                KEY_LENGTH
        );

        SecretKeyFactory factory =
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] hash = factory.generateSecret(spec).getEncoded();

        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
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

    // REGISTRATION
    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        setCorsHeaders(request, response);

        request.setCharacterEncoding("UTF-8");

        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String phone = request.getParameter("phone");
        String password = request.getParameter("password");

        // CHECK REQUIRED FIELDS
        if (name == null || name.trim().isEmpty()) {
    sendResponse(response, 400, "Name is missing.");
    return;
}

if (email == null || email.trim().isEmpty()) {
    sendResponse(response, 400, "Email is missing.");
    return;
}

if (phone == null || phone.trim().isEmpty()) {
    sendResponse(response, 400, "Phone number is missing.");
    return;
}

if (password == null || password.isEmpty()) {
    sendResponse(response, 400, "Password is missing.");
    return;
}

        name = name.trim();
        email = email.trim().toLowerCase();
        phone = phone.trim();

        // VALIDATE LENGTH
        if (name.length() > 100
                || email.length() > 100
                || phone.length() > 15) {

            sendResponse(response, 400,
                    "Name, email or phone number is too long.");
            return;
        }

        // VALIDATE EMAIL
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            sendResponse(response, 400,
                    "Enter a valid email address.");
            return;
        }

        // VALIDATE PHONE
        if (!phone.matches("[0-9]{10,15}")) {

            sendResponse(response, 400,
                    "Enter a valid phone number.");
            return;
        }

        // VALIDATE PASSWORD
        if (password.length() < 8 || password.length() > 128) {

            sendResponse(response, 400,
                    "Password must contain 8 to 128 characters.");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            // CHECK EXISTING EMAIL
            String checkSql =
                    "SELECT user_id FROM users WHERE email = ?";

            try (PreparedStatement checkStmt =
                         con.prepareStatement(checkSql)) {

                checkStmt.setString(1, email);

                try (ResultSet rs = checkStmt.executeQuery()) {

                    if (rs.next()) {

                        sendResponse(response, 409,
                                "Email already registered.");
                        return;
                    }
                }
            }

            // HASH PASSWORD
            String hashedPassword = hashPassword(password);

            // INSERT CUSTOMER
            String insertSql =
                    "INSERT INTO users "
                    + "(name, email, password, phone, role) "
                    + "VALUES (?, ?, ?, ?, 'CUSTOMER')";

            try (PreparedStatement insertStmt =
                         con.prepareStatement(insertSql)) {

                insertStmt.setString(1, name);
                insertStmt.setString(2, email);
                insertStmt.setString(3, hashedPassword);
                insertStmt.setString(4, phone);

                int rows = insertStmt.executeUpdate();

                if (rows > 0) {

                    sendResponse(response, 201,
                            "Registration successful!");

                } else {

                    sendResponse(response, 500,
                            "Registration failed.");
                }
            }

        } catch (Exception e) {

            e.printStackTrace();

            sendResponse(response, 500,
                    "Server error. Please try again.");
        }
    }
}