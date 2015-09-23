package com.bioxx.tfc2.items;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemTerra extends Item 
{

	private boolean showInCreative = true;
	protected int maxSubTypeMeta = 0;
	protected String[] subTypeNames;

	public ItemTerra()
	{
		this.setCreativeTab(CreativeTabs.tabMisc);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item itemIn, CreativeTabs tab, List subItems)
	{
		if(showInCreative)
		{
			if(this.hasSubtypes)
			{
				for(int i = 0; i <= maxSubTypeMeta; i++)
					subItems.add(new ItemStack(itemIn, 1, i));
			}
			else
				subItems.add(new ItemStack(itemIn, 1, 0));
		}
	}

	public void setShowInCreative(boolean b)
	{
		showInCreative = b;
	}

	@Override
	public String getUnlocalizedName(ItemStack stack)
	{
		if(!this.hasSubtypes)
			return super.getUnlocalizedName(stack);
		else
		{
			return super.getUnlocalizedName(stack) + "." + subTypeNames[stack.getItemDamage()];
		}
	}
}
