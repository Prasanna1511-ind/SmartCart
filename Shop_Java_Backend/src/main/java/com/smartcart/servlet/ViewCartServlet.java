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

@WebServlet("/view-cart")
public class ViewCartServlet extends HttpServlet {

    private void setCORS(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");

        if ("http://127.0.0.1:5500".equals(origin)
                || "http://localhost:5500".equals(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }

        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        setCORS(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    private String escapeJSON(String value) {
        if (value == null) return "";

        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        setCORS(request, response);
        response.setHeader("Cache-Control", "no-store");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(401);
            response.getWriter().write("{\"message\":\"Please login first\"}");
            return;
        }

        int userId = Integer.parseInt(session.getAttribute("userId").toString());

        String sql =
                "SELECT c.cart_id, c.product_id, c.quantity, c.size, " +
                "p.product_name, p.price, p.stock, p.image " +
                "FROM cart c " +
                "JOIN products p ON c.product_id = p.product_id " +
                "WHERE c.user_id = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    first = false;

                    json.append("{")
                        .append("\"cart_id\":").append(rs.getInt("cart_id")).append(",")
                        .append("\"product_id\":").append(rs.getInt("product_id")).append(",")
                        .append("\"quantity\":").append(rs.getInt("quantity")).append(",")
                        .append("\"size\":\"").append(escapeJSON(rs.getString("size"))).append("\",")
                        .append("\"product_name\":\"").append(escapeJSON(rs.getString("product_name"))).append("\",")
                        .append("\"price\":").append(rs.getBigDecimal("price")).append(",")
                        .append("\"stock\":").append(rs.getInt("stock")).append(",")
                        .append("\"image\":\"").append(escapeJSON(rs.getString("image"))).append("\"")
                        .append("}");
                }

                json.append("]");

                response.getWriter().write(json.toString());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().write("{\"message\":\"Database error\"}");
        }
    }
}