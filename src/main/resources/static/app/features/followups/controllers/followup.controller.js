/**
 * Follow-up Feature Coordinator Controller
 * Manages Customer Directory (4.1), Order Logs (4.2), Follow-up Checklists, and Reviews/Feedback (4.3).
 */
angular.module('tweklApp')
.controller('FollowupController', [
    '$scope', '$http', '$timeout', 'CustomerGetAllService', 'CustomerGetOrdersService', 'CustomerGetByIdService', 'FollowupToggleStatusService', 'FollowupUpdateService', 'ToastService', 'FilterHelperService',
function($scope, $http, $timeout, CustomerGetAllService, CustomerGetOrdersService, CustomerGetByIdService, FollowupToggleStatusService, FollowupUpdateService, ToastService, FilterHelperService) {

    // Active sub-view: 'list' | 'orders' | 'feedback'
    $scope.customerView = 'list';
    $scope.customers = [];
    $scope.selectedCustomer = null;
    $scope.customerOrders = [];
    $scope.orderFollowups = {}; // map: orderId -> [OrderFollowupCheck]
    $scope.expandedOrderId = null;
    $scope.targetMilestoneHighlight = null;

    // Table pagination & sorting (Customer List)
    $scope.dt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: false,
        totalItems: 0
    };

    // Filter criteria (Customer List)
    $scope.customerFilters = {
        search: '',
        dateFrom: null,
        dateTo: null
    };

    // Table pagination & sorting (Customer Orders)
    $scope.orderDt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: true,
        totalItems: 0
    };

    // Filter criteria (Customer Orders)
    $scope.orderFilters = {
        search: '',
        dateFrom: null,
        dateTo: null
    };

    // Modals state
    $scope.showEditFollowupModal = false;
    $scope.editingFollowup = {};
    $scope.showAddFeedbackModal = false;
    $scope.newCustomerFeedback = { content: '', satisfaction: null, imageUrl: '', authorName: '', orderId: null };
    $scope.showEditFeedbackModal = false;
    $scope.editingCustomerFeedback = {};

    /**
     * Set active customer view ('list', 'orders', 'feedback')
     */
    $scope.setCustomerView = function(view, cust) {
        $scope.customerView = view;
        if (cust) {
            $scope.selectedCustomer = cust;
            $scope.loadCustomerOrders(cust.id);
        }
        if (view === 'list') {
            $scope.selectedCustomer = null;
            $scope.customerOrders = [];
            $scope.expandedOrderId = null;
            $scope.targetMilestoneHighlight = null;
        }
    };

    /**
     * Loads customer directory from backend
     */
    $scope.loadCustomers = function() {
        var preset = $scope.selectedTimePreset ? ($scope.selectedTimePreset.presetKey || $scope.selectedTimePreset.name) : null;
        CustomerGetAllService.execute(preset).then(function(res) {
            $scope.customers = res.data || [];
        }, function(err) {
            ToastService.error('Failed to load customers');
        });
    };

    /**
     * Loads orders for a specific customer
     */
    $scope.loadCustomerOrders = function(customerId) {
        if (!customerId) return;
        CustomerGetOrdersService.execute(customerId).then(function(res) {
            $scope.customerOrders = res.data || [];
            // Auto-load follow-ups for all orders
            $scope.customerOrders.forEach(function(ord) {
                $scope.loadOrderFollowups(ord.id);
            });
        }, function(err) {
            ToastService.error('Failed to load customer orders');
        });
    };

    /**
     * Loads follow-up check records for an order
     */
    $scope.loadOrderFollowups = function(orderId) {
        if (!orderId) return;
        $http.get('/api/orders/' + orderId + '/followups').then(function(res) {
            $scope.orderFollowups[orderId] = res.data || [];
        });
    };

    // =========================================================
    // CUSTOMER TABLE HELPERS & PAGINATION
    // =========================================================
    $scope.getFilteredCustomers = function() {
        return FilterHelperService.filterTable($scope.customers, $scope.customerFilters, null);
    };

    $scope.getPagedCustomers = function() {
        return FilterHelperService.filterTable($scope.customers, $scope.customerFilters, $scope.dt);
    };

    $scope.sortBy = function(field) {
        if ($scope.dt.sortField === field) {
            $scope.dt.sortReverse = !$scope.dt.sortReverse;
        } else {
            $scope.dt.sortField = field;
            $scope.dt.sortReverse = false;
        }
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

    $scope.getTotalPages = function(totalItems) {
        return Math.ceil((totalItems || 1) / $scope.dt.pageSize) || 1;
    };

    $scope.getPageNumbers = function(totalItems) {
        var total = $scope.getTotalPages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    $scope.setPage = function(page) {
        if (page >= 1 && page <= $scope.getTotalPages($scope.getFilteredCustomers().length)) {
            $scope.dt.currentPage = page;
        }
    };

    /**
     * Jump directly to order checklist and highlight target milestone
     */
    $scope.openCustomerOrder = function(cust, orderId, milestone, $event) {
        if ($event) $event.stopPropagation();
        $scope.selectedCustomer = cust;
        $scope.targetMilestoneHighlight = milestone || null;
        $scope.expandedOrderId = orderId || null;
        $scope.customerView = 'orders';
        $scope.loadCustomerOrders(cust.id);
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
        if (!order) return '';
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
        var days = Math.floor(diffMs / (24 * 60 * 60 * 1000));
        return days <= 0 ? 'Today' : (days + ' days ago');
    };

    $scope.formatCountdown = function(minutes) {
        if (minutes == null) return 'Soon';
        if (minutes <= 60) return minutes + 'm';
        var hours = Math.floor(minutes / 60);
        if (hours < 24) return hours + 'h';
        var days = Math.floor(hours / 24);
        return days + 'd';
    };

    $scope.getTotalOrderPages = function(totalItems) {
        return Math.ceil((totalItems || 1) / $scope.orderDt.pageSize) || 1;
    };

    $scope.getOrderPageNumbers = function(totalItems) {
        var total = $scope.getTotalOrderPages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    $scope.setOrderPage = function(page) {
        if (page >= 1 && page <= $scope.getTotalOrderPages($scope.getFilteredCustomerOrders().length)) {
            $scope.orderDt.currentPage = page;
        }
    };

    /**
     * Toggle milestone checkpoint status (Yes ✔ / No ✖)
     */
    $scope.toggleFollowupCheck = function(order, check) {
        FollowupToggleStatusService.execute(check.id).then(function(res) {
            var updated = res.data && res.data.data ? res.data.data : res.data;
            if (updated) {
                check.isCompleted = updated.completed != null ? updated.completed : !check.isCompleted;
                check.status = updated.status || (check.isCompleted ? 'COMPLETED' : 'PENDING');
            } else {
                check.isCompleted = !check.isCompleted;
            }
            ToastService.success('Checkpoint status updated');
        }, function() {
            ToastService.error('Failed to toggle status');
        });
    };

    // =========================================================
    // FEEDBACK & CHECKPOINT MODAL ACTIONS
    // =========================================================
    $scope.openEditFollowupModal = function(order, check) {
        $scope.editingFollowup = angular.copy(check) || {};
        $scope.editingFollowup.orderId = order ? order.id : null;
        $scope.editingFollowup.note = check.note || check.notes || '';
        $scope.editingFollowup.imageUrl = check.imageUrl || check.photoUrl || '';
        $scope.showEditFollowupModal = true;
    };

    $scope.closeEditFollowupModal = function() {
        $scope.showEditFollowupModal = false;
        $scope.editingFollowup = {};
    };

    $scope.saveFollowupCheck = function() {
        if (!$scope.editingFollowup || !$scope.editingFollowup.id) return;
        var payload = {
            notes: $scope.editingFollowup.note || '',
            photoUrl: $scope.editingFollowup.imageUrl || ''
        };
        FollowupUpdateService.execute($scope.editingFollowup.id, payload).then(function(res) {
            ToastService.success('Checkpoint updated successfully');
            if ($scope.editingFollowup.orderId) {
                $scope.loadOrderFollowups($scope.editingFollowup.orderId);
            }
            $scope.closeEditFollowupModal();
        }, function(err) {
            ToastService.error('Failed to update checkpoint');
        });
    };

    $scope.goToFeedbackForCheck = function(order, check, $event) {
        if ($event) $event.stopPropagation();
        $scope.customerView = 'feedback';
        $scope.expandedOrderId = order.id;
        $scope.openEditFollowupModal(order, check);
    };

    $scope.toggleSatisfaction = function(target, val) {
        if (!target) return;
        target.satisfaction = (target.satisfaction === val) ? null : val;
    };

    $scope.handleFollowupImageUpload = function(element) {
        if (element.files && element.files[0]) {
            var reader = new FileReader();
            reader.onload = function(e) {
                $scope.$apply(function() {
                    $scope.editingFollowup.imageUrl = e.target.result;
                });
            };
            reader.readAsDataURL(element.files[0]);
        }
    };

    // Listen for preset change from LayoutController
    $scope.$on('filter:timePresetChanged', function(event, preset) {
        $scope.loadCustomers();
    });

    // Auto-load on init
    $scope.loadCustomers();
}]);
