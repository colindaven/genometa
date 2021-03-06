package org.bioviz.protannot;

import java.awt.Image;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class to handle Integration so it will behave more mac-like on OS X.
 * This is achieved using reflection so that the apple-specific classes will
 * not interfere on other platforms.
 *
 * @author sgblanch
 * @version $Id: MacIntegration.java 5804 2010-04-28 18:54:46Z sgblanch $
 */
final class MacIntegration {
	/** private instance of MacIntegration for singleton pattern */
	private static MacIntegration instance = null;
	private Class<?> applicationClass = null;
	private Object application = null;

	/**
	 * Private constructor to enforce singleton pattern
	 */
	private MacIntegration() {
		try {
			applicationClass = Class.forName("com.apple.eawt.Application");
			Method getApplication = applicationClass.getDeclaredMethod("getApplication");
			application = getApplication.invoke(null);

			Method setEnabledPreferencesMenu = applicationClass.getDeclaredMethod("setEnabledPreferencesMenu", Boolean.TYPE);
			setEnabledPreferencesMenu.invoke(application, true);

			Method addApplicationListener = applicationClass.getDeclaredMethod(
					"addApplicationListener",
					Class.forName("com.apple.eawt.ApplicationListener"));

			Class<?> applicationAdapterClass = Class.forName("com.apple.eawt.ApplicationAdapter");
			Object proxy = ApplicationListenerProxy.newInstance(applicationAdapterClass.newInstance());
			addApplicationListener.invoke(application, proxy);

		} catch (Exception ex) {
			Logger.getLogger(MacIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Initialize the singleton copy of MacIntegration.  This should only be
	 * called once by the application, but it protects itself against multiple
	 * invocations.  Do not call this function on anything platform other
	 * than Macintosh: Undefined things will happen.
	 *
	 * @return a singleton instance of MacIntegration
	 */
	public static synchronized MacIntegration getInstance() {
		if (instance == null) {
			instance = new MacIntegration();
		}
		return instance;
	}

	/**
	 * Wrapper around Apple's com.apple.eawt.setDockIconImage.
	 *
	 * @param image the Image to use as the Dock icon.
	 */
	public void setDockIconImage(Image image) {
		try {
			Method setDockIconImage = applicationClass.getDeclaredMethod("setDockIconImage", Image.class);
			setDockIconImage.invoke(application, image);
		} catch (Exception ex) {
			Logger.getLogger(MacIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}

final class ApplicationListenerProxy implements InvocationHandler {
	private final Object o;

	public static Object newInstance(Object o) {
		return Proxy.newProxyInstance(
				o.getClass().getClassLoader(),
				o.getClass().getInterfaces(),
				new ApplicationListenerProxy(o));
	}

	private ApplicationListenerProxy(Object o) {
		this.o = o;
	}

	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Object result = null;
		try {
			if (method.getName().equals("handleAbout")) {
				Actions.getAboutAction().actionPerformed(null);
				Method setHandled = Class.forName("com.apple.eawt.ApplicationEvent").getDeclaredMethod("setHandled", Boolean.TYPE);
				setHandled.invoke(args[0], true);
			} else if (method.getName().equals("handleQuit")) {
				Actions.getExitAction().actionPerformed(null);
			} else if (method.getName().equals("handlePreferences")) {
				Actions.getPreferencesAction().actionPerformed(null);
			} else {
				result = method.invoke(o, args);
			}
		} catch (Exception ex) {
			Logger.getLogger(MacIntegration.class.getName()).log(Level.SEVERE, null, ex);
		}
		return result;
	}
}
