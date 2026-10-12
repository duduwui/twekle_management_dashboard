/**
 * Customer Feedback & Satisfaction Review Controller
 * Handles customer feedback modal, satisfaction level toggles, image uploads, and notes.
 */
angular.module('tweklApp')
.controller('FeedbackController', [
    '$scope', '$http', 'FollowupService', 'ToastService',
function($scope, $http, FollowupService, ToastService) {

    $scope.showEditFollowupModal = false;
    $scope.editingFollowup = {};
    $scope.showAddFeedbackModal = false;
    $scope.newCustomerFeedback = { content: '', satisfaction: null, imageUrl: '', authorName: '', orderId: null };
    $scope.showEditFeedbackModal = false;
    $scope.editingCustomerFeedback = {};

    $scope.openEditFollowupModal = function(order, check, $event) {
        if ($event) $event.stopPropagation();
        var item = angular.copy(check) || {};
        item.orderId = order ? order.id : null;
        item.note = check.note || check.notes || '';
        item.imageUrl = check.imageUrl || check.photoUrl || '';
        item.satisfaction = (check.satisfaction && check.satisfaction.trim() !== '') ? check.satisfaction.toUpperCase() : 'NEUTRAL';
        $scope.editingFollowup = item;
        $scope.showEditFollowupModal = true;
        if ($scope.$parent && $scope.$parent.openEditFollowupModal) {
            $scope.$parent.openEditFollowupModal(order, check, $event);
        }
    };

    $scope.closeEditFollowupModal = function() {
        $scope.showEditFollowupModal = false;
        $scope.editingFollowup = {};
        if ($scope.$parent && $scope.$parent.closeEditFollowupModal) {
            $scope.$parent.closeEditFollowupModal();
        }
    };

    $scope.toggleSatisfaction = function(target, val) {
        if (!target) return;
        target.satisfaction = val;
    };

    $scope.handleFollowupImageUpload = function(element) {
        if (element.files && element.files[0]) {
            var file = element.files[0];
            if (!file.type.match('image.*')) {
                ToastService.error('Please select an image file');
                return;
            }
            var reader = new FileReader();
            reader.onload = function(e) {
                var img = new Image();
                img.onload = function() {
                    var canvas = document.createElement('canvas');
                    var MAX_WIDTH = 1200;
                    var MAX_HEIGHT = 1200;
                    var width = img.width;
                    var height = img.height;

                    if (width > height) {
                        if (width > MAX_WIDTH) {
                            height *= MAX_WIDTH / width;
                            width = MAX_WIDTH;
                        }
                    } else {
                        if (height > MAX_HEIGHT) {
                            width *= MAX_HEIGHT / height;
                            height = MAX_HEIGHT;
                        }
                    }
                    canvas.width = width;
                    canvas.height = height;
                    var ctx = canvas.getContext('2d');
                    ctx.drawImage(img, 0, 0, width, height);
                    var dataUrl = canvas.toDataURL('image/jpeg', 0.85);
                    $scope.$apply(function() {
                        $scope.editingFollowup.imageUrl = dataUrl;
                        if ($scope.$parent && $scope.$parent.editingFollowup) {
                            $scope.$parent.editingFollowup.imageUrl = dataUrl;
                        }
                    });
                };
                img.src = e.target.result;
            };
            reader.readAsDataURL(file);
        }
    };

    $scope.saveFollowupCheck = function() {
        if (!$scope.editingFollowup || !$scope.editingFollowup.id) return;
        var sat = $scope.editingFollowup.satisfaction || 'NEUTRAL';
        var payload = {
            notes: $scope.editingFollowup.note || '',
            photoUrl: $scope.editingFollowup.imageUrl || '',
            satisfaction: sat
        };
        FollowupService.updateFollowup($scope.editingFollowup.id, payload).then(function(res) {
            ToastService.success('Feedback saved successfully');
            if ($scope.editingFollowup.orderId && $scope.orderFollowups && $scope.orderFollowups[$scope.editingFollowup.orderId]) {
                var list = $scope.orderFollowups[$scope.editingFollowup.orderId];
                for (var i = 0; i < list.length; i++) {
                    if (list[i].id === $scope.editingFollowup.id) {
                        list[i].note = $scope.editingFollowup.note;
                        list[i].notes = $scope.editingFollowup.note;
                        list[i].imageUrl = $scope.editingFollowup.imageUrl;
                        list[i].photoUrl = $scope.editingFollowup.imageUrl;
                        list[i].satisfaction = sat;
                        break;
                    }
                }
            }
            $scope.closeEditFollowupModal();
            if ($scope.$parent && $scope.$parent.loadCustomers) {
                $scope.$parent.loadCustomers();
            }
        }, function(err) {
            ToastService.error('Failed to save feedback');
        });
    };

    $scope.goToFeedbackForCheck = function(order, check, $event) {
        if ($event) $event.stopPropagation();
        $scope.openEditFollowupModal(order, check, $event);
    };

}]);
