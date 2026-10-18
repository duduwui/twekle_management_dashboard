/**
 * ============================================================================
 * Follow-up Feature & Milestone Checklist (followup.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/followup/followup.js
 * Used by:  /WEB-INF/views/administration/followup/followups.html
 * Route:    /admin/followups
 * 
 * Purpose:
 *   Consolidated AngularJS service and controller for customer follow-up tracking:
 *   1. FollowupService: API interface for fetching customer orders, checks,
 *      milestone checklists, toggling statuses, and posting satisfaction reviews.
 *   2. FollowupController:
 *      - Customer directory table with real-time status pills (ALERT, PENDING, DONE).
 *      - Customer purchase orders history accordion view.
 *      - Milestone inspection and checklist modal (editing check notes & photos).
 *      - Time range and milestone completion filter controls.
 * ============================================================================
 */
/* =========================================================
   FOLLOWUP CONTROLLER (MAIN FEATURE COORDINATOR)
   ========================================================= */
var followupControllerDefinition = [
    '$scope', '$http', '$timeout', '$window', '$interval', 'FollowupService', 'ToastService', 'FilterHelperService',
function($scope, $http, $timeout, $window, $interval, FollowupService, ToastService, FilterHelperService) {

    // Active sub-view: 'list' | 'orders' | 'feedback'
    $scope.customerView = 'list';
    $scope.customers = [];
    $scope.selectedCustomer = null;
    $scope.customerOrders = [];
    $scope.orderFollowups = {}; // map: orderId -> [OrderFollowupCheck]
    $scope.expandedOrderId = null;
    $scope.targetMilestoneHighlight = null;

    var pendingTargetCustomerId = null;

    // Table pagination & sorting (Customer List) - Default 40 rows per page
    $scope.dt = {
        currentPage: 1,
        pageSize: 40,
        sortField: 'id',
        sortReverse: false,
        totalItems: window.SERVER_TOTAL_CUSTOMERS || 0,
        totalPages: window.SERVER_TOTAL_PAGES || 1
    };

    // Filter criteria (Customer List) - Default Today/Tomorrow displayed, dateFilterApplied: false on initial load
    $scope.customerFilters = {
        search: '',
        dateFrom: FilterHelperService.getToday(),
        dateTo: FilterHelperService.getTomorrow(),
        dateFilterApplied: false,
        followupStatus: 'ALL'
    };

    // Table pagination & sorting (Customer Orders)
    $scope.orderDt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: true,
        totalItems: 0
    };

    // Filter criteria (Customer Orders) - Default to all orders without date restriction
    $scope.orderFilters = {
        search: '',
        dateFrom: null,
        dateTo: null,
        dateFilterApplied: false
    };

    // Modals state
    $scope.showEditFollowupModal = false;
    $scope.editingFollowup = {};
    $scope.showAddFeedbackModal = false;
    $scope.newCustomerFeedback = { content: '', satisfaction: null, imageUrl: '', authorName: '', orderId: null };
    $scope.showEditFeedbackModal = false;
    $scope.editingCustomerFeedback = {};

    /**
     * Resolves initial sub-view and customer from URL pathname on page refresh
     */
    function initFollowupRoute() {
        var path = window.location.pathname;
        if (path.indexOf('/admin/followups/orders/') === 0 || path.indexOf('/admin/customers/orders/') === 0) {
            var parts = path.split('/');
            var targetId = parts[4] ? parseInt(parts[4], 10) : null;
            if (targetId) {
                $scope.customerView = 'orders';
                pendingTargetCustomerId = targetId;
                FollowupService.getById(targetId).then(function(res) {
                    var c = res.data ? (res.data.data || res.data) : null;
                    if (c) {
                        $scope.selectedCustomer = c;
                    }
                });
                $scope.loadCustomerOrders(targetId);
            }
        } else if (path.indexOf('/admin/followups/feedback/') === 0 || path.indexOf('/admin/customers/feedback/') === 0) {
            var parts = path.split('/');
            var targetId = parts[4] ? parseInt(parts[4], 10) : null;
            if (targetId) {
                $scope.customerView = 'feedback';
                pendingTargetCustomerId = targetId;
                FollowupService.getById(targetId).then(function(res) {
                    var c = res.data ? (res.data.data || res.data) : null;
                    if (c) {
                        $scope.selectedCustomer = c;
                    }
                });
                $scope.loadCustomerOrders(targetId);
            }
        } else {
            $scope.customerView = 'list';
        }

        if (window.location.search) {
            var match = window.location.search.match(/status=([^&]+)/i);
            if (match && match[1]) {
                $scope.customerFilters.followupStatus = decodeURIComponent(match[1]).toUpperCase();
            }
        }
    }

    /**
     * Set active customer view ('list', 'orders', 'feedback')
     */
    $scope.setCustomerView = function(view, cust) {
        $scope.customerView = view;
        if (view === 'orders' || view === 'feedback') {
            $scope.clearOrderDateFilter();
            $scope.orderFilters.search = '';
        }
        if (cust) {
            $scope.selectedCustomer = cust;
            $scope.loadCustomerOrders(cust.id);
        }
        if (view === 'list') {
            $scope.selectedCustomer = null;
            $scope.customerOrders = [];
            $scope.expandedOrderId = null;
            $scope.targetMilestoneHighlight = null;
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/followups');
        } else if (view === 'orders' && cust) {
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/followups/orders/' + cust.id);
        } else if (view === 'feedback' && cust) {
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/followups/feedback/' + cust.id);
        }
    };

    $scope.loadingCustomers = false;

    var searchTimer = null;
    $scope.onSearchChange = function() {
        if (searchTimer) $timeout.cancel(searchTimer);
        searchTimer = $timeout(function() {
            $scope.dt.currentPage = 1;
            $scope.loadCustomers(1);
        }, 300);
    };

    /**
     * Loads customer directory page (40 items) from backend.
     * Replaces current page in memory so memory consumption is always tiny and bounded.
     */
    $scope.loadCustomers = function(targetPage, silent) {
        if (targetPage !== undefined) {
            $scope.dt.currentPage = targetPage;
        }
        if (!silent) {
            $scope.loadingCustomers = true;
        }
        var preset = $scope.selectedTimePreset ? ($scope.selectedTimePreset.presetKey || $scope.selectedTimePreset.name) : null;
        
        var formatISODate = function(d) {
            if (!d) return null;
            if (typeof d === 'string') return d.substring(0, 10);
            if (d instanceof Date && !isNaN(d.getTime())) {
                var y = d.getFullYear();
                var m = ('0' + (d.getMonth() + 1)).slice(-2);
                var day = ('0' + d.getDate()).slice(-2);
                return y + '-' + m + '-' + day;
            }
            return null;
        };

        var params = {
            page: $scope.dt.currentPage || 1,
            size: $scope.dt.pageSize || 40,
            timeFilter: preset || 'ALL',
            status: $scope.customerFilters.followupStatus || 'ALL',
            search: $scope.customerFilters.search || '',
            sortField: $scope.dt.sortField || 'id',
            sortDir: $scope.dt.sortReverse ? 'asc' : 'desc'
        };

        if ($scope.customerFilters.dateFilterApplied) {
            var sDate = formatISODate($scope.customerFilters.dateFrom);
            var eDate = formatISODate($scope.customerFilters.dateTo);
            if (sDate) params.startDate = sDate;
            if (eDate) params.endDate = eDate;
        }

        FollowupService.getPaged(params).then(function(res) {
            $scope.loadingCustomers = false;
            var responseData = res.data && res.data.data ? res.data.data : res.data;
            if (!responseData) return;

            var list = responseData.content || [];
            $scope.dt.totalItems = responseData.totalElements != null ? responseData.totalElements : list.length;
            $scope.dt.totalPages = responseData.totalPages || Math.ceil($scope.dt.totalItems / $scope.dt.pageSize) || 1;
            $scope.dt.currentPage = responseData.currentPage || $scope.dt.currentPage || 1;

            var now = Date.now();
            $scope.customers = list.map(function(summary) {
                var c = summary.customer || summary;
                return angular.extend({}, c, {
                    followupStatus: summary.followupStatus,
                    nextPendingFollowup: summary.nextPendingFollowup,
                    nextPendingOrderId: summary.nextPendingOrderId,
                    allFollowupsCompleted: summary.allFollowupsCompleted,
                    remainingFollowupsCount: summary.remainingFollowupsCount,
                    nextPendingMinutesLeft: summary.nextPendingMinutesLeft,
                    _loadedAt: now
                });
            });

            if (pendingTargetCustomerId && ($scope.customerView === 'orders' || $scope.customerView === 'feedback')) {
                var cust = $scope.customers.find(function(c) { return c.id === pendingTargetCustomerId; });
                if (cust) {
                    $scope.selectedCustomer = cust;
                    $scope.loadCustomerOrders(cust.id);
                }
                pendingTargetCustomerId = null;
            }
        }, function(err) {
            $scope.loadingCustomers = false;
            if (!silent) {
                ToastService.error('Failed to load customers');
            }
        });
    };

    $scope.onDateFilterChange = function() {
        $scope.customerFilters.dateFilterApplied = true;
        $scope.dt.currentPage = 1;
        $scope.loadCustomers(1);
    };

    $scope.clearDateFilter = function() {
        $scope.customerFilters.dateFilterApplied = false;
        $scope.customerFilters.dateFrom = null;
        $scope.customerFilters.dateTo = null;
        $scope.dt.currentPage = 1;
        $scope.loadCustomers(1);
    };

    /**
     * Returns a live-adjusted countdown string for a customer's next pending followup.
     */
    $scope.getLiveCountdown = function(cust) {
        if (!cust || cust.nextPendingMinutesLeft == null) return '';
        var elapsedMins = cust._loadedAt ? Math.floor((Date.now() - cust._loadedAt) / 60000) : 0;
        var mins = cust.nextPendingMinutesLeft - elapsedMins;

        if (mins >= 0) {
            if (mins < 60) return mins + 'm';
            var hours = Math.floor(mins / 60);
            if (hours < 24) return hours + 'h ' + (mins % 60) + 'm';
            var days = Math.floor(hours / 24);
            var remH = hours % 24;
            return remH > 0 ? (days + 'd ' + remH + 'h') : (days + 'd');
        } else {
            var overdue = Math.abs(mins);
            if (overdue < 60) return overdue + 'm overdue';
            var ovH = Math.floor(overdue / 60);
            if (ovH < 24) return ovH + 'h overdue';
            var ovD = Math.floor(ovH / 24);
            var ovRH = ovH % 24;
            return ovRH > 0 ? (ovD + 'd ' + ovRH + 'h overdue') : (ovD + 'd overdue');
        }
    };

    // Tick every 60 seconds to refresh countdown displays without re-fetching
    var _countdownTick = $interval(function() {
        if ($scope.customers && $scope.customers.length) {
            $scope._tick = ($scope._tick || 0) + 1;
        }
    }, 60000);

    // Auto-sync polling every 5 seconds on Page 1 without active search or pending loading
    var _autoSyncInterval = $interval(function() {
        if ($scope.customerView === 'list' && $scope.dt.currentPage === 1 && !$scope.loadingCustomers && (!$scope.customerFilters.search || !$scope.customerFilters.search.trim())) {
            $scope.loadCustomers(1, true);
        }
    }, 5000);

    // Clean up interval when controller is destroyed
    $scope.$on('$destroy', function() {
        $interval.cancel(_countdownTick);
        $interval.cancel(_autoSyncInterval);
    });

    // Reload customers and clear cached followups when auth or time presets change
    $scope.$on('auth:loggedIn', function() {
        $scope.orderFollowups = {};
        $scope.loadCustomers();
    });

    $scope.$on('timePresets:changed', function() {
        $scope.orderFollowups = {};
        $scope.loadCustomers();
        if ($scope.selectedCustomer) {
            $scope.loadCustomerOrders($scope.selectedCustomer.id);
        }
    });

    /**
     * Loads orders for a specific customer
     */
    $scope.loadingOrders = false;
    $scope.loadCustomerOrders = function(customerId) {
        if (!customerId) return;
        $scope.loadingOrders = true;
        FollowupService.getOrders(customerId).then(function(res) {
            $scope.loadingOrders = false;
            var raw = res.data;
            var list = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
            $scope.customerOrders = list;
            if ($scope.expandedOrderId) {
                $scope.loadOrderFollowups($scope.expandedOrderId);
            } else if (list.length === 1) {
                $scope.expandedOrderId = list[0].id;
                $scope.loadOrderFollowups(list[0].id);
            }
        }, function(err) {
            $scope.loadingOrders = false;
            ToastService.error('Failed to load customer orders');
        });
    };

    /**
     * Loads follow-up check records for an order
     */
    $scope.loadOrderFollowups = function(orderId) {
        if (!orderId) return;
        FollowupService.getFollowups(orderId).then(function(res) {
            var raw = res.data;
            $scope.orderFollowups[orderId] = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
        });
    };

    // =========================================================
    // CUSTOMER TABLE HELPERS & PAGINATION
    // =========================================================
    $scope.getFilteredCustomers = function() {
        return $scope.customers || [];
    };

    $scope.getCustomerType = function(c) {
        if (!c) return 'Individual';
        if (c.customerType === 'ORGANIZATION' || c.customerType === 'MARKET') return 'Organization';
        if (c.customerType === 'INDIVIDUAL' || c.customerType === 'PERSON') return 'Individual';
        var isOrg = (c.name || '').toLowerCase().match(/mart|market|superstore|pos|company|corp|solutions|logistics/i);
        return isOrg ? 'Organization' : 'Individual';
    };

    $scope.getPagedCustomers = function() {
        return $scope.customers || [];
    };

    $scope.sortBy = function(field) {
        if ($scope.dt.sortField === field) {
            $scope.dt.sortReverse = !$scope.dt.sortReverse;
        } else {
            $scope.dt.sortField = field;
            $scope.dt.sortReverse = false;
        }
        $scope.loadCustomers(1);
    };

    $scope.getSortIndicator = function(field) {
        if ($scope.dt.sortField !== field) return '';
        return $scope.dt.sortReverse ? '▼' : '▲';
    };

    $scope.getCustomerRowClass = function(cust) {
        if (!cust) return '';
        if (cust.followupStatus === 'ALERT') return 'row-alert-milestone';
        return '';
    };

    $scope.getTotalPages = function() {
        return $scope.dt.totalPages || 1;
    };

    $scope.getPageNumbers = function() {
        var total = $scope.dt.totalPages || 1;
        var current = $scope.dt.currentPage || 1;
        if (total <= 7) {
            var pages = [];
            for (var i = 1; i <= total; i++) pages.push(i);
            return pages;
        }

        var pages = [];
        pages.push(1);

        var start = Math.max(2, current - 2);
        var end = Math.min(total - 1, current + 2);

        if (current <= 4) {
            start = 2;
            end = Math.min(total - 1, 5);
        } else if (current >= total - 3) {
            start = Math.max(2, total - 4);
            end = total - 1;
        }

        if (start > 2) pages.push('...');
        for (var i = start; i <= end; i++) pages.push(i);
        if (end < total - 1) pages.push('...');
        if (total > 1) pages.push(total);

        return pages;
    };

    $scope.setPage = function(page) {
        if (page === '...' || page === $scope.dt.currentPage) return;
        if (page >= 1 && page <= $scope.dt.totalPages) {
            $scope.loadCustomers(page);
        }
    };

    /**
     * Jump directly to order checklist and highlight target milestone
     */
    $scope.openCustomerOrder = function(cust, orderId, milestone, $event) {
        if ($event) $event.stopPropagation();
        $scope.clearOrderDateFilter();
        $scope.orderFilters.search = '';
        $scope.selectedCustomer = cust;
        $scope.targetMilestoneHighlight = milestone || null;
        $scope.expandedOrderId = orderId || null;
        $scope.customerView = 'orders';
        $scope.loadCustomerOrders(cust.id);
        if (orderId) {
            $scope.loadOrderFollowups(orderId);
        }
        if ($window.history && $window.history.pushState) {
            $window.history.pushState(null, '', '/admin/followups/orders/' + cust.id);
        }
    };

    $scope.clearOrderDateFilter = function() {
        $scope.orderFilters.dateFilterApplied = false;
        $scope.orderFilters.dateFrom = null;
        $scope.orderFilters.dateTo = null;
        $scope.selectedOrderTimePreset = null;
        $scope.orderDt.currentPage = 1;
    };

    $scope.clearAllOrderFilters = function() {
        $scope.orderFilters.search = '';
        $scope.clearOrderDateFilter();
    };

    // =========================================================
    // ORDER AUDIT LOG & CHECKLIST HELPERS
    // =========================================================
    $scope.getFilteredCustomerOrders = function() {
        return FilterHelperService.filterTable($scope.customerOrders, $scope.orderFilters, null);
    };

    $scope.getPagedCustomerOrders = function() {
        return FilterHelperService.filterTable($scope.customerOrders, $scope.orderFilters, $scope.orderDt);
    };

    $scope.sortOrdersBy = function(field) {
        if ($scope.orderDt.sortField === field) {
            $scope.orderDt.sortReverse = !$scope.orderDt.sortReverse;
        } else {
            $scope.orderDt.sortField = field;
            $scope.orderDt.sortReverse = false;
        }
    };

    $scope.getOrderSortIndicator = function(field) {
        if ($scope.orderDt.sortField !== field) return '';
        return $scope.orderDt.sortReverse ? '▼' : '▲';
    };

    $scope.getOrderFollowupStatusClass = function(order) {
        return '';
    };

    $scope.toggleOrderExpand = function(order, $event) {
        if ($event) $event.stopPropagation();
        if ($scope.expandedOrderId === order.id) {
            $scope.expandedOrderId = null;
        } else {
            $scope.expandedOrderId = order.id;
            if (!$scope.orderFollowups[order.id]) {
                $scope.loadOrderFollowups(order.id);
            }
        }
    };

    $scope.isOrderExpanded = function(orderId) {
        return $scope.expandedOrderId === orderId;
    };

    $scope.isMilestoneHighlighted = function(check) {
        if (!$scope.targetMilestoneHighlight || !check) return false;
        return check.presetName === $scope.targetMilestoneHighlight;
    };

    $scope.formatOrderAge = function(order) {
        if (!order || !order.orderDate) return 'Due';
        var diffMs = Date.now() - new Date(order.orderDate).getTime();
        var totalMins = Math.floor(diffMs / 60000);
        if (totalMins < 60) return totalMins + 'm ago';
        var hours = Math.floor(totalMins / 60);
        if (hours < 24) return hours + 'h ago';
        var days = Math.floor(hours / 24);
        var remH = hours % 24;
        return remH > 0 ? (days + 'd ' + remH + 'h ago') : (days + 'd ago');
    };

    $scope.formatCountdown = function(minutes) {
        if (minutes == null || minutes < 0) return 'Soon';
        if (minutes < 60) return minutes + 'm';
        var hours = Math.floor(minutes / 60);
        if (hours < 24) return hours + 'h';
        var days = Math.floor(hours / 24);
        var remH = hours % 24;
        return remH > 0 ? (days + 'd ' + remH + 'h') : (days + 'd');
    };

    $scope.getTotalOrderPages = function(totalItems) {
        return Math.ceil((totalItems || 1) / $scope.orderDt.pageSize) || 1;
    };

    $scope.getOrderPageNumbers = function(totalItems) {
        var total = $scope.getTotalOrderPages(totalItems);
        var current = $scope.orderDt.currentPage;
        if (total <= 7) {
            var pages = [];
            for (var i = 1; i <= total; i++) pages.push(i);
            return pages;
        }

        var pages = [];
        pages.push(1);

        var start = Math.max(2, current - 2);
        var end = Math.min(total - 1, current + 2);

        if (current <= 4) {
            start = 2;
            end = Math.min(total - 1, 5);
        } else if (current >= total - 3) {
            start = Math.max(2, total - 4);
            end = total - 1;
        }

        if (start > 2) pages.push('...');
        for (var i = start; i <= end; i++) pages.push(i);
        if (end < total - 1) pages.push('...');
        if (total > 1) pages.push(total);

        return pages;
    };

    $scope.setOrderPage = function(page) {
        if (page === '...') return;
        var total = $scope.getTotalOrderPages($scope.getFilteredCustomerOrders().length);
        if (page >= 1 && page <= total) {
            $scope.orderDt.currentPage = page;
        }
    };

    /**
     * Toggle milestone checkpoint status (Yes / No)
     */
    $scope.toggleFollowupCheck = function(order, check) {
        FollowupService.toggleStatus(check.id).then(function(res) {
            var updated = res.data && res.data.data ? res.data.data : res.data;
            if (updated) {
                check.isCompleted = updated.completed != null ? updated.completed : !check.isCompleted;
                check.completed   = check.isCompleted;
                check.status = updated.status || (check.isCompleted ? 'COMPLETED' : 'PENDING');
            } else {
                check.isCompleted = !check.isCompleted;
                check.completed   = check.isCompleted;
            }
            ToastService.success('Checkpoint status updated');
            if (order && order.id) {
                $scope.loadOrderFollowups(order.id);
            }
            $scope.loadCustomers();
        }, function() {
            ToastService.error('Failed to toggle status');
        });
    };

    // =========================================================
    // FEEDBACK & CHECKPOINT MODAL ACTIONS
    // =========================================================
    $scope.getCheckImages = function(item) {
        if (!item) return [];
        var url = item.imageUrl || item.photoUrl;
        if (!url || typeof url !== 'string' || !url.trim()) return [];
        url = url.trim();
        if (url.startsWith('[') && url.endsWith(']')) {
            try {
                var parsed = JSON.parse(url);
                if (Array.isArray(parsed)) {
                    return parsed.filter(function(x) { return typeof x === 'string' && x.trim().length > 0; });
                }
            } catch(e) {}
        }
        return [url];
    };

    $scope.openEditFollowupModal = function(order, check, $event) {
        if ($event) $event.stopPropagation();
        var item = angular.copy(check) || {};
        item.orderId = order ? order.id : null;
        item.note = check.note || check.notes || '';
        item.imageUrl = check.imageUrl || check.photoUrl || '';
        item.images = angular.copy($scope.getCheckImages(check));
        item.satisfaction = (check.satisfaction && check.satisfaction.trim() !== '') ? check.satisfaction.toUpperCase() : 'NEUTRAL';
        $scope.editingFollowup = item;
        $scope.tempImageUrl = '';
        $scope.showEditFollowupModal = true;
        if ($scope.$parent && $scope.$parent.openEditFollowupModal) {
            $scope.$parent.openEditFollowupModal(order, check, $event);
        }
    };

    $scope.closeEditFollowupModal = function() {
        $scope.showEditFollowupModal = false;
        $scope.editingFollowup = {};
        $scope.tempImageUrl = '';
        if ($scope.$parent && $scope.$parent.closeEditFollowupModal) {
            $scope.$parent.closeEditFollowupModal();
        }
    };

    $scope.removeFollowupImage = function(index) {
        if ($scope.editingFollowup && $scope.editingFollowup.images) {
            $scope.editingFollowup.images.splice(index, 1);
            $scope.editingFollowup.imageUrl = $scope.editingFollowup.images.length > 0 ? JSON.stringify($scope.editingFollowup.images) : '';
        }
    };

    $scope.addFollowupImageUrl = function() {
        if (!$scope.tempImageUrl || !$scope.tempImageUrl.trim()) return;
        if (!$scope.editingFollowup.images) $scope.editingFollowup.images = [];
        $scope.editingFollowup.images.push($scope.tempImageUrl.trim());
        $scope.editingFollowup.imageUrl = JSON.stringify($scope.editingFollowup.images);
        $scope.tempImageUrl = '';
    };

    $scope.saveFollowupCheck = function() {
        if (!$scope.editingFollowup || !$scope.editingFollowup.id) return;
        var sat = $scope.editingFollowup.satisfaction || 'NEUTRAL';
        var finalImages = $scope.editingFollowup.images || [];
        var finalImageUrl = finalImages.length > 0 ? JSON.stringify(finalImages) : '';
        $scope.editingFollowup.imageUrl = finalImageUrl;

        var payload = {
            notes: $scope.editingFollowup.note || '',
            photoUrl: finalImageUrl,
            imageUrl: finalImageUrl,
            satisfaction: sat
        };
        FollowupService.updateFollowup($scope.editingFollowup.id, payload).then(function(res) {
            ToastService.success('Feedback saved successfully');
            if ($scope.editingFollowup.orderId && $scope.orderFollowups[$scope.editingFollowup.orderId]) {
                var list = $scope.orderFollowups[$scope.editingFollowup.orderId];
                for (var i = 0; i < list.length; i++) {
                    if (list[i].id === $scope.editingFollowup.id) {
                        list[i].note = $scope.editingFollowup.note;
                        list[i].notes = $scope.editingFollowup.note;
                        list[i].imageUrl = finalImageUrl;
                        list[i].photoUrl = finalImageUrl;
                        list[i].satisfaction = sat;
                        break;
                    }
                }
            }
            $scope.closeEditFollowupModal();
            $scope.loadCustomers();
        }, function(err) {
            ToastService.error('Failed to save feedback');
        });
    };

    $scope.goToFeedbackForCheck = function(order, check, $event) {
        if ($event) $event.stopPropagation();
        $scope.openEditFollowupModal(order, check, $event);
    };

    $scope.toggleSatisfaction = function(target, val) {
        if (!target) return;
        target.satisfaction = val;
    };

    $scope.handleFollowupImageUpload = function(element) {
        if (element.files && element.files.length > 0) {
            if (!$scope.editingFollowup.images) $scope.editingFollowup.images = [];
            FollowupImageHelper.processImageFiles(
                element.files,
                function(dataUrls) {
                    $scope.$apply(function() {
                        dataUrls.forEach(function(url) {
                            $scope.editingFollowup.images.push(url);
                        });
                        $scope.editingFollowup.imageUrl = JSON.stringify($scope.editingFollowup.images);
                        if ($scope.$parent && $scope.$parent.editingFollowup) {
                            $scope.$parent.editingFollowup.images = $scope.editingFollowup.images;
                            $scope.$parent.editingFollowup.imageUrl = $scope.editingFollowup.imageUrl;
                        }
                        element.value = '';
                    });
                },
                function(errMsg) {
                    ToastService.error(errMsg || 'Failed to process image');
                }
            );
        }
    };

    $scope.$on('followup:updated', function(event, data) {
        if (data && data.orderId && $scope.orderFollowups[data.orderId]) {
            var list = $scope.orderFollowups[data.orderId];
            for (var i = 0; i < list.length; i++) {
                if (list[i].id === data.checkId) {
                    list[i].note = data.note;
                    list[i].notes = data.note;
                    list[i].imageUrl = data.imageUrl;
                    list[i].photoUrl = data.imageUrl;
                    list[i].satisfaction = data.satisfaction;
                    break;
                }
            }
        }
        $scope.loadCustomers();
    });

    $scope.$on('customer:reload', function() {
        $scope.loadCustomers();
    });

    // Listen for preset change from LayoutController
    $scope.$on('filter:timePresetChanged', function(event, preset) {
        if (preset) {
            var range = FilterHelperService.getPresetDateRange(preset);
            $scope.customerFilters.dateFrom = range.dateFrom;
            $scope.customerFilters.dateTo = range.dateTo;
            $scope.customerFilters.dateFilterApplied = true;
        } else {
            $scope.customerFilters.dateFrom = FilterHelperService.getToday();
            $scope.customerFilters.dateTo = FilterHelperService.getTomorrow();
            $scope.customerFilters.dateFilterApplied = false;
        }
        $scope.dt.currentPage = 1;
        $scope.loadCustomers();
    });

    // Listen for order preset change from LayoutController
    $scope.$on('filter:orderTimePresetChanged', function(event, preset) {
        if (preset) {
            var range = FilterHelperService.getPresetDateRange(preset);
            $scope.orderFilters.dateFrom = range.dateFrom;
            $scope.orderFilters.dateTo = range.dateTo;
            $scope.orderFilters.dateFilterApplied = true;
        } else {
            $scope.clearOrderDateFilter();
        }
        $scope.orderDt.currentPage = 1;
    });

    // Listen for queue status filter change from LayoutController
    $scope.$on('filter:followupStatusChanged', function(event, status) {
        $scope.customerFilters.followupStatus = status || 'ALL';
        $scope.dt.currentPage = 1;
        $scope.loadCustomers(1);
    });

    // Listen for customer type filter change from LayoutController
    $scope.$on('filter:customerTypeChanged', function(event, type) {
        $scope.customerFilters.customerType = type || 'ALL';
        $scope.dt.currentPage = 1;
        $scope.loadCustomers(1);
    });

    // Listen for customer city filter change from LayoutController
    $scope.$on('filter:customerCityChanged', function(event, city) {
        $scope.customerFilters.city = city || 'ALL';
        $scope.dt.currentPage = 1;
        $scope.loadCustomers(1);
    });

    $scope.clearFollowupStatusFilter = function() {
        $scope.customerFilters.followupStatus = 'ALL';
        $scope.dt.currentPage = 1;
        $scope.$emit('filter:clearFollowupStatus');
        $scope.loadCustomers(1);
    };

    $scope.exportFollowupsExcel = function() {
        var custs = $scope.customers || [];
        if (custs.length === 0) {
            ToastService.warning('No customer follow-up records to export.');
            return;
        }

        var headers = ['ID', 'Customer Name', 'Type', 'Phone Number', 'City', 'Total Orders', 'Follow-up Status', 'Remaining Checks', 'Next Scheduled Check'];
        var rows = custs.map(function(c) {
            var isOrg = (c.name || '').toLowerCase().match(/mart|market|superstore|pos|company|corp|solutions|logistics/i);
            var typeLabel = isOrg ? 'Organization' : 'Individual';
            var statusLabel = (c.followupStatus === 'ALERT' || c.followupStatus === 'OVERDUE') ? 'Needs Action' : (c.allFollowupsCompleted ? 'Completed' : 'Scheduled');
            return [
                c.id,
                c.name || '',
                typeLabel,
                c.phoneNumber || '',
                c.city || '',
                c.totalOrders != null ? c.totalOrders : 0,
                statusLabel,
                c.remainingFollowupsCount != null ? c.remainingFollowupsCount : 0,
                c.nextPendingFollowup || 'None'
            ];
        });

        var nowStr = new Date().toISOString().slice(0, 10);
        var filename = 'Twekl_Followups_' + nowStr;

        if (window.XLSX && XLSX.utils && XLSX.writeFile) {
            try {
                var wb = XLSX.utils.book_new();
                var ws = XLSX.utils.aoa_to_sheet([headers].concat(rows));
                ws['!cols'] = headers.map(function(h, idx) {
                    var maxLen = h.length;
                    rows.forEach(function(r) {
                        var l = String(r[idx] || '').length;
                        if (l > maxLen) maxLen = Math.min(l, 40);
                    });
                    return { wch: Math.max(maxLen + 3, 12) };
                });
                XLSX.utils.book_append_sheet(wb, ws, 'Follow-ups');
                XLSX.writeFile(wb, filename + '.xlsx');
                ToastService.success('Exported ' + rows.length + ' follow-up records to Excel.');
                return;
            } catch (e) {
                console.warn('XLSX export failed', e);
            }
        }

        // Fallback to CSV
        var csvContent = '\uFEFF' + headers.map(function(h) { return '"' + h + '"'; }).join(',') + '\r\n';
        rows.forEach(function(r) {
            csvContent += r.map(function(val) { return '"' + String(val == null ? '' : val).replace(/"/g, '""') + '"'; }).join(',') + '\r\n';
        });
        var blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        var link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = filename + '.csv';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        ToastService.success('Exported ' + rows.length + ' records to Excel format.');
    };

    // Listen for sub-view navigation from sidebar
    $scope.$on('followup:switchView', function(event, subView) {
        $scope.setCustomerView(subView, $scope.selectedCustomer);
    });

    // Listen for tab activation to ensure data is loaded
    $scope.$on('tab:changed', function(event, tab) {
        if (tab === 'customers' || tab === 'followups') {
            if (!$scope.customers || $scope.customers.length === 0) {
                $scope.loadCustomers();
            }
        }
    });

    // Listen to browser popstate to synchronize view with browser back/forward buttons
    $window.addEventListener('popstate', function() {
        $scope.$apply(function() {
            initFollowupRoute();
        });
    });

    // The page model is authoritative, including an empty directory.
    initFollowupRoute();
    if ($scope.customerFilters.followupStatus && $scope.customerFilters.followupStatus !== 'ALL') {
        $scope.loadCustomers(1);
    } else {
        var loadedAt = Date.now();
        $scope.customers = (window.SERVER_FOLLOWUPS || []).map(function(summary) {
            return angular.extend({}, summary.customer, {
                followupStatus: summary.followupStatus,
                nextPendingFollowup: summary.nextPendingFollowup,
                nextPendingOrderId: summary.nextPendingOrderId,
                allFollowupsCompleted: summary.allFollowupsCompleted,
                remainingFollowupsCount: summary.remainingFollowupsCount,
                nextPendingMinutesLeft: summary.nextPendingMinutesLeft,
                _loadedAt: loadedAt
            });
        });
    }
    if (pendingTargetCustomerId) {
        $scope.selectedCustomer = $scope.customers.find(function(customer) {
            return customer.id === pendingTargetCustomerId;
        });
        if ($scope.selectedCustomer) $scope.loadCustomerOrders(pendingTargetCustomerId);
        pendingTargetCustomerId = null;
    }

}];

// Register the controller used by the follow-up page.
angular.module('tweklApp')
.controller('FollowupController', followupControllerDefinition);
