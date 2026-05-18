package com.gtceuterminal.common.network;

import com.gtceuterminal.common.theme.DefaultThemeConfig;
import com.gtceuterminal.common.theme.ItemTheme;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

// Server → Client: sends the default theme from server to client on login.
public class SPacketDefaultTheme {

    private final int     accentColor;
    private final int     bgColor;
    private final int     panelColor;
    private final int     textColor;
    private final boolean compactMode;
    private final boolean showTooltips;
    private final boolean showBorders;
    private final String  wallpaper;

    public SPacketDefaultTheme(ItemTheme theme) {
        this.accentColor  = theme.accentColor;
        this.bgColor      = theme.bgColor;
        this.panelColor   = theme.panelColor;
        this.textColor    = theme.textColor;
        this.compactMode  = theme.compactMode;
        this.showTooltips = theme.showTooltips;
        this.showBorders  = theme.showBorders;
        this.wallpaper    = theme.wallpaper != null ? theme.wallpaper : "";
    }

    public SPacketDefaultTheme(FriendlyByteBuf buf) {
        this.accentColor  = buf.readInt();
        this.bgColor      = buf.readInt();
        this.panelColor   = buf.readInt();
        this.textColor    = buf.readInt();
        this.compactMode  = buf.readBoolean();
        this.showTooltips = buf.readBoolean();
        this.showBorders  = buf.readBoolean();
        this.wallpaper    = buf.readUtf(256);
    }

    public static void encode(SPacketDefaultTheme msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.accentColor);
        buf.writeInt(msg.bgColor);
        buf.writeInt(msg.panelColor);
        buf.writeInt(msg.textColor);
        buf.writeBoolean(msg.compactMode);
        buf.writeBoolean(msg.showTooltips);
        buf.writeBoolean(msg.showBorders);
        buf.writeUtf(msg.wallpaper, 256);
    }

    public static void handle(SPacketDefaultTheme msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ItemTheme theme = new ItemTheme();
            theme.accentColor  = msg.accentColor;
            theme.bgColor      = msg.bgColor;
            theme.panelColor   = msg.panelColor;
            theme.textColor    = msg.textColor;
            theme.compactMode  = msg.compactMode;
            theme.showTooltips = msg.showTooltips;
            theme.showBorders  = msg.showBorders;
            theme.wallpaper    = msg.wallpaper;
            DefaultThemeConfig.setClientOverride(theme);
        });
        ctx.get().setPacketHandled(true);
    }
}