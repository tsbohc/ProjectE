package moze_intel.projecte.gameObjs.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nonnull;
import moze_intel.projecte.PECore;
import moze_intel.projecte.gameObjs.container.EternalDensityContainer;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GUIEternalDensity extends PEContainerScreen<EternalDensityContainer> {

	private static final ResourceLocation texture = PECore.rl("textures/gui/eternal_density.png");

	public GUIEternalDensity(EternalDensityContainer container, PlayerInventory inv, ITextComponent title) {
		super(container, inv, title);
		this.imageWidth = 180;
		this.imageHeight = 180;
	}

	@Override
	public void init() {
		super.init();
		addButton(new Button(leftPos + 62, topPos + 4, 52, 20, (menu.inventory.isWhitelistMode() ? PELang.WHITELIST : PELang.BLACKLIST).translate(),
				b -> {
					menu.inventory.changeMode();
					b.setMessage(menu.inventory.isWhitelistMode() ? PELang.WHITELIST.translate() : PELang.BLACKLIST.translate());
				}));
	}

	@Override
	protected void renderBg(@Nonnull MatrixStack matrix, float partialTicks, int x, int y) {
		RenderSystem.color4f(1, 1, 1, 1);
		Minecraft.getInstance().textureManager.bind(texture);
		blit(matrix, leftPos, topPos, 0, 0, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(@Nonnull MatrixStack matrix, int x, int y) {
		//Don't render title or inventory as we don't have space
	}
}