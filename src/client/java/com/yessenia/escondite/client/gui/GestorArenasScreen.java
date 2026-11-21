package com.yessenia.escondite.client.gui;

import com.yessenia.escondite.network.AdminActionPayload;
import com.yessenia.escondite.network.AdminArenaInfo;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class GestorArenasScreen extends Screen {

    private static final ResourceLocation TEXTURA_FONDO =
            ResourceLocation.fromNamespaceAndPath("escondite", "textures/gui/gestor_arenas.png");
    private static final ResourceLocation TEXTURA_BOTON =
            ResourceLocation.fromNamespaceAndPath("escondite", "textures/gui/boton.png");
    private static final ResourceLocation TEXTURA_BOTON_HOVER =
            ResourceLocation.fromNamespaceAndPath("escondite", "textures/gui/boton_hover.png");

    private boolean tieneLobbyGeneral;
    private List<AdminArenaInfo> arenas;

    private int tabActual = 0; // 0: Crear, 1: Editar, 2: Spawns/Portal, 3: Eliminar
    private int indiceArenaSeleccionada = 0;
    private boolean dropdownAbierto = false;
    private boolean confirmarEliminacion = false;

    private EditBox txtId;
    private EditBox txtNombre;
    private EditBox txtBloque;
    private EditBox txtSkin;
    private EditBox txtAncho;
    private EditBox txtAlto;
    private EditBox txtLargo;

    private final int panelW = 260;
    private final int panelH = 205;

    public GestorArenasScreen(boolean tieneLobbyGeneral, List<AdminArenaInfo> arenas) {
        super(Component.literal("Gestor de Arenas"));
        this.tieneLobbyGeneral = tieneLobbyGeneral;
        this.arenas = new ArrayList<>(arenas);
    }

    public void actualizarDatos(boolean tieneLobbyGeneral, List<AdminArenaInfo> nuevasArenas) {
        String idPrevio = (!this.arenas.isEmpty() && indiceArenaSeleccionada < this.arenas.size())
                ? this.arenas.get(indiceArenaSeleccionada).id()
                : null;

        this.tieneLobbyGeneral = tieneLobbyGeneral;
        this.arenas = new ArrayList<>(nuevasArenas);
        this.dropdownAbierto = false;
        this.confirmarEliminacion = false;

        if (idPrevio != null) {
            for (int i = 0; i < this.arenas.size(); i++) {
                if (this.arenas.get(i).id().equals(idPrevio)) {
                    this.indiceArenaSeleccionada = i;
                    break;
                }
            }
        }
        if (indiceArenaSeleccionada >= this.arenas.size()) {
            indiceArenaSeleccionada = Math.max(0, this.arenas.size() - 1);
        }
        this.init();
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        if (indiceArenaSeleccionada >= arenas.size()) {
            indiceArenaSeleccionada = Math.max(0, arenas.size() - 1);
        }

        // Botón Cerrar (X) arriba a la derecha con estilo rojo
        this.addRenderableWidget(new BotonEstiloGestor(
                left + panelW - 20, top + 3, 16, 15,
                Component.literal("§c§l✕"),
                b -> this.onClose(),
                false,
                true
        ));

        // Pestañas superiores
        String[] nombresTabs = {"Crear", "Editar", "Spawns/Portal", "Eliminar"};
        int tabW = (panelW - 20) / 4;
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            boolean activa = (tabActual == idx);
            this.addRenderableWidget(new BotonEstiloGestor(
                    left + 10 + (i * tabW), top + 24, tabW - 2, 18,
                    Component.literal((activa ? "§b§l" : "§7") + nombresTabs[i]),
                    b -> {
                        this.tabActual = idx;
                        this.dropdownAbierto = false;
                        this.confirmarEliminacion = false;
                        this.init();
                    },
                    activa,
                    false
            ));
        }

        // Contenido según pestaña
        if (tabActual == 0) {
            construirTabCrear(left, top);
        } else {
            if (arenas.isEmpty()) {
                return;
            }
            construirSelectorArena(left, top);
            if (!dropdownAbierto) {
                switch (tabActual) {
                    case 1 -> construirTabEditar(left, top);
                    case 2 -> construirTabSpawns(left, top);
                    case 3 -> construirTabEliminar(left, top);
                }
            }
        }
    }

    private void construirTabCrear(int left, int top) {
        int col1X = left + 14;
        int col2X = left + 134;
        int fieldW = 112;

        txtId = crearInput(col1X, top + 58, fieldW, "ID (ej. bosque)", "");
        txtNombre = crearInput(col2X, top + 58, fieldW, "Nombre visible", "");

        txtBloque = crearInput(col1X, top + 92, fieldW, "Bloque (ej. oak_log)", "");
        txtSkin = crearInput(col2X, top + 92, fieldW, "Skin (ej. bosque)", "");

        int dimW = 72;
        txtAncho = crearInput(col1X, top + 126, dimW, "Ancho", "40");
        txtAlto = crearInput(col1X + 80, top + 126, dimW, "Alto", "20");
        txtLargo = crearInput(col1X + 160, top + 126, dimW, "Largo", "40");

        this.addRenderableWidget(crearBoton(
                left + 55, top + 162, 150, 22,
                "§a§lGenerar Arena",
                b -> {
                    int ancho = parseEntero(txtAncho.getValue(), 40);
                    int alto = parseEntero(txtAlto.getValue(), 20);
                    int largo = parseEntero(txtLargo.getValue(), 40);
                    enviarAccion("CREAR", txtId.getValue(), txtNombre.getValue(), txtBloque.getValue(), txtSkin.getValue(), ancho, alto, largo);
                    this.onClose();
                }
        ));
    }

    private void construirSelectorArena(int left, int top) {
        AdminArenaInfo actual = arenas.get(indiceArenaSeleccionada);
        String flecha = dropdownAbierto ? " ▲" : " ▼";
        String estadoTag = actual.habilitada() ? "§a● " : "§c● ";

        this.addRenderableWidget(new BotonEstiloGestor(
                left + 14, top + 48, panelW - 28, 18,
                Component.literal(estadoTag + "§f" + actual.nombre() + " §7(" + actual.id() + ")" + flecha),
                b -> {
                    this.dropdownAbierto = !this.dropdownAbierto;
                    this.confirmarEliminacion = false;
                    this.init();
                },
                dropdownAbierto,
                false
        ));

        if (dropdownAbierto) {
            int maxVisibles = Math.min(arenas.size(), 6);
            for (int i = 0; i < maxVisibles; i++) {
                final int idx = i;
                AdminArenaInfo item = arenas.get(i);
                String punto = item.habilitada() ? "§a● " : "§c● ";
                this.addRenderableWidget(crearBoton(
                        left + 18, top + 68 + (i * 19), panelW - 36, 18,
                        punto + "§f" + item.nombre() + " §8[" + item.id() + "]",
                        b -> {
                            this.indiceArenaSeleccionada = idx;
                            this.dropdownAbierto = false;
                            this.confirmarEliminacion = false;
                            this.init();
                        }
                ));
            }
        }
    }

    private void construirTabEditar(int left, int top) {
        AdminArenaInfo actual = arenas.get(indiceArenaSeleccionada);
        int fieldW = panelW - 28;

        txtNombre = crearInput(left + 14, top + 82, fieldW, "Nombre visible", actual.nombre());
        txtBloque = crearInput(left + 14, top + 114, 112, "Bloque base", actual.bloque());
        txtSkin = crearInput(left + 134, top + 114, 112, "Skin escondido", actual.skin());

        String textoEstado = actual.habilitada() ? "§aEstado: Habilitada" : "§cEstado: Deshabilitada";
        this.addRenderableWidget(crearBoton(
                left + 14, top + 142, 112, 20,
                textoEstado,
                b -> enviarAccion("TOGGLE_HABILITAR", actual.id(), "", "", "", 0, 0, 0)
        ));

        this.addRenderableWidget(crearBoton(
                left + 134, top + 142, 112, 20,
                "§bIr a la Arena",
                b -> {
                    enviarAccion("TP_ARENA", actual.id(), "", "", "", 0, 0, 0);
                    this.onClose();
                }
        ));

        this.addRenderableWidget(crearBoton(
                left + 55, top + 170, 150, 20,
                "§a§lGuardar Cambios",
                b -> enviarAccion("EDITAR", actual.id(), txtNombre.getValue(), txtBloque.getValue(), txtSkin.getValue(), 0, 0, 0)
        ));
    }

    private void construirTabSpawns(int left, int top) {
        AdminArenaInfo actual = arenas.get(indiceArenaSeleccionada);
        int btnW = 112;

        String lobbyTag = tieneLobbyGeneral ? "§eFijar Lobby General ✔" : "§eFijar Lobby General";
        this.addRenderableWidget(crearBoton(
                left + 14, top + 74, panelW - 28, 18,
                lobbyTag,
                b -> enviarAccion("FIJAR_LOBBY_GENERAL", actual.id(), "", "", "", 0, 0, 0)
        ));

        this.addRenderableWidget(crearBoton(
                left + 14, top + 98, btnW, 18,
                "§fSpawn Arena",
                b -> enviarAccion("SPAWN_ARENA", actual.id(), "", "", "", 0, 0, 0)
        ));

        this.addRenderableWidget(crearBoton(
                left + 134, top + 98, btnW, 18,
                "§fSpawn Sub-Lobby",
                b -> enviarAccion("SPAWN_SUBLOBBY", actual.id(), "", "", "", 0, 0, 0)
        ));

        this.addRenderableWidget(crearBoton(
                left + 14, top + 122, btnW, 18,
                "§fSpawn Buscador",
                b -> enviarAccion("SPAWN_BUSCADOR", actual.id(), "", "", "", 0, 0, 0)
        ));

        String portalLabel = actual.tienePortal() ? "§bMover Portal ✔" : "§bFijar Portal Aquí";
        this.addRenderableWidget(crearBoton(
                left + 134, top + 122, btnW, 18,
                portalLabel,
                b -> enviarAccion("FIJAR_PORTAL", actual.id(), "", "", "", 0, 0, 0)
        ));

        if (actual.tienePortal()) {
            this.addRenderableWidget(new BotonEstiloGestor(
                    left + 55, top + 148, 150, 18,
                    Component.literal("§cQuitar Portal Flotante"),
                    b -> enviarAccion("QUITAR_PORTAL", actual.id(), "", "", "", 0, 0, 0),
                    false,
                    true
            ));
        }
    }

    private void construirTabEliminar(int left, int top) {
        AdminArenaInfo actual = arenas.get(indiceArenaSeleccionada);

        if (!confirmarEliminacion) {
            this.addRenderableWidget(new BotonEstiloGestor(
                    left + 35, top + 115, 190, 22,
                    Component.literal("§cEliminar Arena '" + actual.nombre() + "'"),
                    b -> {
                        this.confirmarEliminacion = true;
                        this.init();
                    },
                    false,
                    true
            ));
        } else {
            this.addRenderableWidget(new BotonEstiloGestor(
                    left + 20, top + 125, 108, 22,
                    Component.literal("§4§l¡SÍ, BORRAR TODO!"),
                    b -> {
                        enviarAccion("ELIMINAR", actual.id(), "", "", "", 0, 0, 0);
                        this.confirmarEliminacion = false;
                    },
                    false,
                    true
            ));

            this.addRenderableWidget(crearBoton(
                    left + 134, top + 125, 106, 22,
                    "§7Cancelar",
                    b -> {
                        this.confirmarEliminacion = false;
                        this.init();
                    }
            ));
        }
    }

    private Button crearBoton(int x, int y, int w, int h, String texto, Button.OnPress accion) {
        return new BotonEstiloGestor(x, y, w, h, Component.literal(texto), accion, false, false);
    }

    private EditBox crearInput(int x, int y, int w, String hint, String valorInicial) {
        EditBox box = new EditBox(this.font, x, y, w, 16, Component.literal(hint));
        box.setMaxLength(48);
        box.setValue(valorInicial);
        box.setHint(Component.literal("§8" + hint));
        this.addRenderableWidget(box);
        return box;
    }

    private int parseEntero(String texto, int defecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private void enviarAccion(String accion, String id, String nombre, String bloque, String skin, int ancho, int alto, int largo) {
        ClientPlayNetworking.send(new AdminActionPayload(accion, id, nombre, bloque, skin, ancho, alto, largo));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);

        int left = (this.width - panelW) / 2;
        int top = (this.height - panelH) / 2;

        if (Minecraft.getInstance().getResourceManager().getResource(TEXTURA_FONDO).isPresent()) {
            graphics.blit(TEXTURA_FONDO, left, top, 0, 0, panelW, panelH, panelW, panelH);
        } else {
            graphics.fill(left, top, left + panelW, top + panelH, 0xEE141824);
            graphics.renderOutline(left, top, panelW, panelH, 0xFF38BDF8);
            graphics.fill(left + 1, top + 1, left + panelW - 1, top + 20, 0xFF1E293B);
        }

        graphics.drawCenteredString(this.font, "§b§lGESTOR DE ARENAS", left + panelW / 2 - 6, top + 7, 0xFFFFFF);

        if (tabActual == 0) {
            graphics.drawString(this.font, "§7ID único:", left + 14, top + 48, 0xCCCCCC);
            graphics.drawString(this.font, "§7Nombre:", left + 134, top + 48, 0xCCCCCC);
            graphics.drawString(this.font, "§7Bloque base:", left + 14, top + 82, 0xCCCCCC);
            graphics.drawString(this.font, "§7Archivo skin:", left + 134, top + 82, 0xCCCCCC);
            graphics.drawString(this.font, "§7Ancho:", left + 14, top + 116, 0xCCCCCC);
            graphics.drawString(this.font, "§7Alto:", left + 94, top + 116, 0xCCCCCC);
            graphics.drawString(this.font, "§7Largo:", left + 174, top + 116, 0xCCCCCC);
        } else if (arenas.isEmpty()) {
            graphics.drawCenteredString(this.font, "§7No hay arenas creadas todavía.", left + panelW / 2, top + 95, 0xAAAAAA);
        } else if (!dropdownAbierto) {
            if (tabActual == 1) {
                graphics.drawString(this.font, "§7Nombre visible:", left + 14, top + 72, 0xCCCCCC);
                graphics.drawString(this.font, "§7Bloque:", left + 14, top + 104, 0xCCCCCC);
                graphics.drawString(this.font, "§7Skin:", left + 134, top + 104, 0xCCCCCC);
            } else if (tabActual == 3) {
                AdminArenaInfo actual = arenas.get(indiceArenaSeleccionada);
                graphics.drawCenteredString(this.font, "§eEsta acción borrará la estructura,", left + panelW / 2, top + 82, 0xFFFF55);
                graphics.drawCenteredString(this.font, "§eel portal y los datos de §f" + actual.nombre(), left + panelW / 2, top + 94, 0xFFFF55);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ==================== BOTÓN PERSONALIZADO CON ESTILO DE LA VENTANA ====================

    private static class BotonEstiloGestor extends Button {
        private final boolean seleccionado;
        private final boolean peligro;

        public BotonEstiloGestor(int x, int y, int width, int height, Component message, OnPress onPress, boolean seleccionado, boolean peligro) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
            this.seleccionado = seleccionado;
            this.peligro = peligro;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hover = this.isHoveredOrFocused();
            ResourceLocation tex = hover ? TEXTURA_BOTON_HOVER : TEXTURA_BOTON;

            if (Minecraft.getInstance().getResourceManager().getResource(tex).isPresent()) {
                graphics.blit(tex, this.getX(), this.getY(), 0, 0, this.getWidth(), this.getHeight(), this.getWidth(), this.getHeight());
            } else {
                int fondo;
                int borde;

                if (peligro) {
                    fondo = hover ? 0xFF451A24 : 0xFF2D141C;
                    borde = hover ? 0xFFF87171 : 0xFFEF4444;
                } else if (seleccionado || hover) {
                    fondo = 0xFF26384F;
                    borde = 0xFF38BDF8;
                } else {
                    fondo = 0xFF1E293B;
                    borde = 0xFF475569;
                }

                graphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), fondo);
                graphics.renderOutline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), borde);
            }

            Font font = Minecraft.getInstance().font;
            int colorTexto = this.active ? 0xFFFFFF : 0xA0A0A0;
            this.renderScrollingString(graphics, font, 4, colorTexto | (Mth.ceil(this.alpha * 255.0F) << 24));
        }
    }
}