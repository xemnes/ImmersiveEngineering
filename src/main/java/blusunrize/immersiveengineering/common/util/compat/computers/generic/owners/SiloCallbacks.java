/*
 *  BluSunrize
 *  Copyright (c) 2021
 *
 *  This code is licensed under "Blu's License of Common Sense"
 *  Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.util.compat.computers.generic.owners;

import blusunrize.immersiveengineering.common.blocks.metal.SiloTileEntity;
import blusunrize.immersiveengineering.common.util.compat.computers.generic.CallbackEnvironment;
import blusunrize.immersiveengineering.common.util.compat.computers.generic.ComputerCallable;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

public class SiloCallbacks extends MultiblockCallbackOwner<SiloTileEntity>
{
	public SiloCallbacks()
	{
		super(SiloTileEntity.class, "silo");
	}

	@ComputerCallable
	public ItemStack getContents(CallbackEnvironment<SiloTileEntity> env)
	{
		// This stack will be converted to a lua object immediately, so using a large stack size is ok
		return ItemHandlerHelper.copyStackWithSize(env.getObject().identStack, env.getObject().storageAmount);
	}
}
