/**
 * Toast & Notification Controller
 * Manages toast notifications lifecycle, dismissals, and alert rendering.
 */
angular.module('tweklApp')
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
