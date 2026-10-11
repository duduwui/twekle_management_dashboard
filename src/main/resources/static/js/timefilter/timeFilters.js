/**
 * Time Filter Service
 * Manages dynamic time filter presets and status toggling.
 * Endpoints: /internal/time-filters (fallback: /api/time-filters)
 */
angular.module('tweklApp')
.factory('TimeFilterService', ['$http', function($http) {
    var BASE = '/internal/time-filters';
    var FALLBACK = '/api/time-filters';

    function request(method, path, data) {
        var url = BASE + (path || '');
        var fallbackUrl = FALLBACK + (path || '');
        var config = { method: method, url: url, data: data };
        return $http(config).catch(function() {
            return $http({ method: method, url: fallbackUrl, data: data });
        });
    }

    return {
        getAll: function() { return request('GET'); },
        create: function(data) { return request('POST', '', data); },
        toggle: function(id) { return request('PATCH', '/' + id + '/toggle'); },
        delete: function(id) { return request('DELETE', '/' + id); }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('TimeFilterGetAllService', ['TimeFilterService', function(TimeFilterService) {
    return { execute: function() { return TimeFilterService.getAll(); } };
}])
.factory('TimeFilterCreateService', ['TimeFilterService', function(TimeFilterService) {
    return { execute: function(data) { return TimeFilterService.create(data); } };
}])
.factory('TimeFilterToggleService', ['TimeFilterService', function(TimeFilterService) {
    return { execute: function(id) { return TimeFilterService.toggle(id); } };
}])
.factory('TimeFilterDeleteService', ['TimeFilterService', function(TimeFilterService) {
    return { execute: function(id) { return TimeFilterService.delete(id); } };
}]);

/**
 * Time Filter Feature Controller
 * Manages Dynamic Time Filter Presets, Creation, Status Toggling, and Deletion.
 */
angular.module('tweklApp')
.controller('TimeFilterController', [
    '$scope', '$rootScope', 'TimeFilterService', 'ToastService', 'FilterHelperService',
function($scope, $rootScope, TimeFilterService, ToastService, FilterHelperService) {

    // Table pagination & sorting
    $scope.filterRulesDt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: false,
        totalItems: 0
    };

    $scope.filterRulesSearch = '';

    $scope.newPreset = {
        name: '',
        durationValue: 24,
        durationUnit: 'HOURS',
        isActive: true
    };

    /**
     * Loads all preset rules from backend
     */
    $scope.loadPresets = function() {
        TimeFilterService.getAll().then(function(res) {
            var arr = res.data || [];
            arr.sort(function(a, b) {
                var minA = FilterHelperService.toDurationMinutes(a);
                var minB = FilterHelperService.toDurationMinutes(b);
                if (minA !== minB) return minA - minB;
                return (a.id || 0) - (b.id || 0);
            });
            $scope.timeFilterPresets = arr;
        }, function() {
            ToastService.error('Failed to load time filter presets');
        });
    };

    // =========================================================
    // TABLE PAGINATION & SORTING HELPERS
    // =========================================================
    $scope.getFilteredFilterRules = function() {
        var items = $scope.timeFilterPresets || [];
        if (!$scope.filterRulesSearch) return items;
        return items.filter(function(item) {
            return FilterHelperService.searchMatch(JSON.stringify(item), $scope.filterRulesSearch);
        });
    };

    $scope.getPagedFilterRules = function() {
        var filtered = $scope.getFilteredFilterRules();
        var start = ($scope.filterRulesDt.currentPage - 1) * $scope.filterRulesDt.pageSize;
        return filtered.slice(start, start + $scope.filterRulesDt.pageSize);
    };

    $scope.sortFilterRulesBy = function(field) {
        if ($scope.filterRulesDt.sortField === field) {
            $scope.filterRulesDt.sortReverse = !$scope.filterRulesDt.sortReverse;
        } else {
            $scope.filterRulesDt.sortField = field;
            $scope.filterRulesDt.sortReverse = false;
        }
    };

    $scope.getFilterRuleSortIndicator = function(field) {
        if ($scope.filterRulesDt.sortField !== field) return '';
        return $scope.filterRulesDt.sortReverse ? '▼' : '▲';
    };

    $scope.getTotalFilterRulePages = function(totalItems) {
        return Math.ceil((totalItems || 1) / $scope.filterRulesDt.pageSize) || 1;
    };

    $scope.getFilterRulePageNumbers = function(totalItems) {
        var total = $scope.getTotalFilterRulePages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    $scope.setFilterRulePage = function(page) {
        if (page >= 1 && page <= $scope.getTotalFilterRulePages($scope.getFilteredFilterRules().length)) {
            $scope.filterRulesDt.currentPage = page;
        }
    };

    // =========================================================
    // CRUD ACTIONS
    // =========================================================
    $scope.createTimeFilterPreset = function() {
        if (!$scope.newPreset.name || !$scope.newPreset.durationValue) {
            ToastService.error('Please provide preset name and duration value');
            return;
        }

        var presetKey = $scope.newPreset.name.trim()
            .toUpperCase()
            .replace(/[^A-Z0-9]+/g, '_')
            .replace(/^_|_$/g, '');

        var payload = {
            presetKey: presetKey,
            labelEn: $scope.newPreset.name.trim(),
            durationValue: parseInt($scope.newPreset.durationValue, 10),
            durationUnit: $scope.newPreset.durationUnit,
            active: true,
            displayOrder: 0
        };

        TimeFilterService.create(payload).then(function() {
            ToastService.success('Time filter preset created!');
            $scope.newPreset = { name: '', durationValue: 24, durationUnit: 'HOURS' };
            $scope.loadPresets();
            $rootScope.$broadcast('timePresets:changed'); // reload global presets in layout
        }, function(err) {
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to create preset');
        });
    };

    $scope.toggleTimeFilterPreset = function(preset) {
        TimeFilterService.toggle(preset.id).then(function(res) {
            // Reload to get fresh active state from server
            $scope.loadPresets();
            $rootScope.$broadcast('timePresets:changed');
            ToastService.success('Preset updated');
        }, function() {
            ToastService.error('Failed to toggle preset');
        });
    };

    $scope.deleteTimeFilterPreset = function(preset) {
        if (!preset || !preset.id) return;
        TimeFilterService.delete(preset.id).then(function() {
            ToastService.success('Preset deleted');
            $scope.loadPresets();
            $rootScope.$broadcast('timePresets:changed'); // remove from global filter modal
        }, function() {
            ToastService.error('Failed to delete preset');
        });
    };

    $scope.$on('auth:loggedIn', function() {
        $scope.loadPresets();
    });

    $scope.loadPresets();
}]);
