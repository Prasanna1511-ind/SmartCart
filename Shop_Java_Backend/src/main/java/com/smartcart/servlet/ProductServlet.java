package com.smartcart.servlet;

import com.smartcart.config.DBConnection;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/products")
public class ProductServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String sql = "SELECT product_id, product_name, description, " +
                     "price, stock, image_url, category_id, shop_id " +
                     "FROM products WHERE is_active = 1";

        try (Connection con = DBConnection.getConnection()) {

            if (con == null) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().print(
                    "{\"error\":\"Database connection failed\"}"
                );
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;

                while (rs.next()) {

                    if (!first) {
                        json.append(",");
                    }

                    json.append("{");

                    json.append("\"product_id\":").append(rs.getInt("product_id")).append(",");

                    json.append("\"product_name\":\"")
                        .append(escapeJson(rs.getString("product_name"))).append("\",");

                    json.append("\"description\":\"")
                        .append(escapeJson(rs.getString("description"))).append("\",");

                    json.append("\"price\":").append(rs.getBigDecimal("price")).append(",");

                    json.append("\"stock\":").append(rs.getInt("stock")).append(",");

                    json.append("\"image\":\"")
                        .append(escapeJson(rs.getString("image_url"))).append("\",");

                    json.append("\"category_id\":").append(rs.getInt("category_id")).append(",");

                    json.append("\"shop_id\":").append(rs.getInt("shop_id"));

                    json.append("}");

                    first = false;
                }

                json.append("]");

                response.getWriter().print(json.toString());
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            response.getWriter().print(
                "{\"error\":\"Unable to retrieve products\"}"
            );
        }
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}