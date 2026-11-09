package com.smartcart.config;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
        "jdbc:mysql://localhost:3306/smartcart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    private static final String USER = "root";

    private static final String PASSWORD = "Robin@123";

    public static Connection getConnection() {

        try {

            // Load MySQL JDBC Driver
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection con = DriverManager.getConnection(
                URL, USER, PASSWORD
            );

            System.out.println("MySQL Connected Successfully!");

            return con;

        } catch (Exception e) {

            e.printStackTrace();

            return null;
        }
    }
}