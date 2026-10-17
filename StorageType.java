package com.example.storagefinder;

public enum StorageType {
    CHEST("Chests", 0xFFA21A),
    ENDER_CHEST("Ender chests", 0x6C5CFF),
    SHULKER("Shulker boxes", 0xFF4FD8),
    SPAWNER("Spawners", 0xFF4D4D),
    BARREL("Barrels", 0xC8955A),
    HOPPER("Hoppers", 0x9AA5B1),
    PISTON("Pistons", 0xF4D03F),
    STICKY_PISTON("Sticky pistons", 0x58D68D),
    AMETHYST("Amethyst clusters", 0xC9A0FF);

    public final String label;
    public final int rgb;
    private final float[] floats;

    StorageType(String label, int rgb) {
        this.label = label;
        this.rgb = rgb;
        this.floats = new float[]{((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f};
    }

    public int argb() {
        return 0xFF000000 | rgb;
    }

    /** Zwraca stałą tablicę (bez alokacji) — nie modyfikować. */
    public float[] rgbFloats() {
        return floats;
    }
}
