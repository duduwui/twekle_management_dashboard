/**
 * Layout Sub-Controller: Header
 * Manages language switching, user profile popover, and logout.
 */
angular.module('tweklApp')
.controller('HeaderController', ['$scope', 'I18nService', 'AuthLogoutService', 'ToastService',
function($scope, I18nService, AuthLogoutService, ToastService) {

    $scope.currentLang = I18nService.getLanguage();

    /**
     * Switch application language (EN, AR, KU)
     */
    $scope.changeLanguage = function(lang) {
        $scope.currentLang = lang;
        I18nService.setLanguage(lang);
    };

    /**
     * Execute user logout
     */
    $scope.logout = function() {
        AuthLogoutService.execute().then(function() {
            window.location.href = '/login';
        }, function() {
            window.location.href = '/login';
        });
    };
}]);
