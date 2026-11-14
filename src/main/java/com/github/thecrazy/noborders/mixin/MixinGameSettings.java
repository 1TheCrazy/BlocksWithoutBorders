package com.github.thecrazy.noborders.mixin;

import com.github.thecrazy.noborders.WindowModeAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameSettings.class)
public abstract class MixinGameSettings {
    @Inject(method = "setOptionValue", at = @At("HEAD"), cancellable = true)
    private void noborders$cycleMode(GameSettings.Options option, int increment, CallbackInfo ci) {
        if (option == GameSettings.Options.USE_FULLSCREEN) {
            Minecraft.getMinecraft().toggleFullscreen();
            ci.cancel();
        }
    }

    @Inject(method = "getKeyBinding", at = @At("HEAD"), cancellable = true)
    private void noborders$modeLabel(GameSettings.Options option, CallbackInfoReturnable<String> cir) {
        if (option == GameSettings.Options.USE_FULLSCREEN) {
            WindowModeAccess access = (WindowModeAccess) Minecraft.getMinecraft();
            cir.setReturnValue(I18n.format("noborders.displayMode") + ": "
                    + I18n.format(access.noborders$getMode().translationKey()));
        }
    }
}

