package com.example.storagefinder;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

public final class StorageFinderConfig {
    // ---- Storage Finder ----
    public static boolean enabled = false;
    public static final boolean[] types = new boolean[StorageType.values().length];
    public static boolean tracers = true;          // tracery do bloków
    public static int radiusChunks = 8;

    // ---- ESP ----
    public static boolean espEnabled = false;
    public static boolean espPlayers = true;
    public static boolean espHostile = true;
    public static boolean espPassive = false;
    public static int espPlayerColor = 0x3DD6FF;
    public static int espMobColor = 0xFF5555;
    public static int espOpacity = 50;             // %

    // ---- Tracers (gracze / moby) ----
    public static boolean tracersEnabled = false;
    public static boolean trPlayers = true;
    public static boolean trHostile = false;
    public static boolean trPassive = false;
    public static double tracerThickness = 1.0;

    // ---- ArrayList ----
    public static boolean arrayList = true;
    public static boolean alBackground = true;
    public static boolean alSuffix = true;
    public static int alColorMode = 2;             // 0 Static, 1 Rainbow, 2 Gradient
    public static int alSort = 0;                  // 0 Length, 1 A-Z
    public static int accent = 0x3D8BFF;

    // ---- Freecam / Aura ----
    public static double freecamSpeed = 3.0;
    public static double auraRange = 3.0;
    public static double auraSpeed = 8.0;
    public static boolean auraPlayersOnly = true;
    public static boolean auraCooldown = true;
    public static boolean auraWalls = false;
    public static boolean auraRotate = false;

    private static int lastSig = 0;

    static {
        Arrays.fill(types, true);
    }

    private StorageFinderConfig() {}

    public static boolean isOn(StorageType t) {
        return types[t.ordinal()];
    }

    public static void setOn(StorageType t, boolean v) {
        types[t.ordinal()] = v;
    }

    public static void toggle(StorageType t) {
        types[t.ordinal()] = !types[t.ordinal()];
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("storagefinder.properties");
    }

    private static boolean b(Properties p, String k, boolean d) {
        String v = p.getProperty(k);
        return v == null ? d : Boolean.parseBoolean(v);
    }

    private static double dbl(Properties p, String k, double d) {
        try { return Double.parseDouble(p.getProperty(k, Double.toString(d))); } catch (Exception e) { return d; }
    }

    private static int num(Properties p, String k, int d) {
        try { return Integer.parseInt(p.getProperty(k, Integer.toString(d))); } catch (Exception e) { return d; }
    }

    public static void load() {
        try {
            Path f = file();
            if (!Files.exists(f)) return;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(f)) {
                p.load(in);
            }
            enabled = b(p, "enabled", false);
            for (StorageType t : StorageType.values()) {
                types[t.ordinal()] = b(p, "type." + t.name(), true);
            }
            tracers = b(p, "tracers", true);
            radiusChunks = Math.max(2, Math.min(16, num(p, "radius", 8)));

            espEnabled = b(p, "espEnabled", false);
            espPlayers = b(p, "espPlayers", true);
            espHostile = b(p, "espHostile", true);
            espPassive = b(p, "espPassive", false);
            espPlayerColor = num(p, "espPlayerColor", 0x3DD6FF) & 0xFFFFFF;
            espMobColor = num(p, "espMobColor", 0xFF5555) & 0xFFFFFF;
            espOpacity = Math.max(10, Math.min(100, num(p, "espOpacity", 50)));

            tracersEnabled = b(p, "tracersEnabled", false);
            trPlayers = b(p, "trPlayers", true);
            trHostile = b(p, "trHostile", false);
            trPassive = b(p, "trPassive", false);
            tracerThickness = dbl(p, "tracerThickness", 1.0);

            arrayList = b(p, "arrayList", true);
            alBackground = b(p, "alBackground", true);
            alSuffix = b(p, "alSuffix", true);
            alColorMode = Math.max(0, Math.min(2, num(p, "alColorMode", 2)));
            alSort = Math.max(0, Math.min(1, num(p, "alSort", 0)));
            accent = num(p, "accent", 0x3D8BFF) & 0xFFFFFF;

            freecamSpeed = dbl(p, "freecamSpeed", 3.0);
            auraRange = dbl(p, "auraRange", 3.0);
            auraSpeed = dbl(p, "auraSpeed", 8.0);
            auraPlayersOnly = b(p, "auraPlayersOnly", true);
            auraCooldown = b(p, "auraCooldown", true);
            auraWalls = b(p, "auraWalls", false);
            auraRotate = b(p, "auraRotate", false);
        } catch (Exception ignored) {
        }
        lastSig = signature();
    }

    private static int signature() {
        return Objects.hash(enabled, Arrays.hashCode(types), radiusChunks);
    }

    public static void save() {
        int sig = signature();
        if (sig != lastSig) {
            lastSig = sig;
            StorageScanner.requestRescan();   // tylko gdy zmieniło się coś ważnego dla skanera
        }
        try {
            Properties p = new Properties();
            p.setProperty("enabled", Boolean.toString(enabled));
            for (StorageType t : StorageType.values()) {
                p.setProperty("type." + t.name(), Boolean.toString(types[t.ordinal()]));
            }
            p.setProperty("tracers", Boolean.toString(tracers));
            p.setProperty("radius", Integer.toString(radiusChunks));

            p.setProperty("espEnabled", Boolean.toString(espEnabled));
            p.setProperty("espPlayers", Boolean.toString(espPlayers));
            p.setProperty("espHostile", Boolean.toString(espHostile));
            p.setProperty("espPassive", Boolean.toString(espPassive));
            p.setProperty("espPlayerColor", Integer.toString(espPlayerColor));
            p.setProperty("espMobColor", Integer.toString(espMobColor));
            p.setProperty("espOpacity", Integer.toString(espOpacity));

            p.setProperty("tracersEnabled", Boolean.toString(tracersEnabled));
            p.setProperty("trPlayers", Boolean.toString(trPlayers));
            p.setProperty("trHostile", Boolean.toString(trHostile));
            p.setProperty("trPassive", Boolean.toString(trPassive));
            p.setProperty("tracerThickness", Double.toString(tracerThickness));

            p.setProperty("arrayList", Boolean.toString(arrayList));
            p.setProperty("alBackground", Boolean.toString(alBackground));
            p.setProperty("alSuffix", Boolean.toString(alSuffix));
            p.setProperty("alColorMode", Integer.toString(alColorMode));
            p.setProperty("alSort", Integer.toString(alSort));
            p.setProperty("accent", Integer.toString(accent));

            p.setProperty("freecamSpeed", Double.toString(freecamSpeed));
            p.setProperty("auraRange", Double.toString(auraRange));
            p.setProperty("auraSpeed", Double.toString(auraSpeed));
            p.setProperty("auraPlayersOnly", Boolean.toString(auraPlayersOnly));
            p.setProperty("auraCooldown", Boolean.toString(auraCooldown));
            p.setProperty("auraWalls", Boolean.toString(auraWalls));
            p.setProperty("auraRotate", Boolean.toString(auraRotate));
            try (OutputStream out = Files.newOutputStream(file())) {
                p.store(out, "Storage Finder");
            }
        } catch (Exception ignored) {
        }
    }
}
