/**
 * Twekl Management Dashboard - AngularJS 1.8.x Client Application
 * - Grouped Sidebar: "Administration" Accordion (Admins, Users, Roles) + Customer Follow-up
 * - Modern Filter Toolbar: Date From/To, Extra Filters Pop-up Modal, Live Search Bar
 * - Full DataTables: Multi-column sorting, Dynamic Pagination (5/10/25/50 per page), Page Navigation
 * - Spring Security Server-Side Protection Integration (401/403 alerts, Login/Logout)
 * - Comic / Neobrutalism Design System with Unified Twekl Teal Palette
 */
var app = angular.module('tweklApp', []);

// Configure HTTP interceptor to catch 401/403 Spring Security rejections
app.config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push(['$q', '$rootScope', function($q, $rootScope) {
        return {
            'responseError': function(rejection) {
                if (rejection.status === 401) {
                    $rootScope.$broadcast('auth:unauthorized', rejection.data);
                } else if (rejection.status === 403) {
                    $rootScope.$broadcast('auth:forbidden', rejection.data);
                }
                return $q.reject(rejection);
            }
        };
    }]);
}]);

app.controller('DashboardController', ['$scope', '$http', '$timeout', '$window', function($scope, $http, $timeout, $window) {

    // Sidebar Category Accordion State
    $scope.categories = {
        admin: true // Administration accordion open by default
    };

    $scope.toggleCategory = function(cat) {
        $scope.categories[cat] = !$scope.categories[cat];
    };

    $scope.isCategoryOpen = function(cat) {
        return !!$scope.categories[cat];
    };

    $scope.currentTab = 'admins'; // 'admins', 'users', 'roles', or 'customers'
    $scope.adminView = 'list'; // 'list', 'create', 'inspect', 'delete'
    $scope.userView = 'list'; // 'list', 'create', 'inspect', 'update', 'delete'
    $scope.customerView = 'list'; // 'list', 'orders', 'feedback'
    $scope.customerTimeFilter = 'all'; // 'all', '24h', '7d', '30d'
    $scope.currentLang = 'en';

    // Spring Security Authentication State
    $scope.auth = {
        authenticated: false,
        username: '',
        role: '',
        isSuperAdmin: false,
        canCreateRoles: false,
        canManageUsers: false
    };

    $scope.showLoginModal = false;
    $scope.loginForm = {
        username: '',
        password: ''
    };

    // =========================================================
    // FILTER TOOLBAR & PAGINATION STATE
    // =========================================================
    $scope.filters = {
        search: '',
        dateFrom: '',
        dateTo: ''
    };

    // =========================================================
    // DYNAMIC TIME FILTER PRESETS (ADMIN CONFIGURABLE)
    // =========================================================
    $scope.timeFilterPresets = [];
    $scope.selectedTimePreset = null; // Currently active preset filter (or null for all)
    $scope.newPreset = {
        name: '',
        durationValue: 24,
        durationUnit: 'HOURS',
        isActive: true
    };
    $scope.showPresetManager = false; // toggle admin manager section in picker modal

    $scope.loadTimeFilterPresets = function() {
        return $http.get('/api/time-filters').then(function(res) {
            $scope.timeFilterPresets = res.data || [];
        });
    };

    $scope.createTimeFilterPreset = function() {
        if (!$scope.newPreset.name || !$scope.newPreset.durationValue) {
            $scope.showToast('Please enter preset name and duration value', 'error');
            return;
        }
        $http.post('/api/time-filters', $scope.newPreset).then(function(res) {
            $scope.showToast('Custom filter preset "' + res.data.name + '" created successfully!');
            $scope.newPreset = {
                name: '',
                durationValue: 24,
                durationUnit: 'HOURS',
                isActive: true
            };
            $scope.loadTimeFilterPresets().then(function() {
                if ($scope.customerOrders && $scope.customerOrders.length > 0) {
                    $scope.customerOrders.forEach(function(order) {
                        $scope.loadOrderFollowups(order.id);
                    });
                }
            });
        }, function(err) {
            $scope.showToast('Failed to create filter preset', 'error');
        });
    };

    $scope.toggleTimeFilterPreset = function(preset) {
        $http.patch('/api/time-filters/' + preset.id + '/toggle', {}).then(function(res) {
            preset.isActive = res.data.isActive;
            $scope.showToast('Filter preset "' + preset.name + '" set to ' + (preset.isActive ? 'Active' : 'Inactive'));
            if ($scope.customerOrders && $scope.customerOrders.length > 0) {
                $scope.customerOrders.forEach(function(order) {
                    $scope.loadOrderFollowups(order.id);
                });
            }
        });
    };

    $scope.deleteTimeFilterPreset = function(preset) {
        if (!confirm('Are you sure you want to delete filter preset "' + preset.name + '"?')) return;
        $http.delete('/api/time-filters/' + preset.id).then(function() {
            $scope.showToast('Filter preset deleted');
            if ($scope.selectedTimePreset && $scope.selectedTimePreset.id === preset.id) {
                $scope.selectedTimePreset = null;
            }
            $scope.loadTimeFilterPresets().then(function() {
                if ($scope.customerOrders && $scope.customerOrders.length > 0) {
                    $scope.customerOrders.forEach(function(order) {
                        $scope.loadOrderFollowups(order.id);
                    });
                }
            });
        });
    };

    $scope.selectTimePreset = function(preset) {
        if ($scope.selectedTimePreset && $scope.selectedTimePreset.id === (preset ? preset.id : null)) {
            $scope.selectedTimePreset = null; // toggle off
        } else {
            $scope.selectedTimePreset = preset;
        }
        $scope.dt.currentPage = 1;
    };

    $scope.selectTimePreset = function(preset) {
        if ($scope.selectedTimePreset && $scope.selectedTimePreset.id === (preset ? preset.id : null)) {
            $scope.selectedTimePreset = null; // toggle off
        } else {
            $scope.selectedTimePreset = preset;
        }
        $scope.dt.currentPage = 1;
    };

    // =========================================================
    // BIG DYNAMIC TIME FILTER SELECTOR MODAL CONTROLS
    // =========================================================
    $scope.showBigFilterModal = false;
    $scope.filterModalContext = 'customer'; // 'customer', 'order', 'feedback', 'user', 'admin'
    $scope.pendingTimePreset = null;
    $scope.filterModalSearch = '';

    $scope.openFilterModal = function(context) {
        $scope.filterModalContext = context || 'customer';
        $scope.filterModalSearch = '';
        
        // Sync pending selection with current active filter
        if ($scope.filterModalContext === 'order') {
            $scope.pendingTimePreset = $scope.selectedOrderTimePreset ? angular.copy($scope.selectedOrderTimePreset) : null;
        } else if ($scope.filterModalContext === 'feedback') {
            $scope.pendingTimePreset = $scope.selectedFeedbackTimePreset ? angular.copy($scope.selectedFeedbackTimePreset) : null;
        } else {
            $scope.pendingTimePreset = $scope.selectedTimePreset ? angular.copy($scope.selectedTimePreset) : null;
        }
        
        $scope.showBigFilterModal = true;
    };

    $scope.closeFilterModal = function() {
        $scope.showBigFilterModal = false;
        $scope.pendingTimePreset = null;
    };

    $scope.selectPendingPreset = function(preset) {
        $scope.pendingTimePreset = preset;
    };

    $scope.isPendingPresetSelected = function(preset) {
        if (!preset && !$scope.pendingTimePreset) return true;
        if (preset && $scope.pendingTimePreset && preset.id === $scope.pendingTimePreset.id) return true;
        return false;
    };

    $scope.getFilteredModalPresets = function() {
        if (!$scope.timeFilterPresets) return [];
        var active = $scope.timeFilterPresets.filter(function(p) { return p.isActive; });
        var q = ($scope.filterModalSearch || '').toLowerCase().trim();
        if (!q) return active;
        return active.filter(function(p) {
            return (p.name && p.name.toLowerCase().indexOf(q) !== -1) ||
                   ((p.durationValue + ' ' + p.durationUnit).toLowerCase().indexOf(q) !== -1);
        });
    };

    $scope.confirmApplyFilterModal = function() {
        if ($scope.filterModalContext === 'order') {
            $scope.selectedOrderTimePreset = $scope.pendingTimePreset;
            $scope.orderDt.currentPage = 1;
            if ($scope.pendingTimePreset) {
                $scope.showToast('Applied order filter: ' + $scope.pendingTimePreset.name);
            } else {
                $scope.showToast('Order filter set to All Time');
            }
        } else if ($scope.filterModalContext === 'feedback') {
            $scope.selectedFeedbackTimePreset = $scope.pendingTimePreset;
            $scope.feedbackDt.currentPage = 1;
            if ($scope.pendingTimePreset) {
                $scope.showToast('Applied review filter: ' + $scope.pendingTimePreset.name);
            } else {
                $scope.showToast('Review filter set to All Time');
            }
        } else {
            $scope.selectedTimePreset = $scope.pendingTimePreset;
            $scope.dt.currentPage = 1;
            if ($scope.pendingTimePreset) {
                $scope.showToast('Applied time filter: ' + $scope.pendingTimePreset.name);
            } else {
                $scope.showToast('Time filter set to All Time');
            }
        }
        $scope.showBigFilterModal = false;
    };

    $scope.resetFilterModal = function() {
        $scope.pendingTimePreset = null;
        $scope.confirmApplyFilterModal();
    };

    // Legacy fallback bindings
    $scope.showFilterDropdown = false;
    $scope.toggleFilterDropdown = function($event) {
        if ($event) $event.stopPropagation();
        $scope.openFilterModal('customer');
    };
    $scope.closeFilterDropdown = function() {
        $scope.showBigFilterModal = false;
    };
    $scope.applyTimePreset = function(preset) {
        $scope.selectPendingPreset(preset);
        $scope.confirmApplyFilterModal();
    };

    // =========================================================
    // DATATABLE CONTROLS & PAGINATION STATE (30 ROWS DEFAULT)
    // =========================================================
    $scope.dt = {
        pageSize: 30,
        currentPage: 1,
        sortField: 'id',
        sortReverse: false
    };

    $scope.setPageSize = function(size) {
        $scope.dt.pageSize = parseInt(size, 10) || 30;
        $scope.dt.currentPage = 1;
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
        if ($scope.dt.sortField !== field) return '⇅';
        return $scope.dt.sortReverse ? '▼' : '▲';
    };

    $scope.setPage = function(p) {
        if (p < 1) return;
        $scope.dt.currentPage = p;
    };

    $scope.getPageNumbers = function(totalItems) {
        var totalPages = Math.ceil(totalItems / $scope.dt.pageSize) || 1;
        var pages = [];
        for (var i = 1; i <= totalPages; i++) {
            pages.push(i);
        }
        return pages;
    };

    $scope.getTotalPages = function(totalItems) {
        return Math.ceil(totalItems / $scope.dt.pageSize) || 1;
    };

    // Generic Date Matcher Helper
    function isWithinDateRange(itemDateStr, fromStr, toStr) {
        if (!fromStr && !toStr) return true;
        if (!itemDateStr) return true;
        var itemDate = new Date(itemDateStr).getTime();
        if (isNaN(itemDate)) return true;
        if (fromStr) {
            var fromDate = new Date(fromStr).getTime();
            if (itemDate < fromDate) return false;
        }
        if (toStr) {
            var toDate = new Date(toStr).getTime() + (24 * 60 * 60 * 1000 - 1); // end of day
            if (itemDate > toDate) return false;
        }
        return true;
    }

    // Flexible Multi-Token Search Matcher Helper (handles spaces, underscores, dashes, case-insensitive)
    function searchMatch(haystack, needle) {
        if (!needle || !needle.trim()) return true;
        if (!haystack) return false;
        var rawHaystack = ('' + haystack).toLowerCase();
        var normHaystack = rawHaystack.replace(/[_.\-\/]/g, ' ');
        var normNeedle = ('' + needle).toLowerCase().trim().replace(/[_.\-\/]/g, ' ');
        if (!normNeedle) return true;
        
        // Exact substring match on normalized text
        if (normHaystack.indexOf(normNeedle) !== -1 || rawHaystack.indexOf(('' + needle).toLowerCase().trim()) !== -1) {
            return true;
        }

        // Tokenized match: every word in search query matches somewhere in haystack
        var terms = normNeedle.split(/\s+/).filter(Boolean);
        if (terms.length > 0) {
            return terms.every(function(term) {
                return normHaystack.indexOf(term) !== -1 || rawHaystack.indexOf(term) !== -1;
            });
        }
        return false;
    }

    // Dynamic Time Filter Preset Matcher Helper (Admins & Users creation dates)
    function matchesPreset(itemDateStr, preset) {
        if (!preset) return true;
        if (!itemDateStr) return true;
        var itemTime = new Date(itemDateStr).getTime();
        if (isNaN(itemTime)) return true;
        var totalHours = 24;
        var val = preset.durationValue || 24;
        var unit = (preset.durationUnit || 'HOURS').toUpperCase();
        if (unit === 'HOURS') totalHours = val;
        else if (unit === 'DAYS') totalHours = val * 24;
        else if (unit === 'WEEKS') totalHours = val * 24 * 7;
        else if (unit === 'MONTHS') totalHours = val * 24 * 30;

        var cutoff = Date.now() - (totalHours * 3600 * 1000);
        return itemTime >= cutoff;
    }

    // Dynamic Time Filter Preset Matcher for Customers (Recency & Follow-up Interval Buckets)
    function matchesCustomerPreset(c, preset) {
        if (!preset) return true;
        
        // Compute days ago from customer record
        var daysAgo = c.daysSinceLastOrder;
        var itemDateStr = c.lastOrderDate || c.createdAt;
        if (itemDateStr) {
            var itemTime = new Date(itemDateStr).getTime();
            if (!isNaN(itemTime)) {
                var diffMs = Date.now() - itemTime;
                var calculatedDays = Math.max(0, Math.floor(diffMs / (1000 * 3600 * 24)));
                if (daysAgo == null || isNaN(daysAgo)) {
                    daysAgo = calculatedDays;
                }
            }
        }
        if (daysAgo == null || isNaN(daysAgo)) daysAgo = 0;

        var val = parseInt(preset.durationValue, 10) || 24;
        var unit = (preset.durationUnit || 'HOURS').toUpperCase();
        var targetDays = val;

        if (unit === 'HOURS') {
            targetDays = val / 24;
        } else if (unit === 'DAYS') {
            targetDays = val;
        } else if (unit === 'WEEKS') {
            targetDays = val * 7;
        } else if (unit === 'MONTHS') {
            targetDays = val * 30;
        }

        var presetNameLower = (preset.name || '').toLowerCase();

        // 1. Hours / 24 Hours / 1 Day recency bucket
        if (unit === 'HOURS' || targetDays <= 1 || presetNameLower.includes('24') || presetNameLower.includes('hour')) {
            return daysAgo <= 1;
        }

        // 2. Dormant / 30+ Days bucket
        if (targetDays >= 28 || presetNameLower.includes('dormant') || presetNameLower.includes('30+')) {
            return daysAgo >= 25;
        }

        // 3. Milestone Buckets (e.g. 7 Days Ago = ~1 week, 2 Weeks Ago = ~14 days)
        var minDays = Math.max(2, Math.floor(targetDays * 0.7));
        var maxDays = Math.ceil(targetDays * 1.3) + 1;
        return daysAgo >= minDays && daysAgo <= maxDays;
    }

    // Filtered Admins DataTable
    $scope.getFilteredAdmins = function() {
        if (!$scope.admins) return [];
        var q = $scope.filters.search;
        return $scope.admins.filter(function(a) {
            // Flexible Search filter across admin attributes
            if (q && !searchMatch([a.username, a.phoneNumber, a.status, a.superAdmin ? 'super admin' : 'administrator', '#' + a.id].join(' '), q)) {
                return false;
            }
            // Dynamic Time Filter Preset
            if (!matchesPreset(a.createdAt, $scope.selectedTimePreset)) return false;
            // Date Range
            if (!isWithinDateRange(a.createdAt, $scope.filters.dateFrom, $scope.filters.dateTo)) return false;
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.dt.sortField];
            var valB = b[$scope.dt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.dt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.dt.sortReverse ? -1 : 1;
            return 0;
        });
    };

    $scope.getPagedAdmins = function() {
        var filtered = $scope.getFilteredAdmins();
        var start = ($scope.dt.currentPage - 1) * $scope.dt.pageSize;
        return filtered.slice(start, start + $scope.dt.pageSize);
    };

    // Filtered Users DataTable
    $scope.getFilteredUsers = function() {
        if (!$scope.users) return [];
        var q = $scope.filters.search;
        return $scope.users.filter(function(u) {
            // Flexible Search filter across multi-lingual user attributes
            if (q && !searchMatch([u.usernameEn, u.usernameAr, u.usernameKu, u.phoneNumber, u.status, '#' + u.id].join(' '), q)) {
                return false;
            }
            // Dynamic Time Filter Preset
            if (!matchesPreset(u.createdAt, $scope.selectedTimePreset)) return false;
            // Date Range
            if (!isWithinDateRange(u.createdAt, $scope.filters.dateFrom, $scope.filters.dateTo)) return false;
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.dt.sortField];
            var valB = b[$scope.dt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.dt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.dt.sortReverse ? -1 : 1;
            return 0;
        });
    };

    $scope.getPagedUsers = function() {
        var filtered = $scope.getFilteredUsers();
        var start = ($scope.dt.currentPage - 1) * $scope.dt.pageSize;
        return filtered.slice(start, start + $scope.dt.pageSize);
    };

    // Filtered Role Templates DataTable
    $scope.getFilteredRoles = function() {
        if (!$scope.roles) return [];
        var q = $scope.filters.search;
        return $scope.roles.filter(function(r) {
            if (q && !searchMatch([r.name, r.description, '#' + r.id].join(' '), q)) {
                return false;
            }
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.dt.sortField];
            var valB = b[$scope.dt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.dt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.dt.sortReverse ? -1 : 1;
            return 0;
        });
    };

    $scope.getPagedRoles = function() {
        var filtered = $scope.getFilteredRoles();
        var start = ($scope.dt.currentPage - 1) * $scope.dt.pageSize;
        return filtered.slice(start, start + $scope.dt.pageSize);
    };

    // Filtered Customers DataTable
    $scope.getFilteredCustomers = function() {
        if (!$scope.customers) return [];
        var q = $scope.filters.search;
        return $scope.customers.filter(function(c) {
            // Flexible Search filter across customer name, phone, email, city, status
            if (q && !searchMatch([c.name, c.phoneNumber, c.email, c.city, c.status, '#' + c.id].join(' '), q)) {
                return false;
            }
            // Dynamic Time Filter Preset (Smart Recency Bucketing)
            if (!matchesCustomerPreset(c, $scope.selectedTimePreset)) return false;
            // Date Range
            if (!isWithinDateRange(c.lastOrderDate || c.createdAt, $scope.filters.dateFrom, $scope.filters.dateTo)) return false;
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.dt.sortField];
            var valB = b[$scope.dt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.dt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.dt.sortReverse ? -1 : 1;
            return 0;
        });
    };

    $scope.getPagedCustomers = function() {
        var filtered = $scope.getFilteredCustomers();
        var start = ($scope.dt.currentPage - 1) * $scope.dt.pageSize;
        return filtered.slice(start, start + $scope.dt.pageSize);
    };

    // =========================================================
    // 1. CUSTOMER ORDERS DATATABLE CONTROLS
    // =========================================================
    $scope.orderDt = {
        sortField: 'orderDate',
        sortReverse: true,
        currentPage: 1,
        pageSize: 10
    };
    $scope.orderFilters = {
        search: '',
        dateFrom: null,
        dateTo: null
    };
    $scope.selectedOrderTimePreset = null;
    $scope.showOrderFilterDropdown = false;

    $scope.toggleOrderFilterDropdown = function($event) {
        if ($event) $event.stopPropagation();
        $scope.showOrderFilterDropdown = !$scope.showOrderFilterDropdown;
    };
    $scope.closeOrderFilterDropdown = function() {
        $scope.showOrderFilterDropdown = false;
    };
    $scope.applyOrderTimePreset = function(preset) {
        $scope.selectedOrderTimePreset = preset;
        $scope.showOrderFilterDropdown = false;
        $scope.orderDt.currentPage = 1;
        if (preset) {
            $scope.showToast('Order filter set to: ' + preset.name);
        } else {
            $scope.showToast('Order filter set to All Time');
        }
    };
    $scope.selectOrderTimePreset = function(preset) {
        $scope.selectedOrderTimePreset = preset;
        $scope.orderDt.currentPage = 1;
    };
    $scope.sortOrdersBy = function(field) {
        if ($scope.orderDt.sortField === field) {
            $scope.orderDt.sortReverse = !$scope.orderDt.sortReverse;
        } else {
            $scope.orderDt.sortField = field;
            $scope.orderDt.sortReverse = false;
        }
        $scope.orderDt.currentPage = 1;
    };
    $scope.getOrderSortIndicator = function(field) {
        if ($scope.orderDt.sortField !== field) return '↕';
        return $scope.orderDt.sortReverse ? '▼' : '▲';
    };
    $scope.getFilteredCustomerOrders = function() {
        if (!$scope.customerOrders) return [];
        var q = $scope.orderFilters.search;
        return $scope.customerOrders.filter(function(o) {
            if (q && !searchMatch([o.orderNumber, o.itemsSummary, o.orderStatus, o.paymentMethod, '$' + o.totalAmount, '#' + o.id].join(' '), q)) {
                return false;
            }
            if (!matchesPreset(o.orderDate, $scope.selectedOrderTimePreset)) return false;
            if (!isWithinDateRange(o.orderDate, $scope.orderFilters.dateFrom, $scope.orderFilters.dateTo)) return false;
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.orderDt.sortField];
            var valB = b[$scope.orderDt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.orderDt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.orderDt.sortReverse ? -1 : 1;
            return 0;
        });
    };
    $scope.getPagedCustomerOrders = function() {
        var filtered = $scope.getFilteredCustomerOrders();
        var start = ($scope.orderDt.currentPage - 1) * $scope.orderDt.pageSize;
        return filtered.slice(start, start + $scope.orderDt.pageSize);
    };
    $scope.setOrderPage = function(page) {
        if (page < 1 || page > $scope.getTotalOrderPages($scope.getFilteredCustomerOrders().length)) return;
        $scope.orderDt.currentPage = page;
    };
    $scope.getTotalOrderPages = function(totalItems) {
        return Math.ceil(totalItems / $scope.orderDt.pageSize) || 1;
    };
    $scope.getOrderPageNumbers = function(totalItems) {
        var total = $scope.getTotalOrderPages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    // =========================================================
    // 2. CUSTOMER REVIEWS & COMPLIMENTS DATATABLE CONTROLS
    // =========================================================
    $scope.feedbackDt = {
        sortField: 'createdAt',
        sortReverse: true,
        currentPage: 1,
        pageSize: 10
    };
    $scope.feedbackFilters = {
        search: '',
        type: 'ALL',
        dateFrom: null,
        dateTo: null
    };
    $scope.selectedFeedbackTimePreset = null;
    $scope.showFeedbackFilterDropdown = false;

    $scope.toggleFeedbackFilterDropdown = function($event) {
        if ($event) $event.stopPropagation();
        $scope.showFeedbackFilterDropdown = !$scope.showFeedbackFilterDropdown;
    };
    $scope.closeFeedbackFilterDropdown = function() {
        $scope.showFeedbackFilterDropdown = false;
    };
    $scope.applyFeedbackTimePreset = function(preset) {
        $scope.selectedFeedbackTimePreset = preset;
        $scope.showFeedbackFilterDropdown = false;
        $scope.feedbackDt.currentPage = 1;
        if (preset) {
            $scope.showToast('Feedback filter set to: ' + preset.name);
        } else {
            $scope.showToast('Feedback filter set to All Time');
        }
    };
    $scope.selectFeedbackTimePreset = function(preset) {
        $scope.selectedFeedbackTimePreset = preset;
        $scope.feedbackDt.currentPage = 1;
    };
    $scope.setFeedbackTypeFilter = function(type) {
        $scope.feedbackFilters.type = type;
        $scope.feedbackDt.currentPage = 1;
    };
    $scope.sortFeedbacksBy = function(field) {
        if ($scope.feedbackDt.sortField === field) {
            $scope.feedbackDt.sortReverse = !$scope.feedbackDt.sortReverse;
        } else {
            $scope.feedbackDt.sortField = field;
            $scope.feedbackDt.sortReverse = false;
        }
        $scope.feedbackDt.currentPage = 1;
    };
    $scope.getFeedbackSortIndicator = function(field) {
        if ($scope.feedbackDt.sortField !== field) return '↕';
        return $scope.feedbackDt.sortReverse ? '▼' : '▲';
    };
    $scope.getFilteredCustomerFeedbacks = function() {
        if (!$scope.customerFeedbacks) return [];
        var q = $scope.feedbackFilters.search;
        var type = $scope.feedbackFilters.type;
        return $scope.customerFeedbacks.filter(function(fb) {
            if (type && type !== 'ALL' && fb.feedbackType !== type) {
                return false;
            }
            if (q && !searchMatch([fb.customerName, fb.authorName, fb.feedbackType, fb.content, fb.orderNumber, fb.orderSummary, fb.status, '#' + fb.id].join(' '), q)) {
                return false;
            }
            if (!matchesPreset(fb.createdAt, $scope.selectedFeedbackTimePreset)) return false;
            if (!isWithinDateRange(fb.createdAt, $scope.feedbackFilters.dateFrom, $scope.feedbackFilters.dateTo)) return false;
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.feedbackDt.sortField];
            var valB = b[$scope.feedbackDt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.feedbackDt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.feedbackDt.sortReverse ? -1 : 1;
            return 0;
        });
    };
    $scope.getPagedCustomerFeedbacks = function() {
        var filtered = $scope.getFilteredCustomerFeedbacks();
        var start = ($scope.feedbackDt.currentPage - 1) * $scope.feedbackDt.pageSize;
        return filtered.slice(start, start + $scope.feedbackDt.pageSize);
    };
    $scope.setFeedbackPage = function(page) {
        if (page < 1 || page > $scope.getTotalFeedbackPages($scope.getFilteredCustomerFeedbacks().length)) return;
        $scope.feedbackDt.currentPage = page;
    };
    $scope.getTotalFeedbackPages = function(totalItems) {
        return Math.ceil(totalItems / $scope.feedbackDt.pageSize) || 1;
    };
    $scope.getFeedbackPageNumbers = function(totalItems) {
        var total = $scope.getTotalFeedbackPages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    // =========================================================
    // 3. INSPECT ADMIN USERS DATATABLE CONTROLS
    // =========================================================
    $scope.adminInspectDt = {
        sortField: 'usernameEn',
        sortReverse: false,
        currentPage: 1,
        pageSize: 8
    };
    $scope.sortAdminInspectBy = function(field) {
        if ($scope.adminInspectDt.sortField === field) {
            $scope.adminInspectDt.sortReverse = !$scope.adminInspectDt.sortReverse;
        } else {
            $scope.adminInspectDt.sortField = field;
            $scope.adminInspectDt.sortReverse = false;
        }
        $scope.adminInspectDt.currentPage = 1;
    };
    $scope.getAdminInspectSortIndicator = function(field) {
        if ($scope.adminInspectDt.sortField !== field) return '↕';
        return $scope.adminInspectDt.sortReverse ? '▼' : '▲';
    };
    $scope.getFilteredAdminInspectUsers = function() {
        if (!$scope.users) return [];
        var q = $scope.adminInspectUserSearch;
        return $scope.users.filter(function(u) {
            if (q && !searchMatch([u.usernameEn, u.usernameAr, u.usernameKu, u.phoneNumber, u.status, '#' + u.id].join(' '), q)) {
                return false;
            }
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.adminInspectDt.sortField];
            var valB = b[$scope.adminInspectDt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.adminInspectDt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.adminInspectDt.sortReverse ? -1 : 1;
            return 0;
        });
    };
    $scope.getPagedAdminInspectUsers = function() {
        var filtered = $scope.getFilteredAdminInspectUsers();
        var start = ($scope.adminInspectDt.currentPage - 1) * $scope.adminInspectDt.pageSize;
        return filtered.slice(start, start + $scope.adminInspectDt.pageSize);
    };
    $scope.setAdminInspectPage = function(page) {
        if (page < 1 || page > $scope.getTotalAdminInspectPages($scope.getFilteredAdminInspectUsers().length)) return;
        $scope.adminInspectDt.currentPage = page;
    };
    $scope.getTotalAdminInspectPages = function(totalItems) {
        return Math.ceil(totalItems / $scope.adminInspectDt.pageSize) || 1;
    };
    $scope.getAdminInspectPageNumbers = function(totalItems) {
        var total = $scope.getTotalAdminInspectPages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    // =========================================================
    // 4. DELETE ADMIN SELECTION DATATABLE CONTROLS
    // =========================================================
    $scope.deleteAdminDt = {
        sortField: 'username',
        sortReverse: false,
        currentPage: 1,
        pageSize: 8,
        search: ''
    };
    $scope.sortDeleteAdminsBy = function(field) {
        if ($scope.deleteAdminDt.sortField === field) {
            $scope.deleteAdminDt.sortReverse = !$scope.deleteAdminDt.sortReverse;
        } else {
            $scope.deleteAdminDt.sortField = field;
            $scope.deleteAdminDt.sortReverse = false;
        }
        $scope.deleteAdminDt.currentPage = 1;
    };
    $scope.getDeleteAdminSortIndicator = function(field) {
        if ($scope.deleteAdminDt.sortField !== field) return '↕';
        return $scope.deleteAdminDt.sortReverse ? '▼' : '▲';
    };
    $scope.getFilteredDeleteAdmins = function() {
        if (!$scope.admins) return [];
        var q = $scope.deleteAdminDt.search;
        return $scope.admins.filter(function(a) {
            if (q && !searchMatch([a.username, a.phoneNumber, a.status, a.superAdmin ? 'super admin' : 'administrator', '#' + a.id].join(' '), q)) {
                return false;
            }
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.deleteAdminDt.sortField];
            var valB = b[$scope.deleteAdminDt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.deleteAdminDt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.deleteAdminDt.sortReverse ? -1 : 1;
            return 0;
        });
    };
    $scope.getPagedDeleteAdmins = function() {
        var filtered = $scope.getFilteredDeleteAdmins();
        var start = ($scope.deleteAdminDt.currentPage - 1) * $scope.deleteAdminDt.pageSize;
        return filtered.slice(start, start + $scope.deleteAdminDt.pageSize);
    };
    $scope.setDeleteAdminPage = function(page) {
        if (page < 1 || page > $scope.getTotalDeleteAdminPages($scope.getFilteredDeleteAdmins().length)) return;
        $scope.deleteAdminDt.currentPage = page;
    };
    $scope.getTotalDeleteAdminPages = function(totalItems) {
        return Math.ceil(totalItems / $scope.deleteAdminDt.pageSize) || 1;
    };
    $scope.getDeleteAdminPageNumbers = function(totalItems) {
        var total = $scope.getTotalDeleteAdminPages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    // =========================================================
    // 5. DELETE USER SELECTION DATATABLE CONTROLS
    // =========================================================
    $scope.deleteUserDt = {
        sortField: 'usernameEn',
        sortReverse: false,
        currentPage: 1,
        pageSize: 8,
        search: ''
    };
    $scope.sortDeleteUsersBy = function(field) {
        if ($scope.deleteUserDt.sortField === field) {
            $scope.deleteUserDt.sortReverse = !$scope.deleteUserDt.sortReverse;
        } else {
            $scope.deleteUserDt.sortField = field;
            $scope.deleteUserDt.sortReverse = false;
        }
        $scope.deleteUserDt.currentPage = 1;
    };
    $scope.getDeleteUserSortIndicator = function(field) {
        if ($scope.deleteUserDt.sortField !== field) return '↕';
        return $scope.deleteUserDt.sortReverse ? '▼' : '▲';
    };
    $scope.getFilteredDeleteUsers = function() {
        if (!$scope.users) return [];
        var q = $scope.deleteUserDt.search;
        return $scope.users.filter(function(u) {
            if (q && !searchMatch([u.usernameEn, u.usernameAr, u.usernameKu, u.phoneNumber, u.status, '#' + u.id].join(' '), q)) {
                return false;
            }
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.deleteUserDt.sortField];
            var valB = b[$scope.deleteUserDt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.deleteUserDt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.deleteUserDt.sortReverse ? -1 : 1;
            return 0;
        });
    };
    $scope.getPagedDeleteUsers = function() {
        var filtered = $scope.getFilteredDeleteUsers();
        var start = ($scope.deleteUserDt.currentPage - 1) * $scope.deleteUserDt.pageSize;
        return filtered.slice(start, start + $scope.deleteUserDt.pageSize);
    };
    $scope.setDeleteUserPage = function(page) {
        if (page < 1 || page > $scope.getTotalDeleteUserPages($scope.getFilteredDeleteUsers().length)) return;
        $scope.deleteUserDt.currentPage = page;
    };
    $scope.getTotalDeleteUserPages = function(totalItems) {
        return Math.ceil(totalItems / $scope.deleteUserDt.pageSize) || 1;
    };
    $scope.getDeleteUserPageNumbers = function(totalItems) {
        var total = $scope.getTotalDeleteUserPages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    // =========================================================
    // 6. DYNAMIC FILTER RULES DATATABLE CONTROLS
    // =========================================================
    $scope.filterRulesDt = {
        sortField: 'id',
        sortReverse: false,
        currentPage: 1,
        pageSize: 8,
        search: ''
    };
    $scope.sortFilterRulesBy = function(field) {
        if ($scope.filterRulesDt.sortField === field) {
            $scope.filterRulesDt.sortReverse = !$scope.filterRulesDt.sortReverse;
        } else {
            $scope.filterRulesDt.sortField = field;
            $scope.filterRulesDt.sortReverse = false;
        }
        $scope.filterRulesDt.currentPage = 1;
    };
    $scope.getFilterRuleSortIndicator = function(field) {
        if ($scope.filterRulesDt.sortField !== field) return '↕';
        return $scope.filterRulesDt.sortReverse ? '▼' : '▲';
    };
    $scope.getFilteredFilterRules = function() {
        if (!$scope.timeFilterPresets) return [];
        var q = $scope.filterRulesDt.search;
        return $scope.timeFilterPresets.filter(function(r) {
            if (q && !searchMatch([r.name, r.durationValue, r.durationUnit, r.isActive ? 'active' : 'inactive', '#' + r.id].join(' '), q)) {
                return false;
            }
            return true;
        }).sort(function(a, b) {
            var valA = a[$scope.filterRulesDt.sortField];
            var valB = b[$scope.filterRulesDt.sortField];
            if (valA == null) valA = '';
            if (valB == null) valB = '';
            if (typeof valA === 'string') valA = valA.toLowerCase();
            if (typeof valB === 'string') valB = valB.toLowerCase();
            if (valA < valB) return $scope.filterRulesDt.sortReverse ? 1 : -1;
            if (valA > valB) return $scope.filterRulesDt.sortReverse ? -1 : 1;
            return 0;
        });
    };
    $scope.getPagedFilterRules = function() {
        var filtered = $scope.getFilteredFilterRules();
        var start = ($scope.filterRulesDt.currentPage - 1) * $scope.filterRulesDt.pageSize;
        return filtered.slice(start, start + $scope.filterRulesDt.pageSize);
    };
    $scope.setFilterRulePage = function(page) {
        if (page < 1 || page > $scope.getTotalFilterRulePages($scope.getFilteredFilterRules().length)) return;
        $scope.filterRulesDt.currentPage = page;
    };
    $scope.getTotalFilterRulePages = function(totalItems) {
        return Math.ceil(totalItems / $scope.filterRulesDt.pageSize) || 1;
    };
    $scope.getFilterRulePageNumbers = function(totalItems) {
        var total = $scope.getTotalFilterRulePages(totalItems);
        var pages = [];
        for (var i = 1; i <= total; i++) pages.push(i);
        return pages;
    };

    // Data Collections
    $scope.admins = [];
    $scope.users = [];
    $scope.roles = [];
    $scope.customers = [];
    $scope.customerOrders = [];
    $scope.customerFeedbacks = [];
    $scope.customerStats = {
        totalCustomers: 0,
        count24h: 0,
        count7d: 0,
        count30d: 0,
        totalFeedbacks: 0,
        complimentsCount: 0
    };

    $scope.selectedAdmin = null;
    $scope.selectedUser = null;
    $scope.editingUser = null;
    $scope.userModules = [];
    $scope.selectedCustomer = null;

    // New Follow-up Note / Compliment Model
    $scope.newCustomerFeedback = {
        feedbackType: 'COMPLIMENT',
        content: '',
        rating: 5,
        authorName: 'Agent Follow-up'
    };

    // New Order Model
    $scope.newCustomerOrder = {
        orderNumber: 'ORD-' + Math.floor(100 + Math.random() * 900),
        itemsSummary: '',
        totalAmount: 150.0,
        paymentMethod: 'Cash on Delivery',
        orderStatus: 'DELIVERED'
    };

    // Toasts
    $scope.toasts = [];
    $scope.showToast = function(msg, type) {
        var toast = { id: Date.now(), message: msg, type: type || 'success' };
        $scope.toasts.push(toast);
        $timeout(function() {
            var i = $scope.toasts.indexOf(toast);
            if (i !== -1) $scope.toasts.splice(i, 1);
        }, 3600);
    };

    // =========================================================
    // SPRING SECURITY AUTHENTICATION & SESSION MANAGEMENT
    // =========================================================
    $scope.checkAuth = function() {
        return $http.get('/api/auth/current').then(function(res) {
            if (res.data && res.data.authenticated) {
                $scope.auth = {
                    authenticated: true,
                    username: res.data.username,
                    role: res.data.role,
                    isSuperAdmin: res.data.isSuperAdmin,
                    canCreateRoles: res.data.canCreateRoles,
                    canManageUsers: res.data.canManageUsers
                };
            } else {
                $scope.auth = {
                    authenticated: false,
                    username: '',
                    role: '',
                    isSuperAdmin: false,
                    canCreateRoles: false,
                    canManageUsers: false
                };
            }
        });
    };

    $scope.openLoginModal = function() {
        $scope.showLoginModal = true;
        $scope.loginForm = { username: 'twekl_super_admin', password: '' };
    };

    $scope.closeLoginModal = function() {
        $scope.showLoginModal = false;
    };

    $scope.login = function() {
        if (!$scope.loginForm.username || !$scope.loginForm.password) {
            $scope.showToast('Please enter both username and password', 'error');
            return;
        }

        $http.post('/api/auth/login', $scope.loginForm).then(function(res) {
            $scope.auth = {
                authenticated: true,
                username: res.data.username,
                role: res.data.role,
                isSuperAdmin: res.data.superAdmin || res.data.isSuperAdmin,
                canCreateRoles: res.data.canCreateRoles,
                canManageUsers: res.data.canManageUsers
            };
            $scope.showLoginModal = false;
            $scope.showToast('Authenticated as ' + res.data.username + ' (' + res.data.role + ')');
            $scope.refreshAllData();
        }, function(err) {
            var msg = err.data && err.data.message ? err.data.message : 'Invalid credentials';
            $scope.showToast('Authentication Failed: ' + msg, 'error');
        });
    };

    $scope.logout = function() {
        $http.post('/api/auth/logout', {}).then(function() {
            $scope.auth = {
                authenticated: false,
                username: '',
                role: '',
                isSuperAdmin: false,
                canCreateRoles: false,
                canManageUsers: false
            };
            $scope.showToast('You have been logged out');
            $scope.openLoginModal();
        });
    };

    // Listen to Spring Security HTTP interceptor events
    $scope.$on('auth:unauthorized', function(event, data) {
        var msg = data && data.message ? data.message : 'Authentication required. Please log in.';
        $scope.showToast('Spring Security: ' + msg, 'error');
        $scope.openLoginModal();
    });

    $scope.$on('auth:forbidden', function(event, data) {
        var msg = data && data.message ? data.message : 'Access denied by Spring Security. Insufficient administrative privileges.';
        $scope.showToast('Spring Security 403: ' + msg, 'error');
    });

    // =========================================================
    // CLEAN URL ROUTING (HTML5 HISTORY API - NO HASH '#')
    // =========================================================
    $scope.updateUrl = function(path) {
        if (!path) path = '/admin/admins';
        if (!path.startsWith('/')) path = '/' + path;
        
        // Normalize standard path structures
        if (path !== '/login' && !path.startsWith('/admin')) {
            if (path.startsWith('/admins')) path = '/admin/admins' + path.substring(7);
            else if (path.startsWith('/users')) path = '/admin/users' + path.substring(6);
            else if (path.startsWith('/roles') || path.startsWith('/role-templates')) path = '/admin/role-templates';
            else if (path.startsWith('/customers') || path.startsWith('/followups') || path.startsWith('/followup')) {
                var suffix = path.replace(/^\/(customers|followups|followup)/, '');
                path = '/admin/followups' + suffix;
            } else {
                path = '/admin' + path;
            }
        }
        
        if ($window.location.pathname !== path) {
            if ($window.history && $window.history.pushState) {
                $window.history.pushState(null, '', path);
            }
        }
    };

    // Backwards compatibility alias
    $scope.updateHash = $scope.updateUrl;

    $scope.syncRouteFromPath = function() {
        var path = $window.location.pathname || '/admin/admins';
        
        // Convert any legacy hash URL to clean path seamlessly
        if ($window.location.hash && $window.location.hash.length > 1) {
            var legacy = $window.location.hash.replace(/^#\/?/, '');
            path = legacy.startsWith('admin/') ? '/' + legacy : '/admin/' + legacy;
            if ($window.history && $window.history.replaceState) {
                $window.history.replaceState(null, '', path);
            }
        }

        var clean = path.replace(/^\/admin\/?/, '').replace(/^\//, '');
        var parts = clean.split('/');
        var section = parts[0] || 'admins';
        var action = parts[1] || 'list';
        var id = parts[2] ? parseInt(parts[2], 10) : null;

        if (section === 'admins') {
            $scope.currentTab = 'admins';
            $scope.adminView = action;
            $scope.categories.admin = true;
            if (action === 'filters') {
                $scope.loadTimeFilterPresets();
            }
            if (id) {
                if ($scope.admins && $scope.admins.length > 0) {
                    var foundAdmin = $scope.admins.find(function(a) { return a.id === id; });
                    if (foundAdmin) $scope.selectedAdmin = foundAdmin;
                } else {
                    $http.get('/api/admins/' + id).then(function(res) {
                        $scope.selectedAdmin = res.data;
                    });
                }
            }
        } else if (section === 'users') {
            $scope.currentTab = 'users';
            $scope.userView = action;
            $scope.categories.admin = true;
            if (id) {
                if ($scope.users && $scope.users.length > 0) {
                    var foundUser = $scope.users.find(function(u) { return u.id === id; });
                    if (foundUser) {
                        $scope.selectedUser = foundUser;
                        $scope.editingUser = angular.copy(foundUser);
                        $scope.loadUserModules(foundUser.id);
                    }
                } else {
                    $http.get('/api/users/' + id).then(function(res) {
                        $scope.selectedUser = res.data;
                        $scope.editingUser = angular.copy(res.data);
                        $scope.loadUserModules(id);
                    });
                }
            }
        } else if (section === 'roles' || section === 'role-templates') {
            $scope.currentTab = 'roles';
            $scope.categories.admin = true;
            $scope.loadRoles();
        } else if (section === 'customers' || section === 'followups' || section === 'followup') {
            $scope.currentTab = 'customers';
            $scope.customerView = action;
            if (id) {
                if ($scope.customers && $scope.customers.length > 0) {
                    var foundCustomer = $scope.customers.find(function(c) { return c.id === id; });
                    if (foundCustomer) {
                        $scope.selectedCustomer = foundCustomer;
                        if (action === 'orders') {
                            $scope.loadCustomerOrders(foundCustomer.id);
                        } else if (action === 'feedback') {
                            $scope.loadCustomerOrders(foundCustomer.id);
                            $scope.loadCustomerFeedbacks(foundCustomer.id);
                        }
                    }
                } else {
                    $http.get('/api/customers/' + id).then(function(res) {
                        $scope.selectedCustomer = res.data;
                        if (action === 'orders') {
                            $scope.loadCustomerOrders(res.data.id);
                        } else if (action === 'feedback') {
                            $scope.loadCustomerOrders(res.data.id);
                            $scope.loadCustomerFeedbacks(res.data.id);
                        }
                    });
                }
            }
        }
    };

    $scope.syncRouteFromHash = $scope.syncRouteFromPath;

    // Listen to browser Back / Forward buttons (HTML5 popstate)
    $window.addEventListener('popstate', function() {
        $timeout(function() {
            $scope.syncRouteFromPath();
        });
    });

    // Top-Level Tab Navigation
    $scope.setTab = function(tabName) {
        $scope.currentTab = tabName;
        $scope.dt.currentPage = 1;
        $scope.filters.search = '';
        if (tabName === 'admins') {
            $scope.adminView = 'list';
            $scope.categories.admin = true;
            $scope.loadAdmins();
            $scope.updateUrl('/admin/admins');
        } else if (tabName === 'users') {
            $scope.userView = 'list';
            $scope.categories.admin = true;
            $scope.loadUsers();
            $scope.updateUrl('/admin/users');
        } else if (tabName === 'roles') {
            $scope.categories.admin = true;
            $scope.loadRoles();
            $scope.updateUrl('/admin/role-templates');
        } else if (tabName === 'customers') {
            $scope.customerView = 'list';
            $scope.loadCustomers($scope.customerTimeFilter);
            $scope.loadCustomerStats();
            $scope.updateUrl('/admin/followups');
        }
    };

    // =========================================================
    // ADMIN TAB - VIEW ROUTING & CRUD ACTIONS
    // =========================================================
    $scope.newAdmin = {
        username: '',
        password: '',
        phoneNumber: '',
        isSuperAdmin: false,
        canCreateRoles: true,
        canManageUsers: true,
        status: 'ACTIVE'
    };

    $scope.setAdminView = function(view, admin) {
        $scope.currentTab = 'admins';
        $scope.adminView = view || 'list';

        var targetAdmin = admin || $scope.selectedAdmin || ($scope.admins.length > 0 ? $scope.admins[0] : null);

        if (view === 'create') {
            $scope.newAdmin = {
                username: '',
                password: '',
                phoneNumber: '',
                isSuperAdmin: false,
                canCreateRoles: true,
                canManageUsers: true,
                status: 'ACTIVE'
            };
            $scope.updateUrl('/admin/admins/create');
        } else if (view === 'filters') {
            $scope.loadTimeFilterPresets();
            $scope.updateUrl('/admin/admins/filters');
        } else if (view === 'inspect' || view === 'delete') {
            if (targetAdmin) {
                $scope.selectedAdmin = targetAdmin;
                $scope.updateUrl('/admin/admins/' + view + '/' + targetAdmin.id);
            } else {
                $scope.updateUrl('/admin/admins/' + view);
            }
        } else {
            $scope.updateUrl('/admin/admins');
        }
    };

    $scope.loadAdmins = function() {
        return $http.get('/api/admins').then(function(res) {
            $scope.admins = res.data;
            if ($scope.selectedAdmin) {
                var found = $scope.admins.find(function(a) { return a.id === $scope.selectedAdmin.id; });
                if (found) {
                    $scope.selectedAdmin = found;
                } else if ($scope.admins.length > 0) {
                    $scope.selectedAdmin = $scope.admins[0];
                }
            } else if ($scope.admins.length > 0) {
                $scope.selectedAdmin = $scope.admins[0];
            }
        });
    };

    $scope.createAdmin = function() {
        if (!$scope.newAdmin.username || !$scope.newAdmin.password) {
            $scope.showToast('Username and password are required', 'error');
            return;
        }
        $http.post('/api/admins', $scope.newAdmin).then(function(res) {
            $scope.showToast('Administrator created successfully');
            $scope.loadAdmins().then(function() {
                $scope.setAdminView('inspect', res.data);
            });
        }, function(err) {
            var errorMsg = err.data && err.data.fieldErrors ? JSON.stringify(err.data.fieldErrors) : (err.data && err.data.message ? err.data.message : 'Failed to create admin');
            $scope.showToast(errorMsg, 'error');
        });
    };

    $scope.toggleAdminStatus = function(admin, event) {
        if (event) event.stopPropagation();
        $http.patch('/api/admins/' + admin.id + '/toggle-status').then(function(res) {
            admin.status = res.data.status;
            if ($scope.selectedAdmin && $scope.selectedAdmin.id === admin.id) {
                $scope.selectedAdmin.status = res.data.status;
            }
            $scope.showToast('Admin status: ' + admin.status);
        });
    };

    $scope.adminToDelete = null;
    $scope.showDeleteAdminModal = false;

    $scope.openDeleteAdminModal = function(admin, $event) {
        if ($event) $event.stopPropagation();
        $scope.adminToDelete = admin;
        $scope.showDeleteAdminModal = true;
    };

    $scope.closeDeleteAdminModal = function() {
        $scope.showDeleteAdminModal = false;
        $scope.adminToDelete = null;
    };

    $scope.proceedToDeleteAdminPage = function() {
        if (!$scope.adminToDelete) return;
        var targetAdmin = $scope.adminToDelete;
        $scope.closeDeleteAdminModal();
        $scope.setAdminView('delete', targetAdmin);
    };

    $scope.performDeleteAdmin = function(admin) {
        if (!admin) return;
        $http.delete('/api/admins/' + admin.id).then(function() {
            $scope.showToast('Administrator ' + admin.username + ' deleted successfully');
            $scope.selectedAdmin = null;
            $scope.loadAdmins().then(function() {
                $scope.setAdminView('list');
            });
        }, function(err) {
            var msg = err.data && err.data.message ? err.data.message : 'Failed to delete admin';
            $scope.showToast(msg, 'error');
        });
    };

    // =========================================================
    // USER TAB - VIEW ROUTING & CRUD ACTIONS
    // =========================================================
    $scope.newUser = {
        usernameEn: '',
        usernameAr: '',
        usernameKu: '',
        password: '',
        phoneNumber: '',
        status: 'ACTIVE'
    };

    $scope.setUserView = function(view, user) {
        $scope.currentTab = 'users';
        $scope.userView = view || 'list';

        var targetUser = user || $scope.selectedUser || ($scope.users.length > 0 ? $scope.users[0] : null);

        if (view === 'create') {
            $scope.newUser = {
                usernameEn: '',
                usernameAr: '',
                usernameKu: '',
                password: '',
                phoneNumber: '',
                status: 'ACTIVE'
            };
            $scope.updateUrl('/admin/users/create');
        } else if (view === 'inspect' || view === 'update' || view === 'delete') {
            if (targetUser) {
                $scope.selectedUser = targetUser;
                $scope.editingUser = angular.copy(targetUser);
                $scope.loadUserModules(targetUser.id);
                $scope.updateUrl('/admin/users/' + view + '/' + targetUser.id);
            } else {
                $scope.updateUrl('/admin/users/' + view);
            }
        } else {
            $scope.updateUrl('/admin/users');
        }
    };

    $scope.loadUsers = function() {
        return $http.get('/api/users').then(function(res) {
            $scope.users = res.data;
            if ($scope.selectedUser) {
                var found = $scope.users.find(function(u) { return u.id === $scope.selectedUser.id; });
                if (found) {
                    $scope.selectedUser = found;
                    $scope.editingUser = angular.copy(found);
                    $scope.loadUserModules(found.id);
                } else if ($scope.users.length > 0) {
                    $scope.selectedUser = $scope.users[0];
                    $scope.editingUser = angular.copy($scope.users[0]);
                    $scope.loadUserModules($scope.users[0].id);
                }
            } else if ($scope.users.length > 0) {
                $scope.selectedUser = $scope.users[0];
                $scope.editingUser = angular.copy($scope.users[0]);
                $scope.loadUserModules($scope.users[0].id);
            }
        });
    };

    $scope.loadUserModules = function(userId) {
        if (!userId) return;
        $http.get('/api/users/' + userId + '/modules').then(function(res) {
            $scope.userModules = res.data;
        });
    };

    $scope.createUser = function() {
        if (!$scope.newUser.usernameEn || !$scope.newUser.password) {
            $scope.showToast('Username and password are required', 'error');
            return;
        }
        $http.post('/api/users', $scope.newUser).then(function(res) {
            $scope.showToast('User created successfully');
            $scope.loadUsers().then(function() {
                $scope.setUserView('inspect', res.data);
            });
        }, function(err) {
            var errorMsg = err.data && err.data.fieldErrors ? JSON.stringify(err.data.fieldErrors) : (err.data && err.data.message ? err.data.message : 'Failed to create user');
            $scope.showToast(errorMsg, 'error');
        });
    };

    $scope.updateUser = function() {
        if (!$scope.editingUser || !$scope.editingUser.usernameEn) {
            $scope.showToast('Username is required', 'error');
            return;
        }
        $http.put('/api/users/' + $scope.editingUser.id, $scope.editingUser).then(function(res) {
            $scope.showToast('User details updated successfully');
            $scope.selectedUser = res.data;
            $scope.loadUsers().then(function() {
                $scope.setUserView('inspect', res.data);
            });
        }, function(err) {
            var msg = err.data && err.data.message ? err.data.message : 'Failed to update user';
            $scope.showToast(msg, 'error');
        });
    };

    $scope.toggleUserStatus = function(user, event) {
        if (event) event.stopPropagation();
        $http.patch('/api/users/' + user.id + '/toggle-status').then(function(res) {
            user.status = res.data.status;
            if ($scope.selectedUser && $scope.selectedUser.id === user.id) {
                $scope.selectedUser.status = res.data.status;
            }
            $scope.showToast('User status: ' + user.status);
        });
    };

    $scope.performDeleteUser = function(user) {
        if (!user) return;
        $http.delete('/api/users/' + user.id).then(function() {
            $scope.showToast('User ' + user.usernameEn + ' deleted successfully');
            $scope.selectedUser = null;
            $scope.loadUsers().then(function() {
                $scope.setUserView('list');
            });
        }, function(err) {
            var msg = err.data && err.data.message ? err.data.message : 'Failed to delete user';
            $scope.showToast(msg, 'error');
        });
    };

    // DIRECT INLINE CRUD TOGGLE ON ANY 3-COLUMN MODULE CARD
    $scope.toggleModuleField = function(mod, field) {
        var newVal;
        if (field === 'visible') {
            newVal = !mod.visible;
        } else if (field === 'canCreate') {
            newVal = !mod.canCreate;
        } else if (field === 'canRead') {
            newVal = !mod.canRead;
        } else if (field === 'canUpdate') {
            newVal = !mod.canUpdate;
        } else if (field === 'canDelete') {
            newVal = !mod.canDelete;
        }

        var paramField = field.replace('can', '').toLowerCase();

        $http.patch('/api/users/' + $scope.selectedUser.id + '/modules/' + mod.moduleKey + '/toggle?field=' + paramField + '&value=' + newVal)
            .then(function() {
                if (field === 'visible') mod.visible = newVal;
                if (field === 'canCreate') mod.canCreate = newVal;
                if (field === 'canRead') mod.canRead = newVal;
                if (field === 'canUpdate') mod.canUpdate = newVal;
                if (field === 'canDelete') mod.canDelete = newVal;
                $scope.showToast(mod.moduleNameEn + ': ' + paramField.toUpperCase() + ' ' + (newVal ? 'ON' : 'OFF'));
            }, function() {
                $scope.showToast('Failed to update permission', 'error');
            });
    };

    // Module Quick Preset Actions
    $scope.setFullCrud = function(mod) {
        $http.patch('/api/users/' + $scope.selectedUser.id + '/modules/' + mod.moduleKey + '/toggle?field=full_crud&value=true')
            .then(function() {
                mod.canCreate = true;
                mod.canRead = true;
                mod.canUpdate = true;
                mod.canDelete = true;
                mod.visible = true;
                $scope.showToast(mod.moduleNameEn + ': Full CRUD granted');
            });
    };

    $scope.setReadOnly = function(mod) {
        $http.patch('/api/users/' + $scope.selectedUser.id + '/modules/' + mod.moduleKey + '/toggle?field=create&value=false');
        $http.patch('/api/users/' + $scope.selectedUser.id + '/modules/' + mod.moduleKey + '/toggle?field=read&value=true');
        $http.patch('/api/users/' + $scope.selectedUser.id + '/modules/' + mod.moduleKey + '/toggle?field=update&value=false');
        $http.patch('/api/users/' + $scope.selectedUser.id + '/modules/' + mod.moduleKey + '/toggle?field=delete&value=false');
        mod.canCreate = false;
        mod.canRead = true;
        mod.canUpdate = false;
        mod.canDelete = false;
        mod.visible = true;
        $scope.showToast(mod.moduleNameEn + ': Set to Read Only');
    };

    $scope.revokeModuleAccess = function(mod) {
        $http.patch('/api/users/' + $scope.selectedUser.id + '/modules/' + mod.moduleKey + '/toggle?field=full_crud&value=false');
        $http.patch('/api/users/' + $scope.selectedUser.id + '/modules/' + mod.moduleKey + '/toggle?field=visible&value=false');
        mod.canCreate = false;
        mod.canRead = false;
        mod.canUpdate = false;
        mod.canDelete = false;
        mod.visible = false;
        $scope.showToast(mod.moduleNameEn + ': Access Revoked / Hidden');
    };

    // =========================================================
    // ROLE TEMPLATES MANAGEMENT (Dedicated Page)
    // =========================================================
    $scope.newRole = {
        name: '',
        canCreate: false,
        canRead: true,
        canUpdate: false,
        canDelete: false
    };

    $scope.showAddRoleForm = false;
    $scope.toggleAddRoleForm = function() {
        $scope.showAddRoleForm = !$scope.showAddRoleForm;
    };

    $scope.loadRoles = function() {
        return $http.get('/api/roles').then(function(res) {
            $scope.roles = res.data;
        });
    };

    $scope.createRole = function() {
        if (!$scope.newRole.name || $scope.newRole.name.trim() === '') {
            $scope.showToast('Role name is required', 'error');
            return;
        }
        $http.post('/api/roles', $scope.newRole).then(function() {
            $scope.showToast('Role template created successfully');
            $scope.newRole = { name: '', canCreate: false, canRead: true, canUpdate: false, canDelete: false };
            $scope.showAddRoleForm = false;
            $scope.loadRoles();
        }, function(err) {
            $scope.showToast(err.data && err.data.error ? err.data.error : 'Failed to create role', 'error');
        });
    };

    $scope.toggleRoleField = function(role, field) {
        var newVal;
        if (field === 'canCreate') newVal = !role.canCreate;
        if (field === 'canRead') newVal = !role.canRead;
        if (field === 'canUpdate') newVal = !role.canUpdate;
        if (field === 'canDelete') newVal = !role.canDelete;

        var paramField = field.replace('can', '').toLowerCase();

        $http.patch('/api/roles/' + role.id + '/toggle?field=' + paramField + '&value=' + newVal)
            .then(function() {
                if (field === 'canCreate') role.canCreate = newVal;
                if (field === 'canRead') role.canRead = newVal;
                if (field === 'canUpdate') role.canUpdate = newVal;
                if (field === 'canDelete') role.canDelete = newVal;
                $scope.showToast(role.name + ': ' + paramField.toUpperCase() + ' ' + (newVal ? 'ON' : 'OFF'));
            }, function() {
                $scope.showToast('Failed to update role permission', 'error');
            });
    };

    $scope.deleteRole = function(role) {
        $http.delete('/api/roles/' + role.id).then(function() {
            $scope.showToast('Role deleted');
            $scope.loadRoles();
        });
    };

    // =========================================================
    // CUSTOMER FOLLOW-UP MANAGEMENT (PAGE-PER-ACTION WORKFLOW)
    // =========================================================
    $scope.loadCustomers = function(filter) {
        var f = filter || $scope.customerTimeFilter || 'all';
        return $http.get('/api/customers?filter=' + f).then(function(res) {
            $scope.customers = res.data;
            if ($scope.selectedCustomer) {
                var found = $scope.customers.find(function(c) { return c.id === $scope.selectedCustomer.id; });
                if (found) $scope.selectedCustomer = found;
            } else if ($scope.customers.length > 0) {
                $scope.selectedCustomer = $scope.customers[0];
            }
        });
    };

    $scope.loadCustomerStats = function() {
        return $http.get('/api/customers/stats').then(function(res) {
            $scope.customerStats = res.data;
        });
    };

    $scope.setCustomerTimeFilter = function(filter) {
        $scope.customerTimeFilter = filter;
        $scope.loadCustomers(filter);
    };

    $scope.setCustomerView = function(view, customer) {
        $scope.currentTab = 'customers';
        $scope.customerView = view || 'list';

        var targetCustomer = customer || $scope.selectedCustomer || ($scope.customers.length > 0 ? $scope.customers[0] : null);

        if (view === 'orders') {
            if (targetCustomer) {
                $scope.selectedCustomer = targetCustomer;
                if ($scope.selectedTimePreset) {
                    $scope.selectedOrderTimePreset = $scope.selectedTimePreset;
                }
                $scope.orderDt.currentPage = 1;
                $scope.loadCustomerOrders(targetCustomer.id);
                $scope.updateUrl('/admin/followups/orders/' + targetCustomer.id);
            } else {
                $scope.updateUrl('/admin/followups/orders');
            }
        } else if (view === 'feedback') {
            if (targetCustomer) {
                $scope.selectedCustomer = targetCustomer;
                $scope.feedbackDt.currentPage = 1;
                $scope.loadCustomerOrders(targetCustomer.id);
                $scope.loadCustomerFeedbacks(targetCustomer.id);
                $scope.updateUrl('/admin/followups/feedback/' + targetCustomer.id);
            } else {
                $scope.updateUrl('/admin/followups/feedback');
            }
        } else {
            $scope.loadCustomers($scope.customerTimeFilter);
            $scope.loadCustomerStats();
            $scope.updateUrl('/admin/followups');
        }
    };

    $scope.loadCustomerOrders = function(customerId) {
        if (!customerId) return;
        return $http.get('/api/customers/' + customerId + '/orders').then(function(res) {
            $scope.customerOrders = res.data || [];
            // Preload followups for each order so completion status is immediately calculated
            $scope.customerOrders.forEach(function(order) {
                $scope.loadOrderFollowups(order.id);
            });
        });
    };

    $scope.loadCustomerFeedbacks = function(customerId) {
        if (!customerId) return;
        return $http.get('/api/customers/' + customerId + '/feedbacks').then(function(res) {
            $scope.customerFeedbacks = res.data || [];
        });
    };

    $scope.showAddFeedbackModal = false;
    $scope.openAddFeedbackModal = function() {
        var defaultOrderId = ($scope.customerOrders && $scope.customerOrders.length > 0) ? $scope.customerOrders[0].id : null;
        $scope.newCustomerFeedback = {
            orderId: defaultOrderId,
            feedbackType: 'NOTE',
            content: '',
            imageUrl: '',
            authorName: 'Agent Follow-up'
        };
        $scope.showAddFeedbackModal = true;
    };
    $scope.closeAddFeedbackModal = function() {
        $scope.showAddFeedbackModal = false;
    };

    $scope.handleFeedbackImageUpload = function(element) {
        if (element.files && element.files[0]) {
            var file = element.files[0];
            var reader = new FileReader();
            reader.onload = function(e) {
                var img = new Image();
                img.onload = function() {
                    var canvas = document.createElement('canvas');
                    var maxDim = 800;
                    var width = img.width;
                    var height = img.height;
                    if (width > maxDim || height > maxDim) {
                        if (width > height) {
                            height = Math.round((height * maxDim) / width);
                            width = maxDim;
                        } else {
                            width = Math.round((width * maxDim) / height);
                            height = maxDim;
                        }
                    }
                    canvas.width = width;
                    canvas.height = height;
                    var ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    var compressedDataUrl = canvas.toDataURL('image/jpeg', 0.85);

                    $scope.$apply(function() {
                        $scope.newCustomerFeedback.imageUrl = compressedDataUrl;
                    });
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    };

    $scope.clearFeedbackImage = function() {
        $scope.newCustomerFeedback.imageUrl = '';
    };

    $scope.submitCustomerFeedback = function() {
        if (!$scope.selectedCustomer || !$scope.newCustomerFeedback.content) {
            $scope.showToast('Please enter note / compliment details', 'error');
            return;
        }
        $http.post('/api/customers/' + $scope.selectedCustomer.id + '/feedbacks', $scope.newCustomerFeedback).then(function() {
            $scope.showToast('Customer order note & image saved successfully');
            $scope.closeAddFeedbackModal();
            $scope.loadCustomerFeedbacks($scope.selectedCustomer.id);
            $scope.loadCustomerStats();
        }, function(err) {
            $scope.showToast(err.data && err.data.error ? err.data.error : 'Failed to save note', 'error');
        });
    };

    // Edit Existing Feedback / Compliment Modal
    $scope.editingCustomerFeedback = null;
    $scope.showEditFeedbackModal = false;

    $scope.openEditFeedbackModal = function(fb) {
        $scope.editingCustomerFeedback = angular.copy(fb);
        $scope.showEditFeedbackModal = true;
    };

    $scope.closeEditFeedbackModal = function() {
        $scope.showEditFeedbackModal = false;
        $scope.editingCustomerFeedback = null;
    };

    $scope.handleEditFeedbackImageUpload = function(element) {
        if (element.files && element.files[0]) {
            var file = element.files[0];
            var reader = new FileReader();
            reader.onload = function(e) {
                var img = new Image();
                img.onload = function() {
                    var canvas = document.createElement('canvas');
                    var maxDim = 800;
                    var width = img.width;
                    var height = img.height;
                    if (width > maxDim || height > maxDim) {
                        if (width > height) {
                            height = Math.round((height * maxDim) / width);
                            width = maxDim;
                        } else {
                            width = Math.round((width * maxDim) / height);
                            height = maxDim;
                        }
                    }
                    canvas.width = width;
                    canvas.height = height;
                    var ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    var compressedDataUrl = canvas.toDataURL('image/jpeg', 0.85);

                    $scope.$apply(function() {
                        if ($scope.editingCustomerFeedback) {
                            $scope.editingCustomerFeedback.imageUrl = compressedDataUrl;
                        }
                    });
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    };

    $scope.clearEditFeedbackImage = function() {
        if ($scope.editingCustomerFeedback) {
            $scope.editingCustomerFeedback.imageUrl = '';
        }
    };

    $scope.saveEditFeedback = function() {
        if (!$scope.editingCustomerFeedback || !$scope.selectedCustomer) return;
        var fb = $scope.editingCustomerFeedback;
        $http.put('/api/customers/' + $scope.selectedCustomer.id + '/feedbacks/' + fb.id, fb).then(function() {
            $scope.showToast('Note and image updated successfully');
            $scope.closeEditFeedbackModal();
            $scope.loadCustomerFeedbacks($scope.selectedCustomer.id);
        }, function(err) {
            $scope.showToast('Failed to update note details', 'error');
        });
    };

    // =========================================================
    // ORDER / PRODUCT LOG FOLLOW-UP CHECKLIST TREE
    // =========================================================
    $scope.expandedOrders = {};
    $scope.orderFollowups = {};

    $scope.toggleOrderExpand = function(order, $event) {
        if ($event) $event.stopPropagation();
        var id = order.id;
        $scope.expandedOrders[id] = !$scope.expandedOrders[id];
        if ($scope.expandedOrders[id]) {
            $scope.loadOrderFollowups(id);
        }
    };

    $scope.isOrderExpanded = function(orderId) {
        return !!$scope.expandedOrders[orderId];
    };

    $scope.loadOrderFollowups = function(orderId) {
        return $http.get('/api/orders/' + orderId + '/followups').then(function(res) {
            $scope.orderFollowups[orderId] = res.data || [];
        });
    };

    $scope.isOrderFullyFollowedUp = function(order) {
        if (!order) return false;
        var checks = $scope.orderFollowups[order.id];
        if (!checks || checks.length === 0) return false;
        return checks.every(function(c) { return c.isCompleted === true; });
    };

    $scope.getOrderFollowupStatusClass = function(order) {
        if (!order) return '';
        var checks = $scope.orderFollowups[order.id];
        if (!checks) return 'order-row-pending';
        if (checks.length > 0 && checks.every(function(c) { return c.isCompleted === true; })) {
            return 'order-row-completed';
        }
        return 'order-row-pending';
    };

    $scope.toggleFollowupCheck = function(order, check) {
        $http.patch('/api/orders/' + order.id + '/followups/' + check.id + '/toggle', {}).then(function(res) {
            check.isCompleted = res.data.isCompleted;
            check.checkedAt = res.data.checkedAt;
            check.checkedBy = res.data.checkedBy;
            $scope.showToast('Milestone "' + check.presetName + '" marked ' + (check.isCompleted ? 'Completed' : 'Pending'));
            $scope.loadCustomers($scope.customerTimeFilter);
            $scope.loadCustomerStats();
        }, function(err) {
            $scope.showToast('Failed to toggle milestone status', 'error');
        });
    };

    // Edit Follow-up Modal State
    $scope.editingFollowup = null;
    $scope.editingFollowupOrder = null;
    $scope.showEditFollowupModal = false;

    $scope.openEditFollowupModal = function(order, check) {
        $scope.editingFollowupOrder = order;
        $scope.editingFollowup = angular.copy(check);
        $scope.showEditFollowupModal = true;
    };

    $scope.closeEditFollowupModal = function() {
        $scope.showEditFollowupModal = false;
        $scope.editingFollowup = null;
        $scope.editingFollowupOrder = null;
    };

    $scope.handleFollowupImageUpload = function(element) {
        if (element.files && element.files[0]) {
            var file = element.files[0];
            var reader = new FileReader();
            reader.onload = function(e) {
                var img = new Image();
                img.onload = function() {
                    var canvas = document.createElement('canvas');
                    var maxDim = 1200;
                    var width = img.width;
                    var height = img.height;
                    if (width > maxDim || height > maxDim) {
                        if (width > height) {
                            height = Math.round((height * maxDim) / width);
                            width = maxDim;
                        } else {
                            width = Math.round((width * maxDim) / height);
                            height = maxDim;
                        }
                    }
                    canvas.width = width;
                    canvas.height = height;
                    var ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    var compressedDataUrl = canvas.toDataURL('image/jpeg', 0.85);
                    $scope.$apply(function() {
                        if ($scope.editingFollowup) {
                            $scope.editingFollowup.imageUrl = compressedDataUrl;
                        }
                    });
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    };

    $scope.saveFollowupCheck = function() {
        if (!$scope.editingFollowup || !$scope.editingFollowupOrder) return;
        var order = $scope.editingFollowupOrder;
        var check = $scope.editingFollowup;
        $http.put('/api/orders/' + order.id + '/followups/' + check.id, check).then(function(res) {
            $scope.showToast('Follow-up note & details saved successfully');
            $scope.closeEditFollowupModal();
            $scope.loadOrderFollowups(order.id);
        }, function(err) {
            $scope.showToast('Failed to save follow-up details', 'error');
        });
    };

    // Image Preview Lightbox Modal
    $scope.previewImageUrl = null;
    $scope.showImageModal = false;

    $scope.previewImage = function(url) {
        if (!url) return;
        $scope.previewImageUrl = url;
        $scope.showImageModal = true;
    };

    $scope.closeImageModal = function() {
        $scope.showImageModal = false;
        $scope.previewImageUrl = null;
    };

    // Helper functions
    $scope.getUserDisplayName = function(user) {
        if (!user) return '';
        return user.usernameEn || '';
    };

    $scope.formatDaysText = function(days) {
        if (days === 0 || days === 1) return '24 Hours Ago';
        if (days <= 7) return days + ' Days Ago (1 Week)';
        return days + ' Days Ago';
    };

    $scope.refreshAllData = function() {
        $scope.loadAdmins();
        $scope.loadUsers();
        $scope.loadRoles();
        $scope.loadCustomers();
        $scope.loadCustomerStats();
        $scope.loadTimeFilterPresets();
    };

    // Initialize application
    $scope.init = function() {
        $scope.checkAuth().then(function() {
            if ($scope.auth.authenticated) {
                $scope.refreshAllData();
            } else {
                // Auto-login default super admin or open login modal
                $scope.loginForm = { username: 'twekl_super_admin', password: 'Super@2026' };
                $scope.login();
            }
            $scope.syncRouteFromHash();
        });
    };

    $scope.init();
}]);
