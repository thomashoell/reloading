package de.tho.reloading.web;

import java.util.UUID;

/** Row of a list of lots. */
public record LotRow(UUID id, String product, String lotNumber) {
}
