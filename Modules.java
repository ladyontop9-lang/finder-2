package com.example.storagefinder;

import java.util.ArrayList;
import java.util.List;

/** Rejestr modułów widocznych w Click GUI. */
public final class Modules {
    private static final int NONE = 0;                 // brak kolorowej kropki w GUI
    private static final int PLAYER = 0xFF3DD6FF;
    private static final int MOB = 0xFFFF5555;
    private static final int PASSIVE = 0xFF58D68D;

    public static final String[] ACCENT_NAMES = {"Blue", "Amethyst", "Redstone", "Diamond", "Emerald", "Gold"};
    public static final int[] ACCENT_COLORS = {0x3D8BFF, 0xB06BFF, 0xFF4B4B, 0x45E0E6, 0x3FE07D, 0xFFC73A};

    public static final List<String> CATEGORIES = List.of("Combat", "Render", "HUD");
    public static final List<Module> ALL = new ArrayList<>();
    public static final Module ARRAY_LIST;

    private Modules() {}

    private static String f1(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    static {
        // ---- Combat ----
        ALL.add(new Module("Aura", "Combat",
                "Attacks the closest target in range on its own. Waits for cooldown and skips targets that just got hit.",
                Aura::isEnabled, Aura::setEnabled, List.of(
                new Setting.Slider("Range", 3, 6, 0.1, false,
                        () -> StorageFinderConfig.auraRange, v -> StorageFinderConfig.auraRange = v),
                new Setting.Slider("Speed", 1, 20, 1, true,
                        () -> StorageFinderConfig.auraSpeed, v -> StorageFinderConfig.auraSpeed = v),
                new Setting.Bool("Players only", NONE,
                        () -> StorageFinderConfig.auraPlayersOnly, v -> StorageFinderConfig.auraPlayersOnly = v),
                new Setting.Bool("Wait for cooldown", NONE,
                        () -> StorageFinderConfig.auraCooldown, v -> StorageFinderConfig.auraCooldown = v),
                new Setting.Bool("Through walls", NONE,
                        () -> StorageFinderConfig.auraWalls, v -> StorageFinderConfig.auraWalls = v),
                new Setting.Bool("Face target", NONE,
                        () -> StorageFinderConfig.auraRotate, v -> StorageFinderConfig.auraRotate = v)
        )).withSuffix(() -> StorageFinderConfig.auraPlayersOnly ? "Players" : "All"));

        // ---- Render ----
        List<Setting> sf = new ArrayList<>();
        for (StorageType t : StorageType.values()) {
            sf.add(new Setting.Bool(t.label, t.argb(),
                    () -> StorageFinderConfig.isOn(t), v -> StorageFinderConfig.setOn(t, v)));
        }
        sf.add(new Setting.Bool("Tracers", NONE,
                () -> StorageFinderConfig.tracers, v -> StorageFinderConfig.tracers = v));
        sf.add(new Setting.Slider("Scan range", 2, 16, 1, true,
                () -> StorageFinderConfig.radiusChunks, v -> StorageFinderConfig.radiusChunks = (int) Math.round(v)));
        ALL.add(new Module("Storage Finder", "Render",
                "Finds chests, barrels, spawners, hoppers, pistons and amethyst through walls.",
                () -> StorageFinderConfig.enabled, v -> StorageFinderConfig.enabled = v, sf)
                .withSuffix(() -> String.valueOf(StorageScanner.found().size())));

        ALL.add(new Module("ESP", "Render",
                "Draws boxes around players and mobs, visible through walls. Pick the colors yourself.",
                () -> StorageFinderConfig.espEnabled, v -> StorageFinderConfig.espEnabled = v, List.of(
                new Setting.Bool("Players", PLAYER,
                        () -> StorageFinderConfig.espPlayers, v -> StorageFinderConfig.espPlayers = v),
                new Setting.Bool("Hostile mobs", MOB,
                        () -> StorageFinderConfig.espHostile, v -> StorageFinderConfig.espHostile = v),
                new Setting.Bool("Passive mobs", PASSIVE,
                        () -> StorageFinderConfig.espPassive, v -> StorageFinderConfig.espPassive = v),
                new Setting.ColorPick("Player color",
                        () -> StorageFinderConfig.espPlayerColor, v -> StorageFinderConfig.espPlayerColor = v),
                new Setting.ColorPick("Mob color",
                        () -> StorageFinderConfig.espMobColor, v -> StorageFinderConfig.espMobColor = v),
                new Setting.Slider("Opacity", 10, 100, 5, true,
                        () -> StorageFinderConfig.espOpacity, v -> StorageFinderConfig.espOpacity = (int) Math.round(v))
        )).withSuffix(() -> StorageFinderConfig.espPlayers ? "Players" : "Mobs"));

        ALL.add(new Module("Tracers", "Render",
                "Draws a line from your crosshair to players and mobs. Uses the ESP colors.",
                () -> StorageFinderConfig.tracersEnabled, v -> StorageFinderConfig.tracersEnabled = v, List.of(
                new Setting.Bool("Players", PLAYER,
                        () -> StorageFinderConfig.trPlayers, v -> StorageFinderConfig.trPlayers = v),
                new Setting.Bool("Hostile mobs", MOB,
                        () -> StorageFinderConfig.trHostile, v -> StorageFinderConfig.trHostile = v),
                new Setting.Bool("Passive mobs", PASSIVE,
                        () -> StorageFinderConfig.trPassive, v -> StorageFinderConfig.trPassive = v),
                new Setting.Slider("Thickness", 0.5, 4, 0.5, false,
                        () -> StorageFinderConfig.tracerThickness, v -> StorageFinderConfig.tracerThickness = v)
        )));

        ALL.add(new Module("Freecam", "Render",
                "Fly the camera around freely while your character stays where it is.",
                Freecam::isActive, Freecam::setActive, List.of(
                new Setting.Slider("Speed", 1, 10, 0.5, false,
                        () -> StorageFinderConfig.freecamSpeed, v -> StorageFinderConfig.freecamSpeed = v)
        )).withSuffix(() -> f1(StorageFinderConfig.freecamSpeed)));

        // ---- HUD ----
        ARRAY_LIST = new Module("ArrayList", "HUD",
                "Lists enabled modules in the top right corner. The accent color applies to the whole client.",
                () -> StorageFinderConfig.arrayList, v -> StorageFinderConfig.arrayList = v, List.of(
                new Setting.Swatches("Accent", ACCENT_NAMES, ACCENT_COLORS,
                        () -> StorageFinderConfig.accent, v -> StorageFinderConfig.accent = v),
                new Setting.Mode("Color mode", new String[]{"Static", "Rainbow", "Gradient"},
                        () -> StorageFinderConfig.alColorMode, v -> StorageFinderConfig.alColorMode = v),
                new Setting.Mode("Sort", new String[]{"Length", "A-Z"},
                        () -> StorageFinderConfig.alSort, v -> StorageFinderConfig.alSort = v),
                new Setting.Bool("Suffixes", NONE,
                        () -> StorageFinderConfig.alSuffix, v -> StorageFinderConfig.alSuffix = v),
                new Setting.Bool("Background", NONE,
                        () -> StorageFinderConfig.alBackground, v -> StorageFinderConfig.alBackground = v)
        ));
        ALL.add(ARRAY_LIST);
    }
}
