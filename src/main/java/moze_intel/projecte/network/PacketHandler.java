package moze_intel.projecte.network;

import io.netty.buffer.Unpooled;
import java.util.Optional;
import java.util.function.Function;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.emc.EMCMappingHandler;
import moze_intel.projecte.emc.FuelMapper;
import moze_intel.projecte.network.packets.IPEPacket;
import moze_intel.projecte.network.packets.to_client.CooldownResetPKT;
import moze_intel.projecte.network.packets.to_client.SetFlyPKT;
import moze_intel.projecte.network.packets.to_client.StepHeightPKT;
import moze_intel.projecte.network.packets.to_client.SyncBagDataPKT;
import moze_intel.projecte.network.packets.to_client.SyncEmcPKT;
import moze_intel.projecte.network.packets.to_client.SyncEmcPKT.EmcPKTInfo;
import moze_intel.projecte.network.packets.to_client.SyncFuelMapperPKT;
import moze_intel.projecte.network.packets.to_client.UpdateCondenserLockPKT;
import moze_intel.projecte.network.packets.to_client.UpdateWindowIntPKT;
import moze_intel.projecte.network.packets.to_client.UpdateWindowLongPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeClearPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncChangePKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncEmcPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncInputsAndLocksPKT;
import moze_intel.projecte.network.packets.to_client.knowledge.KnowledgeSyncPKT;
import moze_intel.projecte.network.packets.to_server.KeyPressPKT;
import moze_intel.projecte.network.packets.to_server.LeftClickArchangelPKT;
import moze_intel.projecte.network.packets.to_server.SearchUpdatePKT;
import moze_intel.projecte.network.packets.to_server.UpdateGemModePKT;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerListener;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public final class PacketHandler {

	private static final String PROTOCOL_VERSION = Integer.toString(1);
	private static final SimpleChannel HANDLER = NetworkRegistry.ChannelBuilder
			.named(PECore.rl("main_channel"))
			.clientAcceptedVersions(PROTOCOL_VERSION::equals)
			.serverAcceptedVersions(PROTOCOL_VERSION::equals)
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.simpleChannel();
	private static int index;

	public static void register() {
		//Client to server messages
		registerClientToServer(KeyPressPKT.class, KeyPressPKT::decode);
		registerClientToServer(LeftClickArchangelPKT.class, LeftClickArchangelPKT::decode);
		registerClientToServer(SearchUpdatePKT.class, SearchUpdatePKT::decode);
		registerClientToServer(UpdateGemModePKT.class, UpdateGemModePKT::decode);

		//Server to client messages
		registerServerToClient(CooldownResetPKT.class, CooldownResetPKT::decode);
		registerServerToClient(KnowledgeClearPKT.class, KnowledgeClearPKT::decode);
		registerServerToClient(KnowledgeSyncPKT.class, KnowledgeSyncPKT::decode);
		registerServerToClient(KnowledgeSyncEmcPKT.class, KnowledgeSyncEmcPKT::decode);
		registerServerToClient(KnowledgeSyncInputsAndLocksPKT.class, KnowledgeSyncInputsAndLocksPKT::decode);
		registerServerToClient(KnowledgeSyncChangePKT.class, KnowledgeSyncChangePKT::decode);
		registerServerToClient(SetFlyPKT.class, SetFlyPKT::decode);
		registerServerToClient(StepHeightPKT.class, StepHeightPKT::decode);
		registerServerToClient(SyncBagDataPKT.class, SyncBagDataPKT::decode);
		registerServerToClient(SyncEmcPKT.class, SyncEmcPKT::decode);
		registerServerToClient(SyncFuelMapperPKT.class, SyncFuelMapperPKT::decode);
		registerServerToClient(UpdateCondenserLockPKT.class, UpdateCondenserLockPKT::decode);
		registerServerToClient(UpdateWindowIntPKT.class, UpdateWindowIntPKT::decode);
		registerServerToClient(UpdateWindowLongPKT.class, UpdateWindowLongPKT::decode);
	}

	private static <MSG extends IPEPacket> void registerClientToServer(Class<MSG> type, Function<PacketBuffer, MSG> decoder) {
		registerMessage(type, decoder, NetworkDirection.PLAY_TO_SERVER);
	}

	private static <MSG extends IPEPacket> void registerServerToClient(Class<MSG> type, Function<PacketBuffer, MSG> decoder) {
		registerMessage(type, decoder, NetworkDirection.PLAY_TO_CLIENT);
	}

	private static <MSG extends IPEPacket> void registerMessage(Class<MSG> type, Function<PacketBuffer, MSG> decoder, NetworkDirection networkDirection) {
		HANDLER.registerMessage(index++, type, IPEPacket::encode, decoder, IPEPacket::handle, Optional.of(networkDirection));
	}

	public static void sendProgressBarUpdateInt(IContainerListener listener, Container container, int propId, int propVal) {
		if (listener instanceof ServerPlayerEntity) {
			sendTo(new UpdateWindowIntPKT((short) container.containerId, (short) propId, propVal), (ServerPlayerEntity) listener);
		}
	}

	public static void sendProgressBarUpdateLong(IContainerListener listener, Container container, int propId, long propVal) {
		if (listener instanceof ServerPlayerEntity) {
			sendTo(new UpdateWindowLongPKT((short) container.containerId, (short) propId, propVal), (ServerPlayerEntity) listener);
		}
	}

	public static void sendLockSlotUpdate(IContainerListener listener, Container container, ItemInfo lockInfo) {
		if (listener instanceof ServerPlayerEntity) {
			sendTo(new UpdateCondenserLockPKT((short) container.containerId, lockInfo), (ServerPlayerEntity) listener);
		}
	}

	public static <MSG extends IPEPacket> void sendNonLocal(MSG msg, ServerPlayerEntity player) {
		if (player.server.isDedicatedServer() || !player.getGameProfile().getName().equals(player.server.getSingleplayerName())) {
			sendTo(msg, player);
		}
	}

	public static void sendFragmentedEmcPacket(ServerPlayerEntity player) {
		sendNonLocal(new SyncEmcPKT(serializeEmcData()), player);
		sendNonLocal(FuelMapper.getSyncPacket(), player);
	}

	public static void sendFragmentedEmcPacketToAll() {
		if (ServerLifecycleHooks.getCurrentServer() != null) {
			SyncEmcPKT pkt = new SyncEmcPKT(serializeEmcData());
			SyncFuelMapperPKT fuelPkt = FuelMapper.getSyncPacket();
			for (ServerPlayerEntity player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
				sendNonLocal(pkt, player);
				sendNonLocal(fuelPkt, player);
			}
		}
	}

	private static EmcPKTInfo[] serializeEmcData() {
		EmcPKTInfo[] data = EMCMappingHandler.createPacketData();
		//Simulate encoding the EMC packet to get an accurate size
		PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
		int index = buf.writerIndex();
		new SyncEmcPKT(data).encode(buf);
		PECore.debugLog("EMC data size: {} bytes", buf.writerIndex() - index);
		buf.release();
		return data;
	}

	/**
	 * Sends a packet to the server.<br> Must be called Client side.
	 */
	public static <MSG extends IPEPacket> void sendToServer(MSG msg) {
		HANDLER.sendToServer(msg);
	}

	/**
	 * Send a packet to a specific player.<br> Must be called Server side.
	 */
	public static <MSG extends IPEPacket> void sendTo(MSG msg, ServerPlayerEntity player) {
		if (!(player instanceof FakePlayer)) {
			HANDLER.sendTo(msg, player.connection.getConnection(), NetworkDirection.PLAY_TO_CLIENT);
		}
	}
}