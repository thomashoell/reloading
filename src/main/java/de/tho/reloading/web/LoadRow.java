package de.tho.reloading.web;

import java.util.UUID;

/** Row of the list of loads; everything is formatted for display. */
public record LoadRow(UUID id, String loadedOn, String bullet, String powder, String powderCharge, String primer,
                      String casing, String overallLength, int quantity, String rifle) {
}
