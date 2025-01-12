package moze_intel.projecte.gameObjs.entity;

import javax.annotation.Nonnull;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEEntityTypes;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.network.IPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.network.NetworkHooks;

public class EntityWaterProjectile extends ThrowableEntity {

	public EntityWaterProjectile(EntityType<EntityWaterProjectile> type, World world) {
		super(type, world);
	}

	public EntityWaterProjectile(PlayerEntity entity, World world) {
		super(PEEntityTypes.WATER_PROJECTILE.get(), entity, world);
	}

	@Override
	protected void defineSynchedData() {
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.getCommandSenderWorld().isClientSide) {
			if (tickCount > 400 || !getCommandSenderWorld().isLoaded(blockPosition())) {
				remove();
				return;
			}
			Entity thrower = getOwner();
			if (thrower instanceof ServerPlayerEntity) {
				ServerPlayerEntity player = (ServerPlayerEntity) thrower;
				BlockPos.betweenClosedStream(blockPosition().offset(-3, -3, -3), blockPosition().offset(3, 3, 3)).forEach(pos -> {
					BlockState state = level.getBlockState(pos);
					FluidState fluidState = state.getFluidState();
					if (fluidState.is(FluidTags.LAVA)) {
						pos = pos.immutable();
						if (state.getBlock() instanceof FlowingFluidBlock) {
							//If it is a source block convert it
							Block block = fluidState.isSource() ? Blocks.OBSIDIAN : Blocks.COBBLESTONE;
							//Like: ForgeEventFactory#fireFluidPlaceBlockEvent except checks if it was cancelled
							BlockEvent.FluidPlaceBlockEvent event = new BlockEvent.FluidPlaceBlockEvent(level, pos, pos, block.defaultBlockState());
							if (!MinecraftForge.EVENT_BUS.post(event)) {
								PlayerHelper.checkedPlaceBlock(player, pos, event.getNewState());
							}
						} else {
							//Otherwise if it is lava logged, "void" the lava as we can't place a block in that spot
							WorldHelper.drainFluid(level, pos, state, Fluids.LAVA);
						}
						playSound(SoundEvents.GENERIC_BURN, 0.5F, 2.6F + (getCommandSenderWorld().random.nextFloat() - getCommandSenderWorld().random.nextFloat()) * 0.8F);
					}
				});
			}
			if (isInWater()) {
				remove();
			}
			if (getY() > 128) {
				IWorldInfo worldInfo = this.getCommandSenderWorld().getLevelData();
				worldInfo.setRaining(true);
				remove();
			}
		}
	}

	@Override
	public float getGravity() {
		return 0;
	}

	@Override
	protected void onHit(@Nonnull RayTraceResult mop) {
		if (level.isClientSide) {
			return;
		}
		Entity thrower = getOwner();
		if (!(thrower instanceof PlayerEntity)) {
			remove();
			return;
		}
		if (mop instanceof BlockRayTraceResult) {
			BlockRayTraceResult result = (BlockRayTraceResult) mop;
			WorldHelper.placeFluid((ServerPlayerEntity) thrower, level, result.getBlockPos(), result.getDirection(), Fluids.WATER, !ProjectEConfig.server.items.opEvertide.get());
		} else if (mop instanceof EntityRayTraceResult) {
			Entity ent = ((EntityRayTraceResult) mop).getEntity();
			if (ent.isOnFire()) {
				ent.clearFire();
			}
			ent.push(this.getDeltaMovement().x() * 2, this.getDeltaMovement().y() * 2, this.getDeltaMovement().z() * 2);
		}
		remove();
	}

	@Nonnull
	@Override
	public IPacket<?> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public boolean ignoreExplosion() {
		return true;
	}
}