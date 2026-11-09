// SMARTCART CUSTOMER DASHBOARD

const API = "http://localhost:8080/Shop";


// 1. LOAD CUSTOMER SESSION

async function loadCustomerDetails() {

    try {

        const response = await fetch(`${API}/check-session`, {
            credentials: "include"
        });

        if (!response.ok) {
            window.location.href = "login.html";
            return false;
        }

        const user = await response.json();

        if (!user.loggedIn) {
            window.location.href = "login.html";
            return false;
        }

        document.getElementById("customerName").textContent =
            user.name || "Customer";

        document.getElementById("customerEmail").textContent =
            user.email || "Not available";

        document.getElementById("customerPhone").textContent =
            user.phone || "Not available";

        return true;

    } catch (error) {

        console.error("Session Error:", error);

        window.location.href = "login.html";

        return false;
    }
}


// 2. ORDER TRACKING TIMELINE

function createTrackingTimeline(status) {

    const steps = [
        "PLACED",
        "CONFIRMED",
        "SHIPPED",
        "DELIVERED"
    ];

    const currentStatus = String(status || "PENDING").toUpperCase();

    if (currentStatus === "CANCELLED") {

        return `
            <div class="cancelled-status">
                ✖ Order Cancelled
            </div>
        `;
    }

    // PENDING and PLACED both represent the first stage.
    const currentIndex = Math.max(
        0,
        steps.indexOf(currentStatus)
    );

    const stepLabels = [
        "Placed",
        "Confirmed",
        "Shipped",
        "Delivered"
    ];

    return `
        <div class="tracking-container">

            <div class="tracking-status">
                ${currentStatus}
            </div>

            <div class="tracking-steps">

                ${steps.map((step, index) => `
                    <div class="tracking-step ${
                        index <= currentIndex ? "completed" : ""
                    }">

                        <div class="tracking-dot"></div>

                        <span>${stepLabels[index]}</span>

                    </div>
                `).join("")}

            </div>

        </div>
    `;
}


// 3. LOAD CUSTOMER ORDERS FROM MYSQL

async function loadCustomerOrders() {

    const ordersTable = document.getElementById("ordersTable");

    try {

        const response = await fetch(`${API}/customer-orders`, {
            credentials: "include"
        });

        if (response.status === 401) {
            window.location.href = "login.html";
            return;
        }

        if (!response.ok) {
            throw new Error("Unable to load orders");
        }

        const orders = await response.json();

// Display orders in ascending order: 1, 2, 3, 4, 5, 6
orders.sort((a, b) => a.orderId - b.orderId);

        document.getElementById("totalOrders").textContent =
            orders.length;

        const totalPurchase = orders.reduce(
            (sum, order) =>
                sum + Number(order.total ?? order.totalAmount ?? 0),
            0
        );

        document.getElementById("totalPurchase").textContent =
            totalPurchase.toFixed(2);

        if (!orders.length) {

            ordersTable.innerHTML = `
                <tr>
                    <td colspan="6">No orders found</td>
                </tr>
            `;

            return;
        }

        ordersTable.innerHTML = "";

        orders.forEach(order => {

            const row = document.createElement("tr");

            const orderId = order.orderId ?? "N/A";

            const date = order.date ?? order.orderDate ?? "N/A";

            const total = Number(
                order.total ?? order.totalAmount ?? 0
            );

            const payment =
                order.paymentMethod || "N/A";

            const status =
                order.status || order.orderStatus || "PENDING";

            const formattedDate = date === "N/A"
                ? "N/A"
                : new Date(date).toLocaleDateString("en-IN");

            row.innerHTML = `
                <td>#${orderId}</td>

                <td>${formattedDate}</td>

                <td>₹${total.toFixed(2)}</td>

                <td>${payment}</td>

                <td>${status}</td>

                <td>
                    ${createTrackingTimeline(status)}
                </td>
            `;

            ordersTable.appendChild(row);

        });

    } catch (error) {

        console.error("Orders Error:", error);

        ordersTable.innerHTML = `
            <tr>
                <td colspan="6">Unable to load orders</td>
            </tr>
        `;
    }
}


// 4. LOAD WISHLIST COUNT

async function loadWishlistCount() {

    try {

        const response = await fetch(`${API}/wishlist-count`, {
            method: "GET",
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Unable to load wishlist");
        }

        const data = await response.json();

        document.getElementById("totalWishlist").textContent =
            data.wishlistCount;

    } catch (error) {

        console.error("Wishlist Error:", error);

        document.getElementById("totalWishlist").textContent = 0;
    }
}


// 5. LOGOUT

document.getElementById("logoutBtn").addEventListener(
    "click",
    async function () {

        try {

            const response = await fetch(`${API}/logout`, {
                method: "POST",
                credentials: "include"
            });

            if (response.ok) {

                alert("Logged out successfully!");

                window.location.href = "login.html";

            } else {

                alert("Logout failed!");
            }

        } catch (error) {

            console.error("Logout Error:", error);

            alert("Unable to logout!");
        }
    }
);


// 6. START CUSTOMER DASHBOARD

async function loadDashboard() {

    const loggedIn = await loadCustomerDetails();

    if (!loggedIn) {
        return;
    }

    await Promise.all([
        loadWishlistCount(),
        loadCustomerOrders()
    ]);
}

loadDashboard();