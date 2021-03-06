package com.bioxx.tfc2.rendering.model;

import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import com.bioxx.tfc2.Reference;

public class RenderFoxArctic extends RenderLiving
{
	ResourceLocation tex = new ResourceLocation(Reference.ModID+":"+"textures/mob/fox_arctic.png");

	public RenderFoxArctic(RenderManager manager) 
	{
		super(manager, new ModelFoxArctic(), 0.3f);
		this.shadowSize = 0.3f;
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) 
	{
		return tex;
	}
}
