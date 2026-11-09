// SMARTCART ADMIN DASHBOARD

const API_URL = "/Shop";
const PRODUCT_API = `${API_URL}/products`;
const ADMIN_PRODUCT_API = `${API_URL}/admin-products`;
const ADMIN_ORDER_API = `${API_URL}/admin-orders`;

// 1. VERIFY ADMIN

async function verifyAdmin() {

try {

    const response = await fetch(`${API_URL}/check-session`, {
        credentials: "include"
    });

    if (!response.ok) {
        alert("Please login first!");
        window.location.href = "login.html";
        return false;
    }

    const user = await response.json();

    if (!user.loggedIn) {
        alert("Please login first!");
        window.location.href = "login.html";
        return false;
    }

    if (user.role !== "ADMIN") {
        alert("Access denied! Admin permission required.");
        window.location.href = "customer-dashboard.html";
        return false;
    }

    console.log("Admin verified successfully!");
    return true;

} catch (error) {

    console.error("Admin Verification Error:", error);
    alert("Unable to verify admin access.");
    window.location.href = "login.html";
    return false;
}


}

// 2. LOAD ADMIN STATISTICS

async function loadAdminStats() {


try {

    const response = await fetch(`${API_URL}/admin-stats`, {
        credentials: "include"
    });

    if (!response.ok) {
        throw new Error("Unable to load statistics");
    }

    const stats = await response.json();

    document.getElementById("totalProducts").textContent =
        stats.totalProducts;

    document.getElementById("totalCustomers").textContent =
        stats.totalCustomers;

    document.getElementById("adminOrders").textContent =
        stats.totalOrders;

    document.getElementById("totalRevenue").textContent =
        Number(stats.totalRevenue).toFixed(2);

} catch (error) {

    console.error("Admin Statistics Error:", error);
}


}


// 3. LOAD ALL PRODUCTS FOR ADMIN

async function loadAdminProducts() {

    const tableBody = document.getElementById("adminProductList");

    if (!tableBody) return;

    try {

        const response = await fetch(ADMIN_PRODUCT_API, {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Unable to load products");
        }

        const products = await response.json();

        tableBody.innerHTML = "";

        if (products.length === 0) {
            tableBody.innerHTML =
                "<tr><td colspan='6'>No products found.</td></tr>";
            return;
        }

        products.forEach(product => {

            const isActive = Number(product.is_active) === 1;

            tableBody.innerHTML += `
                <tr>
                    <td>${product.product_id}</td>

                    <td>${escapeHTML(product.product_name)}</td>

                    <td>₹${Number(product.price).toFixed(2)}</td>

                    <td>${product.stock}</td>

                    <td>
                        <span style="
                            color: ${isActive ? 'green' : 'red'};
                            font-weight: bold;
                        ">
                            ${isActive ? 'Active' : 'Inactive'}
                        </span>
                    </td>

                    <td>
                        <button
                            type="button"
                            onclick="editProduct(${product.product_id})"
                            ${!isActive ? 'disabled' : ''}>
                            Edit
                        </button>

                        ${
                            isActive
                            ? `<button type="button"
                                onclick="deleteProduct(${product.product_id})">
                                Delete
                               </button>`
                            : `<button type="button"
                                onclick="restoreProduct(${product.product_id})">
                                Restore
                               </button>`
                        }
                    </td>
                </tr>
            `;
        });

    } catch (error) {

        console.error("Product Loading Error:", error);

        tableBody.innerHTML =
            "<tr><td colspan='6'>Unable to load products.</td></tr>";
    }
}

// 4. EDIT PRODUCT

async function editProduct(id) {


try {

    const response = await fetch(ADMIN_PRODUCT_API, {
    credentials: "include"
});

    if (!response.ok) {
        throw new Error("Unable to fetch products");
    }

    const products = await response.json();

    const product = products.find(
        p => Number(p.product_id) === Number(id)
    );

    if (!product) {
        alert("Product not found!");
        return;
    }

    const name = prompt("Product Name:", product.product_name);
    if (name === null) return;

    const description = prompt(
        "Description:",
        product.description || ""
    );
    if (description === null) return;

    const price = prompt("Price:", product.price);
    if (price === null) return;

    const stock = prompt("Stock:", product.stock);
    if (stock === null) return;

    const image = prompt("Image URL:", product.image || "");
    if (image === null) return;

    const categoryId = prompt("Category ID:", product.category_id);
    if (categoryId === null) return;

    const shopId = prompt("Shop ID:", product.shop_id);
    if (shopId === null) return;

    if (
        !name.trim() ||
        Number(price) <= 0 ||
        !Number.isFinite(Number(price)) ||
        !Number.isInteger(Number(stock)) ||
        Number(stock) < 0 ||
        !Number.isInteger(Number(categoryId)) ||
        Number(categoryId) <= 0 ||
        !Number.isInteger(Number(shopId)) ||
        Number(shopId) <= 0
    ) {
        alert("Please enter valid product details.");
        return;
    }

    const data = new URLSearchParams({
        productId: id,
        productName: name.trim(),
        description: description,
        price: price,
        stock: stock,
        image: image,
        categoryId: categoryId,
        shopId: shopId
    });

    const updateResponse = await fetch(ADMIN_PRODUCT_API, {
        method: "PUT",
        credentials: "include",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },
        body: data
    });

    const result = await updateResponse.json();

    if (updateResponse.ok) {

        alert(result.message);

        await loadAdminProducts();
        await loadAdminStats();

    } else {

        alert(result.error || "Unable to update product");
    }

} catch (error) {

    console.error("Edit Product Error:", error);
    alert("Unable to update product.");
}


}

// 5. DELETE PRODUCT

async function deleteProduct(id) {

const confirmDelete = confirm(
    "Are you sure you want to delete this product?"
);

if (!confirmDelete) return;

try {

    const response = await fetch(
        `${ADMIN_PRODUCT_API}?id=${id}`,
        {
            method: "DELETE",
            credentials: "include"
        }
    );

    const result = await response.json();

    if (response.ok) {

        alert(result.message);

        await loadAdminProducts();
        await loadAdminStats();

    } else {

        alert(result.error || "Unable to delete product");
    }

} catch (error) {

    console.error("Delete Product Error:", error);
    alert("Unable to delete product.");
}


}


//6. RESTORE PRODUCT

// RESTORE PRODUCT

async function restoreProduct(id) {

    const confirmRestore = confirm(
        "Do you want to restore this product?"
    );

    if (!confirmRestore) return;

    try {

        const response = await fetch(ADMIN_PRODUCT_API, {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
                action: "restore",
                id: id
            })
        });

        const result = await response.json();

        if (response.ok) {

            alert(result.message);

            await loadAdminProducts();
            await loadAdminStats();

        } else {

            alert(result.error || "Unable to restore product");
        }

    } catch (error) {

        console.error("Restore Product Error:", error);
        alert("Unable to restore product.");
    }
}

// 6. ADD PRODUCT

const productForm = document.getElementById("productForm");

if (productForm) {

productForm.addEventListener("submit", async function(event) {

    event.preventDefault();

    const product = {
        productName: document.getElementById("productName").value.trim(),
        description: document.getElementById("productDescription").value.trim(),
        categoryId: document.getElementById("productCategory").value,
        shopId: document.getElementById("productShop").value,
        price: document.getElementById("productPrice").value,
        stock: document.getElementById("productStock").value,
        image: document.getElementById("productImage").value.trim()
    };

    try {

        const response = await fetch(ADMIN_PRODUCT_API, {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams(product)
        });

        const result = await response.json();

        if (response.ok) {

            alert(result.message);

            productForm.reset();

            await loadAdminStats();
            await loadAdminProducts();

        } else {

            alert(result.error || "Unable to add product");
        }

    } catch (error) {

        console.error("Add Product Error:", error);
        alert("Server connection failed.");
    }
});


}

// 7. HTML ESCAPE

function escapeHTML(value) {

return String(value ?? "").replace(/[&<>"']/g, character => {

    const entities = {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    };

    return entities[character];
});
}

// LOAD ALL CUSTOMER ORDERS

async function loadAdminOrders() {

    const tableBody = document.getElementById("adminOrderTable");

    if (!tableBody) return;

    try {

        const response = await fetch(ADMIN_ORDER_API, {
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Unable to load orders");
        }

        const orders = await response.json();

        tableBody.innerHTML = "";

        if (orders.length === 0) {
            tableBody.innerHTML =
                "<tr><td colspan='6'>No orders found.</td></tr>";
            return;
        }

        orders.forEach(order => {

            const status = order.orderStatus || "PENDING";

            tableBody.innerHTML += `
                <tr>

                    <td>${order.orderId}</td>

                    <td>${escapeHTML(order.customerName)}</td>

                    <td>₹${Number(order.totalAmount).toFixed(2)}</td>

                    <td>${escapeHTML(order.orderDate)}</td>

                    <td>
                        <select id="status-${order.orderId}">

                            <option value="PENDING"
                                ${status === "PENDING" ? "selected" : ""}>
                                Pending
                            </option>

                            <option value="PLACED"
                                ${status === "PLACED" ? "selected" : ""}>
                                Placed
                            </option>

                            <option value="CONFIRMED"
                                ${status === "CONFIRMED" ? "selected" : ""}>
                                Confirmed
                            </option>

                            <option value="SHIPPED"
                                ${status === "SHIPPED" ? "selected" : ""}>
                                Shipped
                            </option>

                            <option value="DELIVERED"
                                ${status === "DELIVERED" ? "selected" : ""}>
                                Delivered
                            </option>

                            <option value="CANCELLED"
                                ${status === "CANCELLED" ? "selected" : ""}>
                                Cancelled
                            </option>

                        </select>
                    </td>

                    <td>
                        <button onclick="viewOrder(${order.orderId})">
                            View
                        </button>

                        <button onclick="updateOrderStatus(${order.orderId})">
                            Update
                        </button>
                    </td>

                </tr>
            `;
        });

    } catch (error) {

        console.error("Order Loading Error:", error);

        tableBody.innerHTML =
            "<tr><td colspan='6'>Unable to load orders.</td></tr>";
    }
}

// VIEW INDIVIDUAL ORDER DETAILS

async function viewOrder(id) {

    try {

        const response = await fetch(
            `${ADMIN_ORDER_API}?id=${id}`,
            {
                method: "GET",
                credentials: "include"
            }
        );

        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || "Unable to load order details");
        }

        console.log("Order Details:", data);

        alert(JSON.stringify(data, null, 2));

    } catch (error) {

        console.error("View Order Error:", error);

        alert("Unable to load order details.");
    }
}

// UPDATE ORDER STATUS

async function updateOrderStatus(id) {

    const statusElement = document.getElementById(`status-${id}`);

    if (!statusElement) return;

    const status = statusElement.value;

    const confirmUpdate = confirm(
        `Update Order #${id} status to ${status}?`
    );

    if (!confirmUpdate) return;

    try {

        const response = await fetch(ADMIN_ORDER_API, {
    method: "PUT",

    credentials: "include",

    headers: {
        "Content-Type": "application/x-www-form-urlencoded"
    },

    body: new URLSearchParams({
        id: id,
        status: status
    })
});

        const result = await response.json();

        if (response.ok) {

            alert(result.message);

            await loadAdminOrders();
            await loadAdminStats();

        } else {

            alert(result.message || "Unable to update order status");
        }

    } catch (error) {

        console.error("Update Order Error:", error);

        alert("Unable to update order status.");
    }
}

// 8. START ADMIN DASHBOARD

async function startAdminDashboard() {

    const isAdmin = await verifyAdmin();

    if (!isAdmin) return;

    console.log("Welcome to SmartCart Admin Dashboard!");

    await loadAdminStats();

    await loadAdminProducts();

    await loadAdminOrders();

}

startAdminDashboard();
