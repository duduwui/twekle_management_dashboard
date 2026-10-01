/**
 * Centralized REST API Service
 * Encapsulates all backend HTTP interactions.
 */
app.factory('ApiService', ['$http', function($http) {
    return {
        // Authentication Endpoints
        auth: {
            getMe: function() { return $http.get('/api/auth/me'); },
            login: function(creds) { return $http.post('/api/auth/login', creds); },
            logout: function() { return $http.post('/api/auth/logout', {}); }
        },

        // Administrator Endpoints
        admins: {
            getAll: function() { return $http.get('/api/admins'); },
            getById: function(id) { return $http.get('/api/admins/' + id); },
            create: function(data) { return $http.post('/api/admins', data); },
            update: function(id, data) { return $http.put('/api/admins/' + id, data); },
            delete: function(id) { return $http.delete('/api/admins/' + id); }
        },

        // User & Permissions Endpoints
        users: {
            getAll: function() { return $http.get('/api/users'); },
            getById: function(id) { return $http.get('/api/users/' + id); },
            create: function(data) { return $http.post('/api/users', data); },
            update: function(id, data) { return $http.put('/api/users/' + id, data); },
            delete: function(id) { return $http.delete('/api/users/' + id); }
        },

        // Role Template Endpoints
        roles: {
            getAll: function() { return $http.get('/api/roles'); },
            getById: function(id) { return $http.get('/api/roles/' + id); },
            create: function(data) { return $http.post('/api/roles', data); },
            update: function(id, data) { return $http.put('/api/roles/' + id, data); },
            delete: function(id) { return $http.delete('/api/roles/' + id); }
        },

        // Customer Lifecycle Endpoints
        customers: {
            getAll: function(timeFilter) {
                var params = timeFilter ? { timeFilter: timeFilter } : {};
                return $http.get('/api/customers', { params: params });
            },
            getById: function(id) { return $http.get('/api/customers/' + id); },
            getOrders: function(customerId) { return $http.get('/api/customers/' + customerId + '/orders'); },
            getStats: function() { return $http.get('/api/customers/stats'); }
        },

        // Follow-up & Order Endpoints
        orders: {
            getFollowups: function(orderId) { return $http.get('/api/orders/' + orderId + '/followups'); },
            createFollowup: function(orderId, data) { return $http.post('/api/orders/' + orderId + '/followups', data); }
        },
        followups: {
            update: function(id, data) { return $http.put('/api/followups/' + id, data); },
            toggleStatus: function(id) { return $http.patch('/api/followups/' + id + '/status', {}); }
        },

        // Time Filters Endpoints
        timeFilters: {
            getAll: function() { return $http.get('/api/time-filters'); },
            create: function(data) { return $http.post('/api/time-filters', data); },
            toggle: function(id) { return $http.patch('/api/time-filters/' + id + '/toggle', {}); },
            delete: function(id) { return $http.delete('/api/time-filters/' + id); }
        },

        // Reporting Endpoints
        reports: {
            get: function(period, params) {
                var config = { params: params || {} };
                if (period) config.params.period = period;
                return $http.get('/api/reports', config);
            }
        }
    };
}]);
