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
        dateTo: FilterHelperService.getToday(),
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

    // Filter criteria (Customer Orders) - Defaults to today's date in pickers
    $scope.orderFilters = {
        search: '',
        dateFrom: FilterHelperService.getToday(),
        dateTo: FilterHelperService.getToday(),
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

        // Clean Human-Readable URL matching: /followup/review/{orderId}, /admin/followup/review/{orderId}, /admin/review/{orderId}
        var reviewOrderMatch = path.match(/(?:\/admin)?\/(?:followups?\/)?review\/(\d+)/i);
        if (reviewOrderMatch) {
            var targetOrderId = reviewOrderMatch[1];
            $scope.customerView = 'feedback';
            $scope.orderFilters.search = String(targetOrderId);
            $scope.expandedOrderId = targetOrderId;

            FollowupService.lookupOrder(targetOrderId).then(function(res) {
                var ord = res.data && res.data.data ? res.data.data : res.data;
                if (ord && ord.customerId) {
                    $scope.selectedCustomer = {
                        id: ord.customerId,
                        name: ord.customerName || ('Customer #' + ord.customerId)
                    };
                    pendingTargetCustomerId = ord.customerId;
                    try {
                        sessionStorage.setItem('review_customer_id', ord.customerId);
                        sessionStorage.setItem('review_order_id', ord.orderId || targetOrderId);
                    } catch(e) {}
                    $scope.loadCustomerOrders(ord.customerId, ord.orderId || targetOrderId, false);
                } else {
                    $scope.customerView = 'list';
                    ToastService.error('Order #' + targetOrderId + ' not found');
                }
            }, function(err) {
                $scope.customerView = 'list';
                ToastService.error('Order #' + targetOrderId + ' not found');
            });
            return;
        }

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
        } else if (path.indexOf('/admin/review') === 0 && !window.location.search) {
            var savedCustId = null;
            try { savedCustId = sessionStorage.getItem('review_customer_id'); } catch(e) {}
            if (savedCustId) {
                var targetId = parseInt(savedCustId, 10);
                if (targetId) {
                    $scope.customerView = 'orders';
                    pendingTargetCustomerId = targetId;
                    FollowupService.getById(targetId).then(function(res) {
                        var c = res.data ? (res.data.data || res.data) : null;
                        if (c) {
                            $scope.selectedCustomer = c;
                        }
                    });
                    var savedOrderId = null;
                    try { savedOrderId = sessionStorage.getItem('review_order_id'); } catch(e) {}
                    $scope.loadCustomerOrders(targetId, savedOrderId, false);
                }
            } else {
                $scope.customerView = 'list';
            }
        } else {
            $scope.customerView = 'list';
        }

        if (window.location.search) {
            try {
                var searchParams = new URLSearchParams(window.location.search);
                var statusParam = searchParams.get('status');
                if (statusParam) {
                    $scope.customerFilters.followupStatus = statusParam.toUpperCase();
                }
                var custIdParam = searchParams.get('customerId') || searchParams.get('id');
                var orderIdParam = searchParams.get('orderId') || searchParams.get('orderNumber');
                var openReviewParam = searchParams.get('openReview') === 'true' || searchParams.get('review') === 'true';
                var viewParam = searchParams.get('view') || 'orders';

                var custNameParam = searchParams.get('customerName') || searchParams.get('customer_name') || searchParams.get('name');
                var dateParam = searchParams.get('date') || searchParams.get('orderDate') || searchParams.get('order_date') || searchParams.get('Date') || searchParams.get('OrderDate');
                if (orderIdParam) {
                    $scope.customerView = viewParam || 'orders';
                    if (viewParam === 'feedback' || window.location.pathname.indexOf('review') !== -1) {
                        if ($window.history && $window.history.replaceState) {
                            $window.history.replaceState(null, '', '/followup/review/' + orderIdParam);
                        }
                    }
                    FollowupService.lookupOrder(orderIdParam, dateParam).then(function(res) {
                        var ord = res.data && res.data.data ? res.data.data : res.data;
                        if (ord && ord.customerId) {
                            var resolvedName = custNameParam || ord.customerName || ('Customer #' + ord.customerId);
                            $scope.selectedCustomer = {
                                id: ord.customerId,
                                name: resolvedName
                            };
                            pendingTargetCustomerId = ord.customerId;
                            try {
                                sessionStorage.setItem('review_customer_id', ord.customerId);
                                sessionStorage.setItem('review_order_id', ord.orderId || orderIdParam);
                            } catch(e) {}

                            // If this order's modal was already opened or dismissed in this browser session, do not auto-reopen on refresh!
                            var targetKey = ord.orderId || orderIdParam;
                            var alreadyOpened = false;
                            try {
                                alreadyOpened = (sessionStorage.getItem('review_modal_opened_' + targetKey) === 'true' ||
                                                 sessionStorage.getItem('review_modal_opened_' + orderIdParam) === 'true');
                            } catch(e) {}

                            var shouldAutoOpen = openReviewParam || (!alreadyOpened && viewParam !== 'feedback');
                            $scope.loadCustomerOrders(ord.customerId, targetKey, shouldAutoOpen);
                        } else {
                            $scope.customerView = 'list';
                            ToastService.error('Order #' + orderIdParam + ' not found');
                        }
                    }, function(err) {
                        $scope.customerView = 'list';
                        ToastService.error('Order #' + orderIdParam + ' not found');
                    });
                } else if (custIdParam) {
                    var targetId = parseInt(custIdParam, 10);
                    if (targetId) {
                        $scope.customerView = viewParam;
                        pendingTargetCustomerId = targetId;
                        try {
                            sessionStorage.setItem('review_customer_id', targetId);
                            if (orderIdParam) sessionStorage.setItem('review_order_id', orderIdParam);
                        } catch(e) {}
                        FollowupService.getById(targetId).then(function(res) {
                            var c = res.data ? (res.data.data || res.data) : null;
                            if (c) {
                                if (custNameParam) c.name = custNameParam;
                                $scope.selectedCustomer = c;
                            }
                        }, function(err) {
                            $scope.customerView = 'list';
                            ToastService.error('Customer #' + targetId + ' not found');
                        });
                        $scope.loadCustomerOrders(targetId, orderIdParam, openReviewParam);
                    }
                }
                var searchParam = searchParams.get('search');
                if (searchParam && !custIdParam) {
                    $scope.customerFilters.search = searchParam;
                }

                var presetParam = searchParams.get('timeFilter') || searchParams.get('preset') || searchParams.get('presetId');
                if (presetParam) {
                    var applyPresetFromParam = function(list) {
                        if (!list || !list.length) return false;
                        var cleanParam = presetParam.trim().toLowerCase();
                        for (var i = 0; i < list.length; i++) {
                            var p = list[i];
                            if (String(p.id) === String(presetParam) ||
                                (p.presetKey && p.presetKey.toLowerCase() === cleanParam) ||
                                (p.name && p.name.toLowerCase() === cleanParam) ||
                                (p.labelEn && p.labelEn.toLowerCase() === cleanParam)) {
                                if (typeof $scope.selectTimePreset === 'function') {
                                    $scope.selectTimePreset(p);
                                } else {
                                    $scope.selectedTimePreset = p;
                                    $scope.$broadcast('filter:timePresetChanged', p);
                                }
                                return true;
                            }
                        }
                        return false;
                    };
                    var presets = $scope.timeFilterPresets || window.SERVER_PRESETS || [];
                    if (!applyPresetFromParam(presets)) {
                        var unwatch = $scope.$watch('timeFilterPresets', function(newVal) {
                            if (newVal && newVal.length && applyPresetFromParam(newVal)) {
                                if (typeof unwatch === 'function') unwatch();
                            }
                        });
                    }
                }
            } catch(e) {
                console.warn('Error parsing search params', e);
            }
        }
    }

    $scope.selectTimePreset = function(preset) {
        $scope.selectedTimePreset = preset;
        if ($window.history && $window.history.pushState) {
            var url = new URL($window.location.href);
            if (preset) {
                if (preset.id) url.searchParams.set('presetId', preset.id);
                url.searchParams.set('timeFilter', preset.presetKey || preset.name || preset.labelEn);
            } else {
                url.searchParams.delete('presetId');
                url.searchParams.delete('timeFilter');
                url.searchParams.delete('preset');
            }
            $window.history.pushState(null, '', url.toString());
        }
        $scope.dt.currentPage = 1;
        $scope.loadCustomers(1);
    };

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
            try {
                sessionStorage.removeItem('review_customer_id');
                sessionStorage.removeItem('review_order_id');
            } catch(e) {}
            $scope.selectedCustomer = null;
            $scope.customerOrders = [];
            $scope.expandedOrderId = null;
            $scope.targetMilestoneHighlight = null;
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/followups');
        } else if (view === 'orders' && cust) {
            if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/followups/orders/' + cust.id);
        } else if (view === 'feedback' && cust) {
            var targetOrd = $scope.expandedOrderId;
            if (!targetOrd) {
                try { targetOrd = sessionStorage.getItem('review_order_id'); } catch(e) {}
            }
            if (targetOrd) {
                if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/followup/review/' + targetOrd);
            } else {
                if ($window.history && $window.history.pushState) $window.history.pushState(null, '', '/admin/followups/feedback/' + cust.id);
            }
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
        var preset = $scope.selectedTimePreset ? ($scope.selectedTimePreset.presetKey || $scope.selectedTimePreset.name || $scope.selectedTimePreset.labelEn || String($scope.selectedTimePreset.id)) : null;
        
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
                    externalId: summary.externalId || (c ? c.externalId : null),
                    followupStatus: summary.followupStatus,
                    nextPendingFollowup: summary.nextPendingFollowup,
                    nextPendingOrderId: summary.nextPendingOrderId,
                    allFollowupsCompleted: summary.allFollowupsCompleted,
                    remainingFollowupsCount: summary.remainingFollowupsCount,
                    nextPendingMinutesLeft: summary.nextPendingMinutesLeft,
                    latestSatisfaction: summary.latestSatisfaction,
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
        $scope.customerFilters.dateFrom = FilterHelperService.getToday();
        $scope.customerFilters.dateTo = FilterHelperService.getToday();
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
            var remM = mins % 60;
            if (hours < 24) return remM > 0 ? (hours + 'h ' + remM + 'm') : (hours + 'h');
            var days = Math.floor(hours / 24);
            var remH = hours % 24;
            return remH > 0 ? (days + 'd ' + remH + 'h') : (days + 'd');
        } else {
            var overdue = Math.abs(mins);
            if (overdue < 60) return overdue + 'm overdue';
            var ovH = Math.floor(overdue / 60);
            var ovM = overdue % 60;
            if (ovH < 24) return ovM > 0 ? (ovH + 'h ' + ovM + 'm overdue') : (ovH + 'h overdue');
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
    $scope.loadCustomerOrders = function(customerId, targetOrderId, openReview) {
        if (!customerId) return;
        $scope.loadingOrders = true;
        FollowupService.getOrders(customerId).then(function(res) {
            $scope.loadingOrders = false;
            var raw = res.data;
            var list = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
            list.forEach(function(o) {
                if (!o.customerPhoneNumber && $scope.selectedCustomer) {
                    o.customerPhoneNumber = $scope.selectedCustomer.phoneNumber;
                }
            });
            $scope.customerOrders = list;

            if ($scope.customerView === 'feedback' && list && list.length > 0) {
                if (targetOrderId) {
                    $scope.orderFilters.search = String(targetOrderId);
                }
                list.forEach(function(o) {
                    if (!$scope.orderFollowups[o.id]) {
                        $scope.loadOrderFollowups(o.id);
                    }
                });
            }

            var chosenOrder = null;
            if (targetOrderId && list.length > 0) {
                for (var i = 0; i < list.length; i++) {
                    if (String(list[i].id) === String(targetOrderId) || String(list[i].orderNumber) === String(targetOrderId)) {
                        chosenOrder = list[i];
                        break;
                    }
                }
            }
            if (!chosenOrder && $scope.expandedOrderId && list.length > 0) {
                for (var j = 0; j < list.length; j++) {
                    if (list[j].id === $scope.expandedOrderId) {
                        chosenOrder = list[j];
                        break;
                    }
                }
            }
            if (!chosenOrder && list.length > 0) {
                chosenOrder = list[0];
            }

            if (chosenOrder) {
                $scope.expandedOrderId = chosenOrder.id;
                $scope.loadOrderFollowups(chosenOrder.id, function(checks) {
                    if (openReview) {
                        try {
                            sessionStorage.setItem('review_modal_opened_' + chosenOrder.id, 'true');
                            if (chosenOrder.orderNumber) {
                                sessionStorage.setItem('review_modal_opened_' + chosenOrder.orderNumber, 'true');
                            }
                        } catch(e) {}

                        // STRICT: Only auto-open if a milestone is actually DUE and NOT completed!
                        var targetCheck = null;
                        if (checks && checks.length > 0) {
                            for (var k = 0; k < checks.length; k++) {
                                if (!checks[k].isCompleted && checks[k].status !== 'COMPLETED' && checks[k].isDue) {
                                    targetCheck = checks[k];
                                    break;
                                }
                            }
                        }
                        if (targetCheck) {
                            $timeout(function() {
                                $scope.openEditFollowupModal(chosenOrder, targetCheck);
                                ToastService.success('Supervisor review session active. Please enter your feedback.');
                            }, 250);
                        }
                    }
                });
            }
        }, function(err) {
            $scope.loadingOrders = false;
            ToastService.error('Failed to load customer orders');
        });
    };

    /**
     * Determines if a milestone checkpoint has been reviewed.
     * Excludes pending, overdue, and unfinished checkpoints.
     * Strictly requires completion and actual review content (notes, satisfaction, or photos).
     */
    $scope.isCheckReviewed = function(check, order) {
        if (!check) return false;
        var status = (check.status || '').toUpperCase();

        // 1. Exclude Unfinished / Expired
        if (status === 'UNFINISHED' || status === 'EXPIRED') return false;
        if (order && $scope.isOrderUnfinished && $scope.isOrderUnfinished(order)) return false;

        // 2. Exclude Pending / Overdue / forcePending
        if (status === 'PENDING' || status === 'OVERDUE' || check.forcePending) return false;

        // 3. Must be completed
        var isCompleted = check.completed === true || check.isCompleted === true
                || status === 'COMPLETED' || status === 'FINISHED';
        if (!isCompleted) return false;

        // 4. Must have review content (satisfaction, notes, or image)
        var hasNotes = (check.note && check.note.trim().length > 0)
                || (check.notes && check.notes.trim().length > 0);
        var hasSatisfaction = check.satisfaction && check.satisfaction.trim().length > 0;
        var hasImages = (check.imageUrl && check.imageUrl.trim().length > 0)
                || (check.photoUrl && check.photoUrl.trim().length > 0)
                || (check.images && check.images.length > 0);

        return hasNotes || hasSatisfaction || hasImages;
    };

    /**
     * Returns ONLY reviewed checkpoints for an order (excludes pending and unfinished).
     */
    $scope.getReviewedFollowups = function(orderId, order) {
        var list = $scope.orderFollowups[orderId] || [];
        return list.filter(function(check) {
            return $scope.isCheckReviewed(check, order);
        });
    };

    /**
     * Loads follow-up check records for an order
     */
    $scope.loadOrderFollowups = function(orderId, callback) {
        if (!orderId) return;
        FollowupService.getFollowups(orderId).then(function(res) {
            var raw = res.data;
            var list = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
            $scope.orderFollowups[orderId] = list;
            if (typeof callback === 'function') {
                callback(list);
            }
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
        $scope.orderFilters.dateFrom = FilterHelperService.getToday();
        $scope.orderFilters.dateTo = FilterHelperService.getToday();
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
        if ($scope.isOrderUnfinished(order)) {
            return 'order-row-unfinished';
        }
        return '';
    };

    $scope.feedbackOrderExpanded = {};
    $scope.toggleOrderExpand = function(order, $event) {
        if ($event) $event.stopPropagation();
        if ($scope.customerView === 'feedback') {
            var curr = $scope.isOrderExpanded(order.id);
            $scope.feedbackOrderExpanded[order.id] = !curr;
            if (!curr && !$scope.orderFollowups[order.id]) {
                $scope.loadOrderFollowups(order.id);
            }
            return;
        }
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
        if ($scope.customerView === 'feedback') {
            if ($scope.feedbackOrderExpanded[orderId] !== undefined) {
                return $scope.feedbackOrderExpanded[orderId];
            }
            return true;
        }
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
        var remM = totalMins % 60;
        if (hours < 24) return remM > 0 ? (hours + 'h ' + remM + 'm ago') : (hours + 'h ago');
        var days = Math.floor(hours / 24);
        var remH = hours % 24;
        return remH > 0 ? (days + 'd ' + remH + 'h ago') : (days + 'd ago');
    };

    $scope.formatCountdown = function(minutes) {
        if (minutes == null || minutes < 0) return 'Soon';
        if (minutes < 60) return minutes + 'm';
        var hours = Math.floor(minutes / 60);
        var remM = minutes % 60;
        if (hours < 24) return remM > 0 ? (hours + 'h ' + remM + 'm') : (hours + 'h');
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
     * Helper to test if order is older than 4 months (120 days)
     */
    $scope.isOrderOlderThanFourMonths = function(order) {
        if (!order || !order.orderDate) return false;
        var orderTime = new Date(order.orderDate).getTime();
        var now = new Date().getTime();
        var diffDays = (now - orderTime) / (1000 * 60 * 60 * 24);
        return diffDays >= 120; // 4 months
    };

    /**
     * Dynamic Status Labels and Classes for Checkpoints:
     * 1. Finished: user added notes/photos, or marked complete
     * 2. Pending: if not older than 4 months, or if undone / deleted
     * 3. Unfinished: order older than 4 months (120 days) without feedback, or skipped
     */
    $scope.getCheckStatusLabel = function(check, order) {
        if (!check) return 'Pending';
        if (check.status === 'COMPLETED' || check.isCompleted || (check.note && check.note.trim().length > 0)) {
            return 'Finished';
        }
        if (check.forcePending) {
            return 'Pending';
        }
        if ($scope.isCheckUnfinished(check, order)) {
            return 'Unfinished';
        }
        return 'Pending';
    };

    $scope.getCheckStatusClass = function(check, order) {
        var label = $scope.getCheckStatusLabel(check, order);
        if (label === 'Finished') return 'tag-finished';
        if (label === 'Unfinished') return 'tag-expired is-unfinished';
        return 'tag-pending';
    };

    /**
     * Checks if an order was marked as Unfinished / Skipped
     */
    $scope.isOrderUnfinished = function(order) {
        if (!order || !order.id) return false;
        if (order.isExpired === true) return true;
        if (order.isExpired === false) return false;
        var checks = $scope.orderFollowups[order.id];
        if (checks && checks.length > 0) {
            var anyCompleted = false;
            var anyUnfinished = false;
            var anyPending = false;
            for (var i = 0; i < checks.length; i++) {
                if (checks[i].isCompleted || checks[i].status === 'COMPLETED' || (checks[i].note && checks[i].note.trim())) {
                    anyCompleted = true;
                } else if (checks[i].forcePending) {
                    anyPending = true;
                } else if (checks[i].status === 'UNFINISHED' || checks[i].status === 'EXPIRED' || checks[i].status === 'SKIPPED') {
                    anyUnfinished = true;
                } else if ($scope.isOrderOlderThanFourMonths(order)) {
                    anyUnfinished = true;
                } else {
                    anyPending = true;
                }
            }
            if (anyUnfinished && !anyPending) return true;
        } else if ($scope.isOrderOlderThanFourMonths(order)) {
            return true;
        }
        return false;
    };
    $scope.isOrderExpired = $scope.isOrderUnfinished;

    /**
     * Checks if a checkpoint is unfinished
     */
    $scope.isCheckUnfinished = function(check, order) {
        if (!check && !order) return false;
        if (check && (check.isCompleted || (check.note && check.note.trim().length > 0) || check.forcePending)) {
            return false;
        }
        if (order && $scope.isOrderUnfinished(order)) {
            return true;
        }
        if (check && (check.status === 'UNFINISHED' || check.status === 'EXPIRED' || check.status === 'SKIPPED')) {
            return true;
        }
        if (order && $scope.isOrderOlderThanFourMonths(order)) {
            return true;
        }
        if (check && check.orderId) {
            var ord = $scope.findOrderById ? $scope.findOrderById(check.orderId) : null;
            if (ord && ($scope.isOrderUnfinished(ord) || $scope.isOrderOlderThanFourMonths(ord))) {
                return true;
            }
        }
        return false;
    };
    $scope.isCheckExpired = $scope.isCheckUnfinished;

    /**
     * Toggles order unfinished state (Mark Unfinished <-> Reactivate / Undo)
     */
    $scope.toggleOrderUnfinished = function(order, $event) {
        if ($event) $event.stopPropagation();
        if (!order || !order.id) return;

        var isCurrentlyUnfinished = $scope.isOrderUnfinished(order);
        if (isCurrentlyUnfinished) {
            // Undo Unfinished / Reactivate to Pending
            FollowupService.unskipOrderChecks(order.id).then(function(res) {
                order.isExpired = false;
                if ($scope.orderFollowups[order.id]) {
                    $scope.orderFollowups[order.id].forEach(function(c) {
                        if (!c.isCompleted && (!c.note || !c.note.trim())) {
                            c.status = 'PENDING';
                            c.forcePending = true;
                        }
                    });
                }
                ToastService.success('Order #' + (order.orderNumber || order.id) + ' restored to Pending');
                $scope.loadOrderFollowups(order.id);
                $scope.loadCustomers();
            }, function() {
                ToastService.error('Failed to restore order checkpoints');
            });
        } else {
            // Mark Unfinished
            FollowupService.skipOrderChecks(order.id).then(function(res) {
                order.isExpired = true;
                if ($scope.orderFollowups[order.id]) {
                    $scope.orderFollowups[order.id].forEach(function(c) {
                        if (!c.isCompleted && (!c.note || !c.note.trim())) {
                            c.status = 'UNFINISHED';
                        }
                    });
                }
                ToastService.warning('Order #' + (order.orderNumber || order.id) + ' marked as Unfinished');
                $scope.loadOrderFollowups(order.id);
                $scope.loadCustomers();
            }, function() {
                ToastService.error('Failed to mark order as Unfinished');
            });
        }
    };
    $scope.toggleOrderExpire = $scope.toggleOrderUnfinished;
    $scope.skipOrderChecks = $scope.toggleOrderExpire;

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
        // Strict guard: cannot add feedback if order is unfinished
        if ((order && $scope.isOrderUnfinished(order)) || (check && (check.status === 'UNFINISHED' || check.status === 'EXPIRED'))) {
            ToastService.error('This order is marked as Unfinished. Click "Undo Unfinished" first to reactivate checkpoints.');
            return;
        }
        // Strict guard: cannot follow-up on future milestones that are not due yet
        if (check && !check.isCompleted && !check.isDue && !check.forcePending && check.status !== 'UNFINISHED') {
            ToastService.warning('This milestone is locked. Follow-up unlocks when the milestone reaches its due time.');
            return;
        }
        var item = angular.copy(check) || {};
        item.orderId = order ? order.id : null;
        item.orderNumber = order ? (order.orderNumber || order.id) : null;
        item.milestoneName = check ? (check.milestoneName || check.presetName || 'Milestone Review') : 'Milestone Review';
        item.presetName = item.milestoneName;
        item.note = check.note || check.notes || '';
        item.imageUrl = check.imageUrl || check.photoUrl || '';
        item.images = angular.copy($scope.getCheckImages(check));
        item.satisfaction = (check.satisfaction && check.satisfaction.trim() !== '') ? check.satisfaction.toUpperCase() : 'NEUTRAL';
        item.status = check.status ? check.status.toUpperCase() : (check.isCompleted ? 'COMPLETED' : 'PENDING');
        $scope.editingFollowup = item;
        $scope.tempImageUrl = '';
        $scope.showEditFollowupModal = true;
        if ($scope.$root) {
            $scope.$root.editingFollowup = item;
            $scope.$root.showEditFollowupModal = true;
        }
        if ($scope.$parent && $scope.$parent.openEditFollowupModal) {
            $scope.$parent.openEditFollowupModal(order, check, $event);
        }
    };

    $scope.onFeedbackNoteChange = function() {
        if (!$scope.editingFollowup) return;
        var hasNote = $scope.editingFollowup.note && $scope.editingFollowup.note.trim().length > 0;
        var hasImages = $scope.editingFollowup.images && $scope.editingFollowup.images.length > 0;
        if (hasNote || hasImages) {
            $scope.editingFollowup.status = 'COMPLETED';
        } else if ($scope.editingFollowup.status !== 'UNFINISHED') {
            $scope.editingFollowup.status = 'PENDING';
        }
    };

    $scope.closeEditFollowupModal = function() {
        if ($scope.editingFollowup && $scope.editingFollowup.orderId) {
            try {
                sessionStorage.setItem('review_modal_opened_' + $scope.editingFollowup.orderId, 'true');
                if ($scope.editingFollowup.orderNumber) {
                    sessionStorage.setItem('review_modal_opened_' + $scope.editingFollowup.orderNumber, 'true');
                }
            } catch(e) {}
        }
        $scope.showEditFollowupModal = false;
        $scope.editingFollowup = {};
        $scope.tempImageUrl = '';
        if ($scope.$root) {
            $scope.$root.showEditFollowupModal = false;
            $scope.$root.editingFollowup = {};
        }
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
        var finalImages = $scope.editingFollowup.images || [];
        var finalImageUrl = finalImages.length > 0 ? JSON.stringify(finalImages) : '';
        $scope.editingFollowup.imageUrl = finalImageUrl;

        var noteText = $scope.editingFollowup.note || '';
        // If user provided notes or proof photos, mark as Finished (COMPLETED). If cleared, it transitions to PENDING!
        var hasFeedback = (noteText.trim().length > 0 || finalImages.length > 0);
        var sat = hasFeedback ? ($scope.editingFollowup.satisfaction || 'NEUTRAL') : null;
        var finalStatus = hasFeedback ? 'COMPLETED' : 'PENDING';

        var checkId = $scope.editingFollowup.id;
        var orderId = $scope.editingFollowup.orderId;

        var payload = {
            notes: noteText,
            photoUrl: finalImageUrl,
            imageUrl: finalImageUrl,
            satisfaction: sat || '',
            status: finalStatus
        };
        FollowupService.updateFollowup(checkId, payload).then(function(res) {
            ToastService.success(hasFeedback ? 'Feedback saved successfully' : 'Feedback cleared. Milestone restored to Pending.');
            var isComp = (finalStatus === 'COMPLETED');
            if ($scope.selectedCustomer && sat) {
                $scope.selectedCustomer.latestSatisfaction = sat;
            }
            if (orderId && $scope.orderFollowups[orderId]) {
                var list = $scope.orderFollowups[orderId];
                for (var i = 0; i < list.length; i++) {
                    if (list[i].id === checkId) {
                        list[i].note = noteText;
                        list[i].notes = noteText;
                        list[i].imageUrl = finalImageUrl;
                        list[i].photoUrl = finalImageUrl;
                        list[i].satisfaction = sat;
                        list[i].status = finalStatus;
                        list[i].isCompleted = isComp;
                        list[i].completed = isComp;
                        if (!isComp) {
                            list[i].forcePending = true;
                        }
                        break;
                    }
                }
            }
            $scope.closeEditFollowupModal();
            if (orderId) {
                $scope.loadOrderFollowups(orderId);
            }
            $scope.loadCustomers();
        }, function(err) {
            ToastService.error('Failed to save feedback');
        });
    };

    $scope.deleteFollowupFeedback = function(check) {
        if (!check || !check.id) return;
        var checkId = check.id;
        var orderId = check.orderId;
        var payload = {
            notes: '',
            photoUrl: '',
            imageUrl: '',
            satisfaction: '',
            status: 'PENDING'
        };
        FollowupService.updateFollowup(checkId, payload).then(function(res) {
            ToastService.success('Feedback deleted. Milestone restored to Pending.');
            if (check) {
                check.note = '';
                check.notes = '';
                check.images = [];
                check.imageUrl = '';
                check.photoUrl = '';
                check.satisfaction = null;
                check.status = 'PENDING';
                check.isCompleted = false;
                check.completed = false;
                check.forcePending = true;
            }
            if (orderId && $scope.orderFollowups[orderId]) {
                var list = $scope.orderFollowups[orderId];
                for (var i = 0; i < list.length; i++) {
                    if (list[i].id === checkId) {
                        list[i].note = '';
                        list[i].notes = '';
                        list[i].images = [];
                        list[i].imageUrl = '';
                        list[i].photoUrl = '';
                        list[i].satisfaction = null;
                        list[i].status = 'PENDING';
                        list[i].isCompleted = false;
                        list[i].completed = false;
                        list[i].forcePending = true;
                        break;
                    }
                }
            }
            $scope.closeEditFollowupModal();
            if (orderId) {
                $scope.loadOrderFollowups(orderId);
            }
            $scope.loadCustomers();
        }, function(err) {
            ToastService.error('Failed to delete feedback');
        });
    };

    $scope.getOrderSatisfaction = function(order) {
        if (!order || !order.id) return null;
        var checks = $scope.orderFollowups[order.id];
        if (!checks || !checks.length) return null;
        var latestSat = null;
        for (var i = 0; i < checks.length; i++) {
            if (checks[i].satisfaction && checks[i].satisfaction.trim().length > 0) {
                latestSat = checks[i].satisfaction.trim().toUpperCase();
            }
        }
        return latestSat;
    };

    $scope.getCustomerSatisfaction = function(cust) {
        if (!cust) return null;
        if (cust.latestSatisfaction && cust.latestSatisfaction.trim().length > 0) {
            return cust.latestSatisfaction.trim().toUpperCase();
        }
        if ($scope.selectedCustomer && cust.id === $scope.selectedCustomer.id && $scope.customerOrders) {
            for (var i = 0; i < $scope.customerOrders.length; i++) {
                var ordSat = $scope.getOrderSatisfaction($scope.customerOrders[i]);
                if (ordSat) return ordSat;
            }
        }
        return null;
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
        if ($scope.selectedCustomer && data && data.satisfaction) {
            $scope.selectedCustomer.latestSatisfaction = data.satisfaction;
        }
        if (data && data.orderId && $scope.orderFollowups[data.orderId]) {
            var list = $scope.orderFollowups[data.orderId];
            for (var i = 0; i < list.length; i++) {
                if (list[i].id === data.checkId) {
                    list[i].note = data.note;
                    list[i].notes = data.note;
                    list[i].imageUrl = data.imageUrl;
                    list[i].photoUrl = data.imageUrl;
                    list[i].satisfaction = data.satisfaction;
                    list[i].status = data.status || 'COMPLETED';
                    list[i].isCompleted = (data.isCompleted != null) ? data.isCompleted : (list[i].status === 'COMPLETED');
                    list[i].completed = list[i].isCompleted;
                    break;
                }
            }
        }
        if (data && data.orderId) {
            $scope.loadOrderFollowups(data.orderId);
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

        var headers = ['ID', 'Customer Name', 'Total Orders', 'Satisfaction', 'Follow-up Status', 'Remaining Checks', 'Next Scheduled Check'];
        var rows = custs.map(function(c) {
            var statusLabel = (c.followupStatus === 'ALERT' || c.followupStatus === 'OVERDUE') ? 'Need Follow-up' : (c.allFollowupsCompleted ? 'Finished' : 'Queued');
            var satLabel = c.latestSatisfaction || 'None';
            return [
                c.id,
                c.name || '',
                c.totalOrders != null ? c.totalOrders : 0,
                satLabel,
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
                latestSatisfaction: summary.latestSatisfaction,
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
