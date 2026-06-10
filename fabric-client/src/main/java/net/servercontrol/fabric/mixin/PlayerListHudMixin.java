package net.servercontrol.fabric.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.servercontrol.fabric.ClientRestrictionState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListHud.class)
public final class PlayerListHudMixin {
    @Inject(method = "setVisible", at = @At("HEAD"), cancellable = true)
    private void servercontrol$blockOpening(boolean visible, CallbackInfo ci) {
        if (visible && ClientRestrictionState.isTabListBlocked()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void servercontrol$blockRender(
            DrawContext context,
            int scaledWindowWidth,
            Scoreboard scoreboard,
            @Nullable ScoreboardObjective objective,
            CallbackInfo ci
    ) {
        if (ClientRestrictionState.isTabListBlocked()) {
            ci.cancel();
        }
    }
}
