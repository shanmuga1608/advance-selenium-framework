package org.automation.config;

import static java.util.Collections.synchronizedList;

import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.WebDriver;

import net.lightbody.bmp.BrowserMobProxy;

/**
 * To create and handle web driver instances.
 * 
 * @author Sujay Sawant
 * @version 1.0.0
 * @since 6/11/2020
 *
 */
public class DriverFactory {

	private static List<WebDriverThread> driverThreadPool = synchronizedList(new ArrayList<WebDriverThread>());
	private static ThreadLocal<WebDriverThread> driverThread;

	/**
	 * Initialize the web driver thread.
	 */
	public static void instantiateDriverObject() {
		driverThread = new ThreadLocal<WebDriverThread>() {
			@Override
			protected WebDriverThread initialValue() {
				WebDriverThread driverThread = new WebDriverThread();
				driverThreadPool.add(driverThread);
				return driverThread;
			}
		};
	}

	/**
	 * Get the current thread's web driver instance.
	 * 
	 * @return web driver instance
	 */
	public static WebDriver getDriver() {
		return driverThread.get().getDriver();
	}

	/**
	 * Get the current thread's browser mob proxy enabled web driver instance.
	 * 
	 * @return browser mob proxy enabled web driver instance
	 */
	public static WebDriver getBrowserMobProxyEnabledDriver() {
		return driverThread.get().getBrowserMobProxyEnabledDriver();
	}

	/**
	 * Get the current thread's browser mob proxy instance.
	 * 
	 * @return browser mob proxy instance
	 */
	public static BrowserMobProxy getBrowserMobProxy() {
		return driverThread.get().getBrowserMobProxy();
	}

	/**
	 * Delete all cookies in the current thread's web driver instance.
	 */
	public static void quitDriver() {
		getDriver().manage().deleteAllCookies();
	}

	/**
	 * Close the all the driver instances.
	 */
	public static void closeDriverObjects() {
		driverThreadPool.forEach(WebDriverThread::quitDriver);
	}

}
