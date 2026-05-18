package com.gtceuterminal.client.gui.planner;

import com.gtceuterminal.GTCEUTerminalMod;
import com.gtceuterminal.common.data.SchematicData;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = GTCEUTerminalMod.MOD_ID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class PlannerState {

    public static final List<PlacedGhost> ghosts = new ArrayList<>();

    // 3D free-cam
    public static boolean freeCamActive    = false;
    public static List<SchematicData> freeCamSchematics = new ArrayList<>();
    public static int freeCamSelectedIdx   = 0;
    public static int freeCamRotSteps      = 0;
    public static BlockPos freeCamSnapPos  = null;

    public static FreeCamera cameraEntity = null;

    private static double frozenX, frozenY, frozenZ;
    private static float  frozenYaw, frozenPitch;

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ghosts.clear();
            forceExit();
            GTCEUTerminalMod.LOGGER.debug("PlannerState: cleared on level unload");
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) {
        if (freeCamActive) forceExit();
    }

    public static void enterFreeCam(List<SchematicData> schematics, int selectedIdx, int rotSteps) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        freeCamSchematics  = new ArrayList<>(schematics);
        freeCamSelectedIdx = Math.max(0, Math.min(selectedIdx, schematics.size() - 1));
        freeCamRotSteps    = rotSteps;
        freeCamSnapPos     = null;

        frozenX     = mc.player.getX();
        frozenY     = mc.player.getY();
        frozenZ     = mc.player.getZ();
        frozenYaw   = mc.player.getYRot();
        frozenPitch = mc.player.getXRot();

        // Spawn a fake LocalPlayer as the camera entity
        cameraEntity = new FreeCamera(-420);
        cameraEntity.copyPosition(mc.player);
        cameraEntity.setYRot(frozenYaw);
        cameraEntity.setXRot(frozenPitch);
        cameraEntity.yRotO = frozenYaw;
        cameraEntity.xRotO = frozenPitch;
        cameraEntity.spawn();

        mc.setCameraEntity(cameraEntity);

        freeCamActive = true;
    }

    public static void exitFreeCam() {
        if (!freeCamActive) return;
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.moveTo(frozenX, frozenY, frozenZ, frozenYaw, frozenPitch);
            mc.player.setDeltaMovement(Vec3.ZERO);
        }

        if (cameraEntity != null) {
            cameraEntity.despawn();
            cameraEntity = null;
        }
        freeCamActive  = false;
        freeCamSnapPos = null;
    }

    private static void forceExit() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (cameraEntity != null) {
            cameraEntity.despawn();
            cameraEntity = null;
        }
        freeCamActive  = false;
        freeCamSnapPos = null;
    }

    public static void tickFreezePlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.setPosRaw(frozenX, frozenY, frozenZ);
        mc.player.fallDistance = 0f;
    }

    public static class PlacedGhost {
        public final SchematicData schematic;
        public final BlockPos      origin;
        public final int           rotSteps;

        public PlacedGhost(SchematicData schematic, BlockPos origin, int rotSteps) {
            this.schematic = schematic;
            this.origin    = origin;
            this.rotSteps  = rotSteps;
        }
    }

    public static boolean tryRemoveAt(int worldX, int worldZ) {
        for (int i = ghosts.size() - 1; i >= 0; i--) {
            PlacedGhost g = ghosts.get(i);

            BlockPos sz   = g.schematic.getSize();
            int sizeX = (g.rotSteps % 2 == 0) ? sz.getX() : sz.getZ();
            int sizeZ = (g.rotSteps % 2 == 0) ? sz.getZ() : sz.getX();
            int gx = g.origin.getX();
            int gz = g.origin.getZ();
            if (worldX >= gx && worldX < gx + sizeX &&
                    worldZ >= gz && worldZ < gz + sizeZ) {
                ghosts.remove(i);
                return true;
            }
        }
        return false;
    }

    public static boolean isOccupied(int worldX, int worldZ, int rotSteps, SchematicData schematic) {
        for (PlacedGhost g : ghosts) {
            BlockPos sz = g.schematic.getSize();
            int sizeX = (g.rotSteps % 2 == 0) ? sz.getX() : sz.getZ();
            int sizeZ = (g.rotSteps % 2 == 0) ? sz.getZ() : sz.getX();
            int gx = g.origin.getX();
            int gz = g.origin.getZ();
            if (worldX >= gx && worldX < gx + sizeX &&
                    worldZ >= gz && worldZ < gz + sizeZ) {
                return true;
            }
        }
        return false;
    }
}