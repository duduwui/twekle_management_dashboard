/**
 * Toast Notification Service
 * Handles user alerts, success, error, and info toasts.
 */
app.factory('ToastService', ['$timeout', '$rootScope', function($timeout, $rootScope) {
    $rootScope.toasts = [];
    var toastCounter = 0;

    function show(message, type, duration) {
        var id = ++toastCounter;
        var toastType = type || 'success';
        var toastDuration = duration || (toastType === 'error' ? 4500 : 3000);

        var toast = {
            id: id,
            message: message,
            type: toastType
        };

        $rootScope.toasts.push(toast);

        $timeout(function() {
            var index = $rootScope.toasts.findIndex(function(t) { return t.id === id; });
            if (index !== -1) {
                $rootScope.toasts.splice(index, 1);
            }
        }, toastDuration);
    }

    return {
        show: show,
        success: function(msg, dur) { show(msg, 'success', dur); },
        error: function(msg, dur) { show(msg, 'error', dur); },
        info: function(msg, dur) { show(msg, 'info', dur); },
        warning: function(msg, dur) { show(msg, 'warning', dur); }
    };
}]);
