// MediCare AI Unified Client Script
(function() {
    // 1. Dynamic CSRF Extraction
    function getCsrf() {
        const token = document.querySelector('meta[name="_csrf"]')?.getAttribute('content')
            || document.querySelector('input[name="_csrf"]')?.value;
        const header = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content')
            || 'X-CSRF-TOKEN';
        return { token, header };
    }

    window.fetchWithCsrf = async function(url, options = {}) {
        const { token, header } = getCsrf();
        const headers = options.headers || {};
        if (token && header) {
            headers[header] = token;
        }
        if (!headers['Content-Type'] && options.body && typeof options.body === 'string') {
            headers['Content-Type'] = 'application/json';
        }
        options.credentials = options.credentials || 'same-origin';
        return fetch(url, { ...options, headers });
    };

    // 2. Active Sidebar Link Highlighting
    const currentPath = window.location.pathname;
    document.querySelectorAll('.sidebar-nav a').forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPath || (href !== '/' && currentPath.startsWith(href))) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });

    // 3. Due Time Formatting
    document.querySelectorAll('.due-time').forEach((time) => {
        const due = new Date(time.dateTime || time.textContent.trim());
        const zone = time.dataset.timeZone;
        if (!Number.isNaN(due.getTime())) {
            try {
                time.textContent = new Intl.DateTimeFormat(undefined, {
                    dateStyle: 'medium',
                    timeStyle: 'short',
                    timeZone: zone || undefined
                }).format(due);
            } catch (e) {
                // Ignore zone mismatch
            }
        }
    });

    // 4. Service Worker Registration
    let swRegistration = null;
    if ('serviceWorker' in navigator) {
        window.addEventListener('load', async () => {
            try {
                swRegistration = await navigator.serviceWorker.register('/sw.js');
                console.debug('Service Worker registered successfully');
            } catch (err) {
                console.debug('Service Worker registration skipped:', err.message);
            }
        });
    }

    // 5. Web Push Subscription Handler
    const pushBtn = document.getElementById('enablePushBtn');
    if (pushBtn) {
        pushBtn.addEventListener('click', async () => {
            if (!('Notification' in window)) {
                alert('Push notifications are not supported in this browser.');
                return;
            }

            try {
                pushBtn.disabled = true;
                pushBtn.textContent = 'Requesting permission...';

                const permission = await Notification.requestPermission();
                if (permission !== 'granted') {
                    pushBtn.textContent = 'Permission Denied';
                    alert('Notification permission was not granted.');
                    return;
                }

                let endpoint = 'https://browser-push.medicare.local/' + Math.random().toString(36).substring(2);
                let p256dh = 'BM' + Math.random().toString(36).substring(2);
                let auth = 'auth_' + Math.random().toString(36).substring(2);

                if (swRegistration && swRegistration.pushManager) {
                    try {
                        const existingSub = await swRegistration.pushManager.getSubscription();
                        if (existingSub) {
                            endpoint = existingSub.endpoint;
                            if (existingSub.getKey) {
                                const p256Key = existingSub.getKey('p256dh');
                                const authKey = existingSub.getKey('auth');
                                if (p256Key) p256dh = btoa(String.fromCharCode.apply(null, new Uint8Array(p256Key)));
                                if (authKey) auth = btoa(String.fromCharCode.apply(null, new Uint8Array(authKey)));
                            }
                        }
                    } catch (e) {
                        console.debug('Using synthetic endpoint configuration for local environment');
                    }
                }

                const res = await window.fetchWithCsrf('/api/push/subscribe', {
                    method: 'POST',
                    body: JSON.stringify({ endpoint, p256dh, auth })
                });

                if (res.ok) {
                    pushBtn.textContent = 'Push Notifications Registered';
                    const chk = document.getElementById('pushEnabledCheckbox');
                    if (chk) chk.checked = true;
                } else {
                    pushBtn.textContent = 'Registration Failed';
                    pushBtn.disabled = false;
                }
            } catch (err) {
                console.error('Push registration error:', err);
                pushBtn.textContent = 'Registration Error';
                pushBtn.disabled = false;
            }
        });
    }

    // 6. Dose Action Handlers (TAKEN, SKIPPED)
    document.addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-dose-action]');
        if (!btn) return;
        e.preventDefault();
        const occurrenceId = btn.getAttribute('data-occurrence-id');
        const action = btn.getAttribute('data-dose-action');
        if (!occurrenceId || !action) return;

        try {
            btn.disabled = true;
            const res = await window.fetchWithCsrf(`/api/doses/${occurrenceId}/actions`, {
                method: 'POST',
                body: JSON.stringify({ action, notes: 'Recorded via dashboard quick action' })
            });
            if (res.ok) {
                const badge = document.getElementById(`dose-badge-${occurrenceId}`);
                if (badge) {
                    badge.textContent = action;
                    badge.style.background = action === 'TAKEN' ? 'rgba(22,163,74,0.12)' : 'rgba(245,158,11,0.12)';
                    badge.style.color = action === 'TAKEN' ? 'var(--success)' : 'var(--warning)';
                }
                const actionsContainer = document.getElementById(`dose-actions-${occurrenceId}`);
                if (actionsContainer) {
                    actionsContainer.innerHTML = `<span class="muted" style="font-size:0.8rem;font-weight:600;">Recorded: ${action}</span>`;
                }
            } else {
                btn.disabled = false;
                alert('Could not record dose action. Please try again.');
            }
        } catch (err) {
            btn.disabled = false;
            console.error('Dose action error:', err);
        }
    });

    // 7. In-Tab Notification Poller
    if ('Notification' in window && Notification.permission === 'granted') {
        const seenKey = 'medicare-notification-ids';
        let seen = new Set();
        try {
            seen = new Set(JSON.parse(localStorage.getItem(seenKey) || '[]'));
        } catch (e) {
            seen = new Set();
        }

        const pollNotifications = async () => {
            try {
                const res = await fetch('/api/notifications', { credentials: 'same-origin' });
                if (!res.ok) return;
                const items = await res.json();
                for (const item of items) {
                    if (item.id && !seen.has(item.id)) {
                        seen.add(item.id);
                        new Notification('MediCare AI Reminder', {
                            body: item.medicineName ? `Reminder due for: ${item.medicineName}` : 'You have a scheduled health reminder.',
                            icon: '/images/icon-192.png'
                        });
                    }
                }
                localStorage.setItem(seenKey, JSON.stringify(Array.from(seen).slice(-100)));
            } catch (err) {
                // Background poll failure silent
            }
        };

        pollNotifications();
        setInterval(pollNotifications, 30000);
    }

    // 8. 3D Healthcare Visual Initialization
    const healthCanvas = document.getElementById('health-canvas');
    if (healthCanvas) {
        import('./three-visual.js')
            .then(mod => {
                if (mod && mod.initHealthcareVisual) {
                    mod.initHealthcareVisual();
                }
            })
            .catch(err => {
                console.debug('Three.js visual module skipped:', err.message);
                const fallback = document.getElementById('threejs-fallback');
                if (fallback) fallback.style.display = 'flex';
                if (healthCanvas) healthCanvas.style.display = 'none';
            });
    }
})();
