package moze_intel.projecte.gameObjs.items.rings;

import java.util.List;
import javax.annotation.Nonnull;
import moze_intel.projecte.api.capabilities.item.IAlchBagItem;
import moze_intel.projecte.api.capabilities.item.IAlchChestItem;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.capability.ExtraFunctionItemCapabilityWrapper;
import moze_intel.projecte.capability.PedestalItemCapabilityWrapper;
import moze_intel.projecte.gameObjs.items.GemEternalDensity;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.PlayerHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.items.IItemHandler;

public class VoidRing extends GemEternalDensity implements IPedestalItem, IExtraFunction {

	public VoidRing(Properties props) {
		super(props);
		addItemCapability(PedestalItemCapabilityWrapper::new);
		addItemCapability(ExtraFunctionItemCapabilityWrapper::new);
	}

	@Override
	public void inventoryTick(@Nonnull ItemStack stack, World world, @Nonnull Entity entity, int slot, boolean isHeld) {
		super.inventoryTick(stack, world, entity, slot, isHeld);
		PEItems.BLACK_HOLE_BAND.get().inventoryTick(stack, world, entity, slot, isHeld);
	}

	@Override
	public void updateInPedestal(@Nonnull World world, @Nonnull BlockPos pos) {
		((IPedestalItem) PEItems.BLACK_HOLE_BAND.get()).updateInPedestal(world, pos);
	}

	@Nonnull
	@Override
	public List<ITextComponent> getPedestalDescription() {
		return ((IPedestalItem) PEItems.BLACK_HOLE_BAND.get()).getPedestalDescription();
	}

	@Override
	public boolean doExtraFunction(@Nonnull ItemStack stack, @Nonnull PlayerEntity player, Hand hand) {
		if (player.getCooldowns().isOnCooldown(this)) {
			return false;
		}
		BlockRayTraceResult lookingAt = PlayerHelper.getBlockLookingAt(player, 64);
		BlockPos c;
		if (lookingAt.getType() == Type.MISS) {
			c = new BlockPos(PlayerHelper.getLookVec(player, 32).getRight());
		} else {
			c = lookingAt.getBlockPos();
		}
		EnderTeleportEvent event = new EnderTeleportEvent(player, c.getX(), c.getY(), c.getZ(), 0);
		if (!MinecraftForge.EVENT_BUS.post(event)) {
			if (player.isPassenger()) {
				player.stopRiding();
			}
			player.teleportTo(event.getTargetX(), event.getTargetY(), event.getTargetZ());
			player.getCommandSenderWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1, 1);
			player.fallDistance = 0.0F;
			player.getCooldowns().addCooldown(this, 10);
			return true;
		}
		return false;
	}

	@Override
	public boolean updateInAlchBag(@Nonnull IItemHandler inv, @Nonnull PlayerEntity player, @Nonnull ItemStack stack) {
		((IAlchBagItem) PEItems.BLACK_HOLE_BAND.get()).updateInAlchBag(inv, player, stack);
		return super.updateInAlchBag(inv, player, stack); // Gem of Eternal Density
	}

	@Override
	public void updateInAlchChest(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull ItemStack stack) {
		super.updateInAlchChest(world, pos, stack); // Gem of Eternal Density
		((IAlchChestItem) PEItems.BLACK_HOLE_BAND.get()).updateInAlchChest(world, pos, stack);
	}
}