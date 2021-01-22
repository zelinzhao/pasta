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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.prefs.Preferences;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.javelus.dpg.DynamicPatchGenerator;
import org.javelus.dpg.io.PlainTextDSUWriter;
import org.javelus.dpg.io.XMLDSUWriter;
import org.javelus.dpg.model.DSU;
import org.javelus.dpg.transformer.TemplateClassGenerator;
import org.w3c.dom.Document;

@SuppressWarnings({"serial", "rawtypes", "unchecked"})
public class MainFrame extends JFrame implements ActionListener {

    private JButton openOldPath;
    private JButton openNewPath;
    private JButton justRun;
    private JButton runAll;

    private JList oldPathList;
    private JList newPathList;

    // private JTextField oldPath;
    // private JTextField newPath;

    private XMLTreePanel outputPanel;
    private JMenuItem removePath;

    public MainFrame() {
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setTitle("Run Dynamic Patch Generator!");

        JPanel contentPane = (JPanel) this.getContentPane();

        contentPane.setLayout(new GridBagLayout());
        JScrollPane jsp = null;

        GridBagConstraints c = createConstraints(0, 0, 1);
        // oldPath = createPathText();
        // c.weightx = 1;
        // contentPane.add(oldPath,c);

        openOldPath = createButton("Open Old Path");
        c = createConstraints(0, 0, 1);
        contentPane.add(openOldPath, c);

        final JPopupMenu menu = new JPopupMenu();
        removePath = new JMenuItem("Remove");
        removePath.addActionListener(this);
        menu.add(removePath);

        MouseAdapter popMenuListener = new MouseAdapter() {
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
        };

        openNewPath = createButton("Open New Path");
        c = createConstraints(1, 0, 1);
        contentPane.add(openNewPath, c);

        oldPathList = new JList();
        oldPathList.setModel(new DefaultListModel());
        oldPathList.addMouseListener(popMenuListener);
        jsp = new JScrollPane(oldPathList);
        jsp.setPreferredSize(new Dimension(200, 200));
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        c = createConstraints(0, 1, 1);
        c.weightx = 1;
        c.weighty = 1;
        contentPane.add(jsp, c);

        newPathList = new JList();
        newPathList.setModel(new DefaultListModel());
        newPathList.addMouseListener(popMenuListener);

        jsp = new JScrollPane(newPathList);
        jsp.setPreferredSize(new Dimension(200, 200));
        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        c = createConstraints(1, 1, 1);
        c.weightx = 1;
        c.weighty = 1;
        contentPane.add(jsp, c);

        outputPanel = new XMLTreePanel();
        c = createConstraints(0, 2, 2);
        c.weightx = 1;
        c.weighty = 5;
        contentPane.add(/* jsp */outputPanel, c);

        runAll = createButton("Run All");
        c = createConstraints(0, 3, 1);
        contentPane.add(runAll, c);

        justRun = createButton("Run Analysis");
        c = createConstraints(1, 3, 1);
        contentPane.add(justRun, c);

        // this.pack();

        Toolkit toolkit = this.getToolkit();
        Dimension size = toolkit.getScreenSize();
        this.setBounds(size.width / 4, size.height / 4, size.width / 2,
                size.height / 2);
        this.setVisible(true);
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

    private JButton createButton(String name) {

        JButton button = new JButton(name);
        button.addActionListener(this);

        return button;
    }

    public static void main(String[] args) {
        new MainFrame();
    }

    private void setDocument(String text) {
        Document document = null;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbFactory.newDocumentBuilder();
            document = builder.parse(new ByteArrayInputStream(text.getBytes()));
            document.normalize();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        outputPanel.setDocument(document);
    }

    static String getPathList(JList list) {
        DefaultListModel model = (DefaultListModel) list.getModel();
        int size = model.getSize();
        if (size > 0) {
            String pathList = "";
            for (int i = 0; i < size; i++) {
                pathList += model.get(i).toString() + File.pathSeparator;
            }
            return pathList;
        }
        return null;
    }

    String getOldPath() {
        return getPathList(oldPathList);
    }

    String getNewPath() {
        return getPathList(newPathList);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub
        Object src = e.getSource();

        if (src == justRun || src == runAll) {
            String oldPath = getOldPath();
            String newPath = getNewPath();
            if (oldPath == null || newPath == null) {
                return;
            }
            File outputDir = null;
            if (src == runAll) {
                outputDir = selectDirectory("/org/javelus/upt/gui/output");
                if (outputDir == null) {
                    return;
                }
            }

            if (newPath.length() != 0 && oldPath.length() != 0) {
                System.out.println("Old path:" + oldPath);
                System.out.println("New path:" + newPath);
                try {
                    DSU update = DynamicPatchGenerator.createUpdate(oldPath,
                            newPath);
                    update.computeUpdateInformation();
                    XMLDSUWriter writer = new XMLDSUWriter();
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    writer.write(update, output);
                    String oldText = output.toString();
                    setDocument(oldText);
                    //setInfo();

                    PlainTextDSUWriter hotspot = new PlainTextDSUWriter();
                    output.reset();
                    hotspot.write(update, output);
                    String text = output.toString();

                    System.out.println(text);
                    
                    if (src == runAll) {
                        {
                            File javelus = new File(outputDir, "javelus.dsu");
                            System.out.println("Write javelus.dsu to file: "
                                    + javelus.getAbsolutePath());
                            FileOutputStream fos = new FileOutputStream(javelus);
                            fos.write(text.getBytes());
                            fos.close();
                        }
                        {
                            File oldjavelus = new File(outputDir,
                                    "javelus.xml");
                            System.out.println("Write javelus.xml to file: "
                                    + oldjavelus.getAbsolutePath());
                            FileOutputStream oldfos = new FileOutputStream(
                                    oldjavelus);
                            oldfos.write(oldText.getBytes());
                            oldfos.close();
                        }
                        TemplateClassGenerator.generate(update, outputDir);
                    }

                    dispose(update);
                    
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
                return;
            }
            JOptionPane.showMessageDialog(
                            this,
                            String.format(
                                    "Some path is null.\nold path is \"%s\".\nnew path is \"%s\".\n",
                                    oldPath, newPath));
            return;
        } else if (src == openOldPath) {
            File[] lastPath = selectPath("/org/javelus/upt/gui/oldpath");
            if (lastPath == null) {
                return;
            }
            DefaultListModel model = (DefaultListModel) oldPathList.getModel();
            for (File f : lastPath) {
                model.addElement(f.getAbsolutePath());
            }
        } else if (src == openNewPath) {
            File[] lastPath = selectPath("/org/javelus/upt/gui/newpath");
            if (lastPath == null) {
                return;
            }
            DefaultListModel model = (DefaultListModel) newPathList.getModel();
            for (File f : lastPath) {
                model.addElement(f.getAbsolutePath());
            }
        } else if (src == removePath) {
            JPopupMenu menu = (JPopupMenu) removePath.getParent();
            JList list = (JList) menu.getInvoker();
            int[] indices = list.getSelectedIndices();

            if (indices.length >= 0) {
                DefaultListModel model = (DefaultListModel) list.getModel();
                model.removeRange(indices[0], indices[indices.length - 1]);
            }

        }

    }

    private void dispose(DSU update) {
//        closeFileSystems(update.getNewStore().getClassPath());
//        closeFileSystems(update.getOldStore().getClassPath());
    }
    
//    private void closeFileSystems(Path [] paths){
//        for(Path path:paths){
//            try {
//                path.getFileSystem().close();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }

	File selectDirectory(String prefKey) {
        Preferences pref = Preferences.userRoot().node(prefKey);
        String lastPath = pref.get("lastPath", "");
        JFileChooser chooser = null;
        if (lastPath.length() > 0) {
            chooser = new JFileChooser(lastPath);
        } else {
            chooser = new JFileChooser();
        }
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        lastPath = chooser.getSelectedFile().getAbsolutePath();
        pref.put("lastPath", lastPath);

        return chooser.getSelectedFile();
    }

    File[] selectPath(String prefKey) {
        Preferences pref = Preferences.userRoot().node(prefKey);
        String lastPath = pref.get("lastPath", "");
        JFileChooser chooser = null;
        if (lastPath.length() > 0) {
            chooser = new JFileChooser(lastPath);
        } else {
            chooser = new JFileChooser();
        }
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        lastPath = chooser.getSelectedFiles()[0].getAbsolutePath();
        pref.put("lastPath", lastPath);
        return chooser.getSelectedFiles();
    }
}
