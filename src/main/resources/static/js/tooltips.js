/*
 * Turns the browser's own title tooltip into a Bootstrap tooltip, which also shows on keyboard focus and on touch.
 * Without JavaScript the title still works on hover.
 *
 * Markup: <abbr class="lb-term" tabindex="0" data-bs-toggle="tooltip" title="...">COAL</abbr>
 * Needs the Bootstrap bundle, so load this file after it.
 */
(function () {
    'use strict';

    function init() {
        if (!window.bootstrap || !window.bootstrap.Tooltip) {
            return;
        }
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
            new window.bootstrap.Tooltip(element, {trigger: 'hover focus'});
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
