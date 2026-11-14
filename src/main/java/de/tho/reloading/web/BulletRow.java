package de.tho.reloading.web;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Row of the bullet list; weight and length are formatted in the units of the current user, the diameter always
 * in inches and millimeters.
 * The SI values are only used for sorting.
 */
public record BulletRow(UUID id, String manufacturer, String name, String diameter, String weight, String length,
                        BigDecimal diameterSi, BigDecimal weightSi) {
}
