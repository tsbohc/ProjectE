package moze_intel.projecte.integration.curios;

import moze_intel.projecte.capability.BasicItemCapability;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.common.capabilities.Capability;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.type.capability.ICurio;

public class CurioItemCapability extends BasicItemCapability<ICurio> implements ICurio {

	@Override
	public Capability<ICurio> getCapability() {
		return CuriosCapability.ITEM;
	}

	@Override
	public void curioTick(String identifier, int index, LivingEntity living) {
		getStack().inventoryTick(living.getCommandSenderWorld(), living, index, false);
	}
}