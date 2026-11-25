/*
 * Length fields that come in pairs: one in millimeters, one in inches. Typing in one fills the other.
 * Only the millimeter field has a name and is sent to the server; the inch field is a convenience.
 * Without JavaScript the millimeter field still works on its own.
 *
 * Markup: a container with data-length-pair holding <input data-unit="mm"> and <input data-unit="in">.
 */
(function () {
    'use strict';

    var MM_PER_INCH = 25.4;

    // trailing zeros and float noise off: 203.20000000000002 -> "203.2"
    function tidy(value, decimals) {
        return String(parseFloat(value.toFixed(decimals)));
    }

    function convert(source, target, factor, decimals) {
        var text = source.value.trim();
        if (text === '') {
            target.value = '';
            return;
        }
        var number = Number(text);
        if (!isFinite(number)) {
            return;
        }
        target.value = tidy(number * factor, decimals);
    }

    function connect(pair) {
        var mm = pair.querySelector('[data-unit="mm"]');
        var inch = pair.querySelector('[data-unit="in"]');
        if (!mm || !inch) {
            return;
        }
        var toInch = function () { convert(mm, inch, 1 / MM_PER_INCH, 3); };
        var toMm = function () { convert(inch, mm, MM_PER_INCH, 2); };
        mm.addEventListener('input', toInch);
        inch.addEventListener('input', toMm);
        // a value from the server (edit form, or a form sent back with errors) shows up in inches as well
        toInch();
    }

    function init() {
        document.querySelectorAll('[data-length-pair]').forEach(connect);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
