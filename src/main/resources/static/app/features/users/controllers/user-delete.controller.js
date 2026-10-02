/**
 * User Action Controller: User Delete
 * Handles user account deletion confirmation and execution.
 */
angular.module('tweklApp')
.controller('UserDeleteController', ['$scope', 'UserDeleteService', 'ToastService',
function($scope, UserDeleteService, ToastService) {

    /**
     * Confirms and deletes user
     */
    $scope.deleteUser = function() {
        if (!$scope.deletingUser || !$scope.deletingUser.id) return;

        UserDeleteService.execute($scope.deletingUser.id).then(function() {
            ToastService.success('User deleted successfully');
            $scope.userModalMode = null;
            $scope.deletingUser = null;
            if (typeof $scope.loadUsers === 'function') {
                $scope.loadUsers();
            }
        }, function(err) {
            ToastService.error('Failed to delete user');
        });
    };

    /**
     * Cancels delete dialog
     */
    $scope.cancelDelete = function() {
        $scope.userModalMode = null;
        $scope.deletingUser = null;
    };
}]);
