package com.gtceuterminal.client.gui.planner;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A fake LocalPlayer used as the free-cam entity.
 * By extending LocalPlayer, Minecraft's MouseHandler.turnPlayer() naturally
 * routes mouse look to this entity — no hacks, no feedback loops.
 *
 * Based on the Freecam mod by hashalite, Thank You (MIT License).
 */
public class FreeCamera extends LocalPlayer {

    public FreeCamera(int entityId) {
        super(
                Minecraft.getInstance(),
                (ClientLevel) Minecraft.getInstance().level,
                Minecraft.getInstance().player.connection,
                Minecraft.getInstance().player.getStats(),
                Minecraft.getInstance().player.getRecipeBook(),
                false,  // isHardcore
                false   // wasShiftKeyDown
        );
        setId(entityId);
        setPose(Pose.SWIMMING);
        getAbilities().flying = true;
        getAbilities().mayfly  = true;
        this.input = new KeyboardInput(Minecraft.getInstance().options);
    }

    public void spawn() {
        com.gtceuterminal.mixins.MixinClientLevel.gtceuterminal$addEntity(
                (net.minecraft.client.multiplayer.ClientLevel) level(), getId(), this);
    }

    public void despawn() {
        ((net.minecraft.client.multiplayer.ClientLevel) level())
                .removeEntity(getId(), RemovalReason.DISCARDED);
    }

    @Override
    protected void checkFallDamage(double h, boolean onGround, BlockState s, BlockPos p) { }

    @Override public boolean onClimbable() { return false; }
    @Override public boolean isInWater()   { return false; }

    @Override
    public void aiStep() {
        getAbilities().flying = true;
        getAbilities().mayfly  = true;
        setOnGround(false);
        fallDistance = 0f;

        if (input != null) {
            input.tick(false, 0f);
        }

        getAbilities().setFlyingSpeed(0.05f);

        travel(new Vec3(input != null ? input.leftImpulse : 0f,
                0f,
                input != null ? input.forwardImpulse : 0f));
    }

    @Override
    public void tick() {
        super.tick();

        xOld    = getX();
        yOld    = getY();
        zOld    = getZ();
        xRotO   = getXRot();
        yRotO   = getYRot();
    }

    @Override
    public void setPose(Pose pose) {
        super.setPose(pose);
    }
}