package org.xmlcml.svgplus.command;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Comment;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;

import org.apache.log4j.Logger;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.svgplus.core.SemanticDocumentElement;
import org.xmlcml.svgplus.document.DocumentIteratorElement;
import org.xmlcml.svgplus.document.DocumentPageIteratorElement;
import org.xmlcml.svgplus.document.DocumentWriterElement;
import org.xmlcml.svgplus.figure.FigureAnalyzerElement;
import org.xmlcml.svgplus.page.BoxDrawerElement;
import org.xmlcml.svgplus.page.BoxProcessorElement;
import org.xmlcml.svgplus.page.ChunkAnalyzerElement;
import org.xmlcml.svgplus.page.ElementStylerElement;
import org.xmlcml.svgplus.page.NodeDeleterElement;
import org.xmlcml.svgplus.page.PageActionElement;
import org.xmlcml.svgplus.page.PageAnalyzerElement;
import org.xmlcml.svgplus.page.PageNormalizerElement;
import org.xmlcml.svgplus.page.PageVariableElement;
import org.xmlcml.svgplus.page.PageWriterElement;
import org.xmlcml.svgplus.page.PathNormalizerElement;
import org.xmlcml.svgplus.page.TextChunkerElement;
import org.xmlcml.svgplus.page.VariableExtractorElement;
import org.xmlcml.svgplus.page.WhitespaceChunkerElement;
import org.xmlcml.svgplus.paths.PathElement;

public abstract class AbstractActionElement extends Element {

	private static final Logger LOGX = Logger.getLogger(AbstractActionElement.class);
	
	public static final String ACTION = "action";
	public static final String DELETE_NAMESPACES = "deleteNamespaces";
	public static final String FILENAME = "filename";
	public static final String FORMAT = "format";
	public static final String MARK = "mark";
	public static final String DEBUG = "debug";
	public static final String COUNT = "count";
	public static final String LOG = "log";
	public static final String MESSAGE = "message";
	public static final String NAME = "name";
	public static final String OUT_DIR = "outDir";
	public static final String REGEX = "regex";
	public static final String SKIP_IF_EXISTS = "skipIfExists";
	public static final String TITLE = "title";
	public static final String XPATH = "xpath";
	public static final String MAX = "max";
	public static final String TIMEOUT = "timeout";
	
	protected SemanticDocumentElement semanticDocumentElement;
	protected AbstractAction abstractAction;

	/** constructor.
	 * 
	 * @param name
	 */
	public AbstractActionElement(String name) {
		super(name);
		init();
	}

	public AbstractActionElement(AbstractActionElement actionElement) {
		super(actionElement);
	}
	
	protected void init() {
		this.abstractAction = createAction();
	}
	
	public AbstractAction getAction() {
		return abstractAction;
	}

	/** check attributes */
	protected abstract List<String> getAttributeNames();
	protected abstract List<String> getRequiredAttributeNames();
	
	protected abstract AbstractAction createAction();

	public void checkAttributes() {
		List<String> allowedNames = getAttributeNames();
		if (allowedNames == null) {
			throw new RuntimeException("Must give some allowed attributes: "+this.getClass());
		}
		List<String> attNames = new ArrayList<String>();
		for (int i = 0; i < this.getAttributeCount(); i++) {
			String attName = this.getAttribute(i).getLocalName();
			if (!allowedNames.contains(attName)) {
				throw new RuntimeException("Unknown attribute : "+attName+" on "+this.getClass());
			}
			attNames.add(attName);
		}
		List<String> requiredNames = getRequiredAttributeNames();
		if (requiredNames != null) {
			for (String requiredName : requiredNames) {
				if (!attNames.contains(requiredName)) {
					throw new RuntimeException("Missing attribute : "+requiredName+" on "+this.getClass()+" // "+this.toXML());
				}
			}
		}
	}

	/** copy constructor from non-subclassed elements
	 */
	public static AbstractActionElement createActionElement(Element element) {
		AbstractActionElement newElement = null;
		String tag = element.getLocalName();
		LOGX.trace("TAG "+tag);
		if (tag == null || tag.equals("")) {
			throw new RuntimeException("no tag");
			
		} else if (tag.equals(DocumentIteratorElement.TAG)) {
			newElement = new DocumentIteratorElement();
			
		} else if (tag.equals(PageActionElement.TAG)) {
			newElement = new PageActionElement();
		} else if (tag.equals(PageAnalyzerElement.TAG)) {
			newElement = new PageAnalyzerElement();
		} else if (tag.equals(PathElement.TAG)) {
			newElement = new PathElement();
		} else if (tag.equals(SemanticDocumentElement.TAG)) {
			newElement = new SemanticDocumentElement();
			
		} else if (tag.equals(BreakElement.TAG)) {
			newElement = new BreakElement();
		} else if (tag.equals(DebugElement.TAG)) {
			newElement = new DebugElement();
		} else if (tag.equals(DocumentPageIteratorElement.TAG)) {
			newElement = new DocumentPageIteratorElement();
		} else if (tag.equals(DocumentWriterElement.TAG)) {
			newElement = new DocumentWriterElement();
			
		} else if (tag.equals(ChunkAnalyzerElement.TAG)) {
			newElement = new ChunkAnalyzerElement();
		} else if (tag.equals(BoxDrawerElement.TAG)) {
			newElement = new BoxDrawerElement();
		} else if (tag.equals(BoxProcessorElement.TAG)) {
			newElement = new BoxProcessorElement();
		} else if (tag.equals(NodeDeleterElement.TAG)) {
			newElement = new NodeDeleterElement();
		} else if (tag.equals(ElementStylerElement.TAG)) {
			newElement = new ElementStylerElement();
		} else if (tag.equals(FigureAnalyzerElement.TAG)) {
			newElement = new FigureAnalyzerElement();
		} else if (tag.equals(PageActionElement.TAG)) {
			throw new RuntimeException("PageActionElement is deprecated");
		} else if (tag.equals(PageAnalyzerElement.TAG)) {
			newElement = new PageAnalyzerElement();
		} else if (tag.equals(PageNormalizerElement.TAG)) {
			newElement = new PageNormalizerElement();
		} else if (tag.equals(PageWriterElement.TAG)) {
			newElement = new PageWriterElement();
		} else if (tag.equals(PathNormalizerElement.TAG)) {
			newElement = new PathNormalizerElement();
		} else if (tag.equals(TextChunkerElement.TAG)) {
			newElement = new TextChunkerElement();
		} else if (tag.equals(VariableExtractorElement.TAG)) {
			newElement = new VariableExtractorElement();
		} else if (tag.equals(WhitespaceChunkerElement.TAG)) {
			newElement = new WhitespaceChunkerElement();
			
		} else if (tag.equals(AssertElement.TAG)) {
			newElement = new AssertElement();
		} else if (tag.equals(PageVariableElement.TAG)) {
			newElement = new PageVariableElement();
			
//		} else if (tag.equals(SemanticDocumentElement.TAG)) {
//			newElement = new SemanticDocumentElement();
//			
		} else {
			throw new RuntimeException("unsupported element: "+tag);
		}
		if (newElement != null) {
			CMLUtil.copyAttributes(element, newElement);
	        createSubclassedChildren(element, newElement);
	        ((AbstractActionElement)newElement).checkAttributes();
		}
        return newElement;
		
	}
	
	protected static void createSubclassedChildren(Element oldElement, AbstractActionElement newElement) {
		if (oldElement != null) {
			for (int i = 0; i < oldElement.getChildCount(); i++) {
				Node node = oldElement.getChild(i);
				Node newNode = null;
				if (node instanceof Text) {
					newNode = new Text(node.getValue());
				} else if (node instanceof Comment) {
					newNode = new Comment(node.getValue());
				} else if (node instanceof ProcessingInstruction) {
					newNode = new ProcessingInstruction((ProcessingInstruction) node);
				} else if (node instanceof Element) {
					newNode = createActionElement((Element) node);
				} else {
					throw new RuntimeException("Cannot create new node: "+node.getClass());
				}
				newElement.appendChild(newNode);
			}
		}
	}

	public String getName() {
		return this.getAttributeValue(NAME);
	}

	public static AbstractActionElement createActionElement(File file) {
		AbstractActionElement actionElement = null;
		try {
			Element elem = new Builder().build(file).getRootElement();
			elem = replaceIncludesRecursively(file, elem);
			actionElement = AbstractActionElement.createActionElement(elem);
		} catch (Exception e) {
			throw new RuntimeException("Cannot read commandfile "+file, e);
		}
		return actionElement;
	}

	private static Element replaceIncludesRecursively(File file, Element elem) {
		Nodes includes = elem.query(".//"+IncludeElement.TAG);
		for (int i = 0; i < includes.size(); i++) {
			Element includeElement = (Element) includes.get(i);
			String includeFilename = includeElement.getAttributeValue(AbstractActionElement.FILENAME);
			if (includeFilename == null) {
				throw new RuntimeException("must give filename");
			}
			try {
				File includeFile = new File(file.getParentFile(), includeFilename).getCanonicalFile();
				Element includeContentElement = new Builder().build(includeFile).getRootElement();
				includeContentElement = replaceIncludesRecursively(includeFile, includeContentElement);
				includeElement.getParent().replaceChild(includeElement, includeContentElement.copy());
			} catch (Exception e) {
				throw new RuntimeException("Cannot create / parse includeFile "+file, e);
			}
		}
		return elem;
	}

	public SemanticDocumentElement getSemanticDocumentElement() {
		if (semanticDocumentElement == null) {
// find parentage after it is added to tree			
			Element element = (Element) this.query("/*").get(0);
			if (!(element instanceof SemanticDocumentElement)) {
//				throw new RuntimeException("root element must be <semanticDocument>, found: <"+element.getLocalName()+">");
			} else {
				semanticDocumentElement = (SemanticDocumentElement) element;
			}
		}
		return semanticDocumentElement;
	}
	
	public String getString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getLocalName()+"\n");
		for (int i = 0; i < this.getAttributeCount(); i++) {
			Attribute attribute = this.getAttribute(i);
			sb.append(" "+attribute.getLocalName()+"='"+attribute.getValue()+"'");
		}
		sb.append("\n");
		return sb.toString();
	}
	
	public void debug(String msg) {
		CMLUtil.debug(this, msg);
	}

	public DocumentIteratorElement getAncestorDocumentIteratorElement() {
		return (DocumentIteratorElement) getAncestorElement(DocumentIteratorElement.TAG);
	}

	public SemanticDocumentElement getAncestorSemanticDocumentElement() {
		return (SemanticDocumentElement) getAncestorElement(SemanticDocumentElement.TAG);
	}

	public AbstractActionElement getAncestorElement(String tag) {
		LOGX.trace(this.getLocalName());
		Nodes nodes = this.query("ancestor-or-self::"+tag+"[1]");
		if (nodes.size() != 1) {
			throw new RuntimeException("Must have ancestor:"+tag);
		}
		return (AbstractActionElement) nodes.get(0);
	}
}
