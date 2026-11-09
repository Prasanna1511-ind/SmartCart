// SMARTCART - DATABASE CONNECTED SHOPPING CART

const API = "http://localhost:8080/Shop";

const cartContainer = document.getElementById("cartContainer");

let cartItems = [];

// 1. LOAD CART FROM MYSQL

async function loadCart() {

    try {

        const response = await fetch(`${API}/view-cart`, {
            method: "GET",
            credentials: "include"
        });

        if (!response.ok) {
            throw new Error("Unable to load cart");
        }

        const data = await response.json();

        // Support both [] and {cart: []} responses
        cartItems = Array.isArray(data) ? data : (data.cart || []);

        displayCart();

    } catch (error) {

        console.error("Cart Error:", error);

        cartContainer.innerHTML =
            "<h2>Unable to load cart. Please login and try again.</h2>";

    }

}


// 2. DISPLAY CART

function displayCart() {

    cartContainer.innerHTML = "";

    let total = 0;

    if (cartItems.length === 0) {

        cartContainer.innerHTML = "<h2>Your cart is empty!</h2>";

        document.getElementById("cartTotal").textContent = "0";

        return;
    }

    cartItems.forEach(item => {

        const quantity = Number(item.quantity);

        const price = Number(item.price);

        const subtotal = price * quantity;

        total += subtotal;

        const imageHTML = item.image
            ? `<img src="${escapeHTML(item.image)}"
                    alt="${escapeHTML(item.product_name)}"
                    onerror="this.style.display='none'">`
            : `<div class="product-image-placeholder">
                    Image Coming Soon
               </div>`;

        cartContainer.innerHTML += `

            <div class="cart-item">

                ${imageHTML}

                <div>

                    <h3>${escapeHTML(item.product_name)}</h3>

                    <p>
                        Price: ₹${price.toLocaleString("en-IN")}
                    </p>

                    <p>
                        Size: ${escapeHTML(item.size || "Not selected")}
                    </p>

                    <p>

                        Quantity:

                        <button onclick="changeQuantity(${Number(item.cart_id)}, -1)">
                            -
                        </button>

                        <span class="quantity">
                            ${quantity}
                        </span>

                        <button onclick="changeQuantity(${Number(item.cart_id)}, 1)">
                            +
                        </button>

                    </p>

                    <p>
                        Subtotal: ₹${subtotal.toLocaleString("en-IN")}
                    </p>

                    <button onclick="removeCart(${Number(item.cart_id)})">
                        Remove
                    </button>

                </div>

            </div>

        `;

    });

    document.getElementById("cartTotal").textContent =
        total.toLocaleString("en-IN");

}


// 3. CHANGE QUANTITY

async function changeQuantity(cartId, change) {

    const item = cartItems.find(
        p => Number(p.cart_id) === Number(cartId)
    );

    if (!item) return;

    const newQuantity = Number(item.quantity) + change;

    if (newQuantity <= 0) {

        await removeCart(cartId);

        return;
    }

    if (newQuantity > Number(item.stock)) {

        alert("Maximum available stock reached!");

        return;
    }

    try {

        const response = await fetch(`${API}/update-cart`, {

            method: "POST",

            credentials: "include",

            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },

            body: new URLSearchParams({

                cartId: cartId,

                quantity: newQuantity

            })

        });

        const data = await response.json();

        if (!response.ok) {

            alert(data.message || "Unable to update quantity");

            return;
        }

        await loadCart();

    } catch (error) {

        console.error("Update Cart Error:", error);

        alert("Something went wrong!");

    }

}


// 4. REMOVE PRODUCT

async function removeCart(cartId) {

    if (!confirm("Are you sure you want to remove this product?")) {
        return;
    }

    try {

        const response = await fetch(`${API}/remove-cart`, {

            method: "POST",

            credentials: "include",

            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },

            body: new URLSearchParams({

                cartId: cartId

            })

        });

        const data = await response.json();

        if (!response.ok) {

            alert(data.message || "Unable to remove product");

            return;
        }

        await loadCart();

    } catch (error) {

        console.error("Remove Cart Error:", error);

        alert("Unable to remove product!");

    }

}


// 5. PROCEED TO CHECKOUT

 function goToCheckout() {

    if (cartItems.length === 0) {

        alert("Your cart is empty!");

        return;
    }

    window.location.href = "checkout.html";

}


// 6. HTML ESCAPE

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


// 7. START CART

loadCart();