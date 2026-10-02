/**
 * Customer Feature Service Facade
 * Delegates customer operations to dedicated single-action micro-services.
 */
app.factory('CustomerService', [
    'CustomerGetAllService',
    'CustomerGetByIdService',
    'CustomerGetOrdersService',
    'CustomerGetStatsService',
function(CustomerGetAllService, CustomerGetByIdService, CustomerGetOrdersService, CustomerGetStatsService) {
    return {
        getAll: function(timeFilter) { return CustomerGetAllService.execute(timeFilter); },
        getById: function(id) { return CustomerGetByIdService.execute(id); },
        getOrders: function(customerId) { return CustomerGetOrdersService.execute(customerId); },
        getStats: function() { return CustomerGetStatsService.execute(); }
    };
}]);
