package com.yessenia.escondite.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record OpenGestorPayload(
        boolean tieneLobbyGeneral,
        List<AdminArenaInfo> arenas
) implements CustomPacketPayload {

    public static final Type<OpenGestorPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("escondite", "open_gestor"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenGestorPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, OpenGestorPayload::tieneLobbyGeneral,
            AdminArenaInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenGestorPayload::arenas,
            OpenGestorPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}