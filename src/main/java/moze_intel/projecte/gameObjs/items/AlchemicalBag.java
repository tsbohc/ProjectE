package moze_intel.projecte.gameObjs.items;

import java.util.Optional;
import javax.annotation.Nonnull;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import moze_intel.projecte.gameObjs.items.rings.BlackHoleBand;
import moze_intel.projecte.gameObjs.items.rings.VoidRing;
import moze_intel.projecte.utils.Constants;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

public class AlchemicalBag extends ItemPE {

	public final DyeColor color;

	public AlchemicalBag(Properties props, DyeColor color) {
		super(props);
		this.color = color;
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> use(@Nonnull World world, @Nonnull PlayerEntity player, @Nonnull Hand hand) {
		if (!world.isClientSide) {
			NetworkHooks.openGui((ServerPlayerEntity) player, new ContainerProvider(player.getItemInHand(hand), hand), buf -> {
				buf.writeEnum(hand);
				buf.writeByte(player.inventory.selected);
				buf.writeBoolean(false);
			});
		}

		return ActionResult.success(player.getItemInHand(hand));
	}

	public static ItemStack getFirstBagWithSuctionItem(PlayerEntity player, NonNullList<ItemStack> inventory) {
		Optional<IAlchBagProvider> cap = Optional.empty();
		for (ItemStack stack : inventory) {
			if (!stack.isEmpty() && stack.getItem() instanceof AlchemicalBag) {
				if (!cap.isPresent()) {
					cap = player.getCapability(ProjectEAPI.ALCH_BAG_CAPABILITY).resolve();
					if (!cap.isPresent()) {
						//If the player really doesn't have the capability and it isn't just not not loaded yet, exit
						break;
					}
				}
				IItemHandler inv = cap.get().getBag(((AlchemicalBag) stack.getItem()).color);
				for (int i = 0; i < inv.getSlots(); i++) {
					ItemStack ring = inv.getStackInSlot(i);
					if (!ring.isEmpty() && (ring.getItem() instanceof BlackHoleBand || ring.getItem() instanceof VoidRing)) {
						if (ItemHelper.checkItemNBT(ring, Constants.NBT_KEY_ACTIVE)) {
							return stack;
						}
					}
				}
			}
		}
		return ItemStack.EMPTY;
	}

	private class ContainerProvider implements INamedContainerProvider {

		private final ItemStack stack;
		private final Hand hand;

		private ContainerProvider(ItemStack stack, Hand hand) {
			this.stack = stack;
			this.hand = hand;
		}

		@Nonnull
		@Override
		public Container createMenu(int windowId, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity player) {
			IItemHandlerModifiable inv = (IItemHandlerModifiable) player.getCapability(ProjectEAPI.ALCH_BAG_CAPABILITY)
					.orElseThrow(NullPointerException::new)
					.getBag(color);
			return new AlchBagContainer(windowId, playerInventory, hand, inv, playerInventory.selected, false);
		}

		@Nonnull
		@Override
		public ITextComponent getDisplayName() {
			return stack.getHoverName();
		}
	}
}