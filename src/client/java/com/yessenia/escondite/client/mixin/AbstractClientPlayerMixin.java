package com.yessenia.escondite.client.mixin;

import com.yessenia.escondite.client.ClientSkinStorage;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerMixin {

    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void aplicarSkinDinamica(CallbackInfoReturnable<PlayerSkin> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        String skinName = ClientSkinStorage.getSkin(self.getUUID());

        if (skinName != null && !skinName.isEmpty()) {
            PlayerSkin original = cir.getReturnValue();
            ResourceLocation texturaCustom = ResourceLocation.fromNamespaceAndPath("escondite", "textures/skins/" + skinName + ".png");
            PlayerSkin modificada = new PlayerSkin(
                    texturaCustom,
                    original.textureUrl(),
                    null, // Oculta capa para no delatar camuflaje
                    original.elytraTexture(),
                    PlayerSkin.Model.WIDE,
                    original.secure()
            );
            cir.setReturnValue(modificada);
        }
    }
}