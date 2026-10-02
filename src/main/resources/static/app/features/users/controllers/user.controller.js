/**
 * Users Feature Coordinator Controller
 * Coordinates active sub-tab ('list' vs 'create') and modals (inspect, update, delete).
 */
angular.module('tweklApp')
.controller('UserController', ['$scope', function($scope) {
    // Active sub-view tab: 'list' | 'create'
    $scope.userView = 'list';
    $scope.userActiveTab = 'list';
    $scope.users = [];
    $scope.userModalMode = null; // 'inspect' | 'update' | 'delete'
    $scope.selectedUser = null;
    $scope.editingUser = null;
    $scope.deletingUser = null;

    /**
     * Switch user sub-tab view ('list' vs 'create')
     */
    $scope.setUserView = function(view) {
        $scope.userView = view;
        $scope.userActiveTab = view;
    };

    /**
     * Inspect user permissions modal
     */
    $scope.inspectUser = function(user) {
        $scope.selectedUser = angular.copy(user);
        $scope.userModalMode = 'inspect';
    };

    /**
     * Open edit user modal
     */
    $scope.editUser = function(user) {
        $scope.editingUser = angular.copy(user);
        $scope.userModalMode = 'update';
    };

    /**
     * Open delete user confirmation dialog
     */
    $scope.confirmDeleteUser = function(user) {
        $scope.deletingUser = user;
        $scope.userModalMode = 'delete';
    };
}]);
