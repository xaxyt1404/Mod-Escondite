package com.yessenia.escondite.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public class FoodDataMixin {

    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;

    @Inject(method = "tick", at = @At("HEAD"))
    private void mantenerHambreLlena(Player player, CallbackInfo ci) {
        if (!player.isCreative()) {
            this.foodLevel = 20;
            this.saturationLevel = 5.0f;
        }
    }
}