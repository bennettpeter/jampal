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

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import pgbennett.id3.*;
import java.net.*;


public class TrackEditor implements ItemListener, ActionListener, Runnable{
    MainFrame mainFrame;
    JFrame dialog;
    JPanel widgetsPanel;
    // File being edited. null if none
    String fileName=null;
    boolean isDialogInitialized=false;
    boolean isDirty=false;
    LibraryAttributes attributes;
    TrackUpdater updater;

    JComboBox frameIdCombo;
    JButton createFrameButton;
    JButton updateButton;
    JButton refreshButton;
    JPanel refreshPush;
    JPanel pushUp;
    JCheckBox superTagCheckBox;
    JCheckBox titleCaseCheckBox;
    JTextField delimTextField;
    Color textBG;
    int numCboxChecked = 0;
//    Font font = new Font ("Dialog", Font.PLAIN,11);
    Font font;


    public static final String[] pictureTypes = {
        "Other",
        "32x32 pixels 'file icon' (PNG only)",
        "Other file icon",
        "Cover (front)",
        "Cover (back)",
        "Leaflet page",
        "Media (e.g. label side of CD)",
        "Lead artist/lead performer/soloist",
        "Artist/performer",
        "Conductor",
        "Band/Orchestra",
        "Composer",
        "Lyricist/text writer",
        "Recording Location",
        "During recording",
        "During performance",
        "Movie/video screen capture",
        "A bright coloured fish",
        "Illustration",
        "Band/artist logotype",
        "Publisher/Studio logotype" };


    public class Frame {
        JCheckBox checkBox;
        JComponent value;
        int colType;
        String id;
        JTextField descText;
        ID3v2Picture picture;
        JComboBox pictureType;
        JLabel langCode;
        // 3 language combo boxes
        JComboBox langCombo;
        JComboBox [] langValueCombo;
        JButton loadButton;
        JButton saveButton;
        JButton delButton;
        JLabel helpLabel;
        // 'T': Text
        // 'M': Multiline
        // 'I': picture
        // 'N': integer value
        // 'L': Language
        // 'B': Binary (PRIV frame)
        char dataType;
        int pictureWidth=-1;
        JPopupMenu popupMenu;
    }

    Vector <Frame> frames;
    HashMap checkboxFrames;
    HashMap idFrames;
    HashMap langComboFrames;
    static HashSet timeStampFrameIds;
    static final String [] timeStampFrameArray = {
        "TDEN","TDOR","TDRC","TDRL","TDTG" };
        

    GridBagConstraints constraints;

    HashSet createTimeStampFrameList() {
        HashSet set = new HashSet();
        for (int ix = 0; ix<timeStampFrameArray.length;ix++) {
            set.add(timeStampFrameArray[ix]);
        }
        return set;
    }

//    JScrollPane scrollPane;
    TrackEditor(MainFrame mainFrame) {
        if (timeStampFrameIds==null)
            timeStampFrameIds = createTimeStampFrameList();
        this.mainFrame = mainFrame;
        font=mainFrame.getSelectedFont();
        attributes = mainFrame.library.attributes;
//        dialog = new JDialog(mainFrame.frame,"Edit Track Tags",false);
        dialog = new JFrame("Edit Track Tags");
//        dialog.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
//        ImageIcon windowIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/notes.png"));
//        dialog.setIconImage(windowIcon.getImage());
        dialog.setIconImages(mainFrame.windowIcons);
        dialog.addWindowListener(
                new WindowListener() {

            @Override
            public void windowOpened(WindowEvent e) {
                
            }

            @Override
            public void windowClosing(WindowEvent e) {
                
            }

            @Override
            public void windowClosed(WindowEvent e) {
                
            }

            @Override
            public void windowIconified(WindowEvent e) {
                setTitle();
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
                setTitle();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                
            }
        });


        try {

//            JPanel contentPanel = new JPanel(new GridLayout());
            widgetsPanel = new JPanel(new GridBagLayout());
//            JPanel resizePanel = new HorizontalResizePanel(new GridLayout(),400);
//            resizePanel.add(widgetsPanel);
            JScrollPane scrollPane = new JScrollPane(widgetsPanel,
                    ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                    ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            widgetsPanel.setOpaque(true); //content panes must be opaque
            scrollPane.getVerticalScrollBar().setUnitIncrement(32);
            dialog.setContentPane(scrollPane);

            // Generic dialog controls

            frameIdCombo = new JComboBox(FrameDictionary.getSingleton().frameNames);
//            Dimension frameIdComboPrefSize = frameIdCombo.getPreferredSize();
//            frameIdComboPrefSize.width = frameIdComboPrefSize.width *3/4;
//            frameIdCombo.setPreferredSize(frameIdComboPrefSize);
            frameIdCombo.setSelectedIndex(-1);
            frameIdCombo.addItemListener(this);
            frameIdCombo.setToolTipText("Select a new frame "+
                    "type to add to the tag.");
            createFrameButton = new JButton("Create Additional Frame");
            createFrameButton.setEnabled(false);
            createFrameButton.addActionListener(this);
            createFrameButton.setActionCommand("create-frame");
            createFrameButton.setToolTipText("Click here after selecting a new frame "+
                    "type in the combo box on the left. ");

            updateButton = new JButton("Update Tags");
            updateButton.setEnabled(false);
            updateButton.addActionListener(this);
            updateButton.setActionCommand("update-tags");
            updateButton.setToolTipText("Update frames that are checked on mp3 files that are selected");

            refreshButton = new JButton("Refresh");
            refreshButton.addActionListener(this);
            refreshButton.setActionCommand("refresh");
            refreshButton.setToolTipText("Re-read tag data from currently selected track");
            
            
            superTagCheckBox = new JCheckBox("Super Tagging");
            superTagCheckBox.setToolTipText("Get tags from file names - Use #1 #2 etc in field values");
            superTagCheckBox.addItemListener(this);

            titleCaseCheckBox = new JCheckBox("Fix Case");
            titleCaseCheckBox.setToolTipText("Set Selected Text Fields To Title Case. Ignores Input Values And Non Text Fields");
            titleCaseCheckBox.addItemListener(this);
            
            delimTextField = new JTextField(null,1);
            delimTextField.setFont(font);
//            delimTextField.setMinimumSize(delimTextField.getSize());
            delimTextField.setEnabled(false);
            delimTextField.setToolTipText("Delimiter for tags from file names");

            refreshPush = new JPanel();
            Dimension dim = refreshPush.getPreferredSize();
            dim.width = 0;
            refreshPush.setPreferredSize(dim);
            pushUp = new JPanel();
            dim = pushUp.getPreferredSize();
            dim.height = 0;
            pushUp.setPreferredSize(dim);
            dialog.setLocationRelativeTo(mainFrame.frame);
            setSelection();
            dialog.addComponentListener(
               new ComponentListener() {

                @Override
                public void componentResized(ComponentEvent e) {
                    resizePictures();
                }

                @Override
                public void componentMoved(ComponentEvent e) {
                }

                @Override
                public void componentShown(ComponentEvent e) {
                }

                @Override
                public void componentHidden(ComponentEvent e) {
                }
            });
        }
        catch(Exception je) {
            je.printStackTrace();
            JOptionPane.showMessageDialog(dialog,
            je.toString(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
            
        }
//        dialog.setVisible(true);
    }

    void setSelection() throws Exception {
        if (numCboxChecked > 0)
            return;
        int row=-1;
        LibraryTrack track;
        if (fileName==null) {
            // If nothing is currently edited take the first item selected
            row=mainFrame.mainTable.getSelectedRow();
            if (row >= 0) {
                track = (LibraryTrack)mainFrame.library.trackVec.get(row);
                fileName=track.columns[attributes.fileNameCol];
                if (track.status == 'D')
                    fileName=null;
            }
        }
        else {
            int rowCount=mainFrame.mainTable.getSelectedRowCount();
            if (rowCount==1) {
                row=mainFrame.mainTable.getSelectedRow();
                LibraryTrack newTrack = (LibraryTrack)mainFrame.library.trackVec.get(row);
                String newFileName=newTrack.columns[attributes.fileNameCol];
                if (newTrack.status == 'D')
                    newFileName=null;
                if (fileName.equals(newFileName))
                    return;
                fileName = newFileName;
            }
        }
        setTitle();
        refresh();
        mainFrame.frame.setVisible(true);
        dialog.toFront();
    }

    void setTitle() {
        if (fileName == null)
            dialog.setTitle("Edit Track Tags");
        else {
            if (dialog.getExtendedState() == JFrame.ICONIFIED) {
                File file = new File(fileName);
                dialog.setTitle(file.getName());
            }
            else
                dialog.setTitle(fileName);
        }
    }


    void refresh()  {
        boolean isDone=false;
        boolean isException=false;
//        String fileName=this.fileName;

        while (!isDone) {
//            dialog.setVisible(false);
            widgetsPanel.removeAll();
            numCboxChecked = 0;

            try {
                MP3File mp3 = null;
                if (fileName!=null) {
                    mp3 = new MP3File();
                    File file = new File(fileName);
                        mp3.init(file,MP3File.BOTH_TAGS);
                }

                constraints = new GridBagConstraints();
                constraints.fill = GridBagConstraints.BOTH;
                constraints.insets = new Insets(4,4,4,4);
                constraints.weightx = 0;
                constraints.weighty = 0;
                constraints.anchor = GridBagConstraints.LINE_START;

                int ix;
                frames = new Vector();
                checkboxFrames = new HashMap();
                idFrames = new HashMap();
                langComboFrames = new HashMap();


                // First lay out the default tags
                // No languages or descriptions
                constraints.gridy=0;


                for (ix=0;ix<attributes.editColType.length;ix++) {
                    addFrame(mp3, attributes.editColId[ix],attributes.editColHeading[ix],
                        attributes.editColType[ix],true);


                }
                // Next lay out other tags in the file
                if (mp3!=null) {
                    ID3v2Frames frames = mp3.getID3v2Tag().getFrames();
                    TreeMap sorted = new TreeMap(frames);
                    Set keySet = sorted.keySet();
                    Iterator it = keySet.iterator();
                    while (it.hasNext()) {
                        String id = (String)it.next();
                        if (idFrames.get(id)!=null)
                            continue;
                        addFrame(mp3, id, null, MP3File.COL_ID3V2TAG,false);
                    }
                }
                // Bottom Controls
                setupBottom();
                isDone=true;
            }
            catch (Exception ex) {
                isDone=isException;
//                fileName=null;
                ex.printStackTrace();
                JOptionPane.showMessageDialog(mainFrame.frame,
                ex.toString(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
                isException=true;
            }
        }
        

    }

    static final int C_BN_LOAD_X = 0;
    static final int C_BN_LOAD_WIDTH = 1;
    static final int C_BN_SAVE_X = 1;
    static final int C_BN_SAVE_WIDTH = 1;
    static final int C_BN_DELETE_X = 2;
    static final int C_BN_DELETE_WIDTH = 1;
    static final int C_CHECKBOX_X = 0;
    static final int C_CHECKBOX_WIDTH = 3;
    static final int C_CHECKBOX_PIC_X = 0;
    static final int C_CHECKBOX_PIC_WIDTH = 2;
    static final int C_DESCRIPTION_X = 0;
    static final int C_DESCRIPTION_WIDTH = 3;
    static final int C_LANG_X = 0;
    static final int C_LANG_WIDTH = 2; // 2
    static final int C_LANGLABEL_X = 2;
    static final int C_LANGLABEL_WIDTH = 1;
    static final int C_MIME_X = 2;
    static final int C_MIME_WIDTH = 1;
    static final int C_VALUE_X = 4;
    static final int C_VALUE_WIDTH = 3;
    static final int C_QUESTION_X = 3;
    static final int C_QUESTION_WIDTH = 1;

    static final int C_FRAME_ID_X = 0;
    static final int C_FRAME_ID_WIDTH = 5;
    static final int C_BN_CREATE_FRAME_X = 5;
    static final int C_BN_CREATE_FRAME_WIDTH = 2;
    static final int C_CB_SUPERTAG_X = 0;
    static final int C_CB_SUPERTAG_WIDTH = 2;
    static final int C_TB_SUPERTAG_X = 2;
    static final int C_TB_SUPERTAG_WIDTH = 1;
    static final int C_CB_FIXCASE_X = 4;
    static final int C_CB_FIXCASE_WIDTH = 1;
    static final int C_PUSH_REFRESH_X = 4;
    static final int C_PUSH_REFRESH_WIDTH = 1;
    static final int C_BN_REFRESH_X = 5;
    static final int C_BN_REFRESH_WIDTH = 1;
    static final int C_BN_UPDATE_X = 6;
    static final int C_BN_UPDATE_WIDTH = 1;
    static final int C_PUSH_UP_X = 0;
    static final int C_PUSH_UP_WIDTH = 7;



    void addFrame(MP3File mp3, String id,String heading,int colType,boolean forceOneLine)
        throws Exception {
        Frame frame = new Frame();

//       |X Heading     |value|?|
//       |--------------|     |
//       |  cb Desc     |     |
//       |  combo  |lang|     |
//       |  image mime  |     |
//       |  Image type  |     |
//       ----------------------        
//       |  combo xxxxxx|  bn |
//       |  cb     |del |  bn |
//
        // change as from 8/23/2009
        // C=center E=east W=west
        // col 0 - load button C
        // col 1 - save button C
        // col 2 - delete button C
        // col 3 - ? button C
        // col 4 - lang 1 C
        // col 5 - lang 2 C
        // col 6 - lang 3 C
        // checkbox = 0 1 2 W
        // checkbox for picture = 0
        // mime type = 2
        // value = 4 5 6 W
        // description = 0 1 2 W
        // pic type = 0 1 2 W
        // lang = 0 1 W
        // lang label = 2 W
        // frame id list = 0 1 2 3 4 W
        // Create frame button = 5 6 E
        // Supertag checkbox = 0 1 W
        // supertag delim = 2 W
        // Fix Case checkbox = 4 W
        // Refresh = 5 C
        // Update = 6 E
        


        // Frame value
        FrameAttributes att;
        String idTrunc = id.substring(0,4);
        att = (FrameAttributes)FrameDictionary.getSingleton().frameTypes.get(idTrunc);
        if (att==null) {
            att = new FrameAttributes();
            att.colHeading=id;
        }
        if (heading==null)
            heading=att.colHeading;

        // Checkbox
        char dataType = att.type;
        JCheckBox cb = new JCheckBox(heading);
        constraints.gridx=C_CHECKBOX_X;
        constraints.gridwidth=C_CHECKBOX_WIDTH;
        if (dataType == 'I') {
            constraints.gridx=C_CHECKBOX_PIC_X;
            constraints.gridwidth=C_CHECKBOX_PIC_WIDTH;
        }
        constraints.gridheight=1;
        constraints.fill = GridBagConstraints.NONE;
        widgetsPanel.add(cb, constraints);
        frame.checkBox=cb;
        cb.addItemListener(this);
        cb.setToolTipText("Click here to Update this frame");

        // Language Code

        int idScanned=4;

        String langCode = new String();
        if (att.langReq) {
            if (id.length() >=7) {
                langCode = id.substring(4,7);
                idScanned=7;
            }
            else if (mp3==null)
                langCode = "eng";
        }

        
        // Help question mark
        constraints.gridx=C_QUESTION_X;
        constraints.gridwidth=C_QUESTION_WIDTH;
        frame.helpLabel = new JLabel("?");
        frame.helpLabel.setForeground(Color.BLUE);
        frame.helpLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        widgetsPanel.add(frame.helpLabel,constraints);
        frame.helpLabel.setToolTipText("Explanation of this frame");
        helpMouseListener(frame.helpLabel,  id);
        

        // Value
        constraints.gridx=C_VALUE_X;
        constraints.gridwidth=C_VALUE_WIDTH;
        if (!forceOneLine) {
            if (att.descReq)
                constraints.gridheight=2;
            if (att.langReq)
                constraints.gridheight++;
        }

        // 2008/01/11 Allow multi line 
        frame.dataType = dataType;
        String s;
        JComponent value;
        JTextField text;
        JTextArea area;
        boolean  deleteOption=false;
        Dimension dim;
        switch (dataType) {
        case 'T': // Text
            s = null;
            if (mp3!=null)        
                s = mp3.getMp3Field(colType,id);
            text = new JTextField(s,12);
            if (timeStampFrameIds.contains(id))
                text.setToolTipText("Format yyyy-MM-ddTHH:mm:ss");
            text.setFont(font);
            value = text;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            break;
        case 'M': // Multiline
            s = null;
            if (mp3!=null)        
                s = mp3.getMp3Field(colType,id);
            area = new HorizontalResizeTextArea(s,2,12, 150);
            area.setFont(font);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            Border loweredbevel = BorderFactory.createLoweredBevelBorder();
            area.setBorder(loweredbevel);
            textBG = area.getBackground();
            value=area;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            break;
        case 'I': // picture
            frame.picture = null;
            if (mp3!=null)        
                frame.picture = mp3.getMp3Picture(id);
            if (frame.picture==null) {
                frame.picture = new ID3v2Picture();
                frame.picture.mimeType="";
                frame.picture.Description="";
                frame.picture.pictureData=new byte[0];
            }
            // Mime Type
            constraints.gridx=C_MIME_X;
            constraints.gridwidth=C_MIME_WIDTH;
            constraints.gridheight=1;
            
            JLabel label = new JLabel(frame.picture.mimeType);
            constraints.fill = GridBagConstraints.HORIZONTAL;
            widgetsPanel.add(label,constraints);
            
            constraints.gridx=C_VALUE_X;
            constraints.gridwidth=C_VALUE_WIDTH;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            label = new HorizontalResizeLabel(120);
            frame.value=label;
//            createImagePopupMenu(frame, frames.size());
//            addMouseListenerToImage(frame);
            resizePicture(frame);
            ImageTransferHandler handler = new ImageTransferHandler(this,frame);
            label.setTransferHandler(handler);
            label.setFont(font);
            label.setToolTipText("Drop picture here.");
            value=label;
            if (forceOneLine) 
                constraints.gridheight=2;
            else
                constraints.gridheight=4;
            break;
        case 'N':   //integer value
            s = null;
            if (mp3!=null)        
                s = mp3.getMp3Integer(id);
            text = new JTextField(s,35);
            text.setToolTipText("Integer value");
            text.setFont(font);
            value = text;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            break;
        case 'L':   // Language (TLAN)
            s = null;
            if (mp3!=null)        
                s = mp3.getMp3Field(colType,id);
            frame.langValueCombo = new JComboBox[3];
            JPanel panel = new JPanel(new GridLayout());
            for (int i = 0; i < 3 ; i++) {
                frame.langValueCombo[i] = new JComboBox(attributes.langNames);
                frame.langValueCombo[i].insertItemAt("",0);
                String langName;
                if (s==null || s.length() < i * 3 + 3)
                    langName="";
                else    
                    langName = attributes.langCodeProps.getProperty(s.substring(i*3, i*3+3));
                frame.langValueCombo[i].setSelectedIndex(-1);
                frame.langValueCombo[i].setSelectedItem(langName);
                frame.langValueCombo[i].setEnabled(false);
                // frame.langValueCombo[i].addItemListener(this);
                dim = frame.langValueCombo[i].getPreferredSize();
                dim.width = 40;
                frame.langValueCombo[i].setPreferredSize(dim);
                // langComboFrames.put(frame.langValueCombo[i],frame);
                panel.add(frame.langValueCombo[i]);
            }
            value = panel;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.anchor = GridBagConstraints.LINE_START;
            break;
        case 'B': // Binary (PRIV frame)
            s = null;
            if (mp3!=null)        
                s = mp3.getMp3Binary(id);

            area = new HorizontalResizeTextArea(s,2,12, 150);
            area.setFont(font);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setEditable(false);
            area.setBackground(null);
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridheight=1;
            constraints.gridy++;
            widgetsPanel.add(area,constraints);
            constraints.gridy--;
            constraints.fill = GridBagConstraints.NONE;
            value = new JLabel("Frame cannot be edited. Select checkbox to delete frame.");
            value.setFont(font);
            frame.checkBox.setToolTipText("Click here to delete this frame");
            deleteOption=true;
            break;
        default:
            value = new JLabel("Frame cannot be edited. Select checkbox to delete frame.");
            value.setFont(font);
            frame.checkBox.setToolTipText("Click here to delete this frame");
            deleteOption=true;
        }
        if (value instanceof JTextComponent) {
            ((JTextComponent)value).setEditable(false);
            value.setBackground(null);
        }
        if (!deleteOption)
            frame.value=value;

        //value.setEnabled(false);
        constraints.weightx = 1;
        widgetsPanel.add(value,constraints);
        constraints.weightx = 0;
        frame.colType=colType;
        constraints.gridwidth=1;
        constraints.gridheight=1;
        

        // Description

        if (!forceOneLine) {
            String desc = new String();
            if (att.descReq) {
                if (id.length() > idScanned)
                    desc = id.substring(idScanned);
            }
            if (dataType=='I')
                desc=frame.picture.Description;

            if (dataType=='I' || att.descReq) {
                constraints.gridheight=1;
                constraints.gridx=C_DESCRIPTION_X;
                constraints.gridwidth=C_DESCRIPTION_WIDTH;
                constraints.gridy++;
                text = new JTextField(desc,15);
                text.setFont(font);
                text.setEditable(false);
                text.setBackground(null);
                if (!text.getText().equals(desc))  {
                    deleteOption = true;
                    frame.value=null;
                    if (value instanceof JTextComponent) {
                        ((JTextComponent)value).setText("Frame cannot be edited. Select checkbox to delete frame.");
                    }
                }
                
                constraints.fill = GridBagConstraints.HORIZONTAL;
                widgetsPanel.add(text,constraints);
                if (!deleteOption)
                    frame.descText=text;
                text.setToolTipText("Enter optional description of this frame. "+
                    "You can have more than one frame of this type if you use different " +
                    "descriptions.");
            }

            if (dataType=='I') {

                // Picture Type
                constraints.gridy++;
                JComboBox combo = new JComboBox(pictureTypes);
                if (frame.picture.pictureType < pictureTypes.length)
                    combo.setSelectedIndex(frame.picture.pictureType);
                dim = combo.getPreferredSize();
                dim.width = 180;
                combo.setPreferredSize(dim);
                combo.setEnabled(false);
                constraints.gridx=C_DESCRIPTION_X;
                constraints.gridwidth=C_DESCRIPTION_WIDTH;
                constraints.fill = GridBagConstraints.NONE;
                constraints.anchor = GridBagConstraints.FIRST_LINE_START;
                widgetsPanel.add(combo,constraints);
                frame.pictureType = combo;
                combo.setToolTipText("Select picture type.");
            }
        }
        // Put buttons even if it is a "one-line"
        if (dataType=='I') {
            // Load / Save Buttons
            constraints.gridy++;
            constraints.gridx=C_BN_LOAD_X;
            constraints.gridwidth=C_BN_LOAD_WIDTH;
            frame.loadButton = new JButton("Load");
            frame.loadButton.setEnabled(false);
            frame.loadButton.addActionListener(this);
            frame.loadButton.setActionCommand("load-"+frames.size());
            constraints.anchor = GridBagConstraints.CENTER;
            widgetsPanel.add(frame.loadButton,constraints);

            constraints.gridx=C_BN_SAVE_X;
            constraints.gridwidth=C_BN_SAVE_WIDTH;
            frame.saveButton = new JButton("Save");
            frame.saveButton.setEnabled(false);
            frame.saveButton.addActionListener(this);
            frame.saveButton.setActionCommand("save-"+frames.size());
//            constraints.anchor = GridBagConstraints.PAGE_START;
            widgetsPanel.add(frame.saveButton,constraints);


            constraints.gridx=C_BN_DELETE_X;
            constraints.gridwidth=C_BN_DELETE_WIDTH;
            frame.delButton = new JButton("Delete");
            frame.delButton.setEnabled(false);
            frame.delButton.addActionListener(this);
            frame.delButton.setActionCommand("dele-"+frames.size());
//            constraints.anchor = GridBagConstraints.FIRST_LINE_END;
            widgetsPanel.add(frame.delButton,constraints);
            constraints.anchor = GridBagConstraints.LINE_START;

        }

        // Language Combo Box

        if (att.langReq && !forceOneLine) {
            constraints.gridx=C_LANG_X;
            constraints.gridwidth=C_LANG_WIDTH;
            constraints.gridheight=1;
            constraints.gridy++;
            JComboBox langCombo = new JComboBox(attributes.langNames);
            if (!deleteOption)
                frame.langCombo = langCombo;
            String langName = attributes.langCodeProps.getProperty(langCode);
//            langCombo.setSelectedIndex(-1);
            langCombo.setSelectedItem(langName);
            langCombo.setEnabled(false);
            langCombo.addItemListener(this);
            dim = langCombo.getPreferredSize();
            dim.width = 120;
            langCombo.setPreferredSize(dim);
//            JPanel panel = new JPanel(new GridLayout());
//            panel.setPreferredSize(dim);
//            panel.add(frame.langCombo);
            constraints.fill = GridBagConstraints.NONE;
            widgetsPanel.add(langCombo,constraints);
            langComboFrames.put(frame.langCombo,frame);
            langCombo.setToolTipText("Select language of this frame. "+
                "You can have more than one frame of this type if you use different " +
                "languages.");
        
            // Language Code label
            constraints.gridx=C_LANGLABEL_X;
            constraints.gridwidth=C_LANGLABEL_WIDTH;
            JLabel label = new JLabel(langCode);
            label.setFont(font);
            frame.langCode = label;
            constraints.fill = GridBagConstraints.NONE;
            widgetsPanel.add(label,constraints);
            label.setToolTipText("Language code.");
        }

        frame.id=id;
        frames.add(frame);
        checkboxFrames.put(frame.checkBox,frame);
        idFrames.put(frame.id,frame);
        constraints.gridy++;
    }

//    void createImagePopupMenu(Frame frame, int frameNum) {
//        JPopupMenu popupMenu = new JPopupMenu();
//        JMenuItem menuItem;
//
//        // Copy and Paste actions added from the java tutorial
//        // j2sdk1.4.2_09/tutorial/uiswing/misc/dnd.html
//        TransferActionListener actionListener = new TransferActionListener(frame.value);
//        menuItem = new JMenuItem("Copy",KeyEvent.VK_C);
//        menuItem.setActionCommand((String)TransferHandler.getCopyAction().
//                 getValue(Action.NAME));
//        menuItem.addActionListener(actionListener);
////        menuItem.setAccelerator(
////          KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
//        popupMenu.add(menuItem);
//
//        menuItem = new JMenuItem("Paste",KeyEvent.VK_P);
//        menuItem.setActionCommand((String)TransferHandler.getPasteAction().
//                 getValue(Action.NAME));
//        menuItem.addActionListener(actionListener);
//        menuItem.setAccelerator(
//          KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
//        popupMenu.add(menuItem);
//
//        menuItem = new JMenuItem("Load File", KeyEvent.VK_L);
//        popupMenu.add(menuItem);
//        menuItem.addActionListener(this);
//        menuItem.setActionCommand("load-"+frameNum);
//
//        menuItem = new JMenuItem("Save File", KeyEvent.VK_S);
//        popupMenu.add(menuItem);
//        menuItem.addActionListener(this);
//        menuItem.setActionCommand("save-"+frameNum);
//
//        menuItem = new JMenuItem("Delete Image", KeyEvent.VK_D);
//        popupMenu.add(menuItem);
//        menuItem.addActionListener(this);
//        menuItem.setActionCommand("dele-"+frameNum);
//
//        frame.popupMenu = popupMenu;
//
//    }
//
//    void addMouseListenerToImage(final Frame frame) {
//        MouseAdapter  mouseListener = new MouseAdapter() {
//            public void mousePressed(MouseEvent e) {
//                maybeShowPopup(e,frame);
//            }
//
//            public void mouseReleased(MouseEvent e) {
//                maybeShowPopup(e,frame);
//            }
//
//        };
//        frame.value.addMouseListener(mouseListener);
//
//
//    }
//
//
//    private void maybeShowPopup(MouseEvent e,Frame frame) {
//        if (e.isPopupTrigger()) {
//            frame.popupMenu.show(e.getComponent(),
//            e.getX(), e.getY());
//        }
//    }
//
//
//
    FileListDialog helpWin = null;
    void helpMouseListener(JComponent help, final String id) {
        MouseAdapter listener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
            }
            
            public void mouseReleased(MouseEvent e) {
            }
            
            public void mouseClicked(MouseEvent e) {
                InputStream input = ClassLoader.getSystemResourceAsStream(
                    "pgbennett/jampal/frames.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                StringBuffer helpText = new StringBuffer();
                String line = "";
                boolean found = false;
                String frameId = id.substring(0,4);
                try {
                    while (line!=null) {
                        int locSpace = line.indexOf(' ');
                        if (locSpace == -1)
                            locSpace = line.length();
                        String firstWord = line.substring(0, locSpace);
                        if (firstWord.equals(frameId) || firstWord.equals(id)) {
                            found = true;
                            helpText.append(line);
                            helpText.append("\n");
                            line = "";
                            while (line != null && (line.startsWith(" ")||line.length()==0)) {
                                helpText.append(line);
                                helpText.append("\n");
                                line = reader.readLine();
                            }
                            break;
                        }
                        line = reader.readLine();
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                if (helpWin!= null) 
                    helpWin.dialog.dispose();
                if (found) {
                    String [] arr = { helpText.toString(), null };
                    helpWin = new FileListDialog(dialog, 
                        "Frame "+frameId, arr, false,false); 
                    helpWin.dialog.setVisible(true);
                }
                else 
                    JOptionPane.showMessageDialog(dialog,
                    "No Help Found for "+frameId,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            } 
                
        };
        help.addMouseListener(listener);
    }
    

    void setupBottom() {

        // Frame id combo box

        constraints.gridx=C_FRAME_ID_X;
        constraints.gridwidth=C_FRAME_ID_WIDTH;
        constraints.gridheight=1;
        constraints.anchor = GridBagConstraints.LINE_START;
        frameIdCombo.setSelectedIndex(-1);
        createFrameButton.setEnabled(false);
        constraints.fill = GridBagConstraints.NONE;
        widgetsPanel.add(frameIdCombo,constraints);

        // create frame button
        constraints.gridx=C_BN_CREATE_FRAME_X;
        constraints.gridwidth=C_BN_CREATE_FRAME_WIDTH;
        constraints.anchor = GridBagConstraints.LINE_END;
        widgetsPanel.add(createFrameButton,constraints);
        constraints.gridy++;
        constraints.anchor = GridBagConstraints.LINE_START;

        // super tagging superTagCheckBox
        constraints.gridx=C_CB_SUPERTAG_X;
        constraints.gridwidth=C_CB_SUPERTAG_WIDTH;
        superTagCheckBox.setSelected(false);
        widgetsPanel.add(superTagCheckBox,constraints);

        // delimiter text field
        constraints.gridx=C_TB_SUPERTAG_X;
        constraints.gridwidth=C_TB_SUPERTAG_WIDTH;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        widgetsPanel.add(delimTextField,constraints);
        delimTextField.setEnabled(false);
        constraints.fill = GridBagConstraints.NONE;

        // Title Case Check Box
        constraints.gridx=C_CB_FIXCASE_X;
        constraints.gridwidth=C_CB_FIXCASE_WIDTH;
        constraints.anchor = GridBagConstraints.LINE_START;
        titleCaseCheckBox.setSelected(false);
        widgetsPanel.add(titleCaseCheckBox,constraints);

        // Push Refresh Button
        constraints.gridx=C_PUSH_REFRESH_X;
        constraints.gridwidth=C_PUSH_REFRESH_WIDTH;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        widgetsPanel.add(refreshPush,constraints);
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0;

        // Refresh Button
        constraints.gridx=C_BN_REFRESH_X;
        constraints.gridwidth=C_BN_REFRESH_WIDTH;
        constraints.anchor = GridBagConstraints.LINE_END;
        widgetsPanel.add(refreshButton,constraints);

        // Update button
        constraints.gridx=C_BN_UPDATE_X;
        constraints.gridwidth=C_BN_UPDATE_WIDTH;
        constraints.anchor = GridBagConstraints.LINE_END;
        updateButton.setEnabled(false);
        widgetsPanel.add(updateButton,constraints);
        dialog.getRootPane().setDefaultButton(updateButton);

        // Push UP Bottom
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridy++;
        constraints.gridx=C_PUSH_UP_X;
        constraints.gridwidth=C_PUSH_UP_WIDTH;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1;
        constraints.weighty = 1;
        constraints.insets = new Insets(0,0,0,0);
        widgetsPanel.add(pushUp,constraints);
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(4,4,4,4);
        constraints.weightx = 0;
        constraints.weighty = 0;

        pack();

    }
    
    Dimension size=null;
    void pack() {
        if (size != null)
            size=dialog.getSize();
        Point loc = dialog.getLocation();
//        dialog.setVisible(false);
        dialog.pack();
        boolean first=false;
        if (size == null) {
            first=true;
            size=dialog.getSize();
            if (size.height > attributes.dialogMaxHeight)
                size.height = attributes.dialogMaxHeight;
            // This ensures there is enough space to add a scrollbar
            // without cutting anything off
            size.width+=20;
            if (size.width > attributes.dialogMaxWidth)
                size.width = attributes.dialogMaxWidth;
        }
        dialog.setSize(size);
        if (first) {
            loc = dialog.getLocation();
            if (loc.y+size.height > attributes.dialogMaxHeight) {
                loc.y = attributes.dialogMaxHeight - size.height;
//                dialog.setLocation(loc);
            }
            if (loc.x+size.width > attributes.dialogMaxWidth) {
                loc.x = attributes.dialogMaxWidth - size.width;
//                dialog.setLocation(loc);
            }
        }
        dialog.setLocation(loc);
        dialog.setVisible(true);
//        loc = dialog.getLocation();
        
    }

    void createFrame() {

        widgetsPanel.remove(frameIdCombo);
        widgetsPanel.remove(createFrameButton);
        dialog.getRootPane().setDefaultButton(null);
        widgetsPanel.remove(updateButton);
        widgetsPanel.remove(refreshButton);
        widgetsPanel.remove(refreshPush);
        widgetsPanel.remove(pushUp);
        widgetsPanel.remove(superTagCheckBox);
        widgetsPanel.remove(delimTextField);
        constraints.gridy-=2;

        String name = (String)frameIdCombo.getSelectedItem();
        FrameAttributes frameAtt = (FrameAttributes)FrameDictionary.getSingleton().frameNameMap.get(name);
        try {
            addFrame(null,frameAtt.id,null,MP3File.COL_ID3V2TAG,false);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        Frame newFrame=(Frame)frames.get(frames.size()-1);
        setupBottom();
        // turn on the new checkbox
        newFrame.checkBox.doClick();

    }

    void updateTags() {
        if (mainFrame.library.readOnly){
            JOptionPane.showMessageDialog(dialog,
                         "Library is Read Only",
                         "Error",
                         JOptionPane.ERROR_MESSAGE);
            return;
        }
        mainFrame.tableCellEditor.cancelCellEditing();
        updater = new TrackUpdater(this);
        // This spins off a worker thread and returns
        updater.updateTags();
    }

    // This gets run in thw AWT thread after updating the tags
    public void run () {
        if (updater!=null) {
            updater.finish();
            updater=null;
        }
    }

    void loadImage(String cmd) {
        try {
            int frameNum = Integer.parseInt(cmd.substring(5));
            Frame frame = (Frame)frames.get(frameNum);
            JFileChooser fileChooser = new JFileChooser();
            String currentDirectory=mainFrame.savedSettings.getProperty("imagecurrentdirectory");
            if (currentDirectory!=null)
                fileChooser.setCurrentDirectory(new File(currentDirectory));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            ImageFileFilter fileFilter = new ImageFileFilter();
            fileChooser.addChoosableFileFilter(fileFilter);
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setFileFilter(fileFilter);
            int returnVal = fileChooser.showOpenDialog(dialog);
            currentDirectory=fileChooser.getCurrentDirectory().getAbsolutePath();
            String fixDirectory;
            try {
                fixDirectory = mainFrame.library.attributes.translateFileName(currentDirectory);
            } catch (JampalException ja) {
                ja.printStackTrace();
                fixDirectory = currentDirectory;
            }
            mainFrame.savedSettings.setProperty("imagecurrentdirectory",fixDirectory);
            if (returnVal == fileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                loadImage(frame,file);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    void loadImage(Frame frame, File file) throws Exception {
        FileInputStream in = new FileInputStream(file);
        long length = file.length();
        String ext = getExtension(file.getName());
        if ("jpg".equalsIgnoreCase(ext)||"jpe".equalsIgnoreCase(ext))
            ext="jpeg";
        String mimeType = "image/"+ext;
        loadImage(frame, in, length, mimeType);
    }

    void loadImage(Frame frame, URL url) throws Exception {
        URLConnection conn = url.openConnection();
        String mimeType = conn.getContentType();
        long length = conn.getContentLength();
        InputStream is = conn.getInputStream();
        loadImage(frame, is, length, mimeType);
    }


    String getExtension (String s) {
        String ext=null;
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
    
    void loadImage(Frame frame, InputStream in, long xxx, String mimeType) throws Exception {
        ArrayList<byte[]> list = new ArrayList<byte[]>();
//        if (length <= 0)
//            return;
        // Read the image into an array of 1024 byte buffers
        try {
            if (!mimeType.startsWith("image/"))
                return;
            int off = 0;
            int length = 0;
            int increment = 0;
            for (;;) {
                byte [] buffer = new byte [1024];
                off = 0;
                while (off < 1024) {
                    increment = in.read(buffer, off, 1024-off);
                    if (increment == -1)
                        break;
                    off += increment;
                    length += increment;
                }
                list.add(buffer);
                if (increment == -1)
                    break;
            }

            // String the buffers into one buffer
            frame.picture.pictureData = new byte [length];
            int destPos = 0;
            for (int ix = 0 ; ix < list.size() ; ix ++) {
                byte [] buffer = list.get(ix);
                if (ix == list.size() -1) {
                    System.arraycopy(buffer, 0, frame.picture.pictureData, destPos, off);
                    destPos += off;
                }
                else {
                    System.arraycopy(buffer, 0, frame.picture.pictureData, destPos, 1024);
                    destPos += 1024;
                }
            }


    //        frame.picture.pictureData = new byte [(int)length];
    //        int count = (int)length;
    //        int off = 0;
    //        while (off < count) {
    //            int increment = in.read(frame.picture.pictureData, off, count-off);
    //            if (increment == -1)
    //                break;
    //            off += increment;
    //        }
            frame.picture.mimeType=mimeType;
            frame.pictureWidth = -1;
            resizePicture(frame);
            pack();
        }
        finally {
            in.close();
        }
        
    }
    boolean resizePicture(Frame frame) throws Exception {
        ImageIcon icon;
        boolean wasResized=false;
        Dimension dim = frame.value.getSize();
        if (dim.width == 0)
            dim.width = 600;
        Icon currentIcon = ((JLabel)frame.value).getIcon();
        int currentIconWidth = 0;
        if (currentIcon != null)
            currentIconWidth = currentIcon.getIconWidth();
        if (frame.pictureWidth == -1) // This is indicator of a new picture
            currentIconWidth = 0;
        // if the window has been made smaller
        if (dim.width < currentIconWidth
        // or window has been made bigger and the icon not at maximum
        ||  (dim.width > currentIconWidth && frame.pictureWidth > currentIconWidth)
        ||  frame.pictureWidth == -1 ) {
//        if (frame.pictureWidth == -1 || frame.pictureWidth > dim.width) {
            if ("image/bmp".equalsIgnoreCase(frame.picture.mimeType)) {
                ByteArrayInputStream bin = new ByteArrayInputStream(frame.picture.pictureData);
                javazoom.Util.BMPLoader bmp = new javazoom.Util.BMPLoader();
                Image image = bmp.getBMPImage(bin);
                icon = new ImageIcon(image);
            }
            else {
                if (frame.picture.pictureData.length == 0)
                    icon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/drop_picture.png"));
                else
                    icon = new ImageIcon(frame.picture.pictureData);
            }
            if (icon.getIconWidth()<= 0 || icon.getIconHeight() <= 0) {
                frame.picture.pictureData = new byte[0];
                icon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/drop_picture.png"));
            }
            frame.pictureWidth = icon.getIconWidth();
            if (frame.pictureWidth > dim.width) {
                // resize the image
                Image image = icon.getImage();
                Image scaledImage = image.getScaledInstance(dim.width, -1, Image.SCALE_DEFAULT);
                icon.setImage(scaledImage);
                wasResized=true;
            }
            ((JLabel)frame.value).setIcon(icon);
        }
        return wasResized;
    }

    void resizePictures() {
        try {
            boolean wasResized = false;
            for (Frame frame : frames) {
                if (frame.dataType == 'I') {
                    if (resizePicture(frame))
                        wasResized=true;
                }
            }
//            if (wasResized)
//                pack();
        } catch (Exception je) {
            je.printStackTrace();
            JOptionPane.showMessageDialog(dialog,
                    je.toString(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    void delImage(String cmd) {
        int frameNum = Integer.parseInt(cmd.substring(5));
        Frame frame = (Frame)frames.get(frameNum);
        frame.picture.pictureData = new byte [0];
        frame.picture.mimeType="";
        ImageIcon icon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/drop_picture.png"));
        ((JLabel)frame.value).setIcon(icon);
        pack();
    }
    void saveImage(String cmd) {
        int frameNum = Integer.parseInt(cmd.substring(5));
        Frame frame = (Frame)frames.get(frameNum);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save image file");
        String currentDirectory=mainFrame.savedSettings.getProperty("imagecurrentdirectory");
        if (currentDirectory!=null)
            fileChooser.setCurrentDirectory(new File(currentDirectory));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        ImageFileFilter fileFilter = new ImageFileFilter();
        fileChooser.addChoosableFileFilter(fileFilter);
        fileChooser.setFileFilter(fileFilter);
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        int returnVal = fileChooser.showSaveDialog(dialog);
        currentDirectory=fileChooser.getCurrentDirectory().getAbsolutePath();
        String fixDirectory;
        try {
            fixDirectory = mainFrame.library.attributes.translateFileName(currentDirectory);
        } catch (JampalException ja) {
            ja.printStackTrace();
            fixDirectory = currentDirectory;
        }
        mainFrame.savedSettings.setProperty("imagecurrentdirectory",fixDirectory);
        try {
            if (returnVal == fileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String filePath = file.getAbsolutePath();
                int pos = frame.picture.mimeType.lastIndexOf("/");
                if (pos != -1) {
                    String extension = frame.picture.mimeType.substring(pos+1);
                    if (!filePath.endsWith("."+extension))
                        file = new File(filePath+"."+extension);
                }
                FileOutputStream out = new FileOutputStream(file);
                out.write(frame.picture.pictureData);
                out.close();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(dialog,
                                     ex.toString(),
                                     "Error",
                                     JOptionPane.ERROR_MESSAGE);
        }
    }

    boolean nestedChange=false;
    public void itemStateChanged(ItemEvent e) {
        Object item = e.getItemSelectable();
        boolean stateSelected = e.getStateChange() == ItemEvent.SELECTED;
        // See if it is a checkbox
        Frame frame = (Frame)checkboxFrames.get(item);
        if (frame!= null) {
            if (!stateSelected)
                numCboxChecked--;
            if (stateSelected)
                numCboxChecked++;
            if (!titleCaseCheckBox.isSelected())
                enableFrameComponents(frame, stateSelected);
            updateButton.setEnabled(numCboxChecked>0);
        }
        // See if it is a languange combobox
        frame = (Frame)langComboFrames.get(item);
        if (frame!= null) {
            String langName = (String)frame.langCombo.getSelectedItem();
            String langCode = (String)attributes.langNameMap.get(langName);
            if (langCode==null)
                langCode="";
            frame.langCode.setText(langCode);
        }
        // Frame Id combo box
        if (frameIdCombo.equals(item))
            createFrameButton.setEnabled(stateSelected);

        // Super tagging check box
        if (superTagCheckBox.equals(item)) {
            if (!nestedChange) {
                nestedChange=true;
                titleCaseCheckBox.setSelected(false);
                nestedChange=false;
            }
            delimTextField.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        }
        
        // Title case check box
        if (titleCaseCheckBox.equals(item)) {
            if (!nestedChange) {
                nestedChange=true;
                superTagCheckBox.setSelected(false);
                nestedChange=false;
            }
            int ix;
            for (ix=0;ix<frames.size();ix++) {
                frame = (Frame)frames.get(ix);
                if (frame.checkBox.isSelected())
                    enableFrameComponents(frame, !stateSelected);
            }
        }
    }

    void enableFrameComponents(Frame frame, boolean stateSelected) {
        if (frame.value instanceof JTextComponent) {
            // Bug in Linux java - if text area is not editable
            // and text zero length - it makes the text area very tall.
            JTextComponent textComponent = (JTextComponent)frame.value;
            String textValue = textComponent.getText();
            if (!stateSelected) {
                if (textValue.length() == 0) {
                    textComponent.setText(" ");
                }
            }
            textComponent.setEditable(stateSelected);
            Color newBG=null;
            if (stateSelected)
                newBG=textBG;
            frame.value.setBackground(newBG);
        }
        if (frame.langCombo!=null) {
            frame.langCombo.setEnabled(stateSelected);
//            if (!stateSelected) {
////%%%                frame.langCombo.setFont(font);
//                frame.langCombo.setForeground(Color.BLACK);
//            }
        }
        if (frame.langValueCombo != null) {
            for (JComboBox box : frame.langValueCombo) {
                if (box != null)
                    box.setEnabled(stateSelected);
            }
        }
        if (frame.descText!=null)
            frame.descText.setEditable(stateSelected);
        if (frame.pictureType!=null)
            frame.pictureType.setEnabled(stateSelected);
        if (frame.loadButton!=null)
            frame.loadButton.setEnabled(stateSelected);
        if (frame.saveButton!=null)
            frame.saveButton.setEnabled(stateSelected);
        if (frame.delButton!=null)
            frame.delButton.setEnabled(stateSelected);
    }
    
    
    // Buttons
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        if ("create-frame".equals(cmd))
            createFrame();
        if ("update-tags".equals(cmd))
            updateTags();
        if ("refresh".equals(cmd)) {
            try {
                fileName=null;
                numCboxChecked=0;
                setSelection();
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        if (cmd.startsWith("load-"))
            loadImage(cmd);
        if (cmd.startsWith("save-"))
            saveImage(cmd);
        if (cmd.startsWith("dele-"))
            delImage(cmd);
    }



}


