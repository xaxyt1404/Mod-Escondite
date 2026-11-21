package com.yessenia.escondite.client.mixin;

import com.yessenia.escondite.client.SlotPosition;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Slot.class)
public class SlotPositionMixin implements SlotPosition {

    @Shadow
    @Final
    @Mutable
    public int x;

    @Shadow
    @Final
    @Mutable
    public int y;

    @Override
    public void escondite$setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }
}