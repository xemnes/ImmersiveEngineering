/*
 * BluSunrize
 * Copyright (c) 2020
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 *
 */

package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.common.IETileTypes;
import blusunrize.immersiveengineering.common.blocks.IETileProviderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.material.Material;

public class SampleDrillBlock extends IETileProviderBlock<SampleDrillTileEntity>
{
	public SampleDrillBlock()
	{
		super("sample_drill", IETileTypes.SAMPLE_DRILL,
				Properties.of(Material.METAL).sound(SoundType.METAL).strength(3, 15).noOcclusion());
	}

	@Override
	protected void createBlockStateDefinition(Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(IEProperties.FACING_HORIZONTAL, IEProperties.MULTIBLOCKSLAVE);
	}

	@Override
	public boolean canIEBlockBePlaced(BlockState newState, BlockPlaceContext context)
	{
		BlockPos start = context.getClickedPos();
		return areAllReplaceable(start, start.above(2), context);
	}
}
