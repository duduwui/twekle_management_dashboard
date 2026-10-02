/**
 * User Action Controller: User Update
 * Handles editing user names and modifying their 3-module CRUD permissions.
 */
angular.module('tweklApp')
.controller('UserUpdateController', ['$scope', 'UserUpdateService', 'ToastService',
function($scope, UserUpdateService, ToastService) {

    /**
     * Submits updated user details
     */
    $scope.updateUser = function() {
        if (!$scope.editingUser || !$scope.editingUser.id) return;

        UserUpdateService.execute($scope.editingUser.id, $scope.editingUser).then(function() {
            ToastService.success('User updated successfully');
            $scope.userModalMode = null;
            $scope.editingUser = null;
            if (typeof $scope.loadUsers === 'function') {
                $scope.loadUsers();
            }
        }, function(err) {
            ToastService.error(err.data?.message || 'Failed to update user');
        });
    };

    /**
     * Cancels update modal
     */
    $scope.cancelUpdate = function() {
        $scope.userModalMode = null;
        $scope.editingUser = null;
    };
}]);
