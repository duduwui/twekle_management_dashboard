/**
 * ============================================================================
 * Staff Administration - Data & Logic Engine (admin-module.js)
 * ============================================================================
 * 
 * Layer:    M-V-E Layer 1 (Module)
 * Location: src/main/resources/js/administration/admin/admin-module.js
 * 
 * Rules:
 *   - Pure calculations, string matching, and validation logic.
 *   - Strictly FORBIDDEN from querying or mutating the DOM.
 * ============================================================================
 */

const AdminModule = (function () {
    'use strict';

    /**
     * Determines if a text block contains the search query (case-insensitive).
     * 
     * @param {string} text - Content from table cells or data attributes.
     * @param {string} query - Typed search query.
     * @returns {boolean} True if matching or query is empty.
     */
    function matchesQuery(text, query) {
        if (!query) return true;
        if (!text) return false;
        return text.toLowerCase().indexOf(query.toLowerCase().trim()) > -1;
    }

    /**
     * Validates administrator account inputs before submission.
     * 
     * @param {string} username - Desired username.
     * @param {string} password - Desired password.
     * @returns {Object} { isValid: boolean, error: string|null }
     */
    function validateCredentials(username, password) {
        if (!username || username.trim().length < 3) {
            return { isValid: false, error: 'Username must be at least 3 characters long.' };
        }
        if (!password || password.length < 6) {
            return { isValid: false, error: 'Password must be at least 6 characters long.' };
        }
        return { isValid: true, error: null };
    }

    return {
        matchesQuery: matchesQuery,
        validateCredentials: validateCredentials
    };
})();
