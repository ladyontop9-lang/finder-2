package com.example.storagefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Automatycznie atakuje najbliższy cel w zasięgu.
 * Poprawki: dystans liczony jak na serwerze (oko -> hitbox), pomijanie celów w "i-frames"
 * (hurtTime), czekanie na pełny cooldown, pomijanie graczy w creative/spectator, opcjonalny line-of-sight.
 */
public final class Aura {
    private static boolean enabled = false;
    private static int cooldown = 0;

    private Aura() {}

    public static boolean isEnabled() { return enabled; }
    public static void setEnabled(boolean v) { enabled = v; cooldown = 0; }

    public static void tick(Minecraft mc) {
        if (!enabled) return;

        LocalPlayer p = mc.player;
        ClientLevel level = mc.level;
        MultiPlayerGameMode gm = mc.gameMode;
        if (p == null || level == null || gm == null || mc.screen != null || !p.isAlive()) return;
        if (p.isUsingItem()) return;

        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (StorageFinderConfig.auraCooldown && p.getAttackStrengthScale(0.0f) < 0.95f) return;

        double range = StorageFinderConfig.auraRange;
        double r2 = range * range;
        boolean playersOnly = StorageFinderConfig.auraPlayersOnly;
        boolean walls = StorageFinderConfig.auraWalls;
        Vec3 eye = p.getEyePosition();

        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(range + 1.0))) {
            if (e == p || !e.isAlive() || e.isSpectator() || !e.isAttackable()) continue;
            if (e.hurtTime > 1) continue;                       // cel ma jeszcze i-frames
            if (playersOnly && !(e instanceof Player)) continue;
            if (e instanceof Player pl && (pl.isCreative() || pl.isSpectator())) continue;

            double d2 = e.getBoundingBox().distanceToSqr(eye);  // tak jak liczy to serwer
            if (d2 > r2 || d2 >= bestD) continue;
            if (!walls && !p.hasLineOfSight(e)) continue;

            best = e;
            bestD = d2;
        }
        if (best == null) return;

        if (StorageFinderConfig.auraRotate && !Freecam.isActive()) {
            Vec3 t = best.getBoundingBox().getCenter();
            double dx = t.x - eye.x, dy = t.y - eye.y, dz = t.z - eye.z;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            p.setYRot((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0));
            p.setXRot((float) -Math.toDegrees(Math.atan2(dy, horiz)));
        }

        gm.attack(p, best);
        p.swing(InteractionHand.MAIN_HAND);
        cooldown = Math.max(0, Math.round(20f / (float) StorageFinderConfig.auraSpeed) - 1);
    }
}
