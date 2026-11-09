package com.smartcart.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.smartcart.config.DBConnection;

@WebServlet("/admin-stats")
public class AdminStatsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
                         throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Check admin session
        HttpSession session = request.getSession(false);

        if (session == null ||
            !"ADMIN".equals(session.getAttribute("userRole"))) {

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(
                "{\"error\":\"Admin access denied\"}"
            );
            return;
        }

        String sql =
            "SELECT " +
            "(SELECT COUNT(*) FROM products) AS totalProducts, " +
            "(SELECT COUNT(*) FROM users WHERE role='CUSTOMER') AS totalCustomers, " +
            "(SELECT COUNT(*) FROM orders) AS totalOrders, " +
            "(SELECT COALESCE(SUM(total_amount),0) FROM orders) AS totalRevenue";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {

                int products = rs.getInt("totalProducts");
                int customers = rs.getInt("totalCustomers");
                int orders = rs.getInt("totalOrders");
                double revenue = rs.getDouble("totalRevenue");

                String json = "{"
                    + "\"totalProducts\":" + products + ","
                    + "\"totalCustomers\":" + customers + ","
                    + "\"totalOrders\":" + orders + ","
                    + "\"totalRevenue\":" + revenue
                    + "}";

                response.getWriter().write(json);
            }

        } catch (Exception e) {

            e.printStackTrace();

            response.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            response.getWriter().write(
                "{\"error\":\"Database error\"}"
            );
        }
    }
}