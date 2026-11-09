// ======================================
// SMARTCART - MAIN JAVASCRIPT
// ======================================


// 1. MOBILE MENU

function toggleMenu() {

    const nav = document.querySelector("nav");

    if (nav) {
        nav.classList.toggle("active");
    }

}


// 2. CART COUNT

function updateCartCount() {

    const cart = JSON.parse(localStorage.getItem("cart")) || [];

    const count = cart.reduce(
        (total, item) => total + item.quantity, 0
    );

    const cartCount = document.getElementById("cartCount");

    if (cartCount) {
        cartCount.textContent = count;
    }

}


// 3. WISHLIST COUNT

function updateWishlistCount() {

    const wishlist = JSON.parse(
        localStorage.getItem("wishlist")
    ) || [];

    const wishlistCount =
        document.getElementById("wishlistCount");

    if (wishlistCount) {
        wishlistCount.textContent = wishlist.length;
    }

}


// 4. LOGOUT FUNCTION

function logout() {

    localStorage.removeItem("user");

    alert("Logged out successfully!");

    window.location.href = "login.html";

}


// 5. SEARCH FUNCTION

function searchProducts() {

    const searchInput =
        document.getElementById("globalSearch");

    if (!searchInput) return;

    const searchValue = searchInput.value.trim();

    if (searchValue !== "") {

        localStorage.setItem("searchQuery", searchValue);

        window.location.href = "pages/products.html";

    }

}


// 6. WEBSITE INITIALIZATION

document.addEventListener("DOMContentLoaded", function() {

    updateCartCount();

    updateWishlistCount();

    console.log("SmartCart Application Loaded Successfully!");

});