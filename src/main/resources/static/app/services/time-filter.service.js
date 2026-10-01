/**
 * Dynamic Time Filter Presets Data Service
 */
app.factory('TimeFilterService', ['$http', function($http) {
    var BASE = '/internal/time-filters';
    var FALLBACK = '/api/time-filters';

    return {
        getAll: function() {
            return $http.get(BASE).catch(function() {
                return $http.get(FALLBACK);
            });
        },
        create: function(data) {
            return $http.post(BASE, data).catch(function() {
                return $http.post(FALLBACK, data);
            });
        },
        toggle: function(id) {
            return $http.patch(BASE + '/' + id + '/toggle', {}).catch(function() {
                return $http.patch(FALLBACK + '/' + id + '/toggle', {});
            });
        },
        delete: function(id) {
            return $http.delete(BASE + '/' + id).catch(function() {
                return $http.delete(FALLBACK + '/' + id);
            });
        }
    };
}]);
