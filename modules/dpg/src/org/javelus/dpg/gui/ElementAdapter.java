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
package org.javelus.dpg.gui;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.w3c.dom.Element;

public class ElementAdapter {

    static Icon CLASS_ICON;
    static Icon METHOD_ICON;
    static Icon FIELD_ICON;
    static Icon FILE_ICON;
    static Icon CLASSLOADER_ICON;

    static {
        URL url = ElementAdapter.class.getResource("resources/class_obj.gif");
        CLASS_ICON = new ImageIcon(url);
        url = ElementAdapter.class.getResource("resources/methpub_obj.gif");
        METHOD_ICON = new ImageIcon(url);
        url = ElementAdapter.class
                .getResource("resources/field_public_obj.gif");
        FIELD_ICON = new ImageIcon(url);
        url = ElementAdapter.class.getResource("resources/file_obj.gif");
        FILE_ICON = new ImageIcon(url);
        url = ElementAdapter.class.getResource("resources/package_obj.gif");
        CLASSLOADER_ICON = new ImageIcon(url);

    }

    public static XMLTreeNode getElement(Element e) {
        if (e.getNodeName().equals("class")) {
            return new ClassXMLTreeNode(e);
        } else if (e.getNodeName().equals("method")) {
            return new MethodXMLTreeNode(e);
        } else if (e.getNodeName().equals("classloader")) {
            return new ClassLoaderXMLTreeNode(e);
        } else if (e.getNodeName().equals("field")) {
            return new FieldXMLTreeNode(e);
        } else if (e.getNodeName().equals("file")) {
            return new FileXMLTreeNode(e);
        }
        return new XMLTreeNode(e);
    }

    public static Icon getIcon(Object value) {
        if (value instanceof ClassXMLTreeNode) {
            return CLASS_ICON;
        } else if (value instanceof MethodXMLTreeNode) {
            return METHOD_ICON;
        } else if (value instanceof FieldXMLTreeNode) {
            return FIELD_ICON;
        } else if (value instanceof FileXMLTreeNode) {
            return FILE_ICON;
        } else if (value instanceof ClassLoaderXMLTreeNode) {
            return CLASSLOADER_ICON;
        }
        return null;
    }
}
