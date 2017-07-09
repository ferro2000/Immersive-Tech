package ferro2000.immersivetech.client.gui;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;

import blusunrize.immersiveengineering.client.ClientUtils;
import ferro2000.immersivetech.common.blocks.metal.tileentities.TileEntitySolarTower;
import ferro2000.immersivetech.common.gui.ContainerSolarTower;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;

public class GuiSolarTower extends GuiContainer {
	
	TileEntitySolarTower tile;

	public GuiSolarTower(InventoryPlayer invPlayer, TileEntitySolarTower tile) {
		super(new ContainerSolarTower(invPlayer, tile));
		this.tile=tile;
	}
	
	@Override
	public void drawScreen(int mx, int my, float partial)
	{
		super.drawScreen(mx, my, partial);
		
		ArrayList<String> tooltip = new ArrayList();
		ClientUtils.handleGuiTank(tile.tanks[0], guiLeft+62,guiTop+21, 16,47, 177,31,20,51, mx,my, "immersivetech:textures/gui/guiSolarTower.png", tooltip);
		ClientUtils.handleGuiTank(tile.tanks[1], guiLeft+112,guiTop+21, 16,47, 177,31,20,51, mx,my, "immersivetech:textures/gui/guiSolarTower.png", tooltip);
		if(mx>guiLeft+157&&mx<guiLeft+164 && my>guiTop+21&&my<guiTop+67)
			tooltip.add(tile.getEnergyStored(null)+"/"+tile.getMaxEnergyStored(null)+" RF");

		if(!tooltip.isEmpty())
		{
			ClientUtils.drawHoveringText(tooltip, mx, my, fontRendererObj, guiLeft+xSize,-1);
			RenderHelper.enableGUIStandardItemLighting();
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mx, int my) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		ClientUtils.bindTexture("immersivetech:textures/gui/guiSolarTower.png");
		this.drawTexturedModalRect(guiLeft,guiTop, 0, 0, xSize, ySize);
		
		int stored = (int)(46*(tile.getEnergyStored(null)/(float)tile.getMaxEnergyStored(null)));
		ClientUtils.drawGradientRect(guiLeft+158,guiTop+22+(46-stored), guiLeft+165,guiTop+68, 0xffb51500, 0xff600b00);

		ClientUtils.handleGuiTank(tile.tanks[0], guiLeft+62,guiTop+21, 16,47, 177,31,20,51, mx,my, "immersivetech:textures/gui/guiSolarTower.png", null);
		ClientUtils.handleGuiTank(tile.tanks[1], guiLeft+112,guiTop+21, 16,47, 177,31,20,51, mx,my, "immersivetech:textures/gui/guiSolarTower.png", null);
	}

}