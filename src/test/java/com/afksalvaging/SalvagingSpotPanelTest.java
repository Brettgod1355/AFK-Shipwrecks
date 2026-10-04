package com.afksalvaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SalvagingSpotPanelTest
{
	@Test
	public void wrappedHtmlGivesTheBodyAWidthInPoints()
	{
		// Swing scales a CSS px by 1.3 but a pt by 1, so a width in pt comes out as pixels.
		assertEquals("<html><body style='width:172pt'>55 tiles south-east</body></html>",
			SalvagingSpotPanel.html("55 tiles south-east", 172));
	}

	@Test
	public void tipsWrapToTheSameWidthRule()
	{
		String html = SalvagingTips.html(190);
		assertTrue(html.startsWith("<html><body style='width:190pt'>"));
		assertTrue(html.endsWith("</ul></body></html>"));
	}
}
