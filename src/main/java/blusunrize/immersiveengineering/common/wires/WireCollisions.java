/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.common.wires;

import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.LocalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireCollisionData;
import blusunrize.immersiveengineering.api.wires.WireCollisionData.CollisionInfo;
import blusunrize.immersiveengineering.api.wires.localhandlers.ICollisionHandler;
import blusunrize.immersiveengineering.api.wires.localhandlers.LocalNetworkHandler;
import blusunrize.immersiveengineering.api.wires.utils.WireUtils;
import blusunrize.immersiveengineering.common.config.IEServerConfig;
import blusunrize.immersiveengineering.common.util.DirectionUtils;
import blusunrize.immersiveengineering.common.util.IEDamageSources;
import blusunrize.immersiveengineering.common.util.Utils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class WireCollisions
{
	public static void handleEntityCollision(BlockPos p, Entity e)
	{
		if(!e.level.isClientSide&&IEServerConfig.WIRES.enableWireDamage.get()&&e instanceof LivingEntity&&
				!e.isInvulnerableTo(IEDamageSources.wireShock)&&
				!(e instanceof Player&&((Player)e).abilities.invulnerable))
		{
			GlobalWireNetwork global = GlobalWireNetwork.getNetwork(e.level);
			WireCollisionData wireData = global.getCollisionData();
			Collection<WireCollisionData.CollisionInfo> atBlock = wireData.getCollisionInfo(p);
			for(CollisionInfo info : atBlock)
			{
				LocalWireNetwork local = info.getLocalNet();
				for(LocalNetworkHandler h : local.getAllHandlers())
					if(h instanceof ICollisionHandler)
						((ICollisionHandler)h).onCollided((LivingEntity)e, p, info);
			}
		}
	}

	public static void notifyBlockUpdate(@Nonnull Level worldIn, @Nonnull BlockPos pos, @Nonnull BlockState oldState, @Nonnull BlockState newState, int flags)
	{
		if(IEServerConfig.WIRES.blocksBreakWires.get()&&!worldIn.isClientSide&&(flags&1)!=0&&!newState.getCollisionShape(worldIn, pos).isEmpty())
		{
			GlobalWireNetwork globalNet = GlobalWireNetwork.getNetwork(worldIn);
			Collection<CollisionInfo> data = globalNet.getCollisionData().getCollisionInfo(pos);
			if(!data.isEmpty())
			{
				Map<Connection, BlockPos> toBreak = new HashMap<>();
				for(CollisionInfo info : data)
					if(info.isInBlock)
					{
						Vec3 vecA = info.conn.getPoint(0, info.conn.getEndA());
						if(Utils.isVecInBlock(vecA, pos, info.conn.getEndA().getPosition(), 1e-3))
							continue;
						Vec3 vecB = info.conn.getPoint(0, info.conn.getEndB());
						if(Utils.isVecInBlock(vecB, pos, info.conn.getEndB().getPosition(), 1e-3))
							continue;
						BlockPos dropPos = pos;
						if(WireUtils.preventsConnection(worldIn, pos, newState, info.intersectA, info.intersectB))
						{
							for(Direction f : DirectionUtils.VALUES)
								if(worldIn.isEmptyBlock(pos.relative(f)))
								{
									dropPos = dropPos.relative(f);
									break;
								}
							toBreak.put(info.conn, dropPos);
						}
					}
				for(Entry<Connection, BlockPos> b : toBreak.entrySet())
					globalNet.removeAndDropConnection(b.getKey(), b.getValue(), worldIn);
			}
		}
	}
}
