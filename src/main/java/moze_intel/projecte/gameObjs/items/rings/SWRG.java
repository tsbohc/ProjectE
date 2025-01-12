package moze_intel.projecte.gameObjs.items.rings;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.capability.PedestalItemCapabilityWrapper;
import moze_intel.projecte.capability.ProjectileShooterItemCapabilityWrapper;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntitySWRGProjectile;
import moze_intel.projecte.gameObjs.items.IFlightProvider;
import moze_intel.projecte.gameObjs.items.ItemPE;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import moze_intel.projecte.handlers.InternalAbilities;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

public class SWRG extends ItemPE implements IPedestalItem, IFlightProvider, IProjectileShooter {

	public SWRG(Properties props) {
		super(props);
		addItemCapability(PedestalItemCapabilityWrapper::new);
		addItemCapability(ProjectileShooterItemCapabilityWrapper::new);
		addItemCapability(IntegrationHelper.CURIO_MODID, IntegrationHelper.CURIO_CAP_SUPPLIER);
	}

	private void tick(ItemStack stack, PlayerEntity player) {
		CompoundNBT nbt = stack.getOrCreateTag();
		if (nbt.getInt(Constants.NBT_KEY_MODE) > 1) {
			// Repel on both sides - smooth animation
			WorldHelper.repelEntitiesSWRG(player.getCommandSenderWorld(), player.getBoundingBox().inflate(5), player);
		}
		if (player.getCommandSenderWorld().isClientSide) {
			return;
		}
		ServerPlayerEntity playerMP = (ServerPlayerEntity) player;
		if (getEmc(stack) == 0 && !consumeFuel(player, stack, 64, false)) {
			if (nbt.getInt(Constants.NBT_KEY_MODE) > 0) {
				changeMode(player, stack, 0);
			}
			if (playerMP.abilities.mayfly) {
				playerMP.getCapability(InternalAbilities.CAPABILITY).ifPresent(InternalAbilities::disableSwrgFlightOverride);
			}
			return;
		}

		if (!playerMP.abilities.mayfly) {
			playerMP.getCapability(InternalAbilities.CAPABILITY).ifPresent(InternalAbilities::enableSwrgFlightOverride);
		}

		if (playerMP.abilities.flying) {
			if (!isFlyingEnabled(nbt)) {
				changeMode(player, stack, nbt.getInt(Constants.NBT_KEY_MODE) == 0 ? 1 : 3);
			}
		} else if (isFlyingEnabled(nbt)) {
			changeMode(player, stack, nbt.getInt(Constants.NBT_KEY_MODE) == 1 ? 0 : 2);
		}

		float toRemove = 0;

		if (playerMP.abilities.flying) {
			toRemove = 0.32F;
		}

		if (nbt.getInt(Constants.NBT_KEY_MODE) == 2) {
			toRemove = 0.32F;
		} else if (nbt.getInt(Constants.NBT_KEY_MODE) == 3) {
			toRemove = 0.64F;
		}

		removeEmc(stack, EMCHelper.removeFractionalEMC(stack, toRemove));

		playerMP.fallDistance = 0;
	}

	private boolean isFlyingEnabled(CompoundNBT nbt) {
		return nbt.getInt(Constants.NBT_KEY_MODE) == 1 || nbt.getInt(Constants.NBT_KEY_MODE) == 3;
	}

	@Override
	public void inventoryTick(@Nonnull ItemStack stack, @Nonnull World world, @Nonnull Entity entity, int invSlot, boolean isHeldItem) {
		if (invSlot >= PlayerInventory.getSelectionSize() || !(entity instanceof PlayerEntity)) {
			return;
		}
		tick(stack, (PlayerEntity) entity);
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity player, @Nonnull Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!world.isClientSide) {
			int newMode = 0;
			switch (stack.getOrCreateTag().getInt(Constants.NBT_KEY_MODE)) {
				case 0:
					newMode = 2;
					break;
				case 1:
					newMode = 3;
					break;
				case 2:
					newMode = 0;
					break;
				case 3:
					newMode = 1;
					break;
			}
			changeMode(player, stack, newMode);
		}
		return ActionResult.success(stack);
	}

	/**
	 * Change the mode of SWRG. Modes:<p> 0 = Ring Off<p> 1 = Flight<p> 2 = Shield<p> 3 = Flight + Shield<p>
	 */
	public void changeMode(PlayerEntity player, ItemStack stack, int mode) {
		CompoundNBT nbt = stack.getOrCreateTag();
		int oldMode = nbt.getInt(Constants.NBT_KEY_MODE);
		if (mode == oldMode) {
			return;
		}
		nbt.putInt(Constants.NBT_KEY_MODE, mode);
		if (player == null) {
			//Don't do sounds if the player is null
			return;
		}
		if (mode == 0 || oldMode == 3) {
			//At least one mode deactivated
			player.getCommandSenderWorld().playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.HEAL.get(), SoundCategory.PLAYERS, 0.8F, 1.0F);
		} else if (oldMode == 0 || mode == 3) {
			//At least one mode activated
			player.getCommandSenderWorld().playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.UNCHARGE.get(), SoundCategory.PLAYERS, 0.8F, 1.0F);
		}
		//Doesn't handle going from mode 1 to 2 or 2 to 1
	}

	@Override
	public boolean canProvideFlight(ItemStack stack, ServerPlayerEntity player) {
		// Dummy result - swrg needs special-casing
		return false;
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return false;
	}

	@Override
	public void updateInPedestal(@Nonnull World world, @Nonnull BlockPos pos) {
		if (!world.isClientSide && ProjectEConfig.server.cooldown.pedestal.swrg.get() != -1) {
			DMPedestalTile tile = WorldHelper.getTileEntity(DMPedestalTile.class, world, pos, true);
			if (tile != null) {
				if (tile.getActivityCooldown() <= 0) {
					List<MobEntity> list = world.getEntitiesOfClass(MobEntity.class, tile.getEffectBounds());
					for (MobEntity living : list) {
						if (living instanceof TameableEntity && ((TameableEntity) living).isTame()) {
							continue;
						}
						LightningBoltEntity lightning = EntityType.LIGHTNING_BOLT.create(world);
						if (lightning != null) {
							lightning.moveTo(living.position());
							world.addFreshEntity(lightning);
						}
					}
					tile.setActivityCooldown(ProjectEConfig.server.cooldown.pedestal.swrg.get());
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
		if (ProjectEConfig.server.cooldown.pedestal.swrg.get() != -1) {
			list.add(PELang.PEDESTAL_SWRG_1.translateColored(TextFormatting.BLUE));
			list.add(PELang.PEDESTAL_SWRG_2.translateColored(TextFormatting.BLUE, MathUtils.tickToSecFormatted(ProjectEConfig.server.cooldown.pedestal.swrg.get())));
		}
		return list;
	}

	@Override
	public boolean shootProjectile(@Nonnull PlayerEntity player, @Nonnull ItemStack stack, @Nullable Hand hand) {
		EntitySWRGProjectile projectile = new EntitySWRGProjectile(player, false, player.level);
		projectile.shootFromRotation(player, player.xRot, player.yRot, 0, 1.5F, 1);
		player.level.addFreshEntity(projectile);
		return true;
	}
}