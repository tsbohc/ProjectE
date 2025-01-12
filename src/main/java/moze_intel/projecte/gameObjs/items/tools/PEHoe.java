package moze_intel.projecte.gameObjs.items.tools;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import moze_intel.projecte.api.capabilities.item.IItemCharge;
import moze_intel.projecte.capability.ChargeItemCapabilityWrapper;
import moze_intel.projecte.capability.ItemCapabilityWrapper;
import moze_intel.projecte.gameObjs.EnumMatterType;
import moze_intel.projecte.utils.ToolHelper;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResultType;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class PEHoe extends HoeItem implements IItemCharge {

	private final EnumMatterType matterType;
	private final int numCharges;

	public PEHoe(EnumMatterType matterType, int numCharges, Properties props) {
		super(matterType, (int) -matterType.getAttackDamageBonus(), matterType.getMatterTier(), props);
		this.matterType = matterType;
		this.numCharges = numCharges;
	}

	@Override
	public boolean isEnchantable(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
		return false;
	}

	@Override
	public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
		return false;
	}

	@Override
	public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
		return 0;
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return true;
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return 1.0D - getChargePercent(stack);
	}

	@Override
	public float getDestroySpeed(@Nonnull ItemStack stack, @Nonnull BlockState state) {
		return ToolHelper.getDestroySpeed(super.getDestroySpeed(stack, state), matterType, getCharge(stack));
	}

	@Override
	public int getNumCharges(@Nonnull ItemStack stack) {
		return numCharges;
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, CompoundNBT nbt) {
		return new ItemCapabilityWrapper(stack, new ChargeItemCapabilityWrapper());
	}

	@Override
	public boolean isCorrectToolForDrops(BlockState state) {
		if (state.getHarvestTool() == ToolType.HOE) {
			//Patch HoeItem to return true for canHarvestBlock if a mod adds a block with the harvest tool of a hoe
			return getTier().getLevel() >= state.getHarvestLevel();
		}
		return super.isCorrectToolForDrops(state);
	}

	@Nonnull
	@Override
	public ActionResultType useOn(@Nonnull ItemUseContext context) {
		return ToolHelper.tillHoeAOE(context, 0);
	}
}