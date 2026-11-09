// LOGIN

const loginForm = document.getElementById("loginForm");

if (loginForm) {
loginForm.addEventListener("submit", async function(event) {
event.preventDefault();


    const user = {
        email: document.getElementById("email").value.trim(),
        password: document.getElementById("password").value
    };

    try {
        const response = await fetch("http://localhost:8080/Shop/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/x-www-form-urlencoded"
            },
            credentials: "include",
            body: new URLSearchParams(user)
        });

        const result = await response.json();

        if (!response.ok) {
            alert(result.message);
            return;
        }

        // Check current session
        const sessionResponse = await fetch(
            "http://localhost:8080/Shop/check-session",
            {
                credentials: "include",
                cache: "no-store"
            }
        );

        const session = await sessionResponse.json();

        console.log("Login Session:", session);

        if (!session.loggedIn) {
            alert("Login session not found. Please try again.");
            return;
        }

        alert(result.message);

        if (session.role === "ADMIN") {
            window.location.replace(
                "http://localhost:8080/frontend/pages/admin-dashboard.html"
            );
        } else {
            window.location.replace(
                "http://localhost:8080/frontend/pages/customer-dashboard.html"
            );
        }

    } catch (error) {
        console.error("Login Error:", error);
        alert("Unable to connect to the server.");
    }
});


}

// ======================================
// REGISTER
// ======================================

const registerForm = document.getElementById("registerForm");

if (registerForm) {

    registerForm.addEventListener("submit", async function(event) {

        event.preventDefault();

        const user = {
            name: document.getElementById("name").value.trim(),
            email: document.getElementById("email").value.trim(),
            phone: document.getElementById("phone").value.trim(),
            password: document.getElementById("password").value
        };

        try {

            const response = await fetch(
                "http://localhost:8080/Shop/register",
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/x-www-form-urlencoded"
                    },

                    body: new URLSearchParams(user)
                }
            );

            const result = await response.json();

            if (!response.ok) {

                alert(result.message);
                return;

            }

            alert(result.message);

            window.location.href = "login.html";

        } catch (error) {

            console.error("Registration Error:", error);

            alert("Unable to connect to the server.");

        }

    });

}
