/*
 *  BluSunrize
 *  Copyright (c) 2021
 *
 *  This code is licensed under "Blu's License of Common Sense"
 *  Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.util.compat.computers.generic.owners;

import blusunrize.immersiveengineering.api.crafting.CrusherRecipe;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity.MultiblockProcess;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity.MultiblockProcessInWorld;
import blusunrize.immersiveengineering.common.blocks.metal.CrusherTileEntity;
import blusunrize.immersiveengineering.common.util.compat.computers.generic.CallbackEnvironment;
import blusunrize.immersiveengineering.common.util.compat.computers.generic.ComputerCallable;
import blusunrize.immersiveengineering.common.util.compat.computers.generic.IndexArgument;
import blusunrize.immersiveengineering.common.util.compat.computers.generic.impl.PoweredMBCallbacks;
import java.util.List;
import net.minecraft.world.item.ItemStack;

public class CrusherCallbacks extends MultiblockCallbackOwner<CrusherTileEntity>
{
	public CrusherCallbacks()
	{
		super(CrusherTileEntity.class, "crusher");
		addAdditional(PoweredMBCallbacks.INSTANCE);
	}

	@ComputerCallable
	public ItemStack getInputQueueElement(CallbackEnvironment<CrusherTileEntity> env, @IndexArgument int index)
	{
		List<MultiblockProcess<CrusherRecipe>> queue = env.getObject().processQueue;
		if(index < 0||index >= queue.size())
			throw new RuntimeException("Invalid index, queue contains "+queue.size()+" elements");
		MultiblockProcess<CrusherRecipe> process = queue.get(index);
		if(process instanceof PoweredMultiblockTileEntity.MultiblockProcessInWorld<?>)
			return ((MultiblockProcessInWorld<CrusherRecipe>)process).inputItems.get(0);
		else
			return null;
	}

	@ComputerCallable
	public int getQueueSize(CallbackEnvironment<CrusherTileEntity> env)
	{
		return env.getObject().processQueue.size();
	}
}
