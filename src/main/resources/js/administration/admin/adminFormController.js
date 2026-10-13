/**
 * Administrator Creation Form Controller
 * Handles account creation, field validation, and role presets.
 */
angular.module('tweklApp')
.controller('AdminFormController', ['$scope', 'AdminService', 'ToastService', function($scope, AdminService, ToastService) {

    $scope.newAdmin = {
        username: '',
        phoneNumber: '',
        password: '',
        superAdmin: false,
        canCreateRoles: true,
        canManageUsers: true,
        status: 'ACTIVE'
    };

    $scope.isSubmitting = false;

    $scope.setSuperAdminMode = function(isSuper) {
        $scope.newAdmin.superAdmin = !!isSuper;
        if (isSuper) {
            $scope.newAdmin.canCreateRoles = true;
            $scope.newAdmin.canManageUsers = true;
        }
    };

    $scope.createAdmin = function() {
        if (!$scope.newAdmin.username || !$scope.newAdmin.password) {
            ToastService.error('Please provide username and password');
            return;
        }

        $scope.isSubmitting = true;
        AdminService.create($scope.newAdmin).then(function(res) {
            $scope.isSubmitting = false;
            ToastService.success('Administrator created successfully');
            $scope.resetForm();
            if ($scope.$parent && $scope.$parent.setAdminView) {
                $scope.$parent.setAdminView('list');
            }
            if ($scope.$parent && $scope.$parent.loadAdmins) {
                $scope.$parent.loadAdmins();
            }
            $scope.$broadcast('admin:created');
        }, function(err) {
            $scope.isSubmitting = false;
            ToastService.error(err.data && err.data.message ? err.data.message : 'Failed to create administrator');
        });
    };

    $scope.resetForm = function() {
        $scope.newAdmin = {
            username: '',
            phoneNumber: '',
            password: '',
            superAdmin: false,
            canCreateRoles: true,
            canManageUsers: true,
            status: 'ACTIVE'
        };
    };

    $scope.cancelCreate = function() {
        $scope.resetForm();
        if ($scope.$parent && $scope.$parent.setAdminView) {
            $scope.$parent.setAdminView('list');
        }
    };

}]);
