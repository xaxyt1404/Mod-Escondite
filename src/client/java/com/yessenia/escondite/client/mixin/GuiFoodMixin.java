package com.yessenia.escondite.client.mixin;

import com.yessenia.escondite.item.ItemFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiFoodMixin {

	@Shadow @Final private Minecraft minecraft;
	@Shadow private int toolHighlightTimer;
	@Shadow private ItemStack lastToolHighlight;

	@Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
	private void ocultarBarraHambre(GuiGraphics guiGraphics, Player player, int y, int x, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "renderSelectedItemName", at = @At("HEAD"))
	private void mantenerNombreEtiquetaPreparacion(GuiGraphics guiGraphics, CallbackInfo ci) {
		if (this.minecraft.player != null) {
			ItemStack enMano = this.minecraft.player.getMainHandItem();
			if ("preparacion".equals(ItemFactory.getTipoItem(enMano))) {
				this.lastToolHighlight = enMano;
				this.toolHighlightTimer = 40;
			}
		}
	}
}