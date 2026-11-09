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

@WebServlet("/add-to-cart")
public class AddToCartServlet extends HttpServlet {

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
            int productId = Integer.parseInt(request.getParameter("productId"));
            int quantity = Integer.parseInt(request.getParameter("quantity"));

            String size = request.getParameter("size");

            if (size != null && size.trim().isEmpty()) {
                size = null;
            }

            if (productId <= 0 || quantity <= 0) {
                sendJSON(response, 400, "Invalid product or quantity");
                return;
            }

            try (Connection con = DBConnection.getConnection()) {

                // Check product stock
                String stockSQL = "SELECT stock FROM products WHERE product_id = ?";

                int stock;

                try (PreparedStatement ps = con.prepareStatement(stockSQL)) {
                    ps.setInt(1, productId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            sendJSON(response, 404, "Product not found");
                            return;
                        }

                        stock = rs.getInt("stock");
                    }
                }

                if (quantity > stock) {
                    sendJSON(response, 400, "Insufficient stock");
                    return;
                }

                // Check existing cart item
                String checkSQL =
                        "SELECT cart_id, quantity FROM cart " +
                        "WHERE user_id = ? AND product_id = ? AND size <=> ?";

                try (PreparedStatement ps = con.prepareStatement(checkSQL)) {

                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    ps.setString(3, size);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {

                            int cartId = rs.getInt("cart_id");
                            int oldQuantity = rs.getInt("quantity");
                            int newQuantity = oldQuantity + quantity;

                            if (newQuantity > stock) {
                                sendJSON(response, 400, "Maximum stock reached");
                                return;
                            }

                            String updateSQL =
                                    "UPDATE cart SET quantity = ? WHERE cart_id = ? AND user_id = ?";

                            try (PreparedStatement update = con.prepareStatement(updateSQL)) {
                                update.setInt(1, newQuantity);
                                update.setInt(2, cartId);
                                update.setInt(3, userId);
                                update.executeUpdate();
                            }

                        } else {

                            String insertSQL =
                                    "INSERT INTO cart (user_id, product_id, quantity, size) " +
                                    "VALUES (?, ?, ?, ?)";

                            try (PreparedStatement insert = con.prepareStatement(insertSQL)) {
                                insert.setInt(1, userId);
                                insert.setInt(2, productId);
                                insert.setInt(3, quantity);
                                insert.setString(4, size);
                                insert.executeUpdate();
                            }
                        }
                    }
                }

                sendJSON(response, 200, "Product added to cart successfully");

            }

        } catch (NumberFormatException e) {
            sendJSON(response, 400, "Invalid input");
        } catch (SQLException e) {
            e.printStackTrace();
            sendJSON(response, 500, "Database error");
        }
    }
}