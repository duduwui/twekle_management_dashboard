/**
 * ============================================================================
 * Twekl Core AngularJS Application Setup (app.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/core/app.js
 * Used by:  All SPA/Angular-enabled views (followups, reports)
 * 
 * Purpose:
 *   1. Initializes the root `tweklApp` AngularJS module.
 *   2. Automatically reads Spring Security CSRF tokens from HTML <meta> tags
 *      and injects them into every POST, PUT, PATCH, and DELETE HTTP request.
 *   3. Enforces `Accept: application/json` across all $http calls.
 *   4. Installs a global response interceptor to handle session timeouts
 *      and unauthorized (401/403) errors cleanly.
 * ============================================================================
 */

var app = angular.module('tweklApp', []);

app.config(['$httpProvider', function ($httpProvider) {

    // 1. Explicitly negotiate JSON for all Angular API requests
    $httpProvider.defaults.headers.common['Accept'] = 'application/json';

    // 2. CSRF Security Token Injection
    // Reads meta tags rendered by Spring Security in the Thymeleaf layout head:
    // <meta name="_csrf" th:content="${_csrf.token}"/>
    // <meta name="_csrf_header" th:content="${_csrf.headerName}"/>
    var tokenMeta = document.querySelector('meta[name="_csrf"]');
    var headerMeta = document.querySelector('meta[name="_csrf_header"]');

    if (tokenMeta && headerMeta) {
        ['post', 'put', 'patch', 'delete'].forEach(function (method) {
            $httpProvider.defaults.headers[method] = $httpProvider.defaults.headers[method] || {};
            $httpProvider.defaults.headers[method][headerMeta.content] = tokenMeta.content;
        });
    }

    // 3. Global HTTP Response Interceptor (Session Expiry & Permissions)
    $httpProvider.interceptors.push(['$q', '$rootScope', function ($q, $rootScope) {
        return {
            'responseError': function (rejection) {
                if (rejection.status === 401) {
                    // Session expired or unauthenticated -> broadcast to show login modal or redirect
                    $rootScope.$broadcast('auth:unauthorized', rejection.data);
                } else if (rejection.status === 403) {
                    // Access forbidden / insufficient role privileges
                    $rootScope.$broadcast('auth:forbidden', rejection.data);
                }
                return $q.reject(rejection);
            }
        };
    }]);

}]);

