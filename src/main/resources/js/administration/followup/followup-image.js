/**
 * ============================================================================
 * Follow-up Image Processing Helper (followup-image.js)
 * ============================================================================
 * 
 * Location: src/main/resources/js/administration/followup/followup-image.js
 * Used by:  Customer inspection checkpoints, photo attachments, and modal uploads
 * 
 * Responsibilities:
 *   - Client-side image validation.
 *   - Proportional Canvas 2D image downscaling (1200x1200 max bounds).
 *   - JPEG compression before payload submission.
 * ============================================================================
 */

var FollowupImageHelper = (function () {
    'use strict';

    var DEFAULT_MAX_WIDTH = 1200;
    var DEFAULT_MAX_HEIGHT = 1200;
    var DEFAULT_QUALITY = 0.85;

    /**
     * Resizes an image file proportionally and provides the JPEG data URL to a success callback.
     * 
     * @param {File} file - HTML input file object.
     * @param {Function} onSuccess - Callback invoked with (dataUrl).
     * @param {Function} onError - Callback invoked with (errorMessage).
     * @param {number} [maxWidth] - Maximum dimension width.
     * @param {number} [maxHeight] - Maximum dimension height.
     */
    function processImageFile(file, onSuccess, onError, maxWidth, maxHeight) {
        if (!file) return;

        if (!file.type || !file.type.match('image.*')) {
            if (typeof onError === 'function') onError('Please select a valid image file');
            return;
        }

        var maxW = maxWidth || DEFAULT_MAX_WIDTH;
        var maxH = maxHeight || DEFAULT_MAX_HEIGHT;

        var reader = new FileReader();
        reader.onerror = function () {
            if (typeof onError === 'function') onError('Failed to read image file');
        };

        reader.onload = function (e) {
            var img = new Image();
            img.onerror = function () {
                if (typeof onError === 'function') onError('Failed to decode image');
            };

            img.onload = function () {
                var canvas = document.createElement('canvas');
                var width = img.width;
                var height = img.height;

                if (width > height) {
                    if (width > maxW) {
                        height *= maxW / width;
                        width = maxW;
                    }
                } else {
                    if (height > maxH) {
                        width *= maxH / height;
                        height = maxH;
                    }
                }

                canvas.width = width;
                canvas.height = height;

                var ctx = canvas.getContext('2d');
                ctx.drawImage(img, 0, 0, width, height);

                var dataUrl = canvas.toDataURL('image/jpeg', DEFAULT_QUALITY);
                if (typeof onSuccess === 'function') {
                    onSuccess(dataUrl);
                }
            };

            img.src = e.target.result;
        };

        reader.readAsDataURL(file);
    }

    return {
        processImageFile: processImageFile
    };
})();
