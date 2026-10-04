/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import javax.imageio.ImageIO;
import net.runelite.cache.ObjectManager;
import net.runelite.cache.OverlayManager;
import net.runelite.cache.definitions.ObjectDefinition;
import net.runelite.cache.definitions.OverlayDefinition;
import net.runelite.cache.fs.Store;
import net.runelite.cache.region.Location;
import net.runelite.cache.region.Position;
import net.runelite.cache.region.Region;
import net.runelite.cache.region.RegionLoader;
import net.runelite.cache.util.KeyProvider;
import net.runelite.cache.util.XteaKeyManager;

/**
 * Reads the game cache and works out which tiles of the surface are open sea a boat can sail,
 * then writes that as a coarse grid for the plugin's sailing distances, plus a picture to check it
 * by eye.
 * <p>
 * How: every salvaging spot is in open water, so the sea is whatever can be reached from the spots
 * over tiles that nothing blocks (tile setting bit 1 clear, no blocking object) and that carry one
 * of the ground overlays found under the spots themselves. Shorelines keep the blocked bit, which
 * is what stops the flood climbing onto land.
 * <p>
 * Usage, from the repository root:
 * {@code ./gradlew -p tools/seamap run --args="<cache dir> <output dir> [keys.json]"}; or
 * {@code --args="<cache dir> --find <object id> [keys.json]"} to print where an object stands.
 * The keys file (OpenRS2 format, {@code mapsquare}/{@code key} renamed to {@code region}/{@code keys})
 * is optional: without it the objects at sea (rocks) are unknown, which only costs a little accuracy.
 */
public final class SeaMapDumper
{
	/** Tiles per side of one cell in the written grid. */
	static final int CELL = 4;
	/** A cell counts as sea when at least this many of its tiles are. */
	static final int CELL_SEA_TILES = 8;

	private static final byte NONE = 0, OPEN = 1, BLOCKED = 2, OBJECT = 3;

	private static final Pattern SPOT = Pattern.compile("^\\s*([A-Z_]+)\\(ShipwreckType\\.\\w+, (\\d+), (\\d+),");
	private static final Pattern DOCK = Pattern.compile("^\\s*([A-Z_]+)\\(\"[^\"]*\", (\\d+), (\\d+),");

	private final int minX, minY, width, height;
	private final byte[] kind;
	private final byte[] setting;
	private final short[] overlay;
	private final short[] underlay;
	private final OverlayManager overlays;

	private SeaMapDumper(RegionLoader regions, ObjectManager objects, OverlayManager overlays)
	{
		this.overlays = overlays;
		minX = regions.getLowestX().getBaseX();
		minY = regions.getLowestY().getBaseY();
		width = regions.getHighestX().getBaseX() + Region.X - minX;
		height = regions.getHighestY().getBaseY() + Region.Y - minY;
		kind = new byte[width * height];
		setting = new byte[width * height];
		overlay = new short[width * height];
		underlay = new short[width * height];
		int withLocations = 0;
		for (Region region : regions.getRegions())
		{
			for (int x = 0; x < Region.X; x++)
			{
				for (int y = 0; y < Region.Y; y++)
				{
					int i = index(region.getBaseX() + x, region.getBaseY() + y);
					setting[i] = region.getTileSetting(0, x, y);
					kind[i] = (setting[i] & 1) != 0 ? BLOCKED : OPEN;
					overlay[i] = (short) region.getOverlayId(0, x, y);
					underlay[i] = (short) region.getUnderlayId(0, x, y);
				}
			}
			if (!region.getLocations().isEmpty())
			{
				withLocations++;
			}
			for (Location loc : region.getLocations())
			{
				Position pos = loc.getPosition();
				if (pos.getZ() != 0 || loc.getType() < 9 || loc.getType() > 11)
				{
					continue;
				}
				ObjectDefinition def = objects.getObject(loc.getId());
				if (def == null || def.getInteractType() == 0)
				{
					continue;
				}
				boolean turned = loc.getOrientation() == 1 || loc.getOrientation() == 3;
				int sizeX = turned ? def.getSizeY() : def.getSizeX();
				int sizeY = turned ? def.getSizeX() : def.getSizeY();
				for (int dx = 0; dx < sizeX; dx++)
				{
					for (int dy = 0; dy < sizeY; dy++)
					{
						int wx = pos.getX() + dx, wy = pos.getY() + dy;
						if (inside(wx, wy) && kind[index(wx, wy)] == OPEN)
						{
							kind[index(wx, wy)] = OBJECT;
						}
					}
				}
			}
		}
		System.out.printf("World %d..%d x %d..%d, %d regions, %d with objects%n",
			minX, minX + width, minY, minY + height, regions.getRegions().size(), withLocations);
	}

	public static void main(String[] args) throws IOException
	{
		if (args.length < 2)
		{
			System.err.println("usage: SeaMapDumper <cache dir> <output dir> [keys.json]");
			System.exit(2);
		}
		boolean finding = "--find".equals(args[1]);
		File out = new File(finding ? "." : args[1]);
		out.mkdirs();
		KeyProvider keys = region -> null;
		String keysPath = finding ? (args.length > 3 ? args[3] : null) : (args.length > 2 ? args[2] : null);
		if (keysPath != null)
		{
			XteaKeyManager manager = new XteaKeyManager();
			try (FileInputStream in = new FileInputStream(keysPath))
			{
				manager.loadKeys(in);
			}
			keys = manager;
		}
		List<Seed> spots = seeds(Paths.get("src/main/java/com/afksalvaging/SalvagingSpot.java").toFile(), SPOT);
		List<Seed> docks = seeds(Paths.get("src/main/java/com/afksalvaging/Mooring.java").toFile(), DOCK);
		System.out.printf("%d spots, %d docks%n", spots.size(), docks.size());

		try (Store store = new Store(new File(args[0])))
		{
			store.load();
			RegionLoader regions = new RegionLoader(store, keys);
			regions.loadRegions();
			regions.calculateBounds();
			ObjectManager objects = new ObjectManager(store);
			objects.load();
			if ("--find".equals(args[1]) && args.length > 2)
			{
				// Where an object stands in the world, for placing things like the bank boat exactly.
				int wanted = Integer.parseInt(args[2]);
				for (Region region : regions.getRegions())
				{
					for (Location loc : region.getLocations())
					{
						if (loc.getId() == wanted)
						{
							Position pos = loc.getPosition();
							System.out.printf("object %d at %d, %d, plane %d (type %d, rotation %d)%n",
								wanted, pos.getX(), pos.getY(), pos.getZ(), loc.getType(), loc.getOrientation());
						}
					}
				}
				return;
			}
			OverlayManager overlays = new OverlayManager(store);
			overlays.load();
			SeaMapDumper dumper = new SeaMapDumper(regions, objects, overlays);
			dumper.run(spots, docks, out);
		}
	}

	private void run(List<Seed> spots, List<Seed> docks, File out) throws IOException
	{
		Map<Integer, Integer> overlays = new TreeMap<>();
		for (Seed spot : spots)
		{
			int i = index(spot.x, spot.y);
			System.out.printf("%-28s %5d %5d kind=%d overlay=%d%n", spot.name, spot.x, spot.y, kind[i], overlay[i]);
			overlays.merge((int) overlay[i], 1, Integer::sum);
		}
		System.out.println("Overlays under the spots: " + overlays);

		BitSet strict = flood(spots, overlays.keySet());
		System.out.printf("Sea tiles reachable from the spots over their own overlays: %,d%n", strict.cardinality());
		BitSet loose = flood(spots, null);
		System.out.printf("...over any open tile: %,d%n", loose.cardinality());
		describe(strict, loose, overlays.keySet());
		BitSet sea = textured(loose);
		System.out.printf("...over any open tile with a textured overlay (the sea written out): %,d%n", sea.cardinality());

		for (Seed dock : docks)
		{
			int near = nearestSeaDistance(sea, dock.x, dock.y, 12);
			System.out.printf("%-28s %5d %5d sea %s%n", dock.name, dock.x, dock.y, near < 0 ? "NOT within 12" : near + " away");
		}

		render(sea, loose, spots, docks, new File(out, "seamap-preview.png"));
		writeGrid(sea, new File(out, "seamap.bin"));
		distancesFrom(sea, docks, spots, "RELLEKKA");
	}

	/**
	 * What the tiles look like in the cache, so the water rule can be checked: the ground overlays
	 * under the spots and the most common ones among tiles the loose flood reaches but the strict
	 * one does not, each with its definition, and the tile setting bits of each class of tile.
	 */
	private void describe(BitSet sea, BitSet loose, java.util.Set<Integer> water)
	{
		Map<Integer, Integer> looseOnly = new TreeMap<>();
		Map<Integer, Integer> looseUnderlay = new TreeMap<>();
		Map<Integer, Integer> seaSettings = new TreeMap<>(), looseSettings = new TreeMap<>(), landSettings = new TreeMap<>(), seaUnderlay = new TreeMap<>();
		for (int i = 0; i < width * height; i++)
		{
			if (sea.get(i))
			{
				seaSettings.merge((int) setting[i], 1, Integer::sum);
				seaUnderlay.merge((int) underlay[i], 1, Integer::sum);
			}
			else if (loose.get(i))
			{
				looseOnly.merge((int) overlay[i], 1, Integer::sum);
				looseUnderlay.merge((int) underlay[i], 1, Integer::sum);
				looseSettings.merge((int) setting[i], 1, Integer::sum);
			}
			else if (kind[i] == OPEN)
			{
				landSettings.merge((int) setting[i], 1, Integer::sum);
			}
		}
		System.out.println("Tile settings: sea " + seaSettings + ", loose-only " + looseSettings + ", other open " + landSettings);
		System.out.println("Underlays: sea " + seaUnderlay + ", loose-only " + looseUnderlay);
		System.out.println("Water overlays under the spots:");
		for (int id : water)
		{
			System.out.println("  " + overlayText(id));
		}
		System.out.println("Overlays of tiles only the loose flood reaches, most common first:");
		looseOnly.entrySet().stream()
			.sorted((a, b) -> b.getValue() - a.getValue())
			.limit(25)
			.forEach(e -> System.out.printf("  %,9d tiles  %s%n", e.getValue(), overlayText(e.getKey())));
		System.out.println("Tiles the loose flood reaches whose overlay has no texture (land it leaked onto, or odd water), by region:");
		Map<Integer, Integer> leaks = new TreeMap<>();
		for (int i = 0; i < width * height; i++)
		{
			if (loose.get(i) && !isWater(i))
			{
				int wx = minX + i % width, wy = minY + i / width;
				leaks.merge(((wx >> 6) << 8) | (wy >> 6), 1, Integer::sum);
			}
		}
		leaks.entrySet().stream()
			.sorted((a, b) -> b.getValue() - a.getValue())
			.limit(20)
			.forEach(e -> System.out.printf("  %,6d tiles in region %d (%d, %d)%n", e.getValue(), e.getKey(),
				(e.getKey() >> 8) << 6, (e.getKey() & 0xFF) << 6));
	}

	/** Every water overlay has a texture; the ground people walk on has none. */
	private boolean isWater(int i)
	{
		int id = overlay[i];
		if (id == 0)
		{
			return false;
		}
		OverlayDefinition def = overlays.provide(id - 1);
		return def != null && def.getTexture() >= 0;
	}

	private BitSet textured(BitSet loose)
	{
		BitSet sea = new BitSet(width * height);
		for (int i = loose.nextSetBit(0); i >= 0; i = loose.nextSetBit(i + 1))
		{
			if (isWater(i))
			{
				sea.set(i);
			}
		}
		return sea;
	}

	/** The overlay's definition; stored ids are one more than the definition ids, as in MapImageDumper. */
	private String overlayText(int storedId)
	{
		if (storedId == 0)
		{
			return "overlay 0 (none)";
		}
		OverlayDefinition def = overlays.provide(storedId - 1);
		if (def == null)
		{
			return "overlay " + storedId + " (no definition)";
		}
		return String.format("overlay %d: texture %d, rgb %06x, secondary %s, hideUnderlay %s", storedId, def.getTexture(),
			def.getRgbColor(), def.getSecondaryRgbColor() == -1 ? "-" : String.format("%06x", def.getSecondaryRgbColor()), def.isHideUnderlay());
	}

	/** Open tiles reachable from the seeds, four-connected; with {@code water} set, only over those overlays. */
	private BitSet flood(List<Seed> seeds, java.util.Set<Integer> water)
	{
		BitSet seen = new BitSet(width * height);
		Deque<Integer> queue = new ArrayDeque<>();
		for (Seed seed : seeds)
		{
			int i = index(seed.x, seed.y);
			if (kind[i] == OPEN && !seen.get(i))
			{
				seen.set(i);
				queue.add(i);
			}
		}
		while (!queue.isEmpty())
		{
			int i = queue.poll();
			int x = i % width, y = i / width;
			step(seen, queue, water, x + 1, y);
			step(seen, queue, water, x - 1, y);
			step(seen, queue, water, x, y + 1);
			step(seen, queue, water, x, y - 1);
		}
		return seen;
	}

	private void step(BitSet seen, Deque<Integer> queue, java.util.Set<Integer> water, int x, int y)
	{
		if (x < 0 || y < 0 || x >= width || y >= height)
		{
			return;
		}
		int i = y * width + x;
		if (seen.get(i) || kind[i] != OPEN || (water != null && !water.contains((int) overlay[i])))
		{
			return;
		}
		seen.set(i);
		queue.add(i);
	}

	private int nearestSeaDistance(BitSet sea, int wx, int wy, int limit)
	{
		for (int d = 0; d <= limit; d++)
		{
			for (int dx = -d; dx <= d; dx++)
			{
				for (int dy = -d; dy <= d; dy++)
				{
					if (Math.max(Math.abs(dx), Math.abs(dy)) == d && inside(wx + dx, wy + dy) && sea.get(index(wx + dx, wy + dy)))
					{
						return d;
					}
				}
			}
		}
		return -1;
	}

	private void render(BitSet sea, BitSet loose, List<Seed> spots, List<Seed> docks, File file) throws IOException
	{
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int i = y * width + x;
				int rgb;
				if (sea.get(i))
				{
					rgb = 0x3aa0ff;
				}
				else if (loose.get(i))
				{
					rgb = 0xff8800; // reachable only when the overlay rule is dropped: a leak onto land, or sea of another overlay
				}
				else if (kind[i] == NONE)
				{
					rgb = 0x000000;
				}
				else if (kind[i] == BLOCKED)
				{
					rgb = 0x303030;
				}
				else if (kind[i] == OBJECT)
				{
					rgb = 0x604040;
				}
				else
				{
					rgb = 0x707070;
				}
				image.setRGB(x, height - 1 - y, rgb);
			}
		}
		mark(image, spots, 0xff2020);
		mark(image, docks, 0xffe000);
		ImageIO.write(image, "png", file);
		System.out.println("Wrote " + file);
	}

	private void mark(BufferedImage image, List<Seed> seeds, int rgb)
	{
		for (Seed seed : seeds)
		{
			for (int dx = -2; dx <= 2; dx++)
			{
				for (int dy = -2; dy <= 2; dy++)
				{
					int x = seed.x - minX + dx, y = seed.y - minY + dy;
					if (x >= 0 && y >= 0 && x < width && y < height)
					{
						image.setRGB(x, height - 1 - y, rgb);
					}
				}
			}
		}
	}

	/**
	 * The grid the plugin reads: gzip over big-endian ints minX, minY, cell size, cells across,
	 * cells up, then one bit per cell, row by row from the south-west, packed eight to a byte.
	 */
	private void writeGrid(BitSet sea, File file) throws IOException
	{
		// Only as far north as the sea goes: the cache carries instanced areas far above the surface.
		int topTile = 0;
		for (int i = sea.length() - 1; i >= 0; i = sea.previousSetBit(i))
		{
			topTile = i / width;
			break;
		}
		int cellsX = (width + CELL - 1) / CELL;
		int cellsY = topTile / CELL + 1;
		byte[] bits = new byte[(cellsX * cellsY + 7) / 8];
		int seaCells = 0;
		for (int cy = 0; cy < cellsY; cy++)
		{
			for (int cx = 0; cx < cellsX; cx++)
			{
				int count = 0;
				for (int dx = 0; dx < CELL; dx++)
				{
					for (int dy = 0; dy < CELL; dy++)
					{
						int x = cx * CELL + dx, y = cy * CELL + dy;
						if (x < width && y < height && sea.get(y * width + x))
						{
							count++;
						}
					}
				}
				if (count >= CELL_SEA_TILES)
				{
					int bit = cy * cellsX + cx;
					bits[bit >> 3] |= (byte) (1 << (bit & 7));
					seaCells++;
				}
			}
		}
		try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file))))
		{
			out.writeInt(minX);
			out.writeInt(minY);
			out.writeInt(CELL);
			out.writeInt(cellsX);
			out.writeInt(cellsY);
			out.write(bits);
		}
		System.out.printf("Wrote %s: %d x %d cells of %d tiles (world y up to %d), %,d sea cells, %,d bytes%n",
			file, cellsX, cellsY, CELL, minY + cellsY * CELL, seaCells, file.length());
	}

	/** A sanity check: sailing distances from one dock to every spot over the written grid. */
	private void distancesFrom(BitSet sea, List<Seed> docks, List<Seed> spots, String dockName)
	{
		Seed from = docks.stream().filter(d -> d.name.equals(dockName)).findFirst().orElse(null);
		if (from == null)
		{
			return;
		}
		int cellsX = (width + CELL - 1) / CELL, cellsY = (height + CELL - 1) / CELL;
		BitSet cells = new BitSet(cellsX * cellsY);
		for (int cy = 0; cy < cellsY; cy++)
		{
			for (int cx = 0; cx < cellsX; cx++)
			{
				int count = 0;
				for (int dx = 0; dx < CELL; dx++)
				{
					for (int dy = 0; dy < CELL; dy++)
					{
						int x = cx * CELL + dx, y = cy * CELL + dy;
						if (x < width && y < height && sea.get(y * width + x))
						{
							count++;
						}
					}
				}
				if (count >= CELL_SEA_TILES)
				{
					cells.set(cy * cellsX + cx);
				}
			}
		}
		int start = nearestCell(cells, cellsX, cellsY, (from.x - minX) / CELL, (from.y - minY) / CELL, 6);
		if (start < 0)
		{
			System.out.println(dockName + " has no sea cell within 6 cells");
			return;
		}
		int[] dist = new int[cellsX * cellsY];
		java.util.Arrays.fill(dist, Integer.MAX_VALUE);
		dist[start] = 0;
		java.util.PriorityQueue<long[]> queue = new java.util.PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));
		queue.add(new long[]{0, start});
		while (!queue.isEmpty())
		{
			long[] head = queue.poll();
			int i = (int) head[1];
			if (head[0] > dist[i])
			{
				continue;
			}
			int cx = i % cellsX, cy = i / cellsX;
			for (int dx = -1; dx <= 1; dx++)
			{
				for (int dy = -1; dy <= 1; dy++)
				{
					int nx = cx + dx, ny = cy + dy;
					if ((dx == 0 && dy == 0) || nx < 0 || ny < 0 || nx >= cellsX || ny >= cellsY || !cells.get(ny * cellsX + nx))
					{
						continue;
					}
					int cost = dx != 0 && dy != 0 ? 14 : 10;
					int n = ny * cellsX + nx;
					if (dist[i] + cost < dist[n])
					{
						dist[n] = dist[i] + cost;
						queue.add(new long[]{dist[n], n});
					}
				}
			}
		}
		System.out.println("From " + dockName + ":");
		for (Seed spot : spots)
		{
			int target = nearestCell(cells, cellsX, cellsY, (spot.x - minX) / CELL, (spot.y - minY) / CELL, 6);
			int straight = Math.max(Math.abs(spot.x - from.x), Math.abs(spot.y - from.y));
			String sail = target < 0 || dist[target] == Integer.MAX_VALUE ? "unreachable" : String.valueOf(dist[target] * CELL / 10);
			System.out.printf("  %-28s straight %5d  sailing %s%n", spot.name, straight, sail);
		}
	}

	private static int nearestCell(BitSet cells, int cellsX, int cellsY, int cx, int cy, int limit)
	{
		for (int d = 0; d <= limit; d++)
		{
			for (int dx = -d; dx <= d; dx++)
			{
				for (int dy = -d; dy <= d; dy++)
				{
					int x = cx + dx, y = cy + dy;
					if (Math.max(Math.abs(dx), Math.abs(dy)) == d && x >= 0 && y >= 0 && x < cellsX && y < cellsY && cells.get(y * cellsX + x))
					{
						return y * cellsX + x;
					}
				}
			}
		}
		return -1;
	}

	private int index(int wx, int wy)
	{
		return (wy - minY) * width + (wx - minX);
	}

	private boolean inside(int wx, int wy)
	{
		return wx >= minX && wy >= minY && wx < minX + width && wy < minY + height;
	}

	private static List<Seed> seeds(File source, Pattern pattern) throws IOException
	{
		List<Seed> seeds = new ArrayList<>();
		for (String line : Files.readAllLines(source.toPath(), StandardCharsets.UTF_8))
		{
			Matcher m = pattern.matcher(line);
			if (m.find())
			{
				seeds.add(new Seed(m.group(1), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))));
			}
		}
		return seeds;
	}

	private static final class Seed
	{
		final String name;
		final int x, y;

		Seed(String name, int x, int y)
		{
			this.name = name;
			this.x = x;
			this.y = y;
		}
	}
}
