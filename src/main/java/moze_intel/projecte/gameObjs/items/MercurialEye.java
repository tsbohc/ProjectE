package moze_intel.projecte.gameObjs.items;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.item.IExtraFunction;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage.EmcAction;
import moze_intel.projecte.capability.ExtraFunctionItemCapabilityWrapper;
import moze_intel.projecte.capability.IItemCapabilitySerializable;
import moze_intel.projecte.capability.ItemCapability;
import moze_intel.projecte.gameObjs.container.MercurialEyeContainer;
import moze_intel.projecte.gameObjs.registries.PESoundEvents;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.PlayerHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.inventory.container.SimpleNamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.INBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class MercurialEye extends ItemMode implements IExtraFunction {

	private static final int CREATION_MODE = 0;
	private static final int EXTENSION_MODE = 1;
	private static final int EXTENSION_MODE_CLASSIC = 2;
	private static final int TRANSMUTATION_MODE = 3;
	private static final int TRANSMUTATION_MODE_CLASSIC = 4;
	private static final int PILLAR_MODE = 5;

	public MercurialEye(Properties props) {
		super(props, (byte) 4, PELang.MODE_MERCURIAL_EYE_1, PELang.MODE_MERCURIAL_EYE_2, PELang.MODE_MERCURIAL_EYE_3, PELang.MODE_MERCURIAL_EYE_4,
				PELang.MODE_MERCURIAL_EYE_5, PELang.MODE_MERCURIAL_EYE_6);
		addItemCapability(ExtraFunctionItemCapabilityWrapper::new);
		addItemCapability(EyeInventoryHandler::new);
	}

	@Override
	public boolean doExtraFunction(@Nonnull ItemStack stack, @Nonnull PlayerEntity player, Hand hand) {
		int selected = player.inventory.selected;
		INamedContainerProvider provider = new SimpleNamedContainerProvider((id, inv, pl) -> new MercurialEyeContainer(id, inv, hand, selected), stack.getHoverName());
		NetworkHooks.openGui((ServerPlayerEntity) player, provider, b -> {
			b.writeEnum(hand);
			b.writeByte(selected);
		});
		return true;
	}

	@Nonnull
	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		ItemStack stack = ctx.getItemInHand();
		return ctx.getLevel().isClientSide ? ActionResultType.SUCCESS : formBlocks(stack, ctx.getPlayer(), ctx.getClickedPos(), ctx.getClickedFace());
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> use(@Nonnull World world, PlayerEntity player, @Nonnull Hand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (getMode(stack) == CREATION_MODE) {
			if (world.isClientSide) {
				return ActionResult.success(stack);
			}
			Vector3d eyeVec = new Vector3d(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
			Vector3d lookVec = player.getLookAngle();
			//I'm not sure why there has to be a one point offset to the X coordinate here, but it's pretty consistent in testing.
			Vector3d targVec = eyeVec.add(lookVec.x * 2, lookVec.y * 2, lookVec.z * 2);
			return ItemHelper.actionResultFromType(formBlocks(stack, player, new BlockPos(targVec), null), stack);
		}
		return ActionResult.pass(stack);
	}

	private ActionResultType formBlocks(ItemStack eye, PlayerEntity player, BlockPos startingPos, @Nullable Direction facing) {
		Optional<IItemHandler> inventoryCapability = eye.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
		if (!inventoryCapability.isPresent()) {
			return ActionResultType.FAIL;
		}
		IItemHandler inventory = inventoryCapability.get();
		ItemStack klein = inventory.getStackInSlot(0);
		if (klein.isEmpty() || !klein.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).isPresent()) {
			return ActionResultType.FAIL;
		}

		World world = player.getCommandSenderWorld();
		BlockState startingState = world.getBlockState(startingPos);
		long startingBlockEmc = EMCHelper.getEmcValue(new ItemStack(startingState.getBlock()));
		ItemStack target = inventory.getStackInSlot(1);
		BlockState newState;
		long newBlockEmc;
		byte mode = getMode(eye);

		if (!target.isEmpty()) {
			newState = ItemHelper.stackToState(target);
			newBlockEmc = EMCHelper.getEmcValue(target);
		} else if (startingBlockEmc != 0 && (mode == EXTENSION_MODE || mode == EXTENSION_MODE_CLASSIC)) {
			//If there is no item key, attempt to determine it for extension mode
			newState = startingState;
			newBlockEmc = startingBlockEmc;
		} else {
			return ActionResultType.FAIL;
		}
		if (newState == null || newState.getBlock().isAir(newState, null, null)) {
			return ActionResultType.FAIL;
		}

		NonNullList<ItemStack> drops = NonNullList.create();
		int charge = getCharge(eye);
		int hitTargets = 0;
		if (mode == CREATION_MODE) {
			Block block = startingState.getBlock();
			if (facing != null && (!startingState.getMaterial().isReplaceable() || player.isShiftKeyDown() && !block.isAir(startingState, world, startingPos))) {
				BlockPos offsetPos = startingPos.relative(facing);
				BlockState offsetState = world.getBlockState(offsetPos);
				if (!offsetState.getMaterial().isReplaceable()) {
					return ActionResultType.FAIL;
				}
				long offsetBlockEmc = EMCHelper.getEmcValue(new ItemStack(offsetState.getBlock()));
				//Just in case it is not air but is a replaceable block like tall grass, get the proper EMC instead of just using 0
				if (doBlockPlace(player, offsetState, offsetPos, newState, eye, offsetBlockEmc, newBlockEmc, drops)) {
					hitTargets++;
				}
			} else if (doBlockPlace(player, startingState, startingPos, newState, eye, startingBlockEmc, newBlockEmc, drops)) {
				//Otherwise replace it (it may have been air), or it may have been something like tall grass
				hitTargets++;
			}
		} else if (mode == PILLAR_MODE) {
			//Fills in replaceable blocks in up to a 3x3x3/6/9/12/15 area
			hitTargets += fillGaps(eye, player, world, startingState, newState, newBlockEmc, getCorners(startingPos, facing, 1, 3 * charge + 2), drops);
		} else if (mode == EXTENSION_MODE_CLASSIC) {
			//if it is replaceable fill in the gaps in up to a 9x9x1 area
			hitTargets += fillGaps(eye, player, world, startingState, newState, newBlockEmc, getCorners(startingPos, facing, charge, 0), drops);
		} else if (mode == TRANSMUTATION_MODE_CLASSIC) {
			//if state is same as the start state replace it in an up to 9x9x1 area
			Pair<BlockPos, BlockPos> corners = getCorners(startingPos, facing, charge, 0);
			for (BlockPos pos : WorldHelper.getPositionsFromBox(new AxisAlignedBB(corners.getLeft(), corners.getRight()))) {
				BlockState placedState = world.getBlockState(pos);
				//Ensure we are immutable so that removal/placing doesn't act weird
				if (placedState == startingState && doBlockPlace(player, placedState, pos.immutable(), newState, eye, startingBlockEmc, newBlockEmc, drops)) {
					hitTargets++;
				}
			}
		} else {
			if (startingState.getBlock().isAir(startingState, world, startingPos) || facing == null) {
				return ActionResultType.FAIL;
			}

			LinkedList<BlockPos> possibleBlocks = new LinkedList<>();
			Set<BlockPos> visited = new HashSet<>();
			possibleBlocks.add(startingPos);
			visited.add(startingPos);

			int side = 2 * charge + 1;
			int size = side * side;
			int totalTries = size * 4;
			for (int attemptedTargets = 0; attemptedTargets < totalTries && !possibleBlocks.isEmpty(); attemptedTargets++) {
				BlockPos pos = possibleBlocks.poll();
				BlockState checkState = world.getBlockState(pos);
				if (startingState != checkState) {
					continue;
				}
				BlockPos offsetPos = pos.relative(facing);
				BlockState offsetState = world.getBlockState(offsetPos);
				if (!offsetState.isFaceSturdy(world, offsetPos, facing)) {
					boolean hit = false;
					if (mode == EXTENSION_MODE) {
						VoxelShape cbBox = startingState.getCollisionShape(world, offsetPos);
						if (world.isUnobstructed(null, cbBox)) {
							long offsetBlockEmc = EMCHelper.getEmcValue(offsetState.getBlock());
							hit = doBlockPlace(player, offsetState, offsetPos, newState, eye, offsetBlockEmc, newBlockEmc, drops);
						}
					} else if (mode == TRANSMUTATION_MODE) {
						hit = doBlockPlace(player, checkState, pos, newState, eye, startingBlockEmc, newBlockEmc, drops);
					}

					if (hit) {
						hitTargets++;
						if (hitTargets >= size) {
							break;
						}
						for (Direction e : Direction.values()) {
							if (facing.getAxis() != e.getAxis()) {
								BlockPos offset = pos.relative(e);
								if (visited.add(offset)) {
									possibleBlocks.offer(offset);
								}
								BlockPos offsetOpposite = pos.relative(e.getOpposite());
								if (visited.add(offsetOpposite)) {
									possibleBlocks.offer(offsetOpposite);
								}
							}
						}
					}
				}
			}
		}

		if (hitTargets > 0) {
			world.playSound(null, player.getX(), player.getY(), player.getZ(), PESoundEvents.POWER.get(), SoundCategory.PLAYERS, 0.8F, 2F / ((float) charge / getNumCharges(eye) + 2F));
			if (!drops.isEmpty()) {
				//Make all the drops fall together
				WorldHelper.createLootDrop(drops, player.getCommandSenderWorld(), startingPos);
			}
		}
		return ActionResultType.SUCCESS;
	}

	private boolean doBlockPlace(PlayerEntity player, BlockState oldState, BlockPos placePos, BlockState newState, ItemStack eye, long oldEMC, long newEMC, NonNullList<ItemStack> drops) {
		Optional<IItemHandler> inventoryCapability = eye.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).resolve();
		if (!inventoryCapability.isPresent()) {
			return false;
		}
		IItemHandler inventory = inventoryCapability.get();
		ItemStack klein = inventory.getStackInSlot(0);
		if (klein.isEmpty()) {
			return false;
		}
		Optional<IItemEmcHolder> holderCapability = klein.getCapability(ProjectEAPI.EMC_HOLDER_ITEM_CAPABILITY).resolve();
		if (!holderCapability.isPresent() || oldState == newState || ItemPE.getEmc(klein) < newEMC - oldEMC ||
			WorldHelper.getTileEntity(player.getCommandSenderWorld(), placePos) != null) {
			return false;
		}

		if (oldEMC == 0 && oldState.getDestroySpeed(player.level, placePos) == -1.0F) {
			//Don't allow replacing unbreakable blocks (unless they have an EMC value)
			return false;
		}

		if (PlayerHelper.checkedReplaceBlock((ServerPlayerEntity) player, placePos, newState)) {
			IItemEmcHolder emcHolder = holderCapability.get();
			if (oldEMC == 0) {
				//Drop the block because it doesn't have an emc value
				drops.addAll(Block.getDrops(oldState, ((ServerPlayerEntity) player).getLevel(), placePos, null));
				emcHolder.extractEmc(klein, newEMC, EmcAction.EXECUTE);
			} else if (oldEMC > newEMC) {
				emcHolder.insertEmc(klein, oldEMC - newEMC, EmcAction.EXECUTE);
			} else if (oldEMC < newEMC) {
				emcHolder.extractEmc(klein, newEMC - oldEMC, EmcAction.EXECUTE);
			}
			return true;
		}
		return false;
	}

	private int fillGaps(ItemStack eye, PlayerEntity player, World world, BlockState startingState, BlockState newState, long newBlockEmc, Pair<BlockPos, BlockPos> corners, NonNullList<ItemStack> drops) {
		int hitTargets = 0;
		for (BlockPos pos : WorldHelper.getPositionsFromBox(new AxisAlignedBB(corners.getLeft(), corners.getRight()))) {
			VoxelShape bb = startingState.getCollisionShape(world, pos);
			if (world.isUnobstructed(null, bb)) {
				BlockState placeState = world.getBlockState(pos);
				if (placeState.getMaterial().isReplaceable()) {
					//Only replace replaceable blocks
					long placeBlockEmc = EMCHelper.getEmcValue(placeState.getBlock());
					//Ensure we are immutable so that changing blocks doesn't act weird
					if (doBlockPlace(player, placeState, pos.immutable(), newState, eye, placeBlockEmc, newBlockEmc, drops)) {
						hitTargets++;
					}
				}
			}
		}
		return hitTargets;
	}

	private Pair<BlockPos, BlockPos> getCorners(BlockPos startingPos, Direction facing, int strength, int depth) {
		if (facing == null) {
			return new ImmutablePair<>(startingPos, startingPos);
		}
		BlockPos start = startingPos;
		BlockPos end = startingPos;
		switch (facing) {
			case UP:
				start = start.offset(-strength, -depth, -strength);
				end = end.offset(strength, 0, strength);
				break;
			case DOWN:
				start = start.offset(-strength, 0, -strength);
				end = end.offset(strength, depth, strength);
				break;
			case SOUTH:
				start = start.offset(-strength, -strength, -depth);
				end = end.offset(strength, strength, 0);
				break;
			case NORTH:
				start = start.offset(-strength, -strength, 0);
				end = end.offset(strength, strength, depth);
				break;
			case EAST:
				start = start.offset(-depth, -strength, -strength);
				end = end.offset(0, strength, strength);
				break;
			case WEST:
				start = start.offset(0, -strength, -strength);
				end = end.offset(depth, strength, strength);
				break;
		}
		return new ImmutablePair<>(start, end);
	}

	private static class EyeInventoryHandler extends ItemCapability<IItemHandler> implements IItemCapabilitySerializable {

		private final IItemHandler inv = new ItemStackHandler(2);
		private final LazyOptional<IItemHandler> invInst = LazyOptional.of(() -> inv);

		@Override
		public INBT serializeNBT() {
			return getCapability().writeNBT(inv, null);
		}

		@Override
		public void deserializeNBT(INBT nbt) {
			getCapability().readNBT(inv, null, nbt);
		}

		@Override
		public Capability<IItemHandler> getCapability() {
			return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
		}

		@Override
		public LazyOptional<IItemHandler> getLazyCapability() {
			return invInst;
		}

		@Override
		public String getStorageKey() {
			return "EyeInventory";
		}
	}
}