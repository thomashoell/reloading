package de.tho.reloading.utility;

/** How the cases of a batch were sized. The names are stored in the database, the labels are for the UI. */
public enum Calibration {
    FULL("Vollkalibriert"),
    NECK("Halskalibriert"),
    SHOULDER_AND_NECK("Schulter und Hals");

    private final String label;

    Calibration(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
