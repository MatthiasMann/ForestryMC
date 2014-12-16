/*******************************************************************************
 * Copyright (c) 2011-2014 SirSengir.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.txt
 * 
 * Various Contributors including, but not limited to:
 * SirSengir (original work), CovertJaguar, Player, Binnie, MysteriousAges
 ******************************************************************************/
package forestry.farming.gadgets;

import buildcraft.api.statements.ITriggerExternal;
import cpw.mods.fml.common.Optional;
import forestry.api.core.ITileStructure;
import forestry.core.inventory.AdjacentInventoryCache;
import forestry.core.inventory.ITileFilter;
import forestry.core.inventory.InvTools;
import forestry.core.inventory.TileInventoryAdapter;
import forestry.core.inventory.wrappers.InventoryMapper;
import forestry.core.utils.Utils;
import forestry.farming.triggers.FarmingTriggers;
import java.util.Collection;
import java.util.LinkedList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;

public class TileHatch extends TileFarm implements ISidedInventory {

	private final AdjacentInventoryCache inventoryCache = new AdjacentInventoryCache(this, getTileCache(), new ITileFilter() {

		@Override
		public boolean matches(TileEntity tile) {
			return !(tile instanceof TileFarm);
		}
	}, null);

	public TileHatch() {
		fixedType = TYPE_HATCH;
	}

	@Override
	public boolean hasFunction() {
		return true;
	}

	@Override
	protected void updateServerSide() {
		if (worldObj.getTotalWorldTime() % 40 == 0)
			dumpStash();
	}

	/* AUTO-EJECTING */
	private IInventory getProductInventory() {
		return new InventoryMapper(getStructureInventory(), TileFarmPlain.SLOT_PRODUCTION_1, TileFarmPlain.SLOT_COUNT_PRODUCTION);
	}

	protected void dumpStash() {

		if (!hasMaster())
			return;

		if (!InvTools.moveOneItemToPipe(getProductInventory(), tileCache)) {
			IInventory struct = getStructureInventory();
			if (struct != null)
				InvTools.moveItemStack(getProductInventory(), inventoryCache.getAdjecentInventories());
		}
	}

	/* IINVENTORY */
	@Override
	public TileInventoryAdapter getStructureInventory() {
		if (hasMaster()) {
			ITileStructure central = getCentralTE();
			if (central != null){
				return (TileInventoryAdapter) central.getInventory();
			}
		}

		return null;
	}

	@Override
	public int getSizeInventory() {
		IInventory inv = getStructureInventory();
		if (inv != null)
			return inv.getSizeInventory();
		else
			return 0;
	}

	@Override
	public ItemStack getStackInSlot(int slotIndex) {
		IInventory inv = getStructureInventory();
		if (inv != null)
			return inv.getStackInSlot(slotIndex);
		else
			return null;
	}

	@Override
	public ItemStack decrStackSize(int slotIndex, int amount) {
		IInventory inv = getStructureInventory();
		if (inv != null)
			return inv.decrStackSize(slotIndex, amount);
		else
			return null;
	}

	@Override
	public ItemStack getStackInSlotOnClosing(int slotIndex) {
		IInventory inv = getStructureInventory();
		if (inv != null)
			return inv.getStackInSlotOnClosing(slotIndex);
		else
			return null;
	}

	@Override
	public void setInventorySlotContents(int slotIndex, ItemStack itemstack) {
		IInventory inv = getStructureInventory();
		if (inv != null)
			inv.setInventorySlotContents(slotIndex, itemstack);
	}

	@Override
	public int getInventoryStackLimit() {
		IInventory inv = getStructureInventory();
		if (inv != null)
			return inv.getInventoryStackLimit();
		else
			return 0;
	}

	@Override
	public void openInventory() {
	}

	@Override
	public void closeInventory() {
	}

	@Override
	public String getInventoryName() {
		return getUnlocalizedName();
	}

	@Override
	public boolean isUseableByPlayer(EntityPlayer player) {
		return Utils.isUseableByPlayer(player, this);
	}

	@Override
	public boolean hasCustomInventoryName() {
		return false;
	}

	@Override
	public boolean isItemValidForSlot(int slotIndex, ItemStack itemstack) {
		if (!hasMaster())
			return false;

		ITileStructure struct = getCentralTE();
		if (!(struct instanceof TileFarmPlain))
			return false;

		if (!getStructureInventory().isItemValidForSlot(slotIndex, itemstack))
			return false;

		TileFarmPlain housing = (TileFarmPlain) struct;
		if (slotIndex == TileFarmPlain.SLOT_FERTILIZER && housing.acceptsAsFertilizer(itemstack))
			return true;
		if (slotIndex >= TileFarmPlain.SLOT_RESOURCES_1 && slotIndex < TileFarmPlain.SLOT_RESOURCES_1 + TileFarmPlain.SLOT_COUNT_RESERVOIRS
				&& housing.acceptsAsResource(itemstack))
			return true;
		if (slotIndex >= TileFarmPlain.SLOT_GERMLINGS_1 && slotIndex < TileFarmPlain.SLOT_GERMLINGS_1 + TileFarmPlain.SLOT_COUNT_RESERVOIRS
				&& housing.acceptsAsGermling(itemstack))
			return true;
		if (slotIndex == TileFarmPlain.SLOT_CAN)
			return FluidContainerRegistry.isFilledContainer(itemstack);

		return false;
	}

	@Override
	public boolean canInsertItem(int slotIndex, ItemStack itemstack, int side) {
		return isItemValidForSlot(xCoord, itemstack);
	}

	@Override
	public boolean canExtractItem(int slotIndex, ItemStack itemstack, int side) {
		if (!getStructureInventory().canExtractItem(slotIndex, itemstack, side))
			return false;

		return slotIndex >= TileFarmPlain.SLOT_PRODUCTION_1 && slotIndex < TileFarmPlain.SLOT_PRODUCTION_1 + TileFarmPlain.SLOT_COUNT_PRODUCTION;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(int side) {
		return getStructureInventory().getAccessibleSlotsFromSide(side);
	}

	/* ITRIGGERPROVIDER */
	@Optional.Method(modid = "BuildCraftAPI|statements")
	@Override
	public Collection<ITriggerExternal> getExternalTriggers(ForgeDirection side, TileEntity tile) {
		if (!hasMaster())
			return null;

		LinkedList<ITriggerExternal> list = new LinkedList<ITriggerExternal>();
		list.add(FarmingTriggers.lowResourceLiquid50);
		list.add(FarmingTriggers.lowResourceLiquid25);
		list.add(FarmingTriggers.lowSoil128);
		list.add(FarmingTriggers.lowSoil64);
		list.add(FarmingTriggers.lowSoil32);
		list.add(FarmingTriggers.lowFertilizer50);
		list.add(FarmingTriggers.lowFertilizer25);
		list.add(FarmingTriggers.lowGermlings25);
		list.add(FarmingTriggers.lowGermlings10);
		return list;
	}

}
