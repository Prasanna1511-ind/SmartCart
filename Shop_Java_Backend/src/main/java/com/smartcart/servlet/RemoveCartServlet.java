package com.smartcart.servlet;

import com.smartcart.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.*;

@WebServlet("/remove-cart")
public class RemoveCartServlet extends HttpServlet {

    private void setCORS(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        if ("http://127.0.0.1:5500".equals(origin)
                || "http://localhost:5500".equals(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }

        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJSON(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        setCORS(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setCORS(request, response);

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJSON(response, 401, "Please login first");
            return;
        }

        int userId = Integer.parseInt(session.getAttribute("userId").toString());

        try {
            int cartId = Integer.parseInt(request.getParameter("cartId"));

            if (cartId <= 0) {
                sendJSON(response, 400, "Invalid cart item");
                return;
            }

            String sql = "DELETE FROM cart WHERE cart_id = ? AND user_id = ?";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, cartId);
                ps.setInt(2, userId);

                int deleted = ps.executeUpdate();

                if (deleted == 0) {
                    sendJSON(response, 404, "Cart item not found");
                    return;
                }

                sendJSON(response, 200, "Product removed from cart successfully");
            }

        } catch (NumberFormatException e) {
            sendJSON(response, 400, "Invalid cart item");
        } catch (SQLException e) {
            e.printStackTrace();
            sendJSON(response, 500, "Database error");
        }
    }
}