
// SMARTCART - MYSQL WISHLIST

const API = "http://localhost:8080/Shop";

const wishlistContainer =
    document.getElementById("wishlistContainer");

let products = [];

// 1. LOAD WISHLIST FROM MYSQL

async function loadWishlist() {

    try {

        const response = await fetch(`${API}/wishlist`, {
            method: "GET",
            credentials: "include"
        });

        if (response.status === 401) {
            alert("Please login first!");
            window.location.href = "login.html";
            return;
        }

        if (!response.ok) {
            throw new Error("Unable to fetch wishlist");
        }

        products = await response.json();

        displayWishlist();

    } catch (error) {

        console.error("Wishlist Error:", error);

        wishlistContainer.innerHTML =
            "<h3>Unable to load wishlist. Please try again.</h3>";
    }
}

// 2. DISPLAY WISHLIST

function displayWishlist() {

    wishlistContainer.innerHTML = "";

    if (products.length === 0) {

        wishlistContainer.innerHTML =
            "<h3>Your wishlist is empty!</h3>";

        return;
    }

    products.forEach(product => {

        const imageHTML = product.image
            ? `<img src="${escapeHTML(product.image)}"
                    alt="${escapeHTML(product.product_name)}"
                    onerror="this.style.display='none'">`
            : `<div class="product-image-placeholder">
                    Image Coming Soon
               </div>`;

        wishlistContainer.innerHTML += `

            <div class="product-card">

                ${imageHTML}

                <h3>${escapeHTML(product.product_name)}</h3>

                <p>₹${Number(product.price).toLocaleString("en-IN")}</p>

                <p>Stock: ${product.stock}</p>

                <button onclick="viewProduct(${product.product_id})">
                    View Details
                </button>

                <button onclick="moveToCart(${product.product_id})">
                    Add To Cart
                </button>

                <button onclick="removeWishlist(${product.product_id})">
                    Remove
                </button>

            </div>
        `;
    });
}

// 3. VIEW PRODUCT

function viewProduct(id) {

    window.location.href =
        `product-details.html?id=${id}`;
}

// 4. REMOVE WISHLIST PRODUCT FROM MYSQL

async function removeWishlist(id) {

    if (!confirm("Remove this product from wishlist?")) {
        return;
    }

    try {

        const response = await fetch(`${API}/wishlist`, {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type":
                    "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
    action: "remove",
    productId: id
})
        });

        const result = await response.json();

        if (!response.ok) {
            alert(result.message || "Unable to remove product");
            return;
        }

        alert("Product removed successfully!");

        await loadWishlist();

    } catch (error) {

        console.error("Remove Wishlist Error:", error);

        alert("Unable to remove product!");
    }
}

// 5. ADD TO CART

// Connect this function to your existing AddToCartServlet
// after checking its request parameter names and API URL.

async function moveToCart(id) {

    const product = products.find(
        p => Number(p.product_id) === Number(id)
    );

    if (!product) {
        alert("Product not found!");
        return;
    }

    if (Number(product.stock) <= 0) {
        alert("This product is out of stock!");
        return;
    }

    const size = prompt("Enter your size: S, M, L, XL or XXL");

    if (!size) return;

    const validSizes = ["S", "M", "L", "XL", "XXL"];

    if (!validSizes.includes(size.trim().toUpperCase())) {
        alert("Please enter a valid size!");
        return;
    }

    try {

        const response = await fetch(`${API}/add-to-cart`, {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            body: new URLSearchParams({
                productId: id,
                quantity: 1,
                size: size.trim().toUpperCase()
            })
        });

        const result = await response.json();

        if (response.status === 401) {
            alert("Please login first!");
            window.location.href = "login.html";
            return;
        }

        if (!response.ok) {
            alert(result.message || "Unable to add product to cart!");
            return;
        }

        alert("Product successfully added to Cart!");

    } catch (error) {

        console.error("Move To Cart Error:", error);
        alert("Unable to connect to server!");

    }
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

// 7. START

loadWishlist();