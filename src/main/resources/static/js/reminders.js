document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.due-time').forEach((time) => {
        const due = new Date(time.dateTime);
        const zone = time.dataset.timeZone;
        if (!Number.isNaN(due.getTime())) {
            time.textContent = new Intl.DateTimeFormat(undefined, {
                dateStyle: 'medium',
                timeStyle: 'short',
                timeZone: zone
            }).format(due);
        }
    });

    const enableButton = document.getElementById('enable-browser-alerts');
    if (!enableButton || !('Notification' in window)) {
        if (enableButton) {
            enableButton.disabled = true;
            enableButton.textContent = 'Browser alerts unavailable';
        }
        return;
    }

    let enabled = Notification.permission === 'granted';
    const status = document.getElementById('browser-alert-status');
    const seenKey = 'medicare-notification-ids';
    let seen;
    try {
        const savedIds = JSON.parse(localStorage.getItem(seenKey) || '[]');
        seen = new Set(Array.isArray(savedIds) ? savedIds.filter(Number.isInteger) : []);
    } catch (error) {
        status.textContent = 'Browser alert history could not be loaded. In-app reminders remain available.';
        seen = new Set();
    }

    const checkNotifications = async () => {
        if (!enabled) return;
        try {
            const response = await fetch('/api/notifications', {
                headers: { Accept: 'application/json' },
                credentials: 'same-origin'
            });
            if (!response.ok) {
                status.textContent = 'Browser alerts could not refresh. The in-app notification center is still available.';
                return;
            }
            const notifications = await response.json();
            for (const notification of notifications) {
                if (notification.state !== 'AVAILABLE' ||
                    !Number.isInteger(notification.id) ||
                    seen.has(notification.id)) continue;
                seen.add(notification.id);
                new Notification('MediCare AI reminder', {
                    body: 'A medicine reminder is available. Open the care portal to view it.'
                });
            }
            localStorage.setItem(seenKey, JSON.stringify(Array.from(seen).slice(-100)));
            status.textContent = '';
        } catch (error) {
            status.textContent = 'Browser alerts are temporarily unavailable. The in-app notification center is still available.';
        }
    };

    enableButton.addEventListener('click', async () => {
        enabled = await Notification.requestPermission() === 'granted';
        enableButton.textContent = enabled ? 'Browser alerts enabled' : 'Browser permission not granted';
        if (enabled) await checkNotifications();
    });

    if (enabled) {
        enableButton.textContent = 'Browser alerts enabled';
        checkNotifications();
        window.setInterval(checkNotifications, 30000);
    }
});
