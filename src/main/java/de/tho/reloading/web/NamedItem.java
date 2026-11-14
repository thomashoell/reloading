package de.tho.reloading.web;

import java.util.UUID;

/** Row of the list of master data that only has a name. */
public record NamedItem(UUID id, String name) {
}
