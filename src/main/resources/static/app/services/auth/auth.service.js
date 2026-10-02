/**
 * Auth Feature Service Facade
 * Delegates authentication operations to dedicated single-action micro-services.
 */
app.factory('AuthService', [
    'AuthGetMeService',
    'AuthLoginService',
    'AuthLogoutService',
function(AuthGetMeService, AuthLoginService, AuthLogoutService) {
    return {
        getMe: function() { return AuthGetMeService.execute(); },
        login: function(credentials) { return AuthLoginService.execute(credentials); },
        logout: function() { return AuthLogoutService.execute(); }
    };
}]);
