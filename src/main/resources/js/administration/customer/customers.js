/**
 * Customer Registrations & Log Directory Controller.
 * Handles client-side multi-tier filtering (All, Today, 7D, 30D),
 * live multi-field search, pagination, and KPI card interactions.
 */
(function() {
    'use strict';

    let currentFilterDays = 0;
    let currentPage = 1;
    const pageSize = 15;
    let filteredRows = [];

    function init() {
        // Read initial days from URL or data attribute
        const urlParams = new URLSearchParams(window.location.search);
        const daysParam = urlParams.get('days');
        if (daysParam !== null && !isNaN(daysParam)) {
            currentFilterDays = Number(daysParam);
        } else {
            const container = document.getElementById('customersPageContainer');
            if (container && container.dataset.initialDays) {
                currentFilterDays = Number(container.dataset.initialDays);
            }
        }

        setupEventListeners();
        applyFilters(1);
    }

    function isWithinCutoff(registeredDateStr, cutoffTimestamp) {
        if (!cutoffTimestamp) return true;
        if (!registeredDateStr) return false;
        const ts = Date.parse(registeredDateStr);
        return !isNaN(ts) && ts > cutoffTimestamp;
    }

    function applyFilters(page) {
        currentPage = page || 1;
        const cutoff = currentFilterDays > 0 ? Date.now() - (currentFilterDays * 86400000) : 0;
        const searchInput = document.getElementById('customerSearchInput');
        const query = searchInput ? (searchInput.value || '').toLowerCase().trim() : '';

        const allRows = Array.from(document.querySelectorAll('#customerLogTable tbody tr.customer-row'));
        filteredRows = allRows.filter(function(row) {
            const isDateMatch = isWithinCutoff(row.dataset.registered, cutoff);
            const searchTarget = (row.dataset.search || row.textContent).toLowerCase();
            const isSearchMatch = !query || searchTarget.indexOf(query) !== -1;
            return isDateMatch && isSearchMatch;
        });

        // Hide all rows first
        allRows.forEach(function(row) {
            row.style.display = 'none';
        });

        const totalFiltered = filteredRows.length;
        const totalPages = Math.ceil(totalFiltered / pageSize) || 1;
        currentPage = Math.max(1, Math.min(currentPage, totalPages));

        const start = (currentPage - 1) * pageSize;
        const end = Math.min(start + pageSize, totalFiltered);

        // Show slice for current page
        for (let i = start; i < end; i++) {
            if (filteredRows[i]) {
                filteredRows[i].style.display = '';
            }
        }

        // Update counts and empty state
        const emptyState = document.getElementById('customersEmptyState');
        const tableContainer = document.getElementById('customersTableContainer');
        const countIndicator = document.getElementById('customerVisibleCount');
        const paginationContainer = document.getElementById('customersPagination');

        if (emptyState) emptyState.style.display = totalFiltered === 0 ? 'block' : 'none';
        if (tableContainer) tableContainer.style.display = totalFiltered === 0 ? 'none' : 'block';
        if (countIndicator) {
            if (totalFiltered === 0) {
                countIndicator.textContent = 'No matching customers found';
            } else {
                countIndicator.textContent = 'Showing ' + (start + 1) + ' to ' + end + ' of ' + totalFiltered + ' customer' + (totalFiltered === 1 ? '' : 's');
            }
        }

        // Render pagination controls
        renderPagination(totalPages);

        // Update active UI styles for buttons and KPI cards
        updateFilterUi();
    }

    function renderPagination(totalPages) {
        const controls = document.getElementById('paginationControls');
        const paginationWrapper = document.getElementById('customersPagination');
        if (!controls || !paginationWrapper) return;

        if (totalPages <= 1) {
            paginationWrapper.style.display = 'none';
            controls.innerHTML = '';
            return;
        }

        paginationWrapper.style.display = 'flex';
        let html = '';

        // Previous button
        html += '<button type="button" class="comic-btn-sm ' + (currentPage === 1 ? 'disabled' : '') + '" ' +
                (currentPage === 1 ? 'disabled' : 'data-page="' + (currentPage - 1) + '"') + '>« Prev</button>';

        // Pages
        for (let p = 1; p <= totalPages; p++) {
            if (p === 1 || p === totalPages || (p >= currentPage - 2 && p <= currentPage + 2)) {
                html += '<button type="button" class="comic-btn-sm ' + (p === currentPage ? 'btn-primary' : 'btn-ghost') + '" data-page="' + p + '">' + p + '</button>';
            } else if (p === currentPage - 3 || p === currentPage + 3) {
                html += '<span style="padding: 0 4px; color: #94A3B8; font-weight: 700;">...</span>';
            }
        }

        // Next button
        html += '<button type="button" class="comic-btn-sm ' + (currentPage === totalPages ? 'disabled' : '') + '" ' +
                (currentPage === totalPages ? 'disabled' : 'data-page="' + (currentPage + 1) + '"') + '>Next »</button>';

        controls.innerHTML = html;
    }

    function updateFilterUi() {
        // Update filter pills
        document.querySelectorAll('[data-filter-days]').forEach(function(el) {
            const days = Number(el.dataset.filterDays);
            const isActive = days === currentFilterDays;
            if (el.tagName === 'BUTTON') {
                el.classList.toggle('active', isActive);
                el.classList.toggle('btn-primary', isActive);
                el.classList.toggle('btn-ghost', !isActive);
            } else {
                el.classList.toggle('active-filter-card', isActive);
            }
        });
    }

    function setupEventListeners() {
        // Filter pills click
        document.addEventListener('click', function(e) {
            const pill = e.target.closest('[data-filter-days]');
            if (pill) {
                e.preventDefault();
                currentFilterDays = Number(pill.dataset.filterDays);
                applyFilters(1);
            }

            const pageBtn = e.target.closest('[data-page]');
            if (pageBtn) {
                e.preventDefault();
                const page = Number(pageBtn.dataset.page);
                if (!isNaN(page)) applyFilters(page);
            }

            const clearBtn = e.target.closest('#clearCustomerSearch');
            if (clearBtn) {
                const searchInput = document.getElementById('customerSearchInput');
                if (searchInput) {
                    searchInput.value = '';
                    clearBtn.style.display = 'none';
                    applyFilters(1);
                }
            }
        });

        // Search input live filtering
        const searchInput = document.getElementById('customerSearchInput');
        if (searchInput) {
            searchInput.addEventListener('input', function() {
                const clearBtn = document.getElementById('clearCustomerSearch');
                if (clearBtn) clearBtn.style.display = this.value ? 'inline-flex' : 'none';
                applyFilters(1);
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
