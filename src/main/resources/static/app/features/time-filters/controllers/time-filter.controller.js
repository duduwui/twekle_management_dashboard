/**
 * Time Filter Feature Controller
 * Manages Dynamic Time Filter Presets, Creation, Status Toggling, and Deletion.
 */
angular.module('tweklApp')
.controller('TimeFilterController', [
    '$scope', 'TimeFilterGetAllService', 'TimeFilterCreateService', 'TimeFilterToggleService', 'TimeFilterDeleteService', 'ToastService', 'FilterHelperService',
function($scope, TimeFilterGetAllService, TimeFilterCreateService, TimeFilterToggleService, TimeFilterDeleteService, ToastService, FilterHelperService) {

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
        TimeFilterGetAllService.execute().then(function(res) {
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

        var payload = {
            name: $scope.newPreset.name,
            durationValue: $scope.newPreset.durationValue,
            durationUnit: $scope.newPreset.durationUnit,
            isActive: true
        };

        TimeFilterCreateService.execute(payload).then(function() {
            ToastService.success('Time filter preset created!');
            $scope.newPreset = { name: '', durationValue: 24, durationUnit: 'HOURS', isActive: true };
            $scope.loadPresets();
        }, function(err) {
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to create preset');
        });
    };

    $scope.toggleTimeFilterPreset = function(preset) {
        TimeFilterToggleService.execute(preset.id).then(function() {
            preset.isActive = !preset.isActive;
            ToastService.success('Preset updated');
        }, function() {
            ToastService.error('Failed to toggle preset');
        });
    };

    $scope.deleteTimeFilterPreset = function(preset) {
        if (!preset || !preset.id) return;
        TimeFilterDeleteService.execute(preset.id).then(function() {
            ToastService.success('Preset deleted');
            $scope.loadPresets();
        }, function() {
            ToastService.error('Failed to delete preset');
        });
    };

    $scope.loadPresets();
}]);
