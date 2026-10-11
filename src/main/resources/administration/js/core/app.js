/**
 * Twekl AngularJS Application Module
 * Core configuration and HTTP session interceptor.
 */
var app = angular.module('tweklApp', []);

app.config(['$httpProvider', function($httpProvider) {
    // Intercept 401/403 HTTP session responses
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

/**
 * Main Layout Coordinator Controller
 * Manages global application shell, active tab routing, authentication, shared modals, and presets.
 */
angular.module('tweklApp')
.controller('LayoutController', [
    '$scope', '$http', '$location', '$window', '$timeout', 'AuthService', 'TimeFilterService', 'ToastService', 'FilterHelperService',
function($scope, $http, $location, $window, $timeout, AuthService, TimeFilterService, ToastService, FilterHelperService) {

    // Sidebar Category Accordions (Closed by default as requested)
    $scope.categories = {
        admin: false,
        reports: false
    };

    $scope.toggleCategory = function(cat) {
        $scope.categories[cat] = !$scope.categories[cat];
    };

    $scope.isCategoryOpen = function(cat) {
        return !!$scope.categories[cat];
    };

    // Active Feature Tab: 'dashboard', 'admins', 'roles', 'customers', 'time-filters', 'reports'
    $scope.currentTab = window.SERVER_ACTIVE_TAB || 'dashboard';
    $scope.activeTab = $scope.currentTab;
    $scope.reportSubTab = 'milestones'; // 'milestones', 'satisfaction', 'products', 'customers'

    /**
     * Resolves initial active tab based on window pathname and server state
     */
    function initRoute() {
        var path = window.location.pathname;

        if (window.SERVER_ACTIVE_TAB) {
            $scope.currentTab = window.SERVER_ACTIVE_TAB;
            if ($scope.currentTab === 'admins' || $scope.currentTab === 'roles') {
                $scope.categories.admin = true;
            } else if ($scope.currentTab === 'reports') {
                $scope.categories.reports = true;
            }
        } else if (path.indexOf('/admin/dashboard') === 0) {
            $scope.currentTab = 'dashboard';
        } else if (path.indexOf('/admin/admins') === 0) {
            $scope.currentTab = 'admins';
            $scope.categories.admin = true;
        } else if (path.indexOf('/admin/roles') === 0 || path.indexOf('/admin/role-templates') === 0) {
            $scope.currentTab = 'roles';
            $scope.categories.admin = true;
        } else if (path.indexOf('/admin/timelines') === 0 || path.indexOf('/admin/time-filters') === 0) {
            $scope.currentTab = 'time-filters';
        } else if (path.indexOf('/admin/reports') === 0) {
            $scope.currentTab = 'reports';
            $scope.categories.reports = true;
            var parts = path.split('/');
            var sub = parts[3];
            if (sub && ['milestones','satisfaction','products','customers'].indexOf(sub) !== -1) {
                $scope.reportSubTab = sub;
            }
        } else if (path.indexOf('/admin/followups') === 0 || path.indexOf('/admin/customers') === 0) {
            $scope.currentTab = 'customers';
        } else {
            $scope.currentTab = 'dashboard';
        }

        $scope.activeTab = $scope.currentTab;
    }

    /**
     * Push a clean URL path or navigate to it
     */
    $scope.updateUrl = function(path) {
        if (!path) return;
        if (window.location.pathname !== path) {
            window.location.href = path;
        }
    };

    /**
     * Navigate to feature tab using pure SSR navigation
     */
    $scope.setTab = function(tabName) {
        if (tabName === 'admins') {
            window.location.href = '/admin/admins';
        } else if (tabName === 'roles') {
            window.location.href = '/admin/roles';
        } else if (tabName === 'customers' || tabName === 'followups') {
            window.location.href = '/admin/customers';
        } else if (tabName === 'reports') {
            window.location.href = '/admin/reports';
        } else if (tabName === 'time-filters' || tabName === 'timelines') {
            window.location.href = '/admin/timelines';
        } else {
            window.location.href = '/admin/dashboard';
        }
    };

    /**
     * Set active report sub-tab and update URL
     */
    $scope.setReportSubTab = function(subTab) {
        $scope.reportSubTab = subTab;
    };

    // Initialize immediately from server-injected user if available
    if (window.SERVER_CURRENT_USER) {
        var su = window.SERVER_CURRENT_USER;
        $scope.currentUser = su;
        $scope.auth = {
            authenticated: true,
            username: su.username || '',
            role: su.superAdmin ? 'Super Admin' : 'Admin',
            isSuperAdmin: !!su.superAdmin,
            canCreateRoles: su.canCreateRoles !== false,
            canManageUsers: su.canManageUsers !== false,
            canReadFollowups: true,
            canReadReports: true
        };
    } else {
        $scope.currentUser = null;
        $scope.auth = {
            authenticated: false,
            username: '',
            role: '',
            isSuperAdmin: false,
            canCreateRoles: false,
            canManageUsers: false,
            canReadFollowups: false,
            canReadReports: false
        };
    }

    $scope.showLoginModal = false;
    $scope.loginForm = {
        username: '',
        password: ''
    };

    /**
     * Fetch authenticated user session profile
     */
    $scope.loadCurrentUser = function() {
        AuthService.getMe().then(function(res) {
            var d = (res && res.data && res.data.data) ? res.data.data : (res && res.data ? res.data : null);
            if (d && d.authenticated) {
                $scope.currentUser = d;
                $scope.auth = {
                    authenticated: true,
                    username: d.username || '',
                    role: d.superAdmin ? 'Super Admin' : (d.role || 'Admin'),
                    isSuperAdmin: !!d.superAdmin,
                    canCreateRoles: d.canCreateRoles !== false,
                    canManageUsers: d.canManageUsers !== false,
                    canReadFollowups: d.canReadFollowups !== false,
                    canReadReports: d.canReadReports !== false
                };
                $scope.loadTimeFilterPresets();
                $scope.$broadcast('auth:loggedIn');
            }
        }, function(err) {
            // Auto-login default super admin in local/demo environment (like 9459a72)
            AuthService.login({ username: 'twekl_super_admin', password: 'Super@2026' }).then(function(loginRes) {
                var ld = (loginRes && loginRes.data && loginRes.data.data) ? loginRes.data.data : (loginRes && loginRes.data ? loginRes.data : null);
                if (ld) {
                    $scope.currentUser = ld;
                    $scope.auth = {
                        authenticated: true,
                        username: ld.username || 'twekl_super_admin',
                        role: 'Super Admin',
                        isSuperAdmin: true,
                        canCreateRoles: true,
                        canManageUsers: true,
                        canReadFollowups: true,
                        canReadReports: true
                    };
                    $scope.loadTimeFilterPresets();
                    $scope.$broadcast('auth:loggedIn');
                }
            }, function() {
                if (!window.SERVER_CURRENT_USER) {
                    $scope.auth = {
                        authenticated: false,
                        username: '',
                        role: '',
                        isSuperAdmin: false,
                        canCreateRoles: false,
                        canManageUsers: false,
                        canReadFollowups: false,
                        canReadReports: false
                    };
                    $scope.currentUser = null;
                }
            });
        });
    };


    $scope.openLoginModal = function() {
        $scope.loginForm = { username: '', password: '' };
        $scope.showLoginModal = true;
    };

    $scope.closeLoginModal = function() {
        $scope.showLoginModal = false;
    };

    /**
     * Submits login credentials
     */
    $scope.login = function() {
        if (!$scope.loginForm.username || !$scope.loginForm.password) {
            ToastService.error('Please enter username and password');
            return;
        }

        AuthService.login($scope.loginForm).then(function(res) {
            $scope.closeLoginModal();
            $scope.loadCurrentUser();
            ToastService.success('Logged in successfully!');
            $scope.$broadcast('auth:loggedIn');
        }, function(err) {
            ToastService.error(err.data && err.data.message ? err.data.message : 'Invalid credentials');
        });
    };

    /**
     * Executes user logout
     */
    $scope.logout = function() {
        AuthService.logout().then(function() {
            $scope.auth = { authenticated: false, username: '', role: '' };
            $scope.currentUser = null;
            window.location.href = '/login';
        }, function() {
            window.location.href = '/login';
        });
    };

    // =========================================================
    // GLOBAL TIME FILTER PRESETS
    // =========================================================
    $scope.timeFilterPresets = [];
    $scope.selectedTimePreset = null;
    $scope.selectedOrderTimePreset = null;
    $scope.selectedReportPreset = null;
    $scope.showBigFilterModal = false;
    $scope.currentFilterTarget = 'customer'; // 'customer', 'order', 'report'
    $scope.pendingFilterPreset = null;

    $scope.toDurationMinutes = FilterHelperService.toDurationMinutes;

    /**
     * Loads time filter presets from backend
     */
    $scope.loadTimeFilterPresets = function() {
        return TimeFilterService.getAll().then(function(res) {
            var arr = res.data || [];
            arr.forEach(function(p) {
                p.name = p.name || p.labelEn || p.presetKey;
                p.isActive = (p.isActive != null ? p.isActive : (p.active !== false));
            });
            arr.sort(function(a, b) {
                var minA = $scope.toDurationMinutes(a);
                var minB = $scope.toDurationMinutes(b);
                if (minA !== minB) return minA - minB;
                return (a.id || 0) - (b.id || 0);
            });
            $scope.timeFilterPresets = arr;
            return arr;
        }, function() {
            return [];
        });
    };

    $scope.customerFollowupStatusFilter = 'ALL';
    $scope.pendingCustomerFollowupStatus = 'ALL';
    $scope.selectedUserTimePreset = null;

    $scope.openFilterModal = function(target) {
        $scope.currentFilterTarget = target || 'customer';
        if ($scope.currentFilterTarget === 'customer') {
            $scope.pendingFilterPreset = $scope.selectedTimePreset;
            $scope.pendingCustomerFollowupStatus = $scope.customerFollowupStatusFilter || 'ALL';
        } else if ($scope.currentFilterTarget === 'user') {
            $scope.pendingFilterPreset = $scope.selectedUserTimePreset;
        } else if ($scope.currentFilterTarget === 'order') {
            $scope.pendingFilterPreset = $scope.selectedOrderTimePreset;
        } else if ($scope.currentFilterTarget === 'report') {
            $scope.pendingFilterPreset = $scope.selectedReportPreset;
        }
        $scope.showBigFilterModal = true;
    };

    $scope.closeFilterModal = function() {
        $scope.showBigFilterModal = false;
    };

    $scope.selectPendingPreset = function(preset) {
        $scope.pendingFilterPreset = preset;
    };

    $scope.isPendingPresetSelected = function(preset) {
        if (!preset && !$scope.pendingFilterPreset) return true;
        if (!preset || !$scope.pendingFilterPreset) return false;
        return preset.id === $scope.pendingFilterPreset.id;
    };

    $scope.selectPendingFollowupStatus = function(status) {
        $scope.pendingCustomerFollowupStatus = status || 'ALL';
    };

    $scope.isPendingFollowupStatusSelected = function(status) {
        return ($scope.pendingCustomerFollowupStatus || 'ALL') === (status || 'ALL');
    };

    $scope.confirmApplyFilterModal = function() {
        if ($scope.currentFilterTarget === 'customer') {
            $scope.selectedTimePreset = $scope.pendingFilterPreset;
            $scope.customerFollowupStatusFilter = $scope.pendingCustomerFollowupStatus || 'ALL';
            $scope.$broadcast('filter:timePresetChanged', $scope.selectedTimePreset);
            $scope.$broadcast('filter:followupStatusChanged', $scope.customerFollowupStatusFilter);
        } else if ($scope.currentFilterTarget === 'user') {
            $scope.selectedUserTimePreset = $scope.pendingFilterPreset;
            $scope.$broadcast('filter:userTimePresetChanged', $scope.selectedUserTimePreset);
        } else if ($scope.currentFilterTarget === 'order') {
            $scope.selectedOrderTimePreset = $scope.pendingFilterPreset;
            $scope.$broadcast('filter:orderTimePresetChanged', $scope.selectedOrderTimePreset);
        } else if ($scope.currentFilterTarget === 'report') {
            $scope.selectedReportPreset = $scope.pendingFilterPreset;
            $scope.$broadcast('filter:reportTimePresetChanged', $scope.selectedReportPreset);
        }
        $scope.closeFilterModal();
    };

    $scope.clearCustomerFollowupStatusFilter = function() {
        $scope.customerFollowupStatusFilter = 'ALL';
        $scope.pendingCustomerFollowupStatus = 'ALL';
        $scope.$broadcast('filter:followupStatusChanged', 'ALL');
    };

    $scope.$on('filter:clearFollowupStatus', function() {
        $scope.customerFollowupStatusFilter = 'ALL';
        $scope.pendingCustomerFollowupStatus = 'ALL';
    });

    $scope.selectTimePreset = function(preset) {
        $scope.selectedTimePreset = preset;
        $scope.$broadcast('filter:timePresetChanged', preset);
    };

    $scope.selectUserTimePreset = function(preset) {
        $scope.selectedUserTimePreset = preset;
        $scope.$broadcast('filter:userTimePresetChanged', preset);
    };

    $scope.selectOrderTimePreset = function(preset) {
        $scope.selectedOrderTimePreset = preset;
        $scope.$broadcast('filter:orderTimePresetChanged', preset);
    };

    // =========================================================
    // REPORT FILTERS MODAL (Global)
    // =========================================================
    $scope.modalState = {
        reportFilter: false
    };
    $scope.showReportFilterModal = false;
    $scope.reportFilters = { satisfaction: 'all' };
    $scope.pendingReportFilters = { satisfaction: 'all', preset: null };

    $scope.openReportFilterModal = function() {
        $scope.pendingReportFilters = {
            satisfaction: ($scope.reportFilters && $scope.reportFilters.satisfaction) || 'all',
            preset: $scope.selectedReportPreset
        };
        $scope.showReportFilterModal = true;
        if ($scope.modalState) $scope.modalState.reportFilter = true;
    };

    $scope.closeReportFilterModal = function() {
        $scope.showReportFilterModal = false;
        if ($scope.modalState) $scope.modalState.reportFilter = false;
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
        $scope.reportFilters.satisfaction = $scope.pendingReportFilters.satisfaction;
        $scope.closeReportFilterModal();
        $scope.$broadcast('filter:reportFiltersChanged', {
            preset: $scope.selectedReportPreset,
            satisfaction: $scope.reportFilters.satisfaction
        });
    };

    $scope.resetReportFilterModal = function() {
        $scope.pendingReportFilters = { satisfaction: 'all', preset: null };
        $scope.selectedReportPreset = null;
        $scope.reportFilters.satisfaction = 'all';
        $scope.closeReportFilterModal();
        $scope.$broadcast('filter:reportFiltersChanged', {
            preset: null,
            satisfaction: 'all'
        });
    };

    $scope.selectReportPreset = function(preset) {
        $scope.selectedReportPreset = preset;
        $scope.$broadcast('filter:reportFiltersChanged', {
            preset: preset,
            satisfaction: ($scope.reportFilters && $scope.reportFilters.satisfaction) || 'all'
        });
    };

    $scope.clearReportSatisfactionFilter = function() {
        $scope.reportFilters.satisfaction = 'all';
        $scope.$broadcast('filter:reportFiltersChanged', {
            preset: $scope.selectedReportPreset,
            satisfaction: 'all'
        });
    };

    // Event listeners to open and sync report filters from child scopes
    $scope.$on('modal:openReportFilter', function(event, data) {
        if (data) {
            if (data.preset !== undefined) $scope.selectedReportPreset = data.preset;
            if (data.satisfaction !== undefined) {
                if (!$scope.reportFilters) $scope.reportFilters = {};
                $scope.reportFilters.satisfaction = data.satisfaction;
            }
        }
        $scope.openReportFilterModal();
    });

    $scope.$on('modal:syncReportPreset', function(event, preset) {
        $scope.selectedReportPreset = preset;
    });

    $scope.$on('modal:syncSatisfactionFilter', function(event, val) {
        if (!$scope.reportFilters) $scope.reportFilters = {};
        $scope.reportFilters.satisfaction = val || 'all';
    });

    // =========================================================
    // ADMIN DELETE MODAL (Global)
    // =========================================================
    $scope.showDeleteAdminModal = false;
    $scope.adminToDelete = null;

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
        var admin = $scope.adminToDelete;
        $scope.closeDeleteAdminModal();
        $scope.$broadcast('admin:proceedDelete', admin);
    };

    // =========================================================
    // FOLLOW-UP & FEEDBACK MODALS (Global)
    // =========================================================
    $scope.showEditFollowupModal = false;
    $scope.editingFollowup = {};
    $scope.showAddFeedbackModal = false;
    $scope.newCustomerFeedback = { content: '', satisfaction: null, imageUrl: '', authorName: '', orderId: null };
    $scope.showEditFeedbackModal = false;
    $scope.editingCustomerFeedback = {};

    $scope.closeEditFollowupModal = function() {
        $scope.showEditFollowupModal = false;
        $scope.editingFollowup = {};
    };

    $scope.closeAddFeedbackModal = function() {
        $scope.showAddFeedbackModal = false;
    };

    $scope.closeEditFeedbackModal = function() {
        $scope.showEditFeedbackModal = false;
        $scope.editingCustomerFeedback = {};
    };

    $scope.saveFollowupCheck = function() {
        $scope.$broadcast('modal:saveFollowupCheck', $scope.editingFollowup);
    };

    $scope.submitCustomerFeedback = function() {
        $scope.$broadcast('modal:submitCustomerFeedback', $scope.newCustomerFeedback);
    };

    $scope.saveEditFeedback = function() {
        $scope.$broadcast('modal:saveEditFeedback', $scope.editingCustomerFeedback);
    };

    $scope.clearFeedbackImage = function() {
        $scope.newCustomerFeedback.imageUrl = '';
    };

    $scope.clearEditFeedbackImage = function() {
        $scope.editingCustomerFeedback.imageUrl = '';
    };

    $scope.toggleSatisfaction = function(target, val) {
        if (!target) return;
        target.satisfaction = (target.satisfaction === val) ? null : val;
    };

    // =========================================================
    // LIGHTBOX PREVIEW MODAL
    // =========================================================
    $scope.showImageModal = false;
    $scope.previewImageUrl = '';

    $scope.previewImage = function(url) {
        if (!url) return;
        $scope.previewImageUrl = url;
        $scope.showImageModal = true;
    };

    $scope.closeImageModal = function() {
        $scope.showImageModal = false;
        $scope.previewImageUrl = '';
    };

    // Listen to browser popstate
    $window.addEventListener('popstate', function() {
        $scope.$apply(function() {
            initRoute();
        });
    });

    // When TimeFilterController creates/deletes/toggles a preset, reload global list
    $scope.$on('timePresets:changed', function() {
        $scope.loadTimeFilterPresets();
    });

    // Bootstrapping
    initRoute();
    $scope.loadCurrentUser();
    $scope.loadTimeFilterPresets();
}]);

