package com.yessenia.escondite.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent LATIGO_ATAQUE = registrar("latigo_ataque");
    public static final SoundEvent PORTAL_ENTRAR = registrar("portal_entrar");
    public static final SoundEvent ORBE_LISTO = registrar("orbe_listo");
    public static final SoundEvent ORBE_NO_LISTO = registrar("orbe_no_listo");
    public static final SoundEvent RULETA_TICK = registrar("ruleta_tick");
    public static final SoundEvent RULETA_GANADOR = registrar("ruleta_ganador");
    public static final SoundEvent ATRAPADO = registrar("atrapado");

    private static SoundEvent registrar(String nombre) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("escondite", nombre);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void register() {
        // Fuerza la carga estática de los eventos de sonido
    }
}