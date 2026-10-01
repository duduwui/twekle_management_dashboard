/**
 * Filter and Search Matching Service
 * Contains date arithmetic, duration conversions, search tokenization, and preset matchers.
 */
app.factory('FilterService', [function() {

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
            var toDate = new Date(toStr).getTime() + (24 * 60 * 60 * 1000 - 1); // end of day
            if (itemDate > toDate) return false;
        }
        return true;
    }

    function searchMatch(haystack, needle) {
        if (!needle || !needle.trim()) return true;
        if (!haystack) return false;
        var rawHaystack = ('' + haystack).toLowerCase();
        var normHaystack = rawHaystack.replace(/[_.\-\/]/g, ' ');
        var normNeedle = ('' + needle).toLowerCase().trim().replace(/[_.\-\/]/g, ' ');
        if (!normNeedle) return true;
        
        // Exact substring match on normalized text
        if (normHaystack.indexOf(normNeedle) !== -1 || rawHaystack.indexOf(('' + needle).toLowerCase().trim()) !== -1) {
            return true;
        }

        // Tokenized match: every word in search query matches somewhere in haystack
        var terms = normNeedle.split(/\s+/).filter(Boolean);
        if (terms.length > 0) {
            return terms.every(function(term) {
                return normHaystack.indexOf(term) !== -1 || rawHaystack.indexOf(term) !== -1;
            });
        }
        return false;
    }

    function matchesPreset(itemDateStr, preset) {
        if (!preset) return true;
        if (!itemDateStr) return false;
        var itemTime = new Date(itemDateStr).getTime();
        if (isNaN(itemTime)) return true;
        
        var targetDays = getPresetTargetDays(preset);
        var totalHours = targetDays * 24;
        var pName = (preset.name || '').toLowerCase();
        var cutoff = Date.now() - (totalHours * 3600 * 1000);

        if (pName.includes('dormant') || pName.includes('30+') || pName.includes('older')) {
            return itemTime <= cutoff;
        }
        return itemTime >= cutoff;
    }

    function matchesCustomerPreset(c, preset) {
        if (!preset) return true;
        
        // Always calculate real days ago from last order date
        var daysAgo = null;
        var itemDateStr = c.lastOrderDate || c.createdAt;
        if (itemDateStr) {
            var itemTime = new Date(itemDateStr).getTime();
            if (!isNaN(itemTime)) {
                var diffMs = Date.now() - itemTime;
                daysAgo = Math.max(0, Math.floor(diffMs / (1000 * 3600 * 24)));
            }
        }
        if (daysAgo == null && c.daysSinceLastOrder != null && !isNaN(c.daysSinceLastOrder)) {
            daysAgo = c.daysSinceLastOrder;
        }
        if (daysAgo == null) daysAgo = 0;

        var targetDays = getPresetTargetDays(preset);
        var pName = (preset.name || '').toLowerCase();
        var unit = (preset.durationUnit || 'HOURS').toUpperCase();

        // 1. Check if customer has a pending checkpoint milestone matching this preset
        var cFollow = (c.nextPendingFollowup || '').toLowerCase();
        var milestoneMatches = false;
        if (cFollow && cFollow !== '-' && cFollow !== 'all done') {
            var cleanPreset = pName.replace(/last |ago|review|dormant|\+/g, '').trim();
            milestoneMatches = cFollow.includes(pName) || pName.includes(cFollow) ||
                               (cleanPreset && cFollow.includes(cleanPreset));
        }

        // 2. Dormant / older accounts (e.g. "30+ Days Dormant", "dormant")
        if (pName.includes('dormant') || pName.includes('30+') || pName.includes('older')) {
            return milestoneMatches || (daysAgo >= Math.floor(targetDays * 0.8));
        }

        // 3. Horizon filters (e.g. "last 24 hours", "last 7 days", "last 30 days", "last 2 months", "within...")
        if (pName.startsWith('last') || pName.startsWith('past') || pName.startsWith('within') || unit === 'HOURS' || targetDays <= 1) {
            var maxHorizonDays = Math.max(1, Math.ceil(targetDays));
            return milestoneMatches || (daysAgo <= maxHorizonDays);
        }

        // 4. Milestone Recency Bucket (e.g. "3 Days Ago", "7 Days Ago", "2 Weeks Ago", "60 Days Review")
        var minDays = Math.max(1, Math.round(targetDays * 0.6));
        var maxDays = Math.round(targetDays * 1.4) + 1;
        var recencyBucket = (daysAgo >= minDays && daysAgo <= maxDays);

        return milestoneMatches || recencyBucket || (daysAgo <= Math.ceil(targetDays));
    }

    return {
        toDurationMinutes: toDurationMinutes,
        getPresetTargetDays: getPresetTargetDays,
        isWithinDateRange: isWithinDateRange,
        searchMatch: searchMatch,
        matchesPreset: matchesPreset,
        matchesCustomerPreset: matchesCustomerPreset
    };
}]);
