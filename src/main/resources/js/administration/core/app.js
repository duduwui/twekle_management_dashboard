/**
 * Twekl AngularJS Application Module
 * Core configuration and HTTP session interceptor.
 */
var app = angular.module('tweklApp', []);

app.config(['$httpProvider', function($httpProvider) {
    // Ensure all AngularJS HTTP requests explicitly negotiate JSON
    $httpProvider.defaults.headers.common['Accept'] = 'application/json';

    // Thymeleaf supplies the session CSRF token for the remaining JSON actions.
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    if (token && header) {
        ['post', 'put', 'patch', 'delete'].forEach(function(method) {
            $httpProvider.defaults.headers[method] = $httpProvider.defaults.headers[method] || {};
            $httpProvider.defaults.headers[method][header.content] = token.content;
        });
    }

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
