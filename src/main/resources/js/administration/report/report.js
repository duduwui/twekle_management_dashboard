/**
 * ============================================================================
 * Reports & Analytics Engine (report.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/report/report.js
 * Used by:  /WEB-INF/views/administration/report/reports.html
 * Route:    /admin/reports
 * 
 * Purpose:
 *   Handles analytics, calculations, and reporting tab transitions across:
 *   1. Milestones: Turnaround times, delay rates, and target day performance.
 *   2. Customer Satisfaction: Rating breakdowns (1-5 stars), positive review %,
 *      sentiment filters, and CSV data export.
 *   3. Product Insights: High/low rated products and customer feedback remarks.
 *   4. Follow-up Summary: Aggregated counts, overdue rates, and staff milestones.
 * ============================================================================
 */
angular.module('tweklApp')
.factory('ReportService', ['$http', function($http) {
    var BASE = '/admin/reports';

    function get(period, params) {
        var config = { params: params || {} };
        if (period) config.params.period = period;
        return $http.get(BASE, config);
    }

    return {
        getReports: function(period, params) { return get(period, params); },
        get: function(period, params) { return get(period, params); }
    };
}])
.controller('ReportController', [
    '$scope', '$http', 'ReportService', 'ToastService', 'FilterHelperService',
function($scope, $http, ReportService, ToastService, FilterHelperService) {

    $scope.loading = false;
    $scope.reportData = {
        milestones: [],
        satisfaction: { breakdown: [], totalRated: 0, positiveRate: 0, recentNotes: [] },
        products: [],
        customers: [],
        followups: [],
        loyaltyTiers: { tiers: [], repeatCustomerCount: 0, repeatCustomerRate: 0, oneTimeCustomerCount: 0, oneTimeRate: 0 }
    };

    // Default Date From: Today, Date To: Tomorrow / Next Day
    $scope.reportFilters = {
        dateFrom: FilterHelperService.getToday(),
        dateTo: FilterHelperService.getTomorrow(),
        satisfactionFilter: 'all',
        milestoneFilter: 'all',
        customerTypeFilter: 'all',
        customerTierFilter: 'all',
        customerStatusFilter: 'all',
        customerCityFilter: 'all',
        searchNotes: '',
        searchProducts: '',
        searchCustomers: '',
        searchFollowups: ''
    };

    $scope.showReportFilterModal = false;
    $scope.pendingReportFilters = {
        satisfaction: 'all',
        preset: null
    };

    var urlParams = new URLSearchParams(window.location.search);
    var tabParam = urlParams.get('tab');
    $scope.reportSubTab = tabParam || window.SERVER_REPORT_SUBTAB || ($scope.$parent && $scope.$parent.reportSubTab) || 'milestones';
    if ($scope.reportSubTab === 'customers' || $scope.reportSubTab === 'financial') {
        $scope.reportSubTab = ($scope.reportSubTab === 'customers') ? 'followups' : 'milestones';
    }

    $scope.setReportSubTab = function(subTab) {
        $scope.reportSubTab = subTab;
        if ($scope.$parent && $scope.$parent.setReportSubTab) {
            $scope.$parent.setReportSubTab(subTab);
        } else if ($scope.$parent) {
            $scope.$parent.reportSubTab = subTab;
        }
        if (window.history && window.history.replaceState) {
            var newUrl = window.location.pathname + '?tab=' + encodeURIComponent(subTab);
            window.history.replaceState(null, '', newUrl);
        }
    };

    $scope.$on('reportSubTab:changed', function(event, subTab) {
        $scope.reportSubTab = (subTab === 'customers') ? 'followups' : subTab;
    });

    // Pagination states for each report table (40 rows per page default)
    $scope.dtMilestones = { currentPage: 1, pageSize: 40, totalItems: 0, totalPages: 1 };
    $scope.dtSatisfaction = { currentPage: 1, pageSize: 40, totalItems: 0, totalPages: 1 };
    $scope.dtProducts = { currentPage: 1, pageSize: 40, totalItems: 0, totalPages: 1 };
    $scope.dtCustomers = { currentPage: 1, pageSize: 40, totalItems: 0, totalPages: 1 };

    $scope.getCheckImages = function(item) {
        if (!item) return [];
        var url = item.imageUrl || item.photoUrl;
        if (!url || typeof url !== 'string' || !url.trim()) return [];
        url = url.trim();
        if (url.startsWith('[') && url.endsWith(']')) {
            try {
                var parsed = JSON.parse(url);
                if (Array.isArray(parsed)) {
                    return parsed.filter(function(x) { return typeof x === 'string' && x.trim().length > 0; });
                }
            } catch(e) {}
        }
        return [url];
    };

    $scope.previewImage = function(url) {
        if (!url) return;
        if ($scope.$parent && $scope.$parent.previewImage) {
            $scope.$parent.previewImage(url);
        } else {
            window.open(url, '_blank');
        }
    };

    function toDateParam(d) {
        if (!d) return null;
        if (typeof d === 'string') return d.substring(0, 10);
        if (d instanceof Date) {
            var y = d.getFullYear();
            var m = ('0' + (d.getMonth() + 1)).slice(-2);
            var day = ('0' + d.getDate()).slice(-2);
            return y + '-' + m + '-' + day;
        }
        return null;
    }

    /**
     * Loads report data from backend
     */
    $scope.loadReportData = function() {
        $scope.loading = true;
        var period = $scope.selectedReportPreset ? ($scope.selectedReportPreset.presetKey || $scope.selectedReportPreset.name) : 'all';
        var params = {};
        if ($scope.reportFilters.dateFrom) {
            params.dateFrom = toDateParam($scope.reportFilters.dateFrom);
        }
        if ($scope.reportFilters.dateTo) {
            params.dateTo = toDateParam($scope.reportFilters.dateTo);
        }

        ReportService.getReports(period, params).then(function(res) {
            $scope.loading = false;
            var raw = res.data;
            var data = (raw && raw.data) ? raw.data : (raw || {});
            $scope.reportData.summary = data.summary || {};
            $scope.reportData.loyaltyTiers = data.loyaltyTiers || { tiers: [], repeatCustomerCount: 0, repeatCustomerRate: 0, oneTimeCustomerCount: 0, oneTimeRate: 0 };
            $scope.reportData.milestones = data.milestones || [];
            $scope.reportData.satisfaction = data.satisfaction || { breakdown: [], recentNotes: [] };
            $scope.reportData.products = data.products || [];
            $scope.reportData.customers = data.customers || [];
            $scope.reportData.followups = $scope.reportData.customers;

            // Reset to page 1 on filter or data reload
            $scope.dtMilestones.currentPage = 1;
            $scope.dtSatisfaction.currentPage = 1;
            $scope.dtProducts.currentPage = 1;
            $scope.dtCustomers.currentPage = 1;
        }, function(err) {
            $scope.loading = false;
            ToastService.error('Failed to load report analytics');
        });
    };

    $scope.applyReportDateFilter = function() {
        $scope.loadReportData();
    };

    $scope.isReportFiltered = function() {
        return !!($scope.selectedReportPreset ||
            ($scope.reportFilters.satisfactionFilter && $scope.reportFilters.satisfactionFilter !== 'all') ||
            ($scope.reportFilters.milestoneFilter && $scope.reportFilters.milestoneFilter !== 'all') ||
            $scope.reportFilters.dateFrom || $scope.reportFilters.dateTo);
    };

    // =========================================================
    // REPORT POPUP MODAL FILTERS
    // =========================================================
    $scope.openReportFilterModal = function() {
        var pf = {
            preset: $scope.selectedReportPreset,
            satisfaction: $scope.reportFilters.satisfactionFilter || 'all',
            customerType: $scope.reportFilters.customerTypeFilter || 'all',
            customerTier: $scope.reportFilters.customerTierFilter || 'all',
            customerStatus: $scope.reportFilters.customerStatusFilter || 'all',
            customerCity: $scope.reportFilters.customerCityFilter || 'all'
        };
        $scope.pendingReportFilters = pf;
        if ($scope.modalState) {
            $scope.modalState.reportFilter = true;
        }
        $scope.showReportFilterModal = true;
        $scope.$emit('modal:openReportFilter', pf);
        if ($scope.$parent && $scope.$parent.openReportFilterModal) {
            $scope.$parent.openReportFilterModal(pf);
        }
    };

    $scope.closeReportFilterModal = function() {
        if ($scope.modalState) {
            $scope.modalState.reportFilter = false;
        }
        $scope.showReportFilterModal = false;
        if ($scope.$parent && $scope.$parent.closeReportFilterModal) {
            $scope.$parent.closeReportFilterModal();
        }
    };

    $scope.resetReportFilterModal = function() {
        $scope.pendingReportFilters = {
            preset: null,
            satisfaction: 'all',
            customerType: 'all',
            customerTier: 'all',
            customerStatus: 'all',
            customerCity: 'all'
        };
        $scope.selectedReportPreset = null;
        $scope.reportFilters.satisfactionFilter = 'all';
        $scope.reportFilters.milestoneFilter = 'all';
        $scope.reportFilters.customerTypeFilter = 'all';
        $scope.reportFilters.customerTierFilter = 'all';
        $scope.reportFilters.customerStatusFilter = 'all';
        $scope.reportFilters.customerCityFilter = 'all';
        $scope.reportFilters.searchCustomers = '';
        $scope.reportFilters.dateFrom = null;
        $scope.reportFilters.dateTo = null;
        $scope.dtCustomers.currentPage = 1;
        $scope.closeReportFilterModal();
        $scope.loadReportData();
        if ($scope.$parent && $scope.$parent.resetReportFilterModal) {
            $scope.$parent.resetReportFilterModal();
        }
    };

    $scope.selectPendingReportPreset = function(preset) {
        if (!$scope.pendingReportFilters) $scope.pendingReportFilters = {};
        $scope.pendingReportFilters.preset = preset;
        if ($scope.$parent && $scope.$parent.selectPendingReportPreset) {
            $scope.$parent.selectPendingReportPreset(preset);
        }
    };

    $scope.isPendingReportPresetSelected = function(preset) {
        var pf = $scope.pendingReportFilters || ($scope.$parent && $scope.$parent.pendingReportFilters);
        if (!pf) return !preset;
        if (!preset && !pf.preset) return true;
        if (!preset || !pf.preset) return false;
        return preset.id === pf.preset.id;
    };

    $scope.confirmApplyReportFilterModal = function() {
        var pf = $scope.pendingReportFilters || ($scope.$parent && $scope.$parent.pendingReportFilters) || {};
        $scope.selectedReportPreset = pf.preset;
        $scope.reportFilters.satisfactionFilter = pf.satisfaction || 'all';
        $scope.reportFilters.customerTypeFilter = pf.customerType || 'all';
        $scope.reportFilters.customerTierFilter = pf.customerTier || 'all';
        $scope.reportFilters.customerStatusFilter = pf.customerStatus || 'all';
        $scope.reportFilters.customerCityFilter = pf.customerCity || 'all';
        $scope.dtCustomers.currentPage = 1;
        $scope.closeReportFilterModal();
        $scope.loadReportData();
        if ($scope.$parent && $scope.$parent.confirmApplyReportFilterModal) {
            $scope.$parent.confirmApplyReportFilterModal();
        }
    };

    $scope.selectReportPreset = function(preset) {
        $scope.selectedReportPreset = preset;
        $scope.$emit('modal:syncReportPreset', preset);
        $scope.loadReportData();
    };

    $scope.clearSatisfactionFilter = function() {
        $scope.reportFilters.satisfactionFilter = 'all';
        $scope.$emit('modal:syncSatisfactionFilter', 'all');
    };

    // =========================================================
    // FILTERED & PAGED SUB-TAB DATA HELPERS (40 rows per page)
    // =========================================================
    $scope.getFilteredReportNotes = function() {
        var notes = ($scope.reportData && $scope.reportData.satisfaction && $scope.reportData.satisfaction.recentNotes) ? $scope.reportData.satisfaction.recentNotes : [];
        var search = ($scope.reportFilters.searchNotes || '').trim().toLowerCase();
        var satFilter = $scope.reportFilters.satisfactionFilter;
        return notes.filter(function(n) {
            if (satFilter && satFilter !== 'all' && n.satisfaction !== satFilter) return false;
            if (search) {
                var cName = (n.customerName || '').toLowerCase();
                var cPhone = (n.customerPhone || '').toLowerCase();
                var ordNum = (n.orderNumber || '').toLowerCase();
                var noteText = (n.note || '').toLowerCase();
                var pName = (n.presetName || '').toLowerCase();
                if (cName.indexOf(search) === -1 &&
                    cPhone.indexOf(search) === -1 &&
                    ordNum.indexOf(search) === -1 &&
                    noteText.indexOf(search) === -1 &&
                    pName.indexOf(search) === -1) {
                    return false;
                }
            }
            return true;
        });
    };

    $scope.getPagedReportNotes = function() {
        var items = $scope.getFilteredReportNotes();
        $scope.dtSatisfaction.totalItems = items.length;
        $scope.dtSatisfaction.totalPages = Math.max(1, Math.ceil(items.length / $scope.dtSatisfaction.pageSize));
        if ($scope.dtSatisfaction.currentPage > $scope.dtSatisfaction.totalPages) $scope.dtSatisfaction.currentPage = $scope.dtSatisfaction.totalPages;
        var start = ($scope.dtSatisfaction.currentPage - 1) * $scope.dtSatisfaction.pageSize;
        return items.slice(start, start + $scope.dtSatisfaction.pageSize);
    };

    $scope.getFilteredReportProducts = function() {
        var prods = $scope.reportData.products || [];
        var search = ($scope.reportFilters.searchProducts || '').trim().toLowerCase();
        if (!search) return prods;
        return prods.filter(function(p) {
            return (p.productName || '').toLowerCase().indexOf(search) !== -1;
        });
    };

    $scope.getPagedReportProducts = function() {
        var items = $scope.getFilteredReportProducts();
        $scope.dtProducts.totalItems = items.length;
        $scope.dtProducts.totalPages = Math.max(1, Math.ceil(items.length / $scope.dtProducts.pageSize));
        if ($scope.dtProducts.currentPage > $scope.dtProducts.totalPages) $scope.dtProducts.currentPage = $scope.dtProducts.totalPages;
        var start = ($scope.dtProducts.currentPage - 1) * $scope.dtProducts.pageSize;
        return items.slice(start, start + $scope.dtProducts.pageSize);
    };

    $scope.customerSort = {
        field: 'totalOrders',
        reverse: true
    };

    $scope.sortByCustomer = function(field) {
        if ($scope.customerSort.field === field) {
            $scope.customerSort.reverse = !$scope.customerSort.reverse;
        } else {
            $scope.customerSort.field = field;
            $scope.customerSort.reverse = (field === 'totalOrders' || field === 'pendingFollowups');
        }
    };

    $scope.getCustomerSortIndicator = function(field) {
        if ($scope.customerSort.field !== field) return '';
        return $scope.customerSort.reverse ? '▼' : '▲';
    };

    $scope.clearCustomerFilters = function() {
        $scope.reportFilters.customerTypeFilter = 'all';
        $scope.reportFilters.customerTierFilter = 'all';
        $scope.reportFilters.customerStatusFilter = 'all';
        $scope.reportFilters.customerCityFilter = 'all';
        $scope.reportFilters.searchCustomers = '';
        $scope.dtCustomers.currentPage = 1;
    };

    $scope.hasCustomerFiltersActive = function() {
        return ($scope.reportFilters.customerTypeFilter && $scope.reportFilters.customerTypeFilter !== 'all') ||
               ($scope.reportFilters.customerTierFilter && $scope.reportFilters.customerTierFilter !== 'all') ||
               ($scope.reportFilters.customerStatusFilter && $scope.reportFilters.customerStatusFilter !== 'all') ||
               ($scope.reportFilters.customerCityFilter && $scope.reportFilters.customerCityFilter !== 'all') ||
               !!$scope.reportFilters.searchCustomers;
    };

    $scope.getFilteredReportCustomers = function() {
        var custs = $scope.reportData.followups || $scope.reportData.customers || [];
        var search = ($scope.reportFilters.searchCustomers || $scope.reportFilters.searchFollowups || '').trim().toLowerCase();
        var typeFilter = $scope.reportFilters.customerTypeFilter || 'all';
        var tierFilter = $scope.reportFilters.customerTierFilter || 'all';
        var statusFilter = $scope.reportFilters.customerStatusFilter || 'all';
        var cityFilter = $scope.reportFilters.customerCityFilter || 'all';

        var filtered = custs.filter(function(c) {
            // 1. Customer Type filter (Organization vs Individual)
            if (typeFilter !== 'all') {
                if (typeFilter === 'ORGANIZATION' || typeFilter === 'MARKET') {
                    if (c.customerType !== 'ORGANIZATION' && c.customerType !== 'MARKET') return false;
                } else if (typeFilter === 'INDIVIDUAL' || typeFilter === 'PERSON') {
                    if (c.customerType !== 'INDIVIDUAL' && c.customerType !== 'PERSON') return false;
                }
            }

            // 2. Customer Tier filter
            if (tierFilter !== 'all') {
                var tk = c.tierKey || '';
                var to = c.totalOrders || 0;
                if ((tierFilter === 'HIGH_VOLUME' || tierFilter === 'VIP') && (to < 5 && tk.indexOf('HIGH_VOLUME') === -1 && tk.indexOf('VIP') === -1)) return false;
                if ((tierFilter === 'REGULAR' || tierFilter === 'FREQUENT') && ((to < 3 || to >= 5) && tk.indexOf('REGULAR') === -1 && tk.indexOf('FREQUENT') === -1)) return false;
                if (tierFilter === 'RETURNING' && (to !== 2 && tk.indexOf('RETURNING') === -1)) return false;
                if ((tierFilter === 'FIRST_TIME' || tierFilter === 'ONE_TIME') && (to !== 1 && tk.indexOf('FIRST_TIME') === -1 && tk.indexOf('ONE_TIME') === -1)) return false;
            }

            // 3. Follow-up queue status filter
            if (statusFilter !== 'all') {
                if (statusFilter === 'DONE' && !c.allFollowupsCompleted && c.followupStatus !== 'DONE') return false;
                if (statusFilter === 'ALERT' && c.followupStatus !== 'ALERT') return false;
                if (statusFilter === 'IDLE' && (c.followupStatus === 'ALERT' || c.allFollowupsCompleted)) return false;
            }

            // 4. City filter
            if (cityFilter !== 'all' && (c.city || '').toLowerCase() !== cityFilter.toLowerCase()) return false;

            // 5. Search text filter
            if (search) {
                var name = (c.name || '').toLowerCase();
                var phone = (c.phoneNumber || '').toLowerCase();
                var city = (c.city || '').toLowerCase();
                var tier = (c.tier || '').toLowerCase();
                var typeLbl = (c.customerTypeLabel || '').toLowerCase();
                var email = (c.email || '').toLowerCase();
                if (name.indexOf(search) === -1 &&
                    phone.indexOf(search) === -1 &&
                    city.indexOf(search) === -1 &&
                    tier.indexOf(search) === -1 &&
                    typeLbl.indexOf(search) === -1 &&
                    email.indexOf(search) === -1) {
                    return false;
                }
            }
            return true;
        });

        // Apply sorting
        if ($scope.customerSort && $scope.customerSort.field) {
            var field = $scope.customerSort.field;
            var rev = $scope.customerSort.reverse;
            filtered.sort(function(a, b) {
                var valA = a[field];
                var valB = b[field];
                if (valA == null) return 1;
                if (valB == null) return -1;
                if (typeof valA === 'string') {
                    var cmp = valA.localeCompare(valB);
                    return rev ? -cmp : cmp;
                }
                return rev ? (valB - valA) : (valA - valB);
            });
        }

        return filtered;
    };
    $scope.getFilteredReportFollowups = $scope.getFilteredReportCustomers;

    $scope.getPagedReportCustomers = function() {
        var items = $scope.getFilteredReportCustomers();
        $scope.dtCustomers.totalItems = items.length;
        $scope.dtCustomers.totalPages = Math.max(1, Math.ceil(items.length / $scope.dtCustomers.pageSize));
        if ($scope.dtCustomers.currentPage > $scope.dtCustomers.totalPages) $scope.dtCustomers.currentPage = $scope.dtCustomers.totalPages;
        var start = ($scope.dtCustomers.currentPage - 1) * $scope.dtCustomers.pageSize;
        return items.slice(start, start + $scope.dtCustomers.pageSize);
    };
    $scope.getPagedReportFollowups = $scope.getPagedReportCustomers;

    // Pagination navigation methods
    $scope.setReportPage = function(dt, page) {
        if (!dt || page < 1 || page > dt.totalPages) return;
        dt.currentPage = page;
    };

    $scope.getReportPageNumbers = function(dt) {
        if (!dt) return [];
        var current = dt.currentPage || 1;
        var total = dt.totalPages || 1;
        var pages = [];
        if (total <= 7) {
            for (var i = 1; i <= total; i++) pages.push(i);
        } else {
            pages.push(1);
            if (current > 3) pages.push('...');
            var start = Math.max(2, current - 1);
            var end = Math.min(total - 1, current + 1);
            for (var j = start; j <= end; j++) pages.push(j);
            if (current < total - 2) pages.push('...');
            pages.push(total);
        }
        return pages;
    };

    function exportToExcel(filename, sheetName, headers, rows) {
        var baseName = filename.replace(/\.xlsx$/i, '').replace(/\.csv$/i, '');
        var xlsxName = baseName + '.xlsx';

        if (window.XLSX && XLSX.utils && XLSX.writeFile) {
            try {
                var wb = XLSX.utils.book_new();
                var aoa = [headers].concat(rows);
                var ws = XLSX.utils.aoa_to_sheet(aoa);

                // Auto-fit column widths
                var colWidths = headers.map(function(h, colIdx) {
                    var maxLen = String(h).length;
                    rows.forEach(function(r) {
                        var cellVal = r[colIdx] != null ? String(r[colIdx]) : '';
                        if (cellVal.length > maxLen) maxLen = Math.min(cellVal.length, 45);
                    });
                    return { wch: Math.max(maxLen + 3, 12) };
                });
                ws['!cols'] = colWidths;

                XLSX.utils.book_append_sheet(wb, ws, sheetName || 'Report');
                XLSX.writeFile(wb, xlsxName);
                return true;
            } catch (e) {
                console.warn('XLSX generation failed, falling back to CSV', e);
            }
        }

        // Fallback to UTF-8 BOM CSV if XLSX is unavailable
        var csvContent = '\uFEFF';
        var escapeField = function(val) {
            if (val == null) return '""';
            var str = String(val).replace(/"/g, '""');
            return '"' + str + '"';
        };
        csvContent += headers.map(escapeField).join(',') + '\r\n';
        rows.forEach(function(row) {
            csvContent += row.map(escapeField).join(',') + '\r\n';
        });

        var blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
        if (window.navigator && window.navigator.msSaveBlob) {
            window.navigator.msSaveBlob(blob, baseName + '.csv');
        } else {
            var link = document.createElement('a');
            var url = URL.createObjectURL(blob);
            link.setAttribute('href', url);
            link.setAttribute('download', baseName + '.csv');
            link.style.visibility = 'hidden';
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            URL.revokeObjectURL(url);
        }
        return false;
    }

    $scope.exportReportExcel = function() {
        var subTab = $scope.reportSubTab || 'customers';
        var nowStr = new Date().toISOString().slice(0, 10);

        if (subTab === 'followups' || subTab === 'customers') {
            var custs = $scope.getFilteredReportCustomers();
            if (!custs || custs.length === 0) {
                ToastService.warning('No customer data to export.');
                return;
            }
            var headers = ['ID', 'Customer Name', 'Type', 'Phone Number', 'City', 'Tier', 'Total Orders', 'Pending Checks'];
            var rows = custs.map(function(c) {
                return [
                    c.id,
                    c.name || '',
                    c.customerTypeLabel || (c.customerType === 'ORGANIZATION' ? 'Organization' : 'Individual'),
                    c.phoneNumber || '',
                    c.city || '',
                    c.tier || '',
                    c.totalOrders != null ? c.totalOrders : 0,
                    c.pendingFollowups != null ? c.pendingFollowups : 0
                ];
            });
            exportToExcel('Twekl_Customer_Report_' + nowStr, 'Customers', headers, rows);
            ToastService.success('Exported ' + rows.length + ' customer records to Excel.');
        } else if (subTab === 'milestones') {
            var miles = ($scope.reportData && $scope.reportData.milestones) ? $scope.reportData.milestones : [];
            if (miles.length === 0) {
                ToastService.warning('No milestone data to export.');
                return;
            }
            var headers = ['Milestone Name', 'Time Preset', 'Total Orders', 'Completed Checks', 'Completion Rate (%)', 'Avg Turnaround (Hrs)', 'Target (Days)'];
            var rows = miles.map(function(m) {
                return [
                    m.milestoneName || '',
                    m.presetName || '',
                    m.totalOrders != null ? m.totalOrders : 0,
                    m.completedChecks != null ? m.completedChecks : 0,
                    m.completionRate != null ? m.completionRate : 0,
                    m.avgTurnaroundHours != null ? m.avgTurnaroundHours : 0,
                    m.targetDays != null ? m.targetDays : 0
                ];
            });
            exportToExcel('Twekl_Milestones_Report_' + nowStr, 'Milestones', headers, rows);
            ToastService.success('Exported milestone report to Excel.');
        } else if (subTab === 'satisfaction') {
            var notes = $scope.getFilteredReportNotes();
            if (!notes || notes.length === 0) {
                ToastService.warning('No satisfaction records to export.');
                return;
            }
            var headers = ['Customer Name', 'Phone Number', 'Order Number', 'Milestone', 'Rating', 'Sentiment', 'Feedback Note'];
            var rows = notes.map(function(n) {
                return [
                    n.customerName || '',
                    n.customerPhone || '',
                    n.orderNumber || '',
                    n.presetName || '',
                    n.rating != null ? n.rating : '',
                    n.satisfaction || '',
                    n.note || ''
                ];
            });
            exportToExcel('Twekl_Satisfaction_Report_' + nowStr, 'Satisfaction', headers, rows);
            ToastService.success('Exported ' + rows.length + ' feedback notes to Excel.');
        } else if (subTab === 'products') {
            var prods = $scope.getFilteredReportProducts();
            if (!prods || prods.length === 0) {
                ToastService.warning('No product records to export.');
                return;
            }
            var headers = ['Product Name', 'Category', 'Total Units Sold', 'Avg Rating', 'Total Reviews', 'Low Rating Count'];
            var rows = prods.map(function(p) {
                return [
                    p.productName || '',
                    p.category || '',
                    p.totalQuantity != null ? p.totalQuantity : 0,
                    p.avgRating != null ? p.avgRating : 0,
                    p.reviewCount != null ? p.reviewCount : 0,
                    p.lowRatingCount != null ? p.lowRatingCount : 0
                ];
            });
            exportToExcel('Twekl_Products_Report_' + nowStr, 'Products', headers, rows);
            ToastService.success('Exported ' + rows.length + ' product records to Excel.');
        }
    };
    $scope.exportReportCsv = $scope.exportReportExcel;

    // Auto-reload when time filter presets change or login occurs
    $scope.$on('timePresets:changed', function() {
        $scope.loadReportData();
    });

    $scope.$on('report:reload', function() {
        $scope.loadReportData();
    });

    // Listen for report filter changes from LayoutController
    $scope.$on('filter:reportFiltersChanged', function(event, data) {
        if (data) {
            $scope.selectedReportPreset = data.preset;
            $scope.reportFilters.satisfactionFilter = data.satisfaction || 'all';
            $scope.reportFilters.customerTypeFilter = data.customerType || 'all';
            $scope.reportFilters.customerTierFilter = data.customerTier || 'all';
            $scope.reportFilters.customerCityFilter = data.customerCity || 'all';
        }
        $scope.loadReportData();
    });

    // Hydrate immediately from server-side rendered data if present
    if (window.SERVER_REPORT_DATA) {
        var srd = window.SERVER_REPORT_DATA;
        $scope.reportData.summary = srd.summary || {};
        $scope.reportData.loyaltyTiers = srd.loyaltyTiers || { tiers: [], repeatCustomerCount: 0, repeatCustomerRate: 0, oneTimeCustomerCount: 0, oneTimeRate: 0 };
        $scope.reportData.milestones = srd.milestones || [];
        $scope.reportData.satisfaction = srd.satisfaction || { breakdown: [], recentNotes: [] };
        $scope.reportData.products = srd.products || [];
        $scope.reportData.customers = srd.customers || [];
        $scope.reportData.followups = $scope.reportData.customers;
    }

    // Initial data comes from the page model. No second request or REST fallback.
}]);
