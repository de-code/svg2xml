package org.xmlcml.svg2xml.cmucl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xmlcml.euclid.Real2;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.graphics.svg.SVGDefs;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGPath;
import org.xmlcml.graphics.svg.SVGRect;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGShape;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.graphics.svg.linestuff.Path2ShapeConverter;
import org.xmlcml.html.HtmlDiv;
import org.xmlcml.html.HtmlHtml;
import org.xmlcml.html.HtmlImg;
import org.xmlcml.html.HtmlTable;
import org.xmlcml.html.HtmlTd;
import org.xmlcml.html.HtmlTr;
import org.xmlcml.pdf2svg.PDF2SVGConverter;
import org.xmlcml.svg2xml.Fixtures;
import org.xmlcml.svg2xml.table.TableContentCreator;
import org.xmlcml.xml.XMLUtil;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import nu.xom.Attribute;

@Ignore // production
public class CMUCLTest {

	public static final Logger LOG = Logger.getLogger(CMUCLTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}
	
	private final static File CMUCL0 = new File(Fixtures.TABLE_DIR, "cmucl0/");
	private static final File CMUCL_OUT_DIR = new File("target/table/cmucl0");
	private static final String BORDER = "border";
	private static final File CM_UCL_DIR = new File("../../cm-ucl");
	public final static String TABLE = "table";
	private static final String VERTICAL_ALIGN_TOP = "vertical-align:top";

	@Test
	public void testBMC() {
		Assert.assertTrue("tables should exist", Fixtures.TABLE_DIR.exists());
		Assert.assertTrue("CMUCL0 should exist", CMUCL0.exists());
		String root = "BMC_Medicine";
		extractTables(root);
	}
	
	@Test
	public void testBMCHTML() {
		String root = "BMC_Medicine";
		createHTML(root);
	}
	
	@Test
	public void testBMCMarkup() {
		String root = "BMC_Medicine";
		markupAndOutputTables(root);
	}
	
	@Test
	public void testInformaRotated() {
		String root = "Informa_ExpOpinInvestDrugsRot";
		markupAndOutputTables(root);
	}
	
	@Test
	/** need to hack the boxes */
	public void testLancet() {
		String root = "TheLancet_1";
		markupAndOutputTables(root);
	}
	
	@Test
	public void testNature() {
		String root = "Nature_EurJClinNutrit";
		markupAndOutputTables(root);
	}
	
	@Test
	public void Nature_SciRep_1() {
		String root = "Nature_SciRep_1";
		markupAndOutputTables(root);
	}
	
	
	@Test
	public void testAllTables() {
		File[] dirs = CMUCL0.listFiles();
		for (File dir : dirs) {
			extractTables(dir.getName());
		}
	}

	@Test
	public void testAllMarkup() {
		File[] dirs = CMUCL0.listFiles();
		for (File dir : dirs) {
			markupAndOutputTables(dir.getName());
		}
	}

	// ======================
	
	public static void createHTML(String root) {
		File inDir = new File(CMUCL0, root+"/");
		File outDir = new File(CMUCL_OUT_DIR, root+"/");
		List<File> tableFiles = new ArrayList<File>
			(FileUtils.listFiles(inDir, new WildcardFileFilter("table*.svg") , TrueFileFilter.INSTANCE));
		for (File tableFile : tableFiles) {
			TableContentCreator tableContentCreator = new TableContentCreator();
			HtmlHtml html = tableContentCreator.createHTMLFromSVG(tableFile);
			File outfile = new File(outDir, tableFile.getName()+".html");
			LOG.trace("writing: "+outfile);
			try {
				XMLUtil.debug(html, outfile, 1);
			} catch (IOException e) {
				LOG.error("Cannot write file: "+outfile+" ("+e+")");
			}
		}
	}

	private static void markupAndOutputTables(String root/*, String filename, int nHeaderCols, int nBodyCols*/) {
		File inDir = new File(CMUCL0, root+"/");
		File outDir = new File(CMUCL_OUT_DIR, root+"/");
		List<File> inputFiles = new ArrayList<File>
		(FileUtils.listFiles(inDir, new WildcardFileFilter("table*.svg") , TrueFileFilter.INSTANCE));
		for (File inputFile : inputFiles) {
			TableContentCreator tableContentCreator = new TableContentCreator(); 
			tableContentCreator.markupAndOutputTable(inputFile, outDir);
		}
	}

	private void extractTables(String root) {
		File outDir = CMUCL_OUT_DIR;
		outDir.mkdirs();
		File bmcDir = new File(CMUCL0, root);
		List<File> tableFiles = new ArrayList<File>
			(FileUtils.listFiles(bmcDir, new WildcardFileFilter("table*.svg") , TrueFileFilter.INSTANCE));
		for (File file : tableFiles) {
			String fileroot = file.getName().replace(".svg", "");
			SVGSVG svg = null;
			try {
				svg = (SVGSVG) SVGUtil.parseToSVGElement(new FileInputStream(file));
			} catch (Exception e) {
				LOG.error("Cannot find/read: "+file);
				continue;
			}
			SVGDefs.removeDefs(svg);
			List<SVGPath> paths = SVGPath.extractSelfAndDescendantPaths(svg);
			List<SVGShape> shapes = new Path2ShapeConverter().convertPathsToShapes(paths);
			SVGG g = new SVGG();
			Multiset<String> classSet = HashMultiset.create();
			for (SVGShape shape : shapes) {
				g.appendChild(shape);
				classSet.add(shape.getClass().getSimpleName());
			}
			SVGSVG.wrapAndWriteAsSVG(g, new File(outDir, root+"/"+fileroot+".shapes.svg"));
		}
	}

	private void addImageToRow(double TD_WIDTH, File png, int imgWidth, int imgHeight, HtmlTr row) {
		HtmlTd imageCell = new HtmlTd();
		row.appendChild(imageCell);
		imageCell.setStyle(VERTICAL_ALIGN_TOP);
		HtmlImg img = new HtmlImg();
		imageCell.appendChild(img);
		img.setSrc("../image/"+png.getName());
		img.setWidth(TD_WIDTH);
		img.setHeight(TD_WIDTH * (double) imgHeight / (double) imgWidth);
		img.setStyle(VERTICAL_ALIGN_TOP);
	}

	// ===================================
	
	private void addSvgToRow(double TD_WIDTH, File svgAnnotFile1, HtmlTr row) {
		HtmlTd svgCell = new HtmlTd();
		row.appendChild(svgCell);
		
		svgCell.setWidth(TD_WIDTH);
		HtmlDiv div = new HtmlDiv();
		svgCell.appendChild(div);
		svgCell.setStyle(VERTICAL_ALIGN_TOP);
		SVGElement svg = null;
		try {
			svg = SVGUtil.parseToSVGElement(new FileInputStream(svgAnnotFile1));
		} catch (FileNotFoundException fnfe) {
			LOG.debug("no SVG file:"+svgAnnotFile1);
		}
		if (svg != null) {
			SVGG g = new SVGG();
			XMLUtil.transferChildren(svg, g);
			svg.appendChild(g);
			Real2Range bbox = svg.getBoundingBox();
			double scale = TD_WIDTH / bbox.getXRange().getRange();
			Real2 corner = bbox.getCorners()[0];
			String transform = "scale("+scale+") ";
			transform += " translate("+(-1.0*corner.getX())+","+(-1.0*corner.getY())+")";
			g.addAttribute(new Attribute("transform", transform));
			SVGRect bboxRect = SVGRect.createFromReal2Range(bbox);
			bboxRect.setStroke("black");
			bboxRect.setStrokeWidth(2.);
			bboxRect.setFill("none");
			g.appendChild(bboxRect);
			div.appendChild(svg);
		}
	}

	@Test
	public void testCreateDoubleTableHTML() throws IOException {
		double TD_WIDTH = 500.0;
		File pmrDir = new File(CM_UCL_DIR, "corpus-oa-pmr");
		File chDir = new File(CM_UCL_DIR, "corpus-oa");
		Assert.assertTrue(pmrDir.getAbsoluteFile().toString()+" exists", pmrDir.exists());
		File[] ctrees = pmrDir.listFiles();
		for (File ctree : ctrees) {
			String root = ctree.getName();
			File chTree = new File(chDir, root+"/");
			File pmrTree = new File(pmrDir, root+"/");
			File pmrImageDir = new File(pmrTree, "image/");
			File pdfTableDir = new File(pmrTree, "pdftable/");
			List<File> pngs = new ArrayList<File>(FileUtils.listFiles(chTree, new String[]{"png"}, false));
			for (File png : pngs) {
				if (png.getName().startsWith(TABLE)) {
	
					String pngName = png.getName();
					String pngRoot = pngName.substring(0, pngName.length() - ".png".length());
					File pngFile = new File(pmrImageDir, pngName);
					BufferedImage image = ImageIO.read(pngFile);
					int imgWidth = image.getWidth();
					int imgHeight = image.getHeight();
					String pngRootSuffix = pngRoot.substring(TABLE.length());
					File svgAnnotFile1 = new File(pdfTableDir, pngRoot+TableContentCreator.DOT_ANNOT_SVG);
					
					HtmlHtml html = new HtmlHtml();
					HtmlTable table = new HtmlTable();
					table.setAttribute(BORDER, "1");
					html.appendChild(table);
					HtmlTr row = new HtmlTr();
					table.appendChild(row);
					
					addImageToRow(TD_WIDTH, png, imgWidth, imgHeight, row);
					addSvgToRow(TD_WIDTH, svgAnnotFile1, row);
					
					XMLUtil.debug(html, new File(pdfTableDir, "doubleTable"+pngRootSuffix+".html"), 1);
				}
			}
		}
	}

	/** production
	 * 
	 */
	@Test
	@Ignore
	public void testCreateSVGFromPDF() throws IOException {
		File pdfOrigDir = new File(CM_UCL_DIR, "corpus-oa");
		File pmrDir = new File(CM_UCL_DIR, "corpus-oa-pmr");
		pmrDir.mkdirs();
		File[] pdfDirs = pdfOrigDir.listFiles();
		for (File pdfDir : pdfDirs) {
			File pmrPdfDir = new File(pmrDir, pdfDir.getName());
			pmrPdfDir.mkdirs();
			File pdfFile = new File(pdfDir, "fulltext.pdf");
			File svgDir = new File(pmrPdfDir, "svg/");
			svgDir.mkdirs();
			new PDF2SVGConverter().run(
					"-logger", "-infofiles", "-logglyphs", "-outdir", svgDir.toString(), pdfFile.toString());
			File pngDir = new File(pmrPdfDir, "png/");
			List<File> pngs = new ArrayList<File>(FileUtils.listFiles(svgDir, new String[] {"png"}, false));
			for (File png : pngs) {
				if (FileUtils.sizeOf(png) == 0) {
					png.delete();
				} else {
					try {
						FileUtils.moveToDirectory(png, pngDir, true);
					} catch (Exception e) {
						LOG.error("cannot move file: "+e);
					}
				}
			}
		}
	}

	@Test
	/** copies key directories for display and creates CSV file.
	 * 
	 */
	public void testCreateTableShow() throws IOException {
		String root = CM_UCL_DIR.toString();
		String srcRoot = root+"/corpus-oa-pmr/";
	    Path pmrFolder = Paths.get(srcRoot);
	    String showRoot = root+"/corpus-oa-pmr-show/";
	    Path showPath = Paths.get(showRoot);
	    StringBuilder csvBuilder = new StringBuilder("CTree,table,\n");
	    Files.walkFileTree(pmrFolder, new CopyFileVisitor(csvBuilder, srcRoot, showPath));
	    FileUtils.write(new File(new File(root), "tables.csv"), csvBuilder.toString());
	}

	@Test
	/** iterates over CProject and marksup tables
	 * adds visible boxes for detected regions and adds titles
	 * 
	 */
	@Ignore // production
	public void testMarkupTables() {
		File pmrDir = new File(CM_UCL_DIR, "corpus-oa-pmr");
		File chDir = new File(CM_UCL_DIR, "corpus-oa");
		File[] ctrees = pmrDir.listFiles();
		for (File ctree : ctrees) {
			LOG.trace("*************"+ctree+"**************");
			File svgDir = new File(ctree, "svg/");
			List<File> svgFiles = new ArrayList<File>(FileUtils.listFiles(svgDir, new String[]{"svg"}, false));
			File tableDir = new File(ctree, "pdftable/");
			tableDir.mkdirs();
			for (File svgFile : svgFiles) {
				if (svgFile.getName().startsWith("table")) {
					LOG.trace("============="+svgFile+"=============");
					TableContentCreator tableContentCreator = new TableContentCreator(); 
					tableContentCreator.markupAndOutputTable(svgFile, tableDir);
					String root = FilenameUtils.getBaseName(svgFile.toString());
					File annotSvg = new File(tableDir, root+TableContentCreator.DOT_ANNOT_SVG);
					try {
						tableContentCreator.createHTML(annotSvg, tableDir);
					} catch (IOException e) {
						LOG.debug("Cannot write html: "+e);
					}
				}
			}
		}
	}
}

class CopyFileVisitor extends SimpleFileVisitor<Path> {
	private static final Logger LOG = Logger.getLogger(CopyFileVisitor.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

    private final Path targetPath;
    private Path sourcePath = null;
	private String srcRoot;
	private StringBuilder csvBuilder;
    
    public CopyFileVisitor(StringBuilder csvBuilder, String srcRoot, Path targetPath) {
        this.targetPath = targetPath;
        this.srcRoot = srcRoot;
        this.csvBuilder = csvBuilder;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir,
    		final BasicFileAttributes attrs) throws IOException {
    	File dirFile = dir.toFile();
    	String name = dirFile.getName();
        if (sourcePath == null) {
            sourcePath = dir;
        } else if (name.equals("png") || name.equals("svg")) {
        	LOG.trace("skipped dir "+dir);
            return FileVisitResult.SKIP_SUBTREE;
        } else if (name.equals("image") || name.equals("pdftable")) {
        	Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
        } else {
        	String filename = dir.toString().substring(srcRoot.length());
        	csvBuilder.append(String.valueOf(filename)+","+
        			","+"\n");
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(final Path file,
	    final BasicFileAttributes attrs) throws IOException {
    	File fileFile = file.toFile();
		Path targetFile = targetPath.resolve(sourcePath.relativize(file));
		boolean copy = true;
		if (fileFile.getName().endsWith(TableContentCreator.DOT_ANNOT_SVG)) {
			// skip SVG
			copy = false;
			LOG.trace("skipped "+file);
		} else if (!targetFile.toFile().exists()) {
			Files.copy(file, targetFile);
		}
		if (copy) {
        	if (!file.toFile().getName().endsWith(".png")) {
        		csvBuilder.append(String.valueOf(
        			file.toFile().getParentFile()).substring(srcRoot.length())+","+
        			file.toFile().getName()+","+"\n");
        	}
		}
	    return FileVisitResult.CONTINUE;
    }
}
