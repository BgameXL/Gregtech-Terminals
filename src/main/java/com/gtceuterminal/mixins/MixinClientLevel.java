package com.gtceuterminal.mixins;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

import java.lang.reflect.Method;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {

    private static volatile Method cachedAddEntity = null;

    public static void gtceuterminal$addEntity(ClientLevel level, int entityId, Entity entity) {
        Method m = cachedAddEntity;
        if (m == null) {
            m = resolveAddEntity();
            cachedAddEntity = m;
        }
        if (m == null) {
            com.gtceuterminal.GTCEUTerminalMod.LOGGER.error(
                    "[GTCEuTerminals] FreeCamera: could not find ClientLevel.addEntity — " +
                            "free-cam will not work. Please report this as a bug.");
            return;
        }
        try {
            m.invoke(level, entityId, entity);
        } catch (Exception e) {
            com.gtceuterminal.GTCEUTerminalMod.LOGGER.error(
                    "[GTCEuTerminals] FreeCamera: addEntity invocation failed", e);
        }
    }

    private static Method resolveAddEntity() {
        String[] names = { "addEntity", "m_104682_", "func_217411_a" };
        for (String name : names) {
            try {
                Method m = ClientLevel.class.getDeclaredMethod(name, int.class, Entity.class);
                m.setAccessible(true);
                com.gtceuterminal.GTCEUTerminalMod.LOGGER.debug(
                        "[GTCEuTerminals] FreeCamera: resolved addEntity as '{}'", name);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        return null;
    }
}