package com.yessenia.escondite.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yessenia.escondite.client.ClientSkinStorage;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void ocultarSombraCamuflaje(PoseStack poseStack, MultiBufferSource bufferSource, Entity entity, float weight, float partialTick, LevelReader level, float radius, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player) {
            String skin = ClientSkinStorage.getSkin(player.getUUID());
            if (skin != null && !skin.isEmpty() && !"buscador".equals(skin)) {
                ci.cancel();
            }
        }
    }
}