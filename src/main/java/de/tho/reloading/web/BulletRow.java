package de.tho.reloading.web;

import java.util.UUID;

/** Row of the bullet list; measures are formatted in the units of the current user. */
public record BulletRow(UUID id, String manufacturer, String name, String diameter, String weight, String length) {
}
