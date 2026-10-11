// Main JavaScript for Leave Management System (loaded on every page)

const API_BASE_URL = '/lms/api';

// Display alerts
function showAlert(message, type = 'success') {
    const alertDiv = document.createElement('div');
    alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
    alertDiv.innerHTML = `
        ${escapeHtml(message)}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    `;
    document.body.insertBefore(alertDiv, document.body.firstChild);

    setTimeout(() => {
        alertDiv.remove();
    }, 5000);
}

// Escape text before inserting it into HTML (names, reasons and comments are typed by users)
function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

// CSRF token from the page's <meta> tags; required for POST/PUT/DELETE
function csrfHeaders() {
    const token = document.querySelector('meta[name="_csrf"]');
    const header = document.querySelector('meta[name="_csrf_header"]');
    return token && header ? { [header.content]: token.content } : {};
}

// API Helper Functions
// Returns the parsed JSON (or {} for an empty success response), or null after showing the error
async function apiCall(endpoint, method = 'GET', data = null) {
    const options = {
        method: method,
        headers: {
            'Content-Type': 'application/json',
            ...(method === 'GET' ? {} : csrfHeaders())
        }
    };

    if (data) {
        options.body = JSON.stringify(data);
    }

    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, options);
        if (response.status === 401) {
            window.location.href = '/lms/login';
            return null;
        }
        if (!response.ok) {
            let message = `Request failed (${response.status})`;
            try {
                const body = await response.json();
                if (body && body.message) message = body.message;
            } catch (ignored) {
                if (response.status === 403) message = 'You do not have permission to do this';
            }
            showAlert(message, 'danger');
            return null;
        }
        const text = await response.text();
        return text ? JSON.parse(text) : {};
    } catch (error) {
        console.error('API Error:', error);
        showAlert('An error occurred while processing your request', 'danger');
        return null;
    }
}

// Logged-in user (account, role and employee profile), fetched once per page
let currentUserPromise = null;
function getCurrentUser() {
    if (!currentUserPromise) {
        currentUserPromise = apiCall('/me');
    }
    return currentUserPromise;
}

// Leave request statuses: display label and Bootstrap badge colour
const LEAVE_STATUS = {
    PENDING_TL: { label: 'Waiting for Team Lead', badge: 'warning' },
    PENDING_HR: { label: 'Waiting for HR', badge: 'info' },
    APPROVED: { label: 'Approved', badge: 'success' },
    REJECTED: { label: 'Rejected', badge: 'danger' },
    CANCELLED: { label: 'Cancelled', badge: 'secondary' }
};

function statusBadge(status) {
    const s = LEAVE_STATUS[status] || { label: status, badge: 'light' };
    return `<span class="badge bg-${s.badge}${s.badge === 'warning' || s.badge === 'info' ? ' text-dark' : ''}">${escapeHtml(s.label)}</span>`;
}

// One line per approval step, e.g. "Team Lead: approved by Lee Test (Mar 3) - Enjoy"
function approvalTrail(approvals) {
    if (!approvals || approvals.length === 0) return '';
    return approvals.map(a => {
        const step = a.level === 'TEAM_LEAD' ? 'Team Lead' : 'HR';
        const action = a.action === 'APPROVED' ? 'approved' : 'rejected';
        return `${step}: ${action} by ${a.approverName} (${formatDate(a.actedAt)})${a.comments ? ' - ' + a.comments : ''}`;
    }).join('\n');
}

// Day counts may be halves (e.g. 11.5); show them without trailing zeros
function formatDays(value) {
    if (value === null || value === undefined || value === '') return '';
    return String(Number(value));
}

// Format date
function formatDate(dateString) {
    if (!dateString) return '';
    const options = { year: 'numeric', month: 'short', day: 'numeric' };
    return new Date(dateString).toLocaleDateString(undefined, options);
}

// Fill a <select> with the leave types from the server (if the page has it) and return the types
async function loadLeaveTypeOptions(selectId) {
    const types = await apiCall('/leave-types');
    if (!types) return [];
    const select = document.getElementById(selectId);
    if (!select) return types;
    select.innerHTML = '<option value="">Select Leave Type</option>' + types.map(t =>
        `<option value="${t.leaveTypeId}">${escapeHtml(t.leaveTypeName)}</option>`).join('');
    return types;
}

// Initialize common date input with today's date minimum
function initializeDateInputs() {
    const today = new Date().toISOString().split('T')[0];
    const dateInputs = document.querySelectorAll('input[type="date"]');
    dateInputs.forEach(input => {
        if (input.id === 'dateOfJoining' || input.dataset.allowPast !== undefined) {
            // Past dates allowed (joining date, days already worked)
        } else {
            input.min = today;
        }
    });
}

// Document ready
document.addEventListener('DOMContentLoaded', function() {
    initializeDateInputs();
});
