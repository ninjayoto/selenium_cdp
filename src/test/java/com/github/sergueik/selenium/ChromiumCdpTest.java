package com.github.sergueik.selenium;

import static java.lang.System.err;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;

import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.time.Duration;

import javax.imageio.ImageIO;

import java.nio.file.Paths;

import java.util.Optional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;

import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.chromium.ChromiumDriver;

import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.CapabilityType;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

/**
 * Selected test scenarios for Selenium 4 Chrome Developer Tools bridge
 * inspired by
 * https://toster.ru/q/653249?e=7897302#comment_1962398
 *
 * @author: Serguei Kouzmine (kouzmine_serguei@yahoo.com)
 */

public class ChromiumCdpTest {

	private static boolean runHeadless = false;
	private static int flexibleWait = 60;
	private static int pollingInterval = 500;
	private static ChromiumDriver driver;
	private static WebDriver webDriver;

	private static String osName = Utils.getOSName();

	private static WebDriverWait wait;
	private static Actions actions;
	private static String baseURL = "about:blank";
	private static Gson gson = new Gson();

	private static String command = null;
	private static Map<String, Object> data = new HashMap<>();
	private static String dataString = null;
	private static Map<String, Object> result = new HashMap<>();
	private static Map<String, Object> params = new HashMap<>();
	private static List<Map<String, Object>> cookies = new ArrayList<>();
	public static Long nodeId = (long) -1;
	public static String isolationId = null;

	@SuppressWarnings("deprecation")
	@BeforeClass
	public static void setUp() throws Exception {
		System
				.setProperty("webdriver.chrome.driver",
						Paths.get(System.getProperty("user.home"))
								.resolve("Downloads").resolve(osName.equals("windows")
										? "chromedriver.exe" : "chromedriver")
								.toAbsolutePath().toString());

		// NOTE: protected constructor method is not visible
		/* 
		 * driver = new ChromiumDriver((CommandExecutor) null, new ImmutableCapabilities(),null);
		 */
		ChromeOptions options = new ChromeOptions();
		options.addArguments(Arrays.asList("--ssl-protocol=any"));
		options.addArguments(Arrays.asList("--ignore-ssl-errors=true"));
		options.addArguments(Arrays.asList("--disable-extensions"));
		options.addArguments(Arrays.asList("--ignore-certificate-errors"));
		options.setExperimentalOption("useAutomationExtension", false);
		Map<String, Object> prefs = new HashMap<>();
		if (runHeadless) {
			options.addArguments(Arrays.asList("--headless", "--disable-gpu"));
		}
		// DesiredCapabilities capabilities = DesiredCapabilities.chrome();
		// capabilities.setCapability(ChromeOptions.CAPABILITY, options);

		driver = new ChromeDriver(options);

		actions = new Actions(driver);
		wait = new WebDriverWait(driver, Duration.ofSeconds(flexibleWait));
		Utils.setDriver(driver);
		// Selenium Driver version sensitive code: 3.13.0 vs. 3.8.0 and older
		wait.pollingEvery(Duration.ofMillis(pollingInterval));
	}

	@Before
	public void beforeTest() throws Exception {
		driver.get(baseURL);
	}

	@AfterClass
	public static void tearDown() {
		if (driver != null) {
			driver.quit();
		}
	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-getDocuments
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-Node
	@SuppressWarnings("unchecked")
	@Test
	public void getDocumentTest() {
		// Arrange
		driver.get("https://www.google.com");
		// returns the root DOM node and subtree, default to depth 1
		command = "DOM.getDocument";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			// Assert
			assertThat(result, hasKey("root"));
			data.clear();
			data = (Map<String, Object>) result.get("root");
			assertThat(data, hasKey("nodeId"));
			assertTrue(Long.parseLong(data.get("nodeId").toString()) != 0);
			err.println("Command " + command + " return node: "
					+ new Gson().toJson(data, Map.class));
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-getDocuments
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-Node
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-describeNode
	@SuppressWarnings("unchecked")
	@Test
	public void describeNodeTest() {
		// Arrange
		driver.get("https://www.google.com");
		String command = "DOM.getDocument";
		params = new HashMap<>();
		params.put("pierce", false);
		params.put("depth", 0);
		try {
			// Act
			result = driver.executeCdpCommand(command, params);
			nodeId = (Long) ((Map<String, Object>) result.get("root")).get("nodeId");
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
		// Describes node given its id
		command = "DOM.describeNode";
		params.clear();
		params.put("nodeId", nodeId);
		params.put("depth", 0);
		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, hasKey("node"));
			data = (Map<String, Object>) result.get("node");
			for (String field : Arrays.asList(new String[] { "baseURL", "localName",
					"nodeName", "nodeType", "nodeValue" })) {
				assertThat(data, hasKey(field));
			}
			assertThat(data.get("nodeName"), is("#document"));
			System.err.println("Command " + command + " returned node: "
					+ new Gson().toJson(data, Map.class));
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-getDocuments
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-Node
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-querySelector
	@SuppressWarnings("unchecked")
	@Test
	public void querySelectorTest() {
		// Arrange
		driver.get("https://www.google.com");
		String command = "DOM.getDocument";
		params = new HashMap<>();
		params.put("pierce", false);
		params.put("depth", 0);
		try {
			// Act
			result = driver.executeCdpCommand(command, params);
			nodeId = Long.parseLong(
					((Map<String, Object>) result.get("root")).get("nodeId").toString());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
		// Executes querySelector on a given node.
		command = "DOM.querySelector";
		params.clear();
		params.put("nodeId", nodeId);
		params.put("selector", "img#hplogo");

		try {
			result = driver.executeCdpCommand(command, params);
			assertThat(result, hasKey("nodeId"));
			nodeId = (Long) result.get("nodeId");
			assertTrue(nodeId != 0);
			err.println("Command " + command + " returned nodeId: " + nodeId);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
		// Returns node's HTML markup
		command = "DOM.getOuterHTML";
		params.clear();
		params.put("nodeId", nodeId);
		dataString = null;
		try {
			result = driver.executeCdpCommand(command, params);
			assertThat(result, notNullValue());
			assertThat(result, hasKey("outerHTML"));
			dataString = (String) result.get("outerHTML");
			assertThat(dataString, notNullValue());
			err.println("Command " + command + " return outerHTML: " + dataString);
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}
	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-getDocuments
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-Node
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-querySelector
	@SuppressWarnings("unchecked")
	@Test
	public void querySelectorAllTest() {
		// Arrange
		driver.get("https://www.google.com");
		String command = "DOM.getDocument";
		params = new HashMap<>();
		params.put("pierce", false);
		params.put("depth", 0);
		try {
			// Act
			result = driver.executeCdpCommand(command, params);
			nodeId = Long.parseLong(
					((Map<String, Object>) result.get("root")).get("nodeId").toString());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
		// Executes querySelectorAll on a given node.
		command = "DOM.querySelectorAll";
		params.clear();
		params.put("nodeId", nodeId);
		params.put("selector", "input[type='submit']");

		try {
			result = driver.executeCdpCommand(command, params);
			assertThat(result, hasKey("nodeIds"));
			List<Long> nodeIds = (List<Long>) result.get("nodeIds");
			assertThat(nodeIds, notNullValue());
			assertTrue(nodeIds.size() != 0);
			err.println("Command " + command + " returned nodeIds: " + nodeIds);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
	}

	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-getDocument
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-Node
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-resolveNode
	@SuppressWarnings("unchecked")
	@Test
	public void resolveNodTest() {
		// Arrange
		driver.get("https://www.google.com");
		String command = "DOM.getDocument";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			nodeId = (Long) ((Map<String, Object>) result.get("root")).get("nodeId");
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
		// select DOM node in #document
		command = "DOM.querySelector";
		params.clear();
		params.put("nodeId", nodeId);
		params.put("selector", "img#hplogo");

		try {
			result = driver.executeCdpCommand(command, params);
			nodeId = (Long) result.get("nodeId");
		} catch (Exception e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}

		// Resolves the JavaScript node object for a given NodeId or BackendNodeId
		command = "DOM.resolveNode";
		params.clear();
		params.put("nodeId", nodeId);

		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, hasKey("object"));
			data.clear();
			// JavaScript object wrapper for given node
			data = (Map<String, Object>) result.get("object");
			for (String field : Arrays.asList(new String[] { "type", "subtype",
					"className", "description", "objectId" })) {
				assertThat(data, hasKey(field));
			}
			dataString = (String) data.get("objectId");
			assertThat(dataString, notNullValue());
			// reuse data to peek into dataString
			data = (Map<String, Object>) new Gson().fromJson(dataString, Map.class);
			// Unique object identifier
			System.err
					.println("Command " + command + " returned objectId data: " + data);

		} catch (WebDriverException | JsonSyntaxException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
	}

	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-getDocument
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-Node
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-resolveNode
	@SuppressWarnings("unchecked")
	@Test
	// TODO: command = "Runtime.callFunctionOn";

	public void callFunctionOnTest() {
		// Arrange
		driver.get("https://www.google.com");
		String command = "DOM.getDocument";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			nodeId = (Long) ((Map<String, Object>) result.get("root")).get("nodeId");
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
		// select DOM node in #document
		command = "DOM.querySelector";
		params.clear();
		params.put("nodeId", nodeId);
		params.put("selector", "img#hplogo");

		try {
			result = driver.executeCdpCommand(command, params);
			nodeId = (Long) result.get("nodeId");
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}

		// Resolves the JavaScript node object for a given NodeId or BackendNodeId
		command = "DOM.resolveNode";
		params.clear();
		params.put("nodeId", nodeId);

		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, hasKey("object"));
			data.clear();
			// JavaScript object wrapper for given node
			data = (Map<String, Object>) result.get("object");
			// reuse data to peek into dataString
			dataString = (String) data.get("object");
			data = (Map<String, Object>) new Gson()
					.fromJson((String) data.get("objectId"), Map.class);
			// Unique object identifier
			System.err
					.println("Command " + command + " returned objectId data: " + data);
		} catch (WebDriverException | JsonSyntaxException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}

		command = "Runtime.callFunctionOn";
		params = new HashMap<>();
		params.put("functionDeclaration", "function() { this.value=''; }");
		params.put("objectId", dataString);

		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, hasKey("result"));
			data.clear();
			data = (Map<String, Object>) result.get("result");
			for (String field : Arrays.asList(
					new String[] { "type", "subtype", "className", "objectId" })) {
				assertThat(data, hasKey(field));
			}
			String objectId = (String) data.get("objectId");
			assertThat(objectId, notNullValue());
			System.err
					.println("Command " + command + " returned objectId: " + objectId);
		} catch (WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-getDocument
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-Node
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-querySelector
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-describeNode
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-focus
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM/#method-highlightNode
	// https://chromedevtools.github.io/devtools-protocol/tot/Runtime#type-RemoteObjectId
	@SuppressWarnings("unchecked")
	@Test
	public void multiCommandTest() {
		// Arrange
		driver.get("https://www.google.com");
		command = "DOM.getDocument";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			// Assert
			assertThat(result, hasKey("root"));
			@SuppressWarnings("unchecked")
			Map<String, Object> node = (Map<String, Object>) result.get("root");
			assertThat(node, hasKey("nodeId"));
			nodeId = Long.parseLong(node.get("nodeId").toString());
			assertTrue(nodeId != 0);
			err.println("Command " + command + " returned nodeId: " + nodeId);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
		command = "DOM.describeNode";
		params = new HashMap<>();
		params.put("nodeId", nodeId);
		params.put("depth", 1);
		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, hasKey("node"));
			// reuse "params" variable
			params.clear();
			params = (Map<String, Object>) result.get("node");
			for (String field : Arrays.asList(
					new String[] { "nodeType", "nodeName", "localName", "nodeValue" })) {
				assertThat(params, hasKey(field));
			}

			System.err.println("Command " + command + " returned: "
					+ new Gson().toJson(params, Map.class));
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}

		command = "DOM.querySelector";
		// params.clear();
		// causes java.lang.UnsupportedOperationException
		params = new HashMap<>();
		params.put("nodeId", nodeId);
		// params.put("selector", "img#hplogo");
		params.put("selector", "input[name='q']");

		try {
			result = driver.executeCdpCommand(command, params);
			// depth, 1
			// Assert
			assertThat(result, hasKey("nodeId"));
			// @SuppressWarnings("unchecked")
			nodeId = Long.parseLong(result.get("nodeId").toString());
			assertTrue(nodeId != 0);
			err.println("Command " + command + " returned  nodeId: " + nodeId);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}

		command = "DOM.resolveNode";
		params = new HashMap<>();
		params.put("nodeId", nodeId);

		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, hasKey("object"));
			data.clear();
			data = (Map<String, Object>) result.get("object");
			for (String field : Arrays.asList(
					new String[] { "type", "subtype", "className", "objectId" })) {
				assertThat(data, hasKey(field));
			}
			String objectId = (String) data.get("objectId");
			assertThat(objectId, notNullValue());
			System.err
					.println("Command " + command + " returned objectId: " + objectId);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}

		command = "DOM.something not defined";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
			// wasn't found
		}
		// DOM.removeNode
		command = "DOM.focus";
		params = new HashMap<>();
		params.put("nodeId", nodeId);
		try {
			// Act
			result = driver.executeCdpCommand(command, params);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
			// : unknown error: unhandled inspector error:
			// {"code":-32000,"message":"Element is not focusable"}
		}
		command = "DOM.highlightNode";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			Utils.sleep(10000);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println(
					"Exception in command " + command + " (ignored): " + e.toString());
		}
		// TODO: command = "Runtime.callFunctionOn";
	}

	// @Ignore
	@Test
	public void getIsolatedIdTest() {
		// Arrange
		baseURL = "https://www.google.com";
		driver.get(baseURL);
		String command = "Runtime.getIsolateId";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			assertThat(result, hasKey("id"));
			isolationId = (String) result.get("id");
			assertThat(isolationId, notNullValue());
			// isolationId = Long.parseLong((String) result.get("id"));
			// assertTrue(isolationId != 0);
			// Assert ?
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	// @Ignore
	@Test
	public void compileScriptTest() {
		// Arrange
		baseURL = "https://www.google.com";
		driver.get(baseURL);
		command = "Runtime.compileScript";
		// "Runtime.runScript"
		params = new HashMap<>();
		params.put("expression", "function() { alert('test'); }");
		params.put("persistScript", false);
		// params.put("sourceURL", null);

		// params.put("executionContextId", 0);
		try {
			result = driver.executeCdpCommand(command, params);
			System.err.println("Response to " + command + ": " + result);
		} catch (UnhandledAlertException e) {
			assertThat(e.toString(), containsString("unexpected alert open"));
			err.println("Exception (ignored): " + e.toString());
		} catch (WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
			// assertThat(e.toString(), containsString("invalid argument: Invalid
			// parameters"));
		}

	}

	// @Ignore
	@Test
	public void evaluateTest() {
		// Arrange
		baseURL = "https://www.google.com";
		driver.get(baseURL);
		command = "Runtime.evaluate";
		params = new HashMap<>();
		params.put("expression", "var f = function() { alert('test'); }");
		// {result={type=undefined}}
		// params.put("expression", "alert('test');");
		// NPE in org.openqa.selenium.chromium.ChromiumDriver.executeCdpCommand
		try {
			result = driver.executeCdpCommand(command, params);
			System.err.println("Response to " + command + ": " + result);
		} catch (WebDriverException e) {
			err.println("Exception (rethrown): " + e.toString());
			throw new RuntimeException(e.toString());
		}

	}

	@Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/Network#method-setUserAgentOverride
	// https://stackoverflow.com/questions/29916054/change-user-agent-for-selenium-driver
	@SuppressWarnings("serial")
	@Test
	public void setUserAgentOverrideTest() {
		// Arrange
		driver.get("https://www.whoishostingthis.com/tools/user-agent/");
		By locator = By.cssSelector("a[href='/']");
		WebElement element = driver.findElement(locator);
		assertThat(element.getAttribute("innerText"), containsString("Mozilla"));
		// Expected: a string containing "Mozilla" but: was "WhoIsHostingThis
		// Act
		try {
			driver.executeCdpCommand("Network.setUserAgentOverride",
					new HashMap<String, Object>() {
						{
							put("userAgent", "python 2.7");
							put("platform", "Windows");
						}
					});
		} catch (WebDriverException e) {
			System.err.println("Exception (ignored): " + e.toString());
			// org.openqa.selenium.WebDriverException: unknown error: unhandled
			// inspector error :
			// {"code":-32601,"message":"'setUserAgentOverride'
			// wasn't found"}
		}
		driver.navigate().refresh();
		Utils.sleep(1000);

		element = driver.findElement(locator);
		assertThat(element.isDisplayed(), is(true));
		assertThat(element.getAttribute("innerText"), is("python 2.7"));
	}

	// @Ignore
	// see:
	// https://github.com/sergueik/powershell_selenium/blob/master/python/print_pdf.py
	// origin: https://habr.com/ru/post/459112/
	// NOTE: Python uses different REST-like route
	// "/session/$sessionId/chromium/send_command_and_get_result"
	// than Java
	// "/session/$sessionId/goog/cdp/execute"
	// https://chromedevtools.github.io/devtools-protocol/tot/Page#method-printToPDF
	@Test
	public void printToPDFTest() {
		driver.get("https://www.google.com");
		String command = "Page.printToPDF";
		params = new HashMap<>();
		params.put("landscape", false);
		params.put("displayHeaderFooter", false);
		params.put("printBackground", true);
		params.put("preferCSSPageSize", true);
		try {
			result = driver.executeCdpCommand(command, params);
			System.err.println("Response to " + command + ": " + result);
			// TODO: assert the response is a valid Base64-encoded pdf data.
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
			assertThat(e.toString(), containsString("PrintToPDF is not implemented"));
			// printToPDFTest(com.github.sergueik.selenium.ChromiumCdpTest):
			// unknown
			// error: unhandled inspector error:
			// {
			// "code": -32000,
			// "message": "PrintToPDF is not implemented"
			// }
		}
	}

	// @Ignore
	@Test
	// https://chromedevtools.github.io/devtools-protocol/tot/Network#method-clearBrowserCache
	public void clearBrowserCacheTest() {
		baseURL = "https://www.google.com";
		driver.get(baseURL);
		String command = "Network.clearBrowserCache";
		try {
			// Act
			driver.executeCdpCommand(command, new HashMap<>());
			// Assert ?
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	// @Ignore
	@Test
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#method-getVersion
	public void getBrowserVersionTest() {
		command = "Browser.getVersion";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<String, Object>());
			// Assert
			assertThat(result, notNullValue());
			System.err.println("Command " + command + " result: " + result);
			/* typical response
			{
			    jsVersion = 7.8.279.23,
			    product = Chrome/78.0.3904.108,
			    protocolVersion = 1.3,
			    revision = @4b26898a39ee037623a72fcfb77279fce0e7d648,
			    userAgent = Mozilla/5.0 (Windows NT6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36
			}
			 */
			for (String field : Arrays.asList(new String[] { "protocolVersion",
					"product", "revision", "userAgent", "jsVersion" })) {
				assertThat(result, hasKey(field));
			}
		} catch (JsonSyntaxException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: " + e.toString());
			throw (new RuntimeException(e));
		}
	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#method-getWindowForTarget
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#method-setWindowBounds
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#type-Bounds
	@Test
	public void setWindowBoundsTest() {
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
		} catch (JsonSyntaxException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: in " + command + "  " + e.toString());
			throw (new RuntimeException(e));
		}
		try {
			command = "Browser.setWindowBounds";
			params = new HashMap<String, Object>();
			Map<String, Object> bounds = new HashMap<String, Object>();
			String windowState = "minimized";
			bounds.put("windowState", windowState);
			params.put("bounds", bounds);
			params.put("windowId", windowId);
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			System.err.println("Command " + command + " result: " + result);
		} catch (JsonSyntaxException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: in " + command + "  " + e.toString());
			throw (new RuntimeException(e));
		}
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		try {
			command = "Browser.setWindowBounds";
			params = new HashMap<String, Object>();
			Map<String, Object> bounds = new HashMap<String, Object>();
			String windowState = "normal";
			bounds.put("windowState", windowState);
			params.put("bounds", bounds);
			params.put("windowId", windowId);
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			System.err.println("Command " + command + " result: " + result);
		} catch (JsonSyntaxException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: in " + command + "  " + e.toString());
			throw (new RuntimeException(e));
		}

	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#method-getWindowForTarget
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#method-getWindowBounds
	// https://chromedevtools.github.io/devtools-protocol/tot/Browser#type-Bounds
	@Test
	public void getBrowserWindowDetailsTest() {
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
		} catch (JsonSyntaxException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: in " + command + "  " + e.toString());
			throw (new RuntimeException(e));
		}
		try {
			command = "Browser.getWindowBounds";
			params = new HashMap<String, Object>();
			params.put("windowId", windowId);
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			System.err.println("Command " + command + " result: " + result);
		} catch (JsonSyntaxException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: in " + command + "  " + e.toString());
			throw (new RuntimeException(e));
		}
	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/Network#method-enable
	@Test
	public void manageNetworkTrackingTest() {
		baseURL = "http://www.example.com/";
		command = "Network.enable";
		try {
			// Act
			params = new HashMap<String, Object>();
			params.put("maxTotalBufferSize", 100000000);
			params.put("maxResourceBufferSize", null);
			params.put("maxPostDataSize", null);
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			System.err.println("Command " + command + " result: " + result);
		} catch (org.openqa.selenium.InvalidArgumentException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (JsonSyntaxException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: " + e.toString());
			throw (new RuntimeException(e));
		}
		driver.get(baseURL);
		command = "Network.disable";
		try {
			// Act
			driver.executeCdpCommand(command, new HashMap<>());
		} catch (JsonSyntaxException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	// @Ignore
	// https://chromedevtools.github.io/devtools-protocol/tot/Network#method-getResponseBody
	@Test
	public void getResponseBodyTest() {
		baseURL = "http://www.example.com/";
		driver.get(baseURL);
		command = "Network.getResponseBody";
		try {
			// Act
			params = new HashMap<String, Object>();
			params.put("requestId", "");
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			System.err.println("Command " + command + " result: " + result);
		} catch (org.openqa.selenium.InvalidArgumentException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (JsonSyntaxException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (Exception e) {
			err.println("Exception: " + e.toString());
			throw (new RuntimeException(e));
		}
	}

	// @Ignore
	@SuppressWarnings("unchecked")
	@Test
	// https://chromedevtools.github.io/devtools-protocol/tot/Network#method-getAllCookies
	public void getAllCookiesTest() {
		baseURL = "https://www.google.com";
		driver.get(baseURL);
		command = "Network.getAllCookies";
		List<String> cookies = new ArrayList<>();

		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			// Assert
			assertThat(result, notNullValue());
			assertThat(result, hasKey("cookies"));
			cookies = (List<String>) result.get("cookies");
			System.err.println(cookies);
			// Assert
			assertThat(cookies, notNullValue());
			assertThat(cookies.size(), greaterThan(0));
			/*
			 * cookies.stream().limit(3).forEach(o -> { try { System.err.println(o); } catch
			 * (java.lang.ClassCastExceptin e) { err.println("Exception (ignored): " +
			 * e.toString()); } }) ;
			 */
			/*
			 * for (String cookie : cookies) { System.err.println("Cookie:" + cookie); }
			 */
		} catch (JsonSyntaxException e) {
			err.println("Exception (ignored): " + e.toString());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	// @Ignore
	@Test
	public void clearBrowserCookiesTest() {
		baseURL = "https://www.google.com";
		driver.get(baseURL);
		command = "Network.clearBrowserCookies";
		try {
			// Act
			driver.executeCdpCommand(command, new HashMap<>());
			// Assert ?
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	// @Ignore
	@Test
	// based on:
	// https://github.com/sahajamit/chrome-devtools-webdriver-integration/blob/master/src/test/java/com/sahajamit/DemoTests.java
	// https://chromedevtools.github.io/devtools-protocol/tot/Page#method-captureScreenshot
	// https://chromedevtools.github.io/devtools-protocol/tot/Page#type-Viewport
	public void capturElementScreenshotTest() {

		baseURL = "https://www.google.com/";
		driver.get(baseURL);
		result = null;
		dataString = null;

		WebElement element = wait
				.until(ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//img[@id = 'hplogo'][@alt='Google']")));
		Utils.highlight(element);
		int x = element.getLocation().getX();
		int y = element.getLocation().getY();
		int width = element.getSize().getWidth();
		int height = element.getSize().getHeight();
		int scale = 1;

		command = "Page.captureScreenshot";
		params = new HashMap<String, Object>();
		Map<String, Object> viewport = new HashMap<>();
		System.err.println("Specified viewport: " + String
				.format("x=%d, y=%d, width=%d, height=%d", x, y, width, height));
		viewport.put("x", (double) x);
		viewport.put("y", (double) y);
		viewport.put("width", (double) width);
		viewport.put("height", (double) height);
		viewport.put("scale", scale);
		params.put("clip", viewport);
		try {
			// Act
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			assertThat(result, hasKey("data"));
			dataString = (String) result.get("data");
			assertThat(dataString, notNullValue());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}

		Base64 base64 = new Base64();
		byte[] image = base64.decode(dataString);
		try {
			BufferedImage o = ImageIO.read(new ByteArrayInputStream(image));
			assertThat(o.getWidth(), greaterThan(0));
			assertThat(o.getHeight(), greaterThan(0));
		} catch (IOException e) {
			err.println("Exception loading image (ignored): " + e.toString());
		}
		String tmpFilename = "logo.png";
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(tmpFilename);
			fileOutputStream.write(image);
			fileOutputStream.close();
		} catch (IOException e) {
			err.println("Exception saving image (ignored): " + e.toString());
		}
	}

	// @Ignore
	@Test
	// https://chromedevtools.github.io/devtools-protocol/tot/Page#method-captureScreenshot
	public void captureScreenshotTest() {
		baseURL = "https://www.google.com";
		driver.get(baseURL);
		result = null;
		dataString = null;
		command = "Page.captureScreenshot";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			// Assert
			assertThat(result, notNullValue());
			assertThat(result, hasKey("data"));
			dataString = (String) result.get("data");
			assertThat(dataString, notNullValue());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}

		Base64 base64 = new Base64();
		byte[] image = base64.decode(dataString);
		try {
			BufferedImage o = ImageIO.read(new ByteArrayInputStream(image));
			assertThat(o.getWidth(), greaterThan(0));
			assertThat(o.getHeight(), greaterThan(0));
		} catch (IOException e) {
			err.println("Exception loading image (ignored): " + e.toString());
		}
		String tmpFilename = "temp.png";
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(tmpFilename);
			fileOutputStream.write(image);
			fileOutputStream.close();
		} catch (IOException e) {
			err.println("Exception saving image (ignored): " + e.toString());
		}
	}

	// @Ignore
	@SuppressWarnings("unchecked")
	@Test
	public void getCookiesWithUrlsTest() {
		// Arrange
		baseURL = "https://www.google.com";
		driver.get(baseURL);
		command = "Page.getCookies";
		params = new HashMap<String, Object>();
		params.put("urls", new String[] { ".google.com" });
		// Act
		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			err.println("Cookies count for www.google.com: "
					+ ((List<Object>) result.get("cookies")).size() + "...");
		} catch (Exception e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	// @Ignore
	@Test
	@SuppressWarnings("unchecked")
	public void getCookiesTest1() {
		// Arrange
		driver.get("https://www.google.com");
		command = "Page.getCookies";
		// Act
		try {
			result = driver.executeCdpCommand(command, new HashMap<String, Object>());
			err.println("Cookies: "
					+ result.get("cookies").toString().substring(0, 100) + "...");
			// Assert
			// deserialiaze
			cookies = gson.fromJson(result.get("cookies").toString(),
					ArrayList.class);
			cookies.stream().limit(10).map(o -> o.keySet())
					.forEach(System.err::println);
			Set<String> cookieKeys = new HashSet<>();
			for (String key : new String[] { "domain", "expires", "httpOnly", "name",
					"path", "secure", "session", "size", "value" }) {
				cookieKeys.add(key);
			}
			assertTrue(cookies.get(0).keySet().containsAll(cookieKeys));
		} catch (JsonSyntaxException e) {
			err.println("Exception deserializing cookies (ignored): " + e.toString());

		} catch (Exception e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	// @Ignore
	@Test
	@SuppressWarnings("unchecked")
	public void getCookiesTest2() {
		// Arrange
		driver.get("https://www.google.com");
		command = "Page.getCookies";
		// Act
		try {
			result = driver.executeCdpCommand(command, new HashMap<String, Object>());
			err.println("Cookies: "
					+ result.get("cookies").toString().substring(0, 100) + "...");
			// Assert
			// direct cast
			cookies = (ArrayList<Map<String, Object>>) result.get("cookies");
			cookies.stream().limit(10).map(o -> o.keySet())
					.forEach(System.err::println);
			Set<String> cookieKeys = new HashSet<>();
			for (String key : new String[] { "domain", "expires", "httpOnly", "name",
					"path", "secure", "session", "size", "value" }) {
				cookieKeys.add(key);
			}
			assertTrue(cookies.get(0).keySet().containsAll(cookieKeys));
		} catch (JsonSyntaxException e) {
			err.println("Exception loading cookies (ignored): " + e.toString());

		} catch (Exception e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	// @Ignore
	@SuppressWarnings("unchecked")
	@Test
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#method-performSearch
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#method-getSearchResults
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-NodeId
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#method-getOuterHTML
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#method-highlightNode
	public void performSearchTest() {

		baseURL = "https://datatables.net/examples/api/highlight.html";
		driver.get(baseURL);
		dataString = null;
		command = "DOM.performSearch";
		params = new HashMap<String, Object>();

		WebElement element = driver
				.findElement(By.xpath("//table[@id='example']/tbody/tr[1]/td[1]"));
		err.println("outerHTML: " + element.getAttribute("outerHTML"));

		params.put("query", "//table[@id='example']/tbody/tr[1]/td[1]");
		// Act
		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			assertThat(result, hasKey("searchId"));
			dataString = (String) result.get("searchId");
			err.println("searchId: " + dataString);
			assertThat(data, notNullValue());
			command = "DOM.getSearchResults";
			params = new HashMap<String, Object>();
			params.put("searchId", dataString);
			params.put("fromIndex", 0);
			params.put("toIndex", 1);
			nodeId = (long) -1;
			result = driver.executeCdpCommand(command, params);
			assertThat(result, notNullValue());
			assertThat(result, hasKey("nodeIds"));
			List<Long> nodes = (List<Long>) result.get("nodeIds");
			assertThat(nodes, notNullValue());
			assertThat(nodes.get(0), notNullValue());
			nodeId = nodes.get(0);
			err.println("nodeId: " + nodeId);
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}
		try {

			command = "DOM.getOuterHTML";
			params = new HashMap<String, Object>();
			params.put("nodeId", nodeId);
			dataString = null;
			result = driver.executeCdpCommand(command, params);
			assertThat(result, notNullValue());
			assertThat(result, hasKey("outerHTML"));
			dataString = (String) result.get("outerHTML");
			assertThat(dataString, notNullValue());
			err.println("outerHTML: " + dataString);
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}

		try {
			// TODO:
			// Exception in DOM.highlightNode (ignored):
			// org.openqa.selenium.InvalidArgumentException: invalid argument:
			// Invalid parameters
			command = "DOM.highlightNode";
			driver.executeCdpCommand(command, new HashMap<String, Object>());
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}
	}

	// @Ignore
	@Test
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#method-getNodeForLocation
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#type-NodeId
	// https://chromedevtools.github.io/devtools-protocol/tot/DOM#method-getOuterHTML
	public void getNodeForLocationTest() {

		baseURL = "https://datatables.net/examples/api/highlight.html";
		driver.get(baseURL);
		command = "DOM.getNodeForLocation";
		params = new HashMap<String, Object>();

		WebElement element = driver
				.findElement(By.xpath("//table[@id='example']/tbody/tr[1]/td[1]"));
		int x = element.getLocation().getX();
		int y = element.getLocation().getX();
		err.println(String.format("x = %d, y = %d", x, y));
		err.println("outerHTML: " + element.getAttribute("outerHTML"));
		params.put("x", x);
		params.put("y", y);
		nodeId = (long) -1;
		// Act
		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			assertThat(result, hasKey("backendNodeId"));
			nodeId = (Long) result.get("backendNodeId");
			err.println("backendNodeId: " + nodeId);
			assertThat(nodeId, notNullValue());
			// might not have frameId
			assertThat(result, hasKey("nodeId"));
			nodeId = (Long) result.get("nodeId");
			err.println("nodeId: " + nodeId);
			assertThat(nodeId, notNullValue());
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}

		try {
			command = "DOM.getOuterHTML";
			params = new HashMap<String, Object>();
			params.put("nodeId", nodeId);
			dataString = null;
			result = driver.executeCdpCommand(command, params);
			assertThat(result, notNullValue());
			assertThat(result, hasKey("outerHTML"));
			dataString = (String) result.get("outerHTML");
			assertThat(dataString, notNullValue());
			err.println("outerHTML: " + dataString);
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}
	}

	// @Ignore
	// based on a more advanced code found in
	// https://github.com/adiohana/selenium-chrome-devtools-examples/blob/master/src/test/java/ChromeDevToolsTest.java
	// https://chromedevtools.github.io/devtools-protocol/tot/Network#method-setCacheDisabled
	// https://chromedevtools.github.io/devtools-protocol/tot/Network#method-setBlockedURLs
	@SuppressWarnings("serial")
	@Test
	public void setBlockedURLsTest() {
		// Arrange
		command = "Network.setCacheDisabled";
		try {
			driver.executeCdpCommand(command, new HashMap<String, Object>() {
				{
					put("cacheDisabled", true);
					// NOTE: value has to be a boolean, otherwise
					// org.openqa.selenium.InvalidArgumentException: invalid argument
				}
			});
		} catch (WebDriverException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());

		}
		baseURL = "https://www.wikipedia.org/";
		command = "Network.setBlockedURLs";
		// Act
		try {
			// NOTE: inline hashmap initialization code looks rather ugly
			driver.executeCdpCommand(command, new HashMap<String, Object>() {
				{
					put("urls", Arrays.asList(new String[] { "*.css", "*.png" }));
				}
			});
		} catch (

		Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}
		driver.get(baseURL);
		// driver.navigate().refresh();
		Utils.sleep(1000);
	}

	// @Ignore
	@Test
	// based on:
	// https://chromedevtools.github.io/devtools-protocol/tot/Emulation#method-setGeolocationOverride
	// see also:
	// https://github.com/sahajamit/chrome-devtools-webdriver-integration/blob/master/src/test/java/com/sahajamit/DemoTests.java
	// https://github.com/sahajamit/chrome-devtools-webdriver-integration/blob/master/src/main/java/com/sahajamit/messaging/MessageBuilder.java
	public void setGeoLocationTest() {

		// Arrange
		command = "Emulation.setGeolocationOverride";
		params = new HashMap<String, Object>();
		Double latitude = 37.422290;
		Double longitude = -122.084057;
		params.put("latitude", latitude);
		params.put("longitude", longitude);
		params.put("accuracy", 100);
		// Act
		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			err.println("Response from " + command + ": " + result);
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}
		// Act

		baseURL = "https://www.google.com/maps";
		driver.get(baseURL);

		// click "my location" button when drawn

		WebElement element = wait
				.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(
						"div[class *='widget-mylocation-button-icon-common']")));
		element.click();
		// unclear what event to wait for here
		Utils.sleep(5000);
		result = null;
		dataString = null;
		command = "Page.captureScreenshot";
		try {
			// Act
			result = driver.executeCdpCommand(command, new HashMap<>());
			// Assert
			assertThat(result, notNullValue());
			assertThat(result, hasKey("data"));
			dataString = (String) result.get("data");
			assertThat(dataString, notNullValue());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}

		Base64 base64 = new Base64();
		byte[] image = base64.decode(dataString);
		try {
			BufferedImage o = ImageIO.read(new ByteArrayInputStream(image));
			assertThat(o.getWidth(), greaterThan(0));
			assertThat(o.getHeight(), greaterThan(0));
		} catch (IOException e) {
			err.println("Exception collecting screenshot (ignored): " + e.toString());
		}
		String tmpFilename = "map.png";
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(tmpFilename);
			fileOutputStream.write(image);
			fileOutputStream.close();
		} catch (IOException e) {
			err.println("Exception saving image file (ignored): " + e.toString());
		}
		// Assert
	}

	
	// @Ignore
	@Test
	// based on:
	// https://chromedevtools.github.io/devtools-protocol/tot/Emulation#method-setGeolocationOverride
	// see also:
	// https://github.com/sahajamit/chrome-devtools-webdriver-integration/blob/master/src/test/java/com/sahajamit/DemoTests.java
	// https://github.com/sahajamit/chrome-devtools-webdriver-integration/blob/master/src/main/java/com/sahajamit/messaging/MessageBuilder.java
	public void setPageScaleFactorTest() {

		// Arrange
		baseURL = "https://www.google.com/maps";
		driver.get(baseURL);

		command = "Emulation.setPageScaleFactor";

		// Act
		params = new HashMap<String, Object>();
		// does not appear to work
		params.put("pageScaleFactor", 2.0);
		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			err.println("Response from " + command + ": " + result);
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}
		// Act
		Utils.sleep(1000);
		params = new HashMap<String, Object>();
		params.put("pageScaleFactor", 1);
		// Act
		try {
			result = driver.executeCdpCommand(command, params);
			// Assert
			assertThat(result, notNullValue());
			err.println("Response from " + command + ": " + result);
		} catch (Exception e) {
			err.println("Exception in " + command + " (ignored): " + e.toString());
		}
		// Act
		Utils.sleep(1000);
	}

	@Test
	// https://chromedevtools.github.io/devtools-protocol/tot/Page#method-captureSnapshot
	public void captureSnapshotTest() {
		driver.get("https://developer.chrome.com/extensions/pageCapture");
		String command = "Page.captureSnapshot";
		dataString = null;
		params = new HashMap<>();
		params.put("format", "mhtml");
		try {
			result = driver.executeCdpCommand(command, params);
			assertThat(result, hasKey("data"));
			dataString = (String) result.get("data");
			// Assert
			// like an email, but the following is failing
			for (String field : Arrays.asList(new String[] {
					"Snapshot-Content-Location", "Subject", "Content-Type" })) {
				assertThat(dataString, containsString(String.format("%s:", field)));
			}
			// assertThat(data, containsString("\n\n"));
			String header = dataString.split("\n\n")[0];
			assertThat(header, notNullValue());
			// System.err.println("Response to " + command + ": header" + header);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}

	@Test
	// based on:
	// https://github.com/SrinivasanTarget/selenium4CDPsamples/blob/master/src/test/java/DevToolsTest.java
	// https://chromedevtools.github.io/devtools-protocol/tot/Performance#method-setTimeDomain
	// https://chromedevtools.github.io/devtools-protocol/tot/Performance#method-enable
	// https://chromedevtools.github.io/devtools-protocol/tot/Performance#method-getMetrics
	public void getPerformanceMetricsTest() {
		driver.get("about:blank");
		String command = "Performance.setTimeDomain";
		params = new HashMap<>();
		params.put("timeDomain", "timeTicks");
		try {
			driver.executeCdpCommand(command, params);
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
		try {
			// Act
			command = "Performance.enable";
			driver.executeCdpCommand(command, new HashMap<>());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}

		driver.get("https://www.wikipedia.org");
		try {
			// Act
			command = "Performance.getMetrics";
			result = driver.executeCdpCommand(command, new HashMap<>());
			// Assert
			assertThat(result, notNullValue());
			assertThat(result, hasKey("metrics"));
			ArrayList<Object> metrics = (ArrayList<Object>) result.get("metrics");
			assertThat(metrics, notNullValue());
			assertThat(metrics.size(), greaterThan(0));
			Object metricEntry = metrics.get(0);
			assertThat(metricEntry, notNullValue());
			err.println("Metric Data: " + metricEntry);
			Map<String, Integer> metricData = (Map<String, Integer>) metricEntry;
			assertThat(metricData, hasKey("name"));
			assertThat(metricData, hasKey("value"));
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
		try {
			// Act
			command = "Performance.disable";
			driver.executeCdpCommand(command, new HashMap<>());
		} catch (org.openqa.selenium.WebDriverException e) {
			err.println("Exception (ignored): " + e.toString());
		}
	}


}
