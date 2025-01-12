package moze_intel.projecte.gameObjs.items.armor;

import javax.annotation.Nonnull;
import moze_intel.projecte.PECore;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.IArmorMaterial;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

public abstract class GemArmorBase extends PEArmor {

	public GemArmorBase(EquipmentSlotType armorType, Properties props) {
		super(GemArmorMaterial.INSTANCE, armorType, props);
	}

	@Override
	public float getFullSetBaseReduction() {
		return 0.9F;
	}

	@Override
	public float getMaxDamageAbsorb(EquipmentSlotType slot, DamageSource source) {
		if (source.isExplosion()) {
			return 750;
		}
		if (slot == EquipmentSlotType.FEET && source == DamageSource.FALL) {
			return 15 / getPieceEffectiveness(slot);
		} else if (slot == EquipmentSlotType.HEAD && source == DamageSource.DROWN) {
			return 15 / getPieceEffectiveness(slot);
		}
		if (source.isBypassArmor()) {
			return 0;
		}
		//If the source is not unblockable, allow our piece to block a certain amount of damage
		if (slot == EquipmentSlotType.HEAD || slot == EquipmentSlotType.FEET) {
			return 400;
		}
		return 500;
	}

	public static boolean hasAnyPiece(PlayerEntity player) {
		return player.inventory.armor.stream().anyMatch(i -> !i.isEmpty() && i.getItem() instanceof GemArmorBase);
	}

	public static boolean hasFullSet(PlayerEntity player) {
		return player.inventory.armor.stream().noneMatch(i -> i.isEmpty() || !(i.getItem() instanceof GemArmorBase));
	}

	private static class GemArmorMaterial implements IArmorMaterial {

		private static final GemArmorMaterial INSTANCE = new GemArmorMaterial();

		@Override
		public int getDurabilityForSlot(@Nonnull EquipmentSlotType slot) {
			return 0;
		}

		@Override
		public int getDefenseForSlot(@Nonnull EquipmentSlotType slot) {
			if (slot == EquipmentSlotType.FEET) {
				return 3;
			} else if (slot == EquipmentSlotType.LEGS) {
				return 6;
			} else if (slot == EquipmentSlotType.CHEST) {
				return 8;
			} else if (slot == EquipmentSlotType.HEAD) {
				return 3;
			}
			return 0;
		}

		@Override
		public int getEnchantmentValue() {
			return 0;
		}

		@Nonnull
		@Override
		public SoundEvent getEquipSound() {
			return SoundEvents.ARMOR_EQUIP_DIAMOND;
		}

		@Nonnull
		@Override
		public Ingredient getRepairIngredient() {
			return Ingredient.EMPTY;
		}

		@Nonnull
		@Override
		public String getName() {
			//Only used on the client
			return PECore.rl("gem_armor").toString();
		}

		@Override
		public float getToughness() {
			return 2;
		}

		@Override
		public float getKnockbackResistance() {
			return 0.25F;
		}
	}
}