package com.bioxx.tfc2.world;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

import com.bioxx.jmapgen.IslandMap;
import com.bioxx.jmapgen.Point;
import com.bioxx.jmapgen.Spline3D;
import com.bioxx.jmapgen.attributes.Attribute;
import com.bioxx.jmapgen.attributes.PortalAttribute;
import com.bioxx.jmapgen.graph.Center;
import com.bioxx.tfc2.Core;
import com.bioxx.tfc2.TFCBlocks;
import com.bioxx.tfc2.api.types.PortalEnumType;
import com.bioxx.tfc2.api.util.Helper;
import com.bioxx.tfc2.blocks.BlockPortal;
import com.bioxx.tfc2.world.generators.WorldGenPortals;

public class TeleporterPaths extends Teleporter
{

	public TeleporterPaths(WorldServer worldIn) 
	{
		super(worldIn);
	}

	@Override
	/**
	 * Finds or creates a portal and places the entity in the correct location
	 * Note: The entity has already had its pos scaled for the scaling factor of this dimension. The coords
	 * will need to unscaled when finding the island map <-Remove me later
	 */
	public void placeInPortal(Entity entity, float yaw)
	{
		//First check if we're teleporting back into the overworld
		if (this.worldServerInstance.provider.getDimension() != 0)
		{
			if (!this.placeInExistingPortal(entity, yaw))
			{
				this.makePortal(entity);
				makePath(entity);
				this.placeInExistingPortal(entity, yaw);
			}
		}
		else
		{
			int playerX = MathHelper.floor_double(entity.posX);
			int playerZ = MathHelper.floor_double(entity.posZ);
			IslandMap islandMap = WorldGen.instance.getIslandMap(playerX >> 12, playerZ >> 12);
			Center closest = islandMap.getClosestCenter(new Point(playerX % 4096,playerZ % 4096));
			//Sometimes due to the world scaling, we might find the closest center is actually a neighbor of the portal hex
			closest = this.getPortalNeighbor(closest);

			BlockPos pos = new BlockPos((playerX >> 12)*4096+closest.point.x, 64+islandMap.convertHeightToMC(closest.getElevation()), (playerZ >> 12)*4096+closest.point.y);
			//Find portal
			pos = this.findPortal(pos);

			PortalAttribute attr = (PortalAttribute) closest.getAttribute(Attribute.Portal);
			if(this.checkRoomForPlayer(pos.north()))
				pos = pos.north();
			else if(this.checkRoomForPlayer(pos.south()))
				pos = pos.south();
			else if(this.checkRoomForPlayer(pos.east()))
				pos = pos.east();
			else if(this.checkRoomForPlayer(pos.west()))
				pos = pos.west();

			entity.setLocationAndAngles(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, entity.rotationYaw+0.5f, entity.rotationPitch);
		}
	}

	private BlockPos findPortal(BlockPos pos)
	{
		IBlockState state;
		for(int x = -30; x < 31; x++)
		{
			for(int z = -30; z < 31; z++)
			{
				for(int y = -20; y < 20; y++)
				{
					state = this.worldServerInstance.getBlockState(pos.add(x, y, z));
					if(state.getBlock() == TFCBlocks.Portal && (Boolean)state.getValue(BlockPortal.CENTER) == true)
					{
						if(this.worldServerInstance.getBlockState(pos.add(x, y, z).down()).getBlock() == TFCBlocks.Portal)
							return pos.add(x, y, z).down();
						else
							return pos.add(x, y, z);
					}
				}
			}
		}
		return BlockPos.ORIGIN;
	}

	@Override
	public boolean placeInExistingPortal(Entity entityIn, float rotationYaw)
	{
		boolean flag = true;
		int playerX = MathHelper.floor_double(entityIn.posX);
		int playerZ = MathHelper.floor_double(entityIn.posZ);
		boolean shouldAddPortalPosition = true;
		boolean foundPortal = false;
		BlockPos object = BlockPos.ORIGIN;
		long k = ChunkCoordIntPair.chunkXZ2Int(playerX, playerZ);

		IslandMap islandMap = Core.getMapForWorld(worldServerInstance, entityIn.getPosition());
		Center closest = islandMap.getClosestCenter(new Point((playerX*8) % 4096,(playerZ*8) % 4096));
		//Check if we already have a portal position cached here
		if (this.destinationCoordinateCache.containsItem(k))
		{
			Teleporter.PortalPosition portalposition = (Teleporter.PortalPosition)this.destinationCoordinateCache.getValueByKey(k);
			object = portalposition;
			portalposition.lastUpdateTime = this.worldServerInstance.getTotalWorldTime();
			shouldAddPortalPosition = false;
		}
		else //If not then we do a simple search for the closest portal block
		{
			BlockPos blockpos4 = new BlockPos(entityIn);

			object = this.findPortal(blockpos4);
		}

		//If we found a portal location then we need to move the player to it
		if (object != BlockPos.ORIGIN)
		{
			if (shouldAddPortalPosition)
			{
				this.destinationCoordinateCache.add(k, new Teleporter.PortalPosition((BlockPos)object, this.worldServerInstance.getTotalWorldTime()));
				this.destinationCoordinateKeys.add(Long.valueOf(k));
			}

			EnumFacing enumfacing = null;
			BlockPos pos = object;
			PortalAttribute attr = (PortalAttribute) closest.getAttribute(Attribute.Portal);

			if(this.checkRoomForPlayer(pos.north()))
				pos = pos.north();
			else if(this.checkRoomForPlayer(pos.south()))
				pos = pos.south();
			else if(this.checkRoomForPlayer(pos.east()))
				pos = pos.east();
			else if(this.checkRoomForPlayer(pos.west()))
				pos = pos.west();

			entityIn.setLocationAndAngles(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, entityIn.rotationYaw+0.5f, entityIn.rotationPitch);
			return true;
		}
		else
		{
			return false;
		}
	}

	private boolean checkRoomForPlayer(BlockPos pos)
	{
		return this.worldServerInstance.isAirBlock(pos) || this.worldServerInstance.isAirBlock(pos.up());
	}

	@Override
	public boolean makePortal(Entity entityIn)
	{
		int playerX = MathHelper.floor_double(entityIn.posX);
		int playerZ = MathHelper.floor_double(entityIn.posZ);
		IslandMap islandMap = WorldGen.instance.getIslandMap(((playerX*8) >> 12), ((playerZ*8) >> 12));
		Center closest = islandMap.getClosestCenter(new Point((playerX*8) % 4096,(playerZ*8) % 4096));
		//Sometimes due to the world scaling, we might find the closest center is actually a neighbor of the portal hex
		closest = this.getPortalNeighbor(closest);
		BlockPos portalPos = new BlockPos(entityIn);

		WorldGenPortals.BuildPortalSchem(worldServerInstance, closest, portalPos, islandMap, true);

		return true;
	}

	public boolean makePath(Entity entityIn)
	{
		int playerX = MathHelper.floor_double(entityIn.posX);
		int playerZ = MathHelper.floor_double(entityIn.posZ);
		int xM = ((playerX*8) >> 12);
		int zM = ((playerZ*8) >> 12);
		int xI = xM * 4096;
		int zI = zM * 4096;
		int xP = (playerX*8) % 4096;
		int zP = (playerZ*8) % 4096;

		IslandMap islandMap = WorldGen.instance.getIslandMap(xM, zM);
		Center closest = islandMap.getClosestCenter(new Point(xP,zP));

		//Sometimes due to the world scaling, we might find the closest center is actually a neighbor of the portal hex
		closest = this.getPortalNeighbor(closest);

		BlockPos portalPos = new BlockPos(entityIn);
		PortalAttribute startAttr = (PortalAttribute) closest.getAttribute(Attribute.Portal);
		int destX = Helper.getXCoord(startAttr.destMapID);
		int destZ = Helper.getYCoord(startAttr.destMapID);
		IslandMap destMap = WorldGen.instance.getIslandMap(destX, destZ);
		Center dest = destMap.getPortalForFacing(startAttr.direction.getOpposite());
		PortalAttribute endAttr = (PortalAttribute) dest.getAttribute(Attribute.Portal);

		double factor = 1/this.worldServerInstance.provider.getMovementFactor();


		BlockPos start = closest.point.toBlockPos().add(xI, 64+islandMap.convertHeightToMC(closest.getElevation()), zI);
		start = new BlockPos(start.getX() * factor, start.getY()-1, start.getZ() * factor);
		BlockPos end = dest.point.toBlockPos().add(destX * 4096, 64+destMap.convertHeightToMC(dest.getElevation()), destZ * 4096);
		end = new BlockPos(end.getX() * factor, end.getY()-1, end.getZ() * factor);

		//Create the spline if it does not exist
		if(startAttr.getSpline() == null)
		{
			//Copy the spline from the other side if it exists for some reason
			if(endAttr.getSpline() != null)
			{
				startAttr.setPath(endAttr.getPath());
			}
			else//Otherwise create new
			{
				Random r = new Random(closest.index + dest.index);
				ArrayList<BlockPos> list = new ArrayList<BlockPos>();
				list.add(start);
				list.add(end);
				Spline3D spline = new Spline3D(list);
				list = new ArrayList<BlockPos>();
				list.add(start);
				double loc = 1D / 7D;
				for(int i = 1; i < 6; i++)
				{
					BlockPos pos = spline.getPoint((double)i*loc);
					pos = pos.add(-30+r.nextInt(61), -5+r.nextInt(11), -30+r.nextInt(61));
					list.add(pos);
				}
				list.add(end);
				startAttr.setPath(list);
				endAttr.setPath(list);
			}
		}

		if(destMap.getIslandData().getPortalState(endAttr.direction) == PortalEnumType.Disabled)
			destMap.getIslandData().enablePortal(endAttr.direction);

		WorldGenPortals.BuildPath(worldServerInstance, start, end, startAttr.getSpline());
		WorldGenPortals.BuildPortalSchem(worldServerInstance, dest, end, destMap, true);

		return true;
	}

	public Center getPortalNeighbor(Center closest)
	{
		if(!closest.hasAttribute(Attribute.Portal))
		{
			for(Center c : closest.neighbors)
			{
				if(c.hasAttribute(Attribute.Portal))
				{
					return c;
				}
			}
		}
		return closest;
	}

	/**
	 * called periodically to remove out-of-date portal locations from the cache list. Argument par1 is a
	 * WorldServer.getTotalWorldTime() value.
	 */
	@Override
	public void removeStalePortalLocations(long worldTime)
	{
		if (worldTime % 100L == 0L)
		{
			Iterator iterator = this.destinationCoordinateKeys.iterator();
			long j = worldTime - 600L;

			while (iterator.hasNext())
			{
				Long olong = (Long)iterator.next();
				Teleporter.PortalPosition portalposition = (Teleporter.PortalPosition)this.destinationCoordinateCache.getValueByKey(olong.longValue());

				if (portalposition == null || portalposition.lastUpdateTime < j)
				{
					iterator.remove();
					this.destinationCoordinateCache.remove(olong.longValue());
				}
			}
		}
	}

}
