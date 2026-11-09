// SMARTCART - DATABASE CONNECTED CHECKOUT

const API = "http://localhost:8080/Shop";

let cartItems = [];

const checkoutForm = document.getElementById("checkoutForm");
const checkoutTotal = document.getElementById("checkoutTotal");
const placeOrderBtn = document.getElementById("placeOrderBtn");

// 1. LOAD CART FROM MYSQL

async function loadCheckout() {

    try {

        const response = await fetch(
            "http://localhost:8080/Shop/view-cart",
            {
                method: "GET",
                credentials: "include"
            }
        );

        const data = await response.json();

        console.log("API DATA:", data);

        cartItems = Array.isArray(data) ? data : (data.cart || []);

        console.log("CART ITEMS:", cartItems);

        if (cartItems.length === 0) {

            checkoutTotal.textContent = "0.00";
            placeOrderBtn.disabled = true;

            alert("Your cart is empty!");

            return;
        }

        let total = 0;

        cartItems.forEach(item => {

            console.log("Price:", item.price);
            console.log("Quantity:", item.quantity);

            total += Number(item.price) * Number(item.quantity);

        });

        checkoutTotal.textContent = total.toFixed(2);

        console.log("FINAL TOTAL:", total);

    } catch (error) {

        console.error("Checkout Error:", error);

        checkoutTotal.textContent = "Error";

    }

}

// 2. CALCULATE TOTAL

function calculateTotal() {


let total = 0;

cartItems.forEach(item => {

    const price = Number(item.price);

    const quantity = Number(item.quantity);

    total += price * quantity;

});

checkoutTotal.textContent = total.toLocaleString("en-IN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
});

return total;


}

// 3. PLACE ORDER

checkoutForm.addEventListener("submit", async function(event) {


event.preventDefault();

if (cartItems.length === 0) {

    alert("Your cart is empty!");

    return;
}

const productIds = [];

const quantities = [];

// VALIDATE CART

for (const item of cartItems) {

    if (
        Number(item.quantity) <= 0 ||
        Number(item.quantity) > Number(item.stock)
    ) {

        alert("Insufficient stock for " + item.product_name);

        return;
    }

    productIds.push(item.product_id);

    quantities.push(item.quantity);

}


// CUSTOMER DETAILS

const payment = document.querySelector(
    'input[name="payment"]:checked'
);

const formData = new URLSearchParams();

formData.append(
    "customerName",
    document.getElementById("customerName").value.trim()
);

formData.append(
    "phone",
    document.getElementById("phone").value.trim()
);

formData.append(
    "address",
    document.getElementById("address").value.trim()
);

formData.append(
    "city",
    document.getElementById("city").value.trim()
);

formData.append(
    "pincode",
    document.getElementById("pincode").value.trim()
);

formData.append("paymentMethod", payment.value);

formData.append("productIds", productIds.join(","));

formData.append("quantities", quantities.join(","));


// SEND ORDER TO JAVA SERVLET

try {

    placeOrderBtn.disabled = true;

    placeOrderBtn.textContent = "Processing...";

    const response = await fetch(`${API}/place-order`, {

        method: "POST",

        credentials: "include",

        headers: {
            "Content-Type": "application/x-www-form-urlencoded"
        },

        body: formData

    });

    const result = await response.json();

    if (!response.ok) {

        alert(result.message || "Order placement failed!");

        return;
    }


    // ORDER SUCCESS

    alert(result.message || "Order placed successfully!");

    window.location.href = "customer-dashboard.html";


} catch (error) {

    console.error("Place Order Error:", error);

    alert("Unable to place order. Please try again.");

} finally {

    placeOrderBtn.disabled = false;

    placeOrderBtn.textContent = "Place Order";

}


});

// 4. START CHECKOUT

loadCheckout();
