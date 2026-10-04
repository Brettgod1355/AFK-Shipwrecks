/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.zip.GZIPInputStream;
import net.runelite.api.coords.WorldPoint;

/**
 * Which cells of the surface are open sea, and how far it is to sail between them.
 * <p>
 * The grid comes from {@code seamap.bin}, written by {@code tools/seamap} from the game cache:
 * the world cut into cells of {@link #cellSize} tiles a side, a cell being sea when at least half
 * its tiles are open water a boat can reach from a salvaging spot. Distances are the shortest way
 * over sea cells, moving to the eight neighbours, so they are the sailing distance give or take a
 * cell: the straight-line "tiles from you" was a long way out wherever land lies between.
 */
public final class SeaMap
{
	/** How far from a point on land (a dock) the sea may start for a distance to be given. */
	public static final int LAND_REACH_TILES = 16;
	/** Distance for a point no sea cell can be reached from. */
	public static final int UNREACHABLE = -1;

	private final int minX, minY, cellSize, cellsX, cellsY;
	private final BitSet sea;
	private int[] costs;

	SeaMap(int minX, int minY, int cellSize, int cellsX, int cellsY, BitSet sea)
	{
		this.minX = minX;
		this.minY = minY;
		this.cellSize = cellSize;
		this.cellsX = cellsX;
		this.cellsY = cellsY;
		this.sea = sea;
	}

	/** The map packaged with the plugin. */
	public static SeaMap load() throws IOException
	{
		try (InputStream raw = SeaMap.class.getResourceAsStream("seamap.bin"))
		{
			if (raw == null)
			{
				throw new IOException("seamap.bin is missing");
			}
			return read(raw);
		}
	}

	static SeaMap read(InputStream raw) throws IOException
	{
		try (DataInputStream in = new DataInputStream(new GZIPInputStream(raw)))
		{
			int minX = in.readInt();
			int minY = in.readInt();
			int cellSize = in.readInt();
			int cellsX = in.readInt();
			int cellsY = in.readInt();
			byte[] bits = new byte[(cellsX * cellsY + 7) / 8];
			in.readFully(bits);
			return new SeaMap(minX, minY, cellSize, cellsX, cellsY, BitSet.valueOf(bits));
		}
	}

	public int cellSize()
	{
		return cellSize;
	}

	public int seaCells()
	{
		return sea.cardinality();
	}

	/** Whether the tile lies in a sea cell. */
	public boolean isSea(WorldPoint point)
	{
		int cx = (point.getX() - minX) / cellSize, cy = (point.getY() - minY) / cellSize;
		return point.getX() >= minX && point.getY() >= minY && cx < cellsX && cy < cellsY && sea.get(cy * cellsX + cx);
	}

	/**
	 * Sailing distances in tiles from one point to each target, {@link #UNREACHABLE} where no sea
	 * joins them. A point on land uses the nearest sea cell within {@link #LAND_REACH_TILES}, so a
	 * dock measures from the water beside it; from further inland every distance is unreachable.
	 */
	public synchronized int[] distances(WorldPoint from, List<WorldPoint> targets)
	{
		int[] result = new int[targets.size()];
		Arrays.fill(result, UNREACHABLE);
		int start = nearestCell(from);
		if (start < 0)
		{
			return result;
		}
		int[] goals = new int[targets.size()];
		int open = 0;
		for (int t = 0; t < goals.length; t++)
		{
			goals[t] = nearestCell(targets.get(t));
			if (goals[t] >= 0)
			{
				open++;
			}
		}
		// Dijkstra in tenths of a cell: a straight step is 10, a diagonal 14.
		int[] cost = scratch();
		cost[start] = 0;
		PriorityQueue<long[]> queue = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
		queue.add(new long[]{0, start});
		while (!queue.isEmpty() && open > 0)
		{
			long[] head = queue.poll();
			int cell = (int) head[1];
			if (head[0] > cost[cell])
			{
				continue;
			}
			for (int t = 0; t < goals.length; t++)
			{
				if (goals[t] == cell)
				{
					result[t] = (int) (head[0] * cellSize / 10);
					goals[t] = -1;
					open--;
				}
			}
			int cx = cell % cellsX, cy = cell / cellsX;
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					int nx = cx + dx, ny = cy + dy;
					if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= cellsX || ny >= cellsY)
					{
						continue;
					}
					int next = ny * cellsX + nx;
					if (!sea.get(next))
					{
						continue;
					}
					long through = head[0] + (dx != 0 && dy != 0 ? 14 : 10);
					if (through < cost[next])
					{
						cost[next] = (int) through;
						queue.add(new long[]{through, next});
					}
				}
			}
		}
		return result;
	}

	/** One cost array for every search (the map is 2.4 MB of ints), reset rather than reallocated; hence {@code synchronized}. */
	private int[] scratch()
	{
		if (costs == null)
		{
			costs = new int[cellsX * cellsY];
		}
		Arrays.fill(costs, Integer.MAX_VALUE);
		return costs;
	}

	/** The sea cell holding the point, else the nearest within {@link #LAND_REACH_TILES}, else -1. */
	int nearestCell(WorldPoint point)
	{
		int cx = Math.floorDiv(point.getX() - minX, cellSize), cy = Math.floorDiv(point.getY() - minY, cellSize);
		int reach = (LAND_REACH_TILES + cellSize - 1) / cellSize;
		for (int d = 0; d <= reach; d++)
		{
			for (int dx = -d; dx <= d; dx++)
			{
				for (int dy = -d; dy <= d; dy++)
				{
					if (Math.max(Math.abs(dx), Math.abs(dy)) != d)
					{
						continue;
					}
					int x = cx + dx, y = cy + dy;
					if (x >= 0 && y >= 0 && x < cellsX && y < cellsY && sea.get(y * cellsX + x))
					{
						return y * cellsX + x;
					}
				}
			}
		}
		return -1;
	}
}
