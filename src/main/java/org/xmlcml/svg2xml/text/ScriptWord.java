package org.xmlcml.svg2xml.text;

import java.util.ArrayList;
import java.util.List;

import org.xmlcml.graphics.svg.SVGText;
import org.xmlcml.svg2xml.page.TextAnalyzer;

/** a word in a ScriptLine
 * 
 * @author pm286
 *
 */
public class ScriptWord extends ScriptLine {
	
	private List<String> characterList;

	public ScriptWord(int nLines) {
		super(new TextStructurer((TextAnalyzer)null));
		textLineList = new ArrayList<TextLine>();
		for (int i = 0; i < nLines; i++) {
			textLineList.add(new TextLine());
		}
	}
	
	public void add(SVGText character, int line) {
		ensureCharacterList();
		if (line >= 0 && line < textLineList.size()) {
			textLineList.get(line).add(character);
		}
		characterList.add(character.getText());
	}
	
	private void ensureCharacterList() {
		if (characterList == null) {
			characterList = new ArrayList<String>();
		}
	}

	@Override
	public String summaryString() {
		StringBuilder sb = new StringBuilder();
		ensureCharacterList();
		for (String s : characterList) {
			sb.append(s);
		}
		return sb.toString();
	}
}
