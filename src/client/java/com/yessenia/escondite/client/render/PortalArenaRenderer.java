package com.yessenia.escondite.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.yessenia.escondite.entity.PortalArenaEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

public class PortalArenaRenderer extends EntityRenderer<PortalArenaEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public PortalArenaRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.35f;
    }

    @Override
    public void render(PortalArenaEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);

        float tiempo = entity.tiempoCliente + partialTick;
        float flotacion = Mth.sin(tiempo * 0.08f) * 0.12f;
        float giroY = (tiempo * 2.5f) % 360.0f;

        // 1. Renderizar el bloque flotante giratorio
        poseStack.pushPose();
        poseStack.translate(0.0, 0.55 + flotacion, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(giroY));
        poseStack.mulPose(Axis.XP.rotationDegrees(15.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(15.0f));
        poseStack.scale(0.68f, 0.68f, 0.68f);
        poseStack.translate(-0.5, -0.5, -0.5);

        Block bloque = BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(entity.getBloqueBase()));
        if (bloque == Blocks.AIR) {
            bloque = Blocks.STONE;
        }
        BlockState state = bloque.defaultBlockState();
        this.blockRenderer.renderSingleBlock(state, poseStack, buffer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();

        // 2. Renderizar texto holográfico encima (solo dentro de 12 bloques y sin cortes con paredes)
        if (this.entityRenderDispatcher.distanceToSqr(entity) <= 144.0) {
            poseStack.pushPose();
            poseStack.translate(0.0, 1.55 + flotacion, 0.0);
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            poseStack.scale(0.025f, -0.025f, 0.025f);

            Matrix4f matrix = poseStack.last().pose();
            Font font = this.getFont();

            Component linea1 = Component.literal("§b§l" + entity.getNombreArena());
            Component linea2 = Component.literal(entity.getLineaEstado());

            float x1 = -font.width(linea1) / 2.0f;
            float x2 = -font.width(linea2) / 2.0f;

            font.drawInBatch(linea1, x1, -10.0f, 0xFFFFFF, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0x66000000, LightTexture.FULL_BRIGHT);
            font.drawInBatch(linea2, x2, 2.0f, 0xFFFFFF, false, matrix, buffer, Font.DisplayMode.SEE_THROUGH, 0x66000000, LightTexture.FULL_BRIGHT);

            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(PortalArenaEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}