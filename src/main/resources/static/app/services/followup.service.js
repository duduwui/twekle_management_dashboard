/**
 * Follow-up Service
 * Manages customer order follow-up checkpoints, milestones, and status toggles.
 * Endpoints: /internal/orders/{orderId}/followups, /internal/followups/{id}
 */
angular.module('tweklApp')
.factory('FollowupService', ['$http', function($http) {
    return {
        getFollowups: function(orderId) {
            return $http.get('/internal/orders/' + orderId + '/followups').catch(function() {
                return $http.get('/api/orders/' + orderId + '/followups');
            });
        },
        createFollowup: function(orderId, data) {
            return $http.post('/internal/orders/' + orderId + '/followups', data).catch(function() {
                return $http.post('/api/orders/' + orderId + '/followups', data);
            });
        },
        updateFollowup: function(id, data) {
            return $http.put('/internal/followups/' + id, data).catch(function() {
                return $http.put('/api/followups/' + id, data);
            });
        },
        toggleStatus: function(id) {
            return $http.patch('/internal/followups/' + id + '/status', {}).catch(function() {
                return $http.patch('/api/followups/' + id + '/status', {});
            });
        }
    };
}])
// Backward-compatibility aliases for micro-services
.factory('FollowupGetService', ['FollowupService', function(FollowupService) {
    return { execute: function(orderId) { return FollowupService.getFollowups(orderId); } };
}])
.factory('FollowupCreateService', ['FollowupService', function(FollowupService) {
    return { execute: function(orderId, data) { return FollowupService.createFollowup(orderId, data); } };
}])
.factory('FollowupUpdateService', ['FollowupService', function(FollowupService) {
    return { execute: function(id, data) { return FollowupService.updateFollowup(id, data); } };
}])
.factory('FollowupToggleStatusService', ['FollowupService', function(FollowupService) {
    return { execute: function(id) { return FollowupService.toggleStatus(id); } };
}]);
