/*
 * BluSunrize
 * Copyright (c) 2021
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */
package blusunrize.immersiveengineering.common.util.compat.crafttweaker.actions;

import com.blamejared.crafttweaker.api.item.IIngredient;
import com.blamejared.crafttweaker.api.managers.IRecipeManager;
import com.blamejared.crafttweaker.impl.item.MCItemStackMutable;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public abstract class AbstractActionRemoveMultipleOutputs<T extends Recipe<?>> extends AbstractActionGenericRemoveRecipe<T>
{

	private final IIngredient output;

	public AbstractActionRemoveMultipleOutputs(IRecipeManager manager, IIngredient output)
	{
		super(manager, output);
		this.output = output;
	}

	@Override
	public boolean shouldRemove(T recipe)
	{
		return getAllOutputs(recipe).stream().map(MCItemStackMutable::new).anyMatch(output::matches);
	}

	public abstract List<ItemStack> getAllOutputs(T recipe);
}
