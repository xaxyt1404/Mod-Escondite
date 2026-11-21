package com.yessenia.escondite.client.mixin;

import com.yessenia.escondite.client.ClientHudStorage;
import com.yessenia.escondite.item.ItemFactory;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Gui.class)
public abstract class GuiFoodMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	private int toolHighlightTimer;

	@Shadow
	private ItemStack lastToolHighlight;

	@Shadow
	protected abstract void renderSlot(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed);

	// 1. Mantener siempre visible el nombre del ítem de Preparación mientras está en la mano
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

	// 2. Ocultar barra de hambre
	@Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
	private void esconderBarraComida(GuiGraphics guiGraphics, Player player, int y, int x, CallbackInfo ci) {
		ci.cancel();
	}

	// 3. Ocultar barra y número de experiencia / niveles
	@Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
	private void esconderBarraExperiencia(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
		ci.cancel();
	}

	@Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
	private void esconderNivelExperiencia(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		ci.cancel();
	}

	// 4. Corazones: solo visibles para ESCONDIDOS durante partida activa, y centrados
	@Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
	private void filtrarCorazonesPorRol(
			GuiGraphics guiGraphics, Player player, int x, int y, int height,
			int offsetHeartIndex, float maxHealth, int currentHealth, int displayHealth,
			int absorptionAmount, boolean renderHighlight, CallbackInfo ci
	) {
		boolean mostrarVida = ClientHudStorage.enPartidaActiva && "ESCONDIDO".equals(ClientHudStorage.rolLocal);
		if (!mostrarVida) {
			ci.cancel();
		}
	}

	@ModifyVariable(method = "renderHearts", at = @At("HEAD"), ordinal = 0, argsOnly = true)
	private int centrarCorazonesX(int x) {
		return (this.minecraft.getWindow().getGuiScaledWidth() / 2) - 40;
	}

	// 5. Hotbar dinámica y centrada
	@Inject(method = "renderItemHotbar", at = @At("HEAD"), cancellable = true)
	private void renderizarHotbarDinamica(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		Player player = this.minecraft.player;
		if (player == null || player.isCreative() || player.isSpectator()) {
			return;
		}

		ci.cancel();

		List<Integer> slotsOcupados = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			if (!player.getInventory().getItem(i).isEmpty()) {
				slotsOcupados.add(i);
			}
		}

		if (slotsOcupados.isEmpty()) {
			return;
		}

		if (!slotsOcupados.contains(player.getInventory().selected)) {
			player.getInventory().selected = slotsOcupados.get(0);
		}

		int screenW = guiGraphics.guiWidth();
		int screenH = guiGraphics.guiHeight();
		int paso = 20;
		int totalW = (slotsOcupados.size() * paso) - 2;
		int startX = (screenW - totalW) / 2;
		int y = screenH - 21;

		// Casillas con diseño oscuro/celeste
		for (int idx = 0; idx < slotsOcupados.size(); idx++) {
			int realSlot = slotsOcupados.get(idx);
			boolean seleccionado = (realSlot == player.getInventory().selected);
			int slotX = startX + (idx * paso);

			int fondo = seleccionado ? 0xEE26384F : 0xCC1E293B;
			int borde = seleccionado ? 0xFF38BDF8 : 0x8838BDF8;

			guiGraphics.fill(slotX, y, slotX + 18, y + 18, fondo);
			guiGraphics.renderOutline(slotX, y, 18, 18, borde);
		}

		// Ítems dentro de cada casilla
		int seed = 1;
		for (int idx = 0; idx < slotsOcupados.size(); idx++) {
			int realSlot = slotsOcupados.get(idx);
			int itemX = startX + (idx * paso) + 1;
			int itemY = y + 1;
			this.renderSlot(guiGraphics, itemX, itemY, deltaTracker, player, player.getInventory().getItem(realSlot), seed++);
		}
	}

	// 6. Cuadro lateral dinámico en ancho y largo
	@Inject(method = "render", at = @At("TAIL"))
	private void renderizarPanelEquipos(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (!ClientHudStorage.enPartidaActiva || this.minecraft.options.hideGui) {
			return;
		}

		Font font = this.minecraft.font;
		int screenW = graphics.guiWidth();
		int screenH = graphics.guiHeight();

		int min = ClientHudStorage.segundosRestantes / 60;
		int seg = ClientHudStorage.segundosRestantes % 60;
		String relojTexto = String.format("%02d:%02d", min, seg);
		boolean ultimos30 = ClientHudStorage.segundosRestantes <= 30 && ClientHudStorage.segundosRestantes > 0;

		String headerBuscadores = "Buscadores (" + ClientHudStorage.buscadores.size() + "):";
		String headerEscondidos = "Escondidos (" + ClientHudStorage.escondidos.size() + "):";

		// Calcular ancho dinámicamente según el texto más largo
		int maxTextoW = Math.max(font.width(ClientHudStorage.nombreArena), font.width(ClientHudStorage.fase));
		maxTextoW = Math.max(maxTextoW, (int) Math.ceil(font.width(relojTexto) * 1.15f));
		maxTextoW = Math.max(maxTextoW, font.width(headerBuscadores));
		maxTextoW = Math.max(maxTextoW, font.width(headerEscondidos));

		for (String nombre : ClientHudStorage.buscadores) {
			maxTextoW = Math.max(maxTextoW, font.width("• " + nombre) + 4);
		}
		for (String nombre : ClientHudStorage.escondidos) {
			maxTextoW = Math.max(maxTextoW, font.width("• " + nombre) + 4);
		}

		int anchoPanel = Math.max(82, maxTextoW + 14);

		// Calcular alto dinámicamente según la cantidad de jugadores en ambos equipos
		int filasBuscadores = Math.max(1, ClientHudStorage.buscadores.size());
		int filasEscondidos = Math.max(1, ClientHudStorage.escondidos.size());
		int altoPanel = 66 + ((filasBuscadores + filasEscondidos) * 10);

		int x = screenW - anchoPanel - 5;
		int y = (screenH - altoPanel) / 2;

		// Fondo translúcido y borde celeste
		graphics.fill(x, y, x + anchoPanel, y + altoPanel, 0x7A101520);
		graphics.renderOutline(x, y, anchoPanel, altoPanel, 0xAA38BDF8);
		graphics.fill(x + 1, y + 1, x + anchoPanel - 1, y + 13, 0x881E293B);

		// Arena y Fase
		graphics.drawCenteredString(font, "§b" + ClientHudStorage.nombreArena, x + anchoPanel / 2, y + 3, 0xFFFFFF);
		graphics.drawCenteredString(font, "§7" + ClientHudStorage.fase, x + anchoPanel / 2, y + 16, 0xCCCCCC);

		// Solo el reloj (rojo y ligeramente más grande en los últimos 30 segundos)
		int relojY = y + 27;
		if (ultimos30) {
			graphics.pose().pushPose();
			float escala = 1.12f;
			float cx = x + anchoPanel / 2.0f;
			graphics.pose().translate(cx, relojY, 0);
			graphics.pose().scale(escala, escala, 1.0f);
			graphics.drawCenteredString(font, "§c" + relojTexto, 0, 0, 0xFF5555);
			graphics.pose().popPose();
		} else {
			graphics.drawCenteredString(font, "§a" + relojTexto, x + anchoPanel / 2, relojY, 0x55FF55);
		}

		int cursorY = y + 39;

		// Equipo Buscadores
		graphics.drawString(font, "§c" + headerBuscadores, x + 5, cursorY, 0xFF5555);
		cursorY += 10;
		if (ClientHudStorage.buscadores.isEmpty()) {
			graphics.drawString(font, "§8- Ninguno", x + 8, cursorY, 0x888888);
			cursorY += 10;
		} else {
			for (String nombre : ClientHudStorage.buscadores) {
				graphics.drawString(font, "§c• " + nombre, x + 8, cursorY, 0xFF7777);
				cursorY += 10;
			}
		}

		cursorY += 3;

		// Equipo Escondidos
		graphics.drawString(font, "§a" + headerEscondidos, x + 5, cursorY, 0x55FF55);
		cursorY += 10;
		if (ClientHudStorage.escondidos.isEmpty()) {
			graphics.drawString(font, "§8- Ninguno", x + 8, cursorY, 0x888888);
		} else {
			for (String nombre : ClientHudStorage.escondidos) {
				graphics.drawString(font, "§a• " + nombre, x + 8, cursorY, 0x77FF77);
				cursorY += 10;
			}
		}
	}
}