# SmartCart -- Local Dress Shop Online Shopping Website

SmartCart is a web-based online shopping application designed for local
dress shops in Tamil Nadu. Customers can register, log in, browse
products, add items to their cart or wishlist, place Cash on Delivery
(COD) orders, and view their order status. Administrators can manage
products, view orders, and update order statuses.

## 1. Project Objectives

-   Provide an online shopping platform for local dress shops.
-   Allow customers to register, log in, and manage their profiles.
-   Display products with details such as name, price, images, and
    stock.
-   Support cart, wishlist, checkout, and COD order placement.
-   Allow customers to view their orders.
-   Provide an admin dashboard for product and order management.
-   Use session handling and role-based access.

## 2. Technologies Used

  Part                    Technology
  ----------------------- -----------------------
  Frontend                HTML, CSS, JavaScript
  Backend                 Java Servlets
  Database                MySQL
  Database Connectivity   JDBC
  Server                  Apache Tomcat 10.1
  Build Tool              Apache Maven
  JDK                     Java 17

## 3. Main Modules

### Customer

-   Registration and Login
-   Product Catalogue and Product Details
-   Shopping Cart
-   Wishlist
-   Checkout and COD Order Placement
-   Customer Dashboard
-   Order History and Order Status

### Admin

-   Admin Login and Role Verification
-   Add, Edit, Delete, and Restore Products
-   View Customer Orders
-   Update Order Status
-   View Dashboard Statistics

### Order Status

`PLACED` → `CONFIRMED` → `SHIPPED` → `DELIVERED`

An order may also be marked as `CANCELLED`.

## 4. Project Structure

``` text
SmartCart/
├── frontend/
│   ├── index.html
│   ├── pages/
│   │   ├── login.html
│   │   ├── register.html
│   │   ├── products.html
│   │   ├── product-details.html
│   │   ├── cart.html
│   │   ├── wishlist.html
│   │   ├── checkout.html
│   │   ├── customer-dashboard.html
│   │   └── admin-dashboard.html
│   ├── css/
│   │   ├── style.css
│   │   ├── responsive.css
│   │   ├── auth.css
│   │   ├── products.css
│   │   ├── cart.css
│   │   └── dashboard.css
│   ├── js/
│   │   ├── app.js
│   │   ├── auth.js
│   │   ├── products.js
│   │   ├── product-details.js
│   │   ├── cart.js
│   │   ├── wishlist.js
│   │   ├── checkout.js
│   │   ├── dashboard.js
│   │   └── admin.js
│   └── assets/
│       ├── images/
│       └── icons/
└── Shop_Java_Backend/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            │   └── Java Servlet and database classes
            └── webapp/
```

*This is a guide to the main folders. Your local project may contain
additional files or a slightly different package structure.*

## 5. Database Setup

1.  Install and start MySQL Server.
2.  Create the database:

``` sql
CREATE DATABASE smartcart;
USE smartcart;
```

3.  Create the required tables. The main tables used by the project are:

-   `users`
-   `categories`
-   `shops`
-   `products`
-   `cart`
-   `wishlist`
-   `orders`
-   `order_items`
-   `payments`
-   `reviews`

4.  Make sure the table columns match the SQL queries in the Java
    backend.
5.  Open the database connection class (for example,
    `DBConnection.java`) and configure your local MySQL credentials.

Example:

``` java
String url = "jdbc:mysql://localhost:3306/smartcart";
String username = "root";
String password = "YOUR_MYSQL_PASSWORD";
```

Replace `YOUR_MYSQL_PASSWORD` with your own MySQL password. Never upload
real passwords to GitHub.

## 6. Software Requirements

Install:

-   JDK 17
-   Apache Maven 3.x
-   Apache Tomcat 10.1
-   MySQL Server
-   MySQL Connector/J
-   VS Code or another Java IDE

Verify Java and Maven:

``` bash
java -version
mvn -version
```

## 7. Build the Backend

Open a terminal in the backend folder:

``` bash
cd Shop_Java_Backend
mvn clean package
```

After a successful build, the WAR file is generated in the `target`
folder. In this project, the deployed file is named `Shop.war`:

``` text
target/Shop.war
```

If your Maven configuration produces a different filename, use the
actual WAR filename in the `target` folder.

## 8. Run Using Apache Tomcat

1.  Start MySQL.
2.  Confirm that the `smartcart` database and required tables are
    available.
3.  Build the backend using Maven.
4.  Copy `Shop.war` into the Tomcat `webapps` folder.
5.  Start Apache Tomcat and wait for deployment.
6.  Place the frontend folder inside Tomcat's `webapps` folder as
    `frontend`.

Open the frontend:

``` text
http://localhost:8080/frontend/index.html
```

Backend context:

``` text
http://localhost:8080/Shop
```

Using the same Tomcat server for the frontend and backend helps the
frontend call the backend through the `/Shop` context.

## 9. Main Backend Endpoints

  Feature            Endpoint
  ------------------ -------------------------
  Login              `/Shop/login`
  Registration       `/Shop/register`
  Check Session      `/Shop/check-session`
  Logout             `/Shop/logout`
  Products           `/Shop/products`
  Admin Products     `/Shop/admin-products`
  Admin Statistics   `/Shop/admin-stats`
  Add to Cart        `/Shop/add-to-cart`
  View Cart          `/Shop/view-cart`
  Update Cart        `/Shop/update-cart`
  Remove Cart Item   `/Shop/remove-cart`
  Wishlist           `/Shop/wishlist`
  Place Order        `/Shop/place-order`
  Customer Orders    `/Shop/customer-orders`

The HTTP method and request data depend on the corresponding frontend
JavaScript and Servlet implementation.

## 10. Application Workflow

1.  The customer registers or logs in.
2.  The customer browses products.
3.  The customer views product details and selects the required options.
4.  The customer adds products to the cart or wishlist.
5.  The customer checks out and places a COD order.
6.  The customer views order details in the customer dashboard.
7.  The administrator manages products and customer orders.
8.  The customer checks the latest order status.

## 11. Security and Access

-   Session handling maintains login information.
-   Customer and administrator roles are checked for role-specific
    actions.
-   Admin features should only be available to authenticated
    administrators.
-   Keep database credentials and other secrets private.

## 12. Future Enhancements

-   Online payment gateway integration
-   Delivery tracking
-   Mobile application
-   Multi-shop management
-   Advanced product filtering
-   Customer reviews and ratings
-   Email and SMS notifications
-   Additional security improvements

## 13. Academic Project Details

**Project:** SmartCart -- Local Dress Shop Online Shopping Website\
**Department:** Artificial Intelligence and Data Science\
**College:** Jai Shriram Engineering College, Tiruppur
