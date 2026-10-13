/** Dashboard interactions use the HTML already rendered by Thymeleaf. */
(function () {
    'use strict';

    document.addEventListener('click', function (event) {
        var opener = event.target.closest('[data-dialog]');
        if (opener) {
            document.getElementById(opener.dataset.dialog).showModal();
        }
        var close = event.target.closest('[data-close-dialog]');
        if (close) close.closest('dialog').close();
    });
}());
