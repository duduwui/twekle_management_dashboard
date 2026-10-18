/**
 * ============================================================================
 * Follow-up & Customer Data Service (followup-service.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/followup/followup-service.js
 * Used by:  /WEB-INF/views/administration/followup/followups.html
 * 
 * Responsibilities:
 *   - HTTP client contract for customers, orders, checklist milestones, and stats.
 *   - FollowupService: Core API interface for follow-up endpoints.
 *   - CustomerService: Alias factory providing backward compatibility.
 * ============================================================================
 */

if (typeof angular !== 'undefined') {
    angular.module('tweklApp')
    .factory('FollowupService', ['$http', function($http) {
        var BASE = '/admin/followups';
        var CUSTOMERS = '/admin/customers';
        var ORDERS = '/admin/orders';

        return {
            // Customer directory & stats (Server-Side Paged & Instant)
            getPaged: function(params) {
                return $http.get(BASE + '/paged', { params: params });
            },
            getAll: function(timeFilter) {
                return $http.get(CUSTOMERS, { params: timeFilter ? { timeFilter: timeFilter } : {} });
            },
            getById: function(id) {
                return $http.get(CUSTOMERS + '/' + id);
            },
            getOrders: function(customerId) {
                return $http.get(CUSTOMERS + '/' + customerId + '/orders');
            },
            getStats: function() {
                return $http.get(CUSTOMERS + '/stats');
            },

            // Follow-up checklists & milestone updates
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
    }])
    .factory('CustomerService', ['FollowupService', function(FollowupService) {
        return FollowupService;
    }]);
}
