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

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.w3c.dom.Document;

@SuppressWarnings("serial")
public class XMLTreePanel extends JPanel {

    private JTree tree;
    private XMLTreeModel model;

    public XMLTreePanel() {
        setLayout(new BorderLayout());

        model = new XMLTreeModel();
        tree = new JTree();
        tree.setModel(model);
        tree.setShowsRootHandles(true);
        tree.setEditable(false);

        tree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean sel, boolean expanded, boolean leaf,
                    int row, boolean hasFocus) {
                Component result = super.getTreeCellRendererComponent(tree,
                        value, sel, expanded, leaf, row, hasFocus);
                Icon icon = valueToIcon(value);
                if (result == this && icon != null) {
                    setIcon(icon);
                }
                return result;
            }

        });
        JScrollPane pane = new JScrollPane(tree);
        // pane.setPreferredSize(new Dimension(300,400));

        add(pane, "Center");

    }

    /* methods that delegate to the custom model */
    public void setDocument(Document document) {
        model.setDocument(document);
    }

    public Document getDocument() {
        return model.getDocument();
    }

    private static Icon valueToIcon(Object value) {
        return ElementAdapter.getIcon(value);
    }

}
