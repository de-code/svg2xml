package org.xmlcml.svg2xml.table;

import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.xmlcml.euclid.IntRange;
import org.xmlcml.euclid.util.MultisetUtil;
import org.xmlcml.svg2xml.text.HorizontalRuler;
import org.xmlcml.svg2xml.text.VerticalRuler;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

/**creates TableGrids from horizontal and vertical Rulers
 * 
 * @author pm286
 *
 */
public class TableGridFactory {
	private static final Logger LOG = Logger.getLogger(TableGridFactory.class);
	static {
		LOG.setLevel(Level.DEBUG);
	}

	private List<VerticalRuler> verticalRulerList;
	private List<HorizontalRuler> horizontalRulerList;
	private IntRange horizontalTotalRange;
	private IntRange verticalTotalRange;
	private TableGrid tableGrid;
	private Multiset<IntRange> horizontalIntRangeSet;
	private Multiset<IntRange> verticalIntRangeSet;
	private List<Multiset.Entry<IntRange>> horizontalIntRangeEntryList;
	private List<Multiset.Entry<IntRange>> verticalIntRangeEntryList;
	private int columnCount;
	private int rowCount;
	
	public TableGridFactory(List<HorizontalRuler> horizontalRulerList, List<VerticalRuler> verticalRulerList) {
		this.verticalRulerList = verticalRulerList;
		this.horizontalRulerList = horizontalRulerList;
	}

	public TableGrid getOrCreateTableGrid() {
		if (tableGrid == null) {
			tableGrid = null;
			createHorizontalIntRanges();
			createVerticalIntRanges();
			LOG.debug("Range: "+horizontalTotalRange+" / "+verticalTotalRange);
			createSimpleGrid();
		}
		return tableGrid;
	}

	private void createSimpleGrid() {
		columnCount = horizontalIntRangeEntryList.size();
		rowCount = verticalIntRangeEntryList.size();
		boolean matched = true;
		for (Multiset.Entry<IntRange> horizontalEntry : horizontalIntRangeEntryList) {
			int verticalCount = horizontalEntry.getCount();
			if (verticalCount != rowCount + 1) { 
				LOG.debug("non-simple table: horizontal "+horizontalEntry.getElement()+" count("+verticalCount+") should be vertical "+(rowCount + 1));
				LOG.debug(horizontalEntry);
				matched = false;
			}
		}
		for (Multiset.Entry<IntRange> verticalEntry : verticalIntRangeEntryList) {
			int horizontalCount = verticalEntry.getCount();
			if (horizontalCount != columnCount + 1) { 
				LOG.debug("non-simple table: vertical "+verticalEntry.getElement()+" count("+horizontalCount+") should be horizontal "+(columnCount + 1));
				LOG.debug(verticalEntry);
				matched = false;
			}
		}
		if (!matched) {
			LOG.debug("horizontal "+horizontalIntRangeEntryList);
			LOG.debug("vertical "+verticalIntRangeEntryList);
		}
		LOG.debug("SIMPLE TABLE: cols ("+columnCount+"), rows ("+rowCount+")");
	}

	private void createHorizontalIntRanges() {
		if (horizontalTotalRange == null) {
			horizontalIntRangeSet = HashMultiset.create();
			for (int i = 0; i < horizontalRulerList.size(); i++) {
				HorizontalRuler horizontalRuler = horizontalRulerList.get(i);
				IntRange horizontalRange = horizontalRuler.getIntRange();
				horizontalTotalRange = horizontalTotalRange == null ? horizontalRange : horizontalTotalRange.plus(horizontalRange);			
				horizontalIntRangeSet.add(horizontalRange);
			}
			horizontalIntRangeEntryList = IntRange.getIntRangeEntryListSortedByCount(horizontalIntRangeSet);
		}
	}
	
	private void createVerticalIntRanges() {
		if (verticalTotalRange == null) {
			verticalIntRangeSet = HashMultiset.create();
			for (int i = 0; i < verticalRulerList.size(); i++) {
				VerticalRuler verticalRuler = verticalRulerList.get(i);
				IntRange verticalRange = verticalRuler.getIntRange();
				verticalTotalRange = verticalTotalRange == null ? verticalRange : verticalTotalRange.plus(verticalRange);			
				verticalIntRangeSet.add(verticalRange);
			}
			verticalIntRangeEntryList = IntRange.getIntRangeEntryListSortedByCount(verticalIntRangeSet);
		}
	}

	public int getColumnCount() {
		return columnCount;
	}

	public int getRowCount() {
		return rowCount;
	}
	
	
	

}