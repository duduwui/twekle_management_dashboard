/**
 * Time Filter Feature Service Facade
 * Delegates time filter operations to dedicated single-action micro-services.
 */
app.factory('TimeFilterService', [
    'TimeFilterGetAllService',
    'TimeFilterCreateService',
    'TimeFilterToggleService',
    'TimeFilterDeleteService',
function(TimeFilterGetAllService, TimeFilterCreateService, TimeFilterToggleService, TimeFilterDeleteService) {
    return {
        getAll: function() { return TimeFilterGetAllService.execute(); },
        create: function(data) { return TimeFilterCreateService.execute(data); },
        toggle: function(id) { return TimeFilterToggleService.execute(id); },
        delete: function(id) { return TimeFilterDeleteService.execute(id); }
    };
}]);
