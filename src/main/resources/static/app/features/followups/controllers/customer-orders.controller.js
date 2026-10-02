/**
 * Follow-up Action Controller: Customer Orders & Audit Checklist (Section 4.2)
 * Handles expanding order checklists, viewing existing notes/photos, and toggling milestone completion checkboxes.
 */
angular.module('tweklApp')
.controller('CustomerOrdersController', ['$scope', 'FollowupToggleStatusService', 'ToastService',
function($scope, FollowupToggleStatusService, ToastService) {

    /**
     * Toggles checklist item status (completed vs pending)
     */
    $scope.toggleCheckStatus = function(check) {
        if (!check || !check.id) return;

        FollowupToggleStatusService.execute(check.id).then(function(res) {
            check.status = (check.status === 'COMPLETED') ? 'PENDING' : 'COMPLETED';
            check.completed = (check.status === 'COMPLETED');
            ToastService.success('Checkpoint status updated');
        }, function(err) {
            ToastService.error('Failed to update status');
        });
    };

    /**
     * Jump from Order Log to Compliments / Feedback view for this order
     */
    $scope.switchToFeedback = function(order) {
        if (typeof $scope.viewFeedback === 'function') {
            $scope.viewFeedback($scope.activeCustomer, order);
        }
    };
}]);
