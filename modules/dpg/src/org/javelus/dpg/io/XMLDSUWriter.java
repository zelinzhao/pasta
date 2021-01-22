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
package org.javelus.dpg.io;

import static org.javelus.DSUSpecConstants.CLASSLOADER_ID_ATT;
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

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.javelus.ClassUpdateType;
import org.javelus.FieldUpdateType;
import org.javelus.MethodUpdateType;
import org.javelus.dpg.model.DSU;
import org.javelus.dpg.model.DSUClass;
import org.javelus.dpg.model.DSUField;
import org.javelus.dpg.model.DSUMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author tiger
 * 
 */
public class XMLDSUWriter {
    
    private static boolean outputNew = false;
    
    Document document;

    protected void outputDeletedClasses(DSU update, Element classLoaderElement) {
        List<DSUClass> deletedClass = update.getDeletedClasses();
        for (DSUClass klass : deletedClass) {
            if (klass.isLibraryClass()) {
                throw new RuntimeException("Should not see deleted class here");
            }
            if (klass.isLoaded() && klass.isChanged()) {
                classLoaderElement.appendChild(class2xml(klass, true));
            }
        }
    }
    
    /**
     * @param update
     * @return an XML element
     */
    private Element update2xml(DSU update) {
        Element updateElement = createDSUElement("update");

        Element classLoaderElement = createDSUElement(DSUCLASSLOADER_TAG);
        classLoaderElement.setAttribute(CLASSLOADER_ID_ATT, "");
        updateElement.appendChild(classLoaderElement);



        Iterator<DSUClass> it = update.getSortedNewClasses();
        while (it.hasNext()) {
            DSUClass klass = it.next();
            DSUClass old = klass.getOldVersion();
            if (old == null) {
                if (outputNew) {
                    // this is a new added class
                    classLoaderElement.appendChild(class2xml(klass, false));
                }
            } else if (old.isLoaded() && old.isChanged()) {
                classLoaderElement.appendChild(class2xml(old, true));
            }
        }

        return updateElement;
    }

    /**
     * @param klass
     * @return an XML element
     */
    private Element class2xml(DSUClass klass, boolean isOld) {
        Element classElement = createDSUElement(DSUCLASS_TAG);

        classElement.setAttribute(CLASS_NAME_ATT, klass.getName().replace('.', '/'));
        classElement.setAttribute(CLASS_UPDATE_TYPE_ATT, klass.getChangeType().toString());

        //
        if (klass.needReloadClass()) {
            Element fileElement = createDSUElement(FILE_TAG);
            fileElement.setTextContent(klass.getNewVersion().getClassFile().toExternalForm());
            classElement.appendChild(fileElement);
        } else if (klass.getChangeType() == ClassUpdateType.ADD) {
            Element fileElement = createDSUElement(FILE_TAG);
            fileElement.setTextContent(klass.getClassFile().toExternalForm());
            classElement.appendChild(fileElement);
        }

        if (isOld) {
            DSUClass newVersion = klass.getNewVersion();
            DSUMethod[] methods = klass.getDeclaredMethods();

            if (methods != null) {
                for (DSUMethod m : methods) {
                    classElement.appendChild(method2xml(m, true));
                }
            }

            if (newVersion != null) {
                methods = newVersion.getDeclaredMethods();

                if (methods != null) {
                    for (DSUMethod m : methods) {
                        Element e = method2xml(m, false);
                        if (e != null) {
                            classElement.appendChild(e);
                        }
                    }
                }
            }

            DSUField[] fields = klass.getDeclaredFields();
            if (fields != null) {
                for (DSUField f : fields) {
                    classElement.appendChild(field2xml(f, true));
                }
            }

            if (newVersion != null) {
                fields = newVersion.getDeclaredFields();
                if (fields != null) {
                    for (DSUField f : fields) {
                        Element e = field2xml(f, false);
                        if (e != null) {
                            classElement.appendChild(e);
                        }
                    }
                }
            }
        } else { // new added class
            DSUMethod[] methods = klass.getDeclaredMethods();

            if (methods != null) {
                for (DSUMethod m : methods) {
                    Element e = method2xml(m, false);
                    if (e != null) {
                        classElement.appendChild(e);
                    }
                }
            }

            DSUField[] fields = klass.getDeclaredFields();
            if (fields != null) {
                for (DSUField f : fields) {
                    Element e = field2xml(f, false);
                    if (e != null) {
                        classElement.appendChild(e);
                    }
                }
            }
        }

        return classElement;
    }

    /**
     * @param method
     * @return an XML element
     */
    private Element method2xml(DSUMethod method, boolean isOld) {
        Element methodElement = createDSUElement(DSUMETHOD_TAG);

        if (isOld) {
            methodElement.setAttribute(METHOD_NAME_ATT, method.getName());
            methodElement.setAttribute(METHOD_DESC_ATT, method.getDescriptor());
            methodElement.setAttribute(METHOD_UPDATE_TYPE_ATT, method.getMethodUpdateType().toString());
            methodElement.setAttribute(METHOD_STATIC_ATT, String.valueOf(method.isStatic()));

        } else {
            if (method.hasOldVersion()) {
                return null;
            }
            methodElement.setAttribute(METHOD_NAME_ATT, method.getName());
            methodElement.setAttribute(METHOD_DESC_ATT, method.getDescriptor());
            methodElement.setAttribute(METHOD_UPDATE_TYPE_ATT, MethodUpdateType.ADD.toString());
            methodElement.setAttribute(METHOD_STATIC_ATT, String.valueOf(method.isStatic()));
        }

        return methodElement;
    }

    private Element field2xml(DSUField field, boolean isOld) {
        Element fieldElement = createDSUElement(DSUFIELD_TAG);

        if (isOld) {
            fieldElement.setAttribute(FIELD_NAME_ATT, field.getName());
            fieldElement.setAttribute(FIELD_DESC_ATT, field.getDescriptor());
            if (field.hasNewVersion()) {
                fieldElement.setAttribute(FIELD_UPDATE_TYPE_ATT, FieldUpdateType.NONE.name());
            } else {
                fieldElement.setAttribute(FIELD_UPDATE_TYPE_ATT, FieldUpdateType.DEL.name());
            }
            fieldElement.setAttribute(FIELD_STATIC_ATT, String.valueOf(field.isStatic()));
        } else {
            if (field.hasOldVersion()) {
                return null;
            }

            fieldElement.setAttribute(FIELD_NAME_ATT, field.getName());
            fieldElement.setAttribute(FIELD_DESC_ATT, field.getDescriptor());
            fieldElement.setAttribute(FIELD_UPDATE_TYPE_ATT, FieldUpdateType.ADD.name());
            fieldElement.setAttribute(FIELD_STATIC_ATT, String.valueOf(field.isStatic()));
        }

        return fieldElement;
    }

    private Element createDSUElement(String tagName) {
        return document.createElement(tagName);
    }

    /**
     * @param update
     * @param output
     */
    public void write(DSU update, OutputStream output) {
        try {
            final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            builderFactory.setValidating(false);
            DocumentBuilder builder = builderFactory.newDocumentBuilder();

            document = builder.newDocument();
            document.appendChild(update2xml(update));

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);

            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            //transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new OutputStreamWriter(output));
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // new DSUWriter().writer();
    }
}
