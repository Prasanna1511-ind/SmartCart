package com.smartcart.servlet;

import com.smartcart.config.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/admin-orders")
public class AdminOrderServlet extends HttpServlet {

    // CHECK ADMIN SESSION
    private boolean isAdmin(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        return session != null &&
               "ADMIN".equals(session.getAttribute("userRole"));
    }

    // JSON RESPONSE
    private void sendJSON(
            HttpServletResponse response,
            int status,
            String json) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(json);
    }

    // ESCAPE JSON VALUES
    private String escapeJSON(String value) {

        if (value == null) return "";

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    // GET ALL ORDERS OR INDIVIDUAL ORDER DETAILS
    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (!isAdmin(request)) {
            sendJSON(response, 403,
                    "{\"message\":\"Admin access required.\"}");
            return;
        }

        String id = request.getParameter("id");

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed.");
            }

            // INDIVIDUAL ORDER DETAILS
            if (id != null && !id.isBlank()) {

                int orderId = Integer.parseInt(id);

                String sql =
                    "SELECT oi.item_id, oi.product_id, " +
                    "p.product_name, oi.quantity, oi.price, oi.size " +
                    "FROM order_items oi " +
                    "LEFT JOIN products p ON oi.product_id = p.product_id " +
                    "WHERE oi.order_id = ?";

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, orderId);

                    try (ResultSet rs = ps.executeQuery()) {

                        while (rs.next()) {

                            if (!first) json.append(",");
                            first = false;

                            json.append("{")
                                .append("\"itemId\":").append(rs.getInt("item_id"))
                                .append(",\"productId\":").append(rs.getInt("product_id"))
                                .append(",\"productName\":\"")
                                .append(escapeJSON(rs.getString("product_name")))
                                .append("\"")
                                .append(",\"quantity\":").append(rs.getInt("quantity"))
                                .append(",\"price\":").append(rs.getBigDecimal("price"))
                                .append(",\"size\":\"")
                                .append(escapeJSON(rs.getString("size")))
                                .append("\"}");
                        }
                    }
                }

                json.append("]");

                sendJSON(response, 200, json.toString());
                return;
            }

            // ALL CUSTOMER ORDERS
            String sql =
                "SELECT o.order_id, o.user_id, o.total_amount, " +
                "o.order_status, o.order_date, o.customer_name, " +
                "o.phone, o.delivery_address, o.city, o.pincode, " +
                "COUNT(oi.item_id) AS total_items " +
                "FROM orders o " +
                "LEFT JOIN order_items oi ON o.order_id = oi.order_id " +
                "GROUP BY o.order_id, o.user_id, o.total_amount, " +
                "o.order_status, o.order_date, o.customer_name, " +
                "o.phone, o.delivery_address, o.city, o.pincode " +
                "ORDER BY o.order_date DESC";

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    if (!first) json.append(",");
                    first = false;

                    json.append("{")
                        .append("\"orderId\":").append(rs.getInt("order_id"))
                        .append(",\"userId\":").append(rs.getInt("user_id"))
                        .append(",\"totalAmount\":").append(rs.getBigDecimal("total_amount"))
                        .append(",\"orderStatus\":\"")
                        .append(escapeJSON(rs.getString("order_status")))
                        .append("\"")
                        .append(",\"orderDate\":\"")
                        .append(rs.getTimestamp("order_date"))
                        .append("\"")
                        .append(",\"customerName\":\"")
                        .append(escapeJSON(rs.getString("customer_name")))
                        .append("\"")
                        .append(",\"phone\":\"")
                        .append(escapeJSON(rs.getString("phone")))
                        .append("\"")
                        .append(",\"address\":\"")
                        .append(escapeJSON(rs.getString("delivery_address")))
                        .append("\"")
                        .append(",\"city\":\"")
                        .append(escapeJSON(rs.getString("city")))
                        .append("\"")
                        .append(",\"pincode\":\"")
                        .append(escapeJSON(rs.getString("pincode")))
                        .append("\"")
                        .append(",\"totalItems\":").append(rs.getInt("total_items"))
                        .append("}");
                }
            }

            json.append("]");

            sendJSON(response, 200, json.toString());

        } catch (Exception e) {

            e.printStackTrace();

            sendJSON(response, 500,
                    "{\"message\":\"Unable to fetch orders.\"}");
        }
    }

    // UPDATE ORDER STATUS
    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        if (!isAdmin(request)) {
            sendJSON(response, 403,
                    "{\"message\":\"Admin access required.\"}");
            return;
        }

        request.setCharacterEncoding("UTF-8");

        Map<String, String> params = new HashMap<>();

        String body = request.getReader()
                             .lines()
                             .reduce("", (a, b) -> a + b);

        for (String pair : body.split("&")) {

            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {

                params.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                );
            }
        }

        try {

            int orderId = Integer.parseInt(params.get("id"));

            String status = params.get("status");

            if (status == null) {
                sendJSON(response, 400,
                        "{\"message\":\"Order status is required.\"}");
                return;
            }

            status = status.toUpperCase();

            if (!status.matches(
                    "PENDING|PLACED|CONFIRMED|SHIPPED|DELIVERED|CANCELLED")) {

                sendJSON(response, 400,
                        "{\"message\":\"Invalid order status.\"}");
                return;
            }

            String sql =
                "UPDATE orders SET order_status = ? WHERE order_id = ?";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, status);
                ps.setInt(2, orderId);

                int rows = ps.executeUpdate();

                if (rows == 0) {
                    sendJSON(response, 404,
                            "{\"message\":\"Order not found.\"}");
                    return;
                }
            }

            sendJSON(response, 200,
                    "{\"success\":true,\"message\":\"Order status updated successfully.\"}");

        } catch (NumberFormatException e) {

            sendJSON(response, 400,
                    "{\"message\":\"Invalid order ID.\"}");

        } catch (Exception e) {

            e.printStackTrace();

            sendJSON(response, 500,
                    "{\"message\":\"Unable to update order status.\"}");
        }
    }
}