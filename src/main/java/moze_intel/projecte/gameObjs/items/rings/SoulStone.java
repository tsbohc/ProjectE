package moze_intel.projecte.gameObjs.items.rings;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.capability.PedestalItemCapabilityWrapper;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import moze_intel.projecte.handlers.InternalTimers;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class SoulStone extends PEToggleItem implements IPedestalItem {

	public SoulStone(Properties props) {
		super(props);
		addItemCapability(PedestalItemCapabilityWrapper::new);
		addItemCapability(IntegrationHelper.CURIO_MODID, IntegrationHelper.CURIO_CAP_SUPPLIER);
	}

	@Override
	public void inventoryTick(@Nonnull ItemStack stack, World world, @Nonnull Entity entity, int slot, boolean held) {
		if (world.isClientSide || slot >= PlayerInventory.getSelectionSize() || !(entity instanceof PlayerEntity)) {
			return;
		}
		super.inventoryTick(stack, world, entity, slot, held);
		PlayerEntity player = (PlayerEntity) entity;
		CompoundNBT nbt = stack.getOrCreateTag();
		if (nbt.getBoolean(Constants.NBT_KEY_ACTIVE)) {
			if (getEmc(stack) < 64 && !consumeFuel(player, stack, 64, false)) {
				nbt.putBoolean(Constants.NBT_KEY_ACTIVE, false);
			} else {
				player.getCapability(InternalTimers.CAPABILITY, null).ifPresent(timers -> {
					timers.activateHeal();
					if (player.getHealth() < player.getMaxHealth() && timers.canHeal()) {
						world.playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.HEAL.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
						player.heal(2.0F);
						removeEmc(stack, 64);
					}
				});
			}
		}
	}

	@Override
	public void updateInPedestal(@Nonnull World world, @Nonnull BlockPos pos) {
		if (!world.isClientSide && ProjectEConfig.server.cooldown.pedestal.soul.get() != -1) {
			DMPedestalTile tile = WorldHelper.getTileEntity(DMPedestalTile.class, world, pos, true);
			if (tile != null) {
				if (tile.getActivityCooldown() == 0) {
					List<ServerPlayerEntity> players = world.getEntitiesOfClass(ServerPlayerEntity.class, tile.getEffectBounds());
					for (ServerPlayerEntity player : players) {
						if (player.getHealth() < player.getMaxHealth()) {
							world.playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.HEAL.get(), SoundCategory.BLOCKS, 1.0F, 1.0F);
							player.heal(1.0F); // 1/2 heart
						}
					}
					tile.setActivityCooldown(ProjectEConfig.server.cooldown.pedestal.soul.get());
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
		if (ProjectEConfig.server.cooldown.pedestal.soul.get() != -1) {
			list.add(PELang.PEDESTAL_SOUL_STONE_1.translateColored(TextFormatting.BLUE));
			list.add(PELang.PEDESTAL_SOUL_STONE_2.translateColored(TextFormatting.BLUE, MathUtils.tickToSecFormatted(ProjectEConfig.server.cooldown.pedestal.soul.get())));
		}
		return list;
	}
}