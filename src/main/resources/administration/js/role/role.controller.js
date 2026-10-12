.controller('RoleController', ['$scope', '$window', 'RoleService', 'ToastService',
function($scope, $window, RoleService, ToastService) {
    $scope.roles = $window.SERVER_ROLE_LIST || [];
    $scope.selectedRole = null;
    $scope.modules = [
        { key: 'ADMIN_MANAGEMENT', name: 'Administration & Staff', description: 'Manage administrator accounts, security credentials, role assignments, and system privilege matrix.' },
        { key: 'CUSTOMER_FOLLOWUP', name: 'Customer Follow-ups', description: 'Customer directory, sales order timelines, checkpoint checklists, note documentation & photo attachments.' },
        { key: 'REPORTS_INSIGHTS', name: 'Reports & Analytics', description: 'Operational milestone turnaround metrics, customer satisfaction feedback, product reviews, and period export.' }
    ];

    $scope.activePerms = {
        ADMIN_MANAGEMENT: { canCreate: false, canRead: true, canUpdate: false, canDelete: false },
        CUSTOMER_FOLLOWUP: { canCreate: false, canRead: true, canUpdate: false, canDelete: false },
        REPORTS_INSIGHTS: { canCreate: false, canRead: true, canUpdate: false, canDelete: false }
    };

    $scope.selectRole = function(role) {
        $scope.selectedRole = role;
        $scope.modules.forEach(function(m) {
            $scope.activePerms[m.key] = {
                canCreate: !!role.canCreate,
                canRead: !!role.canRead,
                canUpdate: !!role.canUpdate,
                canDelete: !!role.canDelete
            };
        });
    };

    $scope.applyCardPreset = function(modKey, preset) {
        var p = $scope.activePerms[modKey];
        if (!p) return;
        if (preset === 'full') {
            p.canCreate = true; p.canRead = true; p.canUpdate = true; p.canDelete = true;
        } else if (preset === 'readonly') {
            p.canCreate = false; p.canRead = true; p.canUpdate = false; p.canDelete = false;
        } else if (preset === 'revoke') {
            p.canCreate = false; p.canRead = false; p.canUpdate = false; p.canDelete = false;
        }
    };

    $scope.applyAllPreset = function(preset) {
        $scope.modules.forEach(function(m) {
            $scope.applyCardPreset(m.key, preset);
        });
    };
}]);
