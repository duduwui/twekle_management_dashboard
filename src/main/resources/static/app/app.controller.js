/**
 * Twekl Management Dashboard - Main Application Controller
 * High-level coordinator delegating to micro-feature controllers.
 */
angular.module('tweklApp')
.controller('DashboardController', ['$scope', function($scope) {
    // Master layout scope placeholder for backward compatibility
    $scope.appTitle = 'Twekl Dashboard';
}]);
