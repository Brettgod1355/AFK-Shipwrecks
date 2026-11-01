package com.afksalvaging;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.OverlayLayer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SalvagingDimmerOverlayTest
{
	private boolean active = true;
	private boolean interfaces = true;
	private boolean chat;
	private boolean chatHidden;
	private int opacity = 50;
	private Color color = Color.BLACK;
	private GameState gameState = GameState.LOGGED_IN;
	private boolean ownBoat = true;
	private boolean mainInterface;
	private Rectangle viewport = new Rectangle(10, 10, 60, 40);

	private final AfkSalvagingConfig config = new AfkSalvagingConfig()
	{
		@Override public boolean unhideInterfaces() { return interfaces; }
		@Override public boolean unhideChat() { return chat; }
		@Override public int dimmerOpacity() { return opacity; }
		@Override public Color dimmerColor() { return color; }
	};

	private final AfkSalvagingPlugin plugin = new AfkSalvagingPlugin()
	{
		@Override public boolean shouldShowDimmer() { return active; }
	};

	private final Widget chatWidget = (Widget) Proxy.newProxyInstance(Widget.class.getClassLoader(),
		new Class<?>[]{Widget.class}, (proxy, method, args) ->
		{
			switch (method.getName())
			{
				case "isHidden": return chatHidden;
				case "getBounds": return new Rectangle(0, 60, 60, 40);
				default: throw new AssertionError(method.getName());
			}
		});

	private final Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(),
		new Class<?>[]{Client.class}, (proxy, method, args) ->
		{
			switch (method.getName())
			{
				case "getGameState": return gameState;
				case "getCanvasWidth":
				case "getCanvasHeight": return 100;
				case "getViewportXOffset": return viewport.x;
				case "getViewportYOffset": return viewport.y;
				case "getViewportWidth": return viewport.width;
				case "getViewportHeight": return viewport.height;
				case "getWidget": return (int) args[0] == InterfaceID.Toplevel.CHAT_CONTAINER ? chatWidget : null;
				default: throw new AssertionError(method.getName());
			}
		});

	private BufferedImage render()
	{
		BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			new SalvagingDimmerOverlay(client, plugin, config).render(graphics);
		}
		finally
		{
			graphics.dispose();
		}
		return image;
	}

	@Test
	public void viewportDimmingUsesOpacityAndDrawsBeneathInterfaces()
	{
		SalvagingDimmerOverlay overlay = new SalvagingDimmerOverlay(client, plugin, config);
		assertEquals(OverlayLayer.UNDER_WIDGETS, overlay.getLayer());
		BufferedImage image = render();
		assertEquals(new Color(0, 0, 0, 128).getRGB(), image.getRGB(20, 20));
		assertEquals(0, image.getRGB(0, 0));
		assertEquals(0, image.getRGB(80, 80));
		interfaces = false;
		overlay.updateLayer();
		assertEquals(OverlayLayer.ABOVE_WIDGETS, overlay.getLayer());
	}

	@Test
	public void fullCanvasCanLeaveOnlyVisibleChatClear()
	{
		interfaces = false;
		chat = true;
		opacity = 100;
		color = Color.BLUE;
		BufferedImage image = render();
		assertEquals(Color.BLUE.getRGB(), image.getRGB(0, 0));
		assertEquals(Color.BLUE.getRGB(), image.getRGB(80, 80));
		assertEquals(0, image.getRGB(20, 80));
		chatHidden = true;
		assertEquals(Color.BLUE.getRGB(), render().getRGB(20, 80));
		chatHidden = false;
		chat = false;
		assertEquals(Color.BLUE.getRGB(), render().getRGB(20, 80));
	}

	@Test
	public void inactiveAndInvalidViewportDrawNothing()
	{
		active = false;
		assertEquals(0, render().getRGB(20, 20));
		active = true;
		viewport = new Rectangle();
		assertEquals(0, render().getRGB(20, 20));
		viewport = new Rectangle(-10, -10, 30, 30);
		BufferedImage image = render();
		assertEquals(new Color(0, 0, 0, 128).getRGB(), image.getRGB(0, 0));
		assertEquals(0, image.getRGB(25, 25));
	}

	@Test
	public void zeroOpacityIsClearAndMissingColorDefaultsToBlack()
	{
		opacity = 0;
		assertEquals(0, render().getRGB(20, 20));
		opacity = 200;
		color = null;
		assertEquals(Color.BLACK.getRGB(), render().getRGB(20, 20));
	}

	@Test
	public void pluginClearsDimmingForInterfacesOtherBoatsAndLoading() throws Exception
	{
		AfkSalvagingPlugin gated = new AfkSalvagingPlugin()
		{
			@Override public boolean isOwnBoat() { return ownBoat; }
			@Override public boolean mainInterfaceOpen() { return mainInterface; }
		};
		Field clientField = AfkSalvagingPlugin.class.getDeclaredField("client");
		clientField.setAccessible(true);
		clientField.set(gated, client);
		Field configField = AfkSalvagingPlugin.class.getDeclaredField("config");
		configField.setAccessible(true);
		configField.set(gated, new AfkSalvagingConfig()
		{
			@Override public boolean dimWhenCrewSalvaging() { return true; }
		});
		gated.getSession().view().crewSalvaging = true;
		assertTrue(gated.shouldShowDimmer());
		mainInterface = true;
		assertFalse(gated.shouldShowDimmer());
		mainInterface = false;
		ownBoat = false;
		assertFalse(gated.shouldShowDimmer());
		ownBoat = true;
		gameState = GameState.LOADING;
		assertFalse(gated.shouldShowDimmer());
		gameState = GameState.LOGIN_SCREEN;
		assertFalse(gated.shouldShowDimmer());
		gameState = GameState.LOGGED_IN;
		assertTrue(gated.shouldShowDimmer());
	}
}
