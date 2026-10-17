package com.example.storagefinder;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** ArrayList w stylu Krukx: kolorowe wpisy, dopiski, tło i pasek po prawej. */
public final class Hud {
    private record Item(String name, String suffix, int width) {}

    private static final List<Item> items = new ArrayList<>();

    private Hud() {}

    public static void init() {
        HudRenderCallback.EVENT.register((graphics, deltaTracker) -> render(graphics));
    }

    private static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int shiftHue(int rgb, float delta) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h /= 6f;
            if (h < 0) h += 1f;
        }
        float s = max == 0 ? 0 : d / max;
        return 0xFF000000 | Mth.hsvToRgb((h + delta) % 1f, s, max);
    }

    private static void render(GuiGraphics g) {
        if (!StorageFinderConfig.arrayList) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;

        boolean sfx = StorageFinderConfig.alSuffix;
        items.clear();
        for (Module m : Modules.ALL) {
            if (m == Modules.ARRAY_LIST || !m.isEnabled()) continue;
            String suffix = (sfx && m.suffix != null) ? m.suffix.get() : "";
            int w = mc.font.width(m.name) + (suffix.isEmpty() ? 0 : mc.font.width(suffix) + 5);
            items.add(new Item(m.name, suffix, w));
        }
        if (items.isEmpty()) return;

        if (StorageFinderConfig.alSort == 1) items.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        else items.sort((a, b) -> b.width() - a.width());

        int accent = 0xFF000000 | StorageFinderConfig.accent;
        int accent2 = shiftHue(accent, 0.12f);
        int mode = StorageFinderConfig.alColorMode;
        boolean bg = StorageFinderConfig.alBackground;
        long time = System.currentTimeMillis();

        int sw = g.guiWidth();
        int lh = mc.font.lineHeight + 3;
        int y = 3;
        int n = items.size();

        for (int i = 0; i < n; i++) {
            Item it = items.get(i);
            int col;
            if (mode == 1) col = 0xFF000000 | Mth.hsvToRgb(((time / 20) % 360 / 360f + i * 0.05f) % 1f, 0.6f, 1f);
            else if (mode == 2) col = mix(accent, accent2, n == 1 ? 0f : i / (float) (n - 1));
            else col = accent;

            int right = sw - 3;                                   // lewa krawędź paska
            int w = it.width() + 9;
            if (bg) g.fill(right - w, y, right, y + lh, 0x8C07070C);
            g.fill(right, y, right + 3, y + lh, col);              // pasek przy krawędzi
            g.drawString(mc.font, it.name(), right - w + 5, y + 2, col);
            if (!it.suffix().isEmpty()) {
                g.drawString(mc.font, it.suffix(), right - w + 5 + mc.font.width(it.name()) + 5, y + 2, 0xFF9A9DB8);
            }
            y += lh;
        }
    }
}
