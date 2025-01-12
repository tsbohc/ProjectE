package moze_intel.projecte.gameObjs.items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.item.IAlchBagItem;
import moze_intel.projecte.api.capabilities.item.IAlchChestItem;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.capability.AlchBagItemCapabilityWrapper;
import moze_intel.projecte.capability.AlchChestItemCapabilityWrapper;
import moze_intel.projecte.capability.PedestalItemCapabilityWrapper;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.tiles.AlchChestTile;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import moze_intel.projecte.handlers.InternalTimers;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class RepairTalisman extends ItemPE implements IAlchBagItem, IAlchChestItem, IPedestalItem {

	private static final Predicate<ItemStack> CAN_REPAIR_ITEM = stack -> !stack.isEmpty() &&
																		 !stack.getCapability(ProjectEAPI.MODE_CHANGER_ITEM_CAPABILITY).isPresent() &&
																		 ItemHelper.isRepairableDamagedItem(stack);

	public RepairTalisman(Properties props) {
		super(props);
		addItemCapability(AlchBagItemCapabilityWrapper::new);
		addItemCapability(AlchChestItemCapabilityWrapper::new);
		addItemCapability(PedestalItemCapabilityWrapper::new);
		addItemCapability(IntegrationHelper.CURIO_MODID, IntegrationHelper.CURIO_CAP_SUPPLIER);
	}

	@Override
	public void inventoryTick(@Nonnull ItemStack stack, World world, @Nonnull Entity entity, int invSlot, boolean isSelected) {
		if (!world.isClientSide && entity instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) entity;
			player.getCapability(InternalTimers.CAPABILITY).ifPresent(timers -> {
				timers.activateRepair();
				if (timers.canRepair()) {
					repairAllItems(player);
				}
			});
		}
	}

	@Override
	public void updateInPedestal(@Nonnull World world, @Nonnull BlockPos pos) {
		if (!world.isClientSide && ProjectEConfig.server.cooldown.pedestal.repair.get() != -1) {
			DMPedestalTile tile = WorldHelper.getTileEntity(DMPedestalTile.class, world, pos, true);
			if (tile != null) {
				if (tile.getActivityCooldown() == 0) {
					world.getEntitiesOfClass(ServerPlayerEntity.class, tile.getEffectBounds()).forEach(RepairTalisman::repairAllItems);
					tile.setActivityCooldown(ProjectEConfig.server.cooldown.pedestal.repair.get());
				} else {
					tile.decrementActivityCooldown();
				}
			}
		}
	}

	@Nonnull
	@Override
	public List<ITextComponent> getPedestalDescription() {
		List<ITextComponent> list = new ArrayList<>();
		if (ProjectEConfig.server.cooldown.pedestal.repair.get() != -1) {
			list.add(PELang.PEDESTAL_REPAIR_TALISMAN_1.translateColored(TextFormatting.BLUE));
			list.add(PELang.PEDESTAL_REPAIR_TALISMAN_2.translateColored(TextFormatting.BLUE, MathUtils.tickToSecFormatted(ProjectEConfig.server.cooldown.pedestal.repair.get())));
		}
		return list;
	}

	@Override
	public void updateInAlchChest(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull ItemStack stack) {
		if (!world.isClientSide) {
			AlchChestTile tile = WorldHelper.getTileEntity(AlchChestTile.class, world, pos, true);
			if (tile != null) {
				CompoundNBT nbt = stack.getOrCreateTag();
				byte coolDown = nbt.getByte(Constants.NBT_KEY_COOLDOWN);
				if (coolDown > 0) {
					nbt.putByte(Constants.NBT_KEY_COOLDOWN, (byte) (coolDown - 1));
				} else {
					tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(inv -> {
						if (repairAllItems(inv, CAN_REPAIR_ITEM)) {
							nbt.putByte(Constants.NBT_KEY_COOLDOWN, (byte) 19);
							//Note: We don't need to recheck comparators as repairing doesn't change the number
							// of items in slots
							tile.markDirty(false, false);
						}
					});
				}
			}
		}
	}

	@Override
	public boolean updateInAlchBag(@Nonnull IItemHandler inv, @Nonnull PlayerEntity player, @Nonnull ItemStack stack) {
		if (player.getCommandSenderWorld().isClientSide) {
			return false;
		}
		CompoundNBT nbt = stack.getOrCreateTag();
		byte coolDown = nbt.getByte(Constants.NBT_KEY_COOLDOWN);
		if (coolDown > 0) {
			nbt.putByte(Constants.NBT_KEY_COOLDOWN, (byte) (coolDown - 1));
		} else if (repairAllItems(inv, CAN_REPAIR_ITEM)) {
			nbt.putByte(Constants.NBT_KEY_COOLDOWN, (byte) 19);
			return true;
		}
		return false;
	}

	private static void repairAllItems(PlayerEntity player) {
		Predicate<ItemStack> canRepairPlayerItem = CAN_REPAIR_ITEM.and(stack -> stack != player.getMainHandItem() || !player.swinging);
		player.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(inv -> repairAllItems(inv, canRepairPlayerItem));
		IItemHandler curios = PlayerHelper.getCurios(player);
		if (curios != null) {
			repairAllItems(curios, canRepairPlayerItem);
		}
	}

	private static boolean repairAllItems(IItemHandler inv, Predicate<ItemStack> canRepairStack) {
		boolean hasAction = false;
		for (int i = 0; i < inv.getSlots(); i++) {
			ItemStack invStack = inv.getStackInSlot(i);
			if (canRepairStack.test(invStack)) {
				invStack.setDamageValue(invStack.getDamageValue() - 1);
				if (!hasAction) {
					hasAction = true;
				}
			}
		}
		return hasAction;
	}
}