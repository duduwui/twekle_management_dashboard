/**
 * Main Layout Coordinator Controller
 * Manages global application shell, active tab routing, and session state.
 */
angular.module('tweklApp')
.controller('LayoutController', ['$scope', 'AuthGetMeService', 'ToastService',
function($scope, AuthGetMeService, ToastService) {

    // Default active tab
    $scope.activeTab = 'followups';
    $scope.currentUser = null;

    /**
     * Resolves initial route based on window pathname
     */
    function initRoute() {
        var path = window.location.pathname;
        if (path.indexOf('/admin/admins') === 0) {
            $scope.activeTab = 'admin';
        } else if (path.indexOf('/admin/users') === 0) {
            $scope.activeTab = 'users';
        } else if (path.indexOf('/admin/time-filters') === 0) {
            $scope.activeTab = 'time-filters';
        } else if (path.indexOf('/admin/reports') === 0) {
            $scope.activeTab = 'reports';
        } else {
            $scope.activeTab = 'followups';
        }
    }

    /**
     * Load logged-in user profile
     */
    $scope.loadCurrentUser = function() {
        AuthGetMeService.execute().then(function(res) {
            $scope.currentUser = res.data;
        }, function(err) {
            // Unauthenticated redirect handled by HTTP interceptor
        });
    };

    // Listen to browser popstate (back/forward navigation)
    window.addEventListener('popstate', function() {
        $scope.$apply(function() {
            initRoute();
        });
    });

    initRoute();
    $scope.loadCurrentUser();
}]);
