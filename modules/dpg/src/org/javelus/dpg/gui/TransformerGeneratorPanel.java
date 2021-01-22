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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import org.javelus.dpg.DynamicPatchGenerator;
import org.javelus.dpg.transformer.TransformerGenerator;

@SuppressWarnings({"serial", "rawtypes", "unchecked"})
public class TransformerGeneratorPanel extends JPanel implements ActionListener {

    private JTextField outputDir;
	private JList pathList;
    private JButton addPath;
    private JButton setOutput;
    private JButton merge;
    private JMenuItem removePath;

	public TransformerGeneratorPanel() {
        setLayout(new GridBagLayout());

        GridBagConstraints c = null;

        outputDir = new JTextField();
        c = createConstraints(0, 0, 1);
        add(outputDir, c);

        pathList = new JList();
        DefaultListModel model = new DefaultListModel();
        pathList.setModel(model);

        JScrollPane jsp = new JScrollPane(pathList);
        jsp.setPreferredSize(new Dimension(400, 200));
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        c = createConstraints(0, 1, 1);
        c.weightx = 1;
        c.weighty = 1;
        add(jsp, c);

        JPanel botomBar = new JPanel();
        botomBar.setLayout(new FlowLayout());
        c = createConstraints(0, 2, 1);
        add(botomBar, c);
        addPath = new JButton("Add Path");
        addPath.addActionListener(this);
        botomBar.add(addPath, c);
        setOutput = new JButton("Set Output");
        setOutput.addActionListener(this);
        botomBar.add(setOutput, c);
        merge = new JButton("Merge");
        merge.addActionListener(this);
        botomBar.add(merge, c);

        final JPopupMenu menu = new JPopupMenu();
        removePath = new JMenuItem("Remove");
        removePath.addActionListener(this);
        menu.add(removePath);

        pathList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }

            public void mouseReleased(MouseEvent evt) {
                if (evt.isPopupTrigger()) {
                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }
        });
    }

    private GridBagConstraints createConstraints(int x, int y, int hspan) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = x; // Column 0
        c.gridy = y; // Row 0
        c.ipadx = 5; // Increases component width by 10 pixels
        c.ipady = 5; // Increases component height by 10 pixels
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = hspan;
        return c;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.add(new TransformerGeneratorPanel());

        frame.setSize(new Dimension(400, 400));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == merge) {
            DefaultListModel model = (DefaultListModel) pathList.getModel();
            int size = model.getSize();
            String parent = ".";
            if (outputDir.getText().length() > 0) {
                parent = outputDir.getText();
            }
            if (size > 0) {
                String path = "";
                for (int i = 0; i < size; i++) {
                    path += model.elementAt(0).toString();
                }
                try {
                    TransformerGenerator generator = DynamicPatchGenerator
                            .createGenerator(path);
                    generator.write(new File(parent));
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

        } else if (source == addPath) {
            Preferences pref = Preferences.userRoot().node("/dilspis/upt/gui");
            String lastPath = pref.get("lastPath", "");
            JFileChooser chooser = null;
            if (lastPath.length() > 0) {
                chooser = new JFileChooser(lastPath);
            } else {
                chooser = new JFileChooser();
            }
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            lastPath = chooser.getSelectedFile().getAbsolutePath();
            pref.put("lastPath", lastPath);
            ((DefaultListModel) pathList.getModel()).addElement(lastPath);
        } else if (source == setOutput) {
            Preferences pref = Preferences.userRoot().node("/dilspis/upt/gui");
            String lastPath = pref.get("lastPath", "");
            JFileChooser chooser = null;
            if (lastPath.length() > 0) {
                chooser = new JFileChooser(lastPath);
            } else {
                chooser = new JFileChooser();
            }
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            lastPath = chooser.getSelectedFile().getAbsolutePath();
            pref.put("lastPath", lastPath);
            outputDir.setText(lastPath);
        } else if (source == removePath) {
            int index = pathList.getSelectedIndex();
            if (index >= 0) {
                DefaultListModel model = (DefaultListModel) pathList.getModel();
                model.remove(index);
            }
        }
    }
}
