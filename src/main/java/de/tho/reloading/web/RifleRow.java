package de.tho.reloading.web;

import java.util.UUID;

/** Row of the rifle list; lengths are formatted in millimeters and inches. */
public record RifleRow(UUID id, String name, String manufacturer, String caliber, String barrelLength,
                       String lengthOfTwist) {
}
