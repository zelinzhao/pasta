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

import java.util.Vector;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLTreeModel implements TreeModel {

    private Document document;
    Vector<TreeModelListener> listeners = new Vector<TreeModelListener>();

    public Document getDocument() {
        return document;
    }

    public void setDocument(Document document) {
        this.document = document;
        TreeModelEvent evt = new TreeModelEvent(this, new TreePath(getRoot()));
        for (TreeModelListener listener : listeners) {
            listener.treeStructureChanged(evt);
        }
    }

    public void addTreeModelListener(TreeModelListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeTreeModelListener(TreeModelListener listener) {
        listeners.remove(listener);
    }

    public Object getChild(Object parent, int index) {
        if (parent instanceof XMLTreeNode) {
            Vector<Element> elements = getChildElements(((XMLTreeNode) parent)
                    .getElement());
            return ElementAdapter.getElement(elements.get(index));
        } else {
            return null;
        }
    }

    public int getChildCount(Object parent) {
        if (parent instanceof XMLTreeNode) {
            Vector<Element> elements = getChildElements(((XMLTreeNode) parent)
                    .getElement());
            return elements.size();
        }
        return 0;
    }

    public int getIndexOfChild(Object parent, Object child) {
        if (parent instanceof XMLTreeNode && child instanceof XMLTreeNode) {
            Element pElement = ((XMLTreeNode) parent).getElement();
            Element cElement = ((XMLTreeNode) child).getElement();
            if (cElement.getParentNode() != pElement) {
                return -1;
            }
            Vector<Element> elements = getChildElements(pElement);
            return elements.indexOf(cElement);
        }
        return -1;
    }

    public Object getRoot() {
        if (document == null) {
            return null;
        }
        Vector<Element> elements = getChildElements(document);
        if (elements.size() > 0) {
            return new XMLTreeNode(elements.get(0));
        } else {
            return null;
        }
    }

    public boolean isLeaf(Object node) {
        if (node instanceof XMLTreeNode) {
            Element element = ((XMLTreeNode) node).getElement();
            Vector<Element> elements = getChildElements(element);
            return elements.size() == 0;
        } else {
            return true;
        }
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new UnsupportedOperationException();
    }

    private Vector<Element> getChildElements(Node node) {
        Vector<Element> elements = new Vector<Element>();
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            if (list.item(i).getNodeType() == Node.ELEMENT_NODE) {
                elements.add((Element) list.item(i));
            }
        }
        return elements;
    }
}
