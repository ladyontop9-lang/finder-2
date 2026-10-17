package com.example.storagefinder;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;

/**
 * Freecam: kamera lata swobodnie, a Twoja postać stoi w miejscu.
 * Kamera to client-side Marker ustawiany jako camera entity; klawisze ruchu są
 * blokowane dla prawdziwego gracza na czas ticka, a obrót myszką trafia do kamery.
 */
public final class Freecam {
    private static boolean active = false;
    private static Marker cam;
    private static double x, y, z;
    private static float yaw, pitch, frozenYaw, frozenPitch;

    private static KeyMapping[] blocked;
    private static final boolean[] saved = new boolean[9];

    private Freecam() {}

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(Freecam::tickStart);
        ClientTickEvents.END_CLIENT_TICK.register(Freecam::tickEnd);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(ctx -> frame(Minecraft.getInstance()));
    }

    public static boolean isActive() { return active; }

    public static void setActive(boolean on) {
        if (on == active) return;
        Minecraft mc = Minecraft.getInstance();
        if (on) enable(mc); else disable(mc);
    }

    private static void enable(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        Vec3 eye = p.getEyePosition();
        x = eye.x; y = eye.y; z = eye.z;
        yaw = p.getYRot(); pitch = p.getXRot();
        frozenYaw = yaw; frozenPitch = pitch;

        cam = new Marker(EntityType.MARKER, mc.level);
        apply();
        mc.setCameraEntity(cam);
        active = true;
    }

    private static void disable(Minecraft mc) {
        active = false;
        if (mc.player != null) mc.setCameraEntity(mc.player);
        cam = null;
    }

    private static KeyMapping[] keys(Options o) {
        if (blocked == null) {
            blocked = new KeyMapping[]{o.keyUp, o.keyDown, o.keyLeft, o.keyRight,
                    o.keyJump, o.keyShift, o.keySprint, o.keyAttack, o.keyUse};
        }
        return blocked;
    }

    private static void tickStart(Minecraft mc) {
        if (!active) return;
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            disable(mc);
            return;
        }
        syncRotation(p);

        KeyMapping[] ks = keys(mc.options);
        for (int i = 0; i < ks.length; i++) {
            saved[i] = ks[i].isDown();
            ks[i].setDown(false);
        }
        while (mc.options.keyAttack.consumeClick()) { }
        while (mc.options.keyUse.consumeClick()) { }
    }

    private static void tickEnd(Minecraft mc) {
        if (!active) return;
        KeyMapping[] ks = keys(mc.options);
        for (int i = 0; i < ks.length; i++) {
            ks[i].setDown(saved[i]);
        }
    }

    /** Obrót wykonany myszką trafia do kamery, a prawdziwy gracz zostaje z zamrożonym obrotem. */
    private static void syncRotation(LocalPlayer p) {
        yaw += p.getYRot() - frozenYaw;
        pitch = Mth.clamp(pitch + (p.getXRot() - frozenPitch), -90f, 90f);
        p.setYRot(frozenYaw);
        p.setXRot(frozenPitch);
        p.yRotO = frozenYaw;
        p.xRotO = frozenPitch;
    }

    private static void frame(Minecraft mc) {
        if (!active || cam == null) return;
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        syncRotation(p);

        if (mc.screen == null) {
            Options o = mc.options;
            double dt = Math.min(mc.getDeltaTracker().getRealtimeDeltaTicks(), 5.0f) / 20.0;
            double speed = StorageFinderConfig.freecamSpeed * 3.0 * (o.keySprint.isDown() ? 2.0 : 1.0);

            double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
            double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
            double rx = -Math.cos(yr), rz = -Math.sin(yr);

            double mx = 0, my = 0, mz = 0;
            if (o.keyUp.isDown()) { mx += fx; my += fy; mz += fz; }
            if (o.keyDown.isDown()) { mx -= fx; my -= fy; mz -= fz; }
            if (o.keyRight.isDown()) { mx += rx; mz += rz; }
            if (o.keyLeft.isDown()) { mx -= rx; mz -= rz; }
            if (o.keyJump.isDown()) my += 1;
            if (o.keyShift.isDown()) my -= 1;

            double len = Math.sqrt(mx * mx + my * my + mz * mz);
            if (len > 1e-6) {
                x += mx / len * speed * dt;
                y += my / len * speed * dt;
                z += mz / len * speed * dt;
            }
        }
        apply();
    }

    private static void apply() {
        cam.setPos(x, y, z);
        cam.setYRot(yaw);
        cam.setXRot(pitch);
        cam.setOldPosAndRot();
    }
}
