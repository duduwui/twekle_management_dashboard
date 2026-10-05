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

    // Active Feature Tab: 'admins', 'users', 'customers', 'time-filters', 'reports'
    $scope.currentTab = 'customers';
    $scope.activeTab = 'customers';
    $scope.reportSubTab = 'milestones'; // 'milestones', 'satisfaction', 'products', 'customers'

    /**
     * Resolves initial active tab based on window pathname
     */
    function initRoute() {
        var path = window.location.pathname;
        var canonicalPath;

        if (path.indexOf('/admin/admins') === 0) {
            $scope.currentTab = 'admins';
            $scope.categories.admin = true;
            canonicalPath = path;
        } else if (path.indexOf('/admin/users') === 0) {
            $scope.currentTab = 'users';
            $scope.categories.admin = true;
            canonicalPath = path;
        } else if (path.indexOf('/admin/time-filters') === 0) {
            $scope.currentTab = 'time-filters';
            canonicalPath = path;
        } else if (path.indexOf('/admin/reports') === 0) {
            $scope.currentTab = 'reports';
            $scope.categories.reports = true;
            var parts = path.split('/');
            var sub = parts[3];
            if (sub && ['milestones','satisfaction','products','customers'].indexOf(sub) !== -1) {
                $scope.reportSubTab = sub;
            }
            canonicalPath = path;
        } else if (path.indexOf('/admin/followups') === 0 || path.indexOf('/admin/customers') === 0) {
            $scope.currentTab = 'customers';
            canonicalPath = path;
        } else if (path.indexOf('/admin/role-templates') === 0) {
            $scope.currentTab = 'roles';
            $scope.categories.admin = true;
            canonicalPath = path;
        } else {
            $scope.currentTab = 'customers';
            canonicalPath = '/admin/followups';
        }

        $scope.activeTab = $scope.currentTab;

        // Synchronize AngularJS $location and HTML5 history so Angular never reverts the URL
        if ($location.path() !== canonicalPath) {
            $location.path(canonicalPath).replace();
        }
        if ($window.history && $window.history.replaceState && canonicalPath && window.location.pathname !== canonicalPath) {
            $window.history.replaceState(null, '', canonicalPath);
        }

        // Also defer URL update to ensure it sticks after all Angular digests
        var _canonical = canonicalPath;
        $timeout(function() {
            if ($location.path() !== _canonical) {
                $location.path(_canonical).replace();
            }
            if ($window.history && $window.history.replaceState && _canonical && window.location.pathname !== _canonical) {
                $window.history.replaceState(null, '', _canonical);
            }
        }, 0);
    }

    /**
     * Push a clean URL path without page reload
     */
    $scope.updateUrl = function(path) {
        if (!path) return;
        if ($location.path() !== path) {
            $location.path(path);
        }
        if ($window.history && $window.history.pushState && window.location.pathname !== path) {
            $window.history.pushState(null, '', path);
        }
    };


    /**
     * Set active tab and update URL
     */
    $scope.setTab = function(tabName) {
        $scope.currentTab = tabName;
        $scope.activeTab = tabName;
        if (tabName === 'admins') {
            $scope.categories.admin = true;
            $scope.updateUrl('/admin/admins');
        } else if (tabName === 'users') {
            $scope.categories.admin = true;
            $scope.updateUrl('/admin/users');
        } else if (tabName === 'customers') {
            $scope.updateUrl('/admin/followups');
        } else if (tabName === 'reports') {
            $scope.categories.reports = true;
            $scope.updateUrl('/admin/reports/' + ($scope.reportSubTab || 'milestones'));
        } else if (tabName === 'time-filters') {
            $scope.updateUrl('/admin/time-filters');
        }
        $scope.$broadcast('tab:changed', tabName);
    };

    /**
     * Set active report sub-tab and update URL
     */
    $scope.setReportSubTab = function(subTab) {
        $scope.reportSubTab = subTab;
        $scope.updateUrl('/admin/reports/' + subTab);
    };

    // =========================================================
    // AUTHENTICATION & USER SESSION STATE
    // =========================================================
    $scope.currentUser = null;
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

    /**
     * Fetch authenticated user session profile
     */
    $scope.loadCurrentUser = function() {
        AuthService.getMe().then(function(res) {
            if (res && res.data) {
                $scope.currentUser = res.data;
                $scope.auth = {
                    authenticated: true,
                    username: res.data.username || '',
                    role: res.data.superAdmin ? 'Super Admin' : (res.data.role || 'Admin'),
                    isSuperAdmin: !!res.data.superAdmin,
                    canCreateRoles: res.data.canCreateRoles !== false,
                    canManageUsers: res.data.canManageUsers !== false
                };
                $scope.loadTimeFilterPresets();
                $scope.$broadcast('auth:loggedIn');
            }
        }, function(err) {
            // Auto-login default super admin in local/demo environment (like 9459a72)
            AuthService.login({ username: 'twekl_super_admin', password: 'Super@2026' }).then(function(loginRes) {
                if (loginRes && loginRes.data) {
                    $scope.currentUser = loginRes.data;
                    $scope.auth = {
                        authenticated: true,
                        username: loginRes.data.username || 'twekl_super_admin',
                        role: 'Super Admin',
                        isSuperAdmin: true,
                        canCreateRoles: true,
                        canManageUsers: true
                    };
                    $scope.loadTimeFilterPresets();
                    $scope.$broadcast('auth:loggedIn');
                }
            }, function() {
                $scope.auth = {
                    authenticated: false,
                    username: '',
                    role: '',
                    isSuperAdmin: false,
                    canCreateRoles: false,
                    canManageUsers: false
                };
                $scope.currentUser = null;
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
