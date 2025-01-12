package moze_intel.projecte.gameObjs.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.tile.IEmcStorage;
import moze_intel.projecte.api.tile.TileEmcBase;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.ItemHelper;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.ItemStackHandler;

public abstract class TileEmc extends TileEmcBase implements ITickableTileEntity {

	private boolean updateComparators;

	public TileEmc(TileEntityType<?> type) {
		this(type, Constants.TILE_MAX_EMC);
	}

	public TileEmc(TileEntityType<?> type, long maxAmount) {
		super(type);
		setMaximumEMC(maxAmount);
	}

	@Override
	public void tick() {
		//Note: We call this at the end of child implementations to try and update any changes immediately instead
		// of them having to be delayed a tick
		if (level != null && !level.isClientSide) {
			//Only update the comparator state if we support comparators and need to update comparators
			if (updateComparators) {
				BlockState state = getBlockState();
				if (!state.isAir(level, worldPosition)) {
					level.updateNeighbourForOutputSignal(worldPosition, state.getBlock());
				}
				updateComparators = false;
			}
		}
	}

	protected boolean emcAffectsComparators() {
		return false;
	}

	@Override
	protected void storedEmcChanged() {
		markDirty(false, emcAffectsComparators());
	}

	@Override
	public void setChanged() {
		markDirty(true, true);
	}

	public void markDirty(boolean recheckBlockState, boolean recheckComparators) {
		//Copy of the base impl of markDirty in TileEntity, except only updates comparator state when something changed
		// and if our block supports having a comparator signal, instead of always doing it
		if (level != null) {
			if (recheckBlockState) {
				//Clear the cached block state so that it is relooked up when needed
				// Note: We do this instead of setting it like vanilla does as we don't
				// override clearCache anywhere AND this lets us avoid needing an AT
				clearCache();
			}
			if (level.hasChunkAt(worldPosition)) {
				level.getChunkAt(worldPosition).markUnsaved();
			}
			if (recheckComparators && !level.isClientSide) {
				updateComparators = true;
			}
		}
	}

	@Nonnull
	@Override
	public final CompoundNBT getUpdateTag() {
		return save(new CompoundNBT());
	}

	/**
	 * The amount provided will be divided and evenly distributed as best as possible between adjacent IEmcStorage. This is limited also by our max extract limit
	 *
	 * @param emc The maximum combined emc to send to others
	 *
	 * @return The amount of Emc we actually sent
	 */
	protected long sendToAllAcceptors(long emc) {
		if (level == null || !canProvideEmc()) {
			//If we cannot provide emc then just return
			return 0;
		}
		emc = Math.min(getEmcExtractLimit(), emc);
		long sentEmc = 0;
		List<IEmcStorage> targets = new ArrayList<>();
		for (Direction dir : Direction.values()) {
			BlockPos neighboringPos = worldPosition.relative(dir);
			//Make sure the neighboring block is loaded as if we are on a chunk border on the edge of loaded chunks this may not be the case
			if (level.isLoaded(neighboringPos)) {
				TileEntity neighboringTile = WorldHelper.getTileEntity(level, neighboringPos);
				if (neighboringTile != null) {
					neighboringTile.getCapability(ProjectEAPI.EMC_STORAGE_CAPABILITY, dir.getOpposite()).ifPresent(theirEmcStorage -> {
						if (!isRelay() || !theirEmcStorage.isRelay()) {
							//If they are both relays don't add the pairing so as to prevent thrashing
							if (theirEmcStorage.insertEmc(1, EmcAction.SIMULATE) > 0) {
								//If they would be wiling to accept any Emc then we consider them to be an "acceptor"
								targets.add(theirEmcStorage);
							}
						}
					});
				}
			}
		}

		if (!targets.isEmpty()) {
			long emcPer = emc / targets.size();
			for (IEmcStorage target : targets) {
				long emcCanProvide = extractEmc(emcPer, EmcAction.SIMULATE);
				long acceptedEmc = target.insertEmc(emcCanProvide, EmcAction.EXECUTE);
				extractEmc(acceptedEmc, EmcAction.EXECUTE);
				sentEmc += acceptedEmc;
			}
		}
		return sentEmc;
	}

	class StackHandler extends ItemStackHandler {

		StackHandler(int size) {
			super(size);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			markDirty(false, true);
		}
	}

	class CompactableStackHandler extends StackHandler {

		//Start as needing to check for compacting when loaded
		private boolean needsCompacting = true;
		private boolean empty;

		CompactableStackHandler(int size) {
			super(size);
		}

		@Override
		protected void onContentsChanged(int slot) {
			super.onContentsChanged(slot);
			needsCompacting = true;
		}

		public void compact() {
			if (needsCompacting) {
				if (level != null && !level.isClientSide) {
					empty = ItemHelper.compactInventory(this);
				}
				needsCompacting = false;
			}
		}

		@Override
		protected void onLoad() {
			super.onLoad();
			empty = IntStream.range(0, getSlots()).allMatch(slot -> getStackInSlot(slot).isEmpty());
		}

		/**
		 * @apiNote Only use this on the server
		 */
		public boolean isEmpty() {
			return empty;
		}
	}
}