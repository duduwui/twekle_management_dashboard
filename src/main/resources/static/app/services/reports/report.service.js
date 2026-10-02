/**
 * Report Feature Service Facade
 * Delegates reporting operations to dedicated single-action micro-services.
 */
app.factory('ReportService', ['ReportGetService', function(ReportGetService) {
    return {
        get: function(period, params) { return ReportGetService.execute(period, params); }
    };
}]);
