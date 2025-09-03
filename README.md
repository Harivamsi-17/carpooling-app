# 🚗 Real-Time Carpooling Platform

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green) ![WebSockets](https://img.shields.io/badge/WebSockets-real--time-blueviolet) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue) ![Docker](https://img.shields.io/badge/Docker-blue?logo=docker) ![JavaScript](https://img.shields.io/badge/JavaScript-ES6-yellow)

A complete full-stack carpooling application designed to manage ride-sharing logistics. The platform features a secure authentication system and leverages WebSockets to provide real-time emergency alerts to drivers, achieving notification times under 500ms.

**🔗 Live Demo:** [View the Live Application](https://carpooling-app-1-hq5d.onrender.com/)

---

## Key Features

* **Secure User Authentication:** Full registration and login functionality using Spring Security and JWTs for protected endpoints.
* **Ride Management (CRUD):** Authenticated users can create, view, update, and delete rides they are offering.
* **Booking System:** Users can search for available rides and book a seat, with the system managing seat availability.
* **Real-Time Emergency Alerts:** A critical safety feature using **Spring WebSockets** to instantly notify drivers of emergencies based on geolocation data.
* **Responsive UI:** A clean, mobile-first design that ensures a great user experience on any device.
* **Light/Dark Mode:** A theme toggle that saves the user's preference in their browser.

---

## Screenshots

**Authentication & User Profile (Light & Dark Mode)**
<img width="1919" height="927" alt="Screenshot 2025-09-03 144043" src="https://github.com/user-attachments/assets/7bcee4d1-0acf-44f2-b039-1254d85b6b33" />
<img width="1919" height="928" alt="Screenshot 2025-09-03 143923" src="https://github.com/user-attachments/assets/f3243948-baa4-46ae-918b-8d2adf36aca0" />

**Ride Management & Booking (Search, Create, and View Rides)**
<img width="1919" height="927" alt="Screenshot 2025-09-03 144415" src="https://github.com/user-attachments/assets/6000f311-9ce9-4674-b0ce-8e735a3c9b0c" />
<img width="1919" height="925" alt="Screenshot 2025-09-03 144230" src="https://github.com/user-attachments/assets/d313a8ac-5394-41ac-a898-53fc0b5fdda2" />
<img width="1919" height="927" alt="Screenshot 2025-09-03 144308" src="https://github.com/user-attachments/assets/9bcf3aac-42a6-4829-a362-aaeb29951824" />

---

## Tech Stack

#### Backend
* **Java 21**
* **Spring Boot:** Spring Web, Spring Security, Spring Data JPA, **Spring WebSockets**
* **Authentication:** JSON Web Tokens (JWT)
* **Database:** PostgreSQL with Hibernate (modeling complex many-to-one and one-to-many relationships)

#### Frontend
* **Vanilla JavaScript (ES6+):** DOM Manipulation, Fetch API for REST calls, WebSocket client
* **HTML5**
* **CSS3:** Flexbox, Grid, Media Queries for responsiveness

#### DevOps
* **Docker:** The application is fully containerized with a multi-stage build for a lean final image.
* **Render.com:** Deployed as a web service with a PostgreSQL database instance.
* **Git & GitHub:** For version control and CI/CD with GitHub Actions (auto-deployment on push).

---

## How to Run Locally

1.  **Clone the repository:**
    ```bash
    git clone https://github.com/Harivamsi-17/carpooling-app.git
    ```
2.  **Backend Setup:**
    * Make sure you have Java 21 and PostgreSQL installed.
    * Create a local PostgreSQL database.
    * In `src/main/resources/application.properties`, update the `spring.datasource` properties to point to your local database.
    * Set your JWT secret in the same file.
    * Run the Spring Boot application from your IDE.
3.  **Frontend Setup:**
    * In the main JavaScript file (e.g., `src/main/resources/static/js/app.js`), change the `API_BASE_URL` to your local backend address (e.g., `http://localhost:8080`).
    * Open the `index.html` file in your browser.

---

## Author

* **HARI VAMSI NAGUBILLI** - [My LinkedIn Profile](https://www.linkedin.com/in/harivamsi1707/)
* Licensed under the MIT License.
