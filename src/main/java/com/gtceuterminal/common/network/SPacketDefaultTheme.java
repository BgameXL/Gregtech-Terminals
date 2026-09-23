package com.gtceuterminal.common.network;


import com.gtceuterminal.common.theme.ItemTheme;
import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Server → Client: sends the default theme from server to client on login.
public class SPacketDefaultTheme {

    private final int accentColor;
    private final int bgColor;
    private final int panelColor;
    private final int textColor;
    private final int textMutedColor;
    private final int borderColor;
    private final int successColor;
    private final int warningColor;
    private final int errorColor;
    private final boolean compactMode;
    private final boolean showTooltips;
    private final boolean showBorders;
    private final String wallpaper;

    public SPacketDefaultTheme(ItemTheme theme) {
        this.accentColor = theme.accentColor;
        this.bgColor = theme.bgColor;
        this.panelColor = theme.panelColor;
        this.textColor = theme.textColor;
        this.textMutedColor = theme.textMutedColor;
        this.borderColor = theme.borderColor;
        this.successColor = theme.successColor;
        this.warningColor = theme.warningColor;
        this.errorColor = theme.errorColor;
        this.compactMode = theme.compactMode;
        this.showTooltips = theme.showTooltips;
        this.showBorders = theme.showBorders;
        this.wallpaper = theme.wallpaper != null ? theme.wallpaper : "";
    }

    public SPacketDefaultTheme(FriendlyByteBuf buf) {
        this.accentColor = buf.readInt();
        this.bgColor = buf.readInt();
        this.panelColor = buf.readInt();
        this.textColor = buf.readInt();
        this.textMutedColor = buf.readInt();
        this.borderColor = buf.readInt();
        this.successColor = buf.readInt();
        this.warningColor = buf.readInt();
        this.errorColor = buf.readInt();
        this.compactMode = buf.readBoolean();
        this.showTooltips = buf.readBoolean();
        this.showBorders = buf.readBoolean();
        this.wallpaper = buf.readUtf(256);
    }

    public static void encode(SPacketDefaultTheme msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.accentColor);
        buf.writeInt(msg.bgColor);
        buf.writeInt(msg.panelColor);
        buf.writeInt(msg.textColor);
        buf.writeInt(msg.textMutedColor);
        buf.writeInt(msg.borderColor);
        buf.writeInt(msg.successColor);
        buf.writeInt(msg.warningColor);
        buf.writeInt(msg.errorColor);
        buf.writeBoolean(msg.compactMode);
        buf.writeBoolean(msg.showTooltips);
        buf.writeBoolean(msg.showBorders);
        buf.writeUtf(msg.wallpaper, 256);
    }

    public static void handle(SPacketDefaultTheme msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                        net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () ->
                                com.gtceuterminal.client.network.ClientPacketHandlers.handleDefaultTheme(
                                        msg.accentColor, msg.bgColor, msg.panelColor, msg.textColor,
                                        msg.textMutedColor, msg.borderColor, msg.successColor,
                                        msg.warningColor, msg.errorColor,
                                        msg.compactMode, msg.showTooltips, msg.showBorders, msg.wallpaper)));
        ctx.get().setPacketHandled(true);
    }
}
