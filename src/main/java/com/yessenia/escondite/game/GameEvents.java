package com.yessenia.escondite.game;

import com.yessenia.escondite.item.ItemFactory;
import com.yessenia.escondite.item.ModItems;
import com.yessenia.escondite.network.AdminNetworkManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
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

        // 3. Clic derecho en el aire (ítems de interfaz y bolas de nieve infinitas)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);

            if (stack.is(ModItems.GESTOR_ARENAS)) {
                if (world.isClientSide()) {
                    return InteractionResultHolder.success(stack);
                }
                if (player instanceof ServerPlayer sp && sp.hasPermissions(2)) {
                    AdminNetworkManager.abrirVentana(sp);
                    return InteractionResultHolder.success(stack);
                }
                return InteractionResultHolder.pass(stack);
            }

            // Bolas de nieve infinitas para Escondidos en partida
            if (stack.is(Items.SNOWBALL)) {
                if (!world.isClientSide() && player instanceof ServerPlayer sp && !sp.isCreative()) {
                    ArenaSession sesion = GameManager.getSesionDeJugador(sp.getUUID());
                    if (sesion != null && sesion.roles.get(sp.getUUID()) == PlayerRole.ESCONDIDO) {
                        int slot = (hand == InteractionHand.OFF_HAND) ? 40 : sp.getInventory().selected;
                        ItemStack copia = stack.copyWithCount(16);
                        sp.getServer().execute(() -> {
                            if (sp.isAlive() && sesion.roles.get(sp.getUUID()) == PlayerRole.ESCONDIDO) {
                                sp.getInventory().setItem(slot, copia);
                            }
                        });
                    }
                }
                return InteractionResultHolder.pass(stack);
            }

            String tipo = ItemFactory.getTipoItem(stack);
            if (tipo.isEmpty()) return InteractionResultHolder.pass(stack);

            // En el cliente: devolver SUCCESS obliga a enviar el paquete al servidor al mirar al aire
            if (world.isClientSide()) {
                if ("preparacion".equals(tipo) || "anfitrion".equals(tipo)
                        || "salir".equals(tipo) || "terminar".equals(tipo)) {
                    return InteractionResultHolder.success(stack);
                }
                return InteractionResultHolder.pass(stack);
            }

            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResultHolder.pass(stack);
            }

            ArenaSession sesion = GameManager.getSesionDeJugador(sp.getUUID());

            switch (tipo) {
                case "preparacion" -> {
                    if (sesion != null) sesion.toggleListo(sp);
                    return InteractionResultHolder.success(stack);
                }
                case "anfitrion" -> {
                    if (sesion != null) sesion.clicGalletaAnfitrion(sp, sp.getServer());
                    return InteractionResultHolder.success(stack);
                }
                case "salir" -> {
                    GameManager.enviarALobbyGeneral(sp, sp.getServer());
                    return InteractionResultHolder.success(stack);
                }
                case "terminar" -> {
                    if (sesion != null) sesion.clicPalaTerminar(sp, sp.getServer());
                    return InteractionResultHolder.success(stack);
                }
            }
            return InteractionResultHolder.pass(stack);
        });

        // 4. Clic derecho sobre bloques (cohetes infinitos e ítems de interfaz)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);

            if (stack.is(ModItems.GESTOR_ARENAS)) {
                if (world.isClientSide()) {
                    return InteractionResult.SUCCESS;
                }
                if (player instanceof ServerPlayer sp && sp.hasPermissions(2)) {
                    AdminNetworkManager.abrirVentana(sp);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            // Cohetes de fuegos artificiales infinitos para Escondidos en partida
            if (stack.is(Items.FIREWORK_ROCKET)) {
                if (!world.isClientSide() && player instanceof ServerPlayer sp && !sp.isCreative()) {
                    ArenaSession sesion = GameManager.getSesionDeJugador(sp.getUUID());
                    if (sesion != null && sesion.roles.get(sp.getUUID()) == PlayerRole.ESCONDIDO) {
                        int slot = (hand == InteractionHand.OFF_HAND) ? 40 : sp.getInventory().selected;
                        ItemStack copia = stack.copyWithCount(16);
                        sp.getServer().execute(() -> {
                            if (sp.isAlive() && sesion.roles.get(sp.getUUID()) == PlayerRole.ESCONDIDO) {
                                sp.getInventory().setItem(slot, copia);
                            }
                        });
                    }
                }
                return InteractionResult.PASS;
            }

            String tipo = ItemFactory.getTipoItem(stack);
            if (tipo.isEmpty()) return InteractionResult.PASS;

            if (world.isClientSide()) {
                if (!player.isCreative() && ("preparacion".equals(tipo) || "anfitrion".equals(tipo)
                        || "salir".equals(tipo) || "terminar".equals(tipo))) {
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            if (player instanceof ServerPlayer sp && !sp.isCreative()) {
                ArenaSession sesion = GameManager.getSesionDeJugador(sp.getUUID());
                switch (tipo) {
                    case "preparacion" -> {
                        if (sesion != null) sesion.toggleListo(sp);
                        return InteractionResult.SUCCESS;
                    }
                    case "anfitrion" -> {
                        if (sesion != null) sesion.clicGalletaAnfitrion(sp, sp.getServer());
                        return InteractionResult.SUCCESS;
                    }
                    case "salir" -> {
                        GameManager.enviarALobbyGeneral(sp, sp.getServer());
                        return InteractionResult.FAIL;
                    }
                    case "terminar" -> {
                        if (sesion != null) sesion.clicPalaTerminar(sp, sp.getServer());
                        return InteractionResult.FAIL;
                    }
                }
            }
            return InteractionResult.PASS;
        });

        // 5. Clic izquierdo en espectador para rotar cámara entre buscadores
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

        // 6. Bloqueo de romper bloques en Supervivencia (Creativo libre)
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            return player.isCreative();
        });

        // 7. Protección contra daño de caída, fuera de partida y fuego amigo
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer victima)) return true;
            if (victima.isCreative()) return true;

            if (source.is(DamageTypeTags.IS_FALL)) {
                return false;
            }

            ArenaSession sesion = GameManager.getSesionDeJugador(victima.getUUID());
            if (sesion == null || sesion.estado != GameState.BUSCANDO) {
                return false;
            }

            Entity atacanteEntity = source.getEntity();
            if (atacanteEntity instanceof Projectile proj && proj.getOwner() != null) {
                atacanteEntity = proj.getOwner();
            }

            if (atacanteEntity instanceof ServerPlayer atacante) {
                PlayerRole rolVictima = sesion.roles.get(victima.getUUID());
                PlayerRole rolAtacante = sesion.roles.get(atacante.getUUID());

                if (rolVictima == rolAtacante || rolVictima == PlayerRole.NINGUNO || rolAtacante == PlayerRole.NINGUNO) {
                    return false;
                }
            }

            return true;
        });

        // 8. Infección instantánea y protección contra pantalla de muerte
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, damageSource, damageAmount) -> {
            if (!(entity instanceof ServerPlayer victima)) return true;

            ArenaSession sesion = GameManager.getSesionDeJugador(victima.getUUID());
            if (sesion != null && sesion.estado == GameState.BUSCANDO) {
                PlayerRole rol = sesion.roles.get(victima.getUUID());
                if (rol == PlayerRole.ESCONDIDO) {
                    ServerPlayer atacante = damageSource.getEntity() instanceof ServerPlayer sp ? sp : null;
                    sesion.infectarEscondido(victima, atacante, victima.getServer());
                    return false;
                } else if (rol == PlayerRole.BUSCADOR) {
                    victima.setHealth(victima.getMaxHealth());
                    return false;
                }
            }
            return true;
        });
    }
}