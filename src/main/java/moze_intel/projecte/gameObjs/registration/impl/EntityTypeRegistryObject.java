package moze_intel.projecte.gameObjs.registration.impl;

import moze_intel.projecte.gameObjs.registration.WrappedRegistryObject;
import moze_intel.projecte.utils.text.IHasTranslationKey;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;

public class EntityTypeRegistryObject<ENTITY extends Entity> extends WrappedRegistryObject<EntityType<ENTITY>> implements IHasTranslationKey {

	public EntityTypeRegistryObject(RegistryObject<EntityType<ENTITY>> registryObject) {
		super(registryObject);
	}

	@Override
	public String getTranslationKey() {
		return get().getDescriptionId();
	}
}