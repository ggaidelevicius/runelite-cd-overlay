package com.cooldownoverlays;

import java.awt.Frame;
import org.junit.Assume;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MacOSRuntimeAccessTest
{
	@Test
	public void macOsDesktopPackagesAreAccessibleToRuneLite() throws ClassNotFoundException
	{
		Assume.assumeTrue(System.getProperty("os.name").startsWith("Mac"));

		Module desktopModule = Frame.class.getModule();
		Module pluginModule = MacOSRuntimeAccessTest.class.getModule();
		assertTrue("macOS desktop integration must be exported to RuneLite",
			desktopModule.isExported("com.apple.eawt", pluginModule));
		assertTrue("macOS fullscreen events must be exported to RuneLite",
			desktopModule.isExported("com.apple.eawt.event", pluginModule));
		// Link the package-private adapter without creating a window or starting RuneLite.
		// Keep this test outside RuneLite's signed packages to avoid signer conflicts.
		assertNotNull(Class.forName("net.runelite.client.ui.OSXFullScreenAdapter", false,
			MacOSRuntimeAccessTest.class.getClassLoader()));
	}
}
