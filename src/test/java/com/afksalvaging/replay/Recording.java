/*
 * SPDX-License-Identifier: BSD-2-Clause
 * Copyright (c) 2026, Brettgod1355 <github.com/Brettgod1355>
 * See LICENSE for redistribution conditions and disclaimer.
 */
package com.afksalvaging.replay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a Black Box recording ({@code events.jsonl}) for a plugin's replay test.
 * <p>
 * Meant to be copied as it is into another plugin's test sources (change only the package), so a replay
 * test needs nothing from Black Box at build time; it uses only Gson, which RuneLite's client brings onto
 * the test classpath. Lines are kept in the order they were written. A replay walks {@link #byTick()} and
 * turns each line into what that plugin's logic expects.
 */
public final class Recording
{
	/** One recorded line: what happened ({@link #kind()}), on which tick, and its fields. */
	public static final class Line
	{
		private final JsonObject json;

		Line(JsonObject json)
		{
			this.json = json;
		}

		public String kind()
		{
			return text("e");
		}

		public int tick()
		{
			return number("tick", -1);
		}

		/** Milliseconds since the recording began. */
		public long ms()
		{
			JsonElement v = json.get("ms");
			return v == null || v.isJsonNull() ? -1 : v.getAsLong();
		}

		public boolean has(String field)
		{
			return json.has(field) && !json.get(field).isJsonNull();
		}

		public String text(String field)
		{
			return has(field) ? json.get(field).getAsString() : null;
		}

		public int number(String field, int otherwise)
		{
			return has(field) ? json.get(field).getAsInt() : otherwise;
		}

		/** The raw object, for fields such as the item list. */
		public JsonObject json()
		{
			return json;
		}

		@Override
		public String toString()
		{
			return json.toString();
		}
	}

	private final List<Line> lines;

	private Recording(List<Line> lines)
	{
		this.lines = Collections.unmodifiableList(lines);
	}

	public static Recording read(Path file) throws IOException
	{
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8))
		{
			return read(reader);
		}
	}

	/** Reads every non-blank line; a line that is not a JSON object fails with its line number. */
	@SuppressWarnings("deprecation") // JsonParser's instance methods, so older Gson versions work too.
	public static Recording read(Reader reader) throws IOException
	{
		List<Line> lines = new ArrayList<>();
		BufferedReader in = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
		JsonParser parser = new JsonParser();
		String text;
		int number = 0;
		while ((text = in.readLine()) != null)
		{
			number++;
			if (text.trim().isEmpty())
			{
				continue;
			}
			JsonElement element;
			try
			{
				element = parser.parse(text);
			}
			catch (RuntimeException e)
			{
				throw new IOException("line " + number + " is not JSON: " + e.getMessage(), e);
			}
			if (!element.isJsonObject())
			{
				throw new IOException("line " + number + " is not a JSON object");
			}
			lines.add(new Line(element.getAsJsonObject()));
		}
		return new Recording(lines);
	}

	public List<Line> lines()
	{
		return lines;
	}

	/** The recording format from the first line, or -1 when it has no session line. */
	public int format()
	{
		return lines.isEmpty() || !"session".equals(lines.get(0).kind()) ? -1 : lines.get(0).number("format", -1);
	}

	/** Every line of one kind, in order. */
	public List<Line> of(String kind)
	{
		List<Line> found = new ArrayList<>();
		for (Line line : lines)
		{
			if (kind.equals(line.kind()))
			{
				found.add(line);
			}
		}
		return found;
	}

	/** The lines grouped by game tick, ticks in the order they first appear. */
	public Map<Integer, List<Line>> byTick()
	{
		Map<Integer, List<Line>> ticks = new LinkedHashMap<>();
		for (Line line : lines)
		{
			ticks.computeIfAbsent(line.tick(), t -> new ArrayList<>()).add(line);
		}
		return ticks;
	}
}
