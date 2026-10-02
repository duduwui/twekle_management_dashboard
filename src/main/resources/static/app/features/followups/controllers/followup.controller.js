/**
 * Follow-up Feature Coordinator Controller
 * Coordinates between main follow-up table, order log (Section 4.2), and feedback/reviews (Section 4.3).
 */
angular.module('tweklApp')
.controller('FollowupController', ['$scope', 'CustomerGetOrdersService', 'CustomerGetByIdService', 'ToastService',
function($scope, CustomerGetOrdersService, CustomerGetByIdService, ToastService) {

    // View state: 'list' | 'orders' | 'feedback'
    $scope.followupSubView = 'list';
    $scope.customers = [];
    $scope.activeCustomer = null;
    $scope.customerOrders = [];
    $scope.expandedOrderId = null;

    /**
     * Open Customer Orders Log (Section 4.2)
     */
    $scope.viewCustomerOrders = function(customer, targetOrderId) {
        $scope.activeCustomer = customer;
        $scope.expandedOrderId = targetOrderId || null;
        CustomerGetOrdersService.execute(customer.id).then(function(res) {
            $scope.customerOrders = res.data || [];
            $scope.followupSubView = 'orders';
        }, function() {
            ToastService.error('Failed to load customer orders');
        });
    };

    /**
     * Open Customer Feedback & Compliments (Section 4.3)
     */
    $scope.viewFeedback = function(customer, targetOrder) {
        $scope.activeCustomer = customer;
        CustomerGetOrdersService.execute(customer.id).then(function(res) {
            $scope.customerOrders = res.data || [];
            $scope.followupSubView = 'feedback';
        }, function() {
            ToastService.error('Failed to load customer feedback');
        });
    };

    /**
     * Return to main Follow-ups table
     */
    $scope.backToFollowups = function() {
        $scope.followupSubView = 'list';
        $scope.activeCustomer = null;
        $scope.customerOrders = [];
    };

    /**
     * Toggle expanded accordion order row
     */
    $scope.toggleOrderAccordion = function(orderId) {
        $scope.expandedOrderId = ($scope.expandedOrderId === orderId) ? null : orderId;
    };
}]);
