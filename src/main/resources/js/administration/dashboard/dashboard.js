/**
 * ============================================================================
 * Dashboard Feature (dashboard.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/dashboard/dashboard.js
 * Used by:  /WEB-INF/views/administration/dashboard/dashboard.html
 * Route:    /admin/dashboard
 * 
 * Responsibilities:
 *   - Real-time ticking and humanized countdown formatting for priority checks.
 *   - Native HTML5 dialog modal controls (open, close, backdrop click).
 *   - Filtering customer registration rows within the registrations dialog.
 * ============================================================================
 */

(function () {
    'use strict';

    const pageLoadTime = Date.now();

    /**
     * Converts minute values into humanized countdown text strings.
     * 
     * @param {number} mins - Remaining or overdue minutes.
     * @returns {string} Human-friendly string (e.g., "15m left", "2h overdue").
     */
    function formatCountdown(mins) {
        if (mins >= 0) {
            if (mins < 60) return mins + 'm left';
            const hours = Math.floor(mins / 60);
            const remM = mins % 60;
            if (hours < 24) return remM > 0 ? (hours + 'h ' + remM + 'm left') : (hours + 'h left');
            const days = Math.floor(hours / 24);
            const remH = hours % 24;
            return remH > 0 ? (days + 'd ' + remH + 'h left') : (days + 'd left');
        } else {
            const overdue = Math.abs(mins);
            if (overdue < 60) return overdue + 'm overdue';
            const ovH = Math.floor(overdue / 60);
            const ovM = overdue % 60;
            if (ovH < 24) return ovM > 0 ? (ovH + 'h ' + ovM + 'm overdue') : (ovH + 'h overdue');
            const ovD = Math.floor(ovH / 24);
            const ovRH = ovH % 24;
            return ovRH > 0 ? (ovD + 'd ' + ovRH + 'h overdue') : (ovD + 'd overdue');
        }
    }

    /**
     * Ticks countdown tags on the priority attention table using current elapsed time.
     */
    function tickCountdowns() {
        const elapsedMins = Math.floor((Date.now() - pageLoadTime) / 60000);

        document.querySelectorAll('.live-countdown-tag[data-minutes-left]').forEach(function (el) {
            const initialMins = parseInt(el.getAttribute('data-minutes-left'), 10);
            if (isNaN(initialMins)) return;

            const currentMins = initialMins - elapsedMins;
            el.textContent = formatCountdown(currentMins);

            if (currentMins <= 0) {
                el.classList.add('tag-danger');
                el.classList.remove('tag-warning');
            } else {
                el.classList.add('tag-warning');
                el.classList.remove('tag-danger');
            }
        });
    }

    /**
     * Checks if a registration timestamp falls within a specific millisecond range.
     * 
     * @param {string} registeredDateStr - Date string from data-registered attribute.
     * @param {number} cutoffTimestamp - Epoch cutoff in milliseconds.
     * @returns {boolean} True if within cutoff or cutoff is zero/null.
     */
    function isWithinCutoff(registeredDateStr, cutoffTimestamp) {
        if (!cutoffTimestamp) return true;
        if (!registeredDateStr) return false;
        return Date.parse(registeredDateStr) > cutoffTimestamp;
    }

    /**
     * Filters customer registration rows inside the KPI details dialog modal.
     * 
     * @param {number} days - Number of days cutoff (1 for today, 7 for week, 30 for month, or 0 for all).
     */
    let currentFilterDays = 0;

    function renderFilteredRegistrations(days) {
        currentFilterDays = Number(days || 0);
        const cutoff = currentFilterDays ? Date.now() - (currentFilterDays * 86400000) : 0;
        const searchInput = document.getElementById('customerSearchInput');
        const query = searchInput ? (searchInput.value || '').toLowerCase().trim() : '';

        let visible = 0;

        document.querySelectorAll('#modalCustomerTable tbody tr, [data-registered]').forEach(function (row) {
            const isDateMatch = isWithinCutoff(row.dataset.registered, cutoff);
            const searchTarget = (row.dataset.search || row.textContent).toLowerCase();
            const isSearchMatch = !query || searchTarget.indexOf(query) !== -1;
            const isVisible = isDateMatch && isSearchMatch;

            row.hidden = !isVisible;
            if (isVisible) visible++;
        });

        const emptyEl = document.getElementById('registrationsEmpty');
        if (emptyEl) emptyEl.hidden = visible > 0;

        const countEl = document.getElementById('customerVisibleCount');
        if (countEl) countEl.textContent = `Showing ${visible} customer${visible === 1 ? '' : 's'}`;

        // Toggle active states on modal top cards
        document.querySelectorAll('.modal-stat-card[data-filter-days]').forEach(function (card) {
            card.classList.toggle('active', Number(card.dataset.filterDays) === currentFilterDays);
        });

        // Toggle active states on filter pills
        document.querySelectorAll('[data-registration-days]').forEach(function (btn) {
            btn.classList.toggle('active', Number(btn.dataset.registrationDays) === currentFilterDays);
            btn.classList.toggle('btn-primary', Number(btn.dataset.registrationDays) === currentFilterDays);
        });
    }

    window.selectRegistrationFilter = function(days) {
        renderFilteredRegistrations(days);
    };

    window.filterCustomerTableLive = function(searchVal) {
        renderFilteredRegistrations(currentFilterDays);
    };

    /**
     * Generic controller to show or hide native HTML5 <dialog> elements.
     * 
     * @param {string} dialogId - ID of dialog element.
     * @param {boolean} shouldOpen - True to open, false to close.
     * @param {number|null} triggerDays - Optional registration days filter.
     */
    function toggleNativeDialog(dialogId, shouldOpen, triggerDays) {
        const dialog = document.getElementById(dialogId);
        if (!dialog) return;

        if (shouldOpen) {
            if (triggerDays !== undefined && triggerDays !== null) {
                renderFilteredRegistrations(triggerDays);
            }
            typeof dialog.showModal === 'function' ? dialog.showModal() : dialog.setAttribute('open', '');
        } else {
            typeof dialog.close === 'function' ? dialog.close() : dialog.removeAttribute('open');
        }
    }

    function setupInteractionListeners() {
        document.addEventListener('click', function (event) {
            // 1. Handle Dialog Target Openers
            const opener = event.target.closest('[data-dialog]');
            if (opener) {
                const daysAttr = opener.dataset.days;
                toggleNativeDialog(
                    opener.dataset.dialog,
                    true,
                    daysAttr !== undefined ? Number(daysAttr) : null
                );
            }

            // 2. Handle Explicit Dialog Closers
            const closeBtn = event.target.closest('[data-close-dialog]');
            if (closeBtn) {
                const parentDialog = closeBtn.closest('dialog');
                if (parentDialog) toggleNativeDialog(parentDialog.id, false);
            }

            // 3. Handle Registration Days Filter Buttons
            const filterBtn = event.target.closest('[data-registration-days]');
            if (filterBtn) {
                renderFilteredRegistrations(Number(filterBtn.dataset.registrationDays));
            }

            // 4. Close On Dialog Backdrop Click
            if (event.target.tagName === 'DIALOG') {
                const rect = event.target.getBoundingClientRect();
                const clickedOutside = (
                    event.clientY < rect.top || event.clientY > rect.bottom ||
                    event.clientX < rect.left || event.clientX > rect.right
                );
                if (clickedOutside) toggleNativeDialog(event.target.id, false);
            }
        });
    }

    /**
     * Client-side pagination with sliding window for Priority Attention Table
     */
    function initPriorityPagination() {
        const table = document.getElementById('priorityAttentionTable');
        const footer = document.getElementById('priorityPaginationFooter');
        const info = document.getElementById('priorityPaginationInfo');
        const controls = document.getElementById('priorityPaginationControls');
        if (!table || !footer || !controls) return;

        const rows = Array.from(table.querySelectorAll('tbody tr'));
        const totalRows = rows.length;
        const pageSize = 10;
        const totalPages = Math.ceil(totalRows / pageSize) || 1;
        let currentPage = 1;

        if (totalRows <= pageSize) {
            footer.style.display = totalRows > 0 ? '' : 'none';
            if (info) info.textContent = `Showing 1 to ${totalRows} of ${totalRows} priority follow-ups`;
            controls.innerHTML = '';
            return;
        }

        function renderPage(page) {
            currentPage = Math.max(1, Math.min(page, totalPages));
            const start = (currentPage - 1) * pageSize;
            const end = start + pageSize;

            rows.forEach((row, idx) => {
                row.style.display = (idx >= start && idx < end) ? '' : 'none';
            });

            if (info) {
                info.textContent = `Showing ${start + 1} to ${Math.min(end, totalRows)} of ${totalRows} priority follow-ups`;
            }

            let pages = [];
            if (totalPages <= 7) {
                for (let i = 1; i <= totalPages; i++) pages.push(i);
            } else {
                pages.push(1);
                let s = Math.max(2, currentPage - 2);
                let e = Math.min(totalPages - 1, currentPage + 2);
                if (currentPage <= 4) {
                    s = 2;
                    e = Math.min(totalPages - 1, 5);
                } else if (currentPage >= totalPages - 3) {
                    s = Math.max(2, totalPages - 4);
                    e = totalPages - 1;
                }
                if (s > 2) pages.push('...');
                for (let i = s; i <= e; i++) pages.push(i);
                if (e < totalPages - 1) pages.push('...');
                if (totalPages > 1) pages.push(totalPages);
            }

            let html = `
                <button type="button" class="page-btn" ${currentPage === 1 ? 'disabled' : ''} data-p="1" title="First Page">«</button>
                <button type="button" class="page-btn" ${currentPage === 1 ? 'disabled' : ''} data-p="${currentPage - 1}">‹ Prev</button>
            `;

            pages.forEach(p => {
                if (p === '...') {
                    html += `<span class="page-btn page-ellipsis">...</span>`;
                } else {
                    html += `<button type="button" class="page-btn ${p === currentPage ? 'active' : ''}" data-p="${p}">${p}</button>`;
                }
            });

            html += `
                <button type="button" class="page-btn" ${currentPage === totalPages ? 'disabled' : ''} data-p="${currentPage + 1}">Next ›</button>
                <button type="button" class="page-btn" ${currentPage === totalPages ? 'disabled' : ''} data-p="${totalPages}" title="Last Page">»</button>
            `;

            controls.innerHTML = html;
        }

        controls.addEventListener('click', function(e) {
            const btn = e.target.closest('button[data-p]');
            if (btn && !btn.disabled) {
                const p = parseInt(btn.dataset.p, 10);
                if (!isNaN(p)) renderPage(p);
            }
        });

        renderPage(1);
    }

    /**
     * Periodically syncs live counters and progress bars from /admin/stats/summary
     */
    function updateDashboardStats() {
        fetch('/admin/stats/summary', { credentials: 'same-origin' })
            .then(function(res) {
                if (!res.ok) return null;
                return res.json();
            })
            .then(function(data) {
                if (!data) return;
                const elTotal = document.getElementById('kpi-customers-total');
                if (elTotal && data.totalCustomers != null) elTotal.textContent = Number(data.totalCustomers).toLocaleString();

                const elQueue = document.getElementById('kpi-followups-queue');
                if (elQueue && data.inQueueFollowups != null) elQueue.textContent = Number(data.inQueueFollowups).toLocaleString();

                const elOverdue = document.getElementById('kpi-followups-overdue');
                if (elOverdue && data.overdueFollowups != null) {
                    elOverdue.textContent = Number(data.overdueFollowups).toLocaleString();
                    if (data.overdueFollowups > 0) {
                        elOverdue.classList.add('kpi-danger');
                        elOverdue.classList.remove('kpi-primary');
                    } else {
                        elOverdue.classList.add('kpi-primary');
                        elOverdue.classList.remove('kpi-danger');
                    }
                }

                const elUnsat = document.getElementById('kpi-feedback-unsatisfied');
                if (elUnsat && data.unsatisfiedCount != null) {
                    elUnsat.textContent = Number(data.unsatisfiedCount).toLocaleString();
                    if (data.unsatisfiedCount > 0) {
                        elUnsat.classList.add('kpi-danger');
                        elUnsat.classList.remove('kpi-primary');
                    } else {
                        elUnsat.classList.add('kpi-primary');
                        elUnsat.classList.remove('kpi-danger');
                    }
                }

                // Sync modal stats if open
                const mToday = document.getElementById('modal-stat-today');
                if (mToday && data.customersToday != null) mToday.textContent = Number(data.customersToday).toLocaleString();

                const mWeek = document.getElementById('modal-stat-week');
                if (mWeek && data.customersThisWeek != null) mWeek.textContent = Number(data.customersThisWeek).toLocaleString();

                const mMonth = document.getElementById('modal-stat-month');
                if (mMonth && data.customersThisMonth != null) mMonth.textContent = Number(data.customersThisMonth).toLocaleString();

                const mTotal = document.getElementById('modal-stat-total');
                if (mTotal && data.totalCustomers != null) mTotal.textContent = Number(data.totalCustomers).toLocaleString();

                const btnProgress = document.getElementById('kpi-progress-btn');
                if (btnProgress && data.completionPercentage != null) {
                    btnProgress.textContent = data.completionPercentage + '% Completed';
                }

                const barComp = document.getElementById('visual-bar-completed');
                if (barComp && data.completedPercentage != null) barComp.style.width = data.completedPercentage + '%';

                const barOver = document.getElementById('visual-bar-overdue');
                if (barOver) {
                    var ovFollowups = Number(data.overdueFollowups || 0);
                    if (ovFollowups > 0) {
                        var ovPct = Number(data.overduePercentage || 0);
                        if (ovPct < 2) ovPct = 2;
                        barOver.style.width = ovPct + '%';
                        barOver.style.minWidth = '8px';
                        barOver.style.display = 'block';
                    } else {
                        barOver.style.width = '0%';
                        barOver.style.minWidth = '0px';
                        barOver.style.display = 'none';
                    }
                }

                const barQueue = document.getElementById('visual-bar-inqueue');
                if (barQueue && data.inQueuePercentage != null) barQueue.style.width = data.inQueuePercentage + '%';

                const legComp = document.getElementById('kpi-legend-completed');
                if (legComp && data.completedFollowups != null) legComp.textContent = Number(data.completedFollowups).toLocaleString();

                const legOver = document.getElementById('kpi-legend-overdue');
                if (legOver && data.overdueFollowups != null) legOver.textContent = Number(data.overdueFollowups).toLocaleString();

                const legQueue = document.getElementById('kpi-legend-inqueue');
                if (legQueue && data.inQueueFollowups != null) legQueue.textContent = Number(data.inQueueFollowups).toLocaleString();

                const legTotal = document.getElementById('kpi-legend-total');
                if (legTotal && data.totalFollowups != null) legTotal.textContent = Number(data.totalFollowups).toLocaleString();

                // Dynamic Charts Sync
                if (data.satisfactionChart) {
                    updateSatisfactionChart(data.satisfactionChart);
                } else if (data.satisfactionRate != null) {
                    updateSatisfactionChart(data);
                }

                if (data.milestoneChart) {
                    updateMilestoneChart(data.milestoneChart);
                }
            })
            .catch(function() {
                // Ignore transient network errors
            });
    }

    function updateSatisfactionChart(data) {
        if (!data) return;
        const total = Number(data.totalRated || 0);
        const satCount = Number(data.satisfiedCount || 0);
        const neuCount = Number(data.neutralCount || 0);
        const unsatCount = Number(data.unsatisfiedCount || 0);

        const satRate = data.satisfactionRate != null ? Number(data.satisfactionRate) : (total > 0 ? (satCount / total) * 100 : 100);
        const avgRating = data.averageRating != null ? Number(data.averageRating) : 5.0;

        const elAvg = document.getElementById('sat-avg-rating');
        if (elAvg) elAvg.textContent = avgRating.toFixed(1);

        const elCenter = document.getElementById('donut-center-pct');
        if (elCenter) elCenter.textContent = Math.round(satRate) + '%';

        const elSatCount = document.getElementById('sat-count-satisfied');
        if (elSatCount) elSatCount.textContent = satCount.toLocaleString();

        const elSatPct = document.getElementById('sat-pct-satisfied');
        if (elSatPct) elSatPct.textContent = total > 0 ? ((satCount / total) * 100).toFixed(1) + '%' : '100%';

        const elNeuCount = document.getElementById('sat-count-neutral');
        if (elNeuCount) elNeuCount.textContent = neuCount.toLocaleString();

        const elNeuPct = document.getElementById('sat-pct-neutral');
        if (elNeuPct) elNeuPct.textContent = total > 0 ? ((neuCount / total) * 100).toFixed(1) + '%' : '0%';

        const elUnsatCount = document.getElementById('sat-count-unsatisfied');
        if (elUnsatCount) elUnsatCount.textContent = unsatCount.toLocaleString();

        const elUnsatPct = document.getElementById('sat-pct-unsatisfied');
        if (elUnsatPct) elUnsatPct.textContent = total > 0 ? ((unsatCount / total) * 100).toFixed(1) + '%' : '0%';

        const elTotal = document.getElementById('sat-total-rated');
        if (elTotal) elTotal.textContent = total.toLocaleString();

        // SVG Donut Circumference: 2 * Math.PI * 48 = 301.59
        const C = 301.59;
        const satPct = total > 0 ? satCount / total : 1.0;
        const neuPct = total > 0 ? neuCount / total : 0;
        const unsatPct = total > 0 ? unsatCount / total : 0;

        const satArc = document.getElementById('donut-arc-satisfied');
        if (satArc) {
            const len = Math.max(0, satPct * C);
            satArc.setAttribute('stroke-dasharray', len + ' ' + (C - len));
            satArc.setAttribute('stroke-dashoffset', '0');
        }

        const neuArc = document.getElementById('donut-arc-neutral');
        if (neuArc) {
            const len = Math.max(0, neuPct * C);
            const offset = -(satPct * C);
            neuArc.setAttribute('stroke-dasharray', len + ' ' + (C - len));
            neuArc.setAttribute('stroke-dashoffset', offset.toString());
        }

        const unsatArc = document.getElementById('donut-arc-unsatisfied');
        if (unsatArc) {
            const len = Math.max(0, unsatPct * C);
            const offset = -((satPct + neuPct) * C);
            unsatArc.setAttribute('stroke-dasharray', len + ' ' + (C - len));
            unsatArc.setAttribute('stroke-dashoffset', offset.toString());
        }
    }

    function updateMilestoneChart(milestones) {
        if (!milestones) return;
        const badge = document.getElementById('milestones-active-badge');
        if (badge) badge.textContent = milestones.length + ' Active';

        const container = document.getElementById('milestones-chart-list');
        if (!container) return;

        if (milestones.length === 0) {
            container.innerHTML = '<div class="milestone-empty" style="text-align: center; color: var(--text-muted); padding: 20px 0; font-size: 0.85rem;">No active time filters found.</div>';
            return;
        }

        let html = '';
        milestones.forEach(function(m) {
            const compRate = Number(m.completionRate || 0);
            const overdueRate = Number(m.overdueRate || 0);
            const pendingRate = Number(m.pendingRate || 0);
            const total = Number(m.total || 0);
            const completed = Number(m.completed || 0);
            const overdue = Number(m.overdue || 0);
            const pending = Number(m.pending || 0);

            const badgeClass = compRate >= 80 ? 'milestone-rate-high' : (compRate >= 40 ? 'milestone-rate-mid' : 'milestone-rate-low');

            html += `
                <div class="milestone-bar-item" data-preset-id="${m.id}">
                    <div class="milestone-meta-row">
                        <span class="milestone-name-tag">
                            <span class="text-muted">#${m.displayOrder || 1}</span>
                            <span>${escapeHtml(m.name || '')}</span>
                        </span>
                        <span class="milestone-rate-badge ${badgeClass}">
                            ${compRate}% Done
                        </span>
                    </div>
                    <div class="milestone-bar-row">
                        <div class="milestone-seg-completed" style="width: ${compRate}%"></div>
                        <div class="milestone-seg-overdue" style="width: ${overdueRate}%"></div>
                        <div class="milestone-seg-pending" style="width: ${pendingRate}%"></div>
                    </div>
                    <div class="milestone-counts-row">
                        <span><strong>${completed}</strong> completed</span>
                        ${overdue > 0 ? `<span style="color: #EF4444;"><strong>${overdue}</strong> overdue</span>` : ''}
                        <span><strong>${pending}</strong> in queue</span>
                        <span><strong>${total}</strong> total</span>
                    </div>
                </div>
            `;
        });
        container.innerHTML = html;
    }

    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function init() {
        setupInteractionListeners();
        tickCountdowns();
        initPriorityPagination();
        updateDashboardStats();

        // Tick countdowns every 60 seconds
        setInterval(function () {
            tickCountdowns();
        }, 60000);

        // Auto-refresh stats counters every 6 seconds
        setInterval(function () {
            updateDashboardStats();
        }, 6000);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
