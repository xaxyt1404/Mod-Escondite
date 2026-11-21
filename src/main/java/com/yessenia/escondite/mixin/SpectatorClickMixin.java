package com.yessenia.escondite.mixin;

import com.yessenia.escondite.Escondite;
import com.yessenia.escondite.game.ArenaSession;
import com.yessenia.escondite.game.GameManager;
import com.yessenia.escondite.game.PlayerRole;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class SpectatorClickMixin {

    @Shadow public ServerPlayer player;
    @Unique private int escondite$ultimoTickRotacion = 0;

    @Inject(method = "handleAnimate", at = @At("HEAD"))
    private void alHacerClicIzquierdo(ServerboundSwingPacket packet, CallbackInfo ci) {
        if (player == null || player.getServer() == null) return;

        Escondite.activarLatigo(player);

        int tickActual = player.getServer().getTickCount();
        if (tickActual - escondite$ultimoTickRotacion < 5) return;

        ArenaSession sesion = GameManager.getSesionDeJugador(player.getUUID());
        if (sesion != null && sesion.roles.get(player.getUUID()) == PlayerRole.ESPECTADOR_BUSCADOR) {
            escondite$ultimoTickRotacion = tickActual;
            sesion.rotarCamaraEspectador(player, player.getServer());
        }
    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void bloquearMovimientoInventario(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        if (player != null && !player.isCreative()) {
            player.containerMenu.broadcastFullState();
            ci.cancel();
        }
    }
}