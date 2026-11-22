# Escondite — Minecraft

Mod multijugador de **Escondite (Hide & Seek)** para **Minecraft 1.21.1 (Fabric)** con gestión de arenas, portales interactivos para el lobby, rotación automática de buscadores por rondas, kits personalizados y cambio dinámico de skins sin depender de servidores externos.

---

## ✨ Características Principales

* **Gestión Visual sin Comandos:** Al entrar en Modo Creativo con permisos de operador (`OP 2`), los administradores reciben automáticamente el **Gestor de Arenas**, que abre una interfaz gráfica con pestañas para crear arenas, editarlas, fijar puntos de aparición (spawns), habilitar/deshabilitar salas o eliminarlas.
* **Generación Automática de Arenas:** Construye al instante la arena cerrada con anillo de barreras superior, sala de Sub-Lobby iluminada, sala de espera del Buscador y sala de aparición conectada.
* **Portales Flotantes en el Lobby:** Cada arena habilitada genera una entidad de portal flotante en el Lobby General con su bloque representativo y estado en tiempo real (*En espera*, *Contando* o *En curso*), permitiendo entrar con solo hacer clic.
* **Sistema Multi-Ronda Justo:** Ruleta visual en pantalla que selecciona al Buscador evitando repetir jugadores hasta que todos hayan buscado al menos una vez.
* **Tiempos Escalables por Jugadores:**
  * **2 a 5 jugadores:** 8 minutos.
  * **Cada +5 jugadores adicionales:** +6 minutos extra (14 min para 6–10 jugadores, 20 min para 11–15, etc.).
* **Skins Dinámicas Integradas:** Los **Escondidos** adoptan automáticamente la textura del bloque de la arena y los **Buscadores** reciben su traje especial, sin necesidad de conexión a los servidores de skins de Mojang.
* **HUD Minimalista y Dinámico:**
  * Barra rápida (hotbar) centrada que muestra únicamente los slots con ítems (más 1 slot vacío exclusivo para que los Escondidos puedan dejar la mano libre al camuflarse).
  * Panel lateral en partida con reloj en vivo y lista de Buscadores y Escondidos activos.
  * Colores de equipo en la lista de jugadores y ocultamiento automático de *nametags* para los Escondidos.
* **Infección y Modo Espectador Anclado:**
  * Al ser atrapado, el Escondido no ve pantalla de muerte: pasa al equipo **Buscador**, cumple una breve espera de 5 segundos y entra a ayudar a buscar.
  * Quienes entren tarde a una sala en curso quedan en **Modo Espectador** anclado a la cámara de los Buscadores (alternando entre ellos con clic izquierdo o derecho).
  * **Gracia de reconexión de 60 segundos** si un jugador pierde la conexión durante la partida.

---

## 🎮 Ítems y Kits del Mod

### Ítems de Sala y Administración
| Ítem | Dónde aparece | Función |
| :--- | :--- | :--- |
| **Orbe de Preparación** | Sub-Lobby (Jugadores) | Alterna entre **No Listo** y **Listo** con clic derecho. |
| **Báculo del Anfitrión** | Sub-Lobby / Entre Rondas | Inicia o cancela la cuenta regresiva de la partida o siguiente ronda. |
| **Martillo de Juez** | Entre Rondas (Anfitrión) | Finaliza la partida anticipadamente en lugar de continuar con las rondas siguientes. |
| **Puerta Hogar** | Sub-Lobby / Entre Rondas | Regresa al jugador al Lobby General. |
| **Gestor de arenas** | Modo Creativo (`OP 2`) | Abre la ventana que permite gestionar las arenas. |

### Kits de Partida
* **Buscador:**
  * **Castigo (Látigo):** Inflige 7.0 de daño con *Aspecto Ígneo II* y efecto visual/sonoro de ataque.
  * **3 Pociones arrojadizas de Lentitud** (8 segundos).
  * **3 Pociones arrojadizas de Veneno II** (5 segundos).
* **Escondido:**
  * **Palitroque:** Garrote con *Empuje III* para defenderse.
  * **Bolas de Nieve infinitas**.
  * **Cohetes de fuegos artificiales infinitos**.
  * **Slot libre de camuflaje** para mantener la mano vacía al esconderse.

---

## 📦 Requisitos e Instalación

1. Instala **Minecraft 1.21.1** con **Fabric Loader** en su última versión disponible.
2. Descarga e instala **Fabric API** para `1.21.1`.
3. Coloca el mod `Escondite` dentro de la carpeta `mods/` **tanto en el servidor como en el cliente** de cada jugador.

---

## 🛠️ Guía Rápida de Configuración (Administradores)

1. Entra al servidor con permisos de operador (`OP 2`) y ponte en **Modo Creativo** para recibir el **Gestor de Arenas** en tu inventario.
2. Ve al punto donde quieras establecer el punto de aparición principal y usa el botón **Fijar Lobby General** (o ejecuta `/escondite fijarlobby`).
3. Abre la pestaña **Crear** del Gestor de Arenas, define el **ID**, **Nombre visible**, **Bloque base** (ej. `minecraft:dirt`), **Skin** (ej. `dirt`) y dimensiones (`Ancho`, `Alto`, `Largo`).
4. Decora el interior de la arena generada con bloques, pasadizos, parkoures, puertas secretas, laberintos (El mod Framed o Reframed blocks servirá para construir ideas creativas) donde los jugadores puedan esconderse y camuflarse.
5. Abre la pestaña **Editar**, selecciona la arena y pulsa **Habilitar**. Deberás ir al Lobby General y ubicarte en la posición donde quieres que aparezca su respectivo portal flotante pulsando la opción `Fijar Portal` en la pestaña **Spawn** del **Gestor de Arenas**.

### Comandos Disponibles
| Comando | Permiso | Descripción |
| :--- | :---: | :--- |
| `/escondite fijarlobby` | OP 2 | Guarda tu ubicación y orientación actual como el Lobby General. |
| `/escondite creararena <id> <nombre> <bloque> <skin> <ancho> <alto> <largo>` | OP 2 | Construye una arena nueva y registra sus puntos de aparición. |
| `/escondite fijarspawn <id> <arena\|sublobby\|buscador>` | OP 2 | Reubica cualquiera de los spawns de la arena en tu posición actual. |
| `/escondite habilitar <id> <true\|false>` | OP 2 | Activa o desactiva una arena en el Lobby. |
| `/escondite eliminararena <id>` | OP 2 | Elimina la estructura de la arena del mundo y su configuración. |
| `/escondite entrar <id>` | Todos | Une al jugador a la sala o como espectador si la ronda está en curso. |
| `/lobby` | Todos | Saca al jugador de la arena actual y lo devuelve al Lobby General. |

---

## 🎨 Soporte para Resource Packs y Skins Personalizadas

El mod incluye las siguentes skins: `buscador`, `dirt`, `stone`, `sand` y `bedrock`. Sonidos empaquetados para funcionar y pronto estará disponible un Paquete de Recursos con modelos 3D de los ítems del mod y nuevas skins de distintos bloques para disfrutar con nuevas arenas.

---

## 📄 Licencia

Proyecto desarrollado para servidores multijugador de Minecraft Fabric 1.21.1.