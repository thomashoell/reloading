package de.tho.reloading.utility;

public enum PrimerSize {
    SMALL_RIFLE("Small Rifle"),
    SMALL_RIFLE_MAGNUM("Small Rifle Magnum"),
    LARGE_RIFLE("Large Rifle"),
    LARGE_RIFLE_MAGNUM("Large Rifle Magnum"),
    SMALL_PISTOL("Small Pistol"),
    SMALL_PISTOL_MAGNUM("Small Pistol Magnum"),
    LARGE_PISTOL("Large Pistol"),
    LARGE_PISTOL_MAGNUM("Large Pistol Magnum");

    private final String label;

    PrimerSize(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
