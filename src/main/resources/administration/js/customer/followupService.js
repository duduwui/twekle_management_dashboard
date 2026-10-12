/**
 * Follow-up Service
 * Manages follow-up checkpoints, milestone toggles, and note/photo updates.
 */
angular.module('tweklApp')
.factory('FollowupService', ['$http', function($http) {
    var BASE = '/admin/followups';
    var ORDERS = '/admin/orders';
    return {
        getFollowups: function(orderId) {
            return $http.get(ORDERS + '/' + orderId + '/followups');
        },
        updateFollowup: function(id, data) {
            return $http.put(BASE + '/' + id, data);
        },
        toggleStatus: function(id) {
            return $http.patch(BASE + '/' + id + '/status', {});
        }
    };
}]);
