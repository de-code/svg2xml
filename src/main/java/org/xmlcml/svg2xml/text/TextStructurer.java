package org.xmlcml.svg2xml.text;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Elements;

import org.apache.log4j.Logger;
import org.xmlcml.euclid.IntArray;
import org.xmlcml.euclid.Real;
import org.xmlcml.euclid.Real2Range;
import org.xmlcml.euclid.RealArray;
import org.xmlcml.euclid.RealRange;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.graphics.svg.SVGSVG;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.html.HtmlDiv;
import org.xmlcml.html.HtmlElement;
import org.xmlcml.html.HtmlP;
import org.xmlcml.html.HtmlSpan;
import org.xmlcml.svg2xml.analyzer.AbstractAnalyzer;
import org.xmlcml.svg2xml.analyzer.TextAnalyzerUtils;
import org.xmlcml.svg2xml.analyzer.TextAnalyzerX;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

/** holds text lines in order
 * to simplify TextAnalyzer
 * 
 * @author pm286
 *
 */
public class TextStructurer {

	private static final Logger LOG = Logger.getLogger(TextStructurer.class);

	/** used for splitting between lineGroups
	 * 
	 * @author pm286
	 *
	 */
	public enum Splitter {
		BOLD,
		FONTSIZE,
		FONTFAMILY,
	};
	
	Pattern NUMBER_ITEM_PATTERN = Pattern.compile("^\\s*[\\[\\(]?\\s*(\\d+)\\s*\\.?[\\]\\)]?\\.?\\s*.*");
	
	/** default ratio for "isLargerThan" */
	public static final double LARGER_FONT_SIZE_RATIO = 1.02;

	private TextAnalyzerX textAnalyzer;
	
	private List<TextLine> linesWithCommonestFont;
	private List<TextLine> linesWithLargestFont;
	private List<TextLine> textLineList;
	private SvgPlusCoordinate largestFontSize;
	private SvgPlusCoordinate commonestFontSize;
	private Real2Range textLinesLargetFontBoundingBox;
	private Set<SvgPlusCoordinate> fontSizeSet;

	private Multiset<String> fontFamilySet;
	private List<Double> actualWidthsOfSpaceCharactersList;
	private Map<TextLine, Integer> textLineSerialMap;
	private List<String> textLineContentList;

	private RealArray interTextLineSeparationArray;
	private RealArray meanFontSizeArray;
	private RealArray modalExcessWidthArray;
	private Multiset<Double> separationSet;
	private Map<Integer, TextLine> textLineByYCoordMap;
	private RealArray textLineCoordinateArray;
	private Multimap<SvgPlusCoordinate, TextLine> textLineListByFontSize;

	private List<Real2Range> textLineChunkBoxes;

	private List<ScriptLine> initialTextLineGroupList;
	private List<TextLine> commonestFontSizeTextLineList;

	private List<ScriptLine> scriptedLineList;

	private HtmlElement createdHtmlElement;

	/** this COPIES the lines in the textAnalyzer
	 * this may not be a good idea
	 * @param textAnalyzer to copy lines from
	 */
	public TextStructurer(TextAnalyzerX textAnalyzer) {
		this.textAnalyzer = textAnalyzer;
		if (textAnalyzer != null) {
			textAnalyzer.setTextStructurer(this);
			List<SVGText> characters = textAnalyzer.getTextCharacters();
			this.createSortedLines(characters, textAnalyzer);
		}
	}

	public static TextStructurer createTextStructurer(File svgFile) {
		return createTextStructurer(svgFile, null);
	}

	public static TextStructurer createTextStructurer(File svgFile, TextAnalyzerX textAnalyzer) {
		TextStructurer container = new TextStructurer(textAnalyzer);
		List<TextLine> textLineList = TextStructurer.createTextLineList(svgFile);
		if (textLineList != null) {
			container.setTextLines(textLineList);
		}
		return container;
	}

	public List<TextLine> getLinesInIncreasingY() {
		if (textLineList == null) {
			ensureTextLineByYCoordMap();
			List<Integer> yCoordList = Arrays.asList(textLineByYCoordMap.keySet().toArray(new Integer[0]));
			Collections.sort(yCoordList);
			textLineList = new ArrayList<TextLine>();
			int i = 0;
			textLineSerialMap = new HashMap<TextLine, Integer>();
			for (Integer y : yCoordList) {
				TextLine textLine = textLineByYCoordMap.get(y);
				textLineList.add(textLine);
				textLineSerialMap.put(textLine, i++);
			}
		}
		return textLineList;
	}
	
	/** some lines may not have spaces
	 * 
	 * @return
	 */
	public List<Double> getActualWidthsOfSpaceCharactersList() {
		if (actualWidthsOfSpaceCharactersList == null) {
			getLinesInIncreasingY();
			if (textLineList != null && textLineList.size() > 0) {
				actualWidthsOfSpaceCharactersList = new ArrayList<Double>();
				for (int i = 0; i < textLineList.size(); i++) {
					Double meanWidth = textLineList.get(i).getMeanWidthOfSpaceCharacters();
					meanWidth = meanWidth == null ? null : Real.normalize(meanWidth, TextAnalyzerX.NDEC_FONTSIZE);
					actualWidthsOfSpaceCharactersList.add(meanWidth);
				}
			}
//			actualWidthsOfSpaceCharactersArray.format(NDEC_FONTSIZE);
		}
		return actualWidthsOfSpaceCharactersList;
	}

	private void ensureTextLineByYCoordMap() {
		if (textLineByYCoordMap == null) {
			textLineByYCoordMap = new HashMap<Integer, TextLine>();
		}
	}
	
	public Integer getSerialNumber(TextLine textLine) {
		return (textLineSerialMap == null) ? null : textLineSerialMap.get(textLine);
	}
	

	public List<String> getTextLineContentList() {
		textLineContentList = null;
		if (textLineList != null) {
			textLineContentList = new ArrayList<String>();
			for (TextLine textLine : textLineList) {
				textLineContentList.add(textLine.getLineString());
			}
		}
		return textLineContentList;
	}
	
	public List<TextLine> getTextLineList() {
		return textLineList;
	}

	public void insertSpaces() {
		if (textLineList != null) {
			for (TextLine textLine : textLineList) {
				textLine.insertSpaces();
			}
		}
	}

	public void insertSpaces(double scaleFactor) {
		if (textLineList != null) {
			for (TextLine textLine : textLineList) {
				textLine.insertSpaces(scaleFactor);
			}
		}
	}


	
	public Set<SvgPlusCoordinate> getFontSizeContainerSet() {
		Set<SvgPlusCoordinate> fontSizeContainerSet = new HashSet<SvgPlusCoordinate>();
		if (fontSizeContainerSet != null) {
			for (TextLine textLine : textLineList) {
				fontSizeContainerSet.addAll(textLine.getFontSizeContainerSet());
			}
		}
		return fontSizeContainerSet;
	}

	public RealArray getMeanFontSizeArray() {
		if (meanFontSizeArray == null) {
			getLinesInIncreasingY();
			if (textLineList != null && textLineList.size() > 0) {
				meanFontSizeArray = new RealArray(textLineList.size());
				for (int i = 0; i < textLineList.size(); i++) {
					meanFontSizeArray.setElementAt(i, textLineList.get(i).getMeanFontSize());
				}
			}
			meanFontSizeArray.format(TextAnalyzerX.NDEC_FONTSIZE);
		}
		return meanFontSizeArray;
	}

	public void setTextLines(List<TextLine> textLineList) {
		if (textLineList != null) {
			this.textLineList = new ArrayList<TextLine>();
			for (TextLine textLine : textLineList) {
				add(textLine);
			}
		}
	}

	private void add(TextLine textLine) {
		ensureTextLineList();
		this.textLineList.add(textLine);
	}
	
	private void ensureTextLineList() {
		if (this.textLineList == null) {
			this.textLineList = new ArrayList<TextLine>();
		}
	}

	public List<TextLine> getLinesWithLargestFont() {
		if (linesWithLargestFont == null) {
			linesWithLargestFont = new ArrayList<TextLine>();
			getLargestFontSize();
			for (int i = 0; i < textLineList.size(); i++){
				TextLine textLine = textLineList.get(i);
				Double fontSize = (textLine == null) ? null : textLine.getFontSize();
				if (fontSize != null) {
					if (Real.isEqual(fontSize, largestFontSize.getDouble(), 0.001)) {
						linesWithLargestFont.add( textLine);
					}
				}
			}
		}
		return linesWithLargestFont;
	}

	public List<TextLine> getLinesWithCommonestFont() {
		if (linesWithCommonestFont == null) {
			linesWithCommonestFont = new ArrayList<TextLine>();
			getCommonestFontSize();
			for (int i = 0; i < textLineList.size(); i++){
				TextLine textLine = textLineList.get(i);
				Double fontSize = (textLine == null) ? null : textLine.getFontSize();
				if (fontSize != null) {
					if (Real.isEqual(fontSize, commonestFontSize.getDouble(), 0.001)) {
						linesWithCommonestFont.add( textLine);
					}
				}
			}
		}
		return linesWithCommonestFont;
	}

	public SvgPlusCoordinate getCommonestFontSize() {
		commonestFontSize = null;
		Map<Double, Integer> fontCountMap = new HashMap<Double, Integer>();
		for (TextLine textLine : textLineList) {
			Double fontSize = textLine.getFontSize();
			Integer ntext = textLine.getCharacterList().size();
			if (fontSize != null) {
				Integer sum = fontCountMap.get(fontSize);
				if (sum == null) {
					sum = ntext;
				} else {
					sum += ntext;
				}
				fontCountMap.put(fontSize, sum);
			}
		}
		getCommonestFontSize(fontCountMap);
		return commonestFontSize;
	}

	private void getCommonestFontSize(Map<Double, Integer> fontCountMap) {
		int frequency = -1;
		for (Double fontSize : fontCountMap.keySet()) {
			int count = fontCountMap.get(fontSize);
			LOG.trace(">> "+fontSize+" .. "+fontCountMap.get(fontSize));
			if (commonestFontSize == null || count > frequency) {
			    commonestFontSize = new SvgPlusCoordinate(fontSize);
			    frequency = count;
			}
		}
		if (commonestFontSize != null) LOG.trace("commonest "+commonestFontSize.getDouble());
	}
	
	public SvgPlusCoordinate getLargestFontSize() {
		largestFontSize = null;
		Set<SvgPlusCoordinate> fontSizes = this.getFontSizeSet();
		for (SvgPlusCoordinate fontSize : fontSizes) {
			if (largestFontSize == null || largestFontSize.getDouble() < fontSize.getDouble()) {
				largestFontSize = fontSize;
			}
		}
		return largestFontSize;
	}
	
	public Real2Range getLargestFontBoundingBox() {
		if (textLinesLargetFontBoundingBox == null) {
			getLinesWithLargestFont();
			getBoundingBox(linesWithLargestFont);
		}
		return textLinesLargetFontBoundingBox;
	}

	public static Real2Range getBoundingBox(List<TextLine> textLines) {
		Real2Range boundingBox = null;
		if (textLines.size() > 0) {
			boundingBox = new Real2Range(new Real2Range(textLines.get(0).getBoundingBox()));
			for (int i = 1; i < textLines.size(); i++) {
				boundingBox.plus(textLines.get(i).getBoundingBox());
			}
		}
		return boundingBox;
	}

	public Set<SvgPlusCoordinate> getFontSizeSet() {
		if (fontSizeSet == null) {
			if (textLineList != null) {
				fontSizeSet = new HashSet<SvgPlusCoordinate>();
				for (TextLine textLine : textLineList) {
					Set<SvgPlusCoordinate> textLineFontSizeSet = textLine.getFontSizeSet();
					fontSizeSet.addAll(textLineFontSizeSet);
				}
			}
		}
		return fontSizeSet;
	}

	/** creates a multiset from addAll() on multisets for each line
	 *  
	 * @return
	 */
	public Multiset<String> getFontFamilyMultiset() {
		if (fontFamilySet == null) {
			fontFamilySet = HashMultiset.create();
			for (TextLine textLine : textLineList) {
				Multiset<String> listFontFamilySet = textLine.getFontFamilyMultiset();
				fontFamilySet.addAll(listFontFamilySet);
			}
		}
		return fontFamilySet;
	}

	/** gets commonest font
	 *  
	 * @return
	 */
	public String getCommonestFontFamily() {
		getFontFamilyMultiset();
		String commonestFontFamily = null;
		int highestCount = -1;
		Set<String> fontFamilyElementSet = fontFamilySet.elementSet();
		for (String fontFamily : fontFamilyElementSet) {
			int count = fontFamilySet.count(fontFamily);
			if (count > highestCount) {
				highestCount = count;
				commonestFontFamily = fontFamily;
			}
		}
		return commonestFontFamily;
	}

	/** gets commonest font
	 *  
	 * @return
	 */
	public int getFontFamilyCount() {
		getFontFamilyMultiset();
		return fontFamilySet.elementSet().size();
	}

	/** get non-overlapping boundingBoxes
	 * @return
	 */
	public List<Real2Range> getDiscreteLineBoxes() {
		List<Real2Range> discreteLineBoxes = new ArrayList<Real2Range>();
//		List<TextLine> textLines = this.getLinesSortedByYCoord();
		return discreteLineBoxes;
	}

	public RealArray getInterTextLineSeparationArray() {
		getTextLineCoordinateArray();
		if (textLineList != null && textLineList.size() > 0) {
			interTextLineSeparationArray = new RealArray();
			Double y0 = textLineCoordinateArray.get(0);
			for (int i = 1; i < textLineCoordinateArray.size(); i++) {
				Double y = textLineCoordinateArray.get(i);
				interTextLineSeparationArray.addElement(y - y0);
				y0 = y;
			}
			interTextLineSeparationArray.format(TextAnalyzerX.NDEC_FONTSIZE);
		}
		return interTextLineSeparationArray;
	}

	public Multimap<SvgPlusCoordinate, TextLine> getTextLineListByFontSize() {
		if (textLineListByFontSize == null) {
			textLineListByFontSize = ArrayListMultimap.create();
			for (TextLine textLine : textLineList) {
				Set<SvgPlusCoordinate> fontSizeSet = textLine.getFontSizeSet();
				if (fontSizeSet != null) {
					for (SvgPlusCoordinate fontSize : fontSizeSet) {
						textLineListByFontSize.put(fontSize, textLine);
					}
				}
			}
		}
		return textLineListByFontSize;
		
	}

	public Map<Integer, TextLine> getTextLineByYCoordMap() {
		return textLineByYCoordMap;
	}

	public RealArray getModalExcessWidthArray() {
		if (modalExcessWidthArray == null) {
			getLinesInIncreasingY();
			if (textLineList != null && textLineList.size() > 0) {
				modalExcessWidthArray = new RealArray(textLineList.size());
				for (int i = 0; i < textLineList.size(); i++) {
					Double modalExcessWidth = textLineList.get(i).getModalExcessWidth();
					modalExcessWidthArray.setElementAt(i, modalExcessWidth);
				}
			}
			modalExcessWidthArray.format(TextAnalyzerX.NDEC_FONTSIZE);
		}
		return modalExcessWidthArray;
	}

	public Multiset<Double> createSeparationSet(int decimalPlaces) {
		getInterTextLineSeparationArray();
		interTextLineSeparationArray.format(decimalPlaces);
		separationSet = HashMultiset.create();
		for (int i = 0; i < interTextLineSeparationArray.size(); i++) {
			separationSet.add(interTextLineSeparationArray.get(i));
		}
		return separationSet;
	}

	public Double getMainInterTextLineSeparation(int decimalPlaces) {
		Double mainTextLineSeparation = null;
		createSeparationSet(decimalPlaces);
		Set<Entry<Double>> ddSet = separationSet.entrySet();
		Entry<Double> maxCountEntry = null;
		Entry<Double> maxSeparationEntry = null;
		for (Entry<Double> dd : ddSet) {
			if (maxCountEntry == null || maxCountEntry.getCount() < dd.getCount()) {
				maxCountEntry = dd;
			}
			if (maxSeparationEntry == null || maxSeparationEntry.getElement() < dd.getElement()) {
				maxSeparationEntry = dd;
			}
		}
		if (maxCountEntry.equals(maxSeparationEntry)) {
			mainTextLineSeparation = maxSeparationEntry.getElement();
		}
		return mainTextLineSeparation;
	}

	public void getSortedTextLines(List<SVGText> textCharacters) {
		if (textLineByYCoordMap == null) {
			textLineByYCoordMap = new HashMap<Integer, TextLine>();
			Multimap<Integer, SVGText> charactersByY = TextAnalyzerUtils.createCharactersByY(textCharacters);
			for (Integer yCoord : charactersByY.keySet()) {
				Collection<SVGText> characters = charactersByY.get(yCoord);
				TextLine textLine = new TextLine(characters, this.textAnalyzer);
				textLine.sortLineByX();
				textLineByYCoordMap.put(yCoord, textLine);
			}
		}
	}

	public RealArray getTextLineCoordinateArray() {
		if (textLineCoordinateArray == null) {
			getLinesInIncreasingY();
			if (textLineList != null && textLineList.size() > 0) {
				textLineCoordinateArray = new RealArray();
				for (TextLine textLine : textLineList) {
					Double y0 = textLine.getYCoord();
					textLineCoordinateArray.addElement(y0);
				}
			}
			textLineCoordinateArray.format(TextAnalyzerX.NDEC_FONTSIZE);
		}
		return textLineCoordinateArray;
	}

	/** finds maximum indent of lines
	 * must be at least 2 lines
	 * currently does not check for capitals, etc.
	 * 
	 */
	public Double getMaxiumumRightIndent() {
		Double indent = null;
		Double xRight = null;
		if (textLineList != null && textLineList.size() > 1) {
			for (TextLine textLine : textLineList) {
				Double xLast = textLine.getLastXCoordinate();
				if (xRight == null) {
					xRight = xLast;
				}
				if (xRight - xLast > TextAnalyzerX.INDENT_MIN) {
					indent = xLast;
				} else if (xLast - xRight > TextAnalyzerX.INDENT_MIN) {
					indent = xRight;
				}
			}
		}
		return indent;
	}

	public TextLineSet getTextLineSetByFontSize(double fontSize) {
		Multimap<SvgPlusCoordinate, TextLine> textLineListByFontSize = this.getTextLineListByFontSize();
		List<TextLine> textLines = (List<TextLine>) textLineListByFontSize.get(new SvgPlusCoordinate(fontSize));
		return new TextLineSet(textLines);
	}

	public List<ScriptLine> getInitialTextLineGroupList() {
		getTextLineChunkBoxes();
		return initialTextLineGroupList;
	}

	/**
	 * This is heuristic. At present it is font-size equality. Font families
	 * are suspect as there are "synonyms", e.g. TimesRoman and TimesNR
	 * 
	 * @return
	 */
	public List<TextLine> getCommonestFontSizeTextLineList() {
		if (commonestFontSizeTextLineList == null) {
			SvgPlusCoordinate commonestFontSize = getCommonestFontSize();
			Double commonestFontSizeValue = (commonestFontSize == null) ?
					null : commonestFontSize.getDouble();
			commonestFontSizeTextLineList = new ArrayList<TextLine>();
			for (TextLine textLine : textLineList) {
				Double fontSize = textLine.getFontSize();
				if (fontSize != null && Real.isEqual(fontSize, commonestFontSizeValue, 0.01)) {
					commonestFontSizeTextLineList.add(textLine);
					LOG.trace("COMMONEST FONT SIZE "+textLine);
				}
			}
		}
		return commonestFontSizeTextLineList;
	}

	public List<ScriptLine> getScriptedLineList() {
		if (scriptedLineList == null) {
			getCommonestFontSizeTextLineList();
			getInitialTextLineGroupList();
			scriptedLineList = new ArrayList<ScriptLine>();
			int i = 0;
			for (ScriptLine textLineGroup : initialTextLineGroupList) {
				List<ScriptLine> splitChunks = textLineGroup.splitIntoUniqueChunks(this);
				for (ScriptLine textLineChunk0 : splitChunks) {
					scriptedLineList.add(textLineChunk0);
				}
				i++;
			}
		}
		LOG.trace("separated "+scriptedLineList.size());
		return scriptedLineList;
	}

	public List<Real2Range> getTextLineChunkBoxes() {
		if (textLineChunkBoxes == null) {
			List<TextLine> textLineList = getLinesInIncreasingY();
			textLineChunkBoxes = new ArrayList<Real2Range>();
			Real2Range bbox = null;
			ScriptLine textLineGroup = null;
			int i = 0;
			initialTextLineGroupList = new ArrayList<ScriptLine>();
			for (TextLine textLine : textLineList) {
				Real2Range bbox0 = textLine.getBoundingBox();
				LOG.trace(">> "+textLine.getLineString());
				if (bbox == null) {
					bbox = bbox0;
					textLineGroup = new ScriptLine(this);
					addBoxAndLines(bbox, textLineGroup);
				} else {
					Real2Range intersectionBox = bbox.intersectionWith(bbox0);
					if (intersectionBox == null) {
						bbox = bbox0;
						textLineGroup = new ScriptLine(this);
						addBoxAndLines(bbox, textLineGroup);
					} else {
						bbox = bbox.plusEquals(bbox0);
					}
				}
				textLineGroup.add(textLine);
			}
		}
		return textLineChunkBoxes;
	}

	private void addBoxAndLines(Real2Range bbox, ScriptLine textLineGroup) {
		textLineChunkBoxes.add(bbox);
		initialTextLineGroupList.add(textLineGroup);
	}

	public static TextStructurer createTextStructurerWithSortedLines(File svgFile) {
		SVGSVG svgPage = (SVGSVG) SVGElement.readAndCreateSVG(svgFile);
		List<SVGText> textCharacters = SVGText.extractTexts(SVGUtil.getQuerySVGElements(svgPage, ".//svg:text"));
		return createTextStructurerWithSortedLines(textCharacters);
	}

	public static TextStructurer createTextStructurerWithSortedLines(List<SVGText> textCharacters, TextAnalyzerX textAnalyzer) {
		TextStructurer textStructurer = new TextStructurer(textAnalyzer);
		textStructurer.createSortedLines(textCharacters, textAnalyzer);
		return textStructurer;
	}

	private void createSortedLines(List<SVGText> textCharacters,
			TextAnalyzerX textAnalyzer) {
		this.getSortedTextLines(textCharacters);
		this.getLinesInIncreasingY();
		textAnalyzer.setTextCharacters(textCharacters);
		textAnalyzer.setTextStructurer(this);
	}
	
	public static TextStructurer createTextStructurerWithSortedLines(List<SVGText> textCharacters) {
		TextAnalyzerX textAnalyzer = new TextAnalyzerX();
		textAnalyzer.setTextCharacters(textCharacters);
		TextStructurer textStructurer = new TextStructurer(textAnalyzer);
		textStructurer.getSortedTextLines(textCharacters);
		textStructurer.getLinesInIncreasingY();
		textAnalyzer.setTextStructurer(textStructurer);
		return textStructurer;
	}
	
	public TextAnalyzerX getTextAnalyzer() {
		return textAnalyzer;
	}

	/** finds maximum indent of lines
	 * must be at least 2 lines
	 * currently does not check for capitals, etc.
	 * 
	 */
	public Double getMaximumLeftIndentForLargestFont() {
		Double indent = null;
		Double xLeft = null;
		List<TextLine> textLineListWithLargestFont = this.getLinesWithCommonestFont();
		if (textLineListWithLargestFont != null && textLineListWithLargestFont.size() > 1) {
			for (TextLine textLine : textLineListWithLargestFont) {
				Double xStart = textLine.getFirstXCoordinate();
				if (xStart == null) {
					throw new RuntimeException("null start");
				}
				if (xLeft == null) {
					xLeft = xStart;
				}
				if (xLeft - xStart > TextAnalyzerX.INDENT_MIN) {
					indent = xLeft;
				} else if (xStart - xLeft > TextAnalyzerX.INDENT_MIN) {
					indent = xStart;
				}
			}
		}
		return indent;
	}

	/** finds maximum indent of lines
	 * must be at least 2 lines
	 * currently does not check for capitals, etc.
	 * 
	 */
	public static Double getMaximumLeftIndent(List<TextLine> textLineList) {
		Double indent = null;
		Double xLeft = null;
		if (textLineList != null && textLineList.size() > 1) {
			for (TextLine textLine : textLineList) {
				Double xStart = textLine.getFirstXCoordinate();
				if (xStart == null) {
					throw new RuntimeException("null start");
				}
				if (xLeft == null) {
					xLeft = xStart;
				}
				if (xLeft - xStart > TextAnalyzerX.INDENT_MIN) {
					indent = xLeft;
				} else if (xStart - xLeft > TextAnalyzerX.INDENT_MIN) {
					indent = xStart;
				}
			}
		}
		return indent;
	}

	public static List<TextLine> createTextLineList(File svgFile) {
		TextStructurer textStructurer = createTextStructurerWithSortedLines(svgFile);
		List<TextLine> textLineList = textStructurer.getLinesInIncreasingY();
		return textLineList;
	}

	public static AbstractAnalyzer createTextAnalyzerWithSortedLines(List<SVGText> characters) {
			TextAnalyzerX textAnalyzer = new TextAnalyzerX();
			/*TextStructurer textStructurer = */TextStructurer.createTextStructurerWithSortedLines(characters, textAnalyzer);
			return textAnalyzer;
	}


	public static HtmlElement createHtmlDiv(List<ScriptLine> textLineGroupList) {
		HtmlDiv div = new HtmlDiv();
		for (ScriptLine group : textLineGroupList) {
			HtmlElement el = null;
			if (group == null) {
//				el = new HtmlP();
//				el.appendChild("PROBLEM");
//				div.appendChild(el);
//				div.debug("XXXXXXXXXXXXXXXXXX");
			} else {
				el = group.createHtml();
				div.appendChild(el);
			}
		}
		return div;
	}

	public HtmlElement createHtmlDivWithParas() {
		List<ScriptLine> textLineGroupList = this.getScriptedLineList();
		LOG.trace("TEXTLINEGROUP splt heres "+textLineGroupList);
		if (textLineGroupList.size() == 0) {
			LOG.trace("TextLineList: "+textLineList);
			// debug
		}
		boolean bb = false;
		createHtmlElementWithParas(textLineGroupList);
		return createdHtmlElement;
	}

	/** only used in tests?
	 * 
	 * @param textLineGroupList
	 * @return
	 */
	 public HtmlElement createHtmlElementWithParas(List<ScriptLine> textLineGroupList) {
		List<TextLine> commonestTextLineList = this.getCommonestFontSizeTextLineList();
		createdHtmlElement = null;
		if (commonestTextLineList.size() == 0){
			 createdHtmlElement = null;
		} else if (commonestTextLineList.size() == 1){
			 createdHtmlElement = commonestTextLineList.get(0).createHtmlLine();
		} else {
			HtmlElement rawDiv = createHtmlDiv(textLineGroupList);
			createdHtmlElement = createDivWithParas(commonestTextLineList, rawDiv);
		}
		return createdHtmlElement;
	}

	private HtmlDiv createDivWithParas(List<TextLine> textLineList, HtmlElement rawDiv) {
		HtmlDiv div = null;
		Double leftIndent = TextStructurer.getMaximumLeftIndent(textLineList);
		Real2Range leftBB = TextStructurer.getBoundingBox(textLineList);
		Elements htmlLines = rawDiv.getChildElements();
		LOG.trace("textLine "+textLineList.size()+"; html: "+ htmlLines.size());
		
		if (leftBB != null) {
			Double deltaLeftIndent = (leftIndent == null) ? 0 : (leftIndent - leftBB.getXRange().getMin());
			Real2Range largestFontBB = TextStructurer.getBoundingBox(textLineList);
			if (largestFontBB != null) {
				RealRange xRange = largestFontBB.getXRange();
				Double indentBoundary = largestFontBB.getXRange().getMin() + deltaLeftIndent/2.0;
				LOG.trace("left, delta, boundary "+leftIndent+"; "+deltaLeftIndent+"; "+indentBoundary);
				div = new HtmlDiv();
				// always start with para
				HtmlP pCurrent = (htmlLines.size() == 0) ? null : 
					TextStructurer.createAndAddNewPara(div, (HtmlP) htmlLines.get(0));
				int size = htmlLines.size();
				for (int i = 1; i < size/*textLineList.size()*/; i++) {
					TextLine textLine = (textLineList.size() <= i) ? null : textLineList.get(i);
					LOG.trace(">"+i+"> "+textLine);
					HtmlP pNext = i < htmlLines.size() ? (HtmlP) HtmlElement.create(htmlLines.get(i)) : null;
					// indent, create new para
					if (pNext == null) {
						LOG.error("Skipping HTML "+pCurrent+" // "+textLine);
					} else if (textLine != null && textLine.getFirstXCoordinate() > indentBoundary) {
						pCurrent = createAndAddNewPara(div, pNext);
					} else {
						mergeParas(pCurrent, pNext);
					}
				}
			}
		}
		return div;
	}
	
	public static HtmlP createAndAddNewPara(HtmlElement div, HtmlP p) {
		HtmlP pNew = (HtmlP) HtmlElement.create(p);
		div.appendChild(pNew);
		return pNew;
	}

	public static void mergeParas(HtmlP pCurrent, HtmlP pNext) {
		Elements currentChildren = pCurrent.getChildElements();
		if (currentChildren.size() > 0) {
			HtmlElement lastCurrent = (HtmlElement) currentChildren.get(currentChildren.size() - 1);
			HtmlSpan currentLastSpan = (lastCurrent instanceof HtmlSpan) ? (HtmlSpan) lastCurrent : null;
			Elements nextChildren = pNext.getChildElements();
			HtmlElement firstNext = nextChildren.size() == 0 ? null : (HtmlElement) nextChildren.get(0);
			HtmlSpan nextFirstSpan = (firstNext != null && firstNext instanceof HtmlSpan) ? (HtmlSpan) firstNext : null;
			int nextCounter = 0;
			// merge texts
			if (currentLastSpan != null && nextFirstSpan != null) {
				String mergedText = mergeLineText(currentLastSpan.getValue(), nextFirstSpan.getValue());
				LOG.trace("Merged "+mergedText);
				lastCurrent.setValue(mergedText);
				nextCounter = 1;
			}
			//merge next line's children
			for (int i = nextCounter; i < nextChildren.size(); i++) {
				pCurrent.appendChild(HtmlElement.create(nextChildren.get(i)));
			}
		}
	}

	private static String mergeLineText(String last, String next) {
		//merge hyphen minus
		if (last.endsWith("-")) {
			return last.substring(0, last.length()-1) + next;
		} else {
			return last + " " + next;
		}
	}

	public boolean endsWithRaggedLine() {
		return createdHtmlElement != null &&
				!createdHtmlElement.getValue().endsWith(".");
	}

	public boolean startsWithRaggedLine() {
		boolean starts = false;
		if (createdHtmlElement != null && createdHtmlElement.getValue().length() > 0) {
			Character c = createdHtmlElement.getValue().charAt(0);
			if (c != null) {
				starts = !Character.isUpperCase(c);
			}
		}
		return starts;
	}

	public boolean lineIsLargerThanCommonestFontSize(int lineNumber) {
		TextLine textLine = (lineNumber < 0 || lineNumber >= textLineList.size()) ?
				null : textLineList.get(lineNumber);
		return lineIsLargerThanCommonestFontSize(textLine);
	}

	public boolean lineIsLargerThanCommonestFontSize(TextLine textLine) {
		boolean isLargerThan = false;
		Double commonestFontSize = getCommonestFontSize().getDouble();
		if (textLine != null && commonestFontSize != null) {
			Double fontSize = textLine.getFontSize();
			if (fontSize != null) {
				isLargerThan = fontSize / commonestFontSize > LARGER_FONT_SIZE_RATIO;
			}
		}
		return isLargerThan;
	}

	public boolean isCommonestFontSize(TextLine textLine) {
		this.getCommonestFontSizeTextLineList();
		return textLine != null && commonestFontSizeTextLineList.contains(textLine);
	}

	public boolean isCommonestFontSize(ScriptLine textLineGroup) {
		this.getCommonestFontSizeTextLineList();
		TextLine largestLine = textLineGroup.getLargestLine();
		return textLineGroup != null && commonestFontSizeTextLineList.contains(largestLine);
	}

	public boolean lineGroupIsLargerThanCommonestFontSize(int lineNumber) {
		TextLine textLine = (lineNumber < 0 || lineNumber >= textLineList.size()) ?
				null : textLineList.get(lineNumber);
		return lineIsLargerThanCommonestFontSize(textLine);
	}

	public boolean lineGroupIsLargerThanCommonestFontSize(ScriptLine textLineGroup) {
		boolean isLargerThan = false;
		Double commonestFontSize = getCommonestFontSize().getDouble();
		if (textLineGroup != null && commonestFontSize != null) {
			Double fontSize = textLineGroup.getFontSize();
			if (fontSize != null) {
				isLargerThan = fontSize / commonestFontSize > LARGER_FONT_SIZE_RATIO;
			}
		}
		return isLargerThan;
	}

	
	/** split after line where font size changes to/from bigger than commonest
	 * dangerous if there are sub or superscripts (use splitGroupBiggerThanCommonest)
	 * @return
	 */
	public IntArray splitBiggerThanCommonest() {
		Double commonestFontSize = this.getCommonestFontSize().getDouble();
		IntArray splitArray = new IntArray();
		for (int i = 0; i < textLineList.size() - 1; i++) {
			TextLine textLineA = textLineList.get(i);
			Double fontSizeA = textLineA.getFontSize();
			TextLine textLineB = textLineList.get(i+1);
			Double fontSizeB = textLineB.getFontSize();
			if (fontSizeA != null && fontSizeB != null) {
				double ratioAB = fontSizeA / fontSizeB;
				// line increases beyond commonest size?
				if (Real.isEqual(fontSizeA, commonestFontSize, 0.01) 
						&& ratioAB < 1./LARGER_FONT_SIZE_RATIO) {
					splitArray.addElement(i);
				} else if (Real.isEqual(fontSizeB, commonestFontSize, 0.01) 
						&& ratioAB > LARGER_FONT_SIZE_RATIO) {
					splitArray.addElement(i);
				}
			}
		}
		return splitArray;
	}

	
	/** split after textLineGroup where font size changes to/from bigger than commonest
	 * 
	 * @return
	 */
	public IntArray splitGroupBiggerThanCommonest() {
		getScriptedLineList();
		Double commonestFontSize = this.getCommonestFontSize().getDouble();
		IntArray splitArray = new IntArray();
		for (int i = 0; i < scriptedLineList.size() - 1; i++) {
			ScriptLine textLineGroupA = scriptedLineList.get(i);
			Double fontSizeA = textLineGroupA.getFontSize();
			ScriptLine textLineB = scriptedLineList.get(i+1);
			Double fontSizeB = textLineB.getFontSize();
			if (fontSizeA != null && fontSizeB != null) {
				double ratioAB = fontSizeA / fontSizeB;
				// line increases beyond commonest size?
				if (Real.isEqual(fontSizeA, commonestFontSize, 0.01) 
						&& ratioAB < 1./LARGER_FONT_SIZE_RATIO) {
					splitArray.addElement(i);
				} else if (Real.isEqual(fontSizeB, commonestFontSize, 0.01) 
						&& ratioAB > LARGER_FONT_SIZE_RATIO) {
					splitArray.addElement(i);
				}
			}
		}
		return splitArray;
	}

	/** split the textStructurer after the lines in array.
	 * if null or size() == 0, returns list with 'this'. so a returned list of size 0
	 * effectively does nothing
	 * @param afterLineGroups if null or size() == 0, returns list with 'this';
	 * @return
	 */
	public List<TextStructurer> splitLineGroupsAfter(IntArray afterLineGroups) {
		getScriptedLineList();
		List<TextStructurer> textStructurerList = new ArrayList<TextStructurer>();
		if (afterLineGroups == null || afterLineGroups.size() == 0) {
			textStructurerList.add(this);
		} else {
			int start = 0;
			for (int i = 0; i < afterLineGroups.size();i++) {
				int lineNumber = afterLineGroups.elementAt(i);
				if (lineNumber > scriptedLineList.size() -1) {
					throw new RuntimeException("bad index: "+lineNumber);
				}
				TextStructurer newTextStructurer = createTextStructurerFromTextLineGroups(start, lineNumber);
				textStructurerList.add(newTextStructurer);
				start = lineNumber + 1;
			}
			TextStructurer newTextStructurer = createTextStructurerFromTextLineGroups(start, scriptedLineList.size() - 1);
			textStructurerList.add(newTextStructurer);
		}
		return textStructurerList;
	}

	private TextStructurer createTextStructurerFromTextLineGroups(int startLineGroup, int lineGroupNumber) {
		getScriptedLineList();
		TextStructurer textStructurer = new TextStructurer(null);
		textStructurer.textAnalyzer = this.textAnalyzer;
		for (int iGroup = startLineGroup; iGroup <= lineGroupNumber; iGroup++) {
			ScriptLine textLineGroup = scriptedLineList.get(iGroup);
			
			List<TextLine> textLineList = textLineGroup.getTextLineList();
			textStructurer.add(textLineList);
		}
		return textStructurer;
	}

	private void add(List<TextLine> textLineList) {
		for (TextLine textLine : textLineList) {
			this.add(textLine);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (textLineList == null) {
			sb.append("null");
		} else {
			sb.append("TextStructurer: "+ textLineList.size());
			for (TextLine textLine :textLineList) {
				sb.append(textLine.toString()+"\n");
			}
		}
		return sb.toString();
	}

	public List<TextStructurer> split(Splitter splitter) {
		if (Splitter.BOLD.equals(splitter)) {
			return splitOnFontBoldChange(0);
		}
		if (Splitter.FONTSIZE.equals(splitter)) {
			return splitOnFontSizeChange(0);
		}
		if (Splitter.FONTFAMILY.equals(splitter)) {
			return splitOnFontFamilyChange(0);
		}
		throw new RuntimeException("Unknown splitter: "+splitter);
	}

	/** splits bold line(s) from succeeeding ones.
	 * may trap smaller headers - must catch this later
	 * @return
	 */
	public List<TextStructurer> splitOnFontBoldChange(int maxFlip) {
		IntArray splitter = getSplitArrayForFontWeightChange(maxFlip);
		LOG.trace("SPLIT "+splitter);
		return splitIntoList(splitter);
	}
	
	/** splits line(s) on fontSize.
	 * @return
	 */
	public List<TextStructurer> splitOnFontSizeChange(int maxFlip) {
		IntArray splitter = getSplitArrayForFontSizeChange(maxFlip);
		return splitIntoList(splitter);
	}

	/** splits line(s) on fontFamily.
	 * @return
	 */
	public List<TextStructurer> splitOnFontFamilyChange(int maxFlip) {
		IntArray splitter = getSplitArrayForFontFamilyChange(maxFlip);
		return splitIntoList(splitter);
	}

	/** splits line(s) on fontSize.
	 * @return
	 */
	public IntArray getSplitArrayForFontWeightChange(int maxFlip) {
		getScriptedLineList();
		Boolean currentBold = null;
		IntArray splitArray = new IntArray();
		if (scriptedLineList.size() > 0) {
			int nFlip = 0;
			for (int i = 0; i < scriptedLineList.size(); i++) {
				boolean isBold = scriptedLineList.get(i).isBold();
				if (currentBold == null) { 
					currentBold = isBold;
					// insist on leading bold
					if (maxFlip < 0 && !isBold) {
						return splitArray;
					}
				} else if (!currentBold.equals(isBold)) {
					splitArray.addElement(i - 1);
					currentBold = isBold;
					if (nFlip++ >= maxFlip) break;
				}
			}
		}
		return splitArray;
	}
	
	
	/** splits line(s) on fontSize.
	 * @return
	 */
	public IntArray getSplitArrayForFontSizeChange(int maxFlip) {
		double EPS = 0.01;
		getScriptedLineList();
		Double currentFontSize = null;
		IntArray splitArray = new IntArray();
		if (scriptedLineList.size() > 0) {
			int nFlip = 0;
			for (int i = 0; i < scriptedLineList.size(); i++) {
				Double fontSize = scriptedLineList.get(i).getFontSize();
				if (currentFontSize == null) {
					currentFontSize = fontSize;
				} else if (!Real.isEqual(fontSize, currentFontSize, EPS)) {
					splitArray.addElement(i - 1);
					currentFontSize = fontSize;
					if (nFlip++ >= maxFlip) break;
				}
			}
		}
		return splitArray;
	}
	
	/** splits line(s) on fontSize.
	 * @return
	 */
	public IntArray getSplitArrayForFontFamilyChange(int maxFlip) {
		getScriptedLineList();
		String currentFontFamily = null;
		IntArray splitArray = new IntArray();
		if (scriptedLineList.size() > 0) {
			int nFlip = 0;
			for (int i = 0; i < scriptedLineList.size(); i++) {
				String fontFamily = scriptedLineList.get(i).getFontFamily();
				if (currentFontFamily == null) {
					currentFontFamily = fontFamily;
				} else if (!fontFamily.equals(currentFontFamily)) {
					splitArray.addElement(i - 1);
					currentFontFamily = fontFamily;
					if (nFlip++ >= maxFlip) break;
				}
			}
		}
		return splitArray;
	}

	private List<TextStructurer> splitIntoList(IntArray splitter) {
		List<TextStructurer> splitList = null;
		if (splitter != null && splitter.size() != 0) {
			splitList = this.splitLineGroupsAfter(splitter);
		}  else {
			splitList = new ArrayList<TextStructurer>();
			splitList.add(this);
		}
		return splitList;
	}

	public SVGG oldCreateSVGGChunk() {
		SVGG g = new SVGG();
		for (TextLine textLine : textLineList) {
			for (SVGText text : textLine) {
				g.appendChild(new SVGText(text));
			}
		}
		return g;
	}

	/** attempts to split into numbered list by line starts.
	 * 
	 * @return
	 */
	public List<TextStructurer> splitNumberedList() {
		getScriptedLineList();
		List<TextStructurer> splitLineGroups = new ArrayList<TextStructurer>();
		int last = 0;
		for (int i = 0; i < scriptedLineList.size(); i++) {
			ScriptLine tlg = scriptedLineList.get(i);
			String value = tlg.getRawValue();
			LOG.trace(value);
			Matcher matcher = NUMBER_ITEM_PATTERN.matcher(value);
			if (matcher.matches()) {
				Integer serial = Integer.parseInt(matcher.group(1));
				LOG.trace(">> "+serial);
				addTextLineGroups(splitLineGroups, last, i);
				last = i;
				LOG.trace("split: "+i);
			}
		}
		addTextLineGroups(splitLineGroups, last, scriptedLineList.size());
		return splitLineGroups;
	}

	private void addTextLineGroups(List<TextStructurer> splitLineGroups, int last, int next) {
		if (next > last) {
			TextStructurer tc = new TextStructurer(null);
			splitLineGroups.add(tc);
			for (int j = last; j < next; j++) {
				tc.add(scriptedLineList.get(j));
			}
		}
	}

	private void add(ScriptLine textLineGroup) {
		ensureScriptedLineList();
		scriptedLineList.add(textLineGroup);
		for (TextLine textLine : textLineGroup) {
			this.add(textLine);
		}
	}

	private List<ScriptLine> ensureScriptedLineList() {
		if (scriptedLineList == null) {
			scriptedLineList = new ArrayList<ScriptLine>();
		}
		return scriptedLineList;
	}
}