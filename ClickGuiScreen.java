package com.example.storagefinder;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Click GUI w stylu Krukx: jedno okno, pasek kategorii po lewej, siatka modułów 2 kolumny,
 * strona ustawień modułu. LPM = włącz/wyłącz, PPM = ustawienia, ŚPM = bind.
 */
public class ClickGuiScreen extends Screen {
    private static final int WIN = 0xFF1D212C, SIDE = 0xFF181B25, CARD = 0xFF292E3B, CARD_H = 0xFF30364A,
            BAR_OFF = 0xFF3B4151, LINE = 0xFF2C3140, TEXT = 0xFFE9EBF1, SUB = 0xFF9298A9, VEIL = 0x8005070C;
    private static final int SW = 26, HEAD = 22, CARD_HT = 26, GAP = 5;
    private static final int ROW_BOOL = 20, ROW_SLIDER = 28, ROW_MODE = 22, ROW_SWATCH = 22, ROW_COLOR = 52;

    private static final String[] NAV_IDS = {"Home", "Combat", "Render", "HUD"};
    private static final String[][] ICONS = {
            {"....#....", "...###...", "..#####..", ".#######.", "#########", ".##...##.", ".##...##.", ".##.#.##.", ".##.#.##."},
            {".......##", "......###", ".....###.", "....###..", "#..###...", ".####....", "..##.....", ".#.##....", "#...#...."},
            {".........", "...###...", ".##...##.", "#..###..#", "#.##.##.#", "#..###..#", ".##...##.", "...###...", "........."},
            {"#########", "#.......#", "#########", "#..#....#", "#..#....#", "#..#....#", "#..#....#", "#..#....#", "#########"}
    };

    private enum Kind { NAV, CLOSE, BACK, CARD, ENABLED, BOOL, SLIDER, CHANNEL, MODE, SWATCH, BIND }

    private record Hit(Kind kind, int x, int y, int w, int h, boolean body, Module mod, Setting setting, int idx) {
        boolean inside(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Hit> hits = new ArrayList<>();
    private EditBox search;

    private int cat = 0;                // indeks w NAV_IDS
    private Module mod = null;          // != null => strona ustawień
    private Module binding = null;
    private Hit dragHit = null;

    private float scroll = 0, scrollTarget = 0, maxScroll = 0;
    private float openT = 0, pageT = 1;
    private long lastNanos = 0;
    private float A = 1f;

    // layout
    private int wx, wy, ww, wh, bx, by, bw, bh;

    public ClickGuiScreen() {
        super(Component.literal("Click GUI"));
    }

    // ------------------------------------------------------------------ helpers

    private static float ease(float t) { return t * t * (3f - 2f * t); }

    private static float approach(float cur, float target, float k) {
        float v = cur + (target - cur) * k;
        return Math.abs(target - v) < 0.003f ? target : v;
    }

    private static int lerp(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = Math.round(ar + (br - ar) * t), g = Math.round(ag + (bg - ag) * t), bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private int accent() { return 0xFF000000 | StorageFinderConfig.accent; }

    private int c(int argb) {
        int al = Math.round(((argb >>> 24) & 255) * A);
        return (al << 24) | (argb & 0xFFFFFF);
    }

    private void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int col) { g.fill(x1, y1, x2, y2, c(col)); }

    private void rr(GuiGraphics g, int x, int y, int w, int h, int r, int col) { Ui.rrect(g, x, y, w, h, r, c(col)); }

    private void txt(GuiGraphics g, String s, int x, int y, int col) { g.drawString(this.font, s, x, y, c(col)); }

    private void txtR(GuiGraphics g, String s, int xr, int y, int col) {
        g.drawString(this.font, s, xr - this.font.width(s), y, c(col));
    }

    private String fit(String s, int maxW) {
        if (this.font.width(s) <= maxW) return s;
        return this.font.plainSubstrByWidth(s, Math.max(0, maxW - this.font.width("..."))) + "...";
    }

    private void startPage() {
        pageT = 0;
        scroll = 0;
        scrollTarget = 0;
    }

    private void layout(int oy) {
        ww = Math.min(440, this.width - 16);
        wh = Math.min(268, this.height - 16);
        wx = (this.width - ww) / 2;
        wy = (this.height - wh) / 2 + oy;
        bx = wx + SW + 8;
        by = wy + HEAD + 8;
        bw = ww - SW - 16;
        bh = wh - HEAD - 16;
    }

    private String keyText(Module m) {
        if (m.keyMapping == null || m.keyMapping.isUnbound()) return "None";
        return m.keyMapping.getTranslatedKeyMessage().getString();
    }

    // ------------------------------------------------------------------ init

    @Override
    protected void init() {
        layout(0);
        int closeX = wx + ww - 6 - 14;
        int sbx = closeX - 5 - 110;
        search = new EditBox(this.font, sbx + 17, wy + 4, 110 - 21, 14, Component.literal("Search"));
        search.setBordered(false);
        search.setTextColor(TEXT);
        search.setHint(Component.literal("Type to search"));
        search.setResponder(s -> {
            if (!s.isEmpty()) mod = null;
            startPage();
        });
        addWidget(search);

        for (Module m : Modules.ALL) {
            m.onAnim = m.isEnabled() ? 1f : 0f;
            m.hoverAnim = 0f;
        }
        lastNanos = 0;
        openT = 0;
        pageT = 1;
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics g, int mx, int my, float partialTick) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0.016f : Math.min(0.1f, (now - lastNanos) / 1_000_000_000f);
        lastNanos = now;
        float k = 1f - (float) Math.exp(-dt * 16f);

        openT = approach(openT, 1f, 1f - (float) Math.exp(-dt * 12f));
        pageT = approach(pageT, 1f, 1f - (float) Math.exp(-dt * 14f));
        scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-dt * 18f));
        if (Math.abs(scrollTarget - scroll) < 0.05f) scroll = scrollTarget;

        float ge = ease(openT);
        int oy = Math.round((1f - ge) * 6f);
        layout(oy);
        hits.clear();

        for (Module m : Modules.ALL) m.onAnim = approach(m.onAnim, m.isEnabled() ? 1f : 0f, k);

        A = ge;
        g.fill(0, 0, this.width, this.height, c(VEIL));

        drawChrome(g, mx, my);
        drawSearch(g, mx, my, partialTick);

        // ---- treść (przewijana, z fade'em przy zmianie strony) ----
        float pe = ease(pageT);
        A = ge * pe;
        int yOff = Math.round((1f - pe) * 4f);
        g.enableScissor(bx - 2, by, bx + bw + 2, by + bh);
        int contentH = (mod != null) ? drawSettings(g, mx, my, yOff, k) : drawGrid(g, mx, my, yOff, k);
        g.disableScissor();
        maxScroll = Math.max(0, contentH - bh);
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;

        A = ge;
        // pasek przewijania
        if (maxScroll > 0) {
            int trackH = bh;
            int thumbH = Math.max(14, Math.round(trackH * (bh / (float) contentH)));
            int thumbY = by + Math.round((trackH - thumbH) * (scroll / maxScroll));
            rr(g, wx + ww - 5, thumbY, 2, thumbH, 1, BAR_OFF);
        }
    }

    private void drawChrome(GuiGraphics g, int mx, int my) {
        rr(g, wx - 1, wy - 1, ww + 2, wh + 2, 6, LINE);
        rr(g, wx, wy, ww, wh, 5, WIN);

        // pasek boczny
        rr(g, wx, wy, SW, wh, 5, SIDE);
        fill(g, wx + 5, wy, wx + SW, wy + wh, SIDE);
        fill(g, wx + SW - 1, wy, wx + SW, wy + wh, LINE);

        int ny = wy + 8;
        for (int i = 0; i < NAV_IDS.length; i++) {
            if (i == 1) {
                fill(g, wx + 8, ny + 1, wx + SW - 9, ny + 2, LINE);
                ny += 6;
            }
            boolean sel = i == cat;
            boolean hover = mx >= wx + 4 && mx < wx + 4 + 18 && my >= ny && my < ny + 18;
            if (sel) rr(g, wx + 4, ny, 18, 18, 3, accent());
            else if (hover) rr(g, wx + 4, ny, 18, 18, 3, CARD);
            drawIcon(g, ICONS[i], wx + 4 + 4, ny + 4, sel ? 0xFFFFFFFF : (hover ? TEXT : SUB));
            hits.add(new Hit(Kind.NAV, wx + 4, ny, 18, 18, false, null, null, i));
            ny += 22;
        }

        // nagłówek
        fill(g, wx + SW, wy + HEAD, wx + ww, wy + HEAD + 1, LINE);

        int cx = wx + SW + 10, cy = wy + 7;
        String catName = NAV_IDS[cat];
        String term = search.getValue().trim();
        if (mod != null) {
            int w = this.font.width(catName);
            boolean hv = mx >= cx && mx < cx + w && my >= wy && my < wy + HEAD;
            txt(g, catName, cx, cy, hv ? accent() : TEXT);
            hits.add(new Hit(Kind.BACK, cx, wy, w, HEAD, false, null, null, 0));
            fill(g, cx + w + 6, cy - 1, cx + w + 7, cy + 9, SUB);
            txt(g, mod.name, cx + w + 12, cy, SUB);
        } else {
            txt(g, catName, cx, cy, TEXT);
            int w = this.font.width(catName);
            fill(g, cx + w + 6, cy - 1, cx + w + 7, cy + 9, SUB);
            if (!term.isEmpty()) txt(g, "Search", cx + w + 12, cy, SUB);
        }

        // przycisk zamknięcia
        int closeX = wx + ww - 6 - 14, closeY = wy + 4;
        boolean ch = mx >= closeX && mx < closeX + 14 && my >= closeY && my < closeY + 14;
        rr(g, closeX, closeY, 14, 14, 3, LINE);
        rr(g, closeX + 1, closeY + 1, 12, 12, 3, ch ? CARD_H : CARD);
        int xc = ch ? TEXT : SUB;
        for (int i = 0; i < 6; i++) {
            fill(g, closeX + 4 + i, closeY + 4 + i, closeX + 5 + i, closeY + 5 + i, xc);
            fill(g, closeX + 9 - i, closeY + 4 + i, closeX + 10 - i, closeY + 5 + i, xc);
        }
        hits.add(new Hit(Kind.CLOSE, closeX, closeY, 14, 14, false, null, null, 0));
    }

    private void drawIcon(GuiGraphics g, String[] rows, int x, int y, int col) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            for (int cc = 0; cc < row.length(); cc++) {
                if (row.charAt(cc) == '#') fill(g, x + cc, y + r, x + cc + 1, y + r + 1, col);
            }
        }
    }

    private void drawSearch(GuiGraphics g, int mx, int my, float pt) {
        int closeX = wx + ww - 6 - 14;
        int sbx = closeX - 5 - 110, sby = wy + 4;
        boolean focus = search.isFocused();
        rr(g, sbx, sby, 110, 14, 3, focus ? accent() : LINE);
        rr(g, sbx + 1, sby + 1, 108, 12, 3, CARD);
        // lupa
        int lx = sbx + 5, ly = sby + 4;
        int lc = SUB;
        fill(g, lx + 1, ly, lx + 4, ly + 1, lc);
        fill(g, lx + 1, ly + 4, lx + 4, ly + 5, lc);
        fill(g, lx, ly + 1, lx + 1, ly + 4, lc);
        fill(g, lx + 4, ly + 1, lx + 5, ly + 4, lc);
        fill(g, lx + 5, ly + 5, lx + 7, ly + 7, lc);

        search.setX(sbx + 17);
        search.setY(sby);
        search.render(g, mx, my, pt);
    }

    // ------------------------------------------------------------------ grid

    private List<Module> visibleModules() {
        String term = search.getValue().trim().toLowerCase();
        List<Module> out = new ArrayList<>();
        for (Module m : Modules.ALL) {
            if (!term.isEmpty()) {
                if (m.name.toLowerCase().contains(term) || m.desc.toLowerCase().contains(term)) out.add(m);
            } else if (cat == 0 || m.category.equals(NAV_IDS[cat])) {
                out.add(m);
            }
        }
        return out;
    }

    private int drawGrid(GuiGraphics g, int mx, int my, int yOff, float k) {
        List<Module> mods = visibleModules();
        int top = by - Math.round(scroll) + yOff;

        if (mods.isEmpty()) {
            txt(g, "No modules match \"" + search.getValue().trim() + "\".", bx, top + 4, SUB);
            return 20;
        }

        int colW = (bw - GAP) / 2;
        boolean inView = mx >= bx && mx < bx + bw && my >= by && my < by + bh;

        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int cx = bx + (i % 2) * (colW + GAP);
            int cy = top + (i / 2) * (CARD_HT + GAP);
            if (cy + CARD_HT < by - 2 || cy > by + bh + 2) continue;

            boolean hover = inView && mx >= cx && mx < cx + colW && my >= cy && my < cy + CARD_HT;
            m.hoverAnim = approach(m.hoverAnim, hover ? 1f : 0f, k);

            rr(g, cx, cy, colW, CARD_HT, 3, lerp(CARD, CARD_H, m.hoverAnim));
            rr(g, cx + colW - 5, cy, 5, CARD_HT, 2, lerp(BAR_OFF, accent(), m.onAnim));

            txt(g, m.name, cx + 7, cy + 5, lerp(TEXT, accent(), m.onAnim));
            txt(g, fit(m.desc, colW - 7 - 11), cx + 7, cy + 15, lerp(SUB, accent(), m.onAnim * 0.8f));

            hits.add(new Hit(Kind.CARD, cx, cy, colW, CARD_HT, true, m, null, 0));
        }
        int rows = (mods.size() + 1) / 2;
        return rows * (CARD_HT + GAP) - GAP;
    }

    // ------------------------------------------------------------------ settings page

    private void toggleSwitch(GuiGraphics g, int x, int y, boolean on) {
        rr(g, x, y, 22, 12, 6, on ? accent() : BAR_OFF);
        rr(g, on ? x + 12 : x + 2, y + 2, 8, 8, 4, on ? 0xFFFFFFFF : 0xFFC9CDD8);
    }

    private int drawSettings(GuiGraphics g, int mx, int my, int yOff, float k) {
        int y = by - Math.round(scroll) + yOff;
        int start = y;

        // opis
        List<FormattedCharSequence> lines = this.font.split(Component.literal(mod.desc), bw);
        for (FormattedCharSequence line : lines) {
            g.drawString(this.font, line, bx, y, c(SUB));
            y += 10;
        }
        y += 4;

        // Enabled
        rr(g, bx, y, bw, ROW_BOOL, 3, CARD);
        txt(g, "Enabled", bx + 7, y + 6, TEXT);
        toggleSwitch(g, bx + bw - 7 - 22, y + 4, mod.isEnabled());
        hits.add(new Hit(Kind.ENABLED, bx, y, bw, ROW_BOOL, true, mod, null, 0));
        y += ROW_BOOL + 4;

        for (Setting s : mod.settings) {
            if (s instanceof Setting.Bool b) y = drawBool(g, b, y);
            else if (s instanceof Setting.Slider sl) y = drawSlider(g, sl, y);
            else if (s instanceof Setting.Mode md) y = drawMode(g, md, y);
            else if (s instanceof Setting.Swatches sw) y = drawSwatches(g, sw, y);
            else if (s instanceof Setting.ColorPick cp) y = drawColor(g, cp, y);
        }

        // Keybind
        rr(g, bx, y, bw, ROW_BOOL, 3, CARD);
        txt(g, "Keybind", bx + 7, y + 6, TEXT);
        boolean wait = binding == mod;
        String kt = wait ? "Press a key" : keyText(mod);
        int btnW = Math.max(40, this.font.width(kt) + 14);
        int btnX = bx + bw - 7 - btnW;
        rr(g, btnX, y + 3, btnW, 14, 3, SIDE);
        txt(g, kt, btnX + (btnW - this.font.width(kt)) / 2, y + 6, wait ? accent() : TEXT);
        hits.add(new Hit(Kind.BIND, btnX, y + 3, btnW, 14, true, mod, null, 0));
        y += ROW_BOOL + 4;

        return (y - start) - 4;
    }

    private int drawBool(GuiGraphics g, Setting.Bool b, int y) {
        rr(g, bx, y, bw, ROW_BOOL, 3, CARD);
        int lx = bx + 7;
        if (b.color != 0) {
            rr(g, lx, y + 7, 6, 6, 2, b.color);
            lx += 11;
        }
        txt(g, b.name, lx, y + 6, TEXT);
        toggleSwitch(g, bx + bw - 7 - 22, y + 4, b.get());
        hits.add(new Hit(Kind.BOOL, bx, y, bw, ROW_BOOL, true, mod, b, 0));
        return y + ROW_BOOL + 4;
    }

    private void track(GuiGraphics g, int x, int y, int w, double frac, int fillCol) {
        rr(g, x, y, w, 3, 1, BAR_OFF);
        int fw = (int) Math.round(w * Math.max(0, Math.min(1, frac)));
        if (fw > 0) rr(g, x, y, Math.max(2, fw), 3, 1, fillCol);
        rr(g, x + fw - 4, y - 3, 8, 8, 4, 0xFFFFFFFF);
    }

    private int drawSlider(GuiGraphics g, Setting.Slider sl, int y) {
        rr(g, bx, y, bw, ROW_SLIDER, 3, CARD);
        txt(g, sl.name, bx + 7, y + 5, TEXT);
        txtR(g, sl.display(), bx + bw - 7, y + 5, SUB);
        int tx = bx + 11, tw = bw - 22;
        track(g, tx, y + 19, tw, (sl.get() - sl.min) / (sl.max - sl.min), accent());
        hits.add(new Hit(Kind.SLIDER, tx, y + 13, tw, 12, true, mod, sl, 0));
        return y + ROW_SLIDER + 4;
    }

    private int drawMode(GuiGraphics g, Setting.Mode md, int y) {
        rr(g, bx, y, bw, ROW_MODE, 3, CARD);
        txt(g, md.name, bx + 7, y + 7, TEXT);
        int total = 0;
        for (String o : md.options) total += this.font.width(o) + 10 + 3;
        total -= 3;
        int cx = bx + bw - 7 - total;
        int sel = md.get();
        for (int i = 0; i < md.options.length; i++) {
            int w = this.font.width(md.options[i]) + 10;
            rr(g, cx, y + 4, w, 14, 3, i == sel ? accent() : SIDE);
            txt(g, md.options[i], cx + 5, y + 7, i == sel ? 0xFFFFFFFF : SUB);
            hits.add(new Hit(Kind.MODE, cx, y + 4, w, 14, true, mod, md, i));
            cx += w + 3;
        }
        return y + ROW_MODE + 4;
    }

    private int drawSwatches(GuiGraphics g, Setting.Swatches sw, int y) {
        rr(g, bx, y, bw, ROW_SWATCH, 3, CARD);
        txt(g, sw.name, bx + 7, y + 7, TEXT);
        int n = sw.colors.length;
        int total = n * 10 + (n - 1) * 5;
        int cx = bx + bw - 7 - total;
        int sel = sw.selected();
        for (int i = 0; i < n; i++) {
            if (i == sel) rr(g, cx - 2, y + 4, 14, 14, 4, 0xFFFFFFFF);
            rr(g, cx, y + 6, 10, 10, 3, 0xFF000000 | sw.colors[i]);
            hits.add(new Hit(Kind.SWATCH, cx - 2, y + 4, 14, 14, true, mod, sw, i));
            cx += 15;
        }
        return y + ROW_SWATCH + 4;
    }

    private int drawColor(GuiGraphics g, Setting.ColorPick cp, int y) {
        rr(g, bx, y, bw, ROW_COLOR, 3, CARD);
        int rgb = cp.get();
        txt(g, cp.name, bx + 7, y + 5, TEXT);
        rr(g, bx + bw - 7 - 22, y + 4, 22, 10, 3, 0xFF000000 | rgb);

        int[] tint = {0xFFFF5555, 0xFF55FF55, 0xFF5577FF};
        int tx = bx + 11, tw = bw - 22;
        for (int ch = 0; ch < 3; ch++) {
            int ty = y + 22 + ch * 10;
            int val = (rgb >> (16 - 8 * ch)) & 255;
            track(g, tx, ty, tw, val / 255.0, tint[ch]);
            hits.add(new Hit(Kind.CHANNEL, tx, ty - 4, tw, 11, true, mod, cp, ch));
        }
        return y + ROW_COLOR + 4;
    }

    // ------------------------------------------------------------------ input

    private void applyDrag(Hit h, double mx) {
        double frac = Math.max(0, Math.min(1, (mx - h.x()) / h.w()));
        if (h.kind() == Kind.SLIDER) {
            Setting.Slider s = (Setting.Slider) h.setting();
            s.set(s.min + frac * (s.max - s.min));
        } else if (h.kind() == Kind.CHANNEL) {
            Setting.ColorPick cp = (Setting.ColorPick) h.setting();
            int shift = 16 - 8 * h.idx();
            int v = (int) Math.round(frac * 255);
            cp.set((cp.get() & ~(0xFF << shift)) | (v << shift));
        }
    }

    private void setBind(Module m, int keyCode) {
        KeyMapping km = m.keyMapping;
        if (km == null) return;
        km.setKey(keyCode < 0 ? InputConstants.UNKNOWN : InputConstants.Type.KEYSYM.getOrCreate(keyCode));
        KeyMapping.resetMapping();
        this.minecraft.options.save();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (binding != null) {
            Module m = binding;
            binding = null;
            int key = event.key();
            if (key == 256) return true;                              // ESC = anuluj
            if (key == 259 || key == 261) setBind(m, -1);             // Backspace / Delete = brak bindu
            else setBind(m, key);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= wx && mouseX < wx + ww && mouseY >= wy && mouseY < wy + wh) {
            scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) scrollY * 18f));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int btn = event.button();

        Module wasBinding = binding;
        binding = null;

        boolean inView = mx >= bx && mx < bx + bw && my >= by && my < by + bh;

        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.inside(mx, my)) continue;
            if (h.body() && !inView) continue;

            switch (h.kind()) {
                case NAV -> {
                    if (btn == 0) {
                        cat = h.idx();
                        mod = null;
                        search.setValue("");
                        startPage();
                    }
                }
                case CLOSE -> {
                    if (btn == 0) onClose();
                }
                case BACK -> {
                    if (btn == 0) {
                        mod = null;
                        startPage();
                    }
                }
                case CARD -> {
                    Module m = h.mod();
                    if (btn == 0) {
                        m.toggle();
                        StorageFinderConfig.save();
                    } else if (btn == 1) {
                        mod = m;
                        startPage();
                    } else if (btn == 2) {
                        mod = m;
                        startPage();
                        binding = m;
                    }
                }
                case ENABLED -> {
                    if (btn == 0) {
                        h.mod().toggle();
                        StorageFinderConfig.save();
                    }
                }
                case BOOL -> {
                    if (btn == 0) {
                        Setting.Bool b = (Setting.Bool) h.setting();
                        b.set(!b.get());
                        StorageFinderConfig.save();
                    }
                }
                case MODE -> {
                    if (btn == 0) {
                        ((Setting.Mode) h.setting()).set(h.idx());
                        StorageFinderConfig.save();
                    }
                }
                case SWATCH -> {
                    if (btn == 0) {
                        ((Setting.Swatches) h.setting()).set(h.idx());
                        StorageFinderConfig.save();
                    }
                }
                case SLIDER, CHANNEL -> {
                    if (btn == 0) {
                        dragHit = h;
                        applyDrag(h, mx);
                    }
                }
                case BIND -> {
                    if (btn == 0) binding = (wasBinding == h.mod()) ? null : h.mod();
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragHit != null) {
            applyDrag(dragHit, event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragHit != null) {
            dragHit = null;
            StorageFinderConfig.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        StorageFinderConfig.save();
        super.onClose();
    }
}
