/**
 * Report Feature Controller
 * Manages Report Navigation, Milestone Turnaround, Satisfaction Scores, Product Insights, and Customer Summaries.
 */
angular.module('tweklApp')
.controller('ReportController', [
    '$scope', '$http', 'ReportGetService', 'ToastService', 'FilterHelperService',
function($scope, $http, ReportGetService, ToastService, FilterHelperService) {

    $scope.loading = false;
    $scope.reportData = {
        milestones: [],
        satisfaction: { breakdown: [], totalRated: 0, positiveRate: 0, recentNotes: [] },
        products: [],
        customers: []
    };

    $scope.reportFilters = {
        dateFrom: null,
        dateTo: null,
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

    /**
     * Loads report data from backend
     */
    $scope.loadReportData = function() {
        $scope.loading = true;
        ReportGetService.execute('all').then(function(res) {
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
        $scope.pendingReportFilters = {
            satisfaction: $scope.reportFilters.satisfactionFilter || 'all',
            preset: $scope.selectedReportPreset
        };
        $scope.showReportFilterModal = true;
    };

    $scope.closeReportFilterModal = function() {
        $scope.showReportFilterModal = false;
    };

    $scope.resetReportFilterModal = function() {
        $scope.pendingReportFilters = { satisfaction: 'all', preset: null };
        $scope.selectedReportPreset = null;
        $scope.reportFilters.satisfactionFilter = 'all';
        $scope.reportFilters.milestoneFilter = 'all';
        $scope.reportFilters.dateFrom = null;
        $scope.reportFilters.dateTo = null;
        $scope.closeReportFilterModal();
    };

    $scope.selectPendingReportPreset = function(preset) {
        $scope.pendingReportFilters.preset = preset;
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
    };

    $scope.selectReportPreset = function(preset) {
        $scope.selectedReportPreset = preset;
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

    // Auto-load on init
    $scope.loadReportData();
}]);
