/**
 * User Feature Service Facade
 * Delegates all user operations to dedicated single-action micro-services.
 */
app.factory('UserService', [
    'UserGetAllService',
    'UserGetByIdService',
    'UserCreateService',
    'UserUpdateService',
    'UserDeleteService',
function(UserGetAllService, UserGetByIdService, UserCreateService, UserUpdateService, UserDeleteService) {
    return {
        getAll: function() { return UserGetAllService.execute(); },
        getById: function(id) { return UserGetByIdService.execute(id); },
        create: function(data) { return UserCreateService.execute(data); },
        update: function(id, data) { return UserUpdateService.execute(id, data); },
        delete: function(id) { return UserDeleteService.execute(id); }
    };
}]);
