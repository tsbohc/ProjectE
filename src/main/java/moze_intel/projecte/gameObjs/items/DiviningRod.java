package moze_intel.projecte.gameObjs.items;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import moze_intel.projecte.capability.ModeChangerItemCapabilityWrapper;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.ILangEntry;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.crafting.FurnaceRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.NonNullLazy;

public class DiviningRod extends ItemPE implements IItemMode {

	private final ILangEntry[] modes;
	private final int maxModes;

	public DiviningRod(Properties props, ILangEntry... modeDesc) {
		super(props);
		modes = modeDesc;
		maxModes = modes.length;
		addItemCapability(ModeChangerItemCapabilityWrapper::new);
	}

	@Nonnull
	@Override
	public ActionResultType useOn(ItemUseContext ctx) {
		PlayerEntity player = ctx.getPlayer();
		if (player == null) {
			return ActionResultType.FAIL;
		}
		World world = ctx.getLevel();
		if (world.isClientSide) {
			return ActionResultType.SUCCESS;
		}
		LongList emcValues = new LongArrayList();
		long totalEmc = 0;
		int numBlocks = 0;
		int depth = getDepthFromMode(ctx.getItemInHand());
		//Lazily retrieve the values for the furnace recipes
		NonNullLazy<List<FurnaceRecipe>> furnaceRecipes = NonNullLazy.of(() -> world.getRecipeManager().getAllRecipesFor(IRecipeType.SMELTING));
		for (BlockPos digPos : WorldHelper.getPositionsFromBox(WorldHelper.getDeepBox(ctx.getClickedPos(), ctx.getClickedFace(), depth))) {
			if (world.isEmptyBlock(digPos)) {
				continue;
			}
			BlockState state = world.getBlockState(digPos);
			List<ItemStack> drops = Block.getDrops(state, (ServerWorld) world, digPos, WorldHelper.getTileEntity(world, digPos), player, ctx.getItemInHand());
			if (drops.isEmpty()) {
				continue;
			}
			ItemStack blockStack = drops.get(0);
			long blockEmc = EMCHelper.getEmcValue(blockStack);
			if (blockEmc == 0) {
				for (FurnaceRecipe furnaceRecipe : furnaceRecipes.get()) {
					if (furnaceRecipe.getIngredients().get(0).test(blockStack)) {
						long currentValue = EMCHelper.getEmcValue(furnaceRecipe.getResultItem());
						if (currentValue != 0) {
							if (!emcValues.contains(currentValue)) {
								emcValues.add(currentValue);
							}
							totalEmc += currentValue;
							break;
						}
					}
				}
			} else {
				if (!emcValues.contains(blockEmc)) {
					emcValues.add(blockEmc);
				}
				totalEmc += blockEmc;
			}
			numBlocks++;
		}

		if (numBlocks == 0) {
			return ActionResultType.FAIL;
		}
		player.sendMessage(PELang.DIVINING_AVG_EMC.translate(numBlocks, totalEmc / numBlocks), Util.NIL_UUID);
		if (this == PEItems.MEDIUM_DIVINING_ROD.get() || this == PEItems.HIGH_DIVINING_ROD.get()) {
			long[] maxValues = new long[3];
			for (int i = 0; i < 3; i++) {
				maxValues[i] = 1;
			}
			emcValues.sort(Comparator.reverseOrder());
			int num = Math.min(emcValues.size(), 3);
			for (int i = 0; i < num; i++) {
				maxValues[i] = emcValues.getLong(i);
			}
			player.sendMessage(PELang.DIVINING_MAX_EMC.translate(maxValues[0]), Util.NIL_UUID);
			if (this == PEItems.HIGH_DIVINING_ROD.get()) {
				player.sendMessage(PELang.DIVINING_SECOND_MAX.translate(maxValues[1]), Util.NIL_UUID);
				player.sendMessage(PELang.DIVINING_THIRD_MAX.translate(maxValues[2]), Util.NIL_UUID);
			}
		}
		return ActionResultType.SUCCESS;
	}

	private int getDepthFromMode(ItemStack stack) {
		byte mode = getMode(stack);
		if (mode < 0 || mode >= maxModes) {
			//No range something went wrong
			return 0;
		} else if (mode == 0) {
			return 3;
		} else if (mode == 1) {
			return 16;
		}//mode == 2
		return 64;
	}

	@Override
	public ILangEntry[] getModeLangEntries() {
		return modes;
	}

	@Override
	public void appendHoverText(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<ITextComponent> tooltips, @Nonnull ITooltipFlag flags) {
		super.appendHoverText(stack, world, tooltips, flags);
		tooltips.add(getToolTip(stack));
	}
}