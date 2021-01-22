/*
 * Copyright (C) 2012  Tianxiao Gu. All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * 
 * Please contact Institute of Computer Software, Nanjing University, 
 * 163 Xianlin Avenue, Nanjing, Jiangsu Provience, 210046, China,
 * or visit moon.nju.edu.cn if you need additional information or have any
 * questions.
 */
package org.javelus.impl;

import static org.javelus.DSUSpecConstants.CLASS_NAME_ATT;
import static org.javelus.DSUSpecConstants.CLASS_UPDATE_TYPE_ATT;
import static org.javelus.DSUSpecConstants.DSUCLASSLOADER_TAG;
import static org.javelus.DSUSpecConstants.DSUCLASS_TAG;
import static org.javelus.DSUSpecConstants.DSUFIELD_TAG;
import static org.javelus.DSUSpecConstants.DSUMETHOD_TAG;
import static org.javelus.DSUSpecConstants.FIELD_DESC_ATT;
import static org.javelus.DSUSpecConstants.FIELD_NAME_ATT;
import static org.javelus.DSUSpecConstants.FIELD_STATIC_ATT;
import static org.javelus.DSUSpecConstants.FIELD_UPDATE_TYPE_ATT;
import static org.javelus.DSUSpecConstants.FILE_TAG;
import static org.javelus.DSUSpecConstants.METHOD_DESC_ATT;
import static org.javelus.DSUSpecConstants.METHOD_NAME_ATT;
import static org.javelus.DSUSpecConstants.METHOD_STATIC_ATT;
import static org.javelus.DSUSpecConstants.METHOD_UPDATE_TYPE_ATT;
import static org.javelus.DSUSpecConstants.TRANSFORMER_TAG;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.javelus.ClassUpdateType;
import org.javelus.DSU;
import org.javelus.DSUClass;
import org.javelus.DSUClassLoader;
import org.javelus.DSUField;
import org.javelus.DSUMethod;
import org.javelus.FieldUpdateType;
import org.javelus.MethodUpdateType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * read a XML-based DSU-specification file
 * 
 */
public class DSUSpecReader {

    private boolean readFile = false;

    private DSUSpecReader(boolean readFile) {
        this.readFile =  readFile;
    }

    /**
     * helper method
     * 
     * @param parentElement
     * @param nodeName
     * @return a child element
     */
    private Element childByLocalName(Element parentElement, String nodeName) {
        NodeList children = parentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (nodeName.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    /**
     * Helper method
     * 
     * @param parentElement
     * @param nodeName
     * @return list of child element
     */
    private List<Element> childrenByLocalName(Element parentElement,
            String nodeName) {
        List<Element> list = new LinkedList<Element>();
        NodeList children = parentElement.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (nodeName.equals(node.getNodeName())) {
                list.add((Element) node);
            }
        }
        return list;
    }

    public static DSU read(String filePath) 
            throws ParserConfigurationException, SAXException, IOException {
        return read(filePath, false);
    }

    /**
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */

    public static DSU read(String filePath, boolean readFile)
            throws ParserConfigurationException, SAXException, IOException {
        if (filePath == null) {
            throw new NullPointerException("file path should not be null!");
        }
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        FileInputStream fis = new FileInputStream(filePath);
        return new DSUSpecReader(readFile).xml2DSU(builder.parse(fis));
    }

    /**
     * @param document
     * @return DSU
     * @throws IOException
     */
    private DSU xml2DSU(Document document) throws IOException {
        Element dsuElement = document.getDocumentElement();

        DSU dsu = null;
        List<Element> cls = childrenByLocalName(dsuElement, DSUCLASSLOADER_TAG);
        int size = cls.size();

        if (size > 0) {
            List<DSUClassLoader> classloaders = new ArrayList<DSUClassLoader>(size);
            for (int i = 0; i < size; i++) {
                classloaders.add(xml2DSUClassLoader(cls.get(i)));
            }
            dsu = new DSUImpl(classloaders);
        } else {
            dsu = new DSUImpl();
        }
        return dsu;
    }

    /**
     * parse a class element
     * 
     * @param classElement
     * @return a dsuclass
     */
    private DSUClass xml2DSUClass(Element classElement) {
        if (classElement == null) {
            throw new NullPointerException("Field Element should not be null!");
        }
        // 1).parse name
        String name = classElement.getAttribute(CLASS_NAME_ATT);

        // 2).parse change type
        ClassUpdateType updateType = ClassUpdateType.valueOf(classElement.getAttribute(CLASS_UPDATE_TYPE_ATT));

        // 3). parse methods
        List<Element> methodElement = childrenByLocalName(classElement, DSUMETHOD_TAG);
        int size = methodElement.size();
        List<DSUMethod> methods = new ArrayList<DSUMethod>(size);
        for (int i = 0; i < size; i++) {
            methods.add(xml2DSUMethod(methodElement.get(i)));
        }

        // 4). parse fields
        List<Element> fieldElement = childrenByLocalName(classElement, DSUFIELD_TAG);
        size = fieldElement.size();
        List<DSUField> fields = new ArrayList<DSUField>(size);
        for (int i = 0; i < size; i++) {
            fields.add(xml2DSUField(fieldElement.get(i)));
        }

        Element fileElement = childByLocalName(classElement, FILE_TAG);
        byte[] classBytes = xml2File(fileElement);

        return new DSUClassImpl(updateType, name, methods, fields, classBytes);
    }

    private DSUField xml2DSUField(Element fieldElement) {
        if (fieldElement == null) {
            throw new NullPointerException("Field Element should not be null!");
        }

        String name = fieldElement.getAttribute(FIELD_NAME_ATT);
        String signature = fieldElement.getAttribute(FIELD_DESC_ATT);
        boolean isStatic = Boolean.valueOf(fieldElement.getAttribute(FIELD_STATIC_ATT));
        FieldUpdateType updateType = FieldUpdateType.valueOf(fieldElement.getAttribute(FIELD_UPDATE_TYPE_ATT));
        return new DSUFieldImpl(updateType, name, signature, isStatic);
    }

    private DSUClass xml2Transformer(Element classElement) {
        if (classElement == null) {
            return null;
        }
        // 1).parse name
        String name = classElement.getAttribute(CLASS_NAME_ATT);

        Element fileElement = childByLocalName(classElement, FILE_TAG);
        byte[] classBytes = xml2File(fileElement);

        return new DSUClassImpl(name, classBytes);
    }

    private byte[] xml2File(Element fileElement) {
        if (fileElement == null) {
            return null;
        }

        if (!readFile) {
            return null;
        }

        String filePath = fileElement.getTextContent().trim();
        try {
            URL url = new URL(filePath);
            BufferedInputStream in = new BufferedInputStream(url.openStream());

            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);

            byte[] tmp = new byte[1024];
            int size = 0;
            while ((size = in.read(tmp)) != -1) {
                out.write(tmp, 0, size);
            }
            in.close();
            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Fail to read file element at " + filePath);
        }
        return null;
    }

    /**
     * parse a classloader
     * 
     * @param clElement
     * @return a classloader
     */
    private DSUClassLoader xml2DSUClassLoader(Element clElement) {
        if (clElement == null) {
            return null;
        }

        Element transformerElement = childByLocalName(clElement, TRANSFORMER_TAG);
        DSUClass transformer = xml2Transformer(transformerElement);

        String id = clElement.getAttribute("id");
        String loaderId = clElement.getAttribute("lid");

        // 3). parse dsu class
        List<Element> classesElement = childrenByLocalName(clElement, DSUCLASS_TAG);
        int size = classesElement.size();
        List<DSUClass> classes = new ArrayList<DSUClass>(size);
        for (int i = 0; i < size; i++) {
            classes.add(xml2DSUClass(classesElement.get(i)));
        }

        return new DSUClassLoaderImpl(id, loaderId, transformer, classes);
    }

    private DSUMethod xml2DSUMethod(Element methodElement) {
        if (methodElement == null) {
            throw new NullPointerException("Method Element should not be null!");
        }

        String name = methodElement.getAttribute(METHOD_NAME_ATT);
        String signature = methodElement.getAttribute(METHOD_DESC_ATT);
        boolean isStatic = Boolean.valueOf(methodElement.getAttribute(METHOD_STATIC_ATT));

        MethodUpdateType updateType = MethodUpdateType.valueOf(methodElement.getAttribute(METHOD_UPDATE_TYPE_ATT));
        return new DSUMethodImpl(updateType, name, signature, isStatic);
    }

}
