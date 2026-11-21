package com.yessenia.escondite.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record AdminActionPayload(
        String accion,
        String id,
        String nombre,
        String bloque,
        String skin,
        int ancho,
        int alto,
        int largo
) implements CustomPacketPayload {

    public static final Type<AdminActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("escondite", "admin_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AdminActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.accion());
                buf.writeUtf(payload.id());
                buf.writeUtf(payload.nombre());
                buf.writeUtf(payload.bloque());
                buf.writeUtf(payload.skin());
                buf.writeVarInt(payload.ancho());
                buf.writeVarInt(payload.alto());
                buf.writeVarInt(payload.largo());
            },
            buf -> new AdminActionPayload(
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readUtf(),
                    buf.readVarInt(),
                    buf.readVarInt(),
                    buf.readVarInt()
            )
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}