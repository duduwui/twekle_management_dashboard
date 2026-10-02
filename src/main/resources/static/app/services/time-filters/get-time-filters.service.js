/**
 * Time Filter Action Service: Get Time Filters
 * Fetches all dynamic time filter presets.
 * Endpoint: GET /internal/time-filters
 */
app.factory('TimeFilterGetAllService', ['$http', function($http) {
    return {
        /**
         * @returns {Promise} Resolves with array of TimeFilterPreset objects
         */
        execute: function() {
            return $http.get('/internal/time-filters').catch(function() {
                return $http.get('/api/time-filters');
            });
        }
    };
}]);
