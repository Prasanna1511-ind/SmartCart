package com.smartcart.servlet;

import com.smartcart.config.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/test-db")
public class TestDBServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            try (Connection connection = DBConnection.getConnection()) {
                out.println("<!DOCTYPE html><html><head><title>Shop DB Test</title></head><body>");
                out.println("<h1>Shop - Database Connected Successfully!</h1>");
                out.println("<p>Connected to database: smartcart</p>");
                out.println("</body></html>");
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("<h1>Database connection failed</h1>");
                out.println("<pre>" + escapeHtml(e.getMessage()) + "</pre>");
                out.println("<p>Check MySQL is running, database smartcart exists, and DBConnection.java credentials are correct.</p>");
                getServletContext().log("Shop database connection failed", e);
            }
        }
    }

    private String escapeHtml(String value) {
        if (value == null) return "Unknown error";
        return value.replace("&", "&amp;").replace("<", "&lt;")
                    .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
