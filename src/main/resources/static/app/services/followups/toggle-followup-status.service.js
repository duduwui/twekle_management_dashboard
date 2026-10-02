/**
 * Follow-up Action Service: Toggle Status
 * Toggles a checkpoint status between COMPLETED (true) and PENDING (false).
 * Endpoint: PATCH /internal/followups/{id}/status
 */
app.factory('FollowupToggleStatusService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Follow-up Check ID
         * @returns {Promise} Resolves with updated status
         */
        execute: function(id) {
            return $http.patch('/internal/followups/' + id + '/status', {}).catch(function() {
                return $http.patch('/api/followups/' + id + '/status', {});
            });
        }
    };
}]);
