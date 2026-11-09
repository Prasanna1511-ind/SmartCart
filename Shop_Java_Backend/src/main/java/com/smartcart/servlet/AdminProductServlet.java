package com.smartcart.servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.smartcart.config.DBConnection;

@WebServlet("/admin-products")
public class AdminProductServlet extends HttpServlet {

    // CHECK ADMIN ACCESS
    private boolean isAdmin(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        if (session == null ||
                !"ADMIN".equals(session.getAttribute("userRole"))) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(
                "{\"error\":\"Admin access denied\"}"
            );

            return false;
        }

        return true;
    }

    // JSON ESCAPE
    private String escapeJson(String value) {

        if (value == null) return "";

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    // GET ALL PRODUCTS FOR ADMIN
    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
                         throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!isAdmin(request, response)) return;

        String sql = "SELECT product_id, product_name, description, " +
                     "price, stock, image, category_id, shop_id, is_active " +
                     "FROM products ORDER BY product_id ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            while (rs.next()) {

                if (!first) json.append(",");

                json.append("{");

                json.append("\"product_id\":").append(rs.getInt("product_id")).append(",");
                json.append("\"product_name\":\"").append(escapeJson(rs.getString("product_name"))).append("\",");
                json.append("\"description\":\"").append(escapeJson(rs.getString("description"))).append("\",");
                json.append("\"price\":").append(rs.getBigDecimal("price")).append(",");
                json.append("\"stock\":").append(rs.getInt("stock")).append(",");
                json.append("\"image\":\"").append(escapeJson(rs.getString("image"))).append("\",");
                json.append("\"category_id\":").append(rs.getInt("category_id")).append(",");
                json.append("\"shop_id\":").append(rs.getInt("shop_id")).append(",");
                json.append("\"is_active\":").append(rs.getInt("is_active"));

                json.append("}");

                first = false;
            }

            json.append("]");
            response.getWriter().write(json.toString());

        } catch (Exception e) {

            e.printStackTrace();
            response.setStatus(500);

            response.getWriter().write(
                "{\"error\":\"Unable to retrieve admin products\"}"
            );
        }
    }

    // ADD PRODUCT OR RESTORE PRODUCT
    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
                          throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!isAdmin(request, response)) return;

        // RESTORE PRODUCT
        if ("restore".equals(request.getParameter("action"))) {

            restoreProduct(request, response);
            return;
        }

        // ADD PRODUCT
        String name = request.getParameter("productName");
        String description = request.getParameter("description");
        String priceText = request.getParameter("price");
        String stockText = request.getParameter("stock");
        String image = request.getParameter("image");
        String categoryText = request.getParameter("categoryId");
        String shopText = request.getParameter("shopId");

        try {

            if (name == null || name.trim().isEmpty()) {
                response.setStatus(400);
                response.getWriter().write(
                    "{\"error\":\"Product name is required\"}"
                );
                return;
            }

            double price = Double.parseDouble(priceText);
            int stock = Integer.parseInt(stockText);
            int categoryId = Integer.parseInt(categoryText);
            int shopId = Integer.parseInt(shopText);

            if (price <= 0 || stock < 0) {
                response.setStatus(400);
                response.getWriter().write(
                    "{\"error\":\"Invalid price or stock\"}"
                );
                return;
            }

            String sql = "INSERT INTO products " +
                    "(product_name, description, price, stock, image, category_id, shop_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, name.trim());
                ps.setString(2, description);
                ps.setDouble(3, price);
                ps.setInt(4, stock);
                ps.setString(5, image);
                ps.setInt(6, categoryId);
                ps.setInt(7, shopId);

                ps.executeUpdate();

                response.getWriter().write(
                    "{\"message\":\"Product added successfully!\"}"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
            response.setStatus(400);

            response.getWriter().write(
                "{\"error\":\"Unable to add product. Check entered details.\"}"
            );
        }
    }

    // RESTORE PRODUCT
    private void restoreProduct(HttpServletRequest request,
                                HttpServletResponse response)
                                throws IOException {

        try {

            int id = Integer.parseInt(request.getParameter("id"));

            String sql = "UPDATE products SET is_active = 1 " +
                         "WHERE product_id = ? AND is_active = 0";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {

                    response.getWriter().write(
                        "{\"message\":\"Product restored successfully!\"}"
                    );

                } else {

                    response.setStatus(404);

                    response.getWriter().write(
                        "{\"error\":\"Product not found or already active\"}"
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
            response.setStatus(500);

            response.getWriter().write(
                "{\"error\":\"Unable to restore product\"}"
            );
        }
    }

    // UPDATE PRODUCT
    @Override
    protected void doPut(HttpServletRequest request,
                         HttpServletResponse response)
                         throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!isAdmin(request, response)) return;

        try {

            String body = request.getReader().lines()
                    .reduce("", (a, b) -> a + b);

            Map<String, String> data = new HashMap<>();

            for (String pair : body.split("&")) {

                String[] parts = pair.split("=", 2);

                if (parts.length == 2) {

                    data.put(
                        URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    );
                }
            }

            int id = Integer.parseInt(data.get("productId"));
            String name = data.get("productName");
            String description = data.get("description");
            double price = Double.parseDouble(data.get("price"));
            int stock = Integer.parseInt(data.get("stock"));
            String image = data.get("image");
            int categoryId = Integer.parseInt(data.get("categoryId"));
            int shopId = Integer.parseInt(data.get("shopId"));

            if (id <= 0 || name == null || name.trim().isEmpty()
                    || price <= 0 || stock < 0) {

                response.setStatus(400);
                response.getWriter().write(
                    "{\"error\":\"Invalid product details\"}"
                );
                return;
            }

            String sql = "UPDATE products SET " +
                         "product_name=?, description=?, price=?, stock=?, " +
                         "image=?, category_id=?, shop_id=? " +
                         "WHERE product_id=? AND is_active = 1";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, name.trim());
                ps.setString(2, description);
                ps.setDouble(3, price);
                ps.setInt(4, stock);
                ps.setString(5, image);
                ps.setInt(6, categoryId);
                ps.setInt(7, shopId);
                ps.setInt(8, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {

                    response.getWriter().write(
                        "{\"message\":\"Product updated successfully!\"}"
                    );

                } else {

                    response.setStatus(404);
                    response.getWriter().write(
                        "{\"error\":\"Active product not found\"}"
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
            response.setStatus(400);

            response.getWriter().write(
                "{\"error\":\"Unable to update product\"}"
            );
        }
    }

    // SOFT DELETE PRODUCT
    @Override
    protected void doDelete(HttpServletRequest request,
                            HttpServletResponse response)
                            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!isAdmin(request, response)) return;

        try {

            int id = Integer.parseInt(request.getParameter("id"));

            if (id <= 0) {
                response.setStatus(400);
                response.getWriter().write(
                    "{\"error\":\"Invalid product ID\"}"
                );
                return;
            }

            String sql = "UPDATE products SET is_active = 0 " +
                         "WHERE product_id = ? AND is_active = 1";

            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setInt(1, id);

                int rows = ps.executeUpdate();

                if (rows > 0) {

                    response.getWriter().write(
                        "{\"message\":\"Product removed successfully!\"}"
                    );

                } else {

                    response.setStatus(404);
                    response.getWriter().write(
                        "{\"error\":\"Product not found or already removed\"}"
                    );
                }
            }

        } catch (Exception e) {

            e.printStackTrace();
            response.setStatus(500);

            response.getWriter().write(
                "{\"error\":\"Unable to remove product\"}"
            );
        }
    }
}
