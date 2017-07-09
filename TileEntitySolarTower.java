package ferro2000.immersivetech.common.blocks.metal.tileentities;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.Lists;

import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IAdvancedCollisionBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IAdvancedSelectionBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IGuiTile;
import blusunrize.immersiveengineering.common.blocks.metal.TileEntityMultiblockMetal;
import blusunrize.immersiveengineering.common.util.Utils;
import ferro2000.immersivetech.api.ITLib;
import ferro2000.immersivetech.api.craftings.SolarTowerRecipes;
import ferro2000.immersivetech.common.blocks.metal.multiblocks.MultiblockSolarTower;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.oredict.OreDictionary;

public class TileEntitySolarTower extends TileEntityMultiblockMetal<TileEntitySolarTower, SolarTowerRecipes> implements IGuiTile, IAdvancedSelectionBounds,IAdvancedCollisionBounds {

	public TileEntitySolarTower()
	{
		super(MultiblockSolarTower.instance, new int[]{7, 3, 3}, 0, true);
	}
	
	public FluidTank[] tanks = new FluidTank[]{new FluidTank(32000),new FluidTank(32000)};
	public ItemStack[] inventory = new ItemStack[4];

	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		tanks[0].readFromNBT(nbt.getCompoundTag("tank0"));
		tanks[1].readFromNBT(nbt.getCompoundTag("tank1"));
		if(!descPacket)
			inventory = Utils.readInventory(nbt.getTagList("inventory", 10), 6);
	}
	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		nbt.setTag("tank0", tanks[0].writeToNBT(new NBTTagCompound()));
		nbt.setTag("tank1", tanks[1].writeToNBT(new NBTTagCompound()));
		if(!descPacket)
			nbt.setTag("inventory", Utils.writeInventory(inventory));
	}
	
	private boolean wasActive = false;
	
	@Override
	public void update() {
		super.update();
		if(worldObj.isRemote || isDummy())
			return;
		boolean update = false;
	
		if(processQueue.size()<this.getProcessQueueMaxLength())
		{
			if(tanks[0].getFluidAmount() > 0)
			{
				SolarTowerRecipes recipe = SolarTowerRecipes.findRecipe(tanks[0].getFluid());
				if(recipe!=null)
				{
					MultiblockProcessInMachine<SolarTowerRecipes> process = new MultiblockProcessInMachine(recipe).setInputTanks(new int[] {0});
					if(this.addProcessToQueue(process, true))
					{
						this.addProcessToQueue(process, false);
						update = true;
					}
				}
			}
		}
		
		if (processQueue.size() > 0)
		{
			wasActive = true;
		}
		else if (wasActive)
		{
			wasActive = false;
			update = true;
		}

		if (this.tanks[1].getFluidAmount()>0)
		{

			ItemStack filledContainer = Utils.fillFluidContainer(tanks[1], inventory[2], inventory[3], null);
			if(filledContainer!=null)
			{
				if(inventory[3]!=null && OreDictionary.itemMatches(inventory[3], filledContainer, true))
					inventory[3].stackSize+=filledContainer.stackSize;
				else if(inventory[3]==null)
					inventory[3] = filledContainer.copy();
				if(--inventory[2].stackSize<=0)
					inventory[2]=null;
				update = true;
			}
			
			if(this.tanks[1].getFluidAmount()>0)
			{
				FluidStack out = Utils.copyFluidStackWithAmount(this.tanks[1].getFluid(), Math.min(this.tanks[1].getFluidAmount(), 80), false);
				BlockPos outputPos = this.getPos().add(0,-1,0).offset(facing,3);	
				
				IFluidHandler output = FluidUtil.getFluidHandler(worldObj, outputPos, facing);
				if(output!=null)
				{
					int accepted = output.fill(out, false);
					if(accepted>0)
					{
						int drained = output.fill(Utils.copyFluidStackWithAmount(out,Math.min(out.amount, accepted),false), true);
						this.tanks[1].drain(drained, true);
						update=true;
					}
				}
			}
			
		}

		ItemStack emptyContainer = Utils.drainFluidContainer(tanks[0], inventory[0], inventory[1], null);
		if(emptyContainer!=null && emptyContainer.stackSize>0)
		{
			if(inventory[1]!=null && OreDictionary.itemMatches(inventory[1], emptyContainer, true))
				inventory[1].stackSize+=emptyContainer.stackSize;
			else if(inventory[1]==null)
				inventory[1] = emptyContainer.copy();
			if(--inventory[0].stackSize<=0)
				inventory[0]=null;
			update = true;
		}

		if(update)
		{
			this.markDirty();
			this.markContainingBlockForUpdate(null);
		}
	}
	
	@Override
	public ItemStack[] getInventory() {
		return inventory;
	}

	@Override
	public boolean isStackValid(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	@Override
	public void doGraphicalUpdates(int slot) {
		this.markDirty();
		this.markContainingBlockForUpdate(null);
	}

	@Override
	public float[] getBlockBounds() {
		if(pos==0||pos==2||pos==6||pos==8) {
			return new float[] {0,0,0, 1,.5f,1};
		}
		if(pos==62||pos==60||pos==54||pos==52) {
			return new float[] {0,.5f,0, 1,1,1};
		}
		return new float[] {0,0,0, 1,1,1};
	}

	@Override
	public boolean canOpenGui() {
		return formed;
	}

	@Override
	public int getGuiID() {
		return ITLib.GUIID_Solar_Tower;
	}

	@Override
	public TileEntity getGuiMaster() {
		return master();
	}

	@Override
	protected SolarTowerRecipes readRecipeFromNBT(NBTTagCompound tag) {
		return SolarTowerRecipes.loadFromNBT(tag);
	}

	@Override
	public int[] getEnergyPos() {
		return null;
	}

	@Override
	public int[] getRedstonePos() {
		return new int[] {10};
	}

	@Override
	public IFluidTank[] getInternalTanks() {
		return tanks;
	}

	@Override
	public SolarTowerRecipes findRecipeForInsertion(ItemStack inserting) {
		return null;
	}

	@Override
	public int[] getOutputSlots() {
		return null;
	}

	@Override
	public int[] getOutputTanks() {
		return new int[] {1};
	}

	@Override
	public boolean additionalCanProcessCheck(MultiblockProcess<SolarTowerRecipes> process) {
		return true;
	}

	@Override
	public void doProcessOutput(ItemStack output) {
		BlockPos pos = getPos().offset(facing,2);
		TileEntity inventoryTile = this.worldObj.getTileEntity(pos);
		if(inventoryTile!=null)
			output = Utils.insertStackIntoInventory(inventoryTile, output, facing.getOpposite());
		if(output!=null)
			Utils.dropStackAtPos(worldObj, pos, output, facing);
	}

	@Override
	public void doProcessFluidOutput(FluidStack output) {
		
	}

	@Override
	public void onProcessFinish(MultiblockProcess<SolarTowerRecipes> process) {
		
	}

	@Override
	public int getMaxProcessPerTick() {
		return 1;
	}

	@Override
	public int getProcessQueueMaxLength() {
		return 1;
	}

	@Override
	public float getMinProcessDistance(MultiblockProcess<SolarTowerRecipes> process) {
		return 0;
	}

	@Override
	public boolean isInWorldProcessingMachine() {
		return false;
	}

	@Override
	protected IFluidTank[] getAccessibleFluidTanks(EnumFacing side) {
		TileEntitySolarTower master = this.master();
		if(master != null)
		{
			if((pos==3||pos == 5) && (side == null || side.getAxis() == facing.rotateYCCW().getAxis()))
			{
				return new FluidTank[] {master.tanks[0]};
			}
			else if(pos==7 && (side==null || side==facing)) 
			{
				return new FluidTank[] {master.tanks[1]};
			}
		}
		return new FluidTank[0];
	}

	@Override
	protected boolean canFillTankFrom(int iTank, EnumFacing side, FluidStack resource) {
		if((pos==3||pos == 5) && (side == null || side.getAxis() == facing.rotateYCCW().getAxis()))
		{
			TileEntitySolarTower master = this.master();
			FluidStack resourceClone = Utils.copyFluidStackWithAmount(resource, 1000, false);
			FluidStack resourceClone2 = Utils.copyFluidStackWithAmount(master.tanks[0].getFluid(), 1000, false);


			if(master==null || master.tanks[iTank].getFluidAmount()>=master.tanks[iTank].getCapacity())
				return false;
			if(master.tanks[0].getFluid()==null)
			{
				SolarTowerRecipes incompleteRecipes = SolarTowerRecipes.findRecipe(resourceClone);
				return incompleteRecipes!=null;
			}
			else
			{
				SolarTowerRecipes incompleteRecipes1 = SolarTowerRecipes.findRecipe(resourceClone);
				SolarTowerRecipes incompleteRecipes2 = SolarTowerRecipes.findRecipe(resourceClone2);
				return incompleteRecipes1 == incompleteRecipes2;
			}
		}
		return false;
	}

	@Override
	protected boolean canDrainTankFrom(int iTank, EnumFacing side) {
		return (pos==7 && (side==null || side==facing));
	}
	
	@Override
	public TileEntitySolarTower getTileForPos(int targetPos)
	{
		BlockPos target = getBlockPosForPos(targetPos);
		TileEntity tile = worldObj.getTileEntity(target);
		return tile instanceof TileEntitySolarTower ? (TileEntitySolarTower) tile : null;
	}
	@Override
	public List<AxisAlignedBB> getAdvancedColisionBounds() {
		return getAdvancedSelectionBounds();
	}
	@Override
	public List<AxisAlignedBB> getAdvancedSelectionBounds() {
		EnumFacing fl = facing;
		EnumFacing fw = facing.rotateY();
		
		int h = (pos-(pos%9))/9;
		
		if(pos==0||pos==2||pos==6||pos==8||pos==62||pos==60||pos==56||pos==54){
			float minY = 0;
			float maxY = .5f;
			if(h==6) {
				minY = .5f;
				maxY = 1;
			}
			List<AxisAlignedBB> list = Lists.newArrayList(new AxisAlignedBB(0,minY,0, 1,maxY,1).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			if(pos==6||pos==8||pos==60||pos==62) {
				fl = fl.getOpposite();
			}
			if(pos==2||pos==8||pos==56||pos==62){
				fw = fw.getOpposite();
			}
			float minX = fl==EnumFacing.WEST?.6875f: fl==EnumFacing.EAST?.0625f: fw==EnumFacing.EAST?.0625f: .6875f;
			float maxX = fl==EnumFacing.EAST?.3125f: fl==EnumFacing.WEST?.9375f: fw==EnumFacing.EAST?.3125f: .9375f;
			float minZ = fl==EnumFacing.NORTH?.6875f: fl==EnumFacing.SOUTH?.0625f: fw==EnumFacing.SOUTH?.0625f: .6875f;
			float maxZ = fl==EnumFacing.SOUTH?.3125f: fl==EnumFacing.NORTH?.9375f: fw==EnumFacing.SOUTH?.3125f: .9375f;
			minY = .5f;
			maxY = 1;
			if(pos>8) {
				minY = 0;
				maxY = .5f;
			}
			
			list.add(new AxisAlignedBB(minX, minY, minZ, maxX, maxY,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minX = fl==EnumFacing.EAST?.5f: fl==EnumFacing.WEST?0: fw==EnumFacing.EAST?.5f: 0;
			maxX = fl==EnumFacing.EAST?1: fl==EnumFacing.WEST?.5f: fw==EnumFacing.EAST?1: .5f;
			
			list.add(new AxisAlignedBB(minX, minY, 0, maxX, maxY,1).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minZ = fl==EnumFacing.NORTH?0: fl==EnumFacing.SOUTH?.5f: fw==EnumFacing.NORTH?0: .5f;
			maxZ = fl==EnumFacing.NORTH?.5f: fl==EnumFacing.SOUTH?1: fw==EnumFacing.NORTH?.5f: 1;
			
			list.add(new AxisAlignedBB(0, minY, minZ, 1, maxY,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			return list;
			
		}
		if((pos!=0 && pos!=54 &&(pos%9==0))) {
			
			float minX = fl==EnumFacing.WEST?.6875f: fl==EnumFacing.EAST?.0625f: fw==EnumFacing.EAST?.0625f: .6875f;
			float maxX = fl==EnumFacing.EAST?.3125f: fl==EnumFacing.WEST?.9375f: fw==EnumFacing.EAST?.3125f: .9375f;
			float minZ = fl==EnumFacing.NORTH?.6875f: fl==EnumFacing.SOUTH?.0625f: fw==EnumFacing.SOUTH?.0625f: .6875f;
			float maxZ = fl==EnumFacing.SOUTH?.3125f: fl==EnumFacing.NORTH?.9375f: fw==EnumFacing.SOUTH?.3125f: .9375f;
			
			List<AxisAlignedBB> list = Lists.newArrayList(new AxisAlignedBB(minX, 0, minZ, maxX, 1,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
						
			minX = fl==EnumFacing.EAST?.5f: fl==EnumFacing.WEST?0: fw==EnumFacing.EAST?.5f: 0;
			maxX = fl==EnumFacing.EAST?1: fl==EnumFacing.WEST?.5f: fw==EnumFacing.EAST?1: .5f;
			
			list.add(new AxisAlignedBB(minX, 0, 0, maxX, 1,1).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minZ = fl==EnumFacing.NORTH?0: fl==EnumFacing.SOUTH?.5f: fw==EnumFacing.NORTH?0: .5f;
			maxZ = fl==EnumFacing.NORTH?.5f: fl==EnumFacing.SOUTH?1: fw==EnumFacing.NORTH?.5f: 1;
			
			list.add(new AxisAlignedBB(0, 0, minZ, 1, 1,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			return list;
			
		}
		if(pos==2+9*h) {
			
			fw = fw.getOpposite();
			
			float minX = fl==EnumFacing.WEST?.6875f: fl==EnumFacing.EAST?.0625f: fw==EnumFacing.EAST?.0625f: .6875f;
			float maxX = fl==EnumFacing.EAST?.3125f: fl==EnumFacing.WEST?.9375f: fw==EnumFacing.EAST?.3125f: .9375f;
			float minZ = fl==EnumFacing.NORTH?.6875f: fl==EnumFacing.SOUTH?.0625f: fw==EnumFacing.SOUTH?.0625f: .6875f;
			float maxZ = fl==EnumFacing.SOUTH?.3125f: fl==EnumFacing.NORTH?.9375f: fw==EnumFacing.SOUTH?.3125f: .9375f;
			
			List<AxisAlignedBB> list = Lists.newArrayList(new AxisAlignedBB(minX, 0, minZ, maxX, 1,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minX = fl==EnumFacing.EAST?.5f: fl==EnumFacing.WEST?0: fw==EnumFacing.EAST?.5f: 0;
			maxX = fl==EnumFacing.EAST?1: fl==EnumFacing.WEST?.5f: fw==EnumFacing.EAST?1: .5f;
			
			list.add(new AxisAlignedBB(minX, 0, 0, maxX, 1,1).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minZ = fl==EnumFacing.NORTH?0: fl==EnumFacing.SOUTH?.5f: fw==EnumFacing.NORTH?0: .5f;
			maxZ = fl==EnumFacing.NORTH?.5f: fl==EnumFacing.SOUTH?1: fw==EnumFacing.NORTH?.5f: 1;
			
			list.add(new AxisAlignedBB(0, 0, minZ, 1, 1,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			return list;
			
		}
		if(pos==6+9*h) {
			
			fl = fl.getOpposite();
			
			float minX = fl==EnumFacing.WEST?.6875f: fl==EnumFacing.EAST?.0625f: fw==EnumFacing.EAST?.0625f: .6875f;
			float maxX = fl==EnumFacing.EAST?.3125f: fl==EnumFacing.WEST?.9375f: fw==EnumFacing.EAST?.3125f: .9375f;
			float minZ = fl==EnumFacing.NORTH?.6875f: fl==EnumFacing.SOUTH?.0625f: fw==EnumFacing.SOUTH?.0625f: .6875f;
			float maxZ = fl==EnumFacing.SOUTH?.3125f: fl==EnumFacing.NORTH?.9375f: fw==EnumFacing.SOUTH?.3125f: .9375f;
			
			List<AxisAlignedBB> list = Lists.newArrayList(new AxisAlignedBB(minX, 0, minZ, maxX, 1,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minX = fl==EnumFacing.EAST?.5f: fl==EnumFacing.WEST?0: fw==EnumFacing.EAST?.5f: 0;
			maxX = fl==EnumFacing.EAST?1: fl==EnumFacing.WEST?.5f: fw==EnumFacing.EAST?1: .5f;
			
			list.add(new AxisAlignedBB(minX, 0, 0, maxX, 1,1).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minZ = fl==EnumFacing.NORTH?0: fl==EnumFacing.SOUTH?.5f: fw==EnumFacing.NORTH?0: .5f;
			maxZ = fl==EnumFacing.NORTH?.5f: fl==EnumFacing.SOUTH?1: fw==EnumFacing.NORTH?.5f: 1;
			
			list.add(new AxisAlignedBB(0, 0, minZ, 1, 1,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			return list;
			
		}
		if(pos==8+9*h) {
			
			fl = fl.getOpposite();
			fw = fw.getOpposite();
			
			float minX = fl==EnumFacing.WEST?.6875f: fl==EnumFacing.EAST?.0625f: fw==EnumFacing.EAST?.0625f: .6875f;
			float maxX = fl==EnumFacing.EAST?.3125f: fl==EnumFacing.WEST?.9375f: fw==EnumFacing.EAST?.3125f: .9375f;
			float minZ = fl==EnumFacing.NORTH?.6875f: fl==EnumFacing.SOUTH?.0625f: fw==EnumFacing.SOUTH?.0625f: .6875f;
			float maxZ = fl==EnumFacing.SOUTH?.3125f: fl==EnumFacing.NORTH?.9375f: fw==EnumFacing.SOUTH?.3125f: .9375f;
			
			List<AxisAlignedBB> list = Lists.newArrayList(new AxisAlignedBB(minX, 0, minZ, maxX, 1,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minX = fl==EnumFacing.EAST?.5f: fl==EnumFacing.WEST?0: fw==EnumFacing.EAST?.5f: 0;
			maxX = fl==EnumFacing.EAST?1: fl==EnumFacing.WEST?.5f: fw==EnumFacing.EAST?1: .5f;
			
			list.add(new AxisAlignedBB(minX, 0, 0, maxX, 1,1).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			minZ = fl==EnumFacing.NORTH?0: fl==EnumFacing.SOUTH?.5f: fw==EnumFacing.NORTH?0: .5f;
			maxZ = fl==EnumFacing.NORTH?.5f: fl==EnumFacing.SOUTH?1: fw==EnumFacing.NORTH?.5f: 1;
			
			list.add(new AxisAlignedBB(0, 0, minZ, 1, 1,maxZ).offset(getPos().getX(),getPos().getY(),getPos().getZ()));
			
			return list;
			
		}
		
		return null;
	}
	@Override
	public boolean isOverrideBox(AxisAlignedBB box, EntityPlayer player, RayTraceResult mop, ArrayList<AxisAlignedBB> list) {
		return false;
	}

}
