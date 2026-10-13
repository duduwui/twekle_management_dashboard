/** Dashboard interactions use the HTML already rendered by Thymeleaf. */
(function () {
    'use strict';

    function filterRegistrations(days) {
        var cutoff = days ? Date.now() - days * 86400000 : 0;
        var visible = 0;
        document.querySelectorAll('[data-registered]').forEach(function (row) {
            row.hidden = !!cutoff && !(Date.parse(row.dataset.registered) > cutoff);
            if (!row.hidden) visible++;
        });
        document.getElementById('registrationsEmpty').hidden = visible > 0;
    }

    document.addEventListener('click', function (event) {
        var opener = event.target.closest('[data-dialog]');
        if (opener) {
            if (opener.dataset.days !== undefined) filterRegistrations(Number(opener.dataset.days));
            document.getElementById(opener.dataset.dialog).showModal();
        }
        var close = event.target.closest('[data-close-dialog]');
        if (close) close.closest('dialog').close();
        var filter = event.target.closest('[data-registration-days]');
        if (filter) filterRegistrations(Number(filter.dataset.registrationDays));
    });
}());
