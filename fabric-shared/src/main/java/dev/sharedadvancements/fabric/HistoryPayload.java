package dev.sharedadvancements.fabric;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;


/** One bounded record per packet; the client never sends history to the server. */
public record HistoryPayload(String json) implements CustomPacketPayload {
    public static final Type<HistoryPayload> TYPE = new Type<>(Platform.id());
    public static final StreamCodec<FriendlyByteBuf, HistoryPayload> CODEC = StreamCodec.of(
            (buffer, value) -> buffer.writeUtf(value.json(), 32767), buffer -> new HistoryPayload(buffer.readUtf(32767)));
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
