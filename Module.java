package com.example.storagefinder;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.KeyMapping;

public final class Module {
    public final String name;
    public final String category;
    public final String desc;
    public final List<Setting> settings;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    /** Bind do włączania/wyłączania (ustawiany w Click GUI). */
    public KeyMapping keyMapping;

    /** Krótki dopisek w ArrayList (np. tryb, liczba znalezionych). */
    public Supplier<String> suffix;

    // stan animacji GUI
    public float onAnim, hoverAnim;

    public Module(String name, String category, String desc, BooleanSupplier getter,
                  Consumer<Boolean> setter, List<Setting> settings) {
        this.name = name;
        this.category = category;
        this.desc = desc;
        this.getter = getter;
        this.setter = setter;
        this.settings = settings;
    }

    public Module withSuffix(Supplier<String> s) {
        this.suffix = s;
        return this;
    }

    public boolean isEnabled() { return getter.getAsBoolean(); }
    public void setEnabled(boolean v) { setter.accept(v); }
    public void toggle() { setEnabled(!isEnabled()); }

    public String slug() {
        return name.toLowerCase().replace(' ', '_');
    }
}
