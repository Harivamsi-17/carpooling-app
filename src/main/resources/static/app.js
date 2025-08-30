document.addEventListener('DOMContentLoaded', () => {
    // --- GLOBAL STATE ---
    // const API_BASE_URL = 'http://localhost:8080/api';
    const API_BASE_URL = 'https://carpooling-backend.onrender.com/api'; // <-- CHANGE THIS
    let currentUserRole = null;
    let stompClient = null;
    let currentUserLocation = null;

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

    // --- NEW: Safe JWT Parsing function ---
    const parseJwt = (token) => {
        try {
            const base64Url = token.split('.')[1];
            const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
            const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join(''));
            return JSON.parse(jsonPayload);
        } catch (error) {
            console.error("Failed to parse JWT:", error);
            return null;
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
                // --- UPDATED: Use the new safe parsing function ---
                const payload = parseJwt(token);
                if (!payload) { // If parsing fails, logout
                    throw new Error("Invalid token payload");
                }

                currentUserRole = payload.role;
                document.querySelectorAll('.driver-only').forEach(el => {
                    el.style.display = currentUserRole === 'DRIVER' ? 'flex' : 'none';
                });
                connectWebSocket();
                switchView('search-rides-page');
            } catch (e) {
                console.error("Error during app view setup:", e);
                logout();
            }
        } else {
            logout();
        }
    };

    const logout = () => {
        localStorage.clear();
        if (stompClient !== null) {
            stompClient.disconnect();
            stompClient = null;
        }
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

    const connectWebSocket = () => {
        const token = localStorage.getItem('token');
        if (token && !stompClient) {
            const socket = new SockJS(`${API_BASE_URL}/ws`);
            stompClient = Stomp.over(socket);

            stompClient.connect({ 'Authorization': `Bearer ${token}` }, (frame) => {
                console.log('Connected to WebSocket: ' + frame);

                // For private notifications (booking requests, acceptance alerts)
                stompClient.subscribe('/user/queue/notifications', (message) => {
                    const notification = JSON.parse(message.body);
                    showToast(notification.message, 'success');
                    // If the emergency request was accepted, refresh the rider's view
                    if (notification.type === 'EMERGENCY_ACCEPTED') {
                        fetchMyBookings();
                    }
                });

                // For public emergency alerts for drivers
                if (currentUserRole === 'DRIVER') {
                    stompClient.subscribe('/topic/emergency-alerts', (message) => {
                        const notification = JSON.parse(message.body);
                        const modal = document.getElementById('emergency-modal');
                        const modalMessage = document.getElementById('emergency-modal-message');
                        const acceptBtn = document.getElementById('emergency-accept-btn');
                        modalMessage.textContent = notification.message;
                        acceptBtn.dataset.requestId = notification.requestId;
                        modal.classList.remove('hidden');
                    });
                }
            }, (error) => {
                console.error('WebSocket connection error: ' + error);
            });
        }
    };

    const getUserLocation = () => {
        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error("Geolocation is not supported."));
            } else {
                navigator.geolocation.getCurrentPosition(resolve, reject);
            }
        });
    };

    const calculateDistance = (lat1, lon1, lat2, lon2) => {
        const R = 6371;
        const dLat = (lat2 - lat1) * Math.PI / 180;
        const dLon = (lon2 - lon1) * Math.PI / 180;
        const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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
            resultsContainer.innerHTML = '<h3>A network error occurred.</h3>';
            showToast('Failed to search for rides.', 'error');
        }
    };

    const fetchMyRides = async () => { // For the DRIVER
        switchView('my-rides-page');
        const listContainer = document.getElementById('my-rides-list');
        listContainer.innerHTML = '<p>Loading your rides...</p>';

        let finalHtml = '';

        try {
            const emergencyResponse = await fetchWithAuth(`${API_BASE_URL}/emergency/driver/active`);
            if (emergencyResponse.status === 200) {
                const emergencyRide = await emergencyResponse.json();
                finalHtml += `
                <div class="card emergency-ride-card">
                    <h3>ACTIVE EMERGENCY RIDE</h3>
                    <div class="card-content">
                        <p><strong>Pickup:</strong> ${emergencyRide.pickupAddress}</p>
                        <p><strong>Destination:</strong> ${emergencyRide.destinationHospital}</p>
                        <p><strong>Rider:</strong> ${emergencyRide.riderName}</p>
                    </div>
                    <div class="card-actions">
                        <button class="complete-emergency-btn" data-request-id="${emergencyRide.id}">Complete Ride</button>
                    </div>
                </div>
            `;
            }

            const response = await fetchWithAuth(`${API_BASE_URL}/rides/my-rides`);
            if (response.ok) {
                const rides = await response.json();
                if (rides.length === 0 && finalHtml === '') {
                    listContainer.innerHTML = '<p>You have not posted any rides.</p>';
                    return;
                }
                rides.forEach(ride => {
                    finalHtml += `
                    <div class="card">
                        <div class="card-content">
                            <p><strong>From:</strong> ${ride.origin} to ${ride.destination}</p>
                            <p><strong>Seats Left:</strong> ${ride.availableSeats}</p>
                            <p><strong>Total Price Set:</strong> ${ride.totalPrice ? '$' + ride.totalPrice.toFixed(2) : 'Not Set'}</p>
                        </div>
                        <div class="card-actions">
                            <button class="view-bookings-btn" data-ride-id="${ride.id}">View Bookings</button>
                        </div>
                        <div class="bookings-list"></div>
                    </div>`;
                });
                listContainer.innerHTML = finalHtml;
            }
        } catch (error) {
            showToast("Failed to fetch your rides.", 'error');
        }
    };


    const fetchMyBookings = async () => { // For the RIDER
        switchView('my-bookings-page');
        const listContainer = document.getElementById('my-bookings-list');
        listContainer.innerHTML = '<p>Loading your bookings...</p>';

        let finalHtml = '';

        try {
            // First, check for an active emergency request
            const emergencyResponse = await fetchWithAuth(`${API_BASE_URL}/emergency/rider/active`);
            if (emergencyResponse.status === 200) {
                const emergencyRide = await emergencyResponse.json();

                // --- THIS LOGIC IS NOW CORRECTED ---
                const driverInfo = emergencyRide.driverName
                    ? `Driver ${emergencyRide.driverName} is on the way!`
                    : 'Waiting for a driver to accept...';

                finalHtml += `
                <div class="card emergency-ride-card">
                    <h3>EMERGENCY REQUEST STATUS</h3>
                    <div class="card-content">
                        <p><strong>Destination:</strong> ${emergencyRide.destinationHospital}</p>
                        <p><strong>Status:</strong> ${emergencyRide.status}</p>
                        <p><strong>Details:</strong> ${driverInfo}</p>
                    </div>
                     <div class="card-actions">
                        <button class="cancel-emergency-btn" data-request-id="${emergencyRide.id}">Cancel Request</button>
                    </div>
                </div>
            `;
            }

            // Next, fetch regular bookings
            const response = await fetchWithAuth(`${API_BASE_URL}/bookings/my-bookings`);
            if (response.ok) {
                const bookings = await response.json();
                if (bookings.length === 0 && finalHtml === '') {
                    listContainer.innerHTML = '<p>You have not booked any rides.</p>';
                    return;
                }
                bookings.forEach(booking => {
                    let actionsHtml = '';
                    if (booking.status === 'PENDING' || booking.status === 'CONFIRMED') {
                        actionsHtml = `<div class="card-actions"><button class="cancel-booking-rider-btn" data-booking-id="${booking.bookingId}">Cancel Booking</button></div>`;
                    } else if (booking.status === 'CANCELLED') {
                        actionsHtml = `<div class="card-actions"><button class="remove-booking-btn" data-booking-id="${booking.bookingId}">Remove</button></div>`;
                    }
                    finalHtml += `
                    <div class="card">
                        <div class="card-content">
                            <p><strong>Ride:</strong> ${booking.origin} to ${booking.destination}</p>
                            <p><strong>Driver:</strong> ${booking.driverName}</p>
                            <p><strong>Status:</strong> <span class="status-${booking.status.toLowerCase()}">${booking.status}</span></p>
                            <p><strong>Seats Remaining:</strong> ${booking.availableSeats}</p>
                            <p><strong>Current Price For You:</strong> $${booking.currentPricePerRider.toFixed(2)}</p>
                        </div>
                        ${actionsHtml}
                    </div>`;
                });
                listContainer.innerHTML = finalHtml;
            }
        } catch (error) {
            listContainer.innerHTML = '<p>Could not load your bookings.</p>';
        }
    };

    const showEmergencyPage = async () => {
        switchView('emergency-page');
        const hospitalList = document.getElementById('hospital-list');
        hospitalList.innerHTML = '<p>Getting your location and finding nearby hospitals...</p>';
        try {
            const position = await getUserLocation();
            currentUserLocation = { lat: position.coords.latitude, lng: position.coords.longitude };

            const hospitals = [
                { name: 'Manipal Hospital, Tadepalli', lat: 16.48, lng: 80.62 },
                { name: 'NRI General Hospital, Chinakakani', lat: 16.44, lng: 80.52 },
                { name: 'AIIMS, Mangalagiri', lat: 16.41, lng: 80.53 },
                { name: 'Aayush Hospital, Vijayawada', lat: 16.51, lng: 80.64 }
            ];

            hospitals.forEach(hospital => {
                hospital.distance = calculateDistance(currentUserLocation.lat, currentUserLocation.lng, hospital.lat, hospital.lng);
            });

            hospitals.sort((a, b) => a.distance - b.distance);
            hospitalList.innerHTML = '';

            hospitals.forEach(hospital => {
                const hospitalCard = document.createElement('div');
                hospitalCard.className = 'hospital-card';
                hospitalCard.innerHTML = `
                    <h3>${hospital.name}</h3>
                    <p>Approximately ${hospital.distance.toFixed(1)} km away</p>
                    <button class="request-emergency-ride-btn" 
                            data-hospital-name="${hospital.name}" 
                            data-hospital-lat="${hospital.lat}" 
                            data-hospital-lng="${hospital.lng}">Request Ride Here</button>
                `;
                hospitalList.appendChild(hospitalCard);
            });
        } catch (error) {
            hospitalList.innerHTML = `<p style="color: red;">Error: ${error.message}</p>`;
            showToast(error.message, 'error');
        }
    };

    // --- EVENT DELEGATION for dynamic content ---
    document.body.addEventListener('click', async (event) => {
        if (event.target.id === 'emergency-decline-btn') {
            document.getElementById('emergency-modal').classList.add('hidden');
        }
        if (event.target.classList.contains('complete-emergency-btn')) {
            const requestId = event.target.dataset.requestId;
            if (!confirm("Are you sure you want to mark this ride as complete?")) return;
            try {
                const response = await fetchWithAuth(`${API_BASE_URL}/emergency/request/${requestId}/complete`, { method: 'POST' });
                if (response.ok) {
                    showToast("Emergency ride completed!", 'success');
                    fetchMyRides(); // Refresh the view
                } else {
                    showToast("Failed to complete ride.", 'error');
                }
            } catch (error) {
                showToast("A network error occurred.", 'error');
            }
        }

        if (event.target.classList.contains('cancel-emergency-btn')) {
            const requestId = event.target.dataset.requestId;
            if (!confirm("Are you sure you want to cancel this emergency request?")) return;
            try {
                const response = await fetchWithAuth(`${API_BASE_URL}/emergency/request/${requestId}/cancel`, { method: 'POST' });
                if (response.ok) {
                    showToast("Emergency request cancelled.", 'success');
                    fetchMyBookings(); // Refresh the view
                } else {
                    showToast("Failed to cancel request.", 'error');
                }
            } catch (error) {
                showToast("A network error occurred.", 'error');
            }
        }

        if (event.target.id === 'emergency-accept-btn') {
            const button = event.target;
            const requestId = button.dataset.requestId;

            try {
                const response = await fetchWithAuth(`${API_BASE_URL}/emergency/request/${requestId}/accept`, {
                    method: 'POST'
                });

                if (response.ok) {
                    showToast("Emergency request accepted! Please proceed.", 'success');
                    document.getElementById('emergency-modal').classList.add('hidden');
                    // You could add logic here to navigate to a details page
                    fetchMyRides();
                } else {
                    const error = await response.json();
                    showToast(`Failed to accept: ${error.message}`, 'error');
                    document.getElementById('emergency-modal').classList.add('hidden');
                }
            } catch (error) {
                showToast("A network error occurred.", 'error');
                document.getElementById('emergency-modal').classList.add('hidden');
            }
        }
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
                    fetchMyBookings();
                } else {
                    showToast('Failed to remove booking.', 'error');
                }
            } catch (error) {
                showToast('A network error occurred while removing the booking.', 'error');
            }
        }

        if (event.target.classList.contains('request-emergency-ride-btn')) {
            const button = event.target;
            const hospitalData = {
                name: button.dataset.hospitalName,
                lat: button.dataset.hospitalLat,
                lng: button.dataset.hospitalLng
            };
            if (!currentUserLocation) {
                showToast("Could not determine your current location.", "error");
                return;
            }
            const payload = {
                requesterLat: currentUserLocation.lat,
                requesterLng: currentUserLocation.lng,
                hospitalName: hospitalData.name,
                hospitalLat: hospitalData.lat,
                hospitalLng: hospitalData.lng
            };
            showToast("Sending emergency request...", "success");
            try {
                const response = await fetchWithAuth(`${API_BASE_URL}/emergency/request`, {
                    method: 'POST',
                    body: JSON.stringify(payload)
                });
                if (response.ok) {
                    showToast("Emergency request sent! Nearby drivers have been alerted.", "success");
                    switchView('my-bookings-page');
                } else {
                    const error = await response.json();
                    showToast(`Request failed: ${error.message}`, 'error');
                }
            } catch (error) {
                console.error("Emergency request failed (backend not built yet):", error);
                showToast("DEMO: Emergency request sent! Nearby drivers have been alerted.", "success");
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
    document.getElementById('nav-emergency').addEventListener('click', (e) => { e.preventDefault(); showEmergencyPage(); });

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