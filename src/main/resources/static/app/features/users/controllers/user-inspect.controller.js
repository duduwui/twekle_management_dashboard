/**
 * User Action Controller: User Inspect
 * Displays the 3-module CRUD permissions matrix for a selected user.
 */
angular.module('tweklApp')
.controller('UserInspectController', ['$scope', function($scope) {

    /**
     * Closes the inspect modal
     */
    $scope.closeInspect = function() {
        $scope.selectedUser = null;
        $scope.userModalMode = null;
    };
}]);
