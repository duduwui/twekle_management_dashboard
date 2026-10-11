/**
 * Common Application Utilities & Services
 * ToastService, I18nService, and FilterHelperService.
 */
angular.module('tweklApp')
.factory('ToastService', ['$timeout', '$rootScope', function($timeout, $rootScope) {
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
        success: function(msg) { show(msg, 'success'); },
        error: function(msg) { show(msg, 'error'); },
        info: function(msg) { show(msg, 'info'); },
        warning: function(msg) { show(msg, 'warning'); }
    };
}])
.factory('I18nService', [function() {
    var translations = {
        'dashboard': 'Dashboard',
        'customers': 'Follow-ups',
        'reports': 'Reports',
        'timelines': 'Time Filters',
        'admins': 'Administration',
        'roles': 'Roles & Permissions'
    };
    return {
        t: function(key) { return translations[key] || key; }
    };
}])
.factory('FilterHelperService', [function() {
    return {
        filterByDateRange: function(items, fromDate, toDate, dateField) {
            if (!items) return [];
            return items.filter(function(item) {
                if (!item[dateField]) return true;
                var d = new Date(item[dateField]);
                if (fromDate && d < new Date(fromDate)) return false;
                if (toDate && d > new Date(toDate)) return false;
                return true;
            });
        }
    };
}]);
