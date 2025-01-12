package moze_intel.projecte.gameObjs.container;

import javax.annotation.Nonnull;
import moze_intel.projecte.gameObjs.blocks.Relay;
import moze_intel.projecte.gameObjs.container.slots.SlotPredicates;
import moze_intel.projecte.gameObjs.container.slots.ValidatedSlot;
import moze_intel.projecte.gameObjs.registration.impl.BlockRegistryObject;
import moze_intel.projecte.gameObjs.registration.impl.ContainerTypeRegistryObject;
import moze_intel.projecte.gameObjs.registries.PEBlocks;
import moze_intel.projecte.gameObjs.registries.PEContainerTypes;
import moze_intel.projecte.gameObjs.tiles.RelayMK1Tile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.IntReferenceHolder;
import net.minecraftforge.items.IItemHandler;

public class RelayMK1Container extends PEContainer {

	public final RelayMK1Tile tile;
	private final IntReferenceHolder kleinChargeProgress = IntReferenceHolder.standalone();
	private final IntReferenceHolder inputBurnProgress = IntReferenceHolder.standalone();
	public final BoxedLong emc = new BoxedLong();

	public RelayMK1Container(int windowId, PlayerInventory invPlayer, RelayMK1Tile relay) {
		this(PEContainerTypes.RELAY_MK1_CONTAINER, windowId, invPlayer, relay);
	}

	protected RelayMK1Container(ContainerTypeRegistryObject<? extends RelayMK1Container> type, int windowId, PlayerInventory invPlayer, RelayMK1Tile relay) {
		super(type, windowId);
		this.longFields.add(emc);
		addDataSlot(kleinChargeProgress);
		addDataSlot(inputBurnProgress);
		this.tile = relay;
		initSlots(invPlayer);
	}

	void initSlots(PlayerInventory invPlayer) {
		IItemHandler input = tile.getInput();
		IItemHandler output = tile.getOutput();
		//Klein Star charge slot
		this.addSlot(new ValidatedSlot(output, 0, 127, 43, SlotPredicates.EMC_HOLDER));
		//Burning slot
		this.addSlot(new ValidatedSlot(input, 0, 67, 43, SlotPredicates.RELAY_INV));
		int counter = 1;
		//Main Relay inventory
		for (int i = 1; i >= 0; i--) {
			for (int j = 2; j >= 0; j--) {
				this.addSlot(new ValidatedSlot(input, counter++, 27 + i * 18, 17 + j * 18, SlotPredicates.RELAY_INV));
			}
		}
		addPlayerInventory(invPlayer, 8, 95);
	}

	@Override
	public void broadcastChanges() {
		emc.set(tile.getStoredEmc());
		kleinChargeProgress.set((int) (tile.getItemChargeProportion() * 8000));
		inputBurnProgress.set((int) (tile.getInputBurnProportion() * 8000));
		super.broadcastChanges();
	}

	protected BlockRegistryObject<Relay, ?> getValidBlock() {
		return PEBlocks.RELAY;
	}

	@Override
	public boolean stillValid(@Nonnull PlayerEntity player) {
		return stillValid(player, tile, getValidBlock());
	}

	public double getKleinChargeProgress() {
		return kleinChargeProgress.get() / 8000.0;
	}

	public double getInputBurnProgress() {
		return inputBurnProgress.get() / 8000.0;
	}
}