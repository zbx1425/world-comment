package cn.zbx1425.worldcomment.fabric;

#if MC_VERSION >= "12100"
import cn.zbx1425.worldcomment.ServerPlatform;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CompatPacketRegistry {

    public HashMap<Identifier, CompatPacket> packets = new HashMap<>();
    public HashMap<Identifier, Consumer<FriendlyByteBuf>> packetsS2C = new HashMap<>();
    public HashMap<Identifier, ServerPlatform.C2SPacketHandler> packetsC2S = new HashMap<>();

    public void registerPacket(Identifier resourceLocation) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
    }

    public void registerNetworkReceiverS2C(Identifier resourceLocation, Consumer<FriendlyByteBuf> consumer) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
        packetsS2C.put(resourceLocation, consumer);
    }

    public void registerNetworkReceiverC2S(Identifier resourceLocation, ServerPlatform.C2SPacketHandler consumer) {
        packets.computeIfAbsent(resourceLocation, CompatPacket::new);
        packetsC2S.put(resourceLocation, consumer);
    }

    public void commitCommon() {
        for (Map.Entry<Identifier, CompatPacket> packetEntry : packets.entrySet()) {
            CompatPacket packet = packetEntry.getValue();
            PayloadTypeRegistry.serverboundPlay().register(packet.TYPE, packet.STREAM_CODEC);
            PayloadTypeRegistry.clientboundPlay().register(packet.TYPE, packet.STREAM_CODEC);
        }
        for (Map.Entry<Identifier, ServerPlatform.C2SPacketHandler> packetC2S : packetsC2S.entrySet()) {
            ServerPlatform.C2SPacketHandler handlerC2S = packetC2S.getValue();
            CompatPacket packet = packets.get(packetC2S.getKey());
            ServerPlayNetworking.registerGlobalReceiver(packet.TYPE, (payload, context) -> {
                handlerC2S.handlePacket(context.server(), context.player(), payload.buffer);
            });
        }
    }

    public void commitClient() {
        for (Map.Entry<Identifier, Consumer<FriendlyByteBuf>> packetS2C : packetsS2C.entrySet()) {
            Consumer<FriendlyByteBuf> handlerS2C = packetS2C.getValue();
            CompatPacket packet = packets.get(packetS2C.getKey());
            ClientPlayNetworking.registerGlobalReceiver(packet.TYPE, (payload, context) -> {
                handlerS2C.accept(payload.buffer);
            });
        }
    }

    public void sendS2C(ServerPlayer player, Identifier id, FriendlyByteBuf payload) {
        CompatPacket packet = packets.get(id);
        ServerPlayNetworking.send(player, packet.new Payload(payload));
    }

    public void sendC2S(Identifier id, FriendlyByteBuf payload) {
        CompatPacket packet = packets.get(id);
        ClientPlayNetworking.send(packet.new Payload(payload));
    }
}

#else
public class CompatPacketRegistry {

}

#endif