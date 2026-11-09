SHOP - JAVA SERVLET + MYSQL STARTER
===================================

This is a Maven WAR project for Apache Tomcat 10.1 and Java 17+.

1. Requirements
   - JDK 17 or later
   - Apache Tomcat 10.1.x
   - MySQL Server
   - Maven
   - VS Code with Extension Pack for Java (optional but helpful)

2. Database
   Open MySQL Workbench and run:
       CREATE DATABASE IF NOT EXISTS smartcart;

   Your existing SmartCart tables can remain in this database.

3. Set your MySQL password
   Open:
       src/main/java/com/smartcart/config/DBConnection.java
   Replace YOUR_MYSQL_PASSWORD with your actual MySQL root password.
   If root has no password, use an empty string: ""

4. Build the project
   Open the Shop_Java_Backend folder in VS Code.
   Open Terminal > New Terminal and run:
       mvn clean package

   If successful, Maven creates:
       target/Shop.war

5. Deploy to Tomcat
   Copy target/Shop.war to:
       D:\apache-tomcat-10.1.59\webapps\

   Stop and start Tomcat, or let Tomcat auto-deploy the WAR.
   Do not manually copy the src folder into webapps.

6. Test
       http://localhost:8080/Shop/
       http://localhost:8080/Shop/test-db

   The second URL should show:
       Shop - Database Connected Successfully!

7. Important
   - Tomcat 10.1 uses jakarta.servlet imports, not javax.servlet.
   - The MySQL JDBC driver is included automatically in the WAR by Maven.
   - Do not add servlet-api.jar manually; Tomcat provides the Servlet API.
   - Keep the project name/context path Shop and database name smartcart.

8. Add your existing frontend pages
   Copy your existing HTML, CSS, JavaScript, images and assets into:
       src/main/webapp/

   Keep CSS/JS paths relative to the webapp root, for example:
       css/style.css
       js/script.js
       images/product.jpg
