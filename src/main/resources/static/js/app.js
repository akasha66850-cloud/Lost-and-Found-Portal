/**
 * Futuristic Dark UI Application Script
 * Exactly styled after the Expanse Dashboard
 */

// ── Auth helpers ─────────────────────────────────────────────
function getToken()    { return localStorage.getItem('token'); }
function getRole()     { return localStorage.getItem('role'); }
function getUsername() { return localStorage.getItem('username'); }
function getFullName() { return localStorage.getItem('fullName') || getUsername() || 'User'; }
function isLoggedIn()  { return !!getToken(); }

function authHeaders() {
    return { 'Authorization': 'Bearer ' + getToken() };
}

function requireLogin(redirectTo) {
    if (!isLoggedIn()) {
        window.location = redirectTo || '/login.html';
        return false;
    }
    return true;
}

function logout() {
    localStorage.clear();
    window.location = '/login.html';
}

// ── Sidebar Builder (Matching Image) ──────────────────────────
function buildSidebar(activePage) {
    const role = getRole();
    const isAdmin = role === 'ADMIN';

    const items = [
        { id: 'dashboard',     icon: 'fa-chart-pie',         label: 'Dashboard',       href: '/index.html' },
        { id: 'report-lost',   icon: 'fa-exclamation-circle', label: 'Report Lost',     href: '/report-lost.html' },
        { id: 'report-found',  icon: 'fa-hand-holding-heart',label: 'Report Found',    href: '/report-found.html' },
        { id: 'search',        icon: 'fa-magnifying-glass',  label: 'Search Items',    href: '/search.html' },
        { id: 'my-reports',    icon: 'fa-file-lines',        label: 'My Reports',      href: '/my-reports.html' },
        { id: 'my-claims',     icon: 'fa-list-check',        label: 'My Claims',       href: '/my-claims.html' },
        { id: 'notifications', icon: 'fa-bell',              label: 'Notifications',   href: '/notifications.html', badgeId: 'sidebarNotifBadge' },
    ];

    if (isAdmin) {
        items.push({ id: 'admin', icon: 'fa-shield-halved', label: 'Admin Panel', href: '/admin-claims.html' });
    }

    let menuHtml = '';
    items.forEach(it => {
        const isActive = it.id === activePage ? 'active' : '';
        const badge = it.badgeId ? `<span class="nav-link-badge" id="${it.badgeId}" style="display:none">0</span>` : '';
        menuHtml += `
            <a href="${it.href}" class="nav-link-item ${isActive}">
                <i class="fas ${it.icon}"></i>
                <span>${it.label}</span>
                ${badge}
            </a>
        `;
    });

    return `
    <aside class="sidebar" id="sidebar">
        <div class="sidebar-brand">
            <div class="brand-logo-glow"><i class="fas fa-cube"></i></div>
            <div class="brand-title">Lost &amp; Found</div>
        </div>

        <nav class="sidebar-menu">
            ${menuHtml}
        </nav>

        <div class="sidebar-bottom">
            <div class="developer-card-badge">
                <div class="dev-sub"><i class="fas fa-code me-1"></i> Developed by</div>
                <div class="dev-name">AKASH P</div>
                <div class="dev-dept">III CSE A (PSG ITECH)</div>
            </div>
            <a href="javascript:void(0)" onclick="logout()" class="nav-link-item" style="color:#f87171">
                <i class="fas fa-arrow-right-from-bracket" style="color:#f87171"></i>
                <span>Logout</span>
            </a>
        </div>
    </aside>
    `;
}

// ── Top Header Builder (Matching Image) ───────────────────────
function buildTopbar(pageTitle) {
    const today = new Date().toLocaleDateString('en-US', {
        weekday: 'short', month: 'short', day: 'numeric', year: 'numeric'
    });
    const name = getFullName();
    const initial = name.charAt(0).toUpperCase();

    return `
    <header class="topbar">
        <div class="topbar-page-title">${pageTitle}</div>
        <div class="topbar-right">
            <div class="topbar-date">${today}</div>
            <div class="user-profile-chip" onclick="window.location='/my-reports.html'" title="View profile">
                <div class="user-avatar-circle">${initial}</div>
                <span class="user-chip-name">${name}</span>
                <i class="fas fa-chevron-down" style="font-size:11px;color:#94a3b8"></i>
            </div>
            <button class="btn-header-logout" onclick="logout()" title="Sign out">
                <i class="fas fa-power-off"></i>
            </button>
        </div>
    </header>
    `;
}

// ── Init Page ─────────────────────────────────────────────────
async function initPage(options = {}) {
    const {
        activePage = 'dashboard',
        pageTitle  = 'Dashboard',
        requireAuth = true
    } = options;

    if (requireAuth && !requireLogin()) return;

    // Mount Sidebar
    const sidebarEl = document.getElementById('sidebar-mount');
    if (sidebarEl) sidebarEl.innerHTML = buildSidebar(activePage);

    // Mount Topbar
    const topbarEl = document.getElementById('topbar-mount');
    if (topbarEl) topbarEl.innerHTML = buildTopbar(pageTitle);

    // Load unread count
    if (isLoggedIn()) {
        try {
            const res = await fetch('/api/notifications/unread-count', { headers: authHeaders() });
            if (res.ok) {
                const data = await res.json();
                const count = data.count || 0;
                const badge = document.getElementById('sidebarNotifBadge');
                if (badge && count > 0) {
                    badge.textContent = count;
                    badge.style.display = 'inline-block';
                }
            }
        } catch (e) { /* ignore */ }
    }
}

// ── Status Badges ─────────────────────────────────────────────
function statusBadge(status) {
    const map = {
        'ACTIVE':                     ['badge-active',    'Active'],
        'RETURNED':                   ['badge-returned',  'Returned'],
        'RESOLVED':                   ['badge-resolved',  'Resolved'],
        'REMOVED':                    ['badge-rejected',  'Removed'],
        'PENDING_VERIFICATION':       ['badge-pending',   'Pending'],
        'APPROVED_AWAITING_HANDOVER': ['badge-approved',  'Approved'],
        'REJECTED':                   ['badge-rejected',  'Rejected'],
        'COMPLETED':                  ['badge-completed', 'Completed'],
        'POTENTIAL_MATCH':            ['badge-pending',   'Potential Match'],
        'CLAIM_IN_PROGRESS':          ['badge-approved',  'Claim In Progress'],
    };
    const [cls, label] = map[status] || ['badge-active', status || 'Unknown'];
    return `<span class="badge ${cls}">${label}</span>`;
}

function typeBadge(isLost) {
    return isLost
        ? '<span class="badge badge-lost"><i class="fas fa-circle-exclamation me-1"></i> LOST</span>'
        : '<span class="badge badge-found"><i class="fas fa-circle-check me-1"></i> FOUND</span>';
}

// ── Time Formatting ───────────────────────────────────────────
function timeAgo(isoString) {
    if (!isoString) return '';
    const diff = Date.now() - new Date(isoString).getTime();
    const mins  = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days  = Math.floor(diff / 86400000);
    if (mins  <  1) return 'just now';
    if (mins  < 60) return `${mins}m ago`;
    if (hours < 24) return `${hours}h ago`;
    return `${days}d ago`;
}

// ── API Helper ────────────────────────────────────────────────
async function apiFetch(url, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (isLoggedIn() && !options.noAuth) {
        headers['Authorization'] = 'Bearer ' + getToken();
    }
    const res = await fetch(url, { ...options, headers });
    return res;
}

// ── Toast Notification ────────────────────────────────────────
function showToast(message, type = 'info') {
    const colors = {
        success: '#10b981', error: '#f43f5e', info: '#3b82f6', warning: '#f59e0b'
    };
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed; bottom: 30px; right: 30px; z-index: 99999;
        background: #1e2235; color: #fff; border: 1px solid ${colors[type] || colors.info};
        padding: 14px 22px; border-radius: 14px; font-size: 15px; font-weight: 600;
        box-shadow: 0 10px 30px rgba(0,0,0,0.6); max-width: 420px;
        display: flex; align-items: center; gap: 12px;
        animation: slideIn .2s ease;
    `;
    toast.innerHTML = `<i class="fas fa-${type === 'success' ? 'circle-check text-success' : type === 'error' ? 'circle-xmark text-danger' : 'circle-info text-primary'}"></i> ${message}`;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity .3s';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
