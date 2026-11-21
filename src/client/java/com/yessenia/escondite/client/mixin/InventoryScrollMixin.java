package com.yessenia.escondite.client.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public abstract class InventoryScrollMixin {

    @Shadow
    public int selected;

    @Shadow
    @Final
    public Player player;

    @Inject(method = "swapPaint", at = @At("HEAD"), cancellable = true)
    private void saltarSlotsVacios(double direction, CallbackInfo ci) {
        if (this.player == null || this.player.isCreative() || this.player.isSpectator()) {
            return;
        }

        Inventory inv = (Inventory) (Object) this;
        int paso = direction > 0 ? -1 : 1;
        int candidato = this.selected;

        for (int intentos = 0; intentos < 9; intentos++) {
            candidato = (candidato + paso + 9) % 9;
            if (!inv.getItem(candidato).isEmpty()) {
                this.selected = candidato;
                ci.cancel();
                return;
            }
        }
        ci.cancel();
    }
}