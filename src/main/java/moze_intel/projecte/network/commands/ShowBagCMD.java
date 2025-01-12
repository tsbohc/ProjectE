package moze_intel.projecte.network.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.annotation.Nonnull;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IAlchBagProvider;
import moze_intel.projecte.gameObjs.container.AlchBagContainer;
import moze_intel.projecte.gameObjs.registries.PEItems;
import moze_intel.projecte.impl.capability.AlchBagImpl;
import moze_intel.projecte.network.commands.argument.ColorArgument;
import moze_intel.projecte.network.commands.argument.UUIDArgument;
import moze_intel.projecte.utils.text.PELang;
import moze_intel.projecte.utils.text.TextComponentUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.DyeColor;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.items.IItemHandlerModifiable;

public class ShowBagCMD {

	private static final SimpleCommandExceptionType NOT_FOUND = new SimpleCommandExceptionType(PELang.SHOWBAG_NOT_FOUND.translate());

	public static LiteralArgumentBuilder<CommandSource> register() {
		return Commands.literal("showbag")
				.requires(cs -> cs.hasPermission(2))
				.then(Commands.argument("color", new ColorArgument())
						.then(Commands.argument("target", EntityArgument.player())
								.executes(ctx -> showBag(ctx, ColorArgument.getColor(ctx, "color"), EntityArgument.getPlayer(ctx, "target"))))
						.then(Commands.argument("uuid", new UUIDArgument())
								.executes(ctx -> showBag(ctx, ColorArgument.getColor(ctx, "color"), UUIDArgument.getUUID(ctx, "uuid")))));
	}

	private static int showBag(CommandContext<CommandSource> ctx, DyeColor color, ServerPlayerEntity player) throws CommandSyntaxException {
		ServerPlayerEntity senderPlayer = ctx.getSource().getPlayerOrException();
		return showBag(senderPlayer, createContainer(senderPlayer, player, color));
	}

	private static int showBag(CommandContext<CommandSource> ctx, DyeColor color, UUID uuid) throws CommandSyntaxException {
		ServerPlayerEntity senderPlayer = ctx.getSource().getPlayerOrException();
		return showBag(senderPlayer, createContainer(senderPlayer, uuid, color));
	}

	private static int showBag(ServerPlayerEntity senderPlayer, INamedContainerProvider container) {
		NetworkHooks.openGui(senderPlayer, container, b -> {
			b.writeBoolean(false);
			b.writeBoolean(false);
		});
		return Command.SINGLE_SUCCESS;
	}

	private static INamedContainerProvider createContainer(ServerPlayerEntity sender, ServerPlayerEntity target, DyeColor color) {
		IItemHandlerModifiable inv = (IItemHandlerModifiable) target.getCapability(ProjectEAPI.ALCH_BAG_CAPABILITY)
				.orElseThrow(NullPointerException::new)
				.getBag(color);
		ITextComponent name = PELang.SHOWBAG_NAMED.translate(PEItems.getBag(color), target.getDisplayName());
		return getContainer(sender, name, inv, false, () -> target.isAlive() && !target.hasDisconnected());
	}

	private static INamedContainerProvider createContainer(ServerPlayerEntity sender, UUID target, DyeColor color) throws CommandSyntaxException {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		//Try to get the bag
		IItemHandlerModifiable inv = loadOfflineBag(server, target, color);
		GameProfile profileByUUID = server.getProfileCache().get(target);
		ITextComponent name;
		if (profileByUUID == null) {
			name = TextComponentUtil.build(PEItems.getBag(color));
		} else {
			//If we have a cache of the player, include their last known name in the name of the bag
			name = PELang.SHOWBAG_NAMED.translate(PEItems.getBag(color), profileByUUID.getName());
		}
		return getContainer(sender, name, inv, true, () -> true);
	}

	private static INamedContainerProvider getContainer(ServerPlayerEntity sender, ITextComponent name, IItemHandlerModifiable inv, boolean immutable,
			BooleanSupplier canInteractWith) {
		return new INamedContainerProvider() {
			@Nonnull
			@Override
			public ITextComponent getDisplayName() {
				return name;
			}

			@Override
			public Container createMenu(int windowId, @Nonnull PlayerInventory playerInv, @Nonnull PlayerEntity player) {
				//Note: Selected is unused for offhand
				return new AlchBagContainer(windowId, sender.inventory, Hand.OFF_HAND, inv, 0, immutable) {
					@Override
					public boolean stillValid(@Nonnull PlayerEntity player) {
						return canInteractWith.getAsBoolean();
					}
				};
			}
		};
	}

	private static IItemHandlerModifiable loadOfflineBag(MinecraftServer server, UUID playerUUID, DyeColor color) throws CommandSyntaxException {
		File playerData = server.getWorldPath(FolderName.PLAYER_DATA_DIR).toFile();
		if (playerData.exists()) {
			File player = new File(playerData, playerUUID.toString() + ".dat");
			if (player.exists() && player.isFile()) {
				try (FileInputStream in = new FileInputStream(player)) {
					CompoundNBT playerDat = CompressedStreamTools.readCompressed(in);
					CompoundNBT bagProvider = playerDat.getCompound("ForgeCaps").getCompound(AlchBagImpl.Provider.NAME.toString());

					IAlchBagProvider provider = ProjectEAPI.ALCH_BAG_CAPABILITY.getDefaultInstance();
					ProjectEAPI.ALCH_BAG_CAPABILITY.readNBT(provider, null, bagProvider);

					return (IItemHandlerModifiable) provider.getBag(color);
				} catch (IOException e) {
					// fall through to below
				}
			}
		}
		throw NOT_FOUND.create();
	}
}