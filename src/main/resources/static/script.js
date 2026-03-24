async function apiRequest(url, options = {}) {
    const response = await fetch(url, options);

    if (!response.ok) {
        let message = "Произошла ошибка";
        try {
            const errorData = await response.json();
            message = errorData.message || errorData.error || JSON.stringify(errorData);
        } catch (e) {
            const errorText = await response.text();
            if (errorText) {
                message = errorText;
            }
        }
        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function showMessage(elementId, text, type = "") {
    const box = document.getElementById(elementId);
    if (!box) return;

    box.textContent = text;
    box.className = "message-box";
    if (type) {
        box.classList.add(type);
    }
}

function normalizeDateTime(value) {
    if (!value) return value;
    return value.length === 16 ? value + ":00" : value;
}

function formatDateTime(value) {
    if (!value) return "-";
    return value.replace("T", " ");
}

async function loadDashboard() {
    const resources = await apiRequest("/api/resources");
    const bookings = await apiRequest("/api/bookings");

    const activeResources = resources.filter(r => r.active).length;
    const confirmed = bookings.filter(b => b.status === "CONFIRMED").length;

    document.getElementById("statResources").textContent = resources.length;
    document.getElementById("statActiveResources").textContent = activeResources;
    document.getElementById("statBookings").textContent = bookings.length;
    document.getElementById("statConfirmed").textContent = confirmed;
}

async function loadResourcesPage() {
    try {
        const resources = await apiRequest("/api/resources");
        const container = document.getElementById("resourceCards");
        if (!container) return;

        container.innerHTML = "";

        if (resources.length === 0) {
            container.innerHTML = `<div class="resource-card"><h3>Ресурсы отсутствуют</h3><p>Пока не добавлено ни одного ресурса.</p></div>`;
            return;
        }

        resources.forEach(resource => {
            const card = document.createElement("div");
            card.className = "resource-card";
            card.innerHTML = `
                <h3>${resource.name}</h3>
                <p><strong>ID:</strong> ${resource.id}</p>
                <p><strong>Тип:</strong> ${resource.type}</p>
                <p><strong>Местоположение:</strong> ${resource.location || "-"}</p>
                <p><strong>Описание:</strong> ${resource.description || "-"}</p>
                <span class="badge ${resource.active ? "confirmed" : "cancelled"}">
                    ${resource.active ? "Активен" : "Неактивен"}
                </span>
            `;
            container.appendChild(card);
        });
    } catch (error) {
        showMessage("resourcesMessage", error.message, "error");
    }
}

async function fillResourceSelectAndPreview() {
    try {
        const resources = await apiRequest("/api/resources");

        const select = document.getElementById("bookingResourceId");
        const preview = document.getElementById("resourcePreview");

        if (select) {
            select.innerHTML = `<option value="">Выберите ресурс</option>`;
            resources.forEach(resource => {
                const option = document.createElement("option");
                option.value = resource.id;
                option.textContent = `${resource.id} — ${resource.name} (${resource.type})`;
                select.appendChild(option);
            });
        }

        if (preview) {
            preview.innerHTML = "";
            resources.forEach(resource => {
                const item = document.createElement("div");
                item.className = "mini-item";
                item.innerHTML = `
                    <strong>${resource.name}</strong><br>
                    Тип: ${resource.type}<br>
                    Локация: ${resource.location || "-"}
                `;
                preview.appendChild(item);
            });
        }
    } catch (error) {
        showMessage("bookingMessage", error.message, "error");
    }
}

async function loadBookingsPage() {
    try {
        const bookings = await apiRequest("/api/bookings");
        const container = document.getElementById("bookingCards");
        if (!container) return;

        const filter = document.getElementById("statusFilter");
        const selectedStatus = filter ? filter.value : "ALL";

        container.innerHTML = "";

        const filtered = selectedStatus === "ALL"
            ? bookings
            : bookings.filter(b => b.status === selectedStatus);

        if (filtered.length === 0) {
            container.innerHTML = `<div class="booking-card"><h3>Записей нет</h3><p>Подходящих бронирований не найдено.</p></div>`;
            return;
        }

        filtered.forEach(booking => {
            const isCancelled = booking.status === "CANCELLED";
            const card = document.createElement("div");
            card.className = "booking-card";
            card.innerHTML = `
                <h3>${booking.resource.name}</h3>
                <p><strong>ID бронирования:</strong> ${booking.id}</p>
                <p><strong>Сотрудник:</strong> ${booking.bookedBy}</p>
                <p><strong>Начало:</strong> ${formatDateTime(booking.startTime)}</p>
                <p><strong>Окончание:</strong> ${formatDateTime(booking.endTime)}</p>
                <p><strong>Цель:</strong> ${booking.purpose || "-"}</p>
                <span class="badge ${isCancelled ? "cancelled" : "confirmed"}">
                    ${isCancelled ? "Отменено" : "Подтверждено"}
                </span>
                <div class="booking-actions">
                    ${!isCancelled ? `<button class="btn danger" onclick="cancelBooking(${booking.id})">Отменить</button>` : ""}
                </div>
            `;
            container.appendChild(card);
        });
    } catch (error) {
        showMessage("bookingsMessage", error.message, "error");
    }
}

async function cancelBooking(id) {
    try {
        await apiRequest(`/api/bookings/${id}/cancel`, {
            method: "PATCH"
        });
        showMessage("bookingsMessage", `Бронирование ${id} успешно отменено`, "success");
        await loadBookingsPage();
    } catch (error) {
        showMessage("bookingsMessage", error.message, "error");
    }
}

function bindResourceForm() {
    const form = document.getElementById("resourceForm");
    if (!form) return;

    form.addEventListener("submit", async function (e) {
        e.preventDefault();

        const body = {
            name: document.getElementById("resourceName").value.trim(),
            type: document.getElementById("resourceType").value.trim(),
            location: document.getElementById("resourceLocation").value.trim(),
            description: document.getElementById("resourceDescription").value.trim(),
            active: true
        };

        try {
            await apiRequest("/api/resources", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(body)
            });

            form.reset();
            showMessage("resourcesMessage", "Ресурс успешно добавлен", "success");
            await loadResourcesPage();
        } catch (error) {
            showMessage("resourcesMessage", error.message, "error");
        }
    });
}

function bindBookingForm() {
    const form = document.getElementById("bookingForm");
    if (!form) return;

    form.addEventListener("submit", async function (e) {
        e.preventDefault();

        const body = {
            resourceId: Number(document.getElementById("bookingResourceId").value),
            bookedBy: document.getElementById("bookedBy").value.trim(),
            startTime: normalizeDateTime(document.getElementById("startTime").value),
            endTime: normalizeDateTime(document.getElementById("endTime").value),
            purpose: document.getElementById("purpose").value.trim()
        };

        try {
            await apiRequest("/api/bookings", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(body)
            });

            form.reset();
            showMessage("bookingMessage", "Бронирование успешно создано", "success");
        } catch (error) {
            showMessage("bookingMessage", error.message, "error");
        }
    });
}

document.addEventListener("DOMContentLoaded", async () => {
    bindResourceForm();
    bindBookingForm();

    const path = window.location.pathname;

    if (path === "/" || path.endsWith("index.html")) {
        await loadDashboard();
    }

    if (path.endsWith("resources.html")) {
        await loadResourcesPage();
    }

    if (path.endsWith("booking.html")) {
        await fillResourceSelectAndPreview();
    }

    if (path.endsWith("bookings.html")) {
        await loadBookingsPage();
    }
});