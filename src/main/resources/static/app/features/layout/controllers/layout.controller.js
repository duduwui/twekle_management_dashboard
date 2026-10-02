/**
 * Main Layout Coordinator Controller
 * Manages global application shell, active tab routing, authentication, shared modals, and presets.
 */
angular.module('tweklApp')
.controller('LayoutController', [
    '$scope', '$http', '$location', '$window', 'AuthGetMeService', 'AuthLoginService', 'AuthLogoutService', 'TimeFilterGetAllService', 'ToastService', 'FilterHelperService',
function($scope, $http, $location, $window, AuthGetMeService, AuthLoginService, AuthLogoutService, TimeFilterGetAllService, ToastService, FilterHelperService) {

    // Sidebar Category Accordions (Open by default)
    $scope.categories = {
        admin: true,
        reports: true
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
        if (path.indexOf('/admin/admins') === 0) {
            $scope.currentTab = 'admins';
        } else if (path.indexOf('/admin/users') === 0) {
            $scope.currentTab = 'users';
        } else if (path.indexOf('/admin/time-filters') === 0) {
            $scope.currentTab = 'time-filters';
        } else if (path.indexOf('/admin/reports') === 0) {
            $scope.currentTab = 'reports';
        } else {
            $scope.currentTab = 'customers';
        }
        $scope.activeTab = $scope.currentTab;
    }

    /**
     * Set active tab
     */
    $scope.setTab = function(tabName) {
        $scope.currentTab = tabName;
        $scope.activeTab = tabName;
    };

    /**
     * Set active report sub-tab
     */
    $scope.setReportSubTab = function(subTab) {
        $scope.reportSubTab = subTab;
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
        AuthGetMeService.execute().then(function(res) {
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
            }
        }, function(err) {
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

        AuthLoginService.execute($scope.loginForm).then(function(res) {
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
        AuthLogoutService.execute().then(function() {
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
        return TimeFilterGetAllService.execute().then(function(res) {
            var arr = res.data || [];
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

    $scope.openFilterModal = function(target) {
        $scope.currentFilterTarget = target || 'customer';
        if ($scope.currentFilterTarget === 'customer' || $scope.currentFilterTarget === 'user') {
            $scope.pendingFilterPreset = $scope.selectedTimePreset;
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

    $scope.confirmApplyFilterModal = function() {
        if ($scope.currentFilterTarget === 'customer' || $scope.currentFilterTarget === 'user') {
            $scope.selectedTimePreset = $scope.pendingFilterPreset;
            $scope.$broadcast('filter:timePresetChanged', $scope.selectedTimePreset);
        } else if ($scope.currentFilterTarget === 'order') {
            $scope.selectedOrderTimePreset = $scope.pendingFilterPreset;
            $scope.$broadcast('filter:orderTimePresetChanged', $scope.selectedOrderTimePreset);
        } else if ($scope.currentFilterTarget === 'report') {
            $scope.selectedReportPreset = $scope.pendingFilterPreset;
            $scope.$broadcast('filter:reportTimePresetChanged', $scope.selectedReportPreset);
        }
        $scope.closeFilterModal();
    };

    $scope.selectTimePreset = function(preset) {
        $scope.selectedTimePreset = preset;
        $scope.$broadcast('filter:timePresetChanged', preset);
    };

    $scope.selectOrderTimePreset = function(preset) {
        $scope.selectedOrderTimePreset = preset;
        $scope.$broadcast('filter:orderTimePresetChanged', preset);
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

    // Bootstrapping
    initRoute();
    $scope.loadCurrentUser();
    $scope.loadTimeFilterPresets();
}]);
