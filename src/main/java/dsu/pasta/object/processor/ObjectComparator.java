package dsu.pasta.object.processor;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;
import org.xmlunit.diff.ElementSelectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import static dsu.pasta.utils.ZPrint.print;

public class ObjectComparator {
    public static HashMap<String, Node> fileNodes = new HashMap<>();
    private static DocumentBuilder dBuilder;
    private static XPath xpath;

    static {
        try {
            dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            xpath = XPathFactory.newInstance().newXPath();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private Diff diff;
    private boolean isSame = false;
    private boolean nullTransNode = false;
    private boolean nullNewNode = false;

    /**
     * @param isNewNew        boolean. True, compare new-new objects. false, compare
     *                        transformed-new objects
     * @param transformedFile
     * @param newFile
     * @param targetTag
     * @param onlyCompareTag
     */
    public ObjectComparator(boolean isNewNew, String transformedFile, String newFile, String targetTag,
                            String onlyCompareTag, boolean useSave) {
        if (transformedFile == null || newFile == null) {
            this.isSame = false;
            return;
        }
        try {
            Node transNode = null;
            if (useSave) {
                transNode = fileNodes.get(transformedFile);
            }
            if (transNode == null) {
                // transformed document only has the special node
                Document transDoc = dBuilder.parse(new File(transformedFile));
                transNode = getNodeForTargetTag(transDoc, "/*", onlyCompareTag);
                if (transNode != null && !transNode.getNodeName().equals("null"))
                    transNode = transDoc.renameNode(transNode, transNode.getNamespaceURI(), "root");
                if (useSave)
                    fileNodes.put(transformedFile, transNode);
            }
            /*******************************/
            Node newNode = null;
            if (useSave) {
                newNode = fileNodes.get(newFile);
            }
            {
                // get special node in new document
                Document newDoc = dBuilder.parse(new File(newFile));
                newNode = getNodeForTargetTag(newDoc, "/*/" + targetTag, onlyCompareTag);
                if (newNode != null)
                    newNode = newDoc.renameNode(newNode, newNode.getNamespaceURI(), "root");
                if (useSave)
                    fileNodes.put(newFile, newNode);
            }
//			if(newNode==null)

            if (transNode == null || transNode.getNodeName().equals("null"))
                this.nullTransNode = true;

            if (newNode == null)
                this.nullNewNode = true;
            //both null
            if (nullTransNode && nullNewNode) {
                this.isSame = true;
                return;
            }
            //one null;
            if (nullTransNode != nullNewNode) {
                this.isSame = false;
                return;
            }
            if (onlyCompareTag.contains("@") && transNode.toString().equals(newNode.toString())) {
                this.isSame = true;
                return;
            }
            // compare transSrc against new node
            this.diff = DiffBuilder.compare(Input.fromNode(transNode)).ignoreComments()
                    .withAttributeFilter(a -> !a.getName().equals("id") && !a.getName().equals("class") && !a.getName().equals("reference"))
                    .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byName)).withTest(Input.fromNode(newNode))
                    .ignoreWhitespace().build();
            this.isSame = !diff.hasDifferences();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String ne = "/home/BigData/subjects/icse21/ftpserver/32ed0b/dumpobjects/new/TransformerTest.TestCtx+testInit_object.xml.clean.xml";
        String tr = "/home/BigData/subjects/icse21/ftpserver/32ed0b/dumpobjects/test.xml";
        ObjectComparator oc = new ObjectComparator(false, tr, ne, "serverContext", "/connectionConfig/maxLogins", false);
        System.out.println(oc.isSame());
//		oc.showDifferencesAsString();
    }

    /**
     * get path from reference attribute
     *
     * @param node
     * @return
     */
    private String getPath(Node node) {
        if (node == null)
            return null;
        NamedNodeMap map = node.getAttributes();
        if (map == null)
            return null;
        Node attr = map.getNamedItem("reference");
        if (attr == null)
            return null;
        return attr.getNodeValue();
    }

    private Node getNodeForTargetTag(Document doc, String targetTag, String onlyTag) {
        try {
            XPathExpression expr = xpath.compile(targetTag);
            Node node = ((NodeList) expr.evaluate(doc, XPathConstants.NODESET)).item(0);
            if (node == null)
                return null;
            String reference = getPath(node);

            if (reference != null) {
                //an absolute path reference, resolve
                if (onlyTag != null && onlyTag.length() > 0)
                    return getNodeForTargetTag(doc, reference + onlyTag, null);
                else
                    return getNodeForTargetTag(doc, reference, null);
            } else {
                if (onlyTag != null && onlyTag.length() > 0)
                    return getNodeForTargetTag(doc, targetTag + onlyTag, null);
                else
                    return node;
            }
        } catch (XPathExpressionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isSame() {
        return this.isSame;
    }

    public void showDifferencesAsString() {
        if (this.isSame) {
            print("Transformed object and new object are same.");
            return;
        }
        Iterator<Difference> iter = diff.getDifferences().iterator();
        while (iter.hasNext()) {
            System.out.println(iter.next().toString());
        }
    }
}
