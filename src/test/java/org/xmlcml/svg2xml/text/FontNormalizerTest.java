package org.xmlcml.svg2xml.text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xmlcml.graphics.svg.SVGElement;
import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.graphics.svg.SVGUtil;
import org.xmlcml.svg2xml.Fixtures;
import org.xmlcml.svg2xml.page.TextAnalyzer;


public class FontNormalizerTest {
	
	private static final Logger LOG = Logger.getLogger(FontNormalizerTest.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private List<SVGText> textList;

	@Before
	public void setup() {
		File file = new File(Fixtures.FONT_DIR, "fontweights.svg");
		try {
			SVGElement svgElement = SVGUtil.parseToSVGElement(new FileInputStream(file));
			textList = SVGText.extractSelfAndDescendantTexts(svgElement);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("cannot parse "+file, e);
		}
	}

	@Test
	public void testBlackFonts() {
		List<String> fontNames = new ArrayList<String>();
		List<Boolean> isBlack = new ArrayList<Boolean>();
		for (int i = 0; i < textList.size(); i++) {
			SVGText text = textList.get(i);
			String fontName = text.getSVGXFontName();
			fontNames.add(fontName);
			isBlack.add(FontNormalizer.isBoldFontName(fontName));
		}
		Assert.assertEquals("fontnames", 
				"[EFEEEK+Syntax-Black, EFEEEK+Syntax-Black, EFEEDJ+Syntax-Roman, EFEEDJ+Syntax-Roman]", 
				fontNames.toString());
		Assert.assertEquals("isBlack", "[true, true, false, false]", isBlack.toString());
	}
	
	@Test
	/**
	 * fills are "#231f20","#231f20","#231f20","#606060"
	 */
	public void testBoldFromGrayness() {
		List<Boolean> isBoldList = new ArrayList<Boolean>();
		List<String> fillList = new ArrayList<String>();
		FontNormalizer fontNormalizer = new FontNormalizer();
		fontNormalizer.setBoldThreshold(0x60);
		for (int i = 0; i < textList.size(); i++) {
			SVGElement text = textList.get(i);
			isBoldList.add(fontNormalizer.isBoldColor(text.getFill()));
			fillList.add(text.getFill());
		}
		Assert.assertEquals("fill", "[#231f20, #231f20, #231f20, #606060]", fillList.toString());
		Assert.assertEquals("isBold", "[true, true, true, false]", isBoldList.toString());
		
		isBoldList = new ArrayList<Boolean>();
		fontNormalizer.setBoldThreshold(0x61);
		for (int i = 0; i < textList.size(); i++) {
			SVGElement text = textList.get(i);
			isBoldList.add(fontNormalizer.isBoldColor(text.getFill()));
		}
		Assert.assertEquals("isBold", "[true, true, true, true]", isBoldList.toString());
		
		isBoldList = new ArrayList<Boolean>();
		fontNormalizer.setBoldThreshold(0x20);
		for (int i = 0; i < textList.size(); i++) {
			SVGElement text = textList.get(i);
			isBoldList.add(fontNormalizer.isBoldColor(text.getFill()));
		}
		Assert.assertEquals("isBold", "[false, false, false, false]", isBoldList.toString());
		
		isBoldList = new ArrayList<Boolean>();
		fontNormalizer.setBoldThreshold(0x23);
		for (int i = 0; i < textList.size(); i++) {
			SVGElement text = textList.get(i);
			isBoldList.add(fontNormalizer.isBoldColor(text.getFill()));
		}
		Assert.assertEquals("isBold", "[false, false, false, false]", isBoldList.toString());
		
		isBoldList = new ArrayList<Boolean>();
		fontNormalizer.setBoldThreshold(0x24);
		for (int i = 0; i < textList.size(); i++) {
			SVGElement text = textList.get(i);
			isBoldList.add(fontNormalizer.isBoldColor(text.getFill()));
		}
		Assert.assertEquals("isBold", "[true, true, true, false]", isBoldList.toString());
	}
}
