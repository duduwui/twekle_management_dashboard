/**
 * ============================================================================
 * Time Filter Presets Service (timefilter.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/timefilter/timefilter.js
 * Used by:  /WEB-INF/views/administration/timefilter/timeFilters.html & Filter Modals
 * Route:    /admin/timelines
 * 
 * Purpose:
 *   Handles Dynamic Time Filter presets management:
 *   1. TimeFilterService: API client for querying available presets (24 Hours, 7 Days, etc.).
 *   2. Provides client helpers for creating presets, toggling active status, and deletion.
 * ============================================================================
 */
angular.module('tweklApp')
.factory('TimeFilterService', ['$http', function($http) {
    var BASE = '/admin/time-filters';

    function request(method, path, data) {
        return $http({ method: method, url: BASE + (path || ''), data: data });
    }

    return {
        getAll: function() { return request('GET'); },
        create: function(data) { return request('POST', '', data); },
        toggle: function(id) { return request('PATCH', '/' + id + '/toggle'); },
        delete: function(id) { return request('DELETE', '/' + id); }
    };
}]);

function filterPresetTable() {
    var input = document.getElementById("presetSearchInput");
    if (!input) return;
    var filter = input.value.toLowerCase();
    var rows = document.querySelectorAll("#presetTable tbody tr.preset-row");
    rows.forEach(function(row) {
        var text = row.innerText.toLowerCase();
        row.style.display = text.indexOf(filter) > -1 ? "" : "none";
    });
}
