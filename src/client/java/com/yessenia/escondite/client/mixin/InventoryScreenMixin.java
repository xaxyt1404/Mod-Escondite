package com.yessenia.escondite.client.mixin;

import com.yessenia.escondite.client.SlotPosition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    @Shadow
    private float xMouse;

    @Shadow
    private float yMouse;

    public InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Unique
    private boolean esModoPersonalizado() {
        return this.minecraft != null
                && this.minecraft.player != null
                && !this.minecraft.player.isCreative()
                && !this.minecraft.player.isSpectator();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void limpiarYCentrarInventario(CallbackInfo ci) {
        if (!esModoPersonalizado()) return;

        // Quitar el botón verde del libro de recetas de forma segura
        List<GuiEventListener> aQuitar = new ArrayList<>();
        for (GuiEventListener child : this.children()) {
            if (child instanceof ImageButton) {
                aQuitar.add(child);
            }
        }
        for (GuiEventListener w : aQuitar) {
            this.removeWidget(w);
        }

        this.leftPos = (this.width - this.imageWidth) / 2;
        reordenarSlotsOcupados();
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void actualizarSlotsEnVivo(CallbackInfo ci) {
        if (esModoPersonalizado()) {
            reordenarSlotsOcupados();
        }
    }

    @Unique
    private void reordenarSlotsOcupados() {
        List<Slot> ocupados = new ArrayList<>();
        for (Slot slot : this.menu.slots) {
            if (slot.hasItem()) {
                ocupados.add(slot);
            } else {
                ((SlotPosition) slot).escondite$setPos(-9999, -9999);
            }
        }

        if (!ocupados.isEmpty()) {
            int totalW = ocupados.size() * 18;
            int startX = (this.imageWidth - totalW) / 2 + 1;
            int slotY = 92;
            for (int i = 0; i < ocupados.size(); i++) {
                Slot s = ocupados.get(i);
                ((SlotPosition) s).escondite$setPos(startX + (i * 18), slotY);
            }
        }
    }

    @Inject(method = "renderLabels", at = @At("HEAD"), cancellable = true)
    private void ocultarTextoFabricacion(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (esModoPersonalizado()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBg", at = @At("HEAD"), cancellable = true)
    private void renderizarFondoMinimalista(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        if (!esModoPersonalizado() || this.minecraft == null) return;
        Player player = this.minecraft.player;
        if (player == null) return;

        ci.cancel();

        int cx = this.leftPos + (this.imageWidth / 2);

        // Recuadro del personaje centrado
        int boxW = 58;
        int boxH = 76;
        int boxX = cx - (boxW / 2);
        int boxY = this.topPos + 8;

        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA101520);
        graphics.renderOutline(boxX, boxY, boxW, boxH, 0xCC38BDF8);

        // Renderizar jugador 3D siguiendo el cursor dentro del recuadro
        InventoryScreen.renderEntityInInventoryFollowsMouse(
                graphics,
                boxX + 4, boxY + 4,
                boxX + boxW - 4, boxY + boxH - 4,
                30, 0.0625F,
                this.xMouse, this.yMouse,
                player
        );

        // Dibujar casillas únicamente para los slots ocupados
        for (Slot slot : this.menu.slots) {
            if (slot.hasItem() && slot.x > -1000) {
                int sx = this.leftPos + slot.x - 1;
                int sy = this.topPos + slot.y - 1;
                graphics.fill(sx, sy, sx + 18, sy + 18, 0xCC1E293B);
                graphics.renderOutline(sx, sy, 18, 18, 0xCC38BDF8);
            }
        }
    }
}