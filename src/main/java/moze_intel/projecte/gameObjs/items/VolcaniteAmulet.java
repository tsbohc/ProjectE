package moze_intel.projecte.gameObjs.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import moze_intel.projecte.api.capabilities.item.IPedestalItem;
import moze_intel.projecte.api.capabilities.item.IProjectileShooter;
import moze_intel.projecte.capability.PedestalItemCapabilityWrapper;
import moze_intel.projecte.capability.ProjectileShooterItemCapabilityWrapper;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.entity.EntityLavaProjectile;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.gameObjs.tiles.DMPedestalTile;
import moze_intel.projecte.integration.IntegrationHelper;
import moze_intel.projecte.utils.ClientKeyHelper;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.PEKeybind;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class VolcaniteAmulet extends ItemPE implements IProjectileShooter, IPedestalItem, IFireProtector {

	public VolcaniteAmulet(Properties props) {
		super(props);
		addItemCapability(PedestalItemCapabilityWrapper::new);
		addItemCapability(ProjectileShooterItemCapabilityWrapper::new);
		addItemCapability(IntegrationHelper.CURIO_MODID, IntegrationHelper.CURIO_CAP_SUPPLIER);
	}

	@Override
	public boolean hasContainerItem(ItemStack stack) {
		return true;
	}

	@Override
	public ItemStack getContainerItem(ItemStack stack) {
		return stack.copy();
	}

	@Nonnull
	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		World world = ctx.getLevel();
		PlayerEntity player = ctx.getPlayer();
		BlockPos pos = ctx.getClickedPos();
		ItemStack stack = ctx.getItemInHand();
		if (player != null && !world.isClientSide && PlayerHelper.hasEditPermission((ServerPlayerEntity) player, pos) && consumeFuel(player, stack, 32, true)) {
			TileEntity tile = WorldHelper.getTileEntity(world, pos);
			Direction sideHit = ctx.getClickedFace();
			if (tile != null) {
				Optional<IFluidHandler> capability = tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, sideHit).resolve();
				if (capability.isPresent()) {
					capability.get().fill(new FluidStack(Fluids.LAVA, FluidAttributes.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
					return ActionResultType.SUCCESS;
				}
			}
			WorldHelper.placeFluid((ServerPlayerEntity) player, world, pos, sideHit, Fluids.LAVA, false);
			world.playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.TRANSMUTE.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
		}
		return ActionResultType.SUCCESS;
	}

	@Override
	public boolean shootProjectile(@Nonnull PlayerEntity player, @Nonnull ItemStack stack, Hand hand) {
		player.getCommandSenderWorld().playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.TRANSMUTE.get(), SoundCategory.PLAYERS, 1, 1);
		EntityLavaProjectile ent = new EntityLavaProjectile(player, player.getCommandSenderWorld());
		ent.shootFromRotation(player, player.xRot, player.yRot, 0, 1.5F, 1);
		player.getCommandSenderWorld().addFreshEntity(ent);
		return true;
	}

	@Override
	public void appendHoverText(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<ITextComponent> tooltips, @Nonnull ITooltipFlag flags) {
		super.appendHoverText(stack, world, tooltips, flags);
		tooltips.add(PELang.TOOLTIP_VOLCANITE_1.translate(ClientKeyHelper.getKeyName(PEKeybind.FIRE_PROJECTILE)));
		tooltips.add(PELang.TOOLTIP_VOLCANITE_2.translate());
		tooltips.add(PELang.TOOLTIP_VOLCANITE_3.translate());
		tooltips.add(PELang.TOOLTIP_VOLCANITE_4.translate());
	}

	@Override
	public void updateInPedestal(@Nonnull World world, @Nonnull BlockPos pos) {
		if (!world.isClientSide && ProjectEConfig.server.cooldown.pedestal.volcanite.get() != -1) {
			DMPedestalTile tile = WorldHelper.getTileEntity(DMPedestalTile.class, world, pos, true);
			if (tile != null) {
				if (tile.getActivityCooldown() == 0) {
					if (world.getLevelData() instanceof IServerWorldInfo) {
						IServerWorldInfo worldInfo = (IServerWorldInfo) world.getLevelData();
						worldInfo.setRainTime(0);
						worldInfo.setThunderTime(0);
						worldInfo.setRaining(false);
						worldInfo.setThundering(false);
					}
					tile.setActivityCooldown(ProjectEConfig.server.cooldown.pedestal.volcanite.get());
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
		if (ProjectEConfig.server.cooldown.pedestal.volcanite.get() != -1) {
			list.add(PELang.PEDESTAL_VOLCANITE_1.translateColored(TextFormatting.BLUE));
			list.add(PELang.PEDESTAL_VOLCANITE_2.translateColored(TextFormatting.BLUE, MathUtils.tickToSecFormatted(ProjectEConfig.server.cooldown.pedestal.volcanite.get())));
		}
		return list;
	}

	@Override
	public boolean canProtectAgainstFire(ItemStack stack, ServerPlayerEntity player) {
		return true;
	}
}