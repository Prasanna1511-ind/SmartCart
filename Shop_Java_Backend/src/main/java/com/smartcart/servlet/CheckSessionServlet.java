package com.smartcart.servlet;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/check-session")
public class CheckSessionServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        HttpSession session = request.getSession(false);

        PrintWriter out = response.getWriter();

        if (session != null &&
                session.getAttribute("userId") != null) {

            String name = String.valueOf(session.getAttribute("userName"));
            String email = String.valueOf(session.getAttribute("userEmail"));
            String phone = String.valueOf(session.getAttribute("userPhone"));
            String role = String.valueOf(session.getAttribute("userRole"));

            name = name.replace("\\", "\\\\").replace("\"", "\\\"");
            email = email.replace("\\", "\\\\").replace("\"", "\\\"");
            phone = phone.replace("\\", "\\\\").replace("\"", "\\\"");
            role = role.replace("\\", "\\\\").replace("\"", "\\\"");

            out.print("{"
                    + "\"loggedIn\":true,"
                    + "\"name\":\"" + name + "\","
                    + "\"email\":\"" + email + "\","
                    + "\"phone\":\"" + phone + "\","
                    + "\"role\":\"" + role + "\""
                    + "}");

        } else {

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"loggedIn\":false}");

        }
    }
}