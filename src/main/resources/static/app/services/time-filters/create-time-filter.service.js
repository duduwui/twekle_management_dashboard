/**
 * Time Filter Action Service: Create Time Filter
 * Saves a new dynamic time filter preset.
 * Endpoint: POST /internal/time-filters
 */
app.factory('TimeFilterCreateService', ['$http', function($http) {
    return {
        /**
         * @param {Object} data - TimeFilterPreset payload {presetKey, labelEn, labelAr, labelKu, durationValue, durationUnit}
         * @returns {Promise} Resolves with created preset
         */
        execute: function(data) {
            return $http.post('/internal/time-filters', data).catch(function() {
                return $http.post('/api/time-filters', data);
            });
        }
    };
}]);
