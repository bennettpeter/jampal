package pgbennett.jampal;

/*
    Copyright 2004 Peter Bennett

    This file is part of Jampal.

    Jampal is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Jampal is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Jampal.  If not, see <http://www.gnu.org/licenses/>.
*/

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;


public class FileListDialog implements ActionListener{
    int [] selectedRows;
    JList updateList;
    int fileCount;
    JDialog dialog;
    Component owner;
    String response;
    boolean isModal;
    JTextArea area;

    FileListDialog(Dialog owner, String title,String [] fileNames, boolean canCancel, 
        boolean isModal) {
        dialog = new JDialog(owner, title,isModal);
        this.owner=owner;
        this.isModal = isModal;
        init(fileNames,canCancel);
    }

    FileListDialog(Frame owner, String title,String [] fileNames, boolean canCancel, 
        boolean isModal) {
        dialog = new JDialog(owner,title,isModal);
        this.owner=owner;
        this.isModal = isModal;
        init(fileNames,canCancel);
    }

    void init(String [] fileNames, boolean canCancel) {

        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLocationRelativeTo(owner);

        // Create a JList that displays the strings in data[]
        StringBuffer buf = new StringBuffer();
        int ix;
        for (ix=0;ix<fileNames.length;ix++) {
            if (fileNames[ix]!=null && !"".equals(fileNames[ix])) {
                buf.append(fileNames[ix]);
                buf.append("\n");
            }
        }
        area = new JTextArea(buf.toString(),12,60);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        Font font = new Font ("Dialog", Font.PLAIN,11);
        area.setFont(font);
        area.setBackground(null);

        // JList doesn't support scrolling directly. To create a scrolling list 
        // you make the JList the viewport view of a JScrollPane. For example: 
        JScrollPane pane = new JScrollPane(area);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0.5;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.LINE_START;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=2;
        panel.add(pane,c);
        c.weighty = 0;

        if (isModal) {
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.NONE;
            c.insets = new Insets(4,4,4,4);
            JButton okButton = new JButton("OK");
            okButton.setEnabled(true);
            okButton.addActionListener(this);
            okButton.setActionCommand("ok");
            c.gridy=1;
            c.gridwidth=1;
            panel.add(okButton,c);
            dialog.getRootPane().setDefaultButton(okButton);
        }
        if (canCancel) {
            JButton cancelButton = new JButton("Cancel");
            cancelButton.setEnabled(true);
            cancelButton.addActionListener(this);
            cancelButton.setActionCommand("cancel");
            c.gridx=1;
            c.gridwidth=1;
            panel.add(cancelButton,c);
        }
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setVisible(true);
    }


    void update(String title, String [] fileNames) {
        dialog.setTitle(title);
        StringBuffer buf = new StringBuffer();
        int ix;
        for (ix=0;ix<fileNames.length;ix++) {
            if (fileNames[ix]!=null && !"".equals(fileNames[ix])) {
                buf.append(fileNames[ix]);
                buf.append("\n");
            }
        }
        area.setText(buf.toString());
    }

    // Buttons
    public void actionPerformed(ActionEvent e) {
        response = e.getActionCommand();
        dialog.dispose();
    }

}