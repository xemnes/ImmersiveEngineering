/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.api.wires;

import blusunrize.immersiveengineering.api.wires.utils.WireUtils;
import com.google.common.base.Preconditions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.StringJoiner;

public class Connection
{
	@Nonnull
	public final WireType type;
	@Nonnull
	private final ConnectionPoint endA;
	@Nonnull
	private final ConnectionPoint endB;
	private final boolean internal;
	private final double roughLength;
	private Vec3 endAOffset;
	private Vec3 endBOffset;

	@Nullable
	private CatenaryData catData;
	boolean blockDataGenerated = false;

	@Nullable
	private LocalWireNetwork cachedLocalNet;
	private int cachedNetVersion = -1;

	private Connection(
			@Nonnull WireType type,
			@Nonnull ConnectionPoint endA, @Nonnull ConnectionPoint endB,
			Vec3 endAOffset, Vec3 endBOffset,
			boolean internal
	)
	{
		this.type = type;
		if(endA.compareTo(endB) < 0)
		{
			this.endAOffset = endBOffset;
			this.endBOffset = endAOffset;
			this.endA = endB;
			this.endB = endA;
		}
		else
		{
			this.endAOffset = endAOffset;
			this.endBOffset = endBOffset;
			this.endA = endA;
			this.endB = endB;
		}
		this.internal = internal;
		this.roughLength = Math.sqrt(endA.getPosition().distSqr(endB.getPosition(), false));
	}

	public Connection(
			@Nonnull WireType type,
			@Nonnull ConnectionPoint endA, @Nonnull ConnectionPoint endB, Vec3 endAOffset, Vec3 endBOffset
	)
	{
		this(type, endA, endB, endAOffset, endBOffset, false);
	}

	public Connection(
			@Nonnull WireType type,
			@Nonnull ConnectionPoint endA, @Nonnull ConnectionPoint endB,
			GlobalWireNetwork netForOffsets
	)
	{
		this(
				type, endA, endB,
				WireUtils.getConnectionOffset(netForOffsets, endA, endB, type),
				WireUtils.getConnectionOffset(netForOffsets, endB, endA, type),
				false
		);
	}

	public Connection(BlockPos pos, int idA, int idB)
	{
		this(
				WireType.INTERNAL_CONNECTION,
				new ConnectionPoint(pos, idA), new ConnectionPoint(pos, idB),
				Vec3.ZERO, Vec3.ZERO,
				true
		);
	}

	public Connection(CompoundTag nbt)
	{
		this(
				WireType.getValue(nbt.getString("type")),
				new ConnectionPoint(nbt.getCompound("endA")),
				new ConnectionPoint(nbt.getCompound("endB")),
				WireUtils.loadVec3(nbt.get("endAOffset")),
				WireUtils.loadVec3(nbt.get("endBOffset")),
				nbt.getBoolean("internal")
		);
	}

	public ConnectionPoint getOtherEnd(ConnectionPoint known)
	{
		if(known.equals(endA))
			return endB;
		else
			return endA;
	}

	@Nonnull
	public ConnectionPoint getEndA()
	{
		return endA;
	}

	@Nonnull
	public ConnectionPoint getEndB()
	{
		return endB;
	}

	public LocalWireNetwork getContainingNet(GlobalWireNetwork global)
	{
		if(cachedLocalNet==null||(cachedLocalNet.getVersion()!=cachedNetVersion&&!cachedLocalNet.isValid(getEndA())))
			cachedLocalNet = global.getLocalNet(getEndA());
		if(cachedLocalNet!=null)
			cachedNetVersion = cachedLocalNet.getVersion();
		return cachedLocalNet;
	}

	public boolean isPositiveEnd(ConnectionPoint p)
	{
		return p.equals(endA);
	}

	public CompoundTag toNBT()
	{
		CompoundTag nbt = new CompoundTag();
		nbt.put("endA", endA.createTag());
		nbt.put("endB", endB.createTag());
		nbt.putString("type", type.getUniqueName());
		nbt.putBoolean("internal", internal);
		nbt.put("endAOffset", WireUtils.storeVec3(endAOffset));
		nbt.put("endBOffset", WireUtils.storeVec3(endBOffset));
		return nbt;
	}

	public boolean isInternal()
	{
		return internal;
	}

	public void generateCatenaryData()
	{
		Vec3 vecA = endAOffset;
		Vec3 vecB = Vec3.atLowerCornerOf(endB.getPosition().subtract(endA.getPosition())).add(endBOffset);
		Vec3 delta = vecB.subtract(vecA);
		double horLength = Math.sqrt(delta.x*delta.x+delta.z*delta.z);

		if(Math.abs(delta.x) < 0.05&&Math.abs(delta.z) < 0.05)
		{
			catData = new CatenaryData(true, 0, 0, 1, delta, 0, vecA);
			return;
		}
		double wireLength = delta.length()*type.getSlack();
		double l;
		{
			double goal = Math.sqrt(wireLength*wireLength-delta.y*delta.y)/horLength;
			double lower = 0;
			double upper = 1;
			while(Math.sinh(upper)/upper < goal)
			{
				lower = upper;
				upper *= 2;
			}
			final int iterations = 20;
			for(int i = 0; i < iterations; ++i)
			{
				double middleL = (lower+upper)/2;
				double middleVal = Math.sinh(middleL)/middleL;
				if(middleVal < goal)
					lower = middleL;
				else if(middleVal > goal)
					upper = middleL;
				else
				{
					upper = lower = middleL;
					break;
				}
			}
			l = (lower+upper)/2;
		}
		double scale = horLength/(2*l);
		double offsetX = (0+horLength-scale*Math.log((wireLength+delta.y)/(wireLength-delta.y)))*0.5;
		double offsetY = (delta.y+0-wireLength*Math.cosh(l)/Math.sinh(l))*0.5;
		catData = new CatenaryData(false, offsetX, offsetY, scale, delta, horLength, vecA);
	}

	public boolean hasCatenaryData()
	{
		return catData!=null;
	}

	public boolean isEnd(ConnectionPoint p)
	{
		return p.equals(endA)||p.equals(endB);
	}

	//pos is relative to 1. 0 is the end corresponding to from, 1 is the other end.
	public Vec3 getPoint(double pos, ConnectionPoint from)
	{
		pos = transformPosition(pos, from);
		Vec3 basic;
		if(hasCatenaryData())
			basic = getCatenaryData().getPoint(pos);
		else
			basic = Vec3.atLowerCornerOf(endB.getPosition().subtract(endA.getPosition())).scale(pos);
		Vec3 add = Vec3.ZERO;
		if(endB.equals(from))
			add = Vec3.atLowerCornerOf(endA.getPosition().subtract(endB.getPosition()));
		return basic.add(add);
	}

	public double getSlope(double pos, ConnectionPoint from)
	{
		if(hasCatenaryData())
		{
			pos = transformPosition(pos, from);
			double slope = getCatenaryData().getSlope(pos);
			if(endB.equals(from))
				slope *= -1;
			return slope;
		}
		else
			return 0;
	}

	public double transformPosition(double pos, ConnectionPoint from)
	{
		if(endB.equals(from))
			return 1-pos;
		else
			return pos;
	}

	public ConnectionPoint getEndFor(BlockPos pos)
	{
		return endA.getPosition().equals(pos)?endA: endB;
	}

	@Nonnull
	public CatenaryData getCatenaryData()
	{
		return Preconditions.checkNotNull(catData);
	}

	void resetCatenaryData(Vec3 newOffsetA, Vec3 newOffsetB)
	{
		catData = null;
		endAOffset = newOffsetA;
		endBOffset = newOffsetB;
	}

	@Override
	public boolean equals(Object o)
	{
		if(this==o) return true;
		if(o==null||getClass()!=o.getClass()) return false;

		Connection that = (Connection)o;

		if(internal!=that.internal) return false;
		if(!type.equals(that.type)) return false;
		if(!endA.equals(that.endA)) return false;
		return endB.equals(that.endB);
	}

	@Override
	public int hashCode()
	{
		int result = type.hashCode();
		result = 31*result+endA.hashCode();
		result = 31*result+endB.hashCode();
		result = 31*result+(internal?1: 0);
		return result;
	}

	@Override
	public String toString()
	{
		StringJoiner ret = new StringJoiner(", ", Connection.class.getSimpleName()+"[", "]")
				.add("type="+type)
				.add("endA="+endA)
				.add("endB="+endB);
		if(internal)
			ret.add("internal");
		return ret.toString();
	}

	public ConnectionPoint[] getEnds()
	{
		return new ConnectionPoint[]{endA, endB};
	}

	public double getBlockDistance()
	{
		return roughLength;
	}

	public Vec3 getEndAOffset()
	{
		return endAOffset;
	}

	public Vec3 getEndBOffset()
	{
		return endBOffset;
	}

	public static record RenderData(
			CatenaryData data,
			WireType type,
			int pointsToRenderSolid,
			int color
	)
	{
		public static final int POINTS_PER_WIRE = 16;

		public static RenderData make(Connection conn, boolean startAtB, int count)
		{
			Preconditions.checkArgument(conn.hasCatenaryData());
			CatenaryData basicCatData = conn.getCatenaryData();
			CatenaryData directionalData;
			if(startAtB)
				directionalData = basicCatData.reverse(conn.getPoint(0, conn.getEndB()));
			else
				directionalData = basicCatData;
			return new RenderData(directionalData, conn.type, count, conn.type.getColour(conn));
		}

		public Vec3 getPoint(int index)
		{
			return data.getPoint(index/(double)POINTS_PER_WIRE);
		}
	}

	public static record CatenaryData(
			boolean isVertical,
			//Relative to endA
			double offsetX,
			double offsetY,
			double scale,
			Vec3 delta,
			double horLength,
			Vec3 vecA
	)
	{
		public CatenaryData reverse(Vec3 otherEndAVec)
		{
			Vec3 delta = delta().scale(-1);
			double offsetX = horLength-offsetX();
			double offsetY = -scale*Math.cosh(-offsetX/scale);
			return new CatenaryData(isVertical, offsetX, offsetY, scale, delta, horLength, otherEndAVec);
		}

		public double getSlope(double pos)
		{
			pos = Mth.clamp(pos, 0, 1);
			if(isVertical)
				return Double.POSITIVE_INFINITY*Math.signum(getDeltaY());
			else
				return Math.sinh((pos*horLength-offsetX)/scale);
		}

		public Vec3 getPoint(double pos)
		{
			if(pos==1)
				return vecA.add(delta);
			double x = delta.x*pos;
			double y;
			if(isVertical)
				y = delta.y*pos;
			else
				y = scale*Math.cosh((horLength*pos-offsetX)/scale)+offsetY;
			double z = delta.z*pos;
			return vecA.add(x, y, z);
		}

		public double getDeltaX()
		{
			return delta.x;
		}

		public double getDeltaY()
		{
			return delta.y;
		}

		public double getDeltaZ()
		{
			return delta.z;
		}
	}
}
