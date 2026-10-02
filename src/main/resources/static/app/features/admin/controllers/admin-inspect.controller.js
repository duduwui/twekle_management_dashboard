/**
 * Admin Action Controller: Admin Inspect
 * Displays detailed information and role flags for a selected administrator.
 */
angular.module('tweklApp')
.controller('AdminInspectController', ['$scope', function($scope) {

    /**
     * Closes inspect modal and returns to list view
     */
    $scope.closeInspect = function() {
        $scope.selectedAdmin = null;
        $scope.adminView = 'list';
    };
}]);
