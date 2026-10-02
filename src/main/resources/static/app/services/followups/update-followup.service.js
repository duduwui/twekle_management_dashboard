/**
 * Follow-up Action Service: Update Follow-up
 * Updates notes, photos, or feedback details for an existing checkpoint.
 * Endpoint: PUT /internal/followups/{id}
 */
app.factory('FollowupUpdateService', ['$http', function($http) {
    return {
        /**
         * @param {number|string} id - Follow-up Check ID
         * @param {Object} data - Updated fields (notes, photoUrl, etc.)
         * @returns {Promise} Resolves with updated checkpoint
         */
        execute: function(id, data) {
            return $http.put('/internal/followups/' + id, data).catch(function() {
                return $http.put('/api/followups/' + id, data);
            });
        }
    };
}]);
