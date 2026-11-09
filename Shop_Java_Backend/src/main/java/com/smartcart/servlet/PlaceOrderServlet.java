package com.smartcart.servlet;

import com.smartcart.config.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/place-order")
public class PlaceOrderServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Connection con = null;

        try {

            // 1. VERIFY CUSTOMER SESSION

            HttpSession session = request.getSession(false);

            if (session == null ||
                !"CUSTOMER".equals(session.getAttribute("userRole"))) {

                response.setStatus(401);
                response.getWriter().write(
                    "{\"message\":\"Please login as a customer.\"}"
                );
                return;
            }

            Object userIdObject = session.getAttribute("userId");

            if (!(userIdObject instanceof Number)) {
                response.setStatus(401);
                response.getWriter().write(
                    "{\"message\":\"Invalid customer session.\"}"
                );
                return;
            }

            int userId = ((Number) userIdObject).intValue();

            // 2. GET CUSTOMER DETAILS

            String customerName = request.getParameter("customerName");
            String phone = request.getParameter("phone");
            String address = request.getParameter("address");
            String city = request.getParameter("city");
            String pincode = request.getParameter("pincode");
            String paymentMethod = request.getParameter("paymentMethod");

            if (customerName == null || customerName.trim().isEmpty() ||
                phone == null || phone.trim().isEmpty() ||
                address == null || address.trim().isEmpty() ||
                city == null || city.trim().isEmpty() ||
                pincode == null || pincode.trim().isEmpty()) {

                throw new IllegalArgumentException(
                    "Please fill all delivery details."
                );
            }

            if (!"COD".equals(paymentMethod)) {
                throw new IllegalArgumentException(
                    "Invalid payment method."
                );
            }

            // 3. GET PRODUCT IDS AND QUANTITIES

            String[] productIds =
                request.getParameter("productIds").split(",");

            String[] quantities =
                request.getParameter("quantities").split(",");

            if (productIds.length == 0 ||
                productIds.length != quantities.length) {

                throw new IllegalArgumentException(
                    "Invalid cart information."
                );
            }

            // Combine duplicate product IDs safely.

            Map<Integer, Integer> cartItems = new LinkedHashMap<>();

            for (int i = 0; i < productIds.length; i++) {

                int productId = Integer.parseInt(productIds[i].trim());
                int quantity = Integer.parseInt(quantities[i].trim());

                if (productId <= 0 || quantity <= 0) {
                    throw new IllegalArgumentException(
                        "Invalid product or quantity."
                    );
                }

                cartItems.merge(productId, quantity, Math::addExact);
            }

            if (cartItems.isEmpty()) {
                throw new IllegalArgumentException(
                    "Your cart is empty."
                );
            }

            // 4. CONNECT TO DATABASE

            con = DBConnection.getConnection();

            if (con == null) {
                throw new SQLException("Database connection failed.");
            }

            con.setAutoCommit(false);

            BigDecimal totalAmount = BigDecimal.ZERO;

            // 5. VERIFY PRODUCTS AND CALCULATE TOTAL

            Map<Integer, BigDecimal> productPrices =
                new LinkedHashMap<>();

            String productSQL =
                "SELECT price, stock FROM products " +
                "WHERE product_id = ? FOR UPDATE";

            try (PreparedStatement ps =
                    con.prepareStatement(productSQL)) {

                for (Map.Entry<Integer, Integer> item :
                        cartItems.entrySet()) {

                    int productId = item.getKey();
                    int quantity = item.getValue();

                    ps.setInt(1, productId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                "A product in your cart is unavailable."
                            );
                        }

                        BigDecimal price = rs.getBigDecimal("price");
                        int stock = rs.getInt("stock");

                        if (quantity > stock) {
                            throw new IllegalArgumentException(
                                "Insufficient stock for product ID: "
                                + productId
                            );
                        }

                        productPrices.put(productId, price);

                        totalAmount = totalAmount.add(
                            price.multiply(BigDecimal.valueOf(quantity))
                        );
                    }
                }
            }

            // 6. INSERT INTO ORDERS TABLE

            String orderSQL =
                "INSERT INTO orders " +
                "(user_id, total_amount, order_status, order_date, " +
                "customer_name, phone, delivery_address, city, pincode) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?)";

            int orderId;

            try (PreparedStatement ps = con.prepareStatement(
                    orderSQL, Statement.RETURN_GENERATED_KEYS)) {

                ps.setInt(1, userId);
                ps.setBigDecimal(2, totalAmount);
                ps.setString(3, "PLACED");
                ps.setString(4, customerName.trim());
                ps.setString(5, phone.trim());
                ps.setString(6, address.trim());
                ps.setString(7, city.trim());
                ps.setString(8, pincode.trim());

                ps.executeUpdate();

                try (ResultSet rs = ps.getGeneratedKeys()) {

                    if (!rs.next()) {
                        throw new SQLException(
                            "Unable to generate order ID."
                        );
                    }

                    orderId = rs.getInt(1);
                }
            }

            // 7. INSERT INTO ORDER_ITEMS AND REDUCE STOCK

            String itemSQL =
                "INSERT INTO order_items " +
                "(order_id, product_id, quantity, price, size) " +
                "VALUES (?, ?, ?, ?, ?)";

            String stockSQL =
                "UPDATE products SET stock = stock - ? " +
                "WHERE product_id = ? AND stock >= ?";

            try (PreparedStatement itemPS =
                    con.prepareStatement(itemSQL);
                 PreparedStatement stockPS =
                    con.prepareStatement(stockSQL)) {

                for (Map.Entry<Integer, Integer> item :
                        cartItems.entrySet()) {

                    int productId = item.getKey();
                    int quantity = item.getValue();

                    itemPS.setInt(1, orderId);
                    itemPS.setInt(2, productId);
                    itemPS.setInt(3, quantity);
                    itemPS.setBigDecimal(
                        4, productPrices.get(productId)
                    );
                    itemPS.setString(5, "");

                    itemPS.executeUpdate();

                    stockPS.setInt(1, quantity);
                    stockPS.setInt(2, productId);
                    stockPS.setInt(3, quantity);

                    if (stockPS.executeUpdate() != 1) {
                        throw new SQLException(
                            "Stock update failed for product ID: "
                            + productId
                        );
                    }
                }
            }

            // 8. INSERT INTO PAYMENTS TABLE

            String paymentSQL =
                "INSERT INTO payments " +
                "(order_id, payment_method, payment_status, payment_date) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP)";

            try (PreparedStatement ps =
                    con.prepareStatement(paymentSQL)) {

                ps.setInt(1, orderId);
                ps.setString(2, "COD");
                ps.setString(3, "PENDING");

                ps.executeUpdate();
            }

            // CLEAR CUSTOMER CART AFTER SUCCESSFUL ORDER
String clearCartSQL = "DELETE FROM cart WHERE user_id = ?";

try (PreparedStatement clearPS = con.prepareStatement(clearCartSQL)) {
    clearPS.setInt(1, userId);
    clearPS.executeUpdate();
}

            // 9. COMMIT TRANSACTION

            con.commit();

            response.setStatus(200);

            response.getWriter().write(
                "{\"success\":true," +
                "\"message\":\"Order placed successfully!\"," +
                "\"orderId\":" + orderId + "," +
                "\"totalAmount\":\"" + totalAmount.toPlainString() + "\"}"
            );

        } catch (Exception e) {

            // ROLLBACK IF ANY OPERATION FAILS

            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException rollbackError) {
                    rollbackError.printStackTrace();
                }
            }

            e.printStackTrace();

            response.setStatus(
                e instanceof IllegalArgumentException ? 400 : 500
            );

            String message =
                e instanceof IllegalArgumentException
                ? e.getMessage()
                : "Unable to place order. Please try again.";

            response.getWriter().write(
                "{\"success\":false,\"message\":\""
                + message.replace("\\", "\\\\")
                         .replace("\"", "\\\"")
                + "\"}"
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