package dsu.pasta.dpg;

import com.github.javaparser.resolution.types.ResolvedType;
import dsu.pasta.javaparser.factory.analyzer.JavaparserSolver;
import dsu.pasta.javassist.JavassistSolver;
import dsu.pasta.utils.ZFileUtils;
import dsu.pasta.utils.ZPrint;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ExtractProjectUpdatedInfoProcessor {

    public static HashSet<DpgClass> dpgClasses = new HashSet<>();

    public static void readUpdateInfo(String upinfoPath) {
        if (!ZFileUtils.fileExistNotEmpty(upinfoPath)) {
            ZPrint.info(upinfoPath + " is not exist. Exit.");
            System.exit(-1);
        }
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        Document doc = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new File(upinfoPath));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        doc.getDocumentElement().normalize();
        NodeList classList = doc.getElementsByTagName("class");

        DpgClass oneClass;
        for (int temp = 0; temp < classList.getLength(); temp++) {
            Node classNode = classList.item(temp);
            String classChange = classNode.getAttributes().getNamedItem("ut").getNodeValue();
            String className = classNode.getAttributes().getNamedItem("name").getNodeValue().replaceAll("\\/", "\\.");
            oneClass = new DpgClass(className);

            boolean addClass = false;
            NodeList fieldMethodList = classNode.getChildNodes();
            for (int i = 0; i < fieldMethodList.getLength(); i++) {
                Node node = fieldMethodList.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE)
                    continue;
                if (node.getNodeName().equals("field")) {
                    String fieldDesc = node.getAttributes().getNamedItem("desc").getNodeValue();
                    String fieldName = node.getAttributes().getNamedItem("name").getNodeValue();
                    String fieldChanged = node.getAttributes().getNamedItem("ut").getNodeValue();
                    if (fieldChanged.equals("DEL")
                            || fieldChanged.equals("ADD")
                            || fieldChanged.equals("BC")) {
                        addClass = true;
                        oneClass.addChangedField(fieldName, className, fieldDesc, fieldChanged);
                    } else {
                        oneClass.addSameField(fieldName, className, fieldDesc);
                    }
                } else if (node.getNodeName().equals("method")) {
                    String methodDesc = node.getAttributes().getNamedItem("desc").getNodeValue();
                    String methodName = node.getAttributes().getNamedItem("name").getNodeValue();
                    String methodChange = node.getAttributes().getNamedItem("ut").getNodeValue();
                    if (methodName.equals("<init>")) {
                        methodName = className.substring(className.lastIndexOf('.') + 1);
                    }
                    if (methodChange.equals("DEL")
                            || methodChange.equals("ADD")
                            || methodChange.equals("BC")) {
                        addClass = true;
                        oneClass.addChangedMethod(methodName, className, methodDesc, methodChange);
                    }
                }
            }
            if (addClass)
                dpgClasses.add(oneClass);
        }
    }

    public static void resolveChangedFieldTypes() {
        for (DpgClass cla : dpgClasses) {
            for (DpgField field : cla.getChangedFields()) {
                String type = JavassistSolver.getFieldTypeFullQualified(cla.getNameWith$(), field.getName());
                if (type != null)
                    field.setFullQualifiedType(type);
            }
        }
    }

    public static void resolveSameFieldTypes() {
        for (DpgClass cla : dpgClasses) {
            for (DpgField field : cla.getSameFields()) {
                String type = JavassistSolver.getFieldTypeFullQualified(cla.getNameWith$(), field.getName());
                if (type != null)
                    field.setFullQualifiedType(type);
            }
        }
    }

    private static DpgClass findClass(String className) {
        className = className.replaceAll("\\$", "\\.");
        for (DpgClass dpgCla : dpgClasses) {
            if (dpgCla.getNameWithout$().equals(className)) {
                return dpgCla;
            }
        }
        return null;
    }

    public static HashSet<String> getChangedFieldTypesOfChangedClass(String className) {
        DpgClass dpgCla = findClass(className);
        if (dpgCla == null)
            return new HashSet<String>();
        else
            return new HashSet<String>(dpgCla.getChangedFieldsTypes());
    }

    public static HashSet<String> getChangedFieldNamesOfChangedClass(String className) {
        DpgClass dpgCla = findClass(className);
        if (dpgCla == null)
            return new HashSet<String>();
        else
            return new HashSet<String>(dpgCla.getChangedFieldsNames());
    }

    /**
     * Is <tt>fieldName</tt> an added field to <tt>className</tt>
     *
     * @param className
     * @param fieldName
     * @return
     */
    public static boolean isAddedFieldInChangedClass(String className, String fieldName) {
        DpgClass dpgClass = findClass(className);
        if (dpgClass == null)
            return false;
        for (DpgField f : dpgClass.getChangedFields()) {
            if (f.getName().equals(fieldName) && f.isADD())
                return true;
        }
        return false;
    }

    public static HashSet<String> getSameFieldTypesOfChangedClass(String className) {
        DpgClass dpgCla = findClass(className);
        if (dpgCla == null)
            return new HashSet<String>();
        else
            return new HashSet<String>(dpgCla.getSameFieldsTypes());
    }

    public static HashSet<String> getSameFieldNamesOfChangedClass(String className) {
        DpgClass dpgCla = findClass(className);
        if (dpgCla == null)
            return new HashSet<String>();
        else
            return new HashSet<String>(dpgCla.getSameFieldsNames());
    }

    /**
     * @param className with/without <tt>$</tt>
     * @return <tt>null</tt> if this is not a changed class;
     */
    public static ResolvedType getChangedClassType(String className) {
        className = className.replaceAll("\\$", "\\.");
        for (DpgClass dpgCla : dpgClasses) {
            if (dpgCla.getNameWithout$().equals(className))
                return JavaparserSolver.getType(className);
        }
        return null;
    }

    public static boolean isChangedClass(String className) {
        className = className.replaceAll("\\$", "\\.");

        for (DpgClass dpgCla : dpgClasses) {
            if (dpgCla.getNameWithout$().equals(className))
                return true;
        }
        return false;
    }

    public static Set<String> getAllChangedClasses() {
        return dpgClasses.stream().map(dc -> dc.getNameWith$()).collect(Collectors.toSet());
    }
}
