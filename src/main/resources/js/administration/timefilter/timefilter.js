/**
 * ============================================================================
 * Time Filter Presets Service (timefilter.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/timefilter/timefilter.js
 * Used by:  /WEB-INF/views/administration/timefilter/timeFilters.html & Filter Modals
 * Route:    /admin/timelines
 * 
 * Purpose:
 *   Handles Dynamic Time Filter presets management:
 *   1. TimeFilterService: API client for querying available presets (24 Hours, 7 Days, etc.).
 *   2. Provides client helpers for creating presets, toggling active status, and deletion.
 * ============================================================================
 */
if (typeof angular !== 'undefined' && angular.module) {
    try {
        angular.module('tweklApp')
        .factory('TimeFilterService', ['$http', function($http) {
            var BASE = '/admin/time-filters';

            function request(method, path, data) {
                return $http({ method: method, url: BASE + (path || ''), data: data });
            }

            return {
                getAll: function() { return request('GET'); },
                create: function(data) { return request('POST', '', data); },
                toggle: function(id) { return request('PATCH', '/' + id + '/toggle'); },
                delete: function(id) { return request('DELETE', '/' + id); }
            };
        }]);
    } catch(e) {}
}

function filterPresetTable() {
    var input = document.getElementById("presetSearchInput");
    if (!input) return;
    var filter = input.value.toLowerCase();
    var rows = document.querySelectorAll("#presetTable tbody tr.preset-row");
    rows.forEach(function(row) {
        var text = row.innerText.toLowerCase();
        row.style.display = text.indexOf(filter) > -1 ? "" : "none";
    });
}

/**
 * Interactive Live Toggle for Time Filter Preset Status (Active <-> Inactive)
 * Prevents full page reload and viewport jumping.
 */
async function toggleTimeFilterPresetLive(btn, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    if (!btn || btn.classList.contains('loading')) return;

    var presetId = btn.getAttribute('data-preset-id');
    if (!presetId) return;

    var wasActive = btn.getAttribute('data-active') === 'true';
    var willBeActive = !wasActive;

    // Optimistic UI toggle
    btn.classList.add('loading');
    btn.classList.toggle('active', willBeActive);
    btn.classList.toggle('inactive', !willBeActive);

    var label = btn.querySelector('.toggle-label');
    if (label) {
        label.textContent = willBeActive ? 'Active' : 'Inactive';
        label.classList.toggle('active', willBeActive);
        label.classList.toggle('inactive', !willBeActive);
    }
    btn.setAttribute('data-active', willBeActive ? 'true' : 'false');
    btn.setAttribute('title', willBeActive ? 'Active - Click to deactivate' : 'Inactive - Click to activate');

    try {
        var response = await fetch('/admin/time-filters/' + presetId + '/toggle', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        });
        if (!response.ok && response.status === 404) {
            // Fallback to legacy path if needed
            response = await fetch('/admin/timelines/' + presetId + '/toggle', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
        }
        var data = await response.json();
        if (!response.ok || !data.success) {
            throw new Error(data.message || 'Failed to update preset status');
        }

        var confirmedActive = (data.active !== undefined) ? data.active : willBeActive;
        btn.classList.toggle('active', confirmedActive);
        btn.classList.toggle('inactive', !confirmedActive);
        if (label) {
            label.textContent = confirmedActive ? 'Active' : 'Inactive';
        }
        btn.setAttribute('data-active', confirmedActive ? 'true' : 'false');
        btn.setAttribute('title', confirmedActive ? 'Active - Click to deactivate' : 'Inactive - Click to activate');

        if (typeof showStatusToast === 'function') {
            var msg = data.message || (confirmedActive ? 'Preset activated successfully' : 'Preset deactivated');
            var toastType = confirmedActive ? 'success' : 'error';
            showStatusToast(msg, toastType);
        }
    } catch (err) {
        // Rollback on failure
        btn.classList.toggle('active', wasActive);
        btn.classList.toggle('inactive', !wasActive);
        if (label) {
            label.textContent = wasActive ? 'Active' : 'Inactive';
        }
        btn.setAttribute('data-active', wasActive ? 'true' : 'false');
        btn.setAttribute('title', wasActive ? 'Active - Click to deactivate' : 'Inactive - Click to activate');

        if (typeof showStatusToast === 'function') {
            showStatusToast(err.message || 'Failed to update preset status', 'error');
        }
    } finally {
        btn.classList.remove('loading');
    }
}
window.toggleTimeFilterPresetLive = toggleTimeFilterPresetLive;

/**
 * Interactive Live Delete for Time Filter Presets
 * Smoothly removes row and updates counter without page reload or scroll jumping.
 */
async function deleteTimeFilterPresetLive(btn, event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    if (!btn || btn.classList.contains('loading')) return;

    var presetId = btn.getAttribute('data-preset-id');
    var label = btn.getAttribute('data-label') || 'this preset';
    if (!presetId) return;

    if (!confirm('Are you sure you want to delete preset rule "' + label + '"?')) {
        return;
    }

    var row = btn.closest('tr.preset-row') || btn.closest('tr');
    btn.disabled = true;
    btn.classList.add('loading');

    try {
        var response = await fetch('/admin/time-filters/' + presetId + '/delete', {
            method: 'POST',
            headers: {
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        });
        if (!response.ok && response.status === 404) {
            response = await fetch('/admin/timelines/' + presetId + '/delete', {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
        }
        var data = await response.json();
        if (!response.ok || !data.success) {
            throw new Error(data.message || 'Failed to delete preset rule');
        }

        // Animate row out
        if (row) {
            row.style.transition = 'all 0.3s ease';
            row.style.opacity = '0';
            row.style.transform = 'translateX(25px)';
            setTimeout(function() {
                row.remove();
                // Update total counters on the page
                var countElements = document.querySelectorAll('.comic-tag strong, .datatable-info strong');
                countElements.forEach(function(el) {
                    var cur = parseInt(el.textContent, 10);
                    if (!isNaN(cur) && cur > 0) el.textContent = cur - 1;
                });
                var remaining = document.querySelectorAll('#presetTable tbody tr.preset-row');
                if (remaining.length === 0) {
                    var tbody = document.querySelector('#presetTable tbody');
                    if (tbody) {
                        tbody.innerHTML = '<tr><td colspan="5" style="text-align: center; padding: 24px; color: var(--text-muted); font-weight: 800;">No custom filter rules configured yet.</td></tr>';
                    }
                }
            }, 300);
        }

        if (typeof showStatusToast === 'function') {
            showStatusToast(data.message || 'Time filter preset deleted successfully.', 'success');
        }
    } catch (err) {
        btn.disabled = false;
        btn.classList.remove('loading');
        if (typeof showStatusToast === 'function') {
            showStatusToast(err.message || 'Failed to delete preset rule', 'error');
        }
    }
}
window.deleteTimeFilterPresetLive = deleteTimeFilterPresetLive;
