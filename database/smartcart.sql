-- ==========================================
-- SMARTCART - ONLINE SHOPPING WEBSITE
-- Database: smartcart
-- ==========================================

CREATE DATABASE IF NOT EXISTS smartcart;

USE smartcart;


-- ==========================================
-- 1. USERS TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone VARCHAR(15),
    address TEXT,
    role ENUM('CUSTOMER', 'ADMIN') DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ==========================================
-- 2. CATEGORIES TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL
);


-- ==========================================
-- 3. SHOPS TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS shops (
    shop_id INT AUTO_INCREMENT PRIMARY KEY,
    shop_name VARCHAR(150) NOT NULL,
    city VARCHAR(100),
    phone VARCHAR(15),
    address TEXT
);


-- ==========================================
-- 4. PRODUCTS TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(150) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    stock INT DEFAULT 0,
    image VARCHAR(255),
    category_id INT,
    shop_id INT,

    FOREIGN KEY (category_id)
    REFERENCES categories(category_id),

    FOREIGN KEY (shop_id)
    REFERENCES shops(shop_id)
);


-- ==========================================
-- 5. CART TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS cart (
    cart_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT DEFAULT 1,
    size VARCHAR(20),

    FOREIGN KEY (user_id)
    REFERENCES users(user_id),

    FOREIGN KEY (product_id)
    REFERENCES products(product_id)
);


-- ==========================================
-- 6. WISHLIST TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS wishlist (
    wishlist_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,

    FOREIGN KEY (user_id)
    REFERENCES users(user_id),

    FOREIGN KEY (product_id)
    REFERENCES products(product_id)
);


-- ==========================================
-- 7. ORDERS TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    order_status VARCHAR(50) DEFAULT 'PLACED',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
    REFERENCES users(user_id)
);


-- ==========================================
-- 8. ORDER ITEMS TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS order_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    product_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    size VARCHAR(20),

    FOREIGN KEY (order_id)
    REFERENCES orders(order_id),

    FOREIGN KEY (product_id)
    REFERENCES products(product_id)
);


-- ==========================================
-- 9. PAYMENTS TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    payment_method VARCHAR(50),
    payment_status VARCHAR(50) DEFAULT 'PENDING',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (order_id)
    REFERENCES orders(order_id)
);


-- ==========================================
-- 10. REVIEWS TABLE
-- ==========================================

CREATE TABLE IF NOT EXISTS reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_id INT NOT NULL,
    rating INT,
    comment TEXT,
    review_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
    REFERENCES users(user_id),

    FOREIGN KEY (product_id)
    REFERENCES products(product_id)
);


-- ==========================================
-- DISPLAY ALL TABLES
-- ==========================================

SHOW TABLES;

-- ==========================================
-- SMARTCART DATABASE CREATED
-- ==========================================