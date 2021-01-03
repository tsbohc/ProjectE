package moze_intel.projecte.common.tag;

import javax.annotation.Nullable;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.PETags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.EntityTypeTagsProvider;
import net.minecraft.entity.EntityType;
import net.minecraftforge.common.data.ExistingFileHelper;

public class PEEntityTypeTagsProvider extends EntityTypeTagsProvider {

	public PEEntityTypeTagsProvider(DataGenerator generator, @Nullable ExistingFileHelper existingFileHelper) {
		super(generator, PECore.MODID, existingFileHelper);
	}

	@Override
	protected void registerTags() {
		getOrCreateBuilder(PETags.Entities.RANDOMIZER_PEACEFUL).add(
				EntityType.SHEEP,
				EntityType.PIG,
				EntityType.COW,
				EntityType.MOOSHROOM,
				EntityType.CHICKEN,
				EntityType.BAT,
				EntityType.VILLAGER,
				EntityType.SQUID,
				EntityType.OCELOT,
				EntityType.WOLF,
				EntityType.HORSE,
				EntityType.RABBIT,
				EntityType.DONKEY,
				EntityType.MULE,
				EntityType.POLAR_BEAR,
				EntityType.LLAMA,
				EntityType.PARROT,
				EntityType.DOLPHIN,
				EntityType.COD,
				EntityType.SALMON,
				EntityType.PUFFERFISH,
				EntityType.TROPICAL_FISH,
				EntityType.TURTLE,
				EntityType.CAT,
				EntityType.FOX,
				EntityType.PANDA,
				EntityType.TRADER_LLAMA,
				EntityType.WANDERING_TRADER,
				EntityType.STRIDER
		);
		getOrCreateBuilder(PETags.Entities.RANDOMIZER_HOSTILE).add(
				EntityType.ZOMBIE,
				EntityType.SKELETON,
				EntityType.CREEPER,
				EntityType.SPIDER,
				EntityType.ENDERMAN,
				EntityType.SILVERFISH,
				EntityType.ZOMBIFIED_PIGLIN,
				EntityType.PIGLIN,
				EntityType.field_242287_aj,
				EntityType.HOGLIN,
				EntityType.ZOGLIN,
				EntityType.GHAST,
				EntityType.BLAZE,
				EntityType.SLIME,
				EntityType.WITCH,
				EntityType.RABBIT,
				EntityType.ENDERMITE,
				EntityType.STRAY,
				EntityType.WITHER_SKELETON,
				EntityType.SKELETON_HORSE,
				EntityType.ZOMBIE_HORSE,
				EntityType.ZOMBIE_VILLAGER,
				EntityType.HUSK,
				EntityType.GUARDIAN,
				EntityType.EVOKER,
				EntityType.VEX,
				EntityType.VINDICATOR,
				EntityType.SHULKER,
				EntityType.DROWNED,
				EntityType.PHANTOM,
				EntityType.PILLAGER
		);
		getOrCreateBuilder(PETags.Entities.BLACKLIST_SWRG);
		getOrCreateBuilder(PETags.Entities.BLACKLIST_INTERDICTION);
	}
}