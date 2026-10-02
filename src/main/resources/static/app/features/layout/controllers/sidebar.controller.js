/**
 * Layout Sub-Controller: Sidebar Navigation
 * Handles active tab switching and clean HTML5 history routing (strictly zero numeric badge clutter).
 */
angular.module('tweklApp')
.controller('SidebarController', ['$scope', '$location',
function($scope, $location) {

    /**
     * Navigate to feature tab with clean URL
     */
    $scope.navigateTo = function(tabName, urlPath) {
        $scope.activeTab = tabName;
        if (window.history && window.history.pushState) {
            window.history.pushState({ tab: tabName }, '', urlPath);
        }
    };
}]);
