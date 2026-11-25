package de.tho.reloading.web;

import java.util.UUID;

/** A product a lot can belong to; group is its manufacturer, label what tells it apart within the manufacturer. */
public record ProductOption(UUID id, String group, String label) {

    public String fullName() {
        return group + " " + label;
    }
}
