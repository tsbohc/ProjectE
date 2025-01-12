package moze_intel.projecte.gameObjs.items.armor;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.IFireProtector;
import moze_intel.projecte.handlers.InternalTimers;
import moze_intel.projecte.utils.WorldHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class GemChest extends GemArmorBase implements IFireProtector {

	public GemChest(Properties props) {
		super(EquipmentSlotType.CHEST, props);
	}

	@Override
	public void appendHoverText(@Nonnull ItemStack stack, @Nullable World world, @Nonnull List<ITextComponent> tooltips, @Nonnull ITooltipFlag flags) {
		super.appendHoverText(stack, world, tooltips, flags);
		tooltips.add(PELang.GEM_LORE_CHEST.translate());
	}

	@Override
	public void onArmorTick(ItemStack chest, World world, PlayerEntity player) {
		if (!world.isClientSide) {
			player.getCapability(InternalTimers.CAPABILITY).ifPresent(timers -> {
				timers.activateFeed();
				if (player.getFoodData().needsFood() && timers.canFeed()) {
					player.getFoodData().eat(2, 10);
				}
			});
		}
	}

	public void doExplode(PlayerEntity player) {
		if (ProjectEConfig.server.difficulty.offensiveAbilities.get()) {
			WorldHelper.createNovaExplosion(player.getCommandSenderWorld(), player, player.getX(), player.getY(), player.getZ(), 9.0F);
		}
	}

	@Override
	public boolean canProtectAgainstFire(ItemStack stack, ServerPlayerEntity player) {
		return player.getItemBySlot(EquipmentSlotType.CHEST) == stack;
	}
}