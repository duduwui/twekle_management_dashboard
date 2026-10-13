/**
 * Toast Notification Service & Controller
 * Manages toast lifecycle, styling, auto-dismiss, and UI event binding.
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
.controller('ToastController', ['$scope', '$rootScope', 'ToastService', function($scope, $rootScope, ToastService) {
    $scope.toasts = $rootScope.toasts || [];

    $scope.dismissToast = function(toastId) {
        var list = $rootScope.toasts || [];
        var idx = list.findIndex(function(t) { return t.id === toastId; });
        if (idx !== -1) {
            list.splice(idx, 1);
        }
    };

    $scope.$on('toast:show', function(event, data) {
        if (data && data.message) {
            ToastService.show(data.message, data.type, data.duration);
        }
    });
}]);
