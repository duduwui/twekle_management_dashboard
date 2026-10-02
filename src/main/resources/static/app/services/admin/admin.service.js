/**
 * Admin Feature Service Facade
 * Delegates all admin operations to dedicated single-action micro-services.
 */
app.factory('AdminService', [
    'AdminGetAllService',
    'AdminGetByIdService',
    'AdminCreateService',
    'AdminUpdateService',
    'AdminDeleteService',
function(AdminGetAllService, AdminGetByIdService, AdminCreateService, AdminUpdateService, AdminDeleteService) {
    return {
        getAll: function() { return AdminGetAllService.execute(); },
        getById: function(id) { return AdminGetByIdService.execute(id); },
        create: function(data) { return AdminCreateService.execute(data); },
        update: function(id, data) { return AdminUpdateService.execute(id, data); },
        delete: function(id) { return AdminDeleteService.execute(id); }
    };
}]);
