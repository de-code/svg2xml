package org.xmlcml.svg2xml.old;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import nu.xom.Builder;
import nu.xom.Element;
import nu.xom.Nodes;

import org.apache.log4j.Logger;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.html.HtmlDiv;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.html.HtmlLi;
import org.xmlcml.html.HtmlUl;
import org.xmlcml.svg2xml.analyzer.ChunkId;
import org.xmlcml.svg2xml.analyzer.PDFAnalyzer;
import org.xmlcml.svg2xml.analyzer.PDFAnalyzerIO;
import org.xmlcml.svg2xml.indexer.FigureIndexer;
import org.xmlcml.svg2xml.indexer.TableIndexer;
import org.xmlcml.svg2xml.page.PageChunkAnalyzer;
import org.xmlcml.svg2xml.page.FigureAnalyzer;
import org.xmlcml.svg2xml.page.ImageAnalyzer;
import org.xmlcml.svg2xml.page.MixedAnalyzer;
import org.xmlcml.svg2xml.page.PathAnalyzer;
import org.xmlcml.svg2xml.page.TableAnalyzer;
import org.xmlcml.svg2xml.page.TextAnalyzer;
import org.xmlcml.svg2xml.text.TextStructurer;

public class HtmlEditor {

	private final static Logger LOG = Logger.getLogger(HtmlEditor.class);
	
	private static final String TEXT = "TEXT";

	private List<HtmlAnalyzer> htmlAnalyzerListSortedByChunkId;
	private PDFAnalyzer pdfAnalyzer;
	private Map<ChunkId, HtmlAnalyzer> htmlAnalyzerByIdMap;
	private List<HtmlAnalyzer> figureHtmlAnalyzerList;
	private List<HtmlAnalyzer> tableHtmlAnalyzerList;
	private List<HtmlAnalyzer> mergedHtmlAnalyzerList;
	private HtmlAnalyzer textDivAnalyzer;

	private PDFAnalyzerIO pdfIo;
	
	public HtmlEditor(PDFAnalyzer pdfAnalyzer) {
		this.pdfAnalyzer = pdfAnalyzer;
		this.pdfIo = pdfAnalyzer.getPDFIO();
	}

    public void accept(HtmlVisitor visitor) {
        visitor.visit(this);
    }
    
	public /*List<HtmlAnalyzer>*/ void categorizeHtml() {
		LOG.debug("Merging HTML");
		HtmlDiv textDiv = new HtmlDiv();
		createTextDivAnalyzer(textDiv);
		HtmlAnalyzer lastAnalyzer = null;
		figureHtmlAnalyzerList = new ArrayList<HtmlAnalyzer>();
		tableHtmlAnalyzerList = new ArrayList<HtmlAnalyzer>();
		mergedHtmlAnalyzerList = new ArrayList<HtmlAnalyzer>();
		for (HtmlAnalyzer htmlAnalyzer : htmlAnalyzerListSortedByChunkId) {
			String id = htmlAnalyzer.getId();
			String classAttribute = htmlAnalyzer.getClassAttribute();
			String classAttribute0 = (classAttribute == null) ? null : classAttribute.split("\\s+")[0];
			LOG.trace("Class "+classAttribute+" "+classAttribute0+" "+htmlAnalyzer.getAnalyzer());
			if (classAttribute == null) { 
				mergedHtmlAnalyzerList.add(htmlAnalyzer);
				LOG.trace("merging "+id);
				merge(lastAnalyzer, htmlAnalyzer, textDiv);
				lastAnalyzer = htmlAnalyzer;
			} else if (HtmlAnalyzer.OMIT.equals(classAttribute)) {
				// already designated as OMIT
				LOG.trace("OMITTED "+id);
			} else if (FigureIndexer.TITLE.equals(classAttribute0)) {
				htmlAnalyzer.setChunkType(classAttribute0);
				figureHtmlAnalyzerList.add(htmlAnalyzer);
				LOG.trace(classAttribute+" = "+id);
			} else if (TableIndexer.TITLE.equals(classAttribute0)) {
				htmlAnalyzer.setId(id);
				htmlAnalyzer.setChunkType(classAttribute0);
				tableHtmlAnalyzerList.add(htmlAnalyzer);
				LOG.trace(classAttribute+" = "+id);
			} else {
				LOG.trace("untreated CLASS "+classAttribute);
			}
			
		}
//		return htmlAnalyzerList;
	}

	private void createTextDivAnalyzer(HtmlDiv textDiv) {
		textDivAnalyzer = new HtmlAnalyzer(textDiv, this);
		textDivAnalyzer.setClassAttribute(TEXT);
		textDivAnalyzer.setChunkType(TEXT);
		textDivAnalyzer.setSerial(1);
		textDivAnalyzer.setId("t.1.0");
	}

	private void merge(HtmlAnalyzer lastAnalyzer, HtmlAnalyzer htmlAnalyzer, HtmlDiv topDiv) {
		TextStructurer lastTextContainer = (lastAnalyzer == null) ? 
				null : lastAnalyzer.getTextStructurer();
		TextStructurer textStructurer = htmlAnalyzer.getTextStructurer();
		boolean merged = false;
		if (lastTextContainer != null && textStructurer != null) {
			if (lastTextContainer.endsWithRaggedLine() && textStructurer.startsWithRaggedLine()) {
				merged = htmlAnalyzer.mergeLinesWithPrevious(lastAnalyzer, topDiv);
			}
		} 
		if (!merged) {
			htmlAnalyzer.addIdSeparator(topDiv);
			Element copyElement = null;
			try {
				copyElement = HtmlElement.create((Element)htmlAnalyzer.createHtmlElement());
			} catch (Exception e) {
				LOG.debug("cannot create HTML: "+e);
				// might be SVG
				copyElement = (Element) htmlAnalyzer.createHtmlElement().copy();
			}
			topDiv.appendChild(copyElement);
		}
//		htmlAnalyzer.removeSVGNodes();
	}

	public void removeDuplicates() {
		getHtmlAnalyzerListSortedByChunkId();
		for (HtmlAnalyzer htmlAnalyzer : htmlAnalyzerListSortedByChunkId) {
			ChunkId id = new ChunkId(htmlAnalyzer.getId());
			if (pdfAnalyzer.getIndex().getUsedIdSet().contains(id)) {
				String classAttribute = htmlAnalyzer.getClassAttribute();
				LOG.trace(id+" "+classAttribute);
				if (classAttribute == null) {
					LOG.trace("skip duplicate: "+id+" "+classAttribute);
					htmlAnalyzer.setClassAttribute(HtmlAnalyzer.OMIT);
				}
			}
		}
	}

	public void outputHtmlElements() {
		LOG.debug("figures HTML");
		for (HtmlAnalyzer htmlAnalyzer : figureHtmlAnalyzerList) {
			htmlAnalyzer.outputElementAsHtml(pdfIo.getExistingOutputDocumentDir());
		}
		LOG.debug("tables HTML");
		for (HtmlAnalyzer htmlAnalyzer : tableHtmlAnalyzerList) {
			htmlAnalyzer.outputElementAsHtml(pdfIo.getExistingOutputDocumentDir());
		}
		LOG.debug("merged HTML");
		for (HtmlAnalyzer htmlAnalyzer : mergedHtmlAnalyzerList) {
			htmlAnalyzer.outputElementAsHtml(pdfIo.getExistingOutputDocumentDir());
		}
		LOG.debug("merged TEXT");
		textDivAnalyzer.outputElementAsHtml(pdfIo.getExistingOutputDocumentDir());
		
	}

	public void mergeCaptions() {
//		for (HtmlAnalyzer htmlAnalyzer : htmlAnalyzerListSortedByChunkId) {
//			String chunkType = htmlAnalyzer.addTypeSerialAttributes();
//			if (FigureSemanticAnalyzer.TITLE.equals(chunkType)) {
//				LOG.trace("FIG FIX");
//				if (htmlAnalyzer.containsDivImage()) {
//					LOG.trace("***********IMG************");
//				} else {
//					HtmlAnalyzer previousAnalyzer = htmlAnalyzer.getPreviousHtmlAnalyzer(htmlAnalyzerByIdMap);
//					htmlAnalyzer.addImageDivTo(previousAnalyzer);
//				}
//			}
//		}
	}

	/** create list of entities
	 * 
	 * @param htmlFiles
	 * @param xpath
	 * @param htmlPattern
	 * @return
	 */
	public HtmlUl searchHtml(List<File> htmlFiles, String xpath, Pattern htmlPattern) {
		Set<String> entitySet = new HashSet<String>();
		HtmlUl ul = null;
		for (File file : htmlFiles) {
			Element html = null;
			try {
				html = new Builder().build(file).getRootElement();
			} catch (Exception e) {
				LOG.error("Failed on html File: "+file);
			}
			if (html != null) {
				ul = new HtmlUl();
				searchHtml(xpath, htmlPattern, ul, entitySet, html);
			}
		}
		return ul;
	}

	private void searchHtml(String xpath, Pattern htmlPattern, HtmlUl ul,
			Set<String> entitySet, Element html) {
		Nodes nodes = html.query(xpath);
		for (int i = 0; i < nodes.size(); i++) {
			String value = nodes.get(i).getValue();
			if (htmlPattern.matcher(value).matches()) {	
				if (!entitySet.contains(value)) {
					LOG.trace(value);
					HtmlLi li = new HtmlLi();
					ul.appendChild(li);
					li.setValue(value);
					entitySet.add(value);
				}
			}
		}
	}

	public List<HtmlAnalyzer> getHtmlAnalyzerListSortedByChunkId() {
		if (htmlAnalyzerListSortedByChunkId == null) {
			List<ChunkId> chunkIdList = Arrays.asList(htmlAnalyzerByIdMap.keySet().toArray(new ChunkId[0]));
			Collections.sort(chunkIdList);
			htmlAnalyzerListSortedByChunkId = new ArrayList<HtmlAnalyzer>();
			for (ChunkId id : chunkIdList) {
				HtmlAnalyzer htmlAnalyzer = htmlAnalyzerByIdMap.get(id);
				htmlAnalyzer.setId(id.toString());
				htmlAnalyzerListSortedByChunkId.add(htmlAnalyzer);
			}
		}
		return htmlAnalyzerListSortedByChunkId;
	}

	Map<ChunkId, HtmlAnalyzer> getHtmlAnalyzerByIdMap() {
		ensureHtmlAnalyzerByIdMap();
		return htmlAnalyzerByIdMap;
	}

//	public void setHtmlElementByIdMap(Map<ChunkId, HtmlElement> htmlElementByIdMap) {
//		this.htmlElementByIdMap = htmlElementByIdMap;
//	}

	public void createLinkedElementList() {
		getHtmlAnalyzerListSortedByChunkId();
		HtmlAnalyzer lastAnalyzer = null;;
		for (HtmlAnalyzer htmlAnalyzer : htmlAnalyzerListSortedByChunkId) {
			htmlAnalyzer.addLinks(lastAnalyzer);
			lastAnalyzer = htmlAnalyzer;
		}
	}

	String getValueFromHtml(ChunkId id) {
		HtmlAnalyzer htmlAnalyzer = getHtmlAnalyzerByIdMap().get(id);
		return htmlAnalyzer.getValue();
	}

	protected HtmlAnalyzer getHtmlAnalyzer(ChunkId id) {
		ensureHtmlAnalyzerByIdMap();
		return (htmlAnalyzerByIdMap == null) ? null : htmlAnalyzerByIdMap.get(id);
	}

	protected void ensureHtmlAnalyzerByIdMap() {
		if (htmlAnalyzerByIdMap == null) {
			htmlAnalyzerByIdMap = new HashMap<ChunkId, HtmlAnalyzer>();
		}
	}

	public SVGG labelChunk() {
		// might iterate through pages
		throw new RuntimeException("NYI");
	}

	void indexHtmlBySvgId(HtmlAnalyzer htmlAnalyzer, ChunkId chunkId) {
		ensureHtmlAnalyzerByIdMap();
		htmlAnalyzerByIdMap.put(chunkId, htmlAnalyzer);
	}

	/** label HtmlElement
	 * 
	 * @param id
	 * @param title
	 * @param serial
	 */
	void labelChunk(ChunkId id, String title, Integer serial) {
		getHtmlAnalyzer(id);
		HtmlAnalyzer htmlAnalyzer = getHtmlAnalyzer(id);
		if (htmlAnalyzer != null) {
			htmlAnalyzer.addClassAttributeIfMissing(title, serial);
		}
	}

	public HtmlUl searchHtml(String italicXpathS, Pattern pattern) {
		throw new RuntimeException("NYI");
	}
	
	public void addHtmlElement(HtmlElement htmlElement, ChunkId chunkId) {
		HtmlAnalyzer htmlAnalyzer = new HtmlAnalyzer(htmlElement, this);
		getHtmlAnalyzerByIdMap().put(chunkId, htmlAnalyzer);
	}

	public void analyzeFigures() {
		for (HtmlAnalyzer figureHtmlAnalyzer : figureHtmlAnalyzerList) {
			FigureAnalyzer figureAnalyzer = createFigureAnalyzer(figureHtmlAnalyzer);
			figureAnalyzer.analyze();
		}
	}

	private FigureAnalyzer createFigureAnalyzer(HtmlAnalyzer figureHtmlAnalyzer) {
		FigureAnalyzer figureAnalyzer = null;
		PageChunkAnalyzer analyzer = figureHtmlAnalyzer.getAnalyzer();
		if (analyzer instanceof MixedAnalyzer) {
			TextAnalyzer textAnalyzer = ((MixedAnalyzer)analyzer).getTextAnalyzer();
			ImageAnalyzer imageAnalyzer = ((MixedAnalyzer)analyzer).getImageAnalyzer();
			PathAnalyzer pathAnalyzer = ((MixedAnalyzer)analyzer).getPathAnalyzer();
			figureAnalyzer = new FigureAnalyzer(textAnalyzer, pathAnalyzer, imageAnalyzer, null);
		} else if (analyzer instanceof TextAnalyzer) {
			figureAnalyzer = new FigureAnalyzer((TextAnalyzer)analyzer, (PathAnalyzer)null, (ImageAnalyzer)null, null);
			LOG.error("Figure has no path/image");
		}
		return figureAnalyzer;
	}

	public void analyzeTables() {
		for (HtmlAnalyzer tableHtmlAnalyzer : tableHtmlAnalyzerList) {
			TableAnalyzer tableAnalyzer = createTableAnalyzer(tableHtmlAnalyzer);
//			tableAnalyzer.analyze();
			HtmlElement htmlElement = tableAnalyzer.createTable();
			LOG.debug("Table "+htmlElement.toXML());
			// transfer any existing id and class attribute
			HtmlElement oldHtmlElement = tableHtmlAnalyzer.createHtmlElement();
			if (oldHtmlElement != null) {
				CMLUtil.copyAttributes(oldHtmlElement, htmlElement);
			}
			tableHtmlAnalyzer.setHtmlElement(htmlElement);
		}
	}

	private TableAnalyzer createTableAnalyzer(HtmlAnalyzer tableHtmlAnalyzer) {
		TableAnalyzer tableAnalyzer = null;
		PageChunkAnalyzer analyzer = tableHtmlAnalyzer.getAnalyzer();
		
		if (analyzer instanceof MixedAnalyzer) {
			MixedAnalyzer mixedAnalyzer = ((MixedAnalyzer)analyzer);
			LOG.trace("M "+mixedAnalyzer);
			TextAnalyzer textAnalyzer = mixedAnalyzer.getTextAnalyzer();
			if (textAnalyzer == null) {
				LOG.error("Table has no text so cannot process");
				return null;
			}
			ImageAnalyzer imageAnalyzer = mixedAnalyzer.getImageAnalyzer();
			if (imageAnalyzer != null) {
				LOG.error("Cannot currently analyze images in Tables");
				return null;
			}
			PathAnalyzer pathAnalyzer = mixedAnalyzer.getPathAnalyzer();
			tableAnalyzer = new TableAnalyzer(textAnalyzer, pathAnalyzer);
		} else if (analyzer instanceof TextAnalyzer) {
			TextAnalyzer textAnalyzer = (TextAnalyzer)analyzer;
			tableAnalyzer = new TableAnalyzer(textAnalyzer, null);
		}
		return tableAnalyzer;
	}



}
