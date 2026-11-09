/**
 * Customer Registrations & Log Directory Controller.
 * Handles client-side multi-tier filtering (Timeframe, Status, City, Orders, Custom Dates),
 * live multi-field search, column sorting, pagination, and Excel export.
 */
(function() {
    'use strict';

    const filterCriteria = {
        days: 0,
        status: 'ALL',
        city: 'ALL',
        minOrders: 0,
        dateFrom: '',
        dateTo: ''
    };

    let currentPage = 1;
    const pageSize = 15;
    let filteredRows = [];
    let sortColumn = 'index';
    let sortDirection = 'asc';

    function init() {
        // Read initial days from URL or data attribute
        const urlParams = new URLSearchParams(window.location.search);
        const daysParam = urlParams.get('days');
        if (daysParam !== null && !isNaN(daysParam)) {
            filterCriteria.days = Number(daysParam);
        } else {
            const container = document.getElementById('customersPageContainer');
            if (container && container.dataset.initialDays) {
                filterCriteria.days = Number(container.dataset.initialDays);
            }
        }

        populateCityOptions();
        setupEventListeners();
        updateSortHeaders();
        applyFilters(1);
    }

    function populateCityOptions() {
        const cityContainer = document.getElementById('criteriaCityOptions');
        if (!cityContainer) return;

        const allRows = Array.from(document.querySelectorAll('#customerLogTable tbody tr.customer-row'));
        const citySet = new Set();
        allRows.forEach(function(row) {
            const c = (row.dataset.city || '').trim();
            if (c && c !== '-') citySet.add(c);
        });

        // Default Iraqi cities
        ['Baghdad', 'Erbil', 'Basra', 'Sulaymaniyah', 'Duhok', 'Najaf', 'Karbala', 'Kirkuk', 'Mosul'].forEach(function(c) {
            citySet.add(c);
        });

        const sortedCities = Array.from(citySet).sort();
        let html = '<label style="display: flex; align-items: center; gap: 8px; font-size: 0.85rem; font-weight: 600; cursor: pointer; color: #1E293B;">' +
                   '<input type="radio" name="criteriaCity" value="ALL" ' + (filterCriteria.city === 'ALL' ? 'checked' : '') + ' style="width: 16px; height: 16px; accent-color: #0F172A; cursor: pointer;">' +
                   '<span>All Cities</span></label>';

        sortedCities.forEach(function(city) {
            const isChecked = filterCriteria.city.toLowerCase() === city.toLowerCase() ? 'checked' : '';
            html += '<label style="display: flex; align-items: center; gap: 8px; font-size: 0.85rem; font-weight: 600; cursor: pointer; color: #1E293B;">' +
                    '<input type="radio" name="criteriaCity" value="' + city + '" ' + isChecked + ' style="width: 16px; height: 16px; accent-color: #0F172A; cursor: pointer;">' +
                    '<span>' + city + '</span></label>';
        });

        cityContainer.innerHTML = html;
    }

    function getRowValue(row, col) {
        switch (col) {
            case 'index':
                return Number(row.dataset.index) || 0;
            case 'name':
                return (row.dataset.name || '').toLowerCase();
            case 'phone':
                return (row.dataset.phone || '').toLowerCase();
            case 'city':
                return (row.dataset.city || '').toLowerCase();
            case 'date':
            case 'time':
                const d = row.dataset.registered || row.dataset.date;
                return d ? Date.parse(d) || 0 : 0;
            case 'orders':
                return Number(row.dataset.orders) || 0;
            case 'status':
                return (row.dataset.status || '').toLowerCase();
            default:
                return '';
        }
    }

    function applyFilters(page) {
        currentPage = page || 1;
        const searchInput = document.getElementById('customerSearchInput');
        const query = searchInput ? (searchInput.value || '').toLowerCase().trim() : '';

        // Calculate date timestamps
        let fromTs = 0;
        let toTs = 0;
        if (filterCriteria.dateFrom) {
            fromTs = new Date(filterCriteria.dateFrom + 'T00:00:00').getTime();
        }
        if (filterCriteria.dateTo) {
            toTs = new Date(filterCriteria.dateTo + 'T23:59:59').getTime();
        }

        let cutoffTs = 0;
        if (!filterCriteria.dateFrom && !filterCriteria.dateTo && filterCriteria.days > 0) {
            cutoffTs = Date.now() - (filterCriteria.days * 86400000);
        }

        const allRows = Array.from(document.querySelectorAll('#customerLogTable tbody tr.customer-row'));
        filteredRows = allRows.filter(function(row) {
            const regDateStr = row.dataset.registered || row.dataset.date;
            const regTs = regDateStr ? Date.parse(regDateStr) : 0;

            // Date filtering
            if (fromTs > 0 && regTs < fromTs) return false;
            if (toTs > 0 && regTs > toTs) return false;
            if (cutoffTs > 0 && regTs < cutoffTs) return false;

            // Status filtering
            if (filterCriteria.status !== 'ALL') {
                const rowStatus = (row.dataset.status || '').toLowerCase();
                if (rowStatus !== filterCriteria.status.toLowerCase()) {
                    return false;
                }
            }

            // City filtering
            if (filterCriteria.city !== 'ALL') {
                const rowCity = (row.dataset.city || '').toLowerCase();
                if (rowCity !== filterCriteria.city.toLowerCase()) {
                    return false;
                }
            }

            // Orders count filtering
            if (filterCriteria.minOrders > 0) {
                const rowOrders = Number(row.dataset.orders || 0);
                if (rowOrders < filterCriteria.minOrders) {
                    return false;
                }
            }

            // Search query matching
            if (query) {
                const searchTarget = (row.dataset.search || row.textContent).toLowerCase();
                if (searchTarget.indexOf(query) === -1) {
                    return false;
                }
            }

            return true;
        });

        // Sort filtered rows based on current active sort column
        filteredRows.sort(function(a, b) {
            const valA = getRowValue(a, sortColumn);
            const valB = getRowValue(b, sortColumn);
            let cmp = 0;
            if (typeof valA === 'number' && typeof valB === 'number') {
                cmp = valA - valB;
            } else {
                cmp = String(valA).localeCompare(String(valB), undefined, { numeric: true, sensitivity: 'base' });
            }
            return sortDirection === 'asc' ? cmp : -cmp;
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

        // Show slice for current page and re-append in sorted order to tbody
        const tbody = document.querySelector('#customerLogTable tbody');
        for (let i = start; i < end; i++) {
            if (filteredRows[i]) {
                filteredRows[i].style.display = '';
                if (tbody) tbody.appendChild(filteredRows[i]);
            }
        }

        // Update counts and empty state
        const emptyState = document.getElementById('customersEmptyState');
        const tableContainer = document.getElementById('customersTableContainer');
        const countIndicator = document.getElementById('customerVisibleCount');

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

    function updateSortHeaders() {
        document.querySelectorAll('#customerLogTable thead th.sortable').forEach(function(th) {
            const col = th.dataset.sort;
            const icon = th.querySelector('.sort-icon');
            if (col === sortColumn) {
                th.classList.toggle('sort-asc', sortDirection === 'asc');
                th.classList.toggle('sort-desc', sortDirection === 'desc');
                if (icon) icon.textContent = sortDirection === 'asc' ? '▲' : '▼';
            } else {
                th.classList.remove('sort-asc', 'sort-desc');
                if (icon) icon.textContent = '↕';
            }
        });
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
        const hasCustomDates = Boolean(filterCriteria.dateFrom || filterCriteria.dateTo);

        // Update Timeframe buttons in toolbar (All Time, Today, Last 7 Days, Last 30 Days)
        document.querySelectorAll('.filter-left-controls button[data-filter-days]').forEach(function(btn) {
            const days = Number(btn.dataset.filterDays);
            const isActive = !hasCustomDates && days === filterCriteria.days;
            btn.classList.toggle('active', isActive);
            btn.classList.toggle('btn-primary', isActive);
            btn.classList.toggle('btn-ghost', !isActive);
        });

        // Update top KPI cards: pure neutral card styles, NO btn-primary, NO btn-ghost, NO green background!
        document.querySelectorAll('.comic-kpi-card[data-filter-days]').forEach(function(card) {
            const days = Number(card.dataset.filterDays);
            const isActive = !hasCustomDates && days === filterCriteria.days;
            card.classList.toggle('kpi-card-active', isActive);
            card.classList.remove('btn-primary', 'btn-ghost', 'active');
        });

        // Update active filter criteria tags and badge
        renderActiveCriteriaTags();
    }

    function renderActiveCriteriaTags() {
        const bar = document.getElementById('activeFilterTagsBar');
        const list = document.getElementById('activeFilterTagsList');
        const badge = document.getElementById('activeCriteriaBadge');
        if (!bar || !list) return;

        const tags = [];

        if (filterCriteria.status !== 'ALL') {
            tags.push({ key: 'status', label: 'Status: ' + filterCriteria.status });
        }
        if (filterCriteria.city !== 'ALL') {
            tags.push({ key: 'city', label: 'City: ' + filterCriteria.city });
        }
        if (filterCriteria.minOrders > 0) {
            tags.push({ key: 'orders', label: 'Orders: ' + filterCriteria.minOrders + '+' });
        }
        if (filterCriteria.dateFrom || filterCriteria.dateTo) {
            const rangeStr = (filterCriteria.dateFrom || 'Start') + ' to ' + (filterCriteria.dateTo || 'Today');
            tags.push({ key: 'date', label: 'Registered: ' + rangeStr });
        }

        if (badge) {
            if (tags.length > 0) {
                badge.textContent = tags.length;
                badge.style.display = 'inline-flex';
            } else {
                badge.style.display = 'none';
            }
        }

        if (tags.length === 0) {
            bar.style.display = 'none';
            list.innerHTML = '';
            return;
        }

        bar.style.display = 'flex';
        let html = '';
        tags.forEach(function(tag) {
            html += '<span class="comic-tag tag-primary active-preset-pill" style="cursor: pointer; display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; font-size: 0.78rem; font-weight: 700;" data-clear-criterion="' + tag.key + '" title="Click to clear filter">' +
                    '<span>' + tag.label + '</span> <strong style="font-size: 0.85rem; line-height: 1;">✕</strong></span>';
        });
        list.innerHTML = html;
    }

    function openCriteriaModal() {
        const overlay = document.getElementById('customerFilterModalOverlay');
        if (!overlay) return;

        // Synchronize modal controls with current filterCriteria
        const statusRadio = document.querySelector('input[name="criteriaStatus"][value="' + filterCriteria.status + '"]');
        if (statusRadio) statusRadio.checked = true;

        const cityRadio = document.querySelector('input[name="criteriaCity"][value="' + filterCriteria.city + '"]');
        if (cityRadio) {
            cityRadio.checked = true;
        } else {
            const allCityRadio = document.querySelector('input[name="criteriaCity"][value="ALL"]');
            if (allCityRadio) allCityRadio.checked = true;
        }

        overlay.style.display = 'flex';
    }

    function closeCriteriaModal() {
        const overlay = document.getElementById('customerFilterModalOverlay');
        if (overlay) overlay.style.display = 'none';
    }

    function openAddCustomerModal() {
        const overlay = document.getElementById('addCustomerModalOverlay');
        if (!overlay) return;
        const errAlert = document.getElementById('addCustomerErrorAlert');
        if (errAlert) errAlert.style.display = 'none';
        const nameInput = document.getElementById('custName');
        if (nameInput) nameInput.value = '';
        const phoneInput = document.getElementById('custPhone');
        if (phoneInput) phoneInput.value = '';
        const emailInput = document.getElementById('custEmail');
        if (emailInput) emailInput.value = '';
        const externalIdInput = document.getElementById('custExternalId');
        if (externalIdInput) externalIdInput.value = '';
        const orderNumberInput = document.getElementById('custOrderNumber');
        if (orderNumberInput) orderNumberInput.value = '';
        const orderAmountInput = document.getElementById('custOrderAmount');
        if (orderAmountInput) orderAmountInput.value = '150.0';
        const autoOrderCheckbox = document.getElementById('custAutoOrder');
        if (autoOrderCheckbox) autoOrderCheckbox.checked = true;
        const orderFields = document.getElementById('initialOrderFields');
        if (orderFields) orderFields.style.display = 'grid';

        overlay.style.display = 'flex';
        setTimeout(function() {
            if (nameInput) nameInput.focus();
        }, 50);
    }

    function closeAddCustomerModal() {
        const overlay = document.getElementById('addCustomerModalOverlay');
        if (overlay) overlay.style.display = 'none';
    }

    function applyCriteriaModal() {
        const selectedStatus = document.querySelector('input[name="criteriaStatus"]:checked');
        const selectedCity = document.querySelector('input[name="criteriaCity"]:checked');

        filterCriteria.status = selectedStatus ? selectedStatus.value : 'ALL';
        filterCriteria.city = selectedCity ? selectedCity.value : 'ALL';

        closeCriteriaModal();
        applyFilters(1);
    }

    function resetCriteriaModal() {
        const allStatusRadio = document.querySelector('input[name="criteriaStatus"][value="ALL"]');
        if (allStatusRadio) allStatusRadio.checked = true;

        const allCityRadio = document.querySelector('input[name="criteriaCity"][value="ALL"]');
        if (allCityRadio) allCityRadio.checked = true;
    }

    function exportCustomersToExcel() {
        const rowsToExport = filteredRows.length > 0 ? filteredRows : Array.from(document.querySelectorAll('#customerLogTable tbody tr.customer-row'));
        if (rowsToExport.length === 0) {
            alert('No customer records to export.');
            return;
        }

        const headers = ['ID', 'Customer Name', 'Registered Date', 'Registered Time', 'Orders', 'Follow-up Status'];
        const data = rowsToExport.map(function(row) {
            const tds = row.querySelectorAll('td');
            return [
                row.dataset.index || (tds[0] ? tds[0].textContent.trim() : ''),
                row.dataset.name || (tds[1] ? tds[1].textContent.trim() : ''),
                tds[2] ? tds[2].textContent.trim() : '',
                tds[3] ? tds[3].textContent.trim() : '',
                row.dataset.orders || (tds[4] ? tds[4].textContent.trim() : '0'),
                row.dataset.status || (tds[5] ? tds[5].textContent.trim() : '')
            ];
        });

        const nowStr = new Date().toISOString().slice(0, 10);
        const filename = 'Twekl_Customers_' + nowStr;

        if (window.XLSX && XLSX.utils && XLSX.writeFile) {
            try {
                const wb = XLSX.utils.book_new();
                const ws = XLSX.utils.aoa_to_sheet([headers].concat(data));
                ws['!cols'] = headers.map(function(h, idx) {
                    let maxLen = h.length;
                    data.forEach(function(r) {
                        const l = String(r[idx] || '').length;
                        if (l > maxLen) maxLen = Math.min(l, 40);
                    });
                    return { wch: Math.max(maxLen + 3, 12) };
                });
                XLSX.utils.book_append_sheet(wb, ws, 'Customers');
                XLSX.writeFile(wb, filename + '.xlsx');
                return;
            } catch (err) {
                console.warn('XLSX export failed, falling back to CSV', err);
            }
        }

        // CSV fallback
        let csvContent = 'data:text/csv;charset=utf-8,\uFEFF';
        csvContent += headers.map(function(h) { return '"' + h + '"'; }).join(',') + '\n';
        data.forEach(function(row) {
            csvContent += row.map(function(v) { return '"' + String(v).replace(/"/g, '""') + '"'; }).join(',') + '\n';
        });
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement('a');
        link.setAttribute('href', encodedUri);
        link.setAttribute('download', filename + '.csv');
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    function setupEventListeners() {
        // Table header click for sorting
        document.addEventListener('click', function(e) {
            const th = e.target.closest('#customerLogTable thead th.sortable');
            if (th) {
                const col = th.dataset.sort;
                if (!col) return;
                if (sortColumn === col) {
                    sortDirection = sortDirection === 'asc' ? 'desc' : 'asc';
                } else {
                    sortColumn = col;
                    sortDirection = 'asc';
                }
                updateSortHeaders();
                applyFilters(1);
                return;
            }

            // Open Add Customer Modal
            const openAddBtn = e.target.closest('#openAddCustomerModalBtn') || e.target.closest('.trigger-add-customer-btn');
            if (openAddBtn) {
                e.preventDefault();
                openAddCustomerModal();
                return;
            }

            // Close Add Customer Modal
            const closeAddBtn = e.target.closest('#closeAddCustomerModalBtn') || e.target.closest('#cancelAddCustomerBtn');
            if (closeAddBtn) {
                e.preventDefault();
                closeAddCustomerModal();
                return;
            }

            // Export Excel button
            const exportBtn = e.target.closest('#exportCustomersExcelBtn');
            if (exportBtn) {
                e.preventDefault();
                exportCustomersToExcel();
                return;
            }

            // Open Filter Modal Button
            const openFilterBtn = e.target.closest('#openCustomerFilterModalBtn');
            if (openFilterBtn) {
                e.preventDefault();
                openCriteriaModal();
                return;
            }

            // Close Filter Modal Button
            const closeFilterBtn = e.target.closest('#closeCustomerFilterModalBtn') || e.target.closest('#cancelCriteriaBtn');
            if (closeFilterBtn) {
                e.preventDefault();
                closeCriteriaModal();
                return;
            }

            // Apply Filter Modal Button
            const applyFilterBtn = e.target.closest('#applyCriteriaBtn');
            if (applyFilterBtn) {
                e.preventDefault();
                applyCriteriaModal();
                return;
            }

            // Reset Filter Modal Button
            const resetFilterBtn = e.target.closest('#resetCriteriaBtn');
            if (resetFilterBtn) {
                e.preventDefault();
                resetCriteriaModal();
                return;
            }

            // Clear single filter criterion tag pill
            const clearTag = e.target.closest('[data-clear-criterion]');
            if (clearTag) {
                e.preventDefault();
                const crit = clearTag.dataset.clearCriterion;
                if (crit === 'status') filterCriteria.status = 'ALL';
                if (crit === 'city') filterCriteria.city = 'ALL';
                if (crit === 'orders') filterCriteria.minOrders = 0;
                if (crit === 'date') {
                    filterCriteria.dateFrom = '';
                    filterCriteria.dateTo = '';
                }
                applyFilters(1);
                return;
            }

            // Clear All Filters Button
            const clearAllBtn = e.target.closest('#clearAllFiltersBtn');
            if (clearAllBtn) {
                e.preventDefault();
                filterCriteria.days = 0;
                filterCriteria.status = 'ALL';
                filterCriteria.city = 'ALL';
                filterCriteria.minOrders = 0;
                filterCriteria.dateFrom = '';
                filterCriteria.dateTo = '';
                applyFilters(1);
                return;
            }

            // Filter pills / KPI card click
            const pill = e.target.closest('[data-filter-days]');
            if (pill) {
                e.preventDefault();
                filterCriteria.days = Number(pill.dataset.filterDays);
                filterCriteria.dateFrom = '';
                filterCriteria.dateTo = '';
                applyFilters(1);
                return;
            }

            // Pagination button
            const pageBtn = e.target.closest('#paginationControls [data-page]');
            if (pageBtn) {
                e.preventDefault();
                const page = Number(pageBtn.dataset.page);
                if (!isNaN(page)) applyFilters(page);
                return;
            }

            // Clear search button
            const clearBtn = e.target.closest('#clearCustomerSearch');
            if (clearBtn) {
                const searchInput = document.getElementById('customerSearchInput');
                if (searchInput) {
                    searchInput.value = '';
                    clearBtn.style.display = 'none';
                    applyFilters(1);
                }
                return;
            }

            // Close modal when clicking on overlay backdrop
            const modalOverlay = document.getElementById('customerFilterModalOverlay');
            if (modalOverlay && e.target === modalOverlay) {
                closeCriteriaModal();
            }
            const addModalOverlay = document.getElementById('addCustomerModalOverlay');
            if (addModalOverlay && e.target === addModalOverlay) {
                closeAddCustomerModal();
            }
        });

        // Close modal on Escape key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape') {
                closeCriteriaModal();
                closeAddCustomerModal();
            }
        });

        // Toggle initial order fields
        const autoOrderCheckbox = document.getElementById('custAutoOrder');
        if (autoOrderCheckbox) {
            autoOrderCheckbox.addEventListener('change', function() {
                const fields = document.getElementById('initialOrderFields');
                if (fields) fields.style.display = this.checked ? 'grid' : 'none';
            });
        }

        // Add Customer Form validation
        const addForm = document.getElementById('addCustomerForm');
        if (addForm) {
            addForm.addEventListener('submit', function(e) {
                const name = (document.getElementById('custName').value || '').trim();
                const phone = (document.getElementById('custPhone').value || '').trim();
                const errAlert = document.getElementById('addCustomerErrorAlert');
                if (!name || !phone) {
                    e.preventDefault();
                    if (errAlert) {
                        errAlert.textContent = 'Please enter both customer name and phone number.';
                        errAlert.style.display = 'block';
                    }
                    return;
                }
                const saveBtn = document.getElementById('saveCustomerBtn');
                if (saveBtn) {
                    saveBtn.disabled = true;
                    saveBtn.textContent = 'Saving...';
                }
            });
        }

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
