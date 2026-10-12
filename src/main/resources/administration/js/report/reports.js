/**
 * Report Service
 * Manages dashboard analytics, turnaround times, and satisfaction reports.
 */
angular.module('tweklApp')
.factory('ReportService', ['$http', function($http) {
    var BASE = '/internal/reports';

    function get(period, params) {
        var config = { params: params || {} };
        if (period) config.params.period = period;
        return $http.get(BASE, config);
    }

    return {
        getReports: function(period, params) { return get(period, params); },
        get: function(period, params) { return get(period, params); }
    };
}]);

/**
 * Report Feature Controller
 * Manages Report Navigation, Milestone Turnaround, Satisfaction Scores, Product Insights, and Customer Summaries.
 */
angular.module('tweklApp')
.controller('ReportController', [
    '$scope', '$http', 'ReportService', 'ToastService', 'FilterHelperService',
function($scope, $http, ReportService, ToastService, FilterHelperService) {

    $scope.loading = false;
    $scope.reportData = {
        milestones: [],
        satisfaction: { breakdown: [], totalRated: 0, positiveRate: 0, recentNotes: [] },
        products: [],
        customers: []
    };

    // Default Date From: Today, Date To: Tomorrow / Next Day
    $scope.reportFilters = {
        dateFrom: FilterHelperService.getToday(),
        dateTo: FilterHelperService.getTomorrow(),
        satisfactionFilter: 'all',
        milestoneFilter: 'all',
        searchPending: '',
        searchNotes: '',
        searchProducts: '',
        searchCustomers: ''
    };

    $scope.showReportFilterModal = false;
    $scope.pendingReportFilters = {
        satisfaction: 'all',
        preset: null
    };

    var urlParams = new URLSearchParams(window.location.search);
    var tabParam = urlParams.get('tab');
    $scope.reportSubTab = tabParam || window.SERVER_REPORT_SUBTAB || ($scope.$parent && $scope.$parent.reportSubTab) || 'milestones';

    $scope.setReportSubTab = function(subTab) {
        $scope.reportSubTab = subTab;
        if ($scope.$parent && $scope.$parent.setReportSubTab) {
            $scope.$parent.setReportSubTab(subTab);
        } else if ($scope.$parent) {
            $scope.$parent.reportSubTab = subTab;
        }
        if (window.history && window.history.replaceState) {
            var newUrl = window.location.pathname + '?tab=' + encodeURIComponent(subTab);
            window.history.replaceState(null, '', newUrl);
        }
    };

    $scope.$on('reportSubTab:changed', function(event, subTab) {
        $scope.reportSubTab = subTab;
    });

    /**
     * Loads report data from backend
     */
    $scope.loadReportData = function() {
        $scope.loading = true;
        var period = $scope.selectedReportPreset ? ($scope.selectedReportPreset.presetKey || $scope.selectedReportPreset.name) : 'all';
        ReportService.getReports(period).then(function(res) {
            $scope.loading = false;
            var data = res.data || {};
            $scope.reportData.milestones = data.milestones || [];
            $scope.reportData.satisfaction = data.satisfaction || { breakdown: [], recentNotes: [] };
            $scope.reportData.products = data.products || [];
            $scope.reportData.customers = data.customers || [];
        }, function(err) {
            $scope.loading = false;
            ToastService.error('Failed to load report analytics');
        });
    };

    $scope.applyReportDateFilter = function() {
        // Date filtering handled by client-side filters
    };

    $scope.isReportFiltered = function() {
        return !!($scope.selectedReportPreset ||
            ($scope.reportFilters.satisfactionFilter && $scope.reportFilters.satisfactionFilter !== 'all') ||
            ($scope.reportFilters.milestoneFilter && $scope.reportFilters.milestoneFilter !== 'all') ||
            $scope.reportFilters.dateFrom || $scope.reportFilters.dateTo);
    };

    // =========================================================
    // REPORT POPUP MODAL FILTERS
    // =========================================================
    $scope.openReportFilterModal = function() {
        if ($scope.modalState) {
            $scope.modalState.reportFilter = true;
        }
        $scope.$emit('modal:openReportFilter', {
            preset: $scope.selectedReportPreset,
            satisfaction: $scope.reportFilters.satisfactionFilter || 'all'
        });
        if ($scope.$parent && $scope.$parent.openReportFilterModal) {
            $scope.$parent.openReportFilterModal();
        }
    };

    $scope.closeReportFilterModal = function() {
        if ($scope.modalState) {
            $scope.modalState.reportFilter = false;
        }
        $scope.showReportFilterModal = false;
        if ($scope.$parent && $scope.$parent.closeReportFilterModal) {
            $scope.$parent.closeReportFilterModal();
        }
    };

    $scope.resetReportFilterModal = function() {
        $scope.pendingReportFilters = { satisfaction: 'all', preset: null };
        $scope.selectedReportPreset = null;
        $scope.reportFilters.satisfactionFilter = 'all';
        $scope.reportFilters.milestoneFilter = 'all';
        $scope.reportFilters.dateFrom = null;
        $scope.reportFilters.dateTo = null;
        $scope.closeReportFilterModal();
        $scope.loadReportData();
        if ($scope.$parent && $scope.$parent.resetReportFilterModal) {
            $scope.$parent.resetReportFilterModal();
        }
    };

    $scope.selectPendingReportPreset = function(preset) {
        $scope.pendingReportFilters.preset = preset;
        if ($scope.$parent && $scope.$parent.selectPendingReportPreset) {
            $scope.$parent.selectPendingReportPreset(preset);
        }
    };

    $scope.isPendingReportPresetSelected = function(preset) {
        if (!preset && !$scope.pendingReportFilters.preset) return true;
        if (!preset || !$scope.pendingReportFilters.preset) return false;
        return preset.id === $scope.pendingReportFilters.preset.id;
    };

    $scope.confirmApplyReportFilterModal = function() {
        $scope.selectedReportPreset = $scope.pendingReportFilters.preset;
        $scope.reportFilters.satisfactionFilter = $scope.pendingReportFilters.satisfaction;
        $scope.closeReportFilterModal();
        $scope.loadReportData();
        if ($scope.$parent && $scope.$parent.confirmApplyReportFilterModal) {
            $scope.$parent.confirmApplyReportFilterModal();
        }
    };

    $scope.selectReportPreset = function(preset) {
        $scope.selectedReportPreset = preset;
        $scope.$emit('modal:syncReportPreset', preset);
        $scope.loadReportData();
    };

    $scope.clearSatisfactionFilter = function() {
        $scope.reportFilters.satisfactionFilter = 'all';
        $scope.$emit('modal:syncSatisfactionFilter', 'all');
    };

    // =========================================================
    // FILTERED SUB-TAB DATA HELPERS
    // =========================================================
    $scope.getFilteredReportNotes = function() {
        var notes = ($scope.reportData && $scope.reportData.satisfaction && $scope.reportData.satisfaction.recentNotes) ? $scope.reportData.satisfaction.recentNotes : [];
        return notes.filter(function(n) {
            if ($scope.reportFilters.searchNotes && !FilterHelperService.searchMatch(JSON.stringify(n), $scope.reportFilters.searchNotes)) return false;
            if ($scope.reportFilters.satisfactionFilter && $scope.reportFilters.satisfactionFilter !== 'all' && n.satisfaction !== $scope.reportFilters.satisfactionFilter) return false;
            return true;
        });
    };

    $scope.getFilteredReportProducts = function() {
        var prods = $scope.reportData.products || [];
        return prods.filter(function(p) {
            if ($scope.reportFilters.searchProducts && !FilterHelperService.searchMatch(p.productName, $scope.reportFilters.searchProducts)) return false;
            return true;
        });
    };

    $scope.getFilteredReportCustomers = function() {
        var custs = $scope.reportData.customers || [];
        return custs.filter(function(c) {
            if ($scope.reportFilters.searchCustomers && !FilterHelperService.searchMatch(JSON.stringify(c), $scope.reportFilters.searchCustomers)) return false;
            return true;
        });
    };

    $scope.exportReportCsv = function() {
        ToastService.info('Exporting CSV report...');
    };

    // Auto-reload when time filter presets change or login occurs
    $scope.$on('timePresets:changed', function() {
        $scope.loadReportData();
    });

    $scope.$on('report:reload', function() {
        $scope.loadReportData();
    });

    // Listen for report filter changes from LayoutController
    $scope.$on('filter:reportFiltersChanged', function(event, data) {
        if (data) {
            $scope.selectedReportPreset = data.preset;
            $scope.reportFilters.satisfactionFilter = data.satisfaction || 'all';
        }
        $scope.loadReportData();
    });

    // Auto-load on init
    $scope.loadReportData();
}]);
