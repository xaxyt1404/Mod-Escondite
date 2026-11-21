package com.yessenia.escondite.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record HudSyncPayload(
        boolean enPartidaActiva,
        String rolLocal,
        String nombreArena,
        String fase,
        int segundosRestantes,
        List<String> buscadores,
        List<String> escondidos
) implements CustomPacketPayload {

    public static final Type<HudSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("escondite", "hud_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HudSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.enPartidaActiva());
                buf.writeUtf(p.rolLocal());
                buf.writeUtf(p.nombreArena());
                buf.writeUtf(p.fase());
                buf.writeVarInt(p.segundosRestantes());
                buf.writeCollection(p.buscadores(), ByteBufCodecs.STRING_UTF8);
                buf.writeCollection(p.escondidos(), ByteBufCodecs.STRING_UTF8);
            },
            buf -> new HudSyncPayload(
                    buf.readBoolean(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readList(ByteBufCodecs.STRING_UTF8),
                    buf.readList(ByteBufCodecs.STRING_UTF8)
            )
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}