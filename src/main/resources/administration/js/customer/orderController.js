/**
 * Customer Order & Follow-up Log Controller
 * Manages customer order records, milestone checklists, and checkpoint status toggles.
 */
angular.module('tweklApp')
.controller('OrderController', [
    '$scope', '$timeout', 'CustomerService', 'FollowupService', 'ToastService', 'FilterHelperService',
function($scope, $timeout, CustomerService, FollowupService, ToastService, FilterHelperService) {

    $scope.customerOrders = $scope.customerOrders || [];
    $scope.orderFollowups = $scope.orderFollowups || {};
    $scope.expandedOrderId = null;
    $scope.targetMilestoneHighlight = null;

    $scope.orderDt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: true,
        totalItems: 0
    };

    $scope.orderFilters = {
        search: '',
        dateFrom: FilterHelperService.getToday(),
        dateTo: FilterHelperService.getTomorrow(),
        dateFilterApplied: false
    };

    $scope.loadCustomerOrders = function(customerId) {
        if (!customerId) return;
        CustomerService.getOrders(customerId).then(function(res) {
            var raw = res.data;
            var list = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
            $scope.customerOrders = list;
            list.forEach(function(ord) {
                $scope.loadOrderFollowups(ord.id);
            });
        }, function(err) {
            ToastService.error('Failed to load customer orders');
        });
    };

    $scope.loadOrderFollowups = function(orderId) {
        if (!orderId) return;
        FollowupService.getFollowups(orderId).then(function(res) {
            var raw = res.data;
            $scope.orderFollowups[orderId] = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
        });
    };

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
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    $scope.setOrderPage = function(page) {
        if (page >= 1 && page <= $scope.getTotalOrderPages($scope.getFilteredCustomerOrders().length)) {
            $scope.orderDt.currentPage = page;
        }
    };

    $scope.toggleFollowupCheck = function(order, check) {
        FollowupService.toggleStatus(check.id).then(function(res) {
            var updated = res.data && res.data.data ? res.data.data : res.data;
            if (updated) {
                check.isCompleted = updated.completed != null ? updated.completed : !check.isCompleted;
                check.completed = check.isCompleted;
                check.status = updated.status || (check.isCompleted ? 'COMPLETED' : 'PENDING');
            } else {
                check.isCompleted = !check.isCompleted;
                check.completed = check.isCompleted;
            }
            ToastService.success('Checkpoint status updated');
            if (order && order.id) {
                $scope.loadOrderFollowups(order.id);
            }
            if ($scope.$parent && $scope.$parent.loadCustomers) {
                $scope.$parent.loadCustomers();
            }
        }, function() {
            ToastService.error('Failed to toggle status');
        });
    };

    $scope.$on('filter:orderTimePresetChanged', function(event, preset) {
        if (preset) {
            var range = FilterHelperService.getPresetDateRange(preset);
            $scope.orderFilters.dateFrom = range.dateFrom;
            $scope.orderFilters.dateTo = range.dateTo;
            $scope.orderFilters.dateFilterApplied = true;
        } else {
            $scope.orderFilters.dateFrom = FilterHelperService.getToday();
            $scope.orderFilters.dateTo = FilterHelperService.getTomorrow();
            $scope.orderFilters.dateFilterApplied = false;
        }
        $scope.orderDt.currentPage = 1;
    });

}]);
