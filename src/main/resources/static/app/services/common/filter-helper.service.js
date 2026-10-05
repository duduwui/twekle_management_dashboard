/**
 * Common Utility Service: Filtering & Searching Helpers
 * Provides pure helper functions for date ranges, multi-word search matching, and table pagination.
 */
app.factory('FilterHelperService', [function() {
    
    /**
     * Converts time filter presets to minutes
     */
    function toDurationMinutes(preset) {
        if (!preset) return 0;
        var val = parseInt(preset.durationValue, 10) || 0;
        var unit = (preset.durationUnit || 'HOURS').toUpperCase();
        if (unit.indexOf('MIN') === 0) return val;
        if (unit.indexOf('HOUR') === 0) return val * 60;
        if (unit.indexOf('DAY') === 0) return val * 24 * 60;
        if (unit.indexOf('WEEK') === 0) return val * 7 * 24 * 60;
        if (unit.indexOf('MONTH') === 0) return val * 30 * 24 * 60;
        return val * 60;
    }

    /**
     * Converts preset to days
     */
    function getPresetTargetDays(preset) {
        if (!preset) return 0;
        var val = parseFloat(preset.durationValue) || 1;
        var unit = (preset.durationUnit || 'HOURS').toUpperCase();
        if (unit === 'HOURS') return val / 24;
        if (unit === 'DAYS') return val;
        if (unit === 'WEEKS') return val * 7;
        if (unit === 'MONTHS') return val * 30;
        return val;
    }

    /**
     * Checks if a date falls within fromStr and toStr
     */
    function isWithinDateRange(itemDateStr, fromStr, toStr) {
        if (!fromStr && !toStr) return true;
        if (!itemDateStr) return true;
        var itemDate = (itemDateStr instanceof Date) ? itemDateStr.getTime() : new Date(itemDateStr).getTime();
        if (isNaN(itemDate)) return true;
        if (fromStr) {
            var fromDate = (fromStr instanceof Date) ? new Date(fromStr.getTime()) : new Date(fromStr);
            fromDate.setHours(0, 0, 0, 0);
            if (itemDate < fromDate.getTime()) return false;
        }
        if (toStr) {
            var toDate = (toStr instanceof Date) ? new Date(toStr.getTime()) : new Date(toStr);
            toDate.setHours(23, 59, 59, 999);
            if (itemDate > toDate.getTime()) return false;
        }
        return true;
    }

    /**
     * Matches haystack text against needle search terms
     */
    function searchMatch(haystack, needle) {
        if (!needle || !needle.trim()) return true;
        if (!haystack) return false;
        var rawHaystack = ('' + haystack).toLowerCase();
        var normHaystack = rawHaystack.replace(/[_.\-\/]/g, ' ');
        var normNeedle = ('' + needle).toLowerCase().trim().replace(/[_.\-\/]/g, ' ');
        if (!normNeedle) return true;
        
        if (normHaystack.indexOf(normNeedle) !== -1 || rawHaystack.indexOf(('' + needle).toLowerCase().trim()) !== -1) {
            return true;
        }

        var terms = normNeedle.split(/\s+/).filter(Boolean);
        if (terms.length > 0) {
            return terms.every(function(term) {
                return normHaystack.indexOf(term) !== -1 || rawHaystack.indexOf(term) !== -1;
            });
        }
        return false;
    }

    /**
     * General DataTable filtering, sorting, and pagination
     */
    function filterTable(items, filterState, dtState) {
        if (!items || !items.length) {
            if (dtState) dtState.totalItems = 0;
            return [];
        }

        var filtered = items.filter(function(item) {
            if (filterState && filterState.search) {
                var searchStr = JSON.stringify(item);
                if (!searchMatch(searchStr, filterState.search)) return false;
            }
            if (filterState && (filterState.dateFrom || filterState.dateTo)) {
                var dateVal = item.createdAt || item.orderDate || item.lastOrderDate || item.updatedAt || item.createdDate || item.date;
                if (!isWithinDateRange(dateVal, filterState.dateFrom, filterState.dateTo)) return false;
            }
            if (filterState && filterState.followupStatus && filterState.followupStatus !== 'ALL') {
                if (filterState.followupStatus === 'ALERT' && item.followupStatus !== 'ALERT') return false;
                if (filterState.followupStatus === 'IDLE' && item.followupStatus !== 'IDLE') return false;
                if (filterState.followupStatus === 'DONE' && (item.followupStatus !== 'DONE' && !item.allFollowupsCompleted)) return false;
            }
            return true;
        });

        if (dtState) {
            dtState.totalItems = filtered.length;
            if (dtState.sortField) {
                filtered.sort(function(a, b) {
                    var valA = a[dtState.sortField];
                    var valB = b[dtState.sortField];
                    if (valA == null) return 1;
                    if (valB == null) return -1;
                    if (typeof valA === 'string') {
                        var res = valA.localeCompare(valB);
                        return dtState.sortReverse ? -res : res;
                    }
                    return dtState.sortReverse ? (valB - valA) : (valA - valB);
                });
            }

            var start = (dtState.currentPage - 1) * dtState.pageSize;
            return filtered.slice(start, start + dtState.pageSize);
        }

        return filtered;
    }

    /**
     * Returns a Date object set to the beginning of today (00:00:00.000)
     */
    function getToday() {
        var d = new Date();
        d.setHours(0, 0, 0, 0);
        return d;
    }

    /**
     * Returns a Date object set to tomorrow (00:00:00.000)
     */
    function getTomorrow() {
        var d = new Date();
        d.setDate(d.getDate() + 1);
        d.setHours(0, 0, 0, 0);
        return d;
    }

    /**
     * Calculates start and end Date objects for a given time filter preset
     */
    function getPresetDateRange(preset) {
        if (!preset) return { dateFrom: null, dateTo: null };
        var mins = toDurationMinutes(preset);
        if (mins <= 0) return { dateFrom: null, dateTo: null };
        var now = new Date();
        var from = new Date(now.getTime() - (mins * 60 * 1000));
        from.setHours(0, 0, 0, 0);
        var to = new Date();
        to.setHours(23, 59, 59, 999);
        return { dateFrom: from, dateTo: to };
    }

    return {
        toDurationMinutes: toDurationMinutes,
        getPresetTargetDays: getPresetTargetDays,
        getPresetDateRange: getPresetDateRange,
        isWithinDateRange: isWithinDateRange,
        searchMatch: searchMatch,
        filterTable: filterTable,
        getToday: getToday,
        getTomorrow: getTomorrow
    };
}]);
