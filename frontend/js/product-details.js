// SMARTCART - PRODUCT DETAILS

const API_URL = "http://localhost:8080/Shop";

const params = new URLSearchParams(window.location.search);

const selectedId = Number(params.get("id"));

const details = document.getElementById("productDetails");


// 1. LOAD SELECTED PRODUCT

async function loadProductDetails() {

    if (!selectedId || selectedId <= 0) {

        details.innerHTML = "<h2>Please select a product!</h2>";

        return;
    }

    details.innerHTML = "<h2>Loading product details...</h2>";

    try {

        const response = await fetch(`${API_URL}/products`, {
            credentials: "include"
        });

        if (!response.ok) {

            throw new Error("Unable to fetch products");

        }

        const products = await response.json();

        const product = products.find(
            p => Number(p.product_id) === selectedId
        );

        if (!product) {

            details.innerHTML = "<h2>Product not found!</h2>";

            return;
        }

        displayProduct(product);

    }

    catch (error) {

        console.error("Product Details Error:", error);

        details.innerHTML =
            "<h2>Unable to load product details. Please try again.</h2>";

    }

}


// 2. DISPLAY PRODUCT DETAILS

function displayProduct(product) {

    const imageHTML = product.image

        ? `<img src="${escapeHTML(product.image)}"
                alt="${escapeHTML(product.product_name)}"
                onerror="this.style.display='none'">`

        : `<div class="product-image-placeholder">
                Image Coming Soon
           </div>`;


    details.innerHTML = `

        <div class="product-card">

            ${imageHTML}

            <h1>${escapeHTML(product.product_name)}</h1>

            <h2>
                ₹${Number(product.price).toLocaleString("en-IN")}
            </h2>

            <p>
                ${escapeHTML(product.description || "No description available.")}
            </p>

            <p>
                Available Stock: ${Number(product.stock)}
            </p>

            <p>
                Quality fashion from your local dress shops.
            </p>

            <label for="productSize">
                Select Size:
            </label>

            <select id="productSize">

                <option value="">Select Size</option>

                <option value="S">S</option>

                <option value="M">M</option>

                <option value="L">L</option>

                <option value="XL">XL</option>

                <option value="XXL">XXL</option>

            </select>

            <br><br>

            <button
                id="addCartBtn"
                ${Number(product.stock) <= 0 ? "disabled" : ""}>

                Add To Cart

            </button>

            <button id="addWishlistBtn">

                Add To Wishlist

            </button>

            <br><br>

            <a href="products.html">
                Back to Products
            </a>

        </div>

    `;


    // ADD TO CART BUTTON

    document.getElementById("addCartBtn")
        .addEventListener("click", () => {

            addToCart(product);

        });


    // ADD TO WISHLIST BUTTON

    const wishlistBtn = document.getElementById("addWishlistBtn");

console.log("Wishlist button found:", wishlistBtn);

wishlistBtn.addEventListener("click", function () {
    console.log("Wishlist button clicked!");
    addToWishlist(product.product_id);
});

}


// 3. ADD TO CART - MYSQL DATABASE

async function addToCart(product) {

    const size = document.getElementById("productSize").value;

    if (!size) {

        alert("Please select your clothing size!");

        return;
    }

    if (Number(product.stock) <= 0) {

        alert("Product is out of stock!");

        return;
    }

    const button = document.getElementById("addCartBtn");

    button.disabled = true;

    button.textContent = "Adding...";

    try {

        const response = await fetch(`${API_URL}/add-to-cart`, {

            method: "POST",

            credentials: "include",

            headers: {

                "Content-Type": "application/x-www-form-urlencoded"

            },

            body: new URLSearchParams({

                productId: product.product_id,

                quantity: 1,

                size: size

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


        alert("Product added to cart successfully!");

        // OPTIONAL: Redirect to cart

        // window.location.href = "cart.html";


    }

    catch (error) {

        console.error("Add to Cart Error:", error);

        alert("Unable to connect to server!");

    }

    finally {

        button.disabled = Number(product.stock) <= 0;

        button.textContent = "Add To Cart";

    }

}


// 4. ADD TO WISHLIST

// ADD TO WISHLIST - MYSQL DATABASE

async function addToWishlist(id) {
    alert("ADD WISHLIST FUNCTION CALLED! Product ID: " + id);

    const button = document.getElementById("addWishlistBtn");

    button.disabled = true;
    button.textContent = "Adding...";

    try {
        console.log("Wishlist function started!");
console.log("Product ID:", id);
console.log("API URL:", `${API_URL}/wishlist`);

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

        console.log("Response Status:", response.status);
        console.log("Response Data:", data);

        if (response.status === 401) {
            alert("Please login first!");
            window.location.href = "login.html";
            return;
        }

        if (!response.ok) {
            alert(data.message || "Unable to add wishlist!");
            return;
        }

        alert(data.message || "Product added to Wishlist!");

    }

    catch (error) {

        console.error("Wishlist Error:", error);
        alert("Unable to connect to server!");

    }

    finally {

        button.disabled = false;
        button.textContent = "Add To Wishlist";

    }

}


// 5. HTML ESCAPE

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


// 6. START

loadProductDetails();