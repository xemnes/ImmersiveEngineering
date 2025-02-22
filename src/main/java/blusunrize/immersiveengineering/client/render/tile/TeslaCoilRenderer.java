/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.render.tile;

import blusunrize.immersiveengineering.client.utils.IERenderTypes;
import blusunrize.immersiveengineering.client.utils.TransformingVertexBuilder;
import blusunrize.immersiveengineering.common.blocks.metal.TeslaCoilTileEntity;
import blusunrize.immersiveengineering.common.blocks.metal.TeslaCoilTileEntity.LightningAnimation;
import blusunrize.immersiveengineering.common.util.Utils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;

public class TeslaCoilRenderer extends BlockEntityRenderer<TeslaCoilTileEntity>
{
	public TeslaCoilRenderer(BlockEntityRenderDispatcher rendererDispatcherIn)
	{
		super(rendererDispatcherIn);
	}

	@Override
	public void render(TeslaCoilTileEntity tile, float partialTicks, PoseStack matrixStack, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn)
	{
		if(tile.isDummy()||!tile.getWorldNonnull().hasChunkAt(tile.getBlockPos()))
			return;

		for(LightningAnimation animation : TeslaCoilTileEntity.effectMap.get(tile.getBlockPos()))
		{
			if(animation.shoudlRecalculateLightning())
				animation.createLightning(Utils.RAND);

			double tx = tile.getBlockPos().getX();
			double ty = tile.getBlockPos().getY();
			double tz = tile.getBlockPos().getZ();
			drawAnimation(animation, tx, ty, tz, new float[]{77/255f, 74/255f, 152/255f, .75f}, 4f, bufferIn, matrixStack);
			drawAnimation(animation, tx, ty, tz, new float[]{1, 1, 1, 1}, 1f, bufferIn, matrixStack);
		}
	}

	public static void drawAnimation(LightningAnimation animation, double tileX, double tileY, double tileZ,
									 float[] rgba, float lineWidth, MultiBufferSource buffers,
									 PoseStack transform)
	{
		VertexConsumer base = buffers.getBuffer(IERenderTypes.getLines(lineWidth));
		TransformingVertexBuilder builder = new TransformingVertexBuilder(base, transform);
		builder.setColor(rgba[0], rgba[1], rgba[2], rgba[3]);
		List<Vec3> subs = animation.subPoints;
		builder.vertex(animation.startPos.x-tileX, animation.startPos.y-tileY, animation.startPos.z-tileZ).endVertex();

		for(Vec3 sub : subs)
		{
			builder.vertex(sub.x-tileX, sub.y-tileY, sub.z-tileZ).endVertex();
			builder.vertex(sub.x-tileX, sub.y-tileY, sub.z-tileZ).endVertex();
		}

		Vec3 end = (animation.targetEntity!=null?animation.targetEntity.position(): animation.targetPos).add(-tileX, -tileY, -tileZ);
		builder.vertex(end.x, end.y, end.z).endVertex();
	}
}