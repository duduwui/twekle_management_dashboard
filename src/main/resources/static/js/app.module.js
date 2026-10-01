/**
 * Twekl Management Dashboard - AngularJS Application Module
 * Configures core application module, HTTP interceptors, and security listeners.
 */
var app = angular.module('tweklApp', []);

// Configure HTTP interceptor to catch 401/403 Spring Security rejections
app.config(['$httpProvider', function($httpProvider) {
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
