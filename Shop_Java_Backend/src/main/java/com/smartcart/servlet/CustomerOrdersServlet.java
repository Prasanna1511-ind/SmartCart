package com.smartcart.servlet;

import com.smartcart.config.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;

@WebServlet("/customer-orders")
public class CustomerOrdersServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(401);
            response.getWriter().print("{\"message\":\"Please login\"}");
            return;
        }

        int userId = ((Number) session.getAttribute("userId")).intValue();

        String sql =
            "SELECT o.order_id, o.total_amount, o.order_status, " +
            "o.order_date, p.payment_method, p.payment_status " +
            "FROM orders o " +
            "LEFT JOIN payments p ON o.order_id = p.order_id " +
            "WHERE o.user_id = ? " +
            "ORDER BY o.order_id DESC";

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement stmt = con.prepareStatement(sql)) {

            if (con == null) {
                throw new SQLException("Database connection failed");
            }

            stmt.setInt(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    first = false;

                    Timestamp date = rs.getTimestamp("order_date");

                    String formattedDate = date == null
                        ? ""
                        : new SimpleDateFormat("dd-MM-yyyy").format(date);

                    json.append("{")
                        .append("\"orderId\":")
                        .append(rs.getInt("order_id"))
                        .append(",\"date\":\"")
                        .append(formattedDate)
                        .append("\",\"total\":")
                        .append(rs.getBigDecimal("total_amount"))
                        .append(",\"paymentMethod\":\"")
                        .append(escape(rs.getString("payment_method")))
                        .append("\",\"paymentStatus\":\"")
                        .append(escape(rs.getString("payment_status")))
                        .append("\",\"status\":\"")
                        .append(escape(rs.getString("order_status")))
                        .append("\"}");
                }
            }

            json.append("]");

            response.getWriter().print(json.toString());

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(500);
            response.getWriter().print(
                "{\"message\":\"Unable to load customer orders\"}"
            );
        }
    }

    private String escape(String value) {

        if (value == null) {
            return "";
        }

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}