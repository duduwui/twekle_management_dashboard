/**
 * User Action Controller: User Create
 * Handles multilingual user form submission and 3-Module permission presets (Software, Sales, Products).
 */
angular.module('tweklApp')
.controller('UserCreateController', ['$scope', 'UserCreateService', 'ToastService',
function($scope, UserCreateService, ToastService) {

    $scope.newUser = {
        usernameEn: '',
        usernameAr: '',
        usernameKu: '',
        password: '',
        permissions: [
            { moduleKey: 'software', moduleNameEn: 'Software Management', canCreate: false, canRead: true, canUpdate: false, canDelete: false },
            { moduleKey: 'sales', moduleNameEn: 'Sales & Invoices', canCreate: false, canRead: true, canUpdate: false, canDelete: false },
            { moduleKey: 'products', moduleNameEn: 'Product Management', canCreate: false, canRead: true, canUpdate: false, canDelete: false }
        ]
    };

    /**
     * Preset helper: Full CRUD permissions
     */
    $scope.applyFullCrud = function(perm) {
        perm.canCreate = true;
        perm.canRead = true;
        perm.canUpdate = true;
        perm.canDelete = true;
    };

    /**
     * Preset helper: Read-only permissions
     */
    $scope.applyReadOnly = function(perm) {
        perm.canCreate = false;
        perm.canRead = true;
        perm.canUpdate = false;
        perm.canDelete = false;
    };

    /**
     * Preset helper: Revoke all permissions
     */
    $scope.applyRevoke = function(perm) {
        perm.canCreate = false;
        perm.canRead = false;
        perm.canUpdate = false;
        perm.canDelete = false;
    };

    /**
     * Submits create user request
     */
    $scope.createUser = function() {
        if (!$scope.newUser.usernameEn || !$scope.newUser.password) {
            ToastService.error('Please provide at least English username and password');
            return;
        }

        UserCreateService.execute($scope.newUser).then(function() {
            ToastService.success('User created successfully!');
            $scope.userActiveTab = 'list';
            if (typeof $scope.loadUsers === 'function') {
                $scope.loadUsers();
            }
        }, function(err) {
            ToastService.error(err.data?.message || 'Failed to create user');
        });
    };
}]);
