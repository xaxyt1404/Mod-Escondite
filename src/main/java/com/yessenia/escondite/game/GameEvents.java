package com.yessenia.escondite.game;

import com.yessenia.escondite.item.ItemFactory;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;

public class GameEvents {

    public static void register() {
        // 1. Tick del servidor
        ServerTickEvents.END_SERVER_TICK.register(GameManager::tick);

        // 2. Conexión y desconexión de jugadores
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            GameManager.alEntrarServidor(handler.player, server);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            GameManager.salirArenaActual(handler.player, server, true);
        });

        // 3. Clic derecho en ítems de interfaz y rotación de cámara espectador
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer sp && !sp.isCreative()) {
                ItemStack stack = sp.getItemInHand(hand);
                String tipo = ItemFactory.getTipoItem(stack);

                if ("puerta_salir".equals(tipo)) {
                    GameManager.enviarALobbyGeneral(sp, sp.getServer());
                    return InteractionResult.FAIL; // Cancela poner la puerta en el suelo
                }

                if ("pala_terminar".equals(tipo)) {
                    ArenaSession sesion = GameManager.getSesionDeJugador(sp.getUUID());
                    if (sesion != null) {
                        sesion.clicPalaTerminar(sp, sp.getServer());
                    }
                    return InteractionResult.FAIL; // Cancela aplanar la tierra
                }

                if ("cohete_infinito".equals(tipo)) {
                    sp.getServer().execute(() -> {
                        sp.getInventory().setItem(2, stack.copyWithCount(16));
                    });
                }
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer sp && !sp.isCreative()) {
                ItemStack stack = sp.getItemInHand(hand);
                String tipo = ItemFactory.getTipoItem(stack);

                if ("puerta_salir".equals(tipo)) {
                    GameManager.enviarALobbyGeneral(sp, sp.getServer());
                    return InteractionResult.FAIL; // Cancela poner la puerta en el suelo
                }

                if ("cohete_infinito".equals(ItemFactory.getTipoItem(stack))) {
                    sp.getServer().execute(() -> {
                        sp.getInventory().setItem(2, stack.copyWithCount(16));
                    });
                }
            }
            return InteractionResult.PASS;
        });

        // 4. Clic izquierdo en espectador para rotar cámara entre buscadores
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer sp) {
                ArenaSession sesion = GameManager.getSesionDeJugador(sp.getUUID());
                if (sesion != null && sesion.roles.get(sp.getUUID()) == PlayerRole.ESPECTADOR_BUSCADOR) {
                    sesion.rotarCamaraEspectador(sp, sp.getServer());
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClientSide() && player instanceof ServerPlayer sp) {
                ArenaSession sesion = GameManager.getSesionDeJugador(sp.getUUID());
                if (sesion != null && sesion.roles.get(sp.getUUID()) == PlayerRole.ESPECTADOR_BUSCADOR) {
                    sesion.rotarCamaraEspectador(sp, sp.getServer());
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.PASS;
        });

        // 5. Bloqueo de romper bloques en Supervivencia (Creativo libre)
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            return player.isCreative();
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer victima)) return true;
            if (victima.isCreative()) return true;

            // 1. Sin daño de caída para nadie
            if (source.is(DamageTypeTags.IS_FALL)) {
                return false;
            }

            ArenaSession sesion = GameManager.getSesionDeJugador(victima.getUUID());
            // En Lobby General o fuera de búsqueda: invulnerables al PvP
            if (sesion == null || sesion.estado != GameState.BUSCANDO) {
                return false;
            }

            // 2. Verificar fuego amigo entre jugadores
            Entity atacanteEntity = source.getEntity();
            if (atacanteEntity instanceof Projectile proj && proj.getOwner() != null) {
                atacanteEntity = proj.getOwner();
            }

            if (atacanteEntity instanceof ServerPlayer atacante) {
                PlayerRole rolVictima = sesion.roles.get(victima.getUUID());
                PlayerRole rolAtacante = sesion.roles.get(atacante.getUUID());

                // Solo existe daño/empuje entre equipos opuestos (BUSCADOR vs ESCONDIDO)
                if (rolVictima == rolAtacante || rolVictima == PlayerRole.NINGUNO || rolAtacante == PlayerRole.NINGUNO) {
                    return false;
                }
            }

            return true;
        });

        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayer victima)) return true;

            ArenaSession sesion = GameManager.getSesionDeJugador(victima.getUUID());
            if (sesion != null && sesion.estado == GameState.BUSCANDO) {
                PlayerRole rol = sesion.roles.get(victima.getUUID());
                if (rol == PlayerRole.ESCONDIDO) {
                    ServerPlayer atacante = damageSource.getEntity() instanceof ServerPlayer sp ? sp : null;
                    sesion.infectarEscondido(victima, atacante, victima.getServer());
                    return false; // Cancela por completo la muerte nativa y la pantalla roja
                } else if (rol == PlayerRole.BUSCADOR) {
                    victima.setHealth(victima.getMaxHealth());
                    return false;
                }
            }
            return true;
        });
    }
}