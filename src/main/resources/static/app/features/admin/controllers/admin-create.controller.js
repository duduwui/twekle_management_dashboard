/**
 * Admin Action Controller: Admin Create
 * Handles new administrator form state, validation, and submission.
 */
angular.module('tweklApp')
.controller('AdminCreateController', ['$scope', 'AdminCreateService', 'ToastService',
function($scope, AdminCreateService, ToastService) {

    $scope.newAdmin = {
        username: '',
        phoneNumber: '',
        password: '',
        superAdmin: false,
        status: 'ACTIVE'
    };

    /**
     * Submits the create administrator form
     */
    $scope.createAdmin = function() {
        if (!$scope.newAdmin.username || !$scope.newAdmin.password) {
            ToastService.error('Please provide username and password');
            return;
        }

        AdminCreateService.execute($scope.newAdmin).then(function(res) {
            ToastService.success('Admin created successfully!');
            $scope.newAdmin = { username: '', phoneNumber: '', password: '', superAdmin: false, status: 'ACTIVE' };
            $scope.adminView = 'list';
            if (typeof $scope.loadAdmins === 'function') {
                $scope.loadAdmins();
            }
        }, function(err) {
            ToastService.error(err.data?.message || 'Failed to create admin');
        });
    };

    /**
     * Cancels creation and returns to list view
     */
    $scope.cancelCreate = function() {
        $scope.adminView = 'list';
    };
}]);
