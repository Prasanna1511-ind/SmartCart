// SMARTCART - DYNAMIC PRODUCT LOADING

const API_URL = "http://localhost:8080/Shop";

const container = document.getElementById("productContainer");

let products = [];


// 1. FETCH PRODUCTS FROM MYSQL

async function loadProducts() {

    if (!container) return;

    container.innerHTML = "<p>Loading products...</p>";

    try {

        const response = await fetch(`${API_URL}/products`);

        if (!response.ok) {
            throw new Error("Failed to fetch products");
        }

        products = await response.json();

        if (!Array.isArray(products)) {
            throw new Error("Invalid product data");
        }

        displayProducts(products);

        filterProducts();

    } catch (error) {

        console.error("Product loading error:", error);

        container.innerHTML =
            "<p>Unable to load products. Please try again.</p>";

    }

}


// 2. DISPLAY PRODUCTS

function displayProducts(list) {

    if (!container) return;

    container.innerHTML = "";

    if (list.length === 0) {
        container.innerHTML = "<p>No products available.</p>";
        return;
    }

    list.forEach(product => {

        // Get image path from MySQL
        const imagePath = product.image_url || product.image;

        // Convert database path into frontend URL
        const imageSrc = imagePath
            ? (imagePath.startsWith("http") || imagePath.startsWith("/")
                ? imagePath
                : `/frontend/${imagePath.replace(/^\/+/, "")}`)
            : "";

        const imageHTML = imageSrc
            ? `<img
                    src="${escapeHTML(imageSrc)}"
                    alt="${escapeHTML(product.product_name)}"
                    class="product-image"
                    onerror="this.onerror=null; this.src='/frontend/assets/images/products/placeholder.jpg';"
               >`
            : `<div class="product-image-placeholder">
                    Image Coming Soon
               </div>`;

        container.innerHTML += `

            <div class="product-card">

                ${imageHTML}

                <h3>${escapeHTML(product.product_name)}</h3>

                <p>${escapeHTML(product.description || "")}</p>

                <h4>
                    ₹${Number(product.price).toLocaleString("en-IN")}
                </h4>

                <p>Stock: ${product.stock}</p>

                <button onclick="viewProduct(${Number(product.product_id)})">
                    View Details
                </button>

                <button onclick="addToWishlist(${Number(product.product_id)})">
                    ♡ Wishlist
                </button>

                <button onclick="addToCart(${Number(product.product_id)})">
                    🛒 Add to Cart
                </button>

            </div>

        `;

    });

}


// 3. VIEW PRODUCT DETAILS

function viewProduct(productId) {

    localStorage.setItem("selectedProduct", productId);

    window.location.href =
        `product-details.html?id=${productId}`;

}


// 4. ADD TO CART - MYSQL DATABASE

async function addToCart(productId) {

    const product = products.find(
        p => Number(p.product_id) === Number(productId)
    );

    if (!product) {

        alert("Product not found!");

        return;
    }

    if (Number(product.stock) <= 0) {

        alert("Product is out of stock!");

        return;
    }

    try {

        const response = await fetch(`${API_URL}/add-to-cart`, {

            method: "POST",

            credentials: "include",

            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },

            body: new URLSearchParams({

                productId: productId,

                quantity: 1,

                size: ""

            })

        });

        const data = await response.json();

        if (response.status === 401) {

            alert("Please login first!");

            window.location.href = "login.html";

            return;
        }

        if (!response.ok) {

            alert(data.message || "Unable to add product!");

            return;
        }

        alert("Product added to cart!");

    } catch (error) {

        console.error("Add to Cart Error:", error);

        alert("Unable to connect to the server!");

    }

}


// 5. ADD TO WISHLIST - MYSQL DATABASE

async function addToWishlist(id) {


    const product = products.find(
        p => Number(p.product_id) === Number(id)
    );

    if (!product) {
        alert("Product not found!");
        return;
    }

    try {

        const response = await fetch(`${API_URL}/wishlist`, {
            

            method: "POST",

            credentials: "include",

            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },

            body: new URLSearchParams({
                action: "add",
                productId: id
            })

        });

        const data = await response.json();

        if (response.status === 401) {

            alert("Please login first!");

            window.location.href = "login.html";

            return;
        }

        if (!response.ok) {

            alert(data.message || "Unable to add to Wishlist!");

            return;
        }

        alert(data.message || "Product added to Wishlist!");

    } catch (error) {

        console.error("Wishlist Error:", error);

        alert("Unable to connect to server!");

    }

}


// 6. SEARCH AND CATEGORY FILTER

function filterProducts() {

    const searchInput = document.getElementById("searchInput");

    const categoryFilter = document.getElementById("categoryFilter");

    const search = searchInput
        ? searchInput.value.toLowerCase().trim()
        : "";

    const category = categoryFilter
        ? categoryFilter.value
        : "all";

    const filtered = products.filter(product => {

        const matchesName =
            (product.product_name || "")
                .toLowerCase()
                .includes(search);

        const matchesCategory =
            category === "all" ||
            String(product.category_id) === category;

        return matchesName && matchesCategory;

    });

    displayProducts(filtered);

}


// 7. HTML ESCAPE

function escapeHTML(value) {

    return String(value ?? "").replace(/[&<>"']/g, function (character) {

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


// 8. INITIALIZE PRODUCTS PAGE

document.addEventListener("DOMContentLoaded", function () {

    if (container) {

        const searchInput =
            document.getElementById("searchInput");

        const categoryFilter =
            document.getElementById("categoryFilter");

        if (searchInput) {
            searchInput.addEventListener("input", filterProducts);
        }

        if (categoryFilter) {
            categoryFilter.addEventListener("change", filterProducts);
        }

        loadProducts();

    }

});