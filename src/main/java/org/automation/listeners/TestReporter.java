package org.automation.listeners;

import static com.aventstack.extentreports.MediaEntityBuilder.createScreenCaptureFromBase64String;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createDirectory;
import static java.nio.file.Files.notExists;
import static java.nio.file.Paths.get;
import static java.time.Instant.ofEpochMilli;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.automation.logger.Log.error;
import static org.testng.Reporter.getOutput;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.testng.IReporter;
import org.testng.IResultMap;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityModelProvider;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

/**
 * Listener class to create report on the completion of the execution.
 * 
 * @author Sujay Sawant
 * @version 1.0.0
 * @since 06/11/2020
 *
 */
public class TestReporter implements IReporter {

	private ExtentReports extent;

	private final Optional<String> browser = Optional.ofNullable(getProperty("browser"));
	private final String operatingSystem = getProperty("os.name").toUpperCase();
	private final String systemArchitecture = getProperty("os.arch").toUpperCase();

	@Override
	public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
		init(xmlSuites);
		suites.forEach(suite -> {
			Map<String, ISuiteResult> results = suite.getResults();
			results.values().forEach(result -> {
				ITestContext context = result.getTestContext();
				buildTestNodes(context.getFailedTests(), Status.FAIL);
				buildTestNodes(context.getSkippedTests(), Status.SKIP);
				buildTestNodes(context.getPassedTests(), Status.PASS);
			});
		});
		getOutput().forEach(extent::setTestRunnerOutput);
		extent.flush();
	}

	/**
	 * To initialize all the report's elements.
	 * 
	 * @param xmlSuites
	 *            all the executed suites
	 */
	private void init(List<XmlSuite> xmlSuites) {
		String suiteName = xmlSuites.get(0).getName();
		Path report = get(getProperty("user.dir"), "target", "extent-reports",
				"Extent Report_" + currentTimeMillis() + "_" + suiteName + ".html");
		if (notExists(report.getParent(), LinkOption.NOFOLLOW_LINKS))
			try {
				createDirectory(report.getParent());
			} catch (IOException e) {
				System.err.println("Unable to create path: " + report.getParent());
				e.printStackTrace();
			}
		ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter(report.toString());
		htmlReporter.config().setDocumentTitle("ExtentReports: " + suiteName);
		htmlReporter.config().setReportName(suiteName);
		htmlReporter.config().setTheme(Theme.STANDARD);
		htmlReporter.config().setEncoding("UTF-8");
		extent = new ExtentReports();
		extent.attachReporter(htmlReporter);
		extent.setReportUsesManualConfiguration(true);
		extent.setSystemInfo("Operating System", operatingSystem);
		extent.setSystemInfo("System Architecture", systemArchitecture);
		extent.setSystemInfo("Browser Selection", browser.orElse("chrome").toUpperCase());
	}

	/**
	 * Build the report for all the tests executed.
	 * 
	 * @param tests
	 *            all the tests executed
	 * @param status
	 *            status of the test executed
	 */
	private void buildTestNodes(IResultMap tests, Status status) {
		SortedSet<ITestResult> results = new TreeSet<ITestResult>();
		tests.getAllResults().forEach(results::add);
		if (tests.size() > 0) {
			results.forEach(result -> {
				ExtentTest test = extent.createTest(result.getTestContext().getCurrentXmlTest().getName() + " - " + result.getName());
				test.assignCategory(result.getMethod().getRealClass().getSimpleName());
				Throwable throwable = result.getThrowable();
				Object[] parameters = result.getParameters();
				if (parameters.length > 0) {
					String params = stream(parameters).map(Object::toString).collect(joining(", "));
					test.info(params);
				}
				getOutput(result).forEach(test::info);
				test.log(status, "Test [" + result.getName() + "] " + status.toString() + "ed");
				if (throwable != null) {
					try {
						String base64String = result.getAttribute("failureScreenshot").toString();
						MediaEntityModelProvider provider = createScreenCaptureFromBase64String(base64String).build();
						test.log(status, throwable, provider);
					} catch (IOException e) {
						error("Unable to add screenshot to extent report", e);
					}
				}
				test.getModel().setStartTime(getTime(result.getStartMillis()));
				test.getModel().setEndTime(getTime(result.getEndMillis()));
			});
		}
	}

	/**
	 * Get the date and time from the epoch milliseconds provided.
	 * 
	 * @param millis
	 *            epoch milliseconds
	 * @return the data and time
	 */
	private Date getTime(long millis) {
		return Date.from(ofEpochMilli(millis));
	}

}
