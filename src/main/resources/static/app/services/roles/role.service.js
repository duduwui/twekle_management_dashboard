/**
 * Role Feature Service Facade
 * Delegates role template operations to dedicated single-action micro-services.
 */
app.factory('RoleService', [
    'RoleGetAllService',
    'RoleGetByIdService',
    'RoleCreateService',
    'RoleUpdateService',
    'RoleDeleteService',
function(RoleGetAllService, RoleGetByIdService, RoleCreateService, RoleUpdateService, RoleDeleteService) {
    return {
        getAll: function() { return RoleGetAllService.execute(); },
        getById: function(id) { return RoleGetByIdService.execute(id); },
        create: function(data) { return RoleCreateService.execute(data); },
        update: function(id, data) { return RoleUpdateService.execute(id, data); },
        delete: function(id) { return RoleDeleteService.execute(id); }
    };
}]);
