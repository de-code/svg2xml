package org.xmlcml.svg2xml.container;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Nodes;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.html.HtmlDiv;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.html.HtmlTable;
import org.xmlcml.html.HtmlUl;
import org.xmlcml.svg2xml.analyzer.ChunkId;
import org.xmlcml.svg2xml.indexer.LicenceIndexer;
import org.xmlcml.svg2xml.page.PageAnalyzer;
import org.xmlcml.svg2xml.util.SVG2XMLUtil;

/** containers contain the outputs of PageAnalyzer
 * they roughly mirror the HTML hierarchy and all implement
 * createHTML methods. 
 * 
 * @author pm286
 *
 */
public abstract class AbstractContainer {

	public final static Logger LOG = Logger.getLogger(AbstractContainer.class);

	public enum ContainerType {
		ACKNOWLEDGMENT,
		AUTHORS,
		CHUNK,
		COPYRIGHT,
		FIGURE,
		HEADER,
		FOOTER,
		LICENSE,
		PAGE,
		SCHEME,
		TABLE,
		TITLE,
		TEXT, 
		LIST, 
	}
	
	private static final String CHUNK = "chunk";

	private static final Pattern TABLE_CAPTION = Pattern.compile(".*Tab(le)?\\s+(\\d+).*");
	private static final Pattern FIGURE_CAPTION = Pattern.compile(".*Fig(ure)?\\s+(\\d+).*");

	private static final double HEADER_MAX = 80;
	private static final double FOOTER_MIN = 715; // not calibrated yet

	private static final Double MIN_TITLE_SIZE = 15.0; // first guess

	Pattern ACKNOWLEDGEMENT_P = Pattern.compile("([Aa]cknowledge?ments?)");
	Pattern AUTHOR_INFO_P = Pattern.compile(".*[Aa]uthors?\\'?\\s+[Ii]nformation.*");
	Pattern AUTHOR_DETAILS_P = Pattern.compile("[Aa]uthors?\\'?\\s+[Dd]etails");
	Pattern AUTHOR_CONTRIBUTIONS_P = Pattern.compile("[Aa]uthors?\\'?\\s+[Cc]ontributions");
	Pattern COPYRIGHT_P = Pattern.compile(".*"+String.valueOf((char)169)+".*");
	Pattern LICENCE_P = Pattern.compile("([Ll]icensee)|(Creative\\s*Commons)");
	
	protected List<AbstractContainer> containerList;
	protected PageAnalyzer pageAnalyzer;
	protected ChunkId chunkId;
	protected SVGG svgChunk;
	protected HtmlElement htmlElement;
	private HtmlDiv figureElement;
	private HtmlUl listElement;
	private HtmlTable tableElement;
	
	private ContainerType type;

	private Integer tableNumber;
	private Integer figureNumber;

	public AbstractContainer(PageAnalyzer pageAnalyzer) {
		this.pageAnalyzer = pageAnalyzer;
		ensureContainerList();
	}

	public HtmlElement createHtmlElement() {
		if (htmlElement == null) {
			htmlElement = new HtmlDiv();
			for (AbstractContainer container : containerList) {
				HtmlElement htmlElement1 = container.createHtmlElement();
				htmlElement.appendChild(htmlElement1);
			}
		}
		return htmlElement;
	}
	public abstract SVGG createSVGGChunk();
	
	private void ensureContainerList() {
		if (containerList == null) {
			this.containerList = new ArrayList<AbstractContainer>();
		}
	}
	
	public void add(AbstractContainer abstractContainer) {
		this.ensureContainerList();
		containerList.add(abstractContainer);
	}
	
	public List<AbstractContainer> getContainerList() {
		return this.containerList;
	}

	public String summaryString() {
		String clazz = this.getClass().getSimpleName();
		StringBuilder sb = new StringBuilder(">>>"+clazz+">>> \n");
		for (AbstractContainer container : containerList) {
			sb.append("\n   "+container.summaryString()+"\n");
		}
		sb.append("<<<"+clazz+"<<<\n");
		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.getClass().getSimpleName()+"\n");
		sb.append("Containers: "+containerList.size()+"\n");
		for (AbstractContainer container : containerList) {
			LOG.debug("CONT: "+container.getClass());
			sb.append(container.toString()+"\n");
		}
		return sb.toString();
	}

	protected String outputList(String title, List<? extends AbstractContainer> containerList) {
		StringBuilder sb = new StringBuilder(title+" "+containerList.size()+"\n");
		for (AbstractContainer container : containerList) {
			sb.append("... "+SVG2XMLUtil.trim(container.toString(), 100)+"\n");
		}
		return sb.toString();
	}
	
	protected String outputSVGList(String title, List<? extends SVGElement> svgList) {
		StringBuilder sb = new StringBuilder();
		if (svgList.size() < 5){
			for (SVGElement element : svgList) {
				String s = element.toXML();
				int l = s.length();
				sb.append(s.subSequence(0, Math.min(80, l))+((l > 80) ? "..." : "")+"\n");
			}
		} else {
			sb.append(title+" "+svgList.size()+"\n");
		}
		return sb.toString();
	}

	public String getSuffix() {
		return this.getClass().getSimpleName().substring(0, 1);
	}


	/** character value of Container
	 * mainly for string-based containers
	 * @return
	 */
	public String getRawValue() {
		return "StringValue: "+this.getClass().getName();
	}

	public void setChunkId(ChunkId chunkId) {
		this.chunkId = chunkId;
	}

	public ChunkId getChunkId() {
		if (chunkId == null) {
			SVGG chunk = this.getSVGChunk();
			if (chunk != null) {
				chunkId = new ChunkId(chunk.getId());
				if (chunkId == null) {
					LOG.debug("CHUNK "+chunk.toXML());
				}
			} else {
				LOG.debug("no chunk "+this.getClass());
			}
		}
		return chunkId;
	}

	public void setSVGChunk(SVGG svgChunk) {
		this.svgChunk = svgChunk;
	}
	
	public SVGG getSVGChunk() {
		return svgChunk;
	}

	public void writeFinalSVGChunk(File outputDocumentDir, Character cc, int humanPageNumber,
			int aggregatedContainerCount) {
		String filename = CHUNK+humanPageNumber+"."+  
			    (aggregatedContainerCount)+this.getSuffix()+String.valueOf(cc);
		SVG2XMLUtil.writeToSVGFile(outputDocumentDir, filename, svgChunk, false);
	}
	
	public ContainerType getType() {
		if (type == null) {
			calculateType();
		}
		return type;
	}

	private void calculateType() {
		createHtmlElement();
		Nodes boldNodes = htmlElement.query("//*[local-name()='b']");
		tableNumber = getCaptionNumber(TABLE_CAPTION, boldNodes);
		figureNumber = getCaptionNumber(FIGURE_CAPTION, boldNodes);
		if (tableNumber != null) {
			type = ContainerType.TABLE;
			LOG.trace("Table: "+tableNumber+" "+this.getClass());
			tableElement = processTable();
		}
		if (figureNumber != null) {
			type = ContainerType.FIGURE;
			LOG.trace("Figure: "+figureNumber+" "+this.getClass());
			figureElement = processFigure();
		}
		if (type == null) {
			Real2Range r2r = this.svgChunk.getBoundingBox();
			RealRange yRange = r2r == null ? null : r2r.getYRange();
			if (yRange != null) {
				if (yRange.getMax() < HEADER_MAX) {
					type = ContainerType.HEADER;
				} else if (yRange.getMin() > FOOTER_MIN) {
					type = ContainerType.FOOTER;
				}
			}
		}
		if (type == null) {
			if (this instanceof ScriptContainer) {
				ScriptContainer scriptContainer = (ScriptContainer) this;
				Double fontSize = scriptContainer.getCommonestFontSize();
				String value = scriptContainer.createHtmlElement().getValue();
				if (isFirstPage()) {
					if (fontSize != null && fontSize > MIN_TITLE_SIZE) {
						type = ContainerType.TITLE;
					}
				}
				if (type == null) {
					if (createHtmlListElement() != null) {
						type = ContainerType.LIST;
					}
				}
				if (type == null) {
					type = getMetadataType(value);
				}
			}
		}
		if (type == null) {
			type = ContainerType.CHUNK;
		}
		
	}

	private HtmlUl createHtmlListElement() {
		ListContainer listContainer = ListContainer.createList((ScriptContainer) this);
		listElement = listContainer.createHtmlElement();
		return listElement;
	}

	private HtmlDiv processFigure() {
		if (this instanceof DivContainer) {
			DivContainer divContainer = (DivContainer) this;
			figureElement = divContainer.createFigureElement();
		} else {
			LOG.error("Unprocessed figure: "+this.getClass());
		}
		return figureElement;
	}

	private HtmlTable processTable() {
		if (this instanceof DivContainer) {
			DivContainer divContainer = (DivContainer) this;
			tableElement = divContainer.createTableHtmlElement();
		}
		return tableElement;
	}

	private ContainerType getMetadataType(String value) {
		ContainerType type = null;
		if (type == null) type = getType(value, ContainerType.LICENSE, LicenceIndexer.PATTERN);
		if (type == null) type = getType(value, ContainerType.ACKNOWLEDGMENT, ACKNOWLEDGEMENT_P);
		if (type == null) type = getType(value, ContainerType.AUTHORS, AUTHOR_DETAILS_P);
		if (type == null) type = getType(value, ContainerType.AUTHORS, AUTHOR_INFO_P);
		if (type == null) type = getType(value, ContainerType.AUTHORS, AUTHOR_CONTRIBUTIONS_P);
		if (type == null) type = getType(value, ContainerType.COPYRIGHT, COPYRIGHT_P);
		return type;
	}

	private ContainerType getType(String value, ContainerType type, Pattern pattern) {
		return (pattern.matcher(value).find()) ? type : null;
	}

	/** checks for chunkId with page = 1
	 * 
	 * @return
	 */
	private boolean isFirstPage() {
		return chunkId == null ? null : chunkId.getPageNumber() == 1;
	}

	private Integer getCaptionNumber(Pattern captionPattern, Nodes nodes) {
		Integer number = null;
		for (int i = 0; i < nodes.size(); i++) {
			String s = nodes.get(i).getValue();
			Matcher m = captionPattern.matcher(s);
			if (m.matches()) {
				try {
					number = new Integer(m.group(2));
				} catch (NumberFormatException nfe) {
					throw new RuntimeException("Bad caption "+m.group(2)+"; "+s+ " in "+captionPattern);
				}
			}
		}
		return number;
	}

	public Element getFigureElement() {
		if (figureElement != null) {
			figureElement.addAttribute(new Attribute("border", "3pt"));
		}
		return figureElement;
	}

	public Element getListElement() {
		return listElement;
	}

	public Element getTableElement() {
		return tableElement;
	}
}
