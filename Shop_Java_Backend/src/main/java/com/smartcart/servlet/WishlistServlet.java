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

@WebServlet("/wishlist")
public class WishlistServlet extends HttpServlet {

    private void setCors(HttpServletResponse response) {
        response.setHeader(
            "Access-Control-Allow-Origin",
            "http://localhost:5500"
        );
        response.setHeader(
            "Access-Control-Allow-Credentials",
            "true"
        );
        response.setHeader(
            "Access-Control-Allow-Methods",
            "GET, POST, OPTIONS"
        );
        response.setHeader(
            "Access-Control-Allow-Headers",
            "Content-Type"
        );
    }

    @Override
    protected void doOptions(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        setCors(response);
        response.setStatus(200);
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        setCors(response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userId") == null) {
            response.setStatus(401);
            response.getWriter().print(
                "{\"message\":\"Please login\"}"
            );
            return;
        }

        int userId = ((Number) session.getAttribute("userId")).intValue();

        String sql =
            "SELECT p.product_id, p.product_name, p.price, " +
            "p.stock, p.image " +
            "FROM wishlist w " +
            "JOIN products p ON w.product_id = p.product_id " +
            "WHERE w.user_id = ? " +
            "ORDER BY w.wishlist_id DESC";

        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    first = false;

                    json.append("{")
                        .append("\"product_id\":")
                        .append(rs.getInt("product_id"))
                        .append(",\"product_name\":\"")
                        .append(escape(rs.getString("product_name")))
                        .append("\",\"price\":")
                        .append(rs.getBigDecimal("price"))
                        .append(",\"stock\":")
                        .append(rs.getInt("stock"))
                        .append(",\"image\":\"")
                        .append(escape(rs.getString("image")))
                        .append("\"}");
                }
            }

            json.append("]");
            response.getWriter().print(json.toString());

        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().print(
                "{\"message\":\"Unable to load wishlist\"}"
            );
        }
    }

@Override
protected void doPost(HttpServletRequest request,
                      HttpServletResponse response)
        throws ServletException, IOException {

    setCors(response);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    System.out.println("WISHLIST POST METHOD CALLED!");

    HttpSession session = request.getSession(false);

    if (session == null || session.getAttribute("userId") == null) {
        response.setStatus(401);
        response.getWriter().write(
            "{\"message\":\"Please login first!\"}"
        );
        return;
    }

    int userId = ((Number) session.getAttribute("userId")).intValue();

    String action = request.getParameter("action");
    String productIdValue = request.getParameter("productId");

    System.out.println("Action: " + action);
    System.out.println("User ID: " + userId);
    System.out.println("Product ID: " + productIdValue);

    try {
        int productId = Integer.parseInt(productIdValue);

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                throw new SQLException("Database connection failed!");
            }

            if ("add".equals(action)) {

                String sql =
                    "INSERT INTO wishlist (user_id, product_id) " +
                    "VALUES (?, ?)";

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, userId);
                    ps.setInt(2, productId);

                    int rows = ps.executeUpdate();

                    System.out.println("Inserted rows: " + rows);
                }

                response.getWriter().write(
                    "{\"message\":\"Product added to Wishlist successfully!\"}"
                );

                System.out.println("WISHLIST INSERT SUCCESS!");

            } else if ("remove".equals(action)) {

                String sql =
                    "DELETE FROM wishlist WHERE user_id=? AND product_id=?";

                try (PreparedStatement ps = con.prepareStatement(sql)) {

                    ps.setInt(1, userId);
                    ps.setInt(2, productId);

                    int rows = ps.executeUpdate();

                    System.out.println("Deleted rows: " + rows);
                }

                response.getWriter().write(
                    "{\"message\":\"Product removed from Wishlist!\"}"
                );

            } else {

                response.setStatus(400);
                response.getWriter().write(
                    "{\"message\":\"Invalid action!\"}"
                );
            }
        }

    } catch (NumberFormatException e) {

        response.setStatus(400);
        response.getWriter().write(
            "{\"message\":\"Invalid product ID!\"}"
        );

    } catch (SQLException e) {

        e.printStackTrace();

        response.setStatus(500);
        response.getWriter().write(
            "{\"message\":\"Database error!\"}"
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
