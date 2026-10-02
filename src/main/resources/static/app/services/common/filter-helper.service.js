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
        var itemDate = new Date(itemDateStr).getTime();
        if (isNaN(itemDate)) return true;
        if (fromStr) {
            var fromDate = new Date(fromStr).getTime();
            if (itemDate < fromDate) return false;
        }
        if (toStr) {
            var toDate = new Date(toStr).getTime() + (24 * 60 * 60 * 1000 - 1);
            if (itemDate > toDate) return false;
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
                var dateVal = item.createdAt || item.updatedAt || item.createdDate;
                if (!isWithinDateRange(dateVal, filterState.dateFrom, filterState.dateTo)) return false;
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

    return {
        toDurationMinutes: toDurationMinutes,
        getPresetTargetDays: getPresetTargetDays,
        isWithinDateRange: isWithinDateRange,
        searchMatch: searchMatch,
        filterTable: filterTable
    };
}]);
