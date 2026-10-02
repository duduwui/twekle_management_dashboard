/**
 * Admin Feature Coordinator Controller
 * Manages view switching between 'list', 'create', 'inspect', and 'delete' states.
 */
angular.module('tweklApp')
.controller('AdminController', ['$scope', function($scope) {
    // Active admin view state: 'list', 'create', 'inspect', 'delete'
    $scope.adminView = 'list';
    $scope.admins = [];
    $scope.selectedAdmin = null;

    /**
     * Change active admin view state
     */
    $scope.setAdminView = function(view) {
        $scope.adminView = view;
    };

    /**
     * Switch view to inspect an admin
     */
    $scope.inspectAdmin = function(admin) {
        $scope.selectedAdmin = angular.copy(admin);
        $scope.adminView = 'inspect';
    };

    /**
     * Switch view to delete confirmation dialog
     */
    $scope.confirmDeleteAdmin = function(admin) {
        $scope.selectedAdmin = admin;
        $scope.adminView = 'delete';
    };
}]);
