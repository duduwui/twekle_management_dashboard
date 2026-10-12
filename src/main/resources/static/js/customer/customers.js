/**
 * Customer Service
 * Handles customer retrieval, stats, and related orders.
 * Endpoints: /internal/customers (fallback: /api/customers)
 */
angular.module('tweklApp')
.factory('CustomerService', ['$http', function($http) {
    var BASE = '/internal/customers';
    var FALLBACK = '/api/customers';

    function request(method, path, config) {
        config = config || {};
        config.method = method;
        config.url = BASE + (path || '');
        var fallbackUrl = FALLBACK + (path || '');

        return $http(config).catch(function() {
            var fallbackConfig = angular.copy(config);
            fallbackConfig.url = fallbackUrl;
            return $http(fallbackConfig);
        });
    }

    return {
        getAll: function(timeFilter) {
            var params = timeFilter ? { timeFilter: timeFilter } : {};
            return request('GET', '', { params: params });
        },
        getById: function(id) {
            return request('GET', '/' + id);
        },
        getOrders: function(customerId) {
            return request('GET', '/' + customerId + '/orders');
        },
        getStats: function() {
            return request('GET', '/stats');
        }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('CustomerGetAllService', ['CustomerService', function(CustomerService) {
    return { execute: function(timeFilter) { return CustomerService.getAll(timeFilter); } };
}])
.factory('CustomerGetByIdService', ['CustomerService', function(CustomerService) {
    return { execute: function(id) { return CustomerService.getById(id); } };
}])
.factory('CustomerGetOrdersService', ['CustomerService', function(CustomerService) {
    return { execute: function(customerId) { return CustomerService.getOrders(customerId); } };
}])
.factory('CustomerGetStatsService', ['CustomerService', function(CustomerService) {
    return { execute: function() { return CustomerService.getStats(); } };
}]);

/**
 * Follow-up Service
 * Manages customer order follow-up checkpoints, milestones, and status toggles.
 * Endpoints: /internal/orders/{orderId}/followups, /internal/followups/{id}
 */
angular.module('tweklApp')
.factory('FollowupService', ['$http', function($http) {
    return {
        getFollowups: function(orderId) {
            return $http.get('/internal/orders/' + orderId + '/followups').catch(function() {
                return $http.get('/api/orders/' + orderId + '/followups');
            });
        },
        createFollowup: function(orderId, data) {
            return $http.post('/internal/orders/' + orderId + '/followups', data).catch(function() {
                return $http.post('/api/orders/' + orderId + '/followups', data);
            });
        },
        updateFollowup: function(id, data) {
            return $http.put('/internal/followups/' + id, data).catch(function() {
                return $http.put('/api/followups/' + id, data);
            });
        },
        toggleStatus: function(id) {
            return $http.patch('/internal/followups/' + id + '/status', {}).catch(function() {
                return $http.patch('/api/followups/' + id + '/status', {});
            });
        }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('FollowupGetService', ['FollowupService', function(FollowupService) {
    return { execute: function(orderId) { return FollowupService.getFollowups(orderId); } };
}])
.factory('FollowupCreateService', ['FollowupService', function(FollowupService) {
    return { execute: function(orderId, data) { return FollowupService.createFollowup(orderId, data); } };
}])
.factory('FollowupUpdateService', ['FollowupService', function(FollowupService) {
    return { execute: function(id, data) { return FollowupService.updateFollowup(id, data); } };
}])
.factory('FollowupToggleStatusService', ['FollowupService', function(FollowupService) {
    return { execute: function(id) { return FollowupService.toggleStatus(id); } };
}]);

/**
 * Follow-up Feature Coordinator Controller
 * Manages Customer Directory (4.1), Order Logs (4.2), Follow-up Checklists, and Reviews/Feedback (4.3).
 */
angular.module('tweklApp')
.controller('FollowupController', [
    '$scope', '$http', '$timeout', '$window', '$interval', 'CustomerService', 'FollowupService', 'ToastService', 'FilterHelperService',
function($scope, $http, $timeout, $window, $interval, CustomerService, FollowupService, ToastService, FilterHelperService) {

    // Active sub-view: 'list' | 'orders' | 'feedback'
    $scope.customerView = 'list';
    $scope.customers = [];
    $scope.selectedCustomer = null;
    $scope.customerOrders = [];
    $scope.orderFollowups = {}; // map: orderId -> [OrderFollowupCheck]
    $scope.expandedOrderId = null;
    $scope.targetMilestoneHighlight = null;

    var pendingTargetCustomerId = null;

    // Table pagination & sorting (Customer List)
    $scope.dt = {
        currentPage: 1,
        pageSize: 10,
        sortField: 'id',
        sortReverse: false,
        totalItems: 0
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

    // Filter criteria (Customer Orders) - Default Today/Tomorrow displayed
    $scope.orderFilters = {
        search: '',
        dateFrom: FilterHelperService.getToday(),
        dateTo: FilterHelperService.getTomorrow(),
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
            }
        } else if (path.indexOf('/admin/followups/feedback/') === 0 || path.indexOf('/admin/customers/feedback/') === 0) {
            var parts = path.split('/');
            var targetId = parts[4] ? parseInt(parts[4], 10) : null;
            if (targetId) {
                $scope.customerView = 'feedback';
                pendingTargetCustomerId = targetId;
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

    /**
     * Loads customer directory from backend.
     * Stamps each customer with _loadedAt so the live countdown can drift correctly.
     */
    $scope.loadCustomers = function() {
        $scope.loadingCustomers = true;
        var preset = $scope.selectedTimePreset ? ($scope.selectedTimePreset.presetKey || $scope.selectedTimePreset.name) : null;
        CustomerService.getAll(preset).then(function(res) {
            $scope.loadingCustomers = false;
            var raw = res.data;
            var list = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
            var now = Date.now();
            $scope.customers = list.map(function(c) {
                c._loadedAt = now; // track when we got this data
                return c;
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
            ToastService.error('Failed to load customers');
        });
    };

    /**
     * Returns a live-adjusted countdown string for a customer's next pending followup.
     * Adjusts server-provided minutesLeft by how much time has elapsed since load.
     */
    $scope.getLiveCountdown = function(cust) {
        if (!cust || cust.nextPendingMinutesLeft == null) return '';
        var elapsedMins = cust._loadedAt ? Math.floor((Date.now() - cust._loadedAt) / 60000) : 0;
        var mins = cust.nextPendingMinutesLeft - elapsedMins;

        if (mins >= 0) {
            // Time remaining
            if (mins < 60) return mins + 'm';
            var hours = Math.floor(mins / 60);
            if (hours < 24) return hours + 'h ' + (mins % 60) + 'm';
            var days = Math.floor(hours / 24);
            var remH = hours % 24;
            return remH > 0 ? (days + 'd ' + remH + 'h') : (days + 'd');
        } else {
            // Overdue — show how long ago it was due
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
        // Force Angular digest so getLiveCountdown() re-evaluates
        // Customers array unchanged — just need scope digest
        if ($scope.customers && $scope.customers.length) {
            $scope._tick = ($scope._tick || 0) + 1;
        }
    }, 60000);

    // Clean up interval when controller is destroyed
    $scope.$on('$destroy', function() { $interval.cancel(_countdownTick); });

    // Reload customers and clear cached followups when auth or time presets change
    $scope.$on('auth:loggedIn', function() {
        $scope.orderFollowups = {};
        $scope.loadCustomers();
        $scope.loadCustomerStats();
    });

    $scope.$on('timePresets:changed', function() {
        $scope.orderFollowups = {};
        $scope.loadCustomers();
        $scope.loadCustomerStats();
        if ($scope.selectedCustomer) {
            $scope.loadCustomerOrders($scope.selectedCustomer.id);
        }
    });



    /**
     * Loads orders for a specific customer
     */
    $scope.loadCustomerOrders = function(customerId) {
        if (!customerId) return;
        CustomerService.getOrders(customerId).then(function(res) {
            var raw = res.data;
            var list = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
            $scope.customerOrders = list;
            // Auto-load follow-ups for all orders
            list.forEach(function(ord) {
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
        FollowupService.getFollowups(orderId).then(function(res) {
            var raw = res.data;
            $scope.orderFollowups[orderId] = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
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
        if ($window.history && $window.history.pushState) {
            $window.history.pushState(null, '', '/admin/followups/orders/' + cust.id);
        }
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

    /**
     * Toggle milestone checkpoint status (Yes ✔ / No ✖)
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
            // Reload followups for this order so the list re-sorts and shows correct next milestone
            if (order && order.id) {
                $scope.loadOrderFollowups(order.id);
            }
            // Reload customer list so Remaining Follow-ups count + Next Pending Followup update
            $scope.loadCustomers();
        }, function() {
            ToastService.error('Failed to toggle status');
        });
    };

    // =========================================================
    // FEEDBACK & CHECKPOINT MODAL ACTIONS
    // =========================================================
    $scope.openEditFollowupModal = function(order, check, $event) {
        if ($event) $event.stopPropagation();
        var item = angular.copy(check) || {};
        item.orderId = order ? order.id : null;
        item.note = check.note || check.notes || '';
        item.imageUrl = check.imageUrl || check.photoUrl || '';
        item.satisfaction = (check.satisfaction && check.satisfaction.trim() !== '') ? check.satisfaction.toUpperCase() : 'NEUTRAL';
        $scope.editingFollowup = item;
        $scope.showEditFollowupModal = true;
        if ($scope.$parent && $scope.$parent.openEditFollowupModal) {
            $scope.$parent.openEditFollowupModal(order, check, $event);
        }
    };

    $scope.closeEditFollowupModal = function() {
        $scope.showEditFollowupModal = false;
        $scope.editingFollowup = {};
        if ($scope.$parent && $scope.$parent.closeEditFollowupModal) {
            $scope.$parent.closeEditFollowupModal();
        }
    };

    $scope.saveFollowupCheck = function() {
        if (!$scope.editingFollowup || !$scope.editingFollowup.id) return;
        var sat = $scope.editingFollowup.satisfaction || 'NEUTRAL';
        var payload = {
            notes: $scope.editingFollowup.note || '',
            photoUrl: $scope.editingFollowup.imageUrl || '',
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
                        list[i].imageUrl = $scope.editingFollowup.imageUrl;
                        list[i].photoUrl = $scope.editingFollowup.imageUrl;
                        list[i].satisfaction = sat;
                        break;
                    }
                }
            }
            $scope.closeEditFollowupModal();
            $scope.loadCustomers();
            $scope.loadStats();
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
        if (element.files && element.files[0]) {
            var file = element.files[0];
            if (!file.type.match('image.*')) {
                ToastService.error('Please select an image file');
                return;
            }
            var reader = new FileReader();
            reader.onload = function(e) {
                var img = new Image();
                img.onload = function() {
                    var canvas = document.createElement('canvas');
                    var MAX_WIDTH = 1200;
                    var MAX_HEIGHT = 1200;
                    var width = img.width;
                    var height = img.height;

                    if (width > height) {
                        if (width > MAX_WIDTH) {
                            height *= MAX_WIDTH / width;
                            width = MAX_WIDTH;
                        }
                    } else {
                        if (height > MAX_HEIGHT) {
                            width *= MAX_HEIGHT / height;
                            height = MAX_HEIGHT;
                        }
                    }
                    canvas.width = width;
                    canvas.height = height;
                    var ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    var dataUrl = canvas.toDataURL('image/jpeg', 0.85);
                    $scope.$apply(function() {
                        $scope.editingFollowup.imageUrl = dataUrl;
                        if ($scope.$parent && $scope.$parent.editingFollowup) {
                            $scope.$parent.editingFollowup.imageUrl = dataUrl;
                        }
                    });
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
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
        $scope.loadStats();
    });

    $scope.$on('customer:reload', function() {
        $scope.loadCustomers();
        $scope.loadStats();
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
            $scope.orderFilters.dateFrom = FilterHelperService.getToday();
            $scope.orderFilters.dateTo = FilterHelperService.getTomorrow();
            $scope.orderFilters.dateFilterApplied = false;
        }
        $scope.orderDt.currentPage = 1;
    });

    // Listen for queue status filter change from LayoutController
    $scope.$on('filter:followupStatusChanged', function(event, status) {
        $scope.customerFilters.followupStatus = status || 'ALL';
        $scope.dt.currentPage = 1;
    });

    $scope.clearFollowupStatusFilter = function() {
        $scope.customerFilters.followupStatus = 'ALL';
        $scope.dt.currentPage = 1;
        $scope.$emit('filter:clearFollowupStatus');
    };

    // Listen for sub-view navigation from sidebar
    $scope.$on('followup:switchView', function(event, subView) {
        $scope.setCustomerView(subView, $scope.selectedCustomer);
    });

    // Listen for tab activation to ensure data is loaded
    $scope.$on('tab:changed', function(event, tab) {
        if (tab === 'customers') {
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

    // Auto-load on init
    initFollowupRoute();
    $scope.loadCustomers();
}]);
