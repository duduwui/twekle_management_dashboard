/**
 * Follow-up Action Controller: Customer Feedback & Notes (Section 4.3)
 * Handles adding/updating notes, uploading photo attachments, and submitting reviews.
 */
angular.module('tweklApp')
.controller('CustomerFeedbackController', ['$scope', 'FollowupUpdateService', 'ToastService',
function($scope, FollowupUpdateService, ToastService) {

    $scope.noteModal = {
        check: null,
        notes: '',
        photoUrl: '',
        isOpen: false
    };

    /**
     * Open note and photo upload modal for a specific checkpoint
     */
    $scope.openNoteModal = function(check) {
        $scope.noteModal.check = check;
        $scope.noteModal.notes = check.notes || '';
        $scope.noteModal.photoUrl = check.photoUrl || '';
        $scope.noteModal.isOpen = true;
    };

    /**
     * Save note and photo changes to database
     */
    $scope.saveNoteModal = function() {
        if (!$scope.noteModal.check) return;

        var payload = {
            notes: $scope.noteModal.notes,
            photoUrl: $scope.noteModal.photoUrl
        };

        FollowupUpdateService.execute($scope.noteModal.check.id, payload).then(function(res) {
            $scope.noteModal.check.notes = $scope.noteModal.notes;
            $scope.noteModal.check.photoUrl = $scope.noteModal.photoUrl;
            ToastService.success('Notes and attachment updated');
            $scope.noteModal.isOpen = false;
        }, function(err) {
            ToastService.error('Failed to save notes');
        });
    };

    /**
     * Close note modal
     */
    $scope.closeNoteModal = function() {
        $scope.noteModal.isOpen = false;
    };
}]);
