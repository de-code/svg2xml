package org.xmlcml.svg2xml.pdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.html.HtmlMenuSystem;
import org.xmlcml.svg2xml.container.AbstractContainer.ContainerType;
import org.xmlcml.svg2xml.page.PageAnalyzer;
import org.xmlcml.svg2xml.page.PageIO;
import org.xmlcml.svg2xml.util.NameComparator;
import org.xmlcml.svg2xml.util.SVG2XMLConstantsX;

/** 
 * Class to deal with IO from PDFAnalyzer
 * 
 * @author pm286
 */
public class PDFAnalyzerIO {
	
	private static final Logger LOG = Logger.getLogger(PDFAnalyzerIO.class);
	static {LOG.setLevel(Level.DEBUG);}

	public static final File TARGET_DIR = new File("target");
	public static final File OUTPUT_DIR = new File(TARGET_DIR, "output");
	public static final File SVG_DIR = new File(TARGET_DIR, "svg");
	public static final String HTTP = "http";
	public static final String DOT_PDF = ".pdf";
	final static PrintStream SYSOUT = System.out;

	private File inFile;
	private String inputName;
	/** 
	 * bar/foo.pdf =>> foo as fileRoot
	 */
	String fileRoot;
	/** 
	 * Top of where rawSVGFiles are kept, e.g. target/svg
	 */
	private File svgTopDir = SVG_DIR;
	/** 
	 * Directory where raw SVG are kept, e.g. target/svg/foo/
	 */
	private File rawSvgDirectory;
	/** 
	 * Top of where outputFiles are kept , e.g. target/output
	 */
	private File outputTopDir = OUTPUT_DIR;
	/** 
	 * Directory where output are kept, e.g. target/svg/foo/
	 */
	File outputDocumentDir;
	/** 
	 * Directory where html are kept, e.g. target/svg/foo/html/ or target/svg/foo/
	 */
	private File htmlDir;

//	private File finalSvgDirectory;

	private PDFAnalyzer pdfAnalyzer;

	public PDFAnalyzerIO(PDFAnalyzer pdfAnalyzer) {
		this.pdfAnalyzer = pdfAnalyzer;
	}
	
	public void setSvgTopDir(File svgDir) {
		this.svgTopDir = svgDir;
	}
	
	public void setOutputTopDir(File outDir) {
		this.outputTopDir = outDir;
	}
	
	public File getOutputTopDir() {
		return outputTopDir;
	}
	
	public void setFileRoot(String fileRoot) {
		this.fileRoot = fileRoot;
	}
	
	void setInputName(String name) {
		this.inputName = name;
	}
	
	public File getInFile() {
		return inFile;
	}
	
	public File getRawSVGDirectory() {
		return rawSvgDirectory;
	}
	
	public void setRawSVGDirectory(File svgDir) {
		rawSvgDirectory = svgDir;
	}
	
//	public void setFinalSVGDirectory(File svgDir) {
//		finalSvgDirectory = svgDir;
//	}
//	
//	public File getFinalSVGDirectory() {
//		return finalSvgDirectory;
//	}
	
	public String getInputName() {
		return inputName;
	}

	public void setPDFURL(String name) {
		setInputName(name);
		setFileRoot(name.substring(0, name.length() - SVG2XMLConstantsX.DOT_PDF.length()));
		if (fileRoot.startsWith(HTTP)) {
			fileRoot = fileRoot.substring(fileRoot.indexOf("//")+2);
			fileRoot = fileRoot.substring(fileRoot.indexOf("/")+1);
			LOG.debug("fileroot "+fileRoot);
		}
		rawSvgDirectory = new File(svgTopDir, fileRoot);
		LOG.debug("raw svgDocument "+rawSvgDirectory);
		outputDocumentDir = new File(outputTopDir, fileRoot);
		outputDocumentDir.mkdirs();
		fileRoot = "";
		LOG.debug("outputDocument "+outputDocumentDir);
	}
	
	void setUpPDF(File inFile) {
		this.inFile = inFile;
		inputName = inFile.getName();
		fileRoot = inputName.substring(0, inputName.length() - SVG2XMLConstantsX.DOT_PDF.length());
		rawSvgDirectory = new File(svgTopDir, fileRoot);
		outputDocumentDir = new File(outputTopDir, fileRoot);
	}

	public String createHttpInputName(String inputName) {
		String inputName1 = inputName.substring(inputName.lastIndexOf("/") + 1);
		if (inputName1.toLowerCase().endsWith(DOT_PDF)) {
			inputName = inputName1.substring(0, inputName1.length()-DOT_PDF.length());
		}
		LOG.debug("filename: "+inputName);
		return inputName;
	}
	
	void outputFiles() {
		File htmlDir = (new File(outputTopDir, fileRoot));
		copyOriginalPDF(inFile, htmlDir);
		createHtmlMenuSystem(htmlDir);
	}
	

	void copyOriginalPDF(File inFile, File htmlDir) {
		try {
			htmlDir.mkdirs();
			IOUtils.copy(new FileInputStream(inFile), new FileOutputStream(new File(htmlDir, "00_"+inputName)));
		} catch (Exception e1) {
			throw new RuntimeException(e1);
		}
	}

	 void createHtmlMenuSystem(File dir) {
		HtmlMenuSystem menuSystem = new HtmlMenuSystem();
		menuSystem.setOutdir(dir.toString());
		File[] filesh = dir.listFiles();
		Arrays.sort(filesh, new NameComparator());
		for (File filex : filesh) {
			menuSystem.addHRef(filex.toString());
		}
		try {
			menuSystem.outputMenuAndBottomAndIndexFrame();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	 
	public void createHTMLDir() {
		 htmlDir = new File(outputTopDir, fileRoot);
	}
	
	public File getExistingOutputDocumentDir() {
		outputDocumentDir.mkdirs();
		return outputDocumentDir;
	}

	List<File> collectRawSVGFiles() {
		File[] rawSvgPageFiles = rawSvgDirectory.listFiles();
		List<File> files = new ArrayList<File>();
		LOG.trace("analyzing Files in: "+rawSvgDirectory);
		if (rawSvgPageFiles == null) {
			throw new RuntimeException("No files in "+rawSvgDirectory);
		} else {
			// sort by integer page number "page12.svg"
			for (int i = 0; i < rawSvgPageFiles.length; i++) {
				for (int j = 0; j < rawSvgPageFiles.length; j++) {
					File filej = rawSvgPageFiles[j];
					if (filej.getName().contains("page"+(i + 1)+".svg")) {
						files.add(filej);
					}
				}
			}
		}
		return files;
	}

	public File getRawSVGPageDirectory() {
		return rawSvgDirectory;
	}

	public void setRawSvgDirectory(File rawSvgDirectory) {
		this.rawSvgDirectory = rawSvgDirectory;
	}

	public void outputFiles(PDFAnalyzerOptions options) {
		for (PageAnalyzer pageAnalyzer : pdfAnalyzer.getPageAnalyzerList()) {
			if (options.summarize) {
				pageAnalyzer.summaryContainers();
			}
			if (options.outputChunks) {
				pageAnalyzer.outputChunks();
			}
			if (options.outputHtmlChunks ||
					options.outputRawFigureHtml ||
					options.outputRawTableHtml) {
				pageAnalyzer.outputHtmlComponents();
			}
			if (options.outputImages) {
				pageAnalyzer.outputImages();
			}
			if (options.outputRunningText) {
				pageAnalyzer.outputHtmlRunningText();
			}
			if (options.outputAnnotatedSvgPages) {
				pageAnalyzer.writeRawSVGPageToRawDirectory();
//				pageAnalyzer.writeFinalSVGPageToFinalDirectory();
			}
		}
		if (options.outputRunningText) {
			pdfAnalyzer.createRunningHtml();
			outputDocumentDir = PageIO.createfinalSVGDocumentDirectory(rawSvgDirectory);
			PageIO.outputFile(pdfAnalyzer.getRunningTextHtml(), PageIO.createHtmlFile(outputDocumentDir, ContainerType.TEXT, "0"));
		}
	}

	public boolean skipOutput(PDFAnalyzerOptions options) {
		boolean skip = false;
		if (options.skipOutput) {
			File outputDirectory = outputDocumentDir;
			skip = outputDirectory.exists();
		}
		return skip;
	}


}
