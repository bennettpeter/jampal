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
import pgbennett.id3.*;


public class ProgressBox implements ActionListener, Runnable {
    JDialog progressDialog;
    JLabel progresslabel;
    boolean cancel=false;
    boolean stop=false;
    String progressText;
    Component owner;
    boolean pending=false;


    ProgressBox(Dialog owner, String title) {
        progressDialog = new JDialog(owner, title);
        this.owner=owner;
        init();
    }

    ProgressBox(Frame owner, String title) {
        progressDialog = new JDialog(owner,title);
        this.owner=owner;
        init();
    }

    void init() {
        owner.setEnabled(false);
        progressDialog.setLocationRelativeTo(owner);
        progresslabel = new JLabel();
        progressDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4,4,4,4);
        c.weightx = 0.5;
        c.weighty = 0.5;
        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=1;
        panel.add(progresslabel,c);

        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.gridy=1;

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(true);
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand("progress-cancel");
        panel.add(cancelButton,c);
        progressDialog.setContentPane(panel);
        panel.setMinimumSize(new Dimension(300, 150));
        progressDialog.setSize(new Dimension(300, 150));
        progressDialog.setVisible(true);
    }

    void updateProgress() {
        if (pending)
            return;
        pending=true;
        SwingUtilities.invokeLater(this);
    }

    // This is run the the awt thread
    public void run() {
        progresslabel.setText(progressText);
        pending=false;
        if (stop) {
            cancel();
            stop=false;
        }
    }

    // Called from worker to stop the progress dialog
    void stop() {
        stop=true;
        SwingUtilities.invokeLater(this);
    }

    private void cancel() {
        cancel=true;
        progressDialog.dispose();
        owner.setEnabled(true);
        owner.setVisible(true);
    }

    // Buttons
    public void actionPerformed(ActionEvent e) {
        if ("progress-cancel".equals(e.getActionCommand()))
            cancel();
    }

}