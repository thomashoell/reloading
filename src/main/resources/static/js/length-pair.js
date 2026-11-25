/*
 * Length fields that come in pairs: one in millimeters, one in inches. Typing in one fills the other.
 * Only the millimeter field has a name and is sent to the server; the inch field is a convenience.
 * Without JavaScript the millimeter field still works on its own.
 *
 * Markup: a container with data-length-pair holding <input data-unit="mm"> and <input data-unit="in">.
 * Converted values are rounded to 2 decimals (mm) and 3 (in); fine measures can ask for more with
 * data-mm-decimals and data-in-decimals on the container.
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

    function decimals(pair, attribute, fallback) {
        var number = parseInt(pair.getAttribute(attribute), 10);
        return isNaN(number) ? fallback : number;
    }

    function connect(pair) {
        var mm = pair.querySelector('[data-unit="mm"]');
        var inch = pair.querySelector('[data-unit="in"]');
        if (!mm || !inch) {
            return;
        }
        var inchDecimals = decimals(pair, 'data-in-decimals', 3);
        var mmDecimals = decimals(pair, 'data-mm-decimals', 2);
        var toInch = function () { convert(mm, inch, 1 / MM_PER_INCH, inchDecimals); };
        var toMm = function () { convert(inch, mm, MM_PER_INCH, mmDecimals); };
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
