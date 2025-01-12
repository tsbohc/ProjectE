package moze_intel.projecte.handlers;

import javax.annotation.Nonnull;
import moze_intel.projecte.PECore;
import moze_intel.projecte.capability.managing.BasicCapabilityResolver;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.items.IFlightProvider;
import moze_intel.projecte.gameObjs.items.IStepAssister;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.utils.PlayerHelper;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public final class InternalAbilities {

	@CapabilityInject(InternalAbilities.class)
	public static Capability<InternalAbilities> CAPABILITY = null;
	public static final ResourceLocation NAME = PECore.rl("internal_abilities");

	private final ServerPlayerEntity player;
	private boolean swrgOverride = false;
	private boolean gemArmorReady = false;
	private boolean stepAssisted = false;
	private boolean hadFlightItem = false;
	private boolean wasFlyingGamemode = false;
	private boolean isFlyingGamemode = false;
	private boolean wasFlying = false;
	private int projectileCooldown = 0;
	private int gemChestCooldown = 0;

	public InternalAbilities(ServerPlayerEntity player) {
		this.player = player;
	}

	public void resetProjectileCooldown() {
		projectileCooldown = ProjectEConfig.server.cooldown.player.projectile.get();
	}

	public int getProjectileCooldown() {
		return projectileCooldown;
	}

	public void resetGemCooldown() {
		gemChestCooldown = ProjectEConfig.server.cooldown.player.gemChest.get();
	}

	public int getGemCooldown() {
		return gemChestCooldown;
	}

	public void setGemState(boolean state) {
		gemArmorReady = state;
	}

	public boolean getGemState() {
		return gemArmorReady;
	}

	// Checks if the server state of player caps mismatches with what ProjectE determines. If so, change it serverside and send a packet to client
	public void tick() {
		if (projectileCooldown > 0) {
			projectileCooldown--;
		}

		if (gemChestCooldown > 0) {
			gemChestCooldown--;
		}

		if (!shouldPlayerFly()) {
			if (hadFlightItem) {
				if (player.abilities.mayfly) {
					PlayerHelper.updateClientServerFlight(player, false);
				}
				hadFlightItem = false;
			}
			wasFlyingGamemode = false;
			wasFlying = false;
		} else {
			if (!hadFlightItem) {
				if (!player.abilities.mayfly) {
					PlayerHelper.updateClientServerFlight(player, true);
				}
				hadFlightItem = true;
			} else if (wasFlyingGamemode && !isFlyingGamemode) {
				//Player was in a gamemode that allowed flight, but no longer is, but they still should be allowed to fly
				//Sync the fact to the client. Also passes wasFlying so that if they were flying previously,
				//and are still allowed to the gamemode change doesn't force them out of it
				PlayerHelper.updateClientServerFlight(player, true, wasFlying);
			}
			wasFlyingGamemode = isFlyingGamemode;
			wasFlying = player.abilities.flying;
		}
		if (!shouldPlayerStep()) {
			if (stepAssisted) {
				//If we don't have step assist but we previously did, then lower the step height
				stepAssisted = false;
				PlayerHelper.updateClientServerStepHeight(player, 0.6F);
			}
		} else if (!stepAssisted) {
			//If we should be able to have auto step, but we don't have it set yet, enable it
			stepAssisted = true;
			PlayerHelper.updateClientServerStepHeight(player, 1.0F);
		}
	}

	public void onDimensionChange() {
		// Resend everything needed on clientside (all except fire resist)
		PlayerHelper.updateClientServerFlight(player, player.abilities.mayfly);
		PlayerHelper.updateClientServerStepHeight(player, stepAssisted ? 1.0F : 0.6F);
	}

	private boolean shouldPlayerFly() {
		if (!hasSwrg()) {
			disableSwrgFlightOverride();
		}
		isFlyingGamemode = player.isCreative() || player.isSpectator();
		if (isFlyingGamemode || swrgOverride) {
			return true;
		}
		return PlayerHelper.checkArmorHotbarCurios(player, stack -> !stack.isEmpty() && stack.getItem() instanceof IFlightProvider && ((IFlightProvider) stack.getItem()).canProvideFlight(stack, player));
	}

	private boolean shouldPlayerStep() {
		return PlayerHelper.checkArmorHotbarCurios(player, stack -> !stack.isEmpty() && stack.getItem() instanceof IStepAssister && ((IStepAssister) stack.getItem()).canAssistStep(stack, player));
	}

	private boolean hasSwrg() {
		return PlayerHelper.checkHotbarCurios(player, stack -> !stack.isEmpty() && stack.getItem() == PEItems.SWIFTWOLF_RENDING_GALE.get());
	}

	public void enableSwrgFlightOverride() {
		swrgOverride = true;
	}

	public void disableSwrgFlightOverride() {
		swrgOverride = false;
	}

	public static class Provider extends BasicCapabilityResolver<InternalAbilities> {

		public Provider(ServerPlayerEntity player) {
			super(() -> new InternalAbilities(player));
		}

		@Nonnull
		@Override
		public Capability<InternalAbilities> getMatchingCapability() {
			return CAPABILITY;
		}
	}
}