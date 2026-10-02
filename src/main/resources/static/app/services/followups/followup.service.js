/**
 * Follow-up Feature Service Facade
 * Delegates follow-up checkpoint operations to dedicated single-action micro-services.
 */
app.factory('FollowupService', [
    'FollowupGetService',
    'FollowupCreateService',
    'FollowupUpdateService',
    'FollowupToggleStatusService',
function(FollowupGetService, FollowupCreateService, FollowupUpdateService, FollowupToggleStatusService) {
    return {
        getFollowups: function(orderId) { return FollowupGetService.execute(orderId); },
        createFollowup: function(orderId, data) { return FollowupCreateService.execute(orderId, data); },
        updateFollowup: function(id, data) { return FollowupUpdateService.execute(id, data); },
        toggleStatus: function(id) { return FollowupToggleStatusService.execute(id); }
    };
}]);
