package com.github.sergueik.selenium;

import static java.lang.System.err;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.gson.JsonSyntaxException;

/**
 * Selected test scenarios for Selenium 4 Chrome Developer Tools bridge inspired
 * by https://toster.ru/q/653249?e=7897302#comment_1962398
 *
 * @author: Serguei Kouzmine (kouzmine_serguei@yahoo.com)
 */
// https://www.logicalincrements.com/articles/resolution

public class ChromiumWindowSizeTest {

	private static final int customWidth = 1366;
	private static final int customHeight = 768;
	private static boolean debug = false;
	private static ChromiumDriver driver;
	private static By locator = null;
	private static WebElement element = null;

	private static WebDriverWait wait;
	private static Actions actions;
	private static int flexibleWait = 60;
	private static int pollingInterval = 500;
	private static String osName = Utils.getOSName();
	private static String screenshotFileName = null;

	private static String page = null;

	private static String command = null;
	private static Map<String, Object> params = new HashMap<>();
	private static Map<String, Object> result = new HashMap<>();
	private static Map<String, Object> data = new HashMap<>();

	@BeforeClass
	public static void setUp() throws Exception {
		System.setProperty("webdriver.chrome.driver", Paths.get(System.getProperty("user.home")).resolve("Downloads")
				.resolve(osName.equals("windows") ? "chromedriver.exe" : "chromedriver").toAbsolutePath().toString());

		ChromeOptions options = new ChromeOptions();
		options.addArguments(Arrays.asList("--ssl-protocol=any", "--ignore-ssl-errors=true", "--disable-extensions",
				"--ignore-certificate-errors", "--headless", "--disable-gpu",
				String.format("window-size=%d,%d", customWidth, customHeight)));

		options.setExperimentalOption("useAutomationExtension", false);

		driver = new ChromeDriver(options);
		actions = new Actions(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(flexibleWait));
		wait.pollingEvery(Duration.ofMillis(pollingInterval));
		Utils.setDriver(driver);
	}

	@AfterClass
	public static void tearDown() {
		if (driver != null) {
			driver.quit();
		}
	}

	@After
	public void clearPage() {
		driver.get("about:blank");
	}

	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#method-getWindowForTarget
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#method-setWindowBounds
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#type-Bounds

	@Ignore
	@SuppressWarnings("unchecked")
	@Test
	public void screenshotSizeTest() {
		page = "image_page.html";

		driver.get(getPageContent(page));
		command = "Browser.getWindowForTarget";
		Long windowId = (long) -1;
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<String, Object>());
			// Assert
			assertThat(result, notNullValue());
			System.err.println("Command " + command + " result: " + result);
			assertThat(result, hasKey("windowId"));
			windowId = (long) result.get("windowId");
			command = "Browser.getWindowBounds";
			params = new HashMap<String, Object>();
			params.put("windowId", windowId);
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			assertThat(result, hasKey("bounds"));
			data = (Map<String, Object>) result.get("bounds");
			assertThat(data, notNullValue());
			assertThat(data, hasKey("width"));
			assertThat(data, hasKey("height"));
			int width = Integer.parseInt(data.get("width").toString());
			assertThat(width, is(customWidth));
			// will fail when visible like:
			// Expected: is <1366> but: was <1051>
			int height = Integer.parseInt(data.get("height").toString());
			assertThat(height, is(customHeight));
			if (debug) {
				System.err.println("Command " + command + " result: " + result);
			}

			if (debug) {
				System.err.println("Internal: result class: " + result.getClass().getName());
				// com.google.common.collect.SingletonImmutableBiMap
			}
			try {
				result.clear();
			} catch (UnsupportedOperationException e) {
				result = null;
			}
			screenshotFileName = "image_page.png";
			getFullPageScreenShot();
		} catch (JsonSyntaxException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		} catch (WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + Utils.processExceptionMessage(e.getMessage()));
		} catch (Exception e) {
			err.println("Exception: in " + command + "  " + e.toString());
			e.printStackTrace();
			throw (new RuntimeException(e));
		}
	}

	@Test
	public void evaluateSizeTest() {
		page = "fixed_size_page.html";
		driver.get(getPageContent(page));
		element = wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div.absolute > span")));
		assertThat(element, notNullValue());
		assertThat(element.isDisplayed(), is(true));
		System.err.println("evaluateSizeTest element location: x=" + element.getLocation().x + "," + "y="
				+ element.getLocation().y);
		// TODO: rendering issue:
		// evaluateSizeTest element location: x=-39,y=721
		screenshotFileName = "fixed_size_page.png";
		getFullPageScreenShot();
	}

	protected String getPageContent(String pagename) {
		try {
			URI uri = ChromiumWindowSizeTest.class.getClassLoader().getResource(pagename).toURI();
			err.println("Testing local file: " + uri.toString());
			return uri.toString();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private void getFullPageScreenShot() {

		String dataString = null;
		try {

			command = "Page.captureScreenshot";
			result = driver.executeCdpCommand(command, new HashMap<>());
			assertThat(result, notNullValue());
			assertThat(result, hasKey("data"));
			dataString = (String) result.get("data");
			assertThat(dataString, notNullValue());
			Base64 base64 = new Base64();
			byte[] image = base64.decode(dataString);
			BufferedImage o = ImageIO.read(new ByteArrayInputStream(image));
			assertThat(o.getWidth(), greaterThan(0));
			assertThat(o.getHeight(), greaterThan(0));
			FileOutputStream fileOutputStream = new FileOutputStream(screenshotFileName);
			fileOutputStream.write(image);
			fileOutputStream.close();
			// read it back
			ImageUtils.getImageDimension();
			System.err.println("Dimensions: " + ImageUtils.dimension.width + "," + ImageUtils.dimension.height);
		} catch (JsonSyntaxException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		} catch (WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + Utils.processExceptionMessage(e.getMessage()));
		} catch (IOException e) {
			err.println("Exception saving image (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: in " + command + "  " + e.toString());
			e.printStackTrace();
			throw (new RuntimeException(e));
		}
	}

	private static class ImageUtils {

		// based on: https://stackoverflow.com/questions/672916/how-to-get-image

		private static ImageReader reader;
		private static ImageInputStream stream;
		private static Dimension dimension = new Dimension(0, 0);;

		public static void getImageDimension() throws IOException {
			try {
				reader = ImageIO.getImageReadersBySuffix("png").next();
				stream = new FileImageInputStream(new File(screenshotFileName));
				reader.setInput(stream);
				int index = reader.getMinIndex();
				dimension = new Dimension(reader.getWidth(index), reader.getHeight(index));
			} catch (IOException e) {
				System.err.println("Error reading image file: " + screenshotFileName + " " + e.toString());
			} finally {
				reader.dispose();
			}
		}
	}
}
