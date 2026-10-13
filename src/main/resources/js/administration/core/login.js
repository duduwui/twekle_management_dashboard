/**
 * Administrative Portal Login Handler
 * Asynchronous credential submission and role-based redirect coordinator.
 */
async function handleLogin(e) {
    e.preventDefault();
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const errEl = document.getElementById('loginError');
    const btn = document.getElementById('loginBtn');
    const btnText = document.getElementById('loginBtnText');
    const spinner = document.getElementById('loginSpinner');

    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    errEl.style.display = 'none';
    btn.disabled = true;
    spinner.style.display = 'inline-block';
    btnText.innerText = 'Signing In...';

    try {
        let res = await fetch('/admin/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        const data = await res.json();

        if (res.ok && data.success && data.data && data.data.authenticated) {
            var targetUrl = '/admin/dashboard';
            var user = data.data;
            if (!user.isSuperAdmin && !user.superAdmin) {
                if (!user.canReadFollowups) {
                    if (user.canReadReports) targetUrl = '/admin/reports';
                    else if (user.canManageUsers) targetUrl = '/admin/admins';
                    else if (user.canCreateRoles) targetUrl = '/admin/roles';
                }
            }
            window.location.href = targetUrl;
        } else {
            errEl.innerText = data.message || 'Invalid username or password';
            errEl.style.display = 'block';
            btn.disabled = false;
            spinner.style.display = 'none';
            btnText.innerText = 'Login';
            passwordInput.value = '';
            passwordInput.focus();
        }
    } catch (err) {
        errEl.innerText = 'Connection error. Please try again.';
        errEl.style.display = 'block';
        btn.disabled = false;
        spinner.style.display = 'none';
        btnText.innerText = 'Login';
    }
}
