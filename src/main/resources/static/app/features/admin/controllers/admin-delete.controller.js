/**
 * Admin Action Controller: Admin Delete
 * Handles administrator deletion confirmation and execution.
 */
angular.module('tweklApp')
.controller('AdminDeleteController', ['$scope', 'AdminDeleteService', 'ToastService',
function($scope, AdminDeleteService, ToastService) {

    /**
     * Executes deletion of selected administrator
     */
    $scope.deleteAdmin = function() {
        if (!$scope.selectedAdmin || !$scope.selectedAdmin.id) return;

        AdminDeleteService.execute($scope.selectedAdmin.id).then(function() {
            ToastService.success('Administrator deleted successfully');
            $scope.selectedAdmin = null;
            $scope.adminView = 'list';
            if (typeof $scope.loadAdmins === 'function') {
                $scope.loadAdmins();
            }
        }, function(err) {
            ToastService.error('Failed to delete administrator');
        });
    };

    /**
     * Cancels deletion dialog
     */
    $scope.cancelDelete = function() {
        $scope.selectedAdmin = null;
        $scope.adminView = 'list';
    };
}]);
