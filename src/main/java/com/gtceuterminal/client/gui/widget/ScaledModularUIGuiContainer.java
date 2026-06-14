package com.gtceuterminal.client.gui.widget;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ScaledModularUIGuiContainer extends ModularUIGuiContainer {

    public ScaledModularUIGuiContainer(ModularUI modularUI, int windowId) {
        super(modularUI, windowId);
    }

    private float fitScale() {
        int uiW = modularUI.getWidth();
        int uiH = modularUI.getHeight();
        if (uiW <= 0 || uiH <= 0) return 1f;
        float s = Math.min((float) width / uiW, (float) height / uiH);
        return Math.min(1f, s);
    }

    private double toUiX(double screenX) {
        float s = fitScale();
        return s >= 1f ? screenX : (screenX - width / 2.0) / s + width / 2.0;
    }

    private double toUiY(double screenY) {
        float s = fitScale();
        return s >= 1f ? screenY : (screenY - height / 2.0) / s + height / 2.0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderBackground(graphics);

        float s = fitScale();
        if (s >= 1f) {
            super.render(graphics, mouseX, mouseY, partialTicks);
            return;
        }
        int mx = (int) toUiX(mouseX);
        int my = (int) toUiY(mouseY);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(width / 2.0, height / 2.0, 0);
        pose.scale(s, s, 1f);
        pose.translate(-width / 2.0, -height / 2.0, 0);
        super.render(graphics, mx, my, partialTicks);
        pose.popPose();
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // intentionally empty — handled in render()
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(toUiX(mouseX), toUiY(mouseY), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(toUiX(mouseX), toUiY(mouseY), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float s = fitScale();
        double dx = s >= 1f ? dragX : dragX / s;
        double dy = s >= 1f ? dragY : dragY / s;
        return super.mouseDragged(toUiX(mouseX), toUiY(mouseY), button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(toUiX(mouseX), toUiY(mouseY), delta);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(toUiX(mouseX), toUiY(mouseY));
    }
}
