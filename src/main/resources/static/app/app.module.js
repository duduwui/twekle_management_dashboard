/**
 * Twekl AngularJS Application Module
 * Core configuration and HTTP session interceptor.
 */
var app = angular.module('tweklApp', []);

app.config(['$httpProvider', function($httpProvider) {
    // Intercept 401/403 HTTP session responses
    $httpProvider.interceptors.push(['$q', '$rootScope', function($q, $rootScope) {
        return {
            'responseError': function(rejection) {
                if (rejection.status === 401) {
                    $rootScope.$broadcast('auth:unauthorized', rejection.data);
                } else if (rejection.status === 403) {
                    $rootScope.$broadcast('auth:forbidden', rejection.data);
                }
                return $q.reject(rejection);
            }
        };
    }]);
}]);
