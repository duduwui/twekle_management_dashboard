/**
 * Order Follow-up Internal Data Service
 */
app.factory('FollowupService', ['$http', function($http) {
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
}]);
