/**
 * ============================================================================
 * Core Layout, Toast & Navigation Controller (layout.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/core/layout.js
 * Used by:  /WEB-INF/views/administration/fragments/adminLayout.html (All Pages)
 * 
 * Purpose:
 *   Acts as the central frontend orchestrator across the entire portal:
 *   1. Toast Notifications: ToastService & ToastController for success/error popups.
 *   2. Sidebar Navigation: Expand/collapse category accordions, mobile drawer,
 *      and active route highlighting based on current URL path.
 *   3. Filter Modals: Handles universal Date Pickers & Time Preset selection
 *      dialogs shared between Follow-ups and Reports.
 *   4. User Profile & Session: Coordinates current admin badge state & logout action.
 * ============================================================================
 */

// =========================================================
// 1. LAYOUT CONTROLLER (AngularJS Portal Orchestrator)
// =========================================================
if (typeof angular !== 'undefined') {
angular.module('tweklApp')
.controller('LayoutController', [
    '$scope', '$http', '$location', '$window', '$timeout', 'TimeFilterService', 'ToastService', 'FilterHelperService',
function($scope, $http, $location, $window, $timeout, TimeFilterService, ToastService, FilterHelperService) {

    // Sidebar Category Accordions
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

    // Active Feature Tab
    $scope.currentTab = window.SERVER_ACTIVE_TAB || 'dashboard';
    $scope.activeTab = $scope.currentTab;
    var initialUrlParams = new URLSearchParams(window.location.search);
    $scope.reportSubTab = initialUrlParams.get('tab') || window.SERVER_REPORT_SUBTAB || 'milestones';

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
        } else if (path.indexOf('/admin/reports') === 0 || path.indexOf('/reports') === 0) {
            $scope.currentTab = 'reports';
            $scope.categories.reports = true;
            var urlParams = new URLSearchParams(window.location.search);
            var tabParam = urlParams.get('tab');
            var parts = path.split('/');
            var sub = (parts.length > 3 && parts[3]) ? parts[3] : (tabParam || window.SERVER_REPORT_SUBTAB);
            if (sub && ['milestones','satisfaction','products','customers'].indexOf(sub) !== -1) {
                $scope.reportSubTab = sub;
            } else {
                $scope.reportSubTab = 'milestones';
            }
        } else if (path.indexOf('/admin/followups') === 0 || path.indexOf('/admin/customers') === 0) {
            $scope.currentTab = 'customers';
        } else {
            $scope.currentTab = 'dashboard';
        }

        $scope.activeTab = $scope.currentTab;
    }

    $scope.updateUrl = function(path) {
        if (!path) return;
        if (window.location.pathname !== path) {
            window.location.href = path;
        }
    };

    $scope.setTab = function(tabName) {
        if (tabName === 'admins') {
            window.location.href = '/admin/admins';
        } else if (tabName === 'roles') {
            window.location.href = '/admin/roles';
        } else if (tabName === 'customers' || tabName === 'followups') {
            window.location.href = '/admin/followups';
        } else if (tabName === 'reports') {
            window.location.href = '/admin/reports';
        } else if (tabName === 'time-filters' || tabName === 'timelines') {
            window.location.href = '/admin/time-filters';
        } else {
            window.location.href = '/admin/dashboard';
        }
    };

    $scope.setReportSubTab = function(subTab) {
        $scope.reportSubTab = subTab;
        $scope.$broadcast('reportSubTab:changed', subTab);
        if (window.history && window.history.pushState) {
            var newUrl = '/admin/reports/' + encodeURIComponent(subTab);
            window.history.pushState(null, '', newUrl);
        }
    };

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
            canReadFollowups: !!su.superAdmin || !!su.canReadFollowups,
            canReadReports: !!su.superAdmin || !!su.canReadReports,
            canUpdateFollowups: !!su.superAdmin || !!su.canUpdateFollowups,
            canCreateFollowups: !!su.superAdmin || !!su.canCreateFollowups
        };
        if (window.SERVER_ACTIONS) {
            angular.extend($scope.auth, window.SERVER_ACTIONS);
        }
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
        if (window.SERVER_ACTIONS) {
            angular.extend($scope.auth, window.SERVER_ACTIONS);
        }
    }

    $scope.showLoginModal = false;
    $scope.loginForm = {
        username: '',
        password: ''
    };

    $scope.openLoginModal = function() { window.location.href = '/login'; };
    $scope.closeLoginModal = function() { $scope.showLoginModal = false; };
    $scope.login = function() { window.location.href = '/login'; };

    $scope.logout = function() {
        document.getElementById('logoutForm').submit();
    };

    // =========================================================
    // GLOBAL TIME FILTER PRESETS
    // =========================================================
    $scope.timeFilterPresets = window.SERVER_PRESETS || [];
    $scope.selectedTimePreset = null;
    $scope.selectedOrderTimePreset = null;
    $scope.selectedReportPreset = null;
    $scope.showBigFilterModal = false;
    $scope.currentFilterTarget = 'customer';
    $scope.pendingFilterPreset = null;

    $scope.toDurationMinutes = FilterHelperService.toDurationMinutes;

    $scope.loadTimeFilterPresets = function() {
        return TimeFilterService.getAll().then(function(res) {
            var raw = res.data;
            var arr = Array.isArray(raw) ? raw : (raw && Array.isArray(raw.data) ? raw.data : []);
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
    $scope.pendingCustomerType = 'ALL';
    $scope.pendingCustomerCity = 'ALL';
    $scope.selectedUserTimePreset = null;

    $scope.openFilterModal = function(target) {
        if (!window.SERVER_PRESETS && !$scope.timeFilterPresets.length) $scope.loadTimeFilterPresets();
        $scope.currentFilterTarget = target || 'customer';
        if ($scope.currentFilterTarget === 'customer') {
            $scope.pendingFilterPreset = $scope.selectedTimePreset;
            $scope.pendingCustomerFollowupStatus = $scope.customerFollowupStatusFilter || 'ALL';
            $scope.pendingCustomerType = $scope.customerTypeFilter || 'ALL';
            $scope.pendingCustomerCity = $scope.customerCityFilter || 'ALL';
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
            $scope.customerTypeFilter = $scope.pendingCustomerType || 'ALL';
            $scope.customerCityFilter = $scope.pendingCustomerCity || 'ALL';
            $scope.$broadcast('filter:timePresetChanged', $scope.selectedTimePreset);
            $scope.$broadcast('filter:followupStatusChanged', $scope.customerFollowupStatusFilter);
            $scope.$broadcast('filter:customerTypeChanged', $scope.customerTypeFilter);
            $scope.$broadcast('filter:customerCityChanged', $scope.customerCityFilter);
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

    $scope.resetBigFilterModal = function() {
        $scope.pendingFilterPreset = null;
        $scope.pendingCustomerFollowupStatus = 'ALL';
        $scope.pendingCustomerType = 'ALL';
        $scope.pendingCustomerCity = 'ALL';
        $scope.confirmApplyFilterModal();
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

    $scope.openReportFilterModal = function(initialFilters) {
        $scope.pendingReportFilters = initialFilters || {
            satisfaction: ($scope.reportFilters && $scope.reportFilters.satisfaction) || 'all',
            preset: $scope.selectedReportPreset,
            milestone: ($scope.reportFilters && $scope.reportFilters.milestone) || 'all',
            customerType: ($scope.reportFilters && $scope.reportFilters.customerType) || 'all',
            customerTier: ($scope.reportFilters && $scope.reportFilters.customerTier) || 'all',
            customerStatus: ($scope.reportFilters && $scope.reportFilters.customerStatus) || 'all',
            customerCity: ($scope.reportFilters && $scope.reportFilters.customerCity) || 'all'
        };
        $scope.showReportFilterModal = true;
        if ($scope.modalState) $scope.modalState.reportFilter = true;
    };

    $scope.closeReportFilterModal = function() {
        $scope.showReportFilterModal = false;
        if ($scope.modalState) $scope.modalState.reportFilter = false;
    };

    $scope.selectPendingReportPreset = function(preset) {
        if (!$scope.pendingReportFilters) $scope.pendingReportFilters = {};
        $scope.pendingReportFilters.preset = preset;
    };

    $scope.isPendingReportPresetSelected = function(preset) {
        if (!$scope.pendingReportFilters) return !preset;
        if (!preset && !$scope.pendingReportFilters.preset) return true;
        if (!preset || !$scope.pendingReportFilters.preset) return false;
        return preset.id === $scope.pendingReportFilters.preset.id;
    };

    $scope.confirmApplyReportFilterModal = function() {
        var pf = $scope.pendingReportFilters || {};
        $scope.selectedReportPreset = pf.preset;
        $scope.reportFilters.satisfaction = pf.satisfaction || 'all';
        $scope.reportFilters.milestone = pf.milestone || 'all';
        $scope.reportFilters.customerType = pf.customerType || 'all';
        $scope.reportFilters.customerTier = pf.customerTier || 'all';
        $scope.reportFilters.customerStatus = pf.customerStatus || 'all';
        $scope.reportFilters.customerCity = pf.customerCity || 'all';
        $scope.closeReportFilterModal();
        $scope.$broadcast('filter:reportFiltersChanged', {
            preset: $scope.selectedReportPreset,
            satisfaction: $scope.reportFilters.satisfaction,
            milestone: $scope.reportFilters.milestone,
            customerType: $scope.reportFilters.customerType,
            customerTier: $scope.reportFilters.customerTier,
            customerStatus: $scope.reportFilters.customerStatus,
            customerCity: $scope.reportFilters.customerCity
        });
    };

    $scope.resetReportFilterModal = function() {
        $scope.pendingReportFilters = {
            satisfaction: 'all',
            preset: null,
            milestone: 'all',
            customerType: 'all',
            customerTier: 'all',
            customerStatus: 'all',
            customerCity: 'all'
        };
        $scope.selectedReportPreset = null;
        $scope.reportFilters = {
            satisfaction: 'all',
            milestone: 'all',
            customerType: 'all',
            customerTier: 'all',
            customerStatus: 'all',
            customerCity: 'all'
        };
        $scope.closeReportFilterModal();
        $scope.$broadcast('filter:reportFiltersChanged', {
            preset: null,
            satisfaction: 'all',
            milestone: 'all',
            customerType: 'all',
            customerTier: 'all',
            customerStatus: 'all',
            customerCity: 'all'
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
    // =========================================================
    // FOLLOW-UP & FEEDBACK MODALS (Global Multi-Image Support)
    // =========================================================
    $scope.showEditFollowupModal = false;
    $scope.editingFollowup = {};
    $scope.tempImageUrl = '';

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
    };

    $scope.isOrderOlderThanFourMonths = function(order) {
        if (!order || !order.orderDate) return false;
        var orderTime = new Date(order.orderDate).getTime();
        var now = new Date().getTime();
        var diffDays = (now - orderTime) / (1000 * 60 * 60 * 24);
        return diffDays >= 120; // 4 months
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

    $scope.isOrderUnfinished = function(order) {
        if (!order || !order.id) return false;
        if (order.isExpired === true) return true;
        if (order.isExpired === false) return false;
        var checks = $scope.orderFollowups ? $scope.orderFollowups[order.id] : null;
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

    $scope.isCheckUnfinished = function(check, order) {
        if (!check && !order) return false;
        if (check && (check.isCompleted || (check.note && check.note.trim().length > 0) || check.forcePending)) {
            return false;
        }
        if (check && (check.status === 'UNFINISHED' || check.status === 'EXPIRED' || check.status === 'SKIPPED')) {
            return true;
        }
        if (order && $scope.isOrderOlderThanFourMonths(order)) {
            return true;
        }
        if (check && check.orderId) {
            var ord = $scope.findOrderById ? $scope.findOrderById(check.orderId) : null;
            if (ord && $scope.isOrderOlderThanFourMonths(ord)) {
                return true;
            }
        }
        return false;
    };
    $scope.isCheckExpired = $scope.isCheckUnfinished;

    $scope.toggleOrderUnfinished = function(order, $event) {
        if ($event) $event.stopPropagation();
        if (!order || !order.id) return;
        var isCurrentlyUnfinished = $scope.isOrderUnfinished(order);
        var url = isCurrentlyUnfinished ? ('/admin/orders/' + order.id + '/unskip') : ('/admin/orders/' + order.id + '/skip');
        $http.post(url).then(function() {
            order.isExpired = !isCurrentlyUnfinished;
            ToastService.success(isCurrentlyUnfinished ? 'Order restored to Pending' : 'Order marked as Unfinished');
            $scope.$broadcast('order:reload', order.id);
            $scope.$broadcast('customer:reload');
        }, function() {
            ToastService.error('Failed to update order state');
        });
    };
    $scope.toggleOrderExpire = $scope.toggleOrderUnfinished;

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
    };

    $scope.toggleSatisfaction = function(target, val) {
        if (!target) return;
        target.satisfaction = val;
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
                        element.value = '';
                    });
                },
                function(errMsg) {
                    ToastService.error(errMsg || 'Failed to process images');
                }
            );
        }
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
        var noteTextVal = hasFeedback ? noteText : '';

        var payload = {
            notes: noteTextVal,
            photoUrl: finalImageUrl,
            imageUrl: finalImageUrl,
            satisfaction: sat || '',
            status: finalStatus
        };
        $http.post('/admin/followups/' + checkId, payload).then(function(res) {
            $scope.closeEditFollowupModal();
            $scope.$broadcast('followup:updated', {
                checkId: checkId,
                orderId: orderId,
                note: noteTextVal,
                notes: noteTextVal,
                imageUrl: finalImageUrl,
                photoUrl: finalImageUrl,
                satisfaction: sat,
                status: finalStatus,
                isCompleted: (finalStatus === 'COMPLETED'),
                forcePending: !hasFeedback
            });
            $scope.$broadcast('dashboard:reload');
            $scope.$broadcast('customer:reload');
            $scope.$broadcast('report:reload');
            ToastService.success(hasFeedback ? 'Milestone and feedback saved' : 'Feedback cleared. Milestone restored to Pending.');
        }).catch(function() {
            ToastService.error('Could not save the follow-up. Your changes have not been submitted.');
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
        $http.post('/admin/followups/' + checkId, payload).then(function(res) {
            $scope.closeEditFollowupModal();
            $scope.$broadcast('followup:updated', {
                checkId: checkId,
                orderId: orderId,
                note: '',
                notes: '',
                imageUrl: '',
                photoUrl: '',
                satisfaction: null,
                status: 'PENDING',
                isCompleted: false,
                forcePending: true
            });
            $scope.$broadcast('dashboard:reload');
            $scope.$broadcast('customer:reload');
            $scope.$broadcast('report:reload');
            ToastService.success('Feedback deleted. Milestone restored to Pending.');
        }).catch(function() {
            ToastService.error('Could not delete feedback.');
        });
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

    $window.addEventListener('popstate', function() {
        $scope.$apply(function() {
            initRoute();
        });
    });

    $scope.$on('timePresets:changed', function() {
        $scope.loadTimeFilterPresets();
    });

    initRoute();

}]);
}

// =========================================================
// 3. NAVIGATION (Clean Sidebar Category Accordion Toggle)
// =========================================================
function toggleCategoryNav(name) {
    var sub = document.getElementById('subnav-' + name);
    var arrow = document.getElementById('arrow-' + name);
    if (!sub) return;
    var isHidden = (sub.style.display === 'none' || getComputedStyle(sub).display === 'none');
    if (isHidden) {
        sub.style.display = 'flex';
        if (arrow) arrow.classList.add('expanded');
    } else {
        sub.style.display = 'none';
        if (arrow) arrow.classList.remove('expanded');
    }
}

// =========================================================
// 4. GLOBAL TOAST & STATUS TOGGLE CONTROLS
// =========================================================

/**
 * Universal Floating Toast Notification (works with or without AngularJS)
 */
function showStatusToast(message, type) {
    if (typeof angular !== 'undefined') {
        try {
            var el = document.querySelector('[ng-app="tweklApp"]') || document.body;
            var inj = angular.element(el).injector();
            if (inj && inj.has('ToastService')) {
                var ts = inj.get('ToastService');
                var rs = inj.get('$rootScope');
                rs.$apply(function() {
                    ts.show(message, type);
                });
                return;
            }
        } catch (e) {}
    }

    var container = document.getElementById('vanillaToastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'vanillaToastContainer';
        container.style.cssText = 'position: fixed; top: 24px; right: 24px; z-index: 999999; display: flex; flex-direction: column; gap: 10px; pointer-events: none;';
        document.body.appendChild(container);
    }

    var toast = document.createElement('div');
    var isSuccess = type !== 'error';
    var bg = isSuccess ? '#ECFDF5' : '#FEF2F2';
    var border = isSuccess ? '#A7F3D0' : '#FECACA';
    var text = isSuccess ? '#065F46' : '#991B1B';
    var icon = isSuccess ? '✓' : '✕';

    toast.style.cssText = 'pointer-events: auto; background: ' + bg + '; border: 1.5px solid ' + border + '; color: ' + text + '; padding: 12px 18px; border-radius: 10px; font-weight: 700; font-size: 0.9rem; box-shadow: 0 4px 14px rgba(0,0,0,0.12); display: flex; align-items: center; gap: 10px; min-width: 260px; max-width: 420px; transform: translateY(-10px); opacity: 0; transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);';
    toast.innerHTML = '<span style="display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px; border-radius:50%; background:' + (isSuccess ? '#10B981' : '#EF4444') + '; color:#fff; font-size:12px; font-weight:900;">' + icon + '</span><span>' + message + '</span>';

    container.appendChild(toast);
    requestAnimationFrame(function() {
        toast.style.transform = 'translateY(0)';
        toast.style.opacity = '1';
    });

    setTimeout(function() {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(-10px)';
        setTimeout(function() {
            if (toast.parentNode) toast.parentNode.removeChild(toast);
        }, 250);
    }, 3500);
}
window.showStatusToast = showStatusToast;

/**
 * Interactive Live Status Toggle for Administrators
 */
async function toggleAdminStatusLive(btn, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    if (!btn || btn.classList.contains('loading')) return;

    var adminId = btn.getAttribute('data-admin-id');
    if (!adminId) return;

    var wasActive = btn.getAttribute('data-active') === 'true';
    var willBeActive = !wasActive;

    // Optimistic UI update
    btn.classList.add('loading');
    btn.classList.toggle('active', willBeActive);
    btn.classList.toggle('inactive', !willBeActive);

    var slider = btn.querySelector('.status-toggle-track, .toggle-slider');
    var thumb = btn.querySelector('.status-toggle-thumb, .toggle-knob');
    var label = btn.querySelector('.status-toggle-label, .toggle-label');

    if (slider) slider.classList.toggle('active', willBeActive);
    if (thumb) thumb.classList.toggle('active', willBeActive);
    if (label) {
        label.textContent = willBeActive ? 'Active' : 'Inactive';
        label.classList.toggle('active', willBeActive);
        label.classList.toggle('inactive', !willBeActive);
    }
    btn.setAttribute('data-active', willBeActive ? 'true' : 'false');
    btn.setAttribute('title', willBeActive ? 'Active - Click to deactivate' : 'Inactive - Click to activate');

    try {
        var response = await fetch('/admin/admins/' + adminId + '/toggle-status', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        var data = {};
        try {
            data = await response.json();
        } catch (e) {
            data = {};
        }

        if (!response.ok || !data.success) {
            throw new Error(data.message || 'Failed to update administrator status');
        }

        var confirmedActive = (data.active !== undefined) ? data.active : willBeActive;
        btn.classList.toggle('active', confirmedActive);
        btn.classList.toggle('inactive', !confirmedActive);
        if (slider) slider.classList.toggle('active', confirmedActive);
        if (thumb) thumb.classList.toggle('active', confirmedActive);
        if (label) {
            label.textContent = confirmedActive ? 'Active' : 'Inactive';
            label.classList.toggle('active', confirmedActive);
            label.classList.toggle('inactive', !confirmedActive);
        }
        btn.setAttribute('data-active', confirmedActive ? 'true' : 'false');
        btn.setAttribute('title', confirmedActive ? 'Active - Click to deactivate' : 'Inactive - Click to activate');

        showStatusToast(data.message || ('Status updated to ' + (confirmedActive ? 'Active' : 'Inactive')), 'success');
    } catch (err) {
        // Rollback on failure
        btn.classList.toggle('active', wasActive);
        btn.classList.toggle('inactive', !wasActive);
        if (slider) slider.classList.toggle('active', wasActive);
        if (thumb) thumb.classList.toggle('active', wasActive);
        if (label) {
            label.textContent = wasActive ? 'Active' : 'Inactive';
            label.classList.toggle('active', wasActive);
            label.classList.toggle('inactive', !wasActive);
        }
        btn.setAttribute('data-active', wasActive ? 'true' : 'false');
        btn.setAttribute('title', wasActive ? 'Active - Click to deactivate' : 'Inactive - Click to activate');

        showStatusToast(err.message || 'Failed to update status', 'error');
    } finally {
        btn.classList.remove('loading');
    }
}
window.toggleAdminStatusLive = toggleAdminStatusLive;

/**
 * Interactive Live Delete for Administrators
 * Confirms with user, deletes via AJAX, and smoothly removes row without page reload.
 */
async function deleteAdminLive(btn, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    if (!btn || btn.classList.contains('loading')) return;

    var form = btn.closest('form');
    var adminId = btn.getAttribute('data-admin-id') || (form && form.action ? (form.action.match(/\/admin\/admins\/(\d+)\/delete-admin/)?.[1]) : null);
    var uname = btn.getAttribute('data-uname') || (form ? form.getAttribute('data-uname') : '') || 'this administrator';

    if (!confirm("Are you sure you want to delete administrator '" + uname + "'?")) {
        return;
    }

    var row = btn.closest('tr.admin-row, tr.admin-table-row') || btn.closest('tr');
    btn.disabled = true;
    btn.classList.add('loading');

    try {
        var actionUrl = (form && form.action) ? form.action : ('/admin/admins/' + adminId + '/delete-admin');
        var response = await fetch(actionUrl, {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        });
        var data = await response.json();
        if (!response.ok || data.success === false) {
            throw new Error(data.message || 'Failed to delete administrator');
        }

        if (row) {
            row.style.transition = 'all 0.3s ease';
            row.style.opacity = '0';
            row.style.transform = 'translateX(25px)';
            setTimeout(function() {
                row.remove();
                var countElements = document.querySelectorAll('.comic-tag strong, .datatable-info strong');
                countElements.forEach(function(el) {
                    var cur = parseInt(el.textContent, 10);
                    if (!isNaN(cur) && cur > 0) el.textContent = cur - 1;
                });
            }, 300);
        }

        showStatusToast(data.message || 'Administrator deleted successfully.', 'success');
    } catch (err) {
        btn.disabled = false;
        btn.classList.remove('loading');
        showStatusToast(err.message || 'Failed to delete administrator', 'error');
    }
}
window.deleteAdminLive = deleteAdminLive;

/* =========================================================
   UNIVERSAL TABLE HEADER SORTING FOR ALL PAGES
   ========================================================= */
(function() {
    function initUniversalTableSorting() {
        document.addEventListener('click', function(e) {
            var th = e.target.closest('table.comic-table thead th');
            if (!th) return;
            var table = th.closest('table.comic-table');
            if (!table) return;

            // Skip tables with specialized controllers or Angular directives
            if (table.id === 'customerLogTable' || table.id === 'priorityAttentionTable' || th.hasAttribute('ng-click') || th.classList.contains('no-sort')) {
                return;
            }

            var headerText = th.textContent.trim().toLowerCase();
            if (headerText === 'action' || headerText === 'actions') {
                return;
            }

            var thIndex = Array.prototype.indexOf.call(th.parentNode.children, th);
            if (thIndex === -1) return;

            var tbody = table.querySelector('tbody');
            if (!tbody) return;

            var rows = Array.prototype.slice.call(tbody.querySelectorAll('tr'));
            if (rows.length <= 1) return;

            var currentDir = th.getAttribute('data-sort-dir');
            var newDir = currentDir === 'asc' ? 'desc' : 'asc';

            Array.prototype.forEach.call(th.parentNode.children, function(otherTh) {
                otherTh.removeAttribute('data-sort-dir');
                otherTh.classList.remove('sort-asc', 'sort-desc');
                var icon = otherTh.querySelector('.universal-sort-icon');
                if (icon) icon.textContent = ' ↕';
            });

            th.setAttribute('data-sort-dir', newDir);
            th.classList.add(newDir === 'asc' ? 'sort-asc' : 'sort-desc');
            var sortIcon = th.querySelector('.universal-sort-icon');
            if (!sortIcon) {
                sortIcon = document.createElement('span');
                sortIcon.className = 'universal-sort-icon';
                sortIcon.style.fontSize = '0.72rem';
                sortIcon.style.marginLeft = '4px';
                sortIcon.style.color = '#0F172A';
                th.appendChild(sortIcon);
            }
            sortIcon.textContent = newDir === 'asc' ? ' ▲' : ' ▼';

            rows.sort(function(rowA, rowB) {
                var cellA = rowA.children[thIndex];
                var cellB = rowB.children[thIndex];
                var valA = cellA ? (cellA.dataset.sortVal || cellA.textContent).trim() : '';
                var valB = cellB ? (cellB.dataset.sortVal || cellB.textContent).trim() : '';

                var numA = parseFloat(valA.replace(/[^0-9.-]/g, ''));
                var numB = parseFloat(valB.replace(/[^0-9.-]/g, ''));
                if (!isNaN(numA) && !isNaN(numB) && valA.match(/^[#$€£\s0-9.,%-]+$/)) {
                    return newDir === 'asc' ? numA - numB : numB - numA;
                }

                return newDir === 'asc' 
                    ? valA.localeCompare(valB, undefined, { numeric: true, sensitivity: 'base' })
                    : valB.localeCompare(valA, undefined, { numeric: true, sensitivity: 'base' });
            });

            rows.forEach(function(r) { tbody.appendChild(r); });
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initUniversalTableSorting);
    } else {
        initUniversalTableSorting();
    }
})();

/* =========================================================
   UNIVERSAL SCROLL POSITION PRESERVATION ON SSR FORMS & PAGE RELOADS
   ========================================================= */
(function() {
    // 1. Immediately restore any saved scroll position upon page load
    try {
        var savedY = sessionStorage.getItem('twekl_scroll_preserve_y');
        if (savedY !== null) {
            sessionStorage.removeItem('twekl_scroll_preserve_y');
            var targetY = parseInt(savedY, 10);
            if (!isNaN(targetY) && targetY > 0) {
                window.scrollTo({ top: targetY, behavior: 'instant' });
                requestAnimationFrame(function() {
                    window.scrollTo({ top: targetY, behavior: 'instant' });
                });
                setTimeout(function() {
                    window.scrollTo({ top: targetY, behavior: 'instant' });
                }, 50);
            }
        }
    } catch (e) {}

    // 2. Intercept standard form submissions that cause real page navigation
    document.addEventListener('submit', function(e) {
        try {
            // Wait to ensure this submit was not intercepted by an AJAX preventDefault()
            setTimeout(function() {
                if (!e.defaultPrevented && window.scrollY > 0) {
                    sessionStorage.setItem('twekl_scroll_preserve_y', window.scrollY.toString());
                }
            }, 0);
        } catch (err) {}
    }, true);
})();
