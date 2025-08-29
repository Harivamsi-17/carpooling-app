document.addEventListener('DOMContentLoaded', () => {
    // --- GLOBAL STATE ---
    const API_BASE_URL = 'http://localhost:8080/api';
    let currentUserRole = null;

    // --- ELEMENT SELECTORS ---
    const authPage = document.getElementById('auth-page');
    const appContent = document.getElementById('app-content');
    const welcomeMessage = document.getElementById('welcome-message');
    const themeToggleButton = document.getElementById('theme-toggle-btn');

    // --- HELPER FUNCTIONS ---
    const switchView = (pageId) => {
        document.querySelectorAll('.page').forEach(page => page.classList.add('hidden'));
        const pageToShow = document.getElementById(pageId);
        if (pageToShow) {
            pageToShow.classList.remove('hidden');
        }
    };

    const showAppView = () => {
        authPage.classList.add('hidden');
        appContent.classList.remove('hidden');
        const token = localStorage.getItem('token');
        const fullName = localStorage.getItem('fullName');
        if (token && fullName) {
            welcomeMessage.textContent = `Welcome, ${fullName}!`;
            try {
                const payload = JSON.parse(atob(token.split('.')[1]));
                currentUserRole = payload.role;
                document.querySelectorAll('.driver-only').forEach(el => {
                    el.style.display = currentUserRole === 'DRIVER' ? 'flex' : 'none';
                });
                switchView('search-rides-page');
            } catch (e) {
                logout();
            }
        } else {
            logout();
        }
    };

    const logout = () => {
        localStorage.clear();
        appContent.classList.add('hidden');
        authPage.classList.remove('hidden');
    };

    const fetchWithAuth = (url, options = {}) => {
        const token = localStorage.getItem('token');
        const headers = { ...options.headers, 'Content-Type': 'application/json' };
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }
        return fetch(url, { ...options, headers });
    };

    const showToast = (message, type = 'success') => {
        const container = document.getElementById('toast-container');
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.textContent = message;
        container.appendChild(toast);
        setTimeout(() => toast.classList.add('show'), 100);
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => {
                if (container.contains(toast)) container.removeChild(toast);
            }, 500);
        }, 3000);
    };

    // --- API & FORM LOGIC ---
    const handleRegister = async (event) => {
        event.preventDefault();
        const data = {
            fullName: document.getElementById('register-fullName').value,
            email: document.getElementById('register-email').value,
            password: document.getElementById('register-password').value,
            phoneNumber: document.getElementById('register-phoneNumber').value,
            role: document.getElementById('register-role').value
        };
        try {
            const response = await fetch(`${API_BASE_URL}/users/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            if (response.ok) {
                showToast('Registration successful! Please log in.', 'success');
                document.getElementById('register-form').reset();
                document.getElementById('show-login-link').click();
            } else {
                const error = await response.json();
                showToast(`Registration failed: ${error.message}`, 'error');
            }
        } catch (error) {
            showToast('A network error occurred.', 'error');
        }
    };

    const handleLogin = async (event) => {
        event.preventDefault();
        const data = {
            email: document.getElementById('login-email').value,
            password: document.getElementById('login-password').value
        };
        try {
            const response = await fetch(`${API_BASE_URL}/users/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data)
            });
            if (response.ok) {
                const authResponse = await response.json();
                localStorage.setItem('token', authResponse.token);
                localStorage.setItem('fullName', authResponse.fullName);
                showAppView();
            } else {
                showToast('Login failed. Check credentials.', 'error');
            }
        } catch (error) {
            showToast('A network error occurred.', 'error');
        }
    };

    const handlePostRide = async (event) => {
        event.preventDefault();
        const rideData = {
            origin: document.getElementById('ride-origin').value,
            destination: document.getElementById('ride-destination').value,
            departureTime: document.getElementById('ride-departureTime').value,
            availableSeats: document.getElementById('ride-availableSeats').value,
            price: document.getElementById('ride-price').value
        };
        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/rides`, {
                method: 'POST',
                body: JSON.stringify(rideData)
            });
            if (response.ok) {
                showToast('Ride posted successfully!', 'success');
                event.target.reset();
                fetchMyRides();
            } else {
                const error = await response.json();
                showToast(`Error: ${error.message}`, 'error');
            }
        } catch (error) {
            showToast('A network error occurred.', 'error');
        }
    };

    // Replace the handleSearchRide function with this
    const handleSearchRide = async (event) => {
        event.preventDefault();
        const origin = document.getElementById('search-origin').value;
        const destination = document.getElementById('search-destination').value;
        const resultsContainer = document.getElementById('ride-results');
        resultsContainer.innerHTML = '<h3>Searching...</h3>';
        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/rides/search?origin=${origin}&destination=${destination}`);
            if (response.ok) {
                const rides = await response.json();
                resultsContainer.innerHTML = '';
                if (rides.length === 0) {
                    resultsContainer.innerHTML = '<h3>No rides found.</h3>';
                } else {
                    rides.forEach(ride => {
                        const rideEl = document.createElement('div');
                        rideEl.className = 'card';
                        rideEl.innerHTML = `
                        <div class="card-content">
                            <p><strong>From:</strong> ${ride.origin} to ${ride.destination}</p>
                            <p><strong>Driver:</strong> ${ride.driverFullName}</p>
                            <p><strong>Seats:</strong> ${ride.availableSeats}</p>
                            <p><strong>Departure:</strong> ${new Date(ride.departureTime).toLocaleString()}</p>
                            <p><strong>Price per Seat:</strong> ${ride.currentPricePerRider ? '$' + ride.currentPricePerRider.toFixed(2) : 'Not Set'}</p>
                        </div>
                        <div class="card-actions">
                            <button class="book-btn" data-ride-id="${ride.id}">Book Now</button>
                        </div>`;
                        resultsContainer.appendChild(rideEl);
                    });
                }
            } else {
                resultsContainer.innerHTML = '<h3>Could not perform search.</h3>';
            }
        } catch (error) {
            console.error("Error in handleSearchRide:", error);
            resultsContainer.innerHTML = '<h3>A network error occurred.</h3>';
            showToast('Failed to search for rides.', 'error');
        }
    };

    // Replace the fetchMyRides function with this
    const fetchMyRides = async () => {
        switchView('my-rides-page');
        const listContainer = document.getElementById('my-rides-list');
        listContainer.innerHTML = '<p>Loading your rides...</p>';
        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/rides/my-rides`);
            if (response.ok) {
                const rides = await response.json();
                listContainer.innerHTML = '';
                if (rides.length === 0) {
                    listContainer.innerHTML = '<p>You have not posted any rides.</p>';
                } else {
                    rides.forEach(ride => {
                        const rideEl = document.createElement('div');
                        rideEl.className = 'card';
                        rideEl.innerHTML = `
                        <div class="card-content">
                            <p><strong>From:</strong> ${ride.origin} to ${ride.destination}</p>
                            <p><strong>Seats Left:</strong> ${ride.availableSeats}</p>
                            <p><strong>Departure:</strong> ${new Date(ride.departureTime).toLocaleString()}</p>
                            <p><strong>Total Price Set:</strong> ${ride.totalPrice ? '$' + ride.totalPrice.toFixed(2) : 'Not Set'}</p>
                        </div>
                        <div class="card-actions">
                            <button class="view-bookings-btn" data-ride-id="${ride.id}">View Bookings</button>
                        </div>
                        <div class="bookings-list"></div>`;
                        listContainer.appendChild(rideEl);
                    });
                }
            } else {
                showToast("Failed to fetch your rides.", 'error');
            }
        } catch (error) {
            console.error("Error in fetchMyRides:", error);
            showToast("Failed to display your rides.", 'error');
        }
    };

    const fetchMyBookings = async () => {
        switchView('my-bookings-page');
        const listContainer = document.getElementById('my-bookings-list');
        listContainer.innerHTML = '<p>Loading your bookings...</p>';
        try {
            const response = await fetchWithAuth(`${API_BASE_URL}/bookings/my-bookings`);
            if (response.ok) {
                const bookings = await response.json();
                listContainer.innerHTML = '';
                if (bookings.length === 0) {
                    listContainer.innerHTML = '<p>You have not booked any rides.</p>';
                } else {
                    bookings.forEach(booking => {
                        const bookingEl = document.createElement('div');
                        bookingEl.className = 'card';

                        let actionsHtml = '';
                        if (booking.status === 'PENDING' || booking.status === 'CONFIRMED') {
                            actionsHtml = `
                            <div class="card-actions">
                                <button class="cancel-booking-rider-btn" data-booking-id="${booking.bookingId}">Cancel Booking</button>
                            </div>
                        `;
                        } else if (booking.status === 'CANCELLED') {
                            actionsHtml = `
                            <div class="card-actions">
                                <button class="remove-booking-btn" data-booking-id="${booking.bookingId}">Remove</button>
                            </div>
                        `;
                        }

                        bookingEl.innerHTML = `
                        <div class="card-content">
                            <p><strong>Ride:</strong> ${booking.origin} to ${booking.destination}</p>
                            <p><strong>Driver:</strong> ${booking.driverName}</p>
                            <p><strong>Status:</strong> <span class="status-${booking.status.toLowerCase()}">${booking.status}</span></p>
                            <p><strong>Seats Remaining:</strong> ${booking.availableSeats}</p>
                            <p><strong>Current Price For You:</strong> $${booking.currentPricePerRider.toFixed(2)}</p>
                        </div>
                        ${actionsHtml}`;
                        listContainer.appendChild(bookingEl);
                    });
                }
            } else {
                listContainer.innerHTML = '<p>Could not load your bookings. Please try again later.</p>';
            }
        } catch (error) {
            console.error("Fetch error:", error);
            listContainer.innerHTML = '<p>A network error occurred.</p>';
        }
    };

    // --- EVENT DELEGATION for dynamic content ---
    document.body.addEventListener('click', async (event) => {
        if (event.target.classList.contains('book-btn')) {
            const rideId = event.target.dataset.rideId;
            try {
                const response = await fetchWithAuth(`${API_BASE_URL}/rides/${rideId}/book`, { method: 'POST' });
                if (response.ok) {
                    showToast(`Booking request sent!`, 'success');
                } else {
                    const error = await response.json();
                    showToast(`Failed to book: ${error.message}`, 'error');
                }
            } catch (error) {
                showToast("A network error occurred.", 'error');
            }
        }

        if (event.target.classList.contains('view-bookings-btn')) {
            const button = event.target;
            const card = button.closest('.card');
            const bookingsContainer = card.querySelector('.bookings-list');
            if (bookingsContainer.innerHTML) {
                bookingsContainer.innerHTML = '';
                return;
            }
            bookingsContainer.innerHTML = '<p>Loading...</p>';
            try {
                const rideId = button.dataset.rideId;
                const response = await fetchWithAuth(`${API_BASE_URL}/bookings/ride/${rideId}`);
                if (response.ok) {
                    const bookings = await response.json();
                    bookingsContainer.innerHTML = '';
                    if (bookings.length === 0) {
                        bookingsContainer.innerHTML = '<p>No bookings yet.</p>';
                    } else {
                        bookings.forEach(booking => {
                            const bookingEl = document.createElement('div');
                            bookingEl.className = 'booking-item';
                            bookingEl.innerHTML = `
                                <span>Rider: ${booking.rider.fullName}</span>
                                <span class="status-${booking.status.toLowerCase()}">Status: ${booking.status}</span>
                                <div class="booking-actions">
                                    ${booking.status === 'PENDING' ? `<button class="confirm-btn" data-booking-id="${booking.id}">Confirm</button>` : ''}
                                    ${booking.status !== 'PENDING' ? `<button class="cancel-btn" data-booking-id="${booking.id}">Cancel</button>` : ''}
                                </div>`;
                            bookingsContainer.appendChild(bookingEl);
                        });
                    }
                } else {
                    bookingsContainer.innerHTML = '<p>Error loading bookings.</p>';
                }
            } catch (error) {
                bookingsContainer.innerHTML = '<p>A network error occurred.</p>';
            }
        }

        if (event.target.classList.contains('confirm-btn') || event.target.classList.contains('cancel-btn')) {
            const isConfirm = event.target.classList.contains('confirm-btn');
            const bookingId = event.target.dataset.bookingId;
            const action = isConfirm ? 'confirm' : 'cancel';
            try {
                const response = await fetchWithAuth(`${API_BASE_URL}/bookings/${bookingId}/${action}`, { method: 'POST' });
                if (response.ok) {
                    showToast(`Booking ${action}ed!`, 'success');
                    fetchMyRides();
                } else {
                    const error = await response.json();
                    showToast(`Failed to ${action}: ${error.message}`, 'error');
                }
            } catch (error) {
                showToast("A network error occurred.", 'error');
            }
        }

        if (event.target.classList.contains('cancel-booking-rider-btn')) {
            const bookingId = event.target.dataset.bookingId;
            if (!confirm("Are you sure you want to cancel this booking?")) {
                return;
            }
            try {
                const response = await fetchWithAuth(`${API_BASE_URL}/bookings/${bookingId}/cancel-by-rider`, {
                    method: 'POST'
                });
                if (response.ok) {
                    showToast('Booking cancelled successfully!', 'success');
                    fetchMyBookings();
                } else {
                    const error = await response.json();
                    showToast(`Cancellation failed: ${error.message}`, 'error');
                }
            } catch (error) {
                showToast('A network error occurred during cancellation.', 'error');
            }
        }

        if (event.target.classList.contains('remove-booking-btn')) {
            const bookingId = event.target.dataset.bookingId;
            if (!confirm("Are you sure you want to permanently remove this booking record?")) {
                return;
            }
            try {
                const response = await fetchWithAuth(`${API_BASE_URL}/bookings/${bookingId}`, {
                    method: 'DELETE'
                });
                if (response.ok) {
                    showToast('Booking record removed.', 'success');
                    fetchMyBookings(); // Refresh the list
                } else {
                    showToast('Failed to remove booking.', 'error');
                }
            } catch (error) {
                showToast('A network error occurred while removing the booking.', 'error');
            }
        }
    });

    // --- STATIC EVENT LISTENERS ---
    document.getElementById('register-form').addEventListener('submit', handleRegister);
    document.getElementById('login-form').addEventListener('submit', handleLogin);
    document.getElementById('post-ride-form').addEventListener('submit', handlePostRide);
    document.getElementById('search-ride-form').addEventListener('submit', handleSearchRide);
    document.getElementById('nav-logout').addEventListener('click', logout);
    document.getElementById('show-register-link').addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('login-container').classList.add('hidden');
        document.getElementById('register-container').classList.remove('hidden');
    });
    document.getElementById('show-login-link').addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('register-container').classList.add('hidden');
        document.getElementById('login-container').classList.remove('hidden');
    });
    document.getElementById('nav-post-ride').addEventListener('click', (e) => { e.preventDefault(); switchView('post-ride-page'); });
    document.getElementById('nav-search-rides').addEventListener('click', (e) => { e.preventDefault(); switchView('search-rides-page'); });
    document.getElementById('nav-my-rides').addEventListener('click', (e) => { e.preventDefault(); fetchMyRides(); });
    document.getElementById('nav-my-bookings').addEventListener('click', (e) => { e.preventDefault(); fetchMyBookings(); });

    themeToggleButton.addEventListener('click', () => {
        document.body.classList.toggle('dark-mode');
        const isDarkMode = document.body.classList.contains('dark-mode');
        localStorage.setItem('theme', isDarkMode ? 'dark' : 'light');
        themeToggleButton.innerHTML = isDarkMode ? '&#9790;' : '&#9728;';
    });

    // --- INITIALIZATION ---
    if (localStorage.getItem('theme') === 'dark') {
        document.body.classList.add('dark-mode');
        themeToggleButton.innerHTML = '&#9790;';
    }
    const token = localStorage.getItem('token');
    if (token) {
        showAppView();
    }
});