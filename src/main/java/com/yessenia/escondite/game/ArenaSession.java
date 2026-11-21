package com.yessenia.escondite.game;

import com.yessenia.escondite.config.ArenaData;
import com.yessenia.escondite.config.ConfigManager;
import com.yessenia.escondite.item.ItemFactory;
import com.yessenia.escondite.network.SkinNetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

import java.util.*;

public class ArenaSession {
    public final ArenaData data;
    public GameState estado = GameState.SUBLOBBY_ESPERA;

    public UUID anfitrion = null;
    public final List<UUID> jugadores = new ArrayList<>();
    public final Set<UUID> listos = new HashSet<>();
    public final Set<UUID> yaFueronBuscadores = new HashSet<>();
    public final Map<UUID, PlayerRole> roles = new HashMap<>();

    // Control de espectadores anclados a buscadores
    public final Map<UUID, Integer> indiceCamaraBuscador = new HashMap<>();

    // Desconectados en ronda activa: UUID -> ticks restantes (1200 ticks = 60s)
    public final Map<UUID, Integer> desconectadosTicks = new HashMap<>();
    public final Map<UUID, PlayerRole> rolesDesconectados = new HashMap<>();

    // Temporizadores internos (en ticks: 20 ticks = 1 segundo)
    private int ticksRestantes = 0;

    // Jugadores recién infectados esperando 5s en la sala 3x3: UUID -> ticks restantes (100 ticks = 5s)
    public final Map<UUID, Integer> infectadosEsperaTicks = new HashMap<>();

    // Variables de la ruleta visual
    private List<UUID> candidatosRuleta = new ArrayList<>();
    private int indiceRuleta = 0;
    private int pasosRuletaTotales = 0;
    private int pasosRuletaActuales = 0;
    private int ticksHastaSiguientePaso = 0;

    private final Random random = new Random();

    public ArenaSession(ArenaData data) {
        this.data = data;
    }

    // ==================== ENTRADA Y SALIDA ====================

    public void agregarJugador(ServerPlayer player, MinecraftServer server) {
        UUID uuid = player.getUUID();
        player.getInventory().clearContent();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.removeAllEffects();

        // Reconexión dentro del minuto de gracia
        if (desconectadosTicks.containsKey(uuid) && enRondaActiva()) {
            desconectadosTicks.remove(uuid);
            PlayerRole rolPrevio = rolesDesconectados.getOrDefault(uuid, PlayerRole.ESPECTADOR_BUSCADOR);
            rolesDesconectados.remove(uuid);

            jugadores.add(uuid);
            roles.put(uuid, rolPrevio);
            actualizarEquipoVisual(player, rolPrevio, server);
            player.setGameMode(GameType.SURVIVAL);

            if (rolPrevio == PlayerRole.BUSCADOR) {
                ItemFactory.darKitBuscador(player, server.registryAccess());
                if (estado == GameState.ESCONDIENDOSE) {
                    data.spawnEsperaBuscador.teleport(player, server);
                } else {
                    data.spawnArena.teleport(player, server);
                }
            } else if (rolPrevio == PlayerRole.ESCONDIDO) {
                ItemFactory.darKitEscondido(player, server.registryAccess());
                data.spawnArena.teleport(player, server);
            } else {
                convertirEnEspectador(player, server);
            }
            return;
        }

        // Si entra durante una ronda activa -> Espectador anclado a buscador
        if (enRondaActiva()) {
            jugadores.add(uuid);
            convertirEnEspectador(player, server);
            return;
        }

        // Si estaba en cuenta regresiva (10s o 5s), se cancela por entrada nueva
        if (estado == GameState.CUENTA_REGRESIVA_INICIO) {
            cancelarCuentaRegresiva(server, "§eCuenta cancelada: entró un jugador.");
        } else if (estado == GameState.CUENTA_REGRESIVA_SORTEO) {
            cancelarCuentaSiguienteRonda(server, "§eCuenta cancelada: entró un jugador.");
        }

        jugadores.add(uuid);
        roles.put(uuid, PlayerRole.NINGUNO);
        actualizarEquipoVisual(player, PlayerRole.NINGUNO, server);
        player.setGameMode(GameType.SURVIVAL);
        data.spawnSubLobby.teleport(player, server);

        if (anfitrion == null) {
            anfitrion = uuid;
        }

        asignarItemsSubLobby(player);
    }

    public void removerJugador(ServerPlayer player, MinecraftServer server, boolean desconexion) {
        UUID uuid = player.getUUID();
        if (!jugadores.contains(uuid)) return;

        jugadores.remove(uuid);
        listos.remove(uuid);
        indiceCamaraBuscador.remove(uuid);
        infectadosEsperaTicks.remove(uuid);
        actualizarEquipoVisual(player, PlayerRole.NINGUNO, server);

        if (player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
            player.setCamera(player);
            player.setGameMode(GameType.SURVIVAL);
        }

        // Si se desconectó en plena ronda, guardar 60s de gracia
        if (desconexion && enRondaActiva()) {
            PlayerRole rol = roles.getOrDefault(uuid, PlayerRole.NINGUNO);
            if (rol == PlayerRole.ESCONDIDO || rol == PlayerRole.BUSCADOR) {
                desconectadosTicks.put(uuid, 1200);
                rolesDesconectados.put(uuid, rol);
            }
        }
        roles.remove(uuid);

        // Si la sala quedó vacía, reiniciar por completo
        if (jugadores.isEmpty() && desconectadosTicks.isEmpty()) {
            reiniciarSalaCompleta();
            return;
        }

        // Relevo de anfitrión si salió el actual
        if (uuid.equals(anfitrion)) {
            anfitrion = jugadores.isEmpty() ? null : jugadores.get(random.nextInt(jugadores.size()));
            if (anfitrion != null) {
                ServerPlayer nuevoAnfitrion = server.getPlayerList().getPlayer(anfitrion);
                if (nuevoAnfitrion != null && (estado == GameState.SUBLOBBY_ESPERA || estado == GameState.ENTRE_RONDAS)) {
                    listos.remove(anfitrion);
                    asignarItemsSubLobby(nuevoAnfitrion);
                    nuevoAnfitrion.displayClientMessage(Component.literal("§6¡Ahora eres el anfitrión de la sala!"), true);
                }
            }
        }

        // Cancelaciones o chequeos de victoria según estado
        if (estado == GameState.CUENTA_REGRESIVA_INICIO) {
            cancelarCuentaRegresiva(server, "§eCuenta cancelada: salió un jugador.");
        } else if (estado == GameState.CUENTA_REGRESIVA_SORTEO) {
            cancelarCuentaSiguienteRonda(server, "§eCuenta cancelada: salió un jugador.");
        } else if (enRondaActiva()) {
            verificarCondicionesFinRonda(server);
        }
    }

    public void asignarItemsSubLobby(ServerPlayer player) {
        player.getInventory().clearContent();
        UUID uuid = player.getUUID();
        boolean esAnfitrion = uuid.equals(anfitrion);

        if (estado == GameState.ENTRE_RONDAS || estado == GameState.CUENTA_REGRESIVA_SORTEO) {
            if (esAnfitrion) {
                boolean contando = (estado == GameState.CUENTA_REGRESIVA_SORTEO);
                player.getInventory().setItem(0, ItemFactory.crearGalletaAnfitrion(contando, true));
                player.getInventory().setItem(4, ItemFactory.crearPalaTerminar());
            }
            player.getInventory().setItem(8, ItemFactory.crearPuertaSalir());
        } else {
            if (esAnfitrion) {
                boolean contando = (estado == GameState.CUENTA_REGRESIVA_INICIO);
                player.getInventory().setItem(0, ItemFactory.crearGalletaAnfitrion(contando, false));
            } else {
                boolean listo = listos.contains(uuid);
                player.getInventory().setItem(0, ItemFactory.crearEtiquetaPreparacion(listo));
            }
            player.getInventory().setItem(8, ItemFactory.crearPuertaSalir());
        }
    }

    // ==================== INTERACCIONES DE SUB-LOBBY ====================

    public void toggleListo(ServerPlayer player) {
        if (estado != GameState.SUBLOBBY_ESPERA) return;
        UUID uuid = player.getUUID();
        if (uuid.equals(anfitrion)) return;

        if (listos.contains(uuid)) {
            listos.remove(uuid);
            player.getInventory().setItem(0, ItemFactory.crearEtiquetaPreparacion(false));
        } else {
            listos.add(uuid);
            player.getInventory().setItem(0, ItemFactory.crearEtiquetaPreparacion(true));
        }
    }

    public void clicGalletaAnfitrion(ServerPlayer player, MinecraftServer server) {
        if (!player.getUUID().equals(anfitrion)) return;

        if (estado == GameState.SUBLOBBY_ESPERA) {
            if (jugadores.size() < 2) {
                player.displayClientMessage(Component.literal("§cSe necesitan al menos 2 jugadores para iniciar."), true);
                return;
            }
            for (UUID id : jugadores) {
                if (!id.equals(anfitrion) && !listos.contains(id)) {
                    player.displayClientMessage(Component.literal("§cFaltan jugadores por marcar Listo."), true);
                    return;
                }
            }
            estado = GameState.CUENTA_REGRESIVA_INICIO;
            ticksRestantes = 200; // 10 segundos
            asignarItemsSubLobby(player);
        } else if (estado == GameState.CUENTA_REGRESIVA_INICIO) {
            cancelarCuentaRegresiva(server, "§cInicio cancelado por el anfitrión.");
        } else if (estado == GameState.ENTRE_RONDAS) {
            if (jugadores.size() < 2) {
                player.displayClientMessage(Component.literal("§cSe necesitan al menos 2 jugadores para la siguiente ronda."), true);
                return;
            }
            estado = GameState.CUENTA_REGRESIVA_SORTEO;
            ticksRestantes = 100; // 5 segundos
            asignarItemsSubLobby(player);
        } else if (estado == GameState.CUENTA_REGRESIVA_SORTEO) {
            cancelarCuentaSiguienteRonda(server, "§cCuenta cancelada por el anfitrión.");
        }
    }

    public void clicPalaTerminar(ServerPlayer player, MinecraftServer server) {
        if (!player.getUUID().equals(anfitrion)) return;
        if (estado != GameState.ENTRE_RONDAS && estado != GameState.CUENTA_REGRESIVA_SORTEO) return;

        yaFueronBuscadores.clear();
        listos.clear();
        estado = GameState.SUBLOBBY_ESPERA;
        ticksRestantes = 0;

        for (UUID id : jugadores) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                roles.put(id, PlayerRole.NINGUNO);
                actualizarEquipoVisual(p, PlayerRole.NINGUNO, server);
                asignarItemsSubLobby(p);
                enviarTitle(p, "§cPARTIDA TERMINADA", "§7De vuelta en espera", 5, 40, 10);
            }
        }
    }

    private void cancelarCuentaRegresiva(MinecraftServer server, String motivo) {
        estado = GameState.SUBLOBBY_ESPERA;
        ticksRestantes = 0;
        for (UUID id : jugadores) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                if (id.equals(anfitrion)) asignarItemsSubLobby(p);
                p.displayClientMessage(Component.literal(motivo), true);
            }
        }
    }

    private void cancelarCuentaSiguienteRonda(MinecraftServer server, String motivo) {
        estado = GameState.ENTRE_RONDAS;
        ticksRestantes = 0;
        for (UUID id : jugadores) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                if (id.equals(anfitrion)) asignarItemsSubLobby(p);
                p.displayClientMessage(Component.literal(motivo), true);
            }
        }
    }

    // ==================== TICK DEL SERVIDOR ====================

    public void tick(MinecraftServer server) {
        // 1. Tick de desconectados en ronda activa
        if (!desconectadosTicks.isEmpty()) {
            var it = desconectadosTicks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Integer> entry = it.next();
                int nuevoTiempo = entry.getValue() - 1;
                if (nuevoTiempo <= 0) {
                    rolesDesconectados.remove(entry.getKey());
                    it.remove();
                    verificarCondicionesFinRonda(server);
                } else {
                    entry.setValue(nuevoTiempo);
                }
            }
        }

        // 2. Mantener anclados a los espectadores en todo momento
        if (enRondaActiva()) {
            actualizarEspectadores(server);
        }

        // 3. Cuenta regresiva individual de 5s para recién infectados en la sala 3x3
        if (!infectadosEsperaTicks.isEmpty()) {
            var itInf = infectadosEsperaTicks.entrySet().iterator();
            while (itInf.hasNext()) {
                Map.Entry<UUID, Integer> entry = itInf.next();
                UUID infId = entry.getKey();
                int rest = entry.getValue();
                ServerPlayer infPlayer = server.getPlayerList().getPlayer(infId);

                if (infPlayer == null || !jugadores.contains(infId)) {
                    itInf.remove();
                    continue;
                }

                if (rest % 20 == 0 && rest > 0) {
                    int seg = rest / 20;
                    enviarTitle(infPlayer, "§c§l¡TE ENCONTRARON!", "§eVolviendo en §f" + seg + "s...", 0, 22, 0);
                    infPlayer.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.2f);
                }

                rest--;
                if (rest <= 0) {
                    itInf.remove();
                    data.spawnArena.teleport(infPlayer, server);
                    enviarTitle(infPlayer, "§c§l¡A BUSCAR!", "", 0, 25, 5);
                    infPlayer.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.MASTER, 0.7f, 1.2f);
                } else {
                    entry.setValue(rest);
                }
            }
        }

        // 4. Máquina de estados
        switch (estado) {
            case CUENTA_REGRESIVA_INICIO -> tickCuentaInicio(server);
            case CUENTA_REGRESIVA_SORTEO -> tickCuentaSorteo(server);
            case SORTEO -> tickRuletaSorteo(server);
            case ESCONDIENDOSE -> tickFaseEscondite(server);
            case BUSCANDO -> tickFaseBusqueda(server);
            default -> {}
        }
    }

    private void tickCuentaInicio(MinecraftServer server) {
        if (ticksRestantes % 20 == 0 && ticksRestantes > 0) {
            int seg = ticksRestantes / 20;
            for (UUID id : jugadores) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    enviarTitle(p, "§6§l" + seg, "§ePreparándose para iniciar...", 0, 22, 0);
                    p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.0f);
                }
            }
        }
        ticksRestantes--;
        if (ticksRestantes <= 0) {
            iniciarRuletaSorteo(server);
        }
    }

    private void tickCuentaSorteo(MinecraftServer server) {
        if (ticksRestantes % 20 == 0 && ticksRestantes > 0) {
            int seg = ticksRestantes / 20;
            for (UUID id : jugadores) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    enviarTitle(p, "§6§l" + seg, "§eSiguiente ronda...", 0, 22, 0);
                    p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.0f);
                }
            }
        }
        ticksRestantes--;
        if (ticksRestantes <= 0) {
            iniciarRuletaSorteo(server);
        }
    }

    public static void actualizarEquipoVisual(ServerPlayer player, PlayerRole rol, MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam equipoActual = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (equipoActual != null) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), equipoActual);
        }

        if (rol == PlayerRole.ESCONDIDO) {
            PlayerTeam teamEsc = scoreboard.getPlayerTeam("escondite_esc");
            if (teamEsc == null) {
                teamEsc = scoreboard.addPlayerTeam("escondite_esc");
            }
            teamEsc.setColor(ChatFormatting.GREEN);
            teamEsc.setNameTagVisibility(Team.Visibility.NEVER);
            scoreboard.addPlayerToTeam(player.getScoreboardName(), teamEsc);

            ArenaSession sesion = GameManager.getSesionDeJugador(player.getUUID());
            String skinArena = (sesion != null) ? sesion.data.skinEscondido : "";
            SkinNetworkManager.setSkinJugador(player, skinArena, server);
        } else if (rol == PlayerRole.BUSCADOR) {
            PlayerTeam teamBus = scoreboard.getPlayerTeam("escondite_bus");
            if (teamBus == null) {
                teamBus = scoreboard.addPlayerTeam("escondite_bus");
            }
            teamBus.setColor(ChatFormatting.RED);
            teamBus.setNameTagVisibility(Team.Visibility.ALWAYS);
            scoreboard.addPlayerToTeam(player.getScoreboardName(), teamBus);

            SkinNetworkManager.setSkinJugador(player, "buscador", server);
        } else {
            SkinNetworkManager.setSkinJugador(player, "", server);
        }
    }

    // ==================== RULETA DE SORTEO ====================

    private void iniciarRuletaSorteo(MinecraftServer server) {
        estado = GameState.SORTEO;
        candidatosRuleta.clear();

        for (UUID id : jugadores) {
            if (!yaFueronBuscadores.contains(id)) {
                candidatosRuleta.add(id);
            }
        }
        if (candidatosRuleta.isEmpty()) {
            candidatosRuleta.addAll(jugadores);
        }

        Collections.shuffle(candidatosRuleta);
        indiceRuleta = 0;
        pasosRuletaActuales = 0;
        pasosRuletaTotales = 18 + random.nextInt(candidatosRuleta.size());
        ticksHastaSiguientePaso = 2;

        for (UUID id : jugadores) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                p.getInventory().clearContent();
                p.displayClientMessage(Component.literal("§bSorteo iniciado"), true);
            }
        }
    }

    private void tickRuletaSorteo(MinecraftServer server) {
        ticksHastaSiguientePaso--;
        if (ticksHastaSiguientePaso > 0) return;

        UUID actualId = candidatosRuleta.get(indiceRuleta % candidatosRuleta.size());
        ServerPlayer actualPlayer = server.getPlayerList().getPlayer(actualId);
        String nombreActual = actualPlayer != null ? actualPlayer.getName().getString() : "???";

        boolean esFinal = (pasosRuletaActuales >= pasosRuletaTotales);

        for (UUID id : jugadores) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                if (esFinal) {
                    enviarTitle(p, "§c§lEl buscador es", "§e§l" + nombreActual, 0, 40, 10);
                    p.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.MASTER, 1.0f, 1.0f);
                } else {
                    enviarTitle(p, "§c§lEl buscador es", "§f" + nombreActual, 0, 15, 0);
                    p.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.MASTER, 0.7f, 1.4f);
                }
            }
        }

        if (esFinal) {
            finalizarSorteoYComenzarEscondite(server, actualId);
        } else {
            indiceRuleta++;
            pasosRuletaActuales++;
            // Desaceleración progresiva
            double progreso = (double) pasosRuletaActuales / pasosRuletaTotales;
            if (progreso < 0.5) ticksHastaSiguientePaso = 2;
            else if (progreso < 0.75) ticksHastaSiguientePaso = 4;
            else if (progreso < 0.9) ticksHastaSiguientePaso = 8;
            else ticksHastaSiguientePaso = 14;
        }
    }

    private void finalizarSorteoYComenzarEscondite(MinecraftServer server, UUID buscadorId) {
        yaFueronBuscadores.add(buscadorId);
        estado = GameState.ESCONDIENDOSE;
        ticksRestantes = 600; // 30 segundos

        for (UUID id : jugadores) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;

            if (id.equals(buscadorId)) {
                roles.put(id, PlayerRole.BUSCADOR);
                actualizarEquipoVisual(p, PlayerRole.BUSCADOR, server);
                ItemFactory.darKitBuscador(p, server.registryAccess());
                p.setGameMode(GameType.SURVIVAL);
                data.spawnEsperaBuscador.teleport(p, server);
            } else if (roles.get(id) == PlayerRole.ESPECTADOR_BUSCADOR) {
                convertirEnEspectador(p, server);
            } else {
                roles.put(id, PlayerRole.ESCONDIDO);
                actualizarEquipoVisual(p, PlayerRole.ESCONDIDO, server);
                ItemFactory.darKitEscondido(p, server.registryAccess());
                p.setGameMode(GameType.SURVIVAL);
                data.spawnArena.teleport(p, server);
            }
        }
    }

    // ==================== FASES DE ESCONDITE Y BÚSQUEDA ====================

    private void tickFaseEscondite(MinecraftServer server) {
        if (ticksRestantes % 20 == 0 && ticksRestantes > 0) {
            int seg = ticksRestantes / 20;
            boolean enGrande = (seg == 30 || seg == 20 || seg == 10);

            for (UUID id : jugadores) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) continue;

                if (enGrande) {
                    enviarTitle(p, "", "§e§l" + seg + " segundos para esconderse", 0, 22, 5);
                    p.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 1.0f, 1.2f);
                } else {
                    p.displayClientMessage(Component.literal("§eEscondiéndose: §f" + seg + "s"), true);
                }
            }
        }

        ticksRestantes--;
        if (ticksRestantes <= 0) {
            iniciarFaseBusqueda(server);
        }
    }

    private void iniciarFaseBusqueda(MinecraftServer server) {
        estado = GameState.BUSCANDO;

        int activos = 0;
        for (PlayerRole rol : roles.values()) {
            if (rol == PlayerRole.ESCONDIDO || rol == PlayerRole.BUSCADOR) activos++;
        }
        int minutos = 8 + (Math.max(0, (activos - 1) / 5) * 6);
        ticksRestantes = minutos * 60 * 20;

        for (UUID id : jugadores) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) continue;

            enviarTitle(p, "§c§l¡A BUSCAR!", "", 5, 30, 10);
            p.playNotifySound(SoundEvents.ENDER_DRAGON_GROWL, SoundSource.MASTER, 0.8f, 1.0f);

            if (roles.get(id) == PlayerRole.BUSCADOR) {
                data.spawnArena.teleport(p, server);
            }
        }
    }

    private void tickFaseBusqueda(MinecraftServer server) {
        if (ticksRestantes % 20 == 0 && ticksRestantes > 0) {
            int totalSeg = ticksRestantes / 20;
            int min = totalSeg / 60;
            int seg = totalSeg % 60;
            String formato = String.format("%02d:%02d", min, seg);

            for (UUID id : jugadores) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) continue;

                if (totalSeg <= 30) {
                    enviarTitle(p, "", "§c§l" + formato, 0, 22, 0);
                    if (totalSeg <= 10) {
                        p.playNotifySound(SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.5f);
                    }
                } else if (roles.get(id) != PlayerRole.ESPECTADOR_BUSCADOR) {
                    p.displayClientMessage(Component.literal("§aTiempo restante: §f" + formato), true);
                }
            }
        }

        ticksRestantes--;
        if (ticksRestantes <= 0) {
            finalizarRonda(server, false); // Ganan los escondidos por tiempo
        }
    }

    public void infectarEscondido(ServerPlayer victima, ServerPlayer atacante, MinecraftServer server) {
        UUID uuid = victima.getUUID();
        if (roles.get(uuid) != PlayerRole.ESCONDIDO) return;

        // 1. Restaurar salud, apagar fuego y limpiar efectos (inmediato y en el siguiente tick)
        victima.setHealth(victima.getMaxHealth());
        victima.setRemainingFireTicks(0);
        victima.clearFire();
        victima.removeAllEffects();
        server.execute(() -> {
            victima.setRemainingFireTicks(0);
            victima.clearFire();
            victima.removeAllEffects();
        });

        // 2. Cambiar rol a BUSCADOR, actualizar equipo TAB y asignar kit
        roles.put(uuid, PlayerRole.BUSCADOR);
        actualizarEquipoVisual(victima, PlayerRole.BUSCADOR, server);
        ItemFactory.darKitBuscador(victima, server.registryAccess());

        // 3. Teletransportar a la sala 3x3 del buscador e iniciar espera de 5s (100 ticks)
        data.spawnEsperaBuscador.teleport(victima, server);
        infectadosEsperaTicks.put(uuid, 100);

        String nombreVictima = victima.getName().getString();
        String nombreAtacante = atacante != null ? atacante.getName().getString() : "un Buscador";

        for (UUID id : jugadores) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                p.displayClientMessage(Component.literal("§c" + nombreVictima + " §ffue atrapado por §c" + nombreAtacante + "§f."), true);
            }
        }

        // 4. Verificar si era el último escondido
        verificarCondicionesFinRonda(server);
    }

    // ==================== ESPECTADOR ANCLADO A BUSCADOR ====================

    public void convertirEnEspectador(ServerPlayer player, MinecraftServer server) {
        UUID uuid = player.getUUID();
        roles.put(uuid, PlayerRole.ESPECTADOR_BUSCADOR);
        player.getInventory().clearContent();
        player.setGameMode(GameType.SPECTATOR);

        if (estado == GameState.SORTEO) {
            data.spawnSubLobby.teleport(player, server);
            player.displayClientMessage(Component.literal("§eEsperando a que finalice el sorteo..."), true);
            return;
        }

        List<ServerPlayer> buscadores = getBuscadoresActivos(server);
        if (!buscadores.isEmpty()) {
            ServerPlayer objetivo = buscadores.get(0);
            indiceCamaraBuscador.put(uuid, 0);
            player.teleportTo(objetivo.serverLevel(), objetivo.getX(), objetivo.getY(), objetivo.getZ(), objetivo.getYRot(), objetivo.getXRot());
            player.setCamera(objetivo);
        }
    }

    public void rotarCamaraEspectador(ServerPlayer espectador, MinecraftServer server) {
        if (roles.get(espectador.getUUID()) != PlayerRole.ESPECTADOR_BUSCADOR) return;
        List<ServerPlayer> buscadores = getBuscadoresActivos(server);
        if (buscadores.isEmpty()) return;

        int actual = indiceCamaraBuscador.getOrDefault(espectador.getUUID(), 0);
        int siguiente = (actual + 1) % buscadores.size();
        indiceCamaraBuscador.put(espectador.getUUID(), siguiente);

        ServerPlayer objetivo = buscadores.get(siguiente);
        espectador.setCamera(objetivo);
        espectador.displayClientMessage(Component.literal("§bViendo a: §f" + objetivo.getName().getString() + " §7| Usa clic izquierdo para alternar entre buscadores"), true);
    }

    private void actualizarEspectadores(MinecraftServer server) {
        if (estado == GameState.SORTEO) return;
        List<ServerPlayer> buscadores = getBuscadoresActivos(server);
        if (buscadores.isEmpty()) return;

        for (UUID id : jugadores) {
            if (roles.get(id) != PlayerRole.ESPECTADOR_BUSCADOR) continue;
            ServerPlayer esp = server.getPlayerList().getPlayer(id);
            if (esp == null) continue;

            if (esp.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
                esp.setGameMode(GameType.SPECTATOR);
            }

            Entity camaraActual = esp.getCamera();
            boolean camaraValida = (camaraActual instanceof ServerPlayer sp) && buscadores.contains(sp);

            if (!camaraValida) {
                int idx = indiceCamaraBuscador.getOrDefault(id, 0) % buscadores.size();
                ServerPlayer objetivo = buscadores.get(idx);
                esp.setCamera(objetivo);
            }

            if (server.getTickCount() % 20 == 0 && esp.getCamera() instanceof ServerPlayer obj) {
                esp.displayClientMessage(Component.literal("§bViendo a: §f" + obj.getName().getString() + " §7| Usa clic izquierdo para alternar entre buscadores"), true);
            }
        }
    }

    public List<ServerPlayer> getBuscadoresActivos(MinecraftServer server) {
        List<ServerPlayer> lista = new ArrayList<>();
        for (UUID id : jugadores) {
            if (roles.get(id) == PlayerRole.BUSCADOR) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) lista.add(p);
            }
        }
        return lista;
    }

    // ==================== FIN DE RONDA Y PARTIDA ====================

    public void verificarCondicionesFinRonda(MinecraftServer server) {
        if (!enRondaActiva()) return;

        int escondidosVivos = 0;
        int buscadoresVivos = 0;

        for (Map.Entry<UUID, PlayerRole> entry : roles.entrySet()) {
            if (entry.getValue() == PlayerRole.ESCONDIDO) escondidosVivos++;
            if (entry.getValue() == PlayerRole.BUSCADOR) buscadoresVivos++;
        }
        for (PlayerRole rolDesc : rolesDesconectados.values()) {
            if (rolDesc == PlayerRole.ESCONDIDO) escondidosVivos++;
            if (rolDesc == PlayerRole.BUSCADOR) buscadoresVivos++;
        }

        if (escondidosVivos == 0) {
            finalizarRonda(server, true); // Ganan buscadores
        } else if (buscadoresVivos == 0) {
            finalizarRonda(server, false); // Ganan escondidos porque no quedan buscadores
        }
    }

    public void finalizarRonda(MinecraftServer server, boolean gananBuscadores) {
        desconectadosTicks.clear();
        rolesDesconectados.clear();
        indiceCamaraBuscador.clear();
        infectadosEsperaTicks.clear();

        String tituloGanador = gananBuscadores ? "§c§l¡GANAN LOS BUSCADORES!" : "§a§l¡GANAN LOS ESCONDIDOS!";

        // Verificar si ya TODOS los presentes fueron buscadores
        boolean todosYaBuscaron = !jugadores.isEmpty();
        for (UUID id : jugadores) {
            if (!yaFueronBuscadores.contains(id)) {
                todosYaBuscaron = false;
                break;
            }
        }

        if (todosYaBuscaron) {
            List<UUID> copia = new ArrayList<>(jugadores);
            reiniciarSalaCompleta();

            for (UUID id : copia) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) continue;
                p.setCamera(p);
                p.setGameMode(GameType.SURVIVAL);
                p.getInventory().clearContent();
                p.setHealth(p.getMaxHealth());
                p.clearFire();
                p.removeAllEffects();

                if (ConfigManager.get().lobbyGeneral != null) {
                    ConfigManager.get().lobbyGeneral.teleport(p, server);
                }
                actualizarEquipoVisual(p, PlayerRole.NINGUNO, server);
                enviarTitle(p, "§6§lPARTIDA TERMINADA", tituloGanador, 5, 60, 15);
            }
        } else {
            estado = GameState.ENTRE_RONDAS;
            for (UUID id : jugadores) {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) continue;
                p.setCamera(p);
                p.setGameMode(GameType.SURVIVAL);
                p.setHealth(p.getMaxHealth());
                p.clearFire();
                p.removeAllEffects();
                roles.put(id, PlayerRole.NINGUNO);

                data.spawnSubLobby.teleport(p, server);
                actualizarEquipoVisual(p, PlayerRole.NINGUNO, server);
                asignarItemsSubLobby(p);
                enviarTitle(p, tituloGanador, "§eEsperando siguiente ronda...", 5, 50, 10);
            }
        }
    }

    public void reiniciarSalaCompleta() {
        estado = GameState.SUBLOBBY_ESPERA;
        anfitrion = null;
        jugadores.clear();
        listos.clear();
        yaFueronBuscadores.clear();
        roles.clear();
        indiceCamaraBuscador.clear();
        desconectadosTicks.clear();
        rolesDesconectados.clear();
        infectadosEsperaTicks.clear();
        ticksRestantes = 0;
    }

    public boolean enRondaActiva() {
        return estado == GameState.SORTEO || estado == GameState.ESCONDIENDOSE || estado == GameState.BUSCANDO;
    }

    public static void enviarTitle(ServerPlayer player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
    }
}