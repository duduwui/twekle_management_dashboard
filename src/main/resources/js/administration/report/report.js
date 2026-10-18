/**
 * ============================================================================
 * Reports & Analytics Engine (report.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/report/report.js
 * Used by:  /WEB-INF/views/administration/report/reports.html
 * Route:    /admin/reports
 * 
 * Purpose:
 *   Handles analytics, calculations, and reporting tab transitions across:
 *   1. Milestones: Turnaround times, delay rates, and target day performance.
 *   2. Customer Satisfaction: Rating breakdowns (1-5 stars), positive review %,
 *      sentiment filters, and CSV data export.
 *   3. Product Insights: High/low rated products and customer feedback remarks.
 *   4. Follow-up Summary: Aggregated counts, overdue rates, and staff milestones.
 * ============================================================================
 */
angular.module('tweklApp')
.factory('ReportService', ['$http', function($http) {
    var BASE = '/admin/reports';

    function get(period, params) {
        var config = { params: params || {} };
        if (period) config.params.period = period;
        return $http.get(BASE, config);
    }

    return {
        getReports: function(period, params) { return get(period, params); },
        get: function(period, params) { return get(period, params); }
    };
}])
.controller('ReportController', [
    '$scope', '$http', 'ReportService', 'ToastService', 'FilterHelperService',
function($scope, $http, ReportService, ToastService, FilterHelperService) {

    $scope.loading = false;
    $scope.reportData = {
        milestones: [],
        satisfaction: { breakdown: [], totalRated: 0, positiveRate: 0, recentNotes: [] },
        products: [],
        customers: [],
        followups: []
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
        searchCustomers: '',
        searchFollowups: ''
    };

    $scope.showReportFilterModal = false;
    $scope.pendingReportFilters = {
        satisfaction: 'all',
        preset: null
    };

    var urlParams = new URLSearchParams(window.location.search);
    var tabParam = urlParams.get('tab');
    $scope.reportSubTab = tabParam || window.SERVER_REPORT_SUBTAB || ($scope.$parent && $scope.$parent.reportSubTab) || 'milestones';
    if ($scope.reportSubTab === 'customers') {
        $scope.reportSubTab = 'followups';
    }

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
        $scope.reportSubTab = (subTab === 'customers') ? 'followups' : subTab;
    });

    // Pagination states for each report table (40 rows per page default)
    $scope.dtMilestones = { currentPage: 1, pageSize: 40, totalItems: 0, totalPages: 1 };
    $scope.dtSatisfaction = { currentPage: 1, pageSize: 40, totalItems: 0, totalPages: 1 };
    $scope.dtProducts = { currentPage: 1, pageSize: 40, totalItems: 0, totalPages: 1 };
    $scope.dtCustomers = { currentPage: 1, pageSize: 40, totalItems: 0, totalPages: 1 };

    function toDateParam(d) {
        if (!d) return null;
        if (typeof d === 'string') return d.substring(0, 10);
        if (d instanceof Date) {
            var y = d.getFullYear();
            var m = ('0' + (d.getMonth() + 1)).slice(-2);
            var day = ('0' + d.getDate()).slice(-2);
            return y + '-' + m + '-' + day;
        }
        return null;
    }

    /**
     * Loads report data from backend
     */
    $scope.loadReportData = function() {
        $scope.loading = true;
        var period = $scope.selectedReportPreset ? ($scope.selectedReportPreset.presetKey || $scope.selectedReportPreset.name) : 'all';
        var params = {};
        if ($scope.reportFilters.dateFrom) {
            params.dateFrom = toDateParam($scope.reportFilters.dateFrom);
        }
        if ($scope.reportFilters.dateTo) {
            params.dateTo = toDateParam($scope.reportFilters.dateTo);
        }

        ReportService.getReports(period, params).then(function(res) {
            $scope.loading = false;
            var raw = res.data;
            var data = (raw && raw.data) ? raw.data : (raw || {});
            $scope.reportData.milestones = data.milestones || [];
            $scope.reportData.pendingContacts = data.pendingContacts || [];
            $scope.reportData.satisfaction = data.satisfaction || { breakdown: [], recentNotes: [] };
            $scope.reportData.products = data.products || [];
            $scope.reportData.customers = data.customers || [];
            $scope.reportData.followups = $scope.reportData.customers;

            // Reset to page 1 on filter or data reload
            $scope.dtMilestones.currentPage = 1;
            $scope.dtSatisfaction.currentPage = 1;
            $scope.dtProducts.currentPage = 1;
            $scope.dtCustomers.currentPage = 1;
        }, function(err) {
            $scope.loading = false;
            ToastService.error('Failed to load report analytics');
        });
    };

    $scope.applyReportDateFilter = function() {
        $scope.loadReportData();
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
    // FILTERED & PAGED SUB-TAB DATA HELPERS (40 rows per page)
    // =========================================================
    $scope.getFilteredReportPending = function() {
        var items = $scope.reportData.pendingContacts || [];
        var search = $scope.reportFilters.searchPending;
        if (!search) return items;
        return items.filter(function(p) {
            return FilterHelperService.searchMatch(JSON.stringify(p), search);
        });
    };

    $scope.getPagedReportPending = function() {
        var items = $scope.getFilteredReportPending();
        $scope.dtMilestones.totalItems = items.length;
        $scope.dtMilestones.totalPages = Math.max(1, Math.ceil(items.length / $scope.dtMilestones.pageSize));
        if ($scope.dtMilestones.currentPage > $scope.dtMilestones.totalPages) $scope.dtMilestones.currentPage = $scope.dtMilestones.totalPages;
        var start = ($scope.dtMilestones.currentPage - 1) * $scope.dtMilestones.pageSize;
        return items.slice(start, start + $scope.dtMilestones.pageSize);
    };

    $scope.getFilteredReportNotes = function() {
        var notes = ($scope.reportData && $scope.reportData.satisfaction && $scope.reportData.satisfaction.recentNotes) ? $scope.reportData.satisfaction.recentNotes : [];
        return notes.filter(function(n) {
            if ($scope.reportFilters.searchNotes && !FilterHelperService.searchMatch(JSON.stringify(n), $scope.reportFilters.searchNotes)) return false;
            if ($scope.reportFilters.satisfactionFilter && $scope.reportFilters.satisfactionFilter !== 'all' && n.satisfaction !== $scope.reportFilters.satisfactionFilter) return false;
            return true;
        });
    };

    $scope.getPagedReportNotes = function() {
        var items = $scope.getFilteredReportNotes();
        $scope.dtSatisfaction.totalItems = items.length;
        $scope.dtSatisfaction.totalPages = Math.max(1, Math.ceil(items.length / $scope.dtSatisfaction.pageSize));
        if ($scope.dtSatisfaction.currentPage > $scope.dtSatisfaction.totalPages) $scope.dtSatisfaction.currentPage = $scope.dtSatisfaction.totalPages;
        var start = ($scope.dtSatisfaction.currentPage - 1) * $scope.dtSatisfaction.pageSize;
        return items.slice(start, start + $scope.dtSatisfaction.pageSize);
    };

    $scope.getFilteredReportProducts = function() {
        var prods = $scope.reportData.products || [];
        return prods.filter(function(p) {
            if ($scope.reportFilters.searchProducts && !FilterHelperService.searchMatch(p.productName, $scope.reportFilters.searchProducts)) return false;
            return true;
        });
    };

    $scope.getPagedReportProducts = function() {
        var items = $scope.getFilteredReportProducts();
        $scope.dtProducts.totalItems = items.length;
        $scope.dtProducts.totalPages = Math.max(1, Math.ceil(items.length / $scope.dtProducts.pageSize));
        if ($scope.dtProducts.currentPage > $scope.dtProducts.totalPages) $scope.dtProducts.currentPage = $scope.dtProducts.totalPages;
        var start = ($scope.dtProducts.currentPage - 1) * $scope.dtProducts.pageSize;
        return items.slice(start, start + $scope.dtProducts.pageSize);
    };

    $scope.getFilteredReportCustomers = function() {
        var custs = $scope.reportData.followups || $scope.reportData.customers || [];
        var search = $scope.reportFilters.searchFollowups || $scope.reportFilters.searchCustomers;
        return custs.filter(function(c) {
            if (search && !FilterHelperService.searchMatch(JSON.stringify(c), search)) return false;
            return true;
        });
    };
    $scope.getFilteredReportFollowups = $scope.getFilteredReportCustomers;

    $scope.getPagedReportCustomers = function() {
        var items = $scope.getFilteredReportCustomers();
        $scope.dtCustomers.totalItems = items.length;
        $scope.dtCustomers.totalPages = Math.max(1, Math.ceil(items.length / $scope.dtCustomers.pageSize));
        if ($scope.dtCustomers.currentPage > $scope.dtCustomers.totalPages) $scope.dtCustomers.currentPage = $scope.dtCustomers.totalPages;
        var start = ($scope.dtCustomers.currentPage - 1) * $scope.dtCustomers.pageSize;
        return items.slice(start, start + $scope.dtCustomers.pageSize);
    };
    $scope.getPagedReportFollowups = $scope.getPagedReportCustomers;

    // Pagination navigation methods
    $scope.setReportPage = function(dt, page) {
        if (!dt || page < 1 || page > dt.totalPages) return;
        dt.currentPage = page;
    };

    $scope.getReportPageNumbers = function(dt) {
        if (!dt) return [];
        var current = dt.currentPage || 1;
        var total = dt.totalPages || 1;
        var pages = [];
        if (total <= 7) {
            for (var i = 1; i <= total; i++) pages.push(i);
        } else {
            pages.push(1);
            if (current > 3) pages.push('...');
            var start = Math.max(2, current - 1);
            var end = Math.min(total - 1, current + 1);
            for (var j = start; j <= end; j++) pages.push(j);
            if (current < total - 2) pages.push('...');
            pages.push(total);
        }
        return pages;
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

    // Hydrate immediately from server-side rendered data if present
    if (window.SERVER_REPORT_DATA) {
        var srd = window.SERVER_REPORT_DATA;
        $scope.reportData.milestones = srd.milestones || [];
        $scope.reportData.satisfaction = srd.satisfaction || { breakdown: [], recentNotes: [] };
        $scope.reportData.products = srd.products || [];
        $scope.reportData.customers = srd.customers || [];
        $scope.reportData.followups = $scope.reportData.customers;
    }

    // Initial data comes from the page model. No second request or REST fallback.
}]);
