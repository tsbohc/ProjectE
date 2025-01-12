package moze_intel.projecte.network.packets.to_client.knowledge;

import java.math.BigInteger;
import moze_intel.projecte.PECore;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.gameObjs.container.TransmutationContainer;
import moze_intel.projecte.network.packets.IPEPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class KnowledgeSyncEmcPKT implements IPEPacket {

	private final BigInteger emc;

	public KnowledgeSyncEmcPKT(BigInteger emc) {
		this.emc = emc;
	}

	@Override
	public void handle(Context context) {
		ClientPlayerEntity player = Minecraft.getInstance().player;
		if (player != null) {
			player.getCapability(ProjectEAPI.KNOWLEDGE_CAPABILITY).ifPresent(cap -> {
				cap.setEmc(emc);
				if (player.containerMenu instanceof TransmutationContainer) {
					((TransmutationContainer) player.containerMenu).transmutationInventory.updateClientTargets();
				}
			});
		}
		PECore.debugLog("** RECEIVED TRANSMUTATION EMC DATA CLIENTSIDE **");
	}

	@Override
	public void encode(PacketBuffer buffer) {
		buffer.writeUtf(emc.toString());
	}

	public static KnowledgeSyncEmcPKT decode(PacketBuffer buffer) {
		String emc = buffer.readUtf();
		return new KnowledgeSyncEmcPKT(emc.isEmpty() ? BigInteger.ZERO : new BigInteger(emc));
	}
}