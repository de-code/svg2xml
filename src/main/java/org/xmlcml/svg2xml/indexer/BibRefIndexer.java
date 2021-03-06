package org.xmlcml.svg2xml.indexer;

import java.util.ArrayList;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.xmlcml.graphics.svg.SVGG;
import org.xmlcml.svg2xml.page.ChunkAnalyzer;
import org.xmlcml.svg2xml.pdf.ChunkId;
import org.xmlcml.svg2xml.pdf.PDFIndex;
import org.xmlcml.svg2xml.table.TableOld;

/**
 * @author pm286
 *
 */
public class BibRefIndexer extends AbstractIndexer {
	private static final Logger LOG = Logger.getLogger(BibRefIndexer.class);
	public static final Pattern PATTERN = Pattern.compile("^[Rr][Ee][Ff][Ee][Rr][Ee][Nn][Cc][Ee][Ss].*", Pattern.DOTALL);
	public static final String TITLE = "REFERENCES";
	
	public BibRefIndexer() {
		super();
	}
	
	public BibRefIndexer(PDFIndex pdfIndex) {
		super(pdfIndex);
	}

	
	public void analyze() {
	}
	
	public Integer indexAndLabelChunk(String content, ChunkId id, Set<ChunkId> usedChunkSet) {
		Integer serial = super.indexAndLabelChunk(content, id);
		// index...
		return serial;
	}
	
	/** Pattern for the content for this analyzer
	 * 
	 * @return pattern (default null)
	 */
	@Override
	protected Pattern getPattern() {
		return PATTERN;
	}

	/** (constant) title for this analyzer
	 * 
	 * @return title (default null)
	 */
	@Override
	public String getTitle() {
		return TITLE;
	}

}
