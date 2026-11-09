package com.smartcart.servlet;

import com.smartcart.config.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.*;
import java.math.BigDecimal;

@WebServlet("/orders")
public class OrderServlet extends HttpServlet {

    private void sendJSON(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        response.getWriter().print(
                "{\"message\":\"" + safeMessage + "\"}"
        );
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        request.setCharacterEncoding("UTF-8");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // 1. CHECK LOGIN SESSION

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            sendJSON(response, 401, "Please login before placing an order.");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");

        // 2. GET CUSTOMER DETAILS

        String customerName = request.getParameter("customerName");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        String city = request.getParameter("city");
        String pincode = request.getParameter("pincode");
        String paymentMethod = request.getParameter("paymentMethod");

        String productIds = request.getParameter("productIds");
        String quantities = request.getParameter("quantities");

        if (customerName == null || phone == null ||
            address == null || city == null || pincode == null ||
            paymentMethod == null || productIds == null ||
            quantities == null) {

            sendJSON(response, 400, "Please provide all required details.");
            return;
        }

        if (customerName.isBlank() || phone.isBlank() ||
            address.isBlank() || city.isBlank() || pincode.isBlank()) {

            sendJSON(response, 400, "Delivery details cannot be empty.");
            return;
        }

        if (!"COD".equals(paymentMethod)) {
            sendJSON(response, 400, "Invalid payment method.");
            return;
        }

        String[] ids = productIds.split(",");
        String[] qty = quantities.split(",");

        if (ids.length == 0 || ids.length != qty.length) {
            sendJSON(response, 400, "Invalid cart information.");
            return;
        }

        Connection con = null;

        try {

            con = DBConnection.getConnection();

            if (con == null) {
                throw new SQLException("Database connection failed.");
            }

            con.setAutoCommit(false);

            BigDecimal total = BigDecimal.ZERO;

            // 3. CREATE ORDER

            String orderSQL =
                "INSERT INTO orders " +
                "(user_id, total_amount, order_status, customer_name, " +
                "phone, delivery_address, city, pincode) " +
                "VALUES (?, ?, 'PENDING', ?, ?, ?, ?, ?)";

            int orderId;

            try (PreparedStatement orderStmt =
                    con.prepareStatement(
                        orderSQL,
                        Statement.RETURN_GENERATED_KEYS
                    )) {

                orderStmt.setInt(1, userId);
                orderStmt.setBigDecimal(2, BigDecimal.ZERO);
                orderStmt.setString(3, customerName.trim());
                orderStmt.setString(4, phone.trim());
                orderStmt.setString(5, address.trim());
                orderStmt.setString(6, city.trim());
                orderStmt.setString(7, pincode.trim());

                orderStmt.executeUpdate();

                try (ResultSet keys = orderStmt.getGeneratedKeys()) {

                    if (!keys.next()) {
                        throw new SQLException("Order ID not generated.");
                    }

                    orderId = keys.getInt(1);
                }
            }

            // 4. GET PRODUCT PRICE AND CHECK STOCK

            String productSQL =
                "SELECT price, stock FROM products " +
                "WHERE product_id = ? FOR UPDATE";

            String itemSQL =
                "INSERT INTO order_items " +
                "(order_id, product_id, quantity, price, size) " +
                "VALUES (?, ?, ?, ?, ?)";

            String stockSQL =
                "UPDATE products SET stock = stock - ? " +
                "WHERE product_id = ? AND stock >= ?";

            try (
                PreparedStatement productStmt = con.prepareStatement(productSQL);
                PreparedStatement itemStmt = con.prepareStatement(itemSQL);
                PreparedStatement stockStmt = con.prepareStatement(stockSQL)
            ) {

                for (int i = 0; i < ids.length; i++) {

                    int productId = Integer.parseInt(ids[i].trim());
                    int quantity = Integer.parseInt(qty[i].trim());

                    if (quantity <= 0) {
                        throw new SQLException("Invalid product quantity.");
                    }

                    productStmt.setInt(1, productId);

                    BigDecimal price;
                    int availableStock;

                    try (ResultSet rs = productStmt.executeQuery()) {

                        if (!rs.next()) {
                            throw new SQLException("Product not found: " + productId);
                        }

                        price = rs.getBigDecimal("price");
                        availableStock = rs.getInt("stock");
                    }

                    if (availableStock < quantity) {
                        throw new SQLException(
                            "Insufficient stock for product ID: " + productId
                        );
                    }

                    // SAVE ORDER ITEM

                    itemStmt.setInt(1, orderId);
                    itemStmt.setInt(2, productId);
                    itemStmt.setInt(3, quantity);
                    itemStmt.setBigDecimal(4, price);
                    itemStmt.setString(5, "");

                    itemStmt.executeUpdate();

                    // REDUCE PRODUCT STOCK

                    stockStmt.setInt(1, quantity);
                    stockStmt.setInt(2, productId);
                    stockStmt.setInt(3, quantity);

                    if (stockStmt.executeUpdate() != 1) {
                        throw new SQLException("Unable to update product stock.");
                    }

                    total = total.add(
                        price.multiply(BigDecimal.valueOf(quantity))
                    );
                }
            }

            // 5. UPDATE ORDER TOTAL

            String updateOrderSQL =
                "UPDATE orders SET total_amount = ? WHERE order_id = ?";

            try (PreparedStatement stmt =
                    con.prepareStatement(updateOrderSQL)) {

                stmt.setBigDecimal(1, total);
                stmt.setInt(2, orderId);

                stmt.executeUpdate();
            }

            // 6. SAVE PAYMENT INFORMATION

            String paymentSQL =
                "INSERT INTO payments " +
                "(order_id, payment_method, payment_status) " +
                "VALUES (?, ?, 'PENDING')";

            try (PreparedStatement stmt =
                    con.prepareStatement(paymentSQL)) {

                stmt.setInt(1, orderId);
                stmt.setString(2, paymentMethod);

                stmt.executeUpdate();
            }

            // 7. COMMIT TRANSACTION

            con.commit();

            sendJSON(
                response,
                200,
                "Order placed successfully! Order ID: " + orderId
            );

        } catch (Exception e) {

            e.printStackTrace();

            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    rollbackError.printStackTrace();
                }
            }

            sendJSON(
                response,
                400,
                "Order failed: " + e.getMessage()
            );

        } finally {

            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}