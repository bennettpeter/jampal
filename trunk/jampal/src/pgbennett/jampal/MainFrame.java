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

import pgbennett.jampal.customize.CustomizeDialog;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.datatransfer.*;
import java.awt.font.TextAttribute;
import java.net.URI;
import java.net.URL;
import javax.swing.text.*;
import pgbennett.id3.*;
import java.text.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import pgbennett.utility.FontChooser;

public class MainFrame implements Runnable, ActionListener, KeyListener,
WindowListener, ListSelectionListener {
    
    static final String[] AboutText = {
        "Jampal Version " + Jampal.version,
        "Copyright (c) Peter Bennett, 2004-2011",
        " ",
        "Jampal is free software: you can redistribute it and/or modify "
        + "it under the terms of the GNU General Public License as published by "
        + "the Free Software Foundation, either version 3 of the License, or "
        + "(at your option) any later version.",
        " ",
        "Jampal is distributed in the hope that it will be useful, "
        + "but WITHOUT ANY WARRANTY; without even the implied warranty of "
        + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the "
        + "GNU General Public License for more details.",
        " ",
        "You should have received a copy of the GNU General Public License "
        + "along with Jampal.  If not, see <http://www.gnu.org/licenses/>.",
        " ",
        "For More information see http://jampal.sourceforge.net",
        " ",
        "This software includes elements from:",
        " Jd3lib http://sourceforge.net/projects/jd3lib",
        " Javalayer http://sourceforge.net/projects/javalayer",
        " Tritonus http://sourceforge.net/projects/tritonus",
        "ID3V2 documentation is Copyright (C) Martin Nilsson 2000, "
        + "derived from ID3 v2.3.0 Informal Standard at "
        + "http://www.id3.org/id3v2.3.0.txt and ID3 tag version 2.4.0 "
        + "- Native Frames at http://www.id3.org/id3v2.4.0-frames.txt",
        " ",
        "This software includes Java look and feel from the following sources:",
        "Liquid - https://liquidlnf.dev.java.net",
        "Jgoodies - http://www.jgoodies.com/freeware/looks",
        "Squareness - http://squareness.beeger.net",
        "Nimrod - http://personales.ya.com/nimrod",
        "Lipstik - http://sourceforge.net/projects/lipstiklf",
        "PGS - https://pgslookandfeel.dev.java.net",
        "Infonode - http://www.infonode.net/index.html?ilf",
        "Napkin - http://sourceforge.net/projects/napkinlaf",
        " ",
        "Your Home Directory is " + System.getProperty("user.home")
    };
    
    public Library library;
    LibraryTableModel model;
    public JTable mainTable;
    Properties savedSettings;
    JScrollPane scrollPane;
    Mp3FileFilter mp3FileFilter = new Mp3FileFilter();
    public JFrame frame;
    // Unique name (file name) that identifies this instance
    public String name;
    public String shortName;
    ProgressBox progressBox;
    File fileChoose[];
    int addCounter;
    JSlider playSlider;
    JLabel playLabel;
    JLabel timeLabel;
    TrackEditor trackEditor;
    AudioPlayer player;
    JPopupMenu popupMenu;
    int editorUpdateCount;
    /** Keys of map are library name, values are the mainframe objects */
    public static HashMap<String, MainFrame> mainFrameMap = new HashMap<String, MainFrame>();
    public static boolean isOpening = false;
    public javax.swing.Timer timer;
    DefaultCellEditor tableCellEditor;
    JCheckBox cellEditCheckBox;     // Cell Editing
    JCheckBoxMenuItem cellEditMenuItem;
    JCheckBoxMenuItem announceMenuItem;
    JCheckBoxMenuItem continuousMenuItem;
    public ActionListener editorUpdateActionListener;
    boolean editorUpdateActionListening;
    JLabel statusLabel;
    int editRow = -1;
    int editCol = -1;
    int focusRow = -1;
    int focusCol = -1;
    SearchDialog searchDialog;
    LibraryTrack trackLastPlaying;
    public MainFrame playListFrame;
    public MainFrame parentFrame;
    CustomizeDialog customizeDialog;
    // Run type for run method
    // V = validate
    // A = Add
    // P = Paste
    char runType;
    LibraryTrack [] selectedTracks;
    // Used in connection with above paste
    TransferObject transferObject;
    
    static final int mruLimit = 10;
    static Vector <String> mruList = new Vector<String> (mruLimit);
    Vector <Component> mruComponents = new Vector <Component> (mruLimit+1);
    
    JMenu fileMenu;
    JSeparator fileSeparator;

    Font selectedFont;
    Font strikeoutFont;

    ArrayList<Image> windowIcons = new ArrayList<Image>();

    public MainFrame(String name) {
        
        try {
            if (name==null) {
                String libDir = System.getProperty("jampal.libdir");
                if (libDir != null) {
                    File dir = new File(libDir);
                    if (!dir.isDirectory())
                        dir.mkdir();
                    if (!dir.isDirectory())
                        libDir=null;
                }
                if (libDir==null)
                    libDir = System.getProperty("user.home")+
                        File.separator+"Music"+File.separator+"library";
//                String defaultName = System.getProperty("user.home")+
//                        File.separator+".jampal"+File.separator+"default.jampal";
                String defaultName = libDir+File.separator+"default.jampal";
                File defaultFile = new File(defaultName);
                if (!defaultFile.exists()) {
                    File libDirFile = new File(libDir);
                    libDirFile.mkdirs();
                    new Library(this, true, "Basic library", defaultName);
                }
                name=defaultName;
            }       

            if (name.endsWith(".jampal"))
                name = name.substring(0,name.length()-7);
            File nameFile = new File(name);
            name = nameFile.getAbsolutePath();
            this.name=name;
            shortName = nameFile.getName();
            MainFrame already = (MainFrame)mainFrameMap.get(name);
            if (already != null) {
                already.frame.setVisible(true);
                return;
            }
            String title = shortName + " - jampal";
            frame = new JFrame(title);
            ImageIcon splashIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/splash.jpg"));
            JLabel splashLabel = new JLabel(splashIcon);
            JPanel splashPanel = new JPanel();
            splashPanel.add(splashLabel);
            frame.setContentPane(splashPanel);
            frame.pack();
            
            
            frame.setLocation(300,150);
            frame.setVisible(true);
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            frame.paint(frame.getGraphics());
            windowIcons = new ArrayList<Image>();
            ImageIcon windowIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/notes1.png"));
            windowIcons.add(windowIcon.getImage());
            windowIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/notes2.png"));
            windowIcons.add(windowIcon.getImage());
            frame.setIconImages(windowIcons);
            timer = new javax.swing.Timer(500,null);
            timer.start();
            createUpdateActionListener();
            GridBagConstraints constraints;
            constraints = new GridBagConstraints();
            library = new Library(this);
            if (library.attributes==null) {
                openLibrary();
                if (mainFrameMap.isEmpty()&&!isOpening)
                    System.exit(0);
                frame.dispose();
                return;
            }
            
            savedSettings=library.attributes.libraryProperties;
//            createMenu();
//            createPopupMenu();
            
            //Create and set up the content pane.
            model = new LibraryTableModel(library);
            library.model = model;
            JPanel contentPane = new JPanel(new GridBagLayout());
            
            constraints.fill = GridBagConstraints.NONE;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.anchor = GridBagConstraints.LINE_START;
            constraints.gridwidth=1;
            constraints.gridy=0;
            constraints.gridx=0;
            
            constraints.weightx = 0.1;
            playLabel = new JLabel();
            if (!library.attributes.compact) {
                contentPane.add(playLabel,constraints);
                constraints.gridx++;
            }
            constraints.weightx = 0;
            // Cell Editing start
            cellEditCheckBox = new JCheckBox("Automatic Cell Editing");
            cellEditCheckBox.setActionCommand("cbox-celledit");
            cellEditCheckBox.addActionListener(this); 
            if (!library.attributes.compact) { 
                contentPane.add(cellEditCheckBox,constraints); 
                constraints.gridx++; 
            } 
            // Cell Editing End
            timeLabel = new JLabel();
            if (library.attributes.compact) {
                constraints.weightx = 0.1;
            }
            contentPane.add(timeLabel,constraints);
            constraints.gridx++;
            playSlider = new JSlider(0,10000,0);
            Dimension size = playSlider.getMinimumSize();
            if (library.attributes.compact)
                size.setSize(100,size.getHeight());
            else
                size.setSize(200,size.getHeight());
            playSlider.setMinimumSize(size);
            playSlider.setToolTipText("Track Playing Progress");
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 0;
            contentPane.add(playSlider,constraints);
            
            constraints.weightx = 0;
            constraints.gridwidth=1;
            constraints.gridx++;
            
            createPlayButtons(contentPane,constraints);
            constraints.weightx = 0.1;
            
            mainTable = new JTable(model);
            createMenu();
            String resizeMode = library.attributes.libraryProperties.getProperty("resize-mode");
            int resizeIx = 0;
            try {
                if (resizeMode != null)
                    resizeIx = Integer.parseInt(resizeMode);
            }
            catch(Exception ex) {
                ex.printStackTrace();
            }
            mainTable.setAutoResizeMode(CustomizeDialog.resizeModesInt[resizeIx]);
            mainTable.putClientProperty("JTable.autoStartsEdit",Boolean.FALSE);
            mainTable.addKeyListener(this);
            createHeaderRenderers();
            scrollPane = new JScrollPane(mainTable);
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weighty = 0.1;
            constraints.gridwidth=constraints.gridx;
            constraints.gridy++;
            constraints.gridx=0;
            contentPane.add(scrollPane,constraints);
            statusLabel = new JLabel("Welcome to Jampal");
            constraints.weighty = 0;
            constraints.gridy++;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            contentPane.add(statusLabel,constraints);
            contentPane.setOpaque(true); //content panes must be opaque
            frame.setContentPane(contentPane);
            frame.addWindowListener(this);
            addMouseListenerToTable();
            addMouseListenerToHeaderInTable();
            ListSelectionModel listModel = mainTable.getSelectionModel();
            listModel.addListSelectionListener(this);
            TableColumnModel columnModel = mainTable.getColumnModel();
            ListSelectionModel colSelectionModel = columnModel.getSelectionModel();
            colSelectionModel.addListSelectionListener(new ColumnSelectionListener());
            
            loadSavedSettings();
            String announcements = savedSettings.getProperty("announcements");
            announceMenuItem.setSelected("true".equals(announcements));
            String continuous = savedSettings.getProperty("continuous");
            if (continuousMenuItem!=null)
                continuousMenuItem.setSelected("true".equals(continuous));
            player = new AudioPlayer(this);
            
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            
            restoreSize();
            setCustomFonts();
            mainFrameMap.put(name,this);
            updateMRUList();
            // DnD feature.
            mainTable.setDragEnabled(true);
            mainTable.setTransferHandler(new TableTransferHandler(this));
            JTableHeader th = mainTable.getTableHeader();
            th.setTransferHandler(new TableTransferHandler(this));
            contentPane.setTransferHandler(new TableTransferHandler(this));
            // Special cell editor for language
            int col;
            for (col=0;col<library.attributes.numDisplayCol;col++) {
                int type=library.attributes.colType[library.attributes.displayCol[col]];
                if (type==MP3File.COL_ID3V2TAG) {
                    String frame=library.attributes.colId[library.attributes.displayCol[col]];
                    if ("TLAN".equals(frame)) {
                        TableColumn langColumn = mainTable.getColumnModel().getColumn(col);
                        JComboBox langComboBox = new JComboBox(library.attributes.langNames);
                        langComboBox.insertItemAt("", 0);
                        langColumn.setCellEditor(new DefaultCellEditor(langComboBox));
                    }
                }
            }                
        frame.setCursor(null);

        }
        catch(Exception je) {
            je.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            je.toString(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
            frame.dispose();
            mainFrameMap.remove(name);
        }
        
    }

    
    void createUpdateActionListener() {
        editorUpdateActionListener =  new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (editorUpdateCount > 0) {
                    editorUpdateCount--;
                    if (editorUpdateCount == 0) {
                        timer.removeActionListener(editorUpdateActionListener);
                        editorUpdateActionListening=false;
                        updateSelection();
                    }
                }
            }
        };
    }
    
    
    
    void createMenu() {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;
        
        
        //Create the menu bar.
        menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);
        
        //Build the first menu.
        menu = new JMenu("File");
        fileMenu = menu;
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);
        
        //a group of JMenuItems
        menuItem = new JMenuItem("New Library...",KeyEvent.VK_N);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-new");
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Open Library or Playlist...",KeyEvent.VK_O);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-open");
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Add Tracks to Library...", KeyEvent.VK_A);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-add");
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Open Default Playlist",KeyEvent.VK_P);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-openplaylist");
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Save Playlist as file...",KeyEvent.VK_S);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-save");
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Load Playlist from file...",KeyEvent.VK_L);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-load");
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Save Library");
        menu.add(menuItem);
        menuItem.setActionCommand("menu-compress");
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Reload Library",KeyEvent.VK_R);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-reload");
        menuItem.addActionListener(this);
        
        fileSeparator = new JSeparator();
        menu.add(fileSeparator);
        
        //Build the next menu.
        menu=new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(menu);

        // popupMenu is a copy of the edit menu
        
        popupMenu = new JPopupMenu();

        for (int it = 0; it < 2; it++) {
            ArrayList<JMenuItem> list = new ArrayList<JMenuItem>();

            menuItem = new JMenuItem("Add Tracks to PlayList",KeyEvent.VK_A);
            list.add(menuItem);
            menuItem.setActionCommand("menu-playlist");
            menuItem.addActionListener(this);

            menuItem = new JMenuItem("Find...", KeyEvent.VK_F);
            list.add(menuItem);
            menuItem.setActionCommand("menu-find");
            if (it == 0)
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
            menuItem.addActionListener(this);

            menuItem = new JMenuItem("Edit Track Tags...", KeyEvent.VK_T);
            list.add(menuItem);
            menuItem.setActionCommand("menu-edit");
            menuItem.addActionListener(this);

            menuItem = new JMenuItem("View Track Properties...", KeyEvent.VK_P);
            list.add(menuItem);
            menuItem.setActionCommand("menu-properties");
            menuItem.addActionListener(this);

            menuItem = new JMenuItem("Delete Tracks From Library...", KeyEvent.VK_D);
            list.add(menuItem);
            menuItem.setActionCommand("menu-delete");
            menuItem.addActionListener(this);

            // Copy and Paste actions added from the java tutorial
            // j2sdk1.4.2_09/tutorial/uiswing/misc/dnd.html
            TransferActionListener actionListener = new TransferActionListener(mainTable);
            menuItem = new JMenuItem("Copy");
            menuItem.setActionCommand((String) TransferHandler.getCopyAction().
                    getValue(Action.NAME));
            menuItem.addActionListener(actionListener);
            if (it == 0)
                menuItem.setAccelerator(
                    KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
            list.add(menuItem);
            menuItem = new JMenuItem("Paste");
            menuItem.setActionCommand((String) TransferHandler.getPasteAction().
                    getValue(Action.NAME));
            menuItem.addActionListener(actionListener);
            if (it == 0)
                menuItem.setAccelerator(
                    KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
            list.add(menuItem);

            menuItem = new JMenuItem("Validate Tracks", KeyEvent.VK_V);
            list.add(menuItem);
            menuItem.setActionCommand("menu-validate");
            menuItem.addActionListener(this);

            String type = "Library...";
            if (library.attributes.libraryType == 'P') {
                type = "Playlist";
            }
            menuItem = new JMenuItem("Clear " + type, KeyEvent.VK_C);
            list.add(menuItem);
            menuItem.setActionCommand("menu-clear");
            menuItem.addActionListener(this);

            menuItem = new JMenuItem("Clear Last Playing Indicator", KeyEvent.VK_L);
            list.add(menuItem);
            menuItem.setActionCommand("menu-clearlast");
            menuItem.addActionListener(this);


            menuItem = new JMenuItem("Edit Cell", KeyEvent.VK_E);
            list.add(menuItem);
            menuItem.setActionCommand("menu-editcell");
            if (it == 0)
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK));
            menuItem.addActionListener(this);

            // Cell Editing start
            if (it == 0) {
                cellEditMenuItem = new JCheckBoxMenuItem("Automatic Cell Editing");
                list.add(cellEditMenuItem);
                cellEditMenuItem.setActionCommand("menu-celledit");
                cellEditMenuItem.addActionListener(this);
            }

            menuItem = new JMenuItem("Combine Duplicates");
            list.add(menuItem);
            menuItem.setActionCommand("menu-combine");
            menuItem.addActionListener(this);

            menuItem = new JMenuItem("Customize");
            list.add(menuItem);
            menuItem.setActionCommand("menu-customize");
            menuItem.addActionListener(this);

            menuItem = new JMenuItem("Font...");
            list.add(menuItem);
            menuItem.setActionCommand("menu-font");
            menuItem.addActionListener(this);

            menuItem = new JMenuItem("Reset Font");
            list.add(menuItem);
            menuItem.setActionCommand("menu-resetfont");
            menuItem.addActionListener(this);
            
            switch (it) {
                case 0:
                    for (JMenuItem item : list) {
                        menu.add(item);
                    }
                    break;
                case 1:
                    for (JMenuItem item : list) {
                        popupMenu.add(item);
                    }
                    break;
            }

        }
        

        //Build the next menu.
        menu=new JMenu("Play");
        menu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(menu);
        
        menuItem = new JMenuItem("Play/Pause", KeyEvent.VK_P);
        menu.add(menuItem);
        menuItem.setActionCommand("play-or-pause");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P,InputEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Stop", KeyEvent.VK_S);
        menu.add(menuItem);
        menuItem.setActionCommand("play-stop");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S,InputEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Previous", KeyEvent.VK_V);
        menu.add(menuItem);
        menuItem.setActionCommand("play-prev");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B,InputEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Next", KeyEvent.VK_N);
        menu.add(menuItem);
        menuItem.setActionCommand("play-next");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N,InputEvent.CTRL_MASK));
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("Shuffle", KeyEvent.VK_H);
        menu.add(menuItem);
        menuItem.setActionCommand("shuffle");
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H,InputEvent.CTRL_MASK));
        menuItem.addActionListener(this);

        announceMenuItem = new JCheckBoxMenuItem("Announcements");
        announceMenuItem.setMnemonic(KeyEvent.VK_A);
        menu.add(announceMenuItem);
        announceMenuItem.setActionCommand("menu-announce");
        announceMenuItem.addActionListener(this);
        
        continuousMenuItem = new JCheckBoxMenuItem("Continuous");
        continuousMenuItem.setMnemonic(KeyEvent.VK_C);
        menu.add(continuousMenuItem);
        continuousMenuItem.setActionCommand("menu-continuous");
        continuousMenuItem.addActionListener(this);
        
        
        //Build the next menu.
        menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(menu);

        menuItem = new JMenuItem("Help...", KeyEvent.VK_H);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-help");
        menuItem.addActionListener(this);
        
        menuItem = new JMenuItem("About Jampal...", KeyEvent.VK_A);
        menu.add(menuItem);
        menuItem.setActionCommand("menu-about");
        menuItem.addActionListener(this);
        
        getMRUList();
        installMRUList();
        
    }
    
//    JPopupMenu createPopupMenu(JMenu menu) {
//        JPopupMenu popup = new JPopupMenu();
//
//        Component items[] = menu.getMenuComponents();
//        for (Component item : items) {
//            if (item instanceof JMenuItem)
//                popup.add(item);
//        }
//        return popup;
//
//        JMenuItem menuItem;
//
//        menuItem = new JMenuItem("Add Tracks to PlayList",KeyEvent.VK_T);
//        popupMenu.add(menuItem);
//        menuItem.setActionCommand("menu-playlist");
//        menuItem.addActionListener(this);
//
//        menuItem = new JMenuItem("Edit Track Tags...", KeyEvent.VK_T);
//        popupMenu.add(menuItem);
//        menuItem.setActionCommand("menu-edit");
//        menuItem.addActionListener(this);
//
//        menuItem = new JMenuItem("View Track Properties...", KeyEvent.VK_P);
//        popupMenu.add(menuItem);
//        menuItem.setActionCommand("menu-properties");
//        menuItem.addActionListener(this);
//
//        String type = "Library...";
//        if (library.attributes.libraryType == 'P')
//            type = "Playlist";
//
//        menuItem = new JMenuItem("Delete Tracks From "+ type + "...", KeyEvent.VK_D);
//        popupMenu.add(menuItem);
//        menuItem.setActionCommand("menu-delete");
//        menuItem.addActionListener(this);
//
//        // Copy and Paste actions added from the java tutorial
//        // j2sdk1.4.2_09/tutorial/uiswing/misc/dnd.html
//        TransferActionListener actionListener = new TransferActionListener(mainTable);
//        menuItem = new JMenuItem("Copy");
//        menuItem.setActionCommand((String)TransferHandler.getCopyAction().
//                 getValue(Action.NAME));
//        menuItem.addActionListener(actionListener);
//        menuItem.setAccelerator(
//          KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
//        popupMenu.add(menuItem);
//        menuItem = new JMenuItem("Paste");
//        menuItem.setActionCommand((String)TransferHandler.getPasteAction().
//                 getValue(Action.NAME));
//        menuItem.addActionListener(actionListener);
//        menuItem.setAccelerator(
//          KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
//        popupMenu.add(menuItem);
//
//        menuItem = new JMenuItem("Validate Tracks", KeyEvent.VK_V);
//        popupMenu.add(menuItem);
//        menuItem.setActionCommand("menu-validate");
//        menuItem.addActionListener(this);
//
//        menuItem = new JMenuItem("Clear "+type, KeyEvent.VK_C);
//        popupMenu.add(menuItem);
//        menuItem.setActionCommand("menu-clear");
//        menuItem.addActionListener(this);
//
//        menuItem = new JMenuItem("Clear Last Playing Indicator", KeyEvent.VK_L);
//        popupMenu.add(menuItem);
//        menuItem.setActionCommand("menu-clearlast");
//        menuItem.addActionListener(this);
        
//    }
    
    
    void createPlayButtons(JPanel contentPane, GridBagConstraints constraints) {
        JButton button;
        ImageIcon playIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/prev.png"));
        button = new JButton(playIcon);
        button.addActionListener(this);
        button.setActionCommand("play-prev");
        button.setToolTipText("Play Previous Track");
        contentPane.add(button,constraints);
        constraints.gridx++;
        
        playIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/play.png"));
        button = new JButton(playIcon);
        button.addActionListener(this);
        button.setActionCommand("play");
        button.setToolTipText("Play Track");
        contentPane.add(button,constraints);
        constraints.gridx++;
        
        playIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/fwd.png"));
        button = new JButton(playIcon);
        button.addActionListener(this);
        button.setActionCommand("play-next");
        button.setToolTipText("Play Next Track");
        contentPane.add(button,constraints);
        constraints.gridx++;
        
        playIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/pause.png"));
        button = new JButton(playIcon);
        button.addActionListener(this);
        button.setActionCommand("play-pause");
        button.setToolTipText("Pause");
        contentPane.add(button,constraints);
        constraints.gridx++;
        
        playIcon = new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/stop.png"));
        button = new JButton(playIcon);
        button.addActionListener(this);
        button.setActionCommand("play-stop");
        button.setToolTipText("Stop");
        contentPane.add(button,constraints);
        constraints.gridx++;
        
        button = new JButton("Shuffle");
        button.addActionListener(this);
        button.setActionCommand("shuffle");
        button.setToolTipText("Randomize order of Tracks");
        contentPane.add(button,constraints);
        constraints.gridx++;
        
    }
    
    void createHeaderRenderers() {
        TableColumnModel columnModel = mainTable.getColumnModel();
        JTableHeader header = mainTable.getTableHeader();
        TableCellRenderer defaultHeaderRenderer = header.getDefaultRenderer();
        TableCellRenderer defaultCellRenderer = mainTable.getDefaultRenderer(String.class);
        MyRenderer cellRenderer = new MyRenderer(defaultCellRenderer);

        int col;
        for (col=0;col<mainTable.getColumnCount();col++) {
            TableColumn column = columnModel.getColumn(col);
            MyRenderer myRenderer = new MyRenderer(defaultHeaderRenderer);
            column.setHeaderRenderer(myRenderer);
            column.setCellRenderer(cellRenderer);
        }
    }
    
    static ImageIcon altAlbumIcon =
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/cd_go.png"));
//        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/tiny_alt_album.png"));
    static ImageIcon hasAltIcon =
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/cd.png"));
    static ImageIcon playingIcon = 
        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/tiny_play.png"));
//    static ImageIcon blankIcon =
//        new ImageIcon(ClassLoader.getSystemResource("pgbennett/jampal/tiny_blank.png"));
    
    class MyRenderer implements TableCellRenderer, Runnable {

        
        TableCellRenderer guiDefault;
        Icon icon;
//        ArrayList<int[]> changeList = new ArrayList<int[]>();
        HashSet<int[]> changeList = new HashSet<int[]>();

        
        MyRenderer(TableCellRenderer guiDefault) {
            this.guiDefault = guiDefault;
        }
        void setIcon(Icon icon) {
            this.icon = icon;
        }
        
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {
            // This is to highlight the focus cell
//            int selectedCol;
//            int selectedColCount = table.getSelectedColumnCount();
//            if (selectedColCount != 1)
//                selectedCol = -1;
//            else
//                selectedCol = table.getSelectedColumn();
            int oldFocusRow;
            int oldFocusCol;
            oldFocusRow = focusRow;
            oldFocusCol = focusCol;
            if (hasFocus) {
//                isSelected=false;
                focusRow = row;
                focusCol = column;
            }
//            System.out.println("getTableCellRendererComponent,"+row+","+column+","+focusRow+","+focusCol+","+hasFocus);
//            if (focusRow == -1 || focusCol == -1)
//                updateFocus();
            if (row == focusRow && column == focusCol) {
                isSelected=false;
                hasFocus=true;
            }
            if (oldFocusCol != -1 && oldFocusRow != -1) {
                if (focusRow != oldFocusRow || focusCol != oldFocusCol) {
                    int [] changeCell =  {oldFocusRow,oldFocusCol} ;
                    changeList.add(changeCell);
                    SwingUtilities.invokeLater(this);
//                    int oldModelCol = mainTable.convertColumnIndexToModel(oldFocusCol);
//                    model.fireTableCellUpdated(oldFocusRow, oldModelCol);
                }
            }


//            if (row == 0 && column == 0)
//                System.out.println(focusRow+","+focusCol);
//            if (column == selectedCol) // && row == focusRow)
//                isSelected=false;
            JLabel label = (JLabel)guiDefault.getTableCellRendererComponent(table,  value,
            isSelected, hasFocus, row, column);
            if (row==-1) {
                label.setHorizontalTextPosition(JLabel.LEADING);
                label.setIcon(icon);
            }
            else {
                LibraryTrack track = library.trackVec.get(row);
                label.setIcon(null);
                int modelIndex = library.mainFrame.mainTable.convertColumnIndexToModel(column);
                int type=library.attributes.colType[library.attributes.displayCol[modelIndex]];
                if (type == MP3File.COL_TITLE) {
                    if (track.equals(trackLastPlaying)) {
                        label.setIcon(playingIcon);
                        label.setHorizontalTextPosition(JLabel.TRAILING);
                    }
                }
                if (type == MP3File.COL_ALBUM) {
                    if (track.alternateNum > 0) {
                        label.setIcon(altAlbumIcon);
                        label.setHorizontalTextPosition(JLabel.LEADING);
                    }
                    else if (track.hasAlternates) {
                        label.setIcon(hasAltIcon);
//                        label.setHorizontalTextPosition(JLabel.TRAILING);
                        label.setHorizontalTextPosition(JLabel.LEADING);
                    }

                }
                // Check for delete row
                if (library.attributes.deleteFrameIx != -1) {
//                    Font font = getSelectedFont();
                    if (track.columns[library.attributes.deleteFrameIx].indexOf('D')!= -1) {
                        label.setFont(strikeoutFont);
                    }
                    else
                        label.setFont(selectedFont);
                }
            }
            return label;
        }
        
        @Override
        public void run() {
            for ( int [] changeCell : changeList) {
                int oldModelCol = mainTable.convertColumnIndexToModel(changeCell[1]);
                model.fireTableCellUpdated(changeCell[0], oldModelCol);
            }
            changeList.clear();
        }
    }
    
    static final int[] m3jColType = { 
         MP3File.COL_TITLE,
         MP3File.COL_ARTIST,
         MP3File.COL_FILENAME
    };

    
    void fileLoadm3u() {
        openPlayList();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select mu3 playlist file");
        String currentDirectory=savedSettings.getProperty("m3ucurrentdirectory");
        if (currentDirectory!=null)
            fileChooser.setCurrentDirectory(new File(currentDirectory));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        Mp3FileFilter m3uFileFilter = new Mp3FileFilter();
        m3uFileFilter.setExtension("m3u","m3u Playlist Files");
        fileChooser.addChoosableFileFilter(m3uFileFilter);
        Mp3FileFilter m3jFileFilter = new Mp3FileFilter();
        m3jFileFilter.setExtension("m3j","m3j Jampal Playlist Files");
        fileChooser.addChoosableFileFilter(m3jFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(m3jFileFilter);
        int returnVal = fileChooser.showOpenDialog(frame);
        currentDirectory=fileChooser.getCurrentDirectory().getAbsolutePath();
        String fixDirectory;
        try {
            fixDirectory = library.attributes.translateFileName(currentDirectory);
        } catch (JampalException ja) {
            ja.printStackTrace();
            fixDirectory = currentDirectory;
        }
        savedSettings.setProperty("m3ucurrentdirectory",fixDirectory);
        
        try {
            if (returnVal == fileChooser.APPROVE_OPTION) {
                
                Mp3FileFilter selectedFilter = (Mp3FileFilter)fileChooser.getFileFilter();
                String reqdExtension = selectedFilter.reqdExtension;
                TransferObject transferObject = null;

                File playlistFile = fileChooser.getSelectedFile();
                FileReader fileReader = new FileReader(playlistFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);

                Vector vec = new Vector();
                for (;;) {
                    String trackStr = bufferedReader.readLine();
                    if (trackStr==null)
                        break;
                    if ("m3u".equals(reqdExtension)) {
                        File trackFile = new File(trackStr);
                        vec.add(trackFile);
                    }
                    if ("m3j".equals(reqdExtension)) {
                        vec.add(trackStr);
                    }
                }
                
                Transferable transferable = null;
                
                if ("m3j".equals(reqdExtension)) {
                    transferObject = new TransferObject();
                    transferObject.colCount = 3;
                    transferObject.colType = m3jColType;
                    transferObject.colTag = new String[transferObject.colCount];
                    transferObject.fileNameCol = 2;
                    transferObject.fileCount = vec.size();
                    transferObject.columns = new String[transferObject.fileCount][transferObject.colCount];
                    transferObject.hasAlternates = new boolean[transferObject.fileCount];
                    transferObject.alternateNum = new short[transferObject.fileCount];
                    transferObject.hashCodes = new int[transferObject.fileCount];
                    int ix;
                    for (ix=0;ix < transferObject.fileCount; ix++) {
                        String [] split = ((String)(vec.get(ix))).split("\t");
                        int leng = split.length;
                        transferObject.columns[ix][2]="/*";
                        if (leng>0) {
                            transferObject.columns[ix][0]=split[0];
                            transferObject.columns[ix][2] = 
                                transferObject.columns[ix][2]+split[0];
                        }
                        if (leng>1) {
                            transferObject.columns[ix][1]=split[1];
                            transferObject.columns[ix][2] = 
                                transferObject.columns[ix][2] +" - "+split[1];
                        }
                    }
                    transferable = new TrackTransferable(transferObject,null);
                }
                
                if ("m3u".equals(reqdExtension)) {
                    transferObject = null; // new TransferObject();
                    transferable = new TrackTransferable(transferObject,vec);
                }
                TransferHandler handler = playListFrame.mainTable.getTransferHandler();
                handler.importData(playListFrame.mainTable,transferable);
                // Select last playing track also
                int playRow = playListFrame.library.trackVec.indexOf(playListFrame.trackLastPlaying);
                if (playRow >= 0)
                    playListFrame.mainTable.addRowSelectionInterval(playRow,playRow);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    JFileChooser addTrackFileChooser;
    JCheckBox deleteTagcb;
    void fileAddTracks() {
        if (library.readOnly){
            JOptionPane.showMessageDialog(frame,
                         "Library is Read Only",
                         "Error",
                         JOptionPane.ERROR_MESSAGE);
            return;
        }
        addTrackFileChooser = new JFileChooser();
        addTrackFileChooser.setDialogTitle("Select mp3 files or directories containing mp3 files");
        String currentDirectory=savedSettings.getProperty("trackcurrentdirectory");
        if (currentDirectory!=null)
            addTrackFileChooser.setCurrentDirectory(new File(currentDirectory));
        addTrackFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        addTrackFileChooser.setMultiSelectionEnabled(true);
        addTrackFileChooser.addChoosableFileFilter(mp3FileFilter);
        addTrackFileChooser.setAcceptAllFileFilterUsed(false);
        addTrackFileChooser.setFileFilter(mp3FileFilter);
        deleteTagcb = new JCheckBox();
        deleteTagcb.setActionCommand("cbox-deletetag");
        deleteTagcb.addActionListener(this);
        JComponent accessory = createAccessoryPanel(deleteTagcb);
        addTrackFileChooser.setAccessory(accessory);
        try {
            int returnVal = addTrackFileChooser.showOpenDialog(frame);
            currentDirectory=addTrackFileChooser.getCurrentDirectory().getAbsolutePath();
            String fixDirectory;
            try {
                fixDirectory = library.attributes.translateFileName(currentDirectory);
            } catch (JampalException ja) {
                ja.printStackTrace();
                fixDirectory = currentDirectory;
            }
            savedSettings.setProperty("trackcurrentdirectory",fixDirectory);
            if (returnVal == addTrackFileChooser.APPROVE_OPTION) {
                fileChoose = addTrackFileChooser.getSelectedFiles();
                if (deleteTagcb.isSelected()) {
                    fileChoose = new File[1];
                    fileChoose [0]= addTrackFileChooser.getSelectedFile();
                    if (!removeTag()) {
                        fileChoose=null;
                        return;
                    }
                }
                mainTable.clearSelection();
                int rows = mainTable.getRowCount();
                // Select last row so it adds them there
                if (rows >= 1)
                    mainTable.addRowSelectionInterval(rows-1,rows-1);
                progressBox = new ProgressBox(frame,"Add Tracks to Library");
                library.startBulkUpdate();
                Thread thread = new Thread(this,"mainframe-addtracks-files");
                runType = 'A'; // Add Tracks
                thread.start();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        }
        finally {
            deleteTagcb.removeActionListener(this);
            deleteTagcb=null;
            addTrackFileChooser=null;
        }
    }
    
    JComponent createAccessoryPanel(JCheckBox cb) {

        Font font = new Font ("Dialog", Font.PLAIN,11);
        GridBagConstraints constraints;
        constraints = new GridBagConstraints();
        JPanel panel = new JPanel(new GridBagLayout());

        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(4,4,4,4);
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.anchor = GridBagConstraints.PAGE_START;
        constraints.gridwidth=1;
        constraints.gridy=0;
        constraints.gridx=0;
        JTextArea text = new JTextArea("Click Here\nto Remove\nID3v2 Tags");
        panel.add(text,constraints);
        text.setEditable(false);
        text.setBackground(null);
        text.setFont(font);
        constraints.gridy++;
        panel.add(cb,constraints);
        constraints.gridy++;
        constraints.weighty = 0.1;
        JLabel dummy = new JLabel();
        panel.add(dummy,constraints);
        return panel;
        
    }    
    
    boolean removeTag() throws Exception{
            
        int resp = JOptionPane.showConfirmDialog(frame,
        "Are you sure you want to remove the tag ?",
        "Remove Id3v2 Tag",
        JOptionPane.YES_NO_OPTION);
        if (resp != JOptionPane.YES_OPTION)
            return false;
        resp = JOptionPane.showConfirmDialog(frame,
        "This should only be done if there is a problem\n" +
        "loading the file into Jampal. ID3v2 Tag information\n" +
        "will be lost",
        "Remove Id3v2 Tag",
        JOptionPane.OK_CANCEL_OPTION);
        if (resp != JOptionPane.OK_OPTION)
            return false;
        RandomAccessFile raf=null;
        try {
            raf = new RandomAccessFile(fileChoose[0], "rw");
            raf.seek(0);
            byte [] buf = new byte [3];
            raf.read(buf);
            if ("ID3".equals(new String(buf)))
                raf.writeBytes("XXX");
            else {
                JOptionPane.showMessageDialog(frame,
                "There is no ID3v2 tag in file "+ fileChoose[0],
                "Cannot remove ID3V2 tag",
                JOptionPane.ERROR_MESSAGE);
                return false;
            }

        }
        finally {
            if (raf!=null)
                raf.close();
        }
        
        return true;
    }
    
    Vector <String>errorMessages;
    
    @Override
    public void run() {
        if (progressBox!=null)
            errorMessages = new Vector();
        if (progressBox==null){
            String dialogTitle=null;
            switch (runType) {
                case 'A':
                case 'P':
                    // This is run at the end of the file opening process
                    // in the AWT thread
                    try {
                        library.endBulkUpdate();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                        errorMessages.add(ex.toString());
                    }
                    startPlaying=false;
                    dialogTitle = "Add to Library - Errors";
                    break;
                case 'V':
                    dialogTitle = "Validate - Results";
                    if (errorMessages.size()==0)
                        errorMessages.add("All Selected tracks are valid and have been refreshed");
//                    else {
                    model.resetSort(false);
                    model.fireTableDataChanged();
                    int ix;
                    for (ix=0;ix<selectedTracks.length;ix++) {
                        int selectedRow=library.trackVec.indexOf(selectedTracks[ix]);
                        if (selectedRow >= 0)
                            mainTable.addRowSelectionInterval(selectedRow,selectedRow);
                    }
//                    }
                    selectedTracks=null;
                    break;
                    
            }
            if (errorMessages!=null && errorMessages.size()>0) {
                String [] errorText = new String [errorMessages.size()];
                errorMessages.toArray(errorText);
                FileListDialog listdialog = new FileListDialog(frame, 
                    dialogTitle ,errorText,false,true);
                errorMessages=null;
            }
            runType=0;
            
        }
        else {
            if (library.readOnly){
                progressBox.stop();
                progressBox=null;
                errorMessages.add("Library is Read Only");
                SwingUtilities.invokeLater(this);
                return;
            }
            addCounter=0;
            int ix = 0;
            switch (runType) {
                case 'A':
                    // This is run in a worker thread to add files
                    for (int i = 0;i < fileChoose.length;i++) {
                        if (progressBox.cancel)
                            break;
                        try {
                            addFile(fileChoose[i]);
                        }
                        catch (Exception ex) {
                            errorMessages.add(ex.toString());
                            ex.printStackTrace();
                        }
                    }
                    break;
                case 'P':
                    // This is run in a worker thread to paste files
                    for (ix=0;ix<transferObject.fileCount;ix++) {
                        if (progressBox.cancel)
                            break;
                        try {
                            transferObject.updateOneFile(ix);
                        }
                        catch (Exception ex) {
                            errorMessages.add(ex.toString());
                            ex.printStackTrace();
                        }
                        addCounter++;
                        progressBox.progressText = "Number of Tracks Pasted: " +ix;
                        progressBox.updateProgress();
                    }
                    break;
                case 'V':
                    ix = 0;
                    Vector newSelectedTracks = new Vector();
                    for (ix=0;ix<selectedTracks.length;ix++) {
                        if (progressBox.cancel) {
                            errorMessages.add("Validation was canceled");
                            break;
                        }
                        String oldFileName="";
                        try {
                            boolean mustUpdate = true;
                            LibraryTrack oldTrack = selectedTracks[ix];
                            oldFileName = oldTrack.columns[library.attributes.fileNameCol];
                            LibraryTrack newTrack;
                            int selectedRow=library.trackVec.indexOf(oldTrack);
                            if (selectedRow >= 0) {
                                newTrack = player.validateSongFile(selectedRow,false);
                                selectedTracks[ix] = newTrack;
                                String newFileName = newTrack.columns[library.attributes.fileNameCol];
                                if (!oldFileName.equals(newFileName)) {
                                    errorMessages.add("Repaired:"+oldFileName+"\n   -> "+newFileName);
                                }
                                else {
                                    File file = new File(oldFileName);
                                    if (!file.isFile() ) {
                                        errorMessages.add("Cannot Repair:"+oldFileName);
                                        newSelectedTracks.add(newTrack);
                                        mustUpdate=false;
                                    }
                                }
                                if (mustUpdate) {
                                    String fileName = newTrack.columns[library.attributes.fileNameCol];
                                    library.updateTrackAllViews(new File(fileName), null);
                                }
                            }
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                            errorMessages.add("Error on file:"+oldFileName+", "+ex.toString());
                            newSelectedTracks.add(selectedTracks[ix]);
                        }
                        addCounter++;
                        progressBox.progressText = "Number of Tracks Validated: " +addCounter;
                        progressBox.updateProgress();
                    }
                    if (newSelectedTracks.size()>0) {
                        selectedTracks = new LibraryTrack [newSelectedTracks.size()];
                        selectedTracks = (LibraryTrack[])newSelectedTracks.toArray(selectedTracks);
                    }
                    break;
            }
            progressBox.stop();
            progressBox=null;
            SwingUtilities.invokeLater(this);
        }
    }
    
    // This is run in a worker thread to add files
    void addFile(File file) throws Exception {
        if (progressBox.cancel)
            return;
        if (file.isDirectory()) {
            File [] files = file.listFiles(mp3FileFilter);
            int i;
            if (files!=null) {
                for (i=0;i<files.length;i++) {
                    if (files[i]!=null)
                        addFile(files[i]);
                }
            }
        }
        else {
            try {
                if (library.attributes.libraryType == 'L')
                    library.synchronizeTrack(null,file, 'A'); //true
                else
                    library.addEntry(null,file);
            }
            catch (Exception ex) {
                errorMessages.add(ex.toString());
                ex.printStackTrace();
            }
            addCounter++;
            progressBox.progressText = "Number of Tracks Added: " +addCounter;
            progressBox.updateProgress();
        }
    }
    

    
    
    void editTrackTags() {
        if (trackEditor==null) {
            editorUpdateCount=0;
            trackEditor = new TrackEditor(this);
            trackEditor.dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            trackEditor.dialog.addWindowListener(this);
        }
        trackEditor.dialog.setExtendedState(JFrame.NORMAL);
        trackEditor.dialog.toFront();
    }

    FileListDialog propsWin = null;

    void ViewTrackProperties() {
//        if (propsWin!= null)
//            propsWin.dialog.dispose();
        try {
            int selectedRow=mainTable.getSelectedRow();
            if (selectedRow < 0)
                return;
            LibraryTrack track = (LibraryTrack)library.trackVec.get(selectedRow);
            String fileName=track.columns[library.attributes.fileNameCol];
            if (track.status == 'D'){
                fileName=null;
                return;
            }
            File file = new File(fileName);
            MP3File mp3= new MP3File();
            mp3.init(file,MP3File.BOTH_TAGS);

            String [] arr = new String[11];
            int ix=0;
            arr[ix++]="File Name:\t"+fileName;
            arr[ix++]="File Size:\t"+mp3.getFileSize();
            arr[ix++]="Mpeg Version:\t"+mp3.getMPEGVersion()+" "+mp3.getMPEGLayer();
            arr[ix++]="Bit Rate:\t"+mp3.getBitRate() + " K";
            arr[ix++]="Sample Rate:\t"+mp3.getSampleRate();
            arr[ix++]="Channel Mode:\t"+mp3.getMPEGChannelMode();
            arr[ix++]="Emphasis:\t"+mp3.getMPEGEmphasis();
            arr[ix++]="Playing Time:\t"+mp3.getPlayingTimeString();
            arr[ix++]="Tag Size:\t"+(mp3.getTagSize()+mp3.getID3v2Tag().getHead().getHeaderSize());
            arr[ix++]="Tag Type:\t"+  (mp3.id3v1Exists() ? "ID3V1 ": "") 
                    + (mp3.id3v2Exists() ? "ID3V2": "");
            arr[ix++]="Frames:\t"+ mp3.getFrameIds();
            String title =  mp3.getTitle() + " - "+ mp3.getArtist();

            if (propsWin == null) {
                propsWin = new FileListDialog(frame, title,
                        arr, false,false);
                propsWin.area.setTabSize(10);
                propsWin.dialog.setVisible(true);
            }
            else {
                propsWin.update(title,arr);
                propsWin.dialog.setVisible(true);
            }
        
        }
        catch(Exception je) {
            je.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            je.toString(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        }
    }
    
    void find() {
        if (searchDialog==null) {
            searchDialog = new SearchDialog(this,false);
            searchDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            searchDialog.setLocationRelativeTo(frame);
        }
        searchDialog.setVisible(true);
        searchDialog.toFront();
    }

    
    
    /** Get an array of file names of all files in the library.
     * Note that this may include nulls if there
     * are blank lines (deleted tracks)
     * @return Array of file names
     */
    String [] getFileNames() {
        int fileCount = mainTable.getRowCount();
        String [] fileNames = new String[fileCount];
        int ix = 0;
        for (ix=0;ix<fileCount;ix++) {
            LibraryTrack track = (LibraryTrack)library.trackVec.get(ix);
            if (track.status != 'D')
                fileNames[ix]=track.columns[library.attributes.fileNameCol];
        }
        return fileNames;
    }
    
    
    void save(String fileType) {
        openPlayList();
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save m3u file");
        String currentDirectory=savedSettings.getProperty("m3ucurrentdirectory");
        if (currentDirectory!=null)
            fileChooser.setCurrentDirectory(new File(currentDirectory));
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        Mp3FileFilter m3uFileFilter = new Mp3FileFilter();
        m3uFileFilter.setExtension("m3u","m3u Playlist Files");
        fileChooser.addChoosableFileFilter(m3uFileFilter);
        Mp3FileFilter m3jFileFilter = new Mp3FileFilter();
        m3jFileFilter.setExtension("m3j","m3j Jampal Playlist Files");
        fileChooser.addChoosableFileFilter(m3jFileFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileFilter(m3jFileFilter);
        int returnVal = fileChooser.showSaveDialog(frame);
        currentDirectory=fileChooser.getCurrentDirectory().getAbsolutePath();
        String fixDirectory;
        try {
            fixDirectory = library.attributes.translateFileName(currentDirectory);
        } catch (JampalException ja) {
            ja.printStackTrace();
            fixDirectory = currentDirectory;
        }
        savedSettings.setProperty("m3ucurrentdirectory",fixDirectory);
        try {
            if (returnVal == fileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String filePath = file.getAbsolutePath();
                Mp3FileFilter selectedFilter = (Mp3FileFilter)fileChooser.getFileFilter();
                String reqdExtension = selectedFilter.reqdExtension;
                if (!filePath.endsWith("."+reqdExtension))
                    file = new File(filePath+"."+reqdExtension);
                PrintWriter m3uWriter = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file, false)));
                int fileCount = playListFrame.mainTable.getRowCount();
                int titleCol = playListFrame.library.attributes.findColumnNum(MP3File.COL_TITLE,null);
                int artistCol = playListFrame.library.attributes.findColumnNum(MP3File.COL_ARTIST,null);
                
                int ix;
                for (ix=0;ix<fileCount;ix++) {
                    LibraryTrack track = (LibraryTrack)playListFrame.library.trackVec.get(ix);
                    if (track.status != 'D') {
                        if ("m3u".equals(reqdExtension))
                            m3uWriter.println(track.columns[playListFrame.library.attributes.fileNameCol]);
                        else 
                            m3uWriter.println(track.columns[titleCol]+"\t"+track.columns[artistCol]);
                    }
                }
                m3uWriter.close();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        }
        
    }
    
    void compress() {
        String error=null;
        try {
            error = updateSavedSettings();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            error = ex.toString();
        }
        if (error!=null)
            JOptionPane.showMessageDialog(frame,
            error,
            "Cannot Save Library",
            JOptionPane.ERROR_MESSAGE);
    }
    
    void deleteTracks() {
        if (library.readOnly) {
            JOptionPane.showMessageDialog(frame,
                         "Library is Read Only",
                         "Error",
                         JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            TrackUpdater updater = new TrackUpdater(this);
            updater.deleteTracks();
            if (trackEditor!=null)
                trackEditor.setSelection();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Cannot Delete Tracks",
            JOptionPane.ERROR_MESSAGE);
            
        }
    }

   
    void validateTracks() {
        if (library.readOnly) {
            JOptionPane.showMessageDialog(frame,
                         "Library is Read Only",
                         "Error",
                         JOptionPane.ERROR_MESSAGE);
            return;
        }
        runType='V'; // Validate
        int [] rows;
        rows = mainTable.getSelectedRows();
        selectedTracks = new LibraryTrack [rows.length];
        int ix;
        for (ix=0;ix<rows.length;ix++) {
            selectedTracks[ix] = (LibraryTrack)library.trackVec.get(rows[ix]);
        }
        progressBox = new ProgressBox(frame,"Validate Tracks");
        Thread thread = new Thread(this,"mainframe-validate");
        thread.start();
    }
    
    
    void reload() {
        try {
            library.loadLibrary();
            // We need to hide here for an instant
            // If we do not do this, AWT / Swing takes a very long
            // time to recover from firing table data changed
            // if current position is at end of list and list is now
            // shorter
            frame.setVisible(false);
            model.fireTableDataChanged();
            frame.setVisible(true);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Reload Library",
            JOptionPane.ERROR_MESSAGE);
        }
        
    }

    void clearLast(){
        int playRow = library.trackVec.indexOf(trackLastPlaying);
        trackLastPlaying=null;
        if (playRow >= 0)
            model.fireTableRowsUpdated(playRow,playRow);
    }
    
    void clearLibrary(){
        if (library.attributes.libraryType == 'L') {
            int resp = JOptionPane.showConfirmDialog(frame,
            "Are you sure you want to clear the library ?",
            "Clear Library",
            JOptionPane.YES_NO_OPTION);
            if (resp != JOptionPane.YES_OPTION)
                return;
        }
        try {
            library.clearLibrary();
            model.fireTableDataChanged();
        }
        catch (Exception ex) {
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Clear Library",
            JOptionPane.ERROR_MESSAGE);
        }
    }
    
    void openLibrary() {
        isOpening=true;
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select Library");
            String currentDirectory=null;
            if (name!=null) {
                File nameFile = new File(name);
                currentDirectory = nameFile.getParent();
                if (currentDirectory==null)
                    currentDirectory = ".";
            }
            if (currentDirectory==null)
                currentDirectory=Jampal.jampalDirectory;
            if (currentDirectory!=null)
                fileChooser.setCurrentDirectory(new File(currentDirectory));
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setAcceptAllFileFilterUsed(false);
            LibraryFileFilter filter = new LibraryFileFilter();
            fileChooser.addChoosableFileFilter(filter);
            fileChooser.setFileFilter(filter);
            int returnVal = fileChooser.showOpenDialog(frame);
            if (returnVal == fileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                String s = file.getName();
                if (s.endsWith(".jampal"))
                    s=s.substring(0,s.length()-7);
                if (s.indexOf(File.separatorChar)==-1)
                    s=fileChooser.getCurrentDirectory()+File.separator+s;
                new MainFrame(s);
                MainFrame newFrame = (MainFrame)mainFrameMap.get(s);
                if (newFrame!=null && newFrame.parentFrame==null)
                    newFrame.parentFrame=this;
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Open Library",
            JOptionPane.ERROR_MESSAGE);
        }
        finally {
            isOpening=false;
        }
    }
    void newLibrary() {
        try {
            Library newlibrary = new Library(this,true,null,null);
            if (newlibrary.attributes!=null) {
                new MainFrame(newlibrary.attributes.propFileName);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "New Library",
            JOptionPane.ERROR_MESSAGE);
        }
    }
    
    boolean startPlaying;
    void playList() {
        openPlayList();
        MainFrame playListFrame = (MainFrame)mainFrameMap.get(library.attributes.playListName);
        playListFrame.mainTable.clearSelection();
        int rows = playListFrame.mainTable.getRowCount();
        // Select last row so it adds them there
        if (rows>0)
            playListFrame.mainTable.addRowSelectionInterval(rows-1,rows-1);
        // add tracks to playlist
        Transferable transferable = new TrackTransferable(this);
        TransferHandler handler = playListFrame.mainTable.getTransferHandler();
        handler.importData(playListFrame.mainTable,transferable);
        // Select last playing track also
        int playRow = playListFrame.library.trackVec.indexOf(playListFrame.trackLastPlaying);
        if (playRow >= 0)
            playListFrame.mainTable.addRowSelectionInterval(playRow,playRow);
        
    }
    
    
    public void openPlayList() {
        new MainFrame(library.attributes.playListName);
        playListFrame = mainFrameMap.get(library.attributes.playListName);
        if (playListFrame!=null && playListFrame.parentFrame==null)
            playListFrame.parentFrame=this;
    }

    void combineDuplicates() {
        try {
            int [] selectedRows = mainTable.getSelectedRows();
            // Accumulate albums and alternates
            TreeSet <String> albumsList = new TreeSet<String>();
            // Fileslist - unique file names only
            TreeSet <String> filesList = new TreeSet<String>();
            String fileNameKeep = null;
            String albumKeep = null;
            // Check if all selected rows and focus row are visible
            Rectangle visibleRect = mainTable.getVisibleRect();
            Rectangle rectUnion = new Rectangle(0,0,-1,-1);
            boolean isFocusSelected=false;
            String [] songNames = new String[selectedRows.length];
            int ixSongName=0;
            boolean songNameMatch = true;
            for (int selectedRow : selectedRows) {
                rectUnion = rectUnion.union(mainTable.getCellRect(selectedRow,focusCol,false));
                LibraryTrack track = library.trackVec.get(selectedRow);
                String fileName=track.columns[library.attributes.fileNameCol];
                filesList.add(fileName);
                track = new LibraryTrack();
                File file = new File(fileName);
                MP3File mp3 = track.readMp3File(file, library.attributes, (short)0);
                if (track.hasAlternates) {
                    for (int ix = 0 ; ix < track.alternateAlbums.length; ix++) {
                        albumsList.add(track.alternateAlbums[ix].trim() + ":" +
                                Integer.parseInt(track.alternateTracks[ix]));
                    }
                }
                if (selectedRow == focusRow) {
                    isFocusSelected=true;
                    fileNameKeep = fileName;
                    albumKeep = mp3.getAlbum().trim() + ":" + mp3.getTrack();
                }
                else {
                    albumsList.add(mp3.getAlbum().trim() + ":" + mp3.getTrack());
                }
                songNames[ixSongName++] = mp3.getTitle() + " - " + mp3.getArtist();
                if (ixSongName > 1) {
                    if (!songNames[ixSongName-1].equals(songNames[ixSongName-2]))
                        songNameMatch=false;
                }

            }
            String deleteFrameId = library.attributes.libraryProperties.getProperty("delete-frame");
            boolean frameFound = false;
            for (String id : library.attributes.colId) {
                if (deleteFrameId.equals(id)) {
                    frameFound = true;
                    break;
                }
            }
            if (!frameFound) {
                JOptionPane.showMessageDialog(frame,
                             "Cannot combine Duplicates.\n" +
                             "Please add *JAMPAL frame to the library",
                             "Error",
                             JOptionPane.ERROR_MESSAGE);
                return;

            }
            if (filesList.size() <= 1) {
                if (selectedRows.length > 1)
                    JOptionPane.showMessageDialog(frame,
                                 "Cannot combine Duplicates.\n" +
                                 "The selected songs are already combined.",
                                 "Error",
                                 JOptionPane.ERROR_MESSAGE);
                else
                    JOptionPane.showMessageDialog(frame,
                                 "Cannot combine Duplicates.\n" +
                                 "Please select more than one file.",
                                 "Error",
                                 JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!isFocusSelected) {
                JOptionPane.showMessageDialog(frame,
                             "Cannot combine Duplicates.\n" +
                             "Please make sure the focus is in a selected row.",
                             "Error",
                             JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!visibleRect.contains(rectUnion)) {
                JOptionPane.showMessageDialog(frame,
                             "Cannot combine Duplicates.\n" +
                             "Please only select rows that are visible.",
                             "Error",
                             JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!songNameMatch) {
                FileListDialog matchDialog = new FileListDialog(frame,
                    "Warning - Song Names do not Match" ,songNames,true,true);
                if (!"ok".equals(matchDialog.response))
                    return;
            }


            String [] confirm = new String[selectedRows.length+1];
            confirm[0]="Version to keep\n" + fileNameKeep;
            confirm[1]="\nVersions to delete";
            int ixConfirm=2;
            for (String fileName : filesList) {
                if (!fileNameKeep.equals(fileName))
                    confirm[ixConfirm++] = fileName;
            }

            FileListDialog listdialog = new FileListDialog(frame,
                "Combine Duplicate Tracks" ,confirm,true,true);
            if (!"ok".equals(listdialog.response))
                return;


            // Update the rows
            for (String fileName : filesList) {
                File file = new File(fileName);
                MP3File mp3 = new MP3File();
                mp3.init(file,MP3File.BOTH_TAGS);
                mp3.syncV1AndV2();
                String deleteFrame = mp3.getMp3Field(MP3File.COL_ID3V2TAG, deleteFrameId).trim();
                if (fileName.equals(fileNameKeep)) {
                    albumsList.remove(albumKeep);
                    StringBuilder alternateBuilder = new StringBuilder();
                    for (String item : albumsList) {
                        alternateBuilder.append(item);
                        alternateBuilder.append("\n");
                    }
                    alternateBuilder.deleteCharAt(alternateBuilder.length()-1);
                    mp3.setMp3Field(MP3File.COL_ID3V2TAG, "COMMengJampal:Alternate",
                            null, null,alternateBuilder.toString());
                    int ixD = deleteFrame.indexOf('D');
                    if (ixD != -1) {
                        deleteFrame = deleteFrame.replace("D", "");
                        mp3.setMp3Field(MP3File.COL_ID3V2TAG, deleteFrameId,
                            null, null, deleteFrame);
                    }

                }
                else {
                    mp3.setMp3Field(MP3File.COL_ID3V2TAG, "COMMengJampal:Alternate",
                            null, null, null);
                    mp3.setMp3Field(MP3File.COL_ID3V2TAG, deleteFrameId,
                        null, null, deleteFrame+"D");
                }
                mp3.writeTags();
                library.updateTrackAllViews(new File(fileName),null);
            }

        } catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        }

    }
    
    void loadSavedSettings() throws Exception{
        int width;
        int height;
        TableColumnModel colModel = mainTable.getColumnModel();
        int tableWidth = getIntProperty("tablewidth");
        if (tableWidth==0)
            tableWidth=640;
        int count = colModel.getColumnCount();
        int i;
        for (i=0;i<count;i++) {
            TableColumn column = colModel.getColumn(i);
            width=getIntProperty("colwidth"+i);
            if (width==0)
                width = tableWidth / count;
            column.setPreferredWidth(width);
        }
        library.loadLibrary();
        String trackPlayingIndexStr = savedSettings.getProperty("tracklastplayingnum");
        if (trackPlayingIndexStr!=null) {
            int trackPlayingIndex = Integer.parseInt(trackPlayingIndexStr);
            if (trackPlayingIndex >= 0 && trackPlayingIndex < library.trackVec.size())
                trackLastPlaying = library.trackVec.get(trackPlayingIndex);
        }
        this.model.fireTableDataChanged();

    }
    
    void restoreSize() throws Exception {
        frame.pack();
        
        int width = getIntProperty("tablewidth");
        if (width==0)
            width=640;
        int height = getIntProperty("tableheight");
        if (height==0)
            height=300;
        frame.setSize(new Dimension(width, height));
        int locationx = getIntProperty("locationx");
        int locationy = getIntProperty("locationy");
        frame.setLocation(locationx,locationy);
        frame.setVisible(true);
        int selectedRow = getIntProperty("selectedrow");
        if (mainTable!=null) {
            mainTable.clearSelection();
            if (selectedRow >= 0 && selectedRow < mainTable.getRowCount()) {
                mainTable.addRowSelectionInterval(selectedRow,selectedRow);
                Rectangle cell1 = mainTable.getCellRect(selectedRow,
                                     0,true);
                mainTable.scrollRectToVisible(cell1);
            }
        }

    }
    
    int getIntProperty(String key) throws Exception {
        String valueS = savedSettings.getProperty(key);
        int result=0;
        if (valueS!=null)
            result = Integer.parseInt(valueS);
        return result;
    }
   
    
    String updateSavedSettings() {
        String librarySaveError = null;
        try {
            Dimension tableDim = frame.getSize(null);
            savedSettings.setProperty("tablewidth",String.valueOf((int)tableDim.getWidth()));
            savedSettings.setProperty("tableheight",String.valueOf((int)tableDim.getHeight()));
            Point location = frame.getLocation();
            savedSettings.setProperty("locationx",String.valueOf((int)location.x));
            savedSettings.setProperty("locationy",String.valueOf((int)location.y));
            int selectedRow = mainTable.getSelectedRow();
            savedSettings.setProperty("selectedrow",String.valueOf(selectedRow));
            savedSettings.setProperty("announcements",String.valueOf(announceMenuItem.isSelected()));
            if (continuousMenuItem!=null)
                savedSettings.setProperty("continuous",String.valueOf(continuousMenuItem.isSelected()));
            TableColumnModel model = mainTable.getColumnModel();
            int count = model.getColumnCount();
            int i;
            for (i=0;i<count;i++) {
                int viewIndex = mainTable.convertColumnIndexToView(i);
                TableColumn column = model.getColumn(viewIndex);
                int width = column.getPreferredWidth();
                savedSettings.setProperty("colwidth"+i,String.valueOf(width));
            }
            if (trackLastPlaying==null)
                savedSettings.remove("tracklastplayingnum");
            else
                savedSettings.setProperty("tracklastplayingnum",String.valueOf(library.trackVec.indexOf(trackLastPlaying)));
            FileOutputStream file = new FileOutputStream(library.attributes.propFileName);
            savedSettings.store(file,null);
            file.close();
            librarySaveError = library.saveLibrary();
        }
        catch(Exception je) {
            je.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            je.toString(),
            "Error saving library settings",
            JOptionPane.ERROR_MESSAGE);
        }
        return librarySaveError;
    }
    
    // Add a mouse listener to the Table to trigger a table sort
    // when a column heading is clicked in the JTable.
    void addMouseListenerToHeaderInTable() {
        mainTable.setColumnSelectionAllowed(false);
        MouseAdapter listHeaderMouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                Component header=mainTable.getTableHeader();
                // Do not sort if resize cursor set
                Cursor cursor = header.getCursor();
                Cursor defaultCursor = Cursor.getDefaultCursor();
                if(cursor != defaultCursor)
                    return;
                TableColumnModel columnModel = mainTable.getColumnModel();
                int viewColumn = columnModel.getColumnIndexAtX(e.getX());
                int column = mainTable.convertColumnIndexToModel(viewColumn);
                if (e.getButton()== MouseEvent.BUTTON1 && e.getClickCount() == 1 && column != -1) {
                    search=new StringBuffer();
                    statusLabel.setText("Tracks Sorted");
                    library.model.setSortCol(column);
                }
            }
        };
        JTableHeader th = mainTable.getTableHeader();
        th.addMouseListener(listHeaderMouseListener);
        th.addMouseListener(listMouseListener);
    }
    
    MouseAdapter listMouseListener;
    void addMouseListenerToTable() {
        listMouseListener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }
            
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
            
            public void mouseClicked(MouseEvent e) {
                if (e.getButton()== MouseEvent.BUTTON1 && e.getClickCount() >= 2
                    && !cellEditCheckBox.isSelected() // Cell Editing
                    && !mainTable.isEditing()
                    ) {
                    player.play();
                }
            }
            
        };
        mainTable.addMouseListener(listMouseListener);
        String s = new String();
        tableCellEditor =
            (DefaultCellEditor)mainTable.getDefaultEditor(s.getClass());
        tableCellEditor.setClickCountToStart(1);
        Component comp = tableCellEditor.getComponent();
        comp.addMouseListener(listMouseListener);
//%%%        Font font = comp.getFont();
//%%%        font=font.deriveFont((float)11);
//%%%        comp.setFont(font);
        
    }
    
    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            popupMenu.show(e.getComponent(),
            e.getX(), e.getY());
        }
    }
    
    // ActionListener
    public void actionPerformed(ActionEvent e) {
        if (search.length() > 0) {
            search = new StringBuffer();
            statusLabel.setText("Search Canceled");
        }

        String command = e.getActionCommand();
        if (command.equals("menu-add"))
            fileAddTracks();
        else if (command.equals("menu-find"))
            find();
        else if (command.equals("menu-edit"))
            editTrackTags();
        else if (command.equals("menu-properties"))
            ViewTrackProperties();
        else if (command.equals("menu-delete"))
            deleteTracks();
        else if (command.equals("menu-open"))
            openLibrary();
        else if (command.equals("menu-new"))
            newLibrary();
        else if (command.equals("menu-clear"))
            clearLibrary();
        else if (command.equals("menu-clearlast"))
            clearLast();
        else if (command.equals("menu-playlist"))
            playList();
        else if (command.equals("menu-openplaylist"))
            openPlayList();
        else if (command.equals("menu-save"))
            save("m3u");
        else if (command.equals("menu-load"))
            fileLoadm3u();
        else if (command.equals("menu-compress"))
            compress();
        else if (command.equals("menu-reload"))
            reload();
        else if (command.equals("play"))
            player.play();
        else if (command.equals("play-stop"))
            player.stop();
        else if (command.equals("play-pause"))
            player.pause();
        else if (command.equals("play-or-pause")) {
            if (player.state == 'P')
                player.pause();
            else
                player.play();
        }
        else if (command.equals("play-next"))
            player.next();
        else if (command.equals("play-prev"))
            player.prev();
        else if (command.equals("shuffle"))
            library.model.shuffle();
        else if (command.equals("cbox-celledit")) { 
            cellEditMenuItem.setSelected(cellEditCheckBox.isSelected());
        }
        else if (command.equals("menu-celledit")) { 
            cellEditCheckBox.setSelected(cellEditMenuItem.isSelected());
        }
        else if (command.equals("menu-editcell")) {
            editCell();
        }
        else if (command.equals("menu-combine")) {
            combineDuplicates();
        }
        else if (command.equals("menu-customize")) {
            if (customizeDialog!=null)
                customizeDialog.dispose();
            customizeDialog = new CustomizeDialog(this);
            customizeDialog.setVisible(true);
        }
        else if (command.equals("menu-font")) {
            onFont();
        }
        else if (command.equals("menu-resetfont")) {
            onResetFont();
        }
        else if (command.equals("menu-about")) {
            new FileListDialog(frame, "About Jampal",AboutText, false,
            true);
        }
        else if (command.equals("menu-help")) {
            openHelpPage();
        }
        else if (command.equals("cbox-deletetag")) {
            if (deleteTagcb.isSelected()) {
                addTrackFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                addTrackFileChooser.setMultiSelectionEnabled(false);
            }
            else {
                addTrackFileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
                addTrackFileChooser.setMultiSelectionEnabled(true);
            }
        }
        else if (command.equals("menu-validate")) {
            validateTracks();
        }
       
    }
    private void openHelpPage() {                                               
        try {
            String helpFileStr = Jampal.environmentProperties.getProperty("helpFile");
            if (helpFileStr.startsWith(".")) {
                helpFileStr = Jampal.baseDirectory + "/" + helpFileStr;
                File abstractPath = new File(helpFileStr);
                helpFileStr = abstractPath.getCanonicalPath();
            }
//            if (File.separatorChar == '/')
//                helpFileStr = "file:///usr/share/doc/jampal/html/summary.html";
//            if (helpFileStr == null) {
//
//                // url will have one of these
//                // Protocol "jar"  file "file:/C:/Program%20Files/.../pbackup.jar!/pgbennett/pbackup/Mainframe.class"
//                // Protocol "file" file "/C:/proj/cvs/pbackup/pbackup_j/build/classes/pgbennett/pbackup/Mainframe.class"
//                URL url =  ClassLoader.getSystemClassLoader().getResource("pgbennett/jampal/MainFrame.class");
//                if (url!=null) {
//                    if ("jar".equals(url.getProtocol())) {
//                        String file = url.getFile();
//                        int end = file.lastIndexOf('!');
//                        end = file.lastIndexOf('/',end);
//                        helpFileStr = "file://" + file.substring(5, end) + "/doc/summary.html";
//                    }
//                }
//            }
            URI uri;
            Desktop desktop = null;
            try {
                desktop = Desktop.getDesktop();
            }
            catch(Exception ex) {
                Logger.getLogger(Jampal.class.getName()).log(Level.SEVERE, null, ex);
                desktop = null;
            }
            Runtime runtime = Runtime.getRuntime();
            String browser = Jampal.initialProperties.getProperty("browser");
            if (helpFileStr != null) {
                File helpFile = new File(helpFileStr);
                if (!helpFile.isFile())
                    helpFileStr=null;
            }
            if (helpFileStr != null) {
                File uriFile = new File(helpFileStr);
                uri = uriFile.toURI();
                if (desktop != null) {
                    try {
                        desktop.browse(uri);
                    } catch (Exception ex) {
                        Logger.getLogger(Jampal.class.getName()).log(Level.SEVERE, null, ex);
                        helpFileStr=null;
                    }
                }
                else {
                    String cmdArray[] = {browser,uri.toString()}; 
                    runtime.exec(cmdArray);
                }
            
            }
            if (helpFileStr == null) {
                helpFileStr = "http://jampal.sourceforge.net";
                uri = new URI(helpFileStr);
                if (desktop != null)
                    desktop.browse(uri);
                else {
                    String cmdArray[] = {browser,uri.toString()}; 
                    runtime.exec(cmdArray);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(frame,
            "Please browse help at http://jampal.sourceforge.net",
            "Unable to display help",
            JOptionPane.ERROR_MESSAGE);
        }
    }                                              
    
    
    StringBuffer search = new StringBuffer();
    long keyTime = 0;
    // KeyListener
    public void keyTyped(KeyEvent e) {
        if (cellEditCheckBox.isSelected()) // Cell Editing
            return; // Cell Editing
        char keyChar = e.getKeyChar();
        int modifiers = e.getModifiersEx();
        if ((modifiers & (InputEvent.CTRL_DOWN_MASK|InputEvent.ALT_DOWN_MASK))!=0)
            return;
        if (Character.isWhitespace(keyChar)&& keyChar!=' ') {
            return;
        }
        if (keyChar == KeyEvent.VK_DELETE)
            deleteTracks();
        if (model.displaySortCols.length == 0)
            return;
        long now = (new Date()).getTime();
        if (now - keyTime > 10000) {
            search = new StringBuffer();
            statusLabel.setText("Search Canceled");
        }

        keyTime = now;
        if (keyChar == KeyEvent.VK_ESCAPE) {
            if (search.length() > 0) {
                search = new StringBuffer();
                statusLabel.setText("Search Canceled");
            }
            return;
        }            
        else {
            if (keyChar == KeyEvent.VK_BACK_SPACE) {
                if (search.length()>0) {
                    search.deleteCharAt(search.length()-1);
                    if (search.length()==0) {
                        statusLabel.setText("Search Canceled");
                        return;
                    }
                }
                else
                    return;
            }

            else {
                if (!editorUpdateActionListening) {
                    timer.addActionListener(editorUpdateActionListener);
                    editorUpdateActionListening=true;
                }
                search.append(keyChar);
            }
            StringBuffer save = search;
            library.model.search(search.toString().toLowerCase());
            search=save;
            statusLabel.setText("Search: "+search);
        }
    }
    
    
    public void keyPressed(KeyEvent e){
        int keyCode = e.getKeyCode();
        int modifiers = e.getModifiersEx();
        if ((modifiers & (InputEvent.CTRL_DOWN_MASK|InputEvent.ALT_DOWN_MASK))!=0)
            return;
        if (keyCode == KeyEvent.VK_F2) {
            e.consume();
            editCell();
            return;
        }
        if (cellEditCheckBox.isSelected()) {
            return;
        }
        if (editCol!=-1) {
            tableCellEditor.cancelCellEditing();
            editCol=-1;
            editRow=-1;
        }
    }
    
    public void keyReleased(KeyEvent e) {
    }

    class EditCellRunnable implements Runnable {
        public void run() {
            editCell();
        }
    }
    
    EditCellRunnable editCellRunnable  = new EditCellRunnable();
    
    public void editCell() {
        if (search.length() > 0) {
            search = new StringBuffer();
            statusLabel.setText("Search Canceled");
        }
//        if (focusRow == -1 || focusCol == -1) {
//            ListSelectionModel rowSelectionModel = mainTable.getSelectionModel();
//            focusRow=rowSelectionModel.getAnchorSelectionIndex();
//            TableColumnModel columnModel =  mainTable.getColumnModel();
//            ListSelectionModel columnSelectionModel = columnModel.getSelectionModel();
//            focusCol = columnSelectionModel.getAnchorSelectionIndex();
//        }
        editRow = focusRow;
        if (focusCol==-1)
            editCol = -1;
        else
            editCol = mainTable.convertColumnIndexToModel(focusCol);
        if (editRow == -1 || editCol == -1)
            return;
        Rectangle cell1 = mainTable.getCellRect(focusRow,
                             focusCol,true);
        mainTable.scrollRectToVisible(cell1);
        mainTable.editCellAt(focusRow,focusCol);
        Component editor = mainTable.getEditorComponent();
        if (editor != null) {
            editor.requestFocusInWindow();
            if (editor instanceof JTextComponent)  {
                JTextComponent textEdit = (JTextComponent)editor;
                Caret caret = textEdit.getCaret();
                caret.setDot(0);
                caret.moveDot(textEdit.getText().length());
            }
        }
        
    }
    
    
    // Interface WindowListener
    public void windowOpened(WindowEvent e) {
    }
    boolean savedOnClose = false;
    public void windowClosing(WindowEvent e) {
        if (frame.equals(e.getWindow())) {
            // prevent duplicate saving in case close button is clicked twice
            if (savedOnClose)
                return;
            try {
                if (trackEditor!=null) {
                    trackEditor.dialog.dispose();
                    trackEditor=null;
                }

                updateSavedSettings();
                mainFrameMap.remove(name);
                player.stop();
                if (library.attributes.libraryType == 'L') {
                    String [] libraryNames = new String[1];
                    libraryNames[0]=name;
                    Jampal.updateLibNamesProperties(libraryNames);
                }
                if (playListFrame!=null && playListFrame!=this) {
                    WindowEvent e1 = new WindowEvent(playListFrame.frame,WindowEvent.WINDOW_CLOSING);
                    MainFrame frame = playListFrame;
                    frame.windowClosing(e1);
                    frame.windowClosed(e1);
                    frame.frame.dispose();
                    playListFrame = null;
                }
                Set mainframes = mainFrameMap.entrySet();
                Iterator it = mainframes.iterator();
                while(it.hasNext()){
                    Map.Entry entry = (Map.Entry)it.next();
                    MainFrame mf = (MainFrame)entry.getValue();
                    if (mf.parentFrame == this && mf != this) {
                        mf.parentFrame = null;
                    }
                }
                
                if (parentFrame!=null && parentFrame.playListFrame == this) {
                    parentFrame.playListFrame = null;
                }
                // frame.dispose();
                savedOnClose=true;
                // Extra cleanup to free up memory
                library.trackVec.clear();
                library.trackMap.clear();
            }
            catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame,
                ex.toString(),
                "Error closing library "+library.libraryFile.getAbsolutePath(),
                JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    public void windowClosed(WindowEvent e){
        if (trackEditor!=null && trackEditor.dialog.equals(e.getWindow()))
            trackEditor=null;
        if (mainFrameMap.isEmpty()&&!isOpening) {
            Jampal.save();
            System.exit(0);
        }
    }
    public void windowIconified(WindowEvent e){
    }
    public void windowDeiconified(WindowEvent e){
    }
    public void windowActivated(WindowEvent e){
    }
    public void windowDeactivated(WindowEvent e){
    }
    
    class ColumnSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent e) {
//            focusRow=-1;
//            focusCol=-1;
//            updateFocus();
            if (cellEditCheckBox.isSelected() && mainTable.getCellEditor()==null) {
//                updateFocus();
                // editCell();
                if (mainTable.getSelectedRowCount() == 1
                    && mainTable.getSelectedColumnCount() == 1) {
                    focusCol = mainTable.getSelectedColumn();
                    SwingUtilities.invokeLater(editCellRunnable);
                }
            }
        }
    }
    
    // ListSelectionListener interface
    public void valueChanged(ListSelectionEvent e){
//        focusRow=-1;
//        focusCol=-1;
//        updateFocus();
        if (search.length()>0) {
            search = new StringBuffer();
            statusLabel.setText("Search Canceled");
        }
        if (!editorUpdateActionListening) {
            timer.addActionListener(editorUpdateActionListener);
            editorUpdateActionListening=true;
        }
        editorUpdateCount=2;
        if (cellEditCheckBox.isSelected() && mainTable.getCellEditor()==null) {
            // editCell();
//            updateFocus();
            if (mainTable.getSelectedRowCount() == 1) {
                focusRow = mainTable.getSelectedRow();
                SwingUtilities.invokeLater(editCellRunnable);
            }
        }
        // updateSelection();
    }
    static DecimalFormat thousandsFormatter = new DecimalFormat("###,###");
    static DecimalFormat zeroFormatter = new DecimalFormat("00");


//    void updateFocus() {
//        int oldFocusRow = focusRow;
//        int oldFocusCol = focusCol;
//        ListSelectionModel rowSelectionModel = mainTable.getSelectionModel();
//        focusRow=rowSelectionModel.getAnchorSelectionIndex();
//        TableColumnModel columnModel =  mainTable.getColumnModel();
//        ListSelectionModel columnSelectionModel = columnModel.getSelectionModel();
//        focusCol = columnSelectionModel.getAnchorSelectionIndex();
//        int selectedColCount = mainTable.getSelectedColumnCount();
//        if (selectedColCount == 1)
//            focusCol = mainTable.getSelectedColumn();
//        int selectedRowCount = mainTable.getSelectedRowCount();
//        if (selectedRowCount == 1)
//            focusRow = mainTable.getSelectedRow();
//        if (oldFocusRow != -1 && oldFocusCol != -1) {
//            System.out.println("updateFocus,"+oldFocusRow+","+oldFocusCol+","+focusRow+","+focusCol);
//            model.fireTableCellUpdated(oldFocusRow, oldFocusCol);
//        }
//    }

    void updateSelection() {
        try {
            int [] rows=mainTable.getSelectedRows();
            if (search.length()==0) {
                int timeCol = library.attributes.findColumnNum(MP3File.COL_PLAYINGTIME,null);
                int timeMSCol = library.attributes.findColumnNum(MP3File.COL_PLAYINGTIMEMS,null);
                int fileSizeCol = library.attributes.findColumnNum(MP3File.COL_FILESIZE,null);
                int fileNameCol = library.attributes.findColumnNum(MP3File.COL_FILENAME,null);
                int ix;
                int totalSize=0;
                int totalTime=0;
                int totalFiles=0;
                Set <String> altFiles = new HashSet<String>();
                for (ix=0; ix<rows.length; ix++) {
                    if (rows[ix] < library.trackVec.size()) {
                        LibraryTrack tr = (LibraryTrack)library.trackVec.get(rows[ix]);
                        if (timeMSCol > -1) {
                            totalTime += Integer.parseInt(tr.columns[timeMSCol]) / 1000 ;
                        }
                        else {
                            if (timeCol > -1) {
                                String colStr = tr.columns[timeCol];
                                if (colStr!=null && colStr.length() > 0) {
                                    String temp[] = colStr.split(":");
                                    if (temp.length == 2)
                                        totalTime += Integer.parseInt(temp[0]) * 60 
                                                    + Integer.parseInt(temp[1]);
                                }
                            }
                        }
                        boolean unique = true;
                        if (tr.nextEntry!=null || tr.prevEntry!= null) {
                            String fileName = tr.columns[fileNameCol];
                            if (altFiles.contains(fileName))
                                unique=false;
                            else
                                altFiles.add(fileName);
                        }
                        if (unique) {
                            totalFiles++;
                            if (fileSizeCol > -1) {
                                String temp = tr.columns[fileSizeCol];
                                if (temp!=null && temp.length()>0)
                                    totalSize += Integer.parseInt(temp.substring(0,temp.length()-1));
                            }
                        }
                    }
                }
                StringBuffer timeStr = new StringBuffer();
                if (totalTime > 0) {
                    long days = totalTime /24/3600;
                    long hours = (totalTime / 3600) % 24;
                    long mins = (totalTime / 60) % 60;
                    long secs = totalTime % 60;

                    timeFormat(days,"day",timeStr);
                    timeFormat(hours,"hour",timeStr);
                    timeFormat(mins,"minute",timeStr);
                    timeFormat(secs,"second",timeStr);
                    timeStr.insert(0,";   Playing Time: ");

                }
                String sizeStr="";
                if (totalSize > 0)
                    sizeStr = ";   Size: " + thousandsFormatter.format(totalSize) + "K";
                if (rows.length == 1)
                    statusLabel.setText("1 Track Selected (Row Number "
                            + thousandsFormatter.format(mainTable.getSelectedRow()+1) + ")" + timeStr + sizeStr);
                else
                    statusLabel.setText(" "+thousandsFormatter.format(rows.length)+" Tracks Selected" 
                            +timeStr+";  " +thousandsFormatter.format(totalFiles)+" Files"+sizeStr);
            }
            if (trackEditor!=null && editorUpdateCount==0) {
                trackEditor.setSelection();
            }
            if (propsWin!= null && propsWin.dialog.isVisible())
                ViewTrackProperties();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Error selecting record ",
            JOptionPane.ERROR_MESSAGE);
        }
    }


    // 12/4/2007 MRU feature
    
    void updateMRUList() {
        getMRUList();
        int ix = 0;
        while (ix != -1) {
            ix = mruList.indexOf(name);
            if (ix != -1)
                mruList.remove(ix);
        }
        mruList.add(0,name);
        while (mruList.size() > mruLimit)
            mruList.remove(mruList.size()-1);
        Preferences prefs = Preferences.userNodeForPackage(Jampal.class);
        for (ix = 0; ix < mruLimit; ix ++) {
            prefs.remove("MRU"+ix);
        }
        for (ix = 0; ix < mruList.size(); ix++) {
            prefs.put("MRU"+ix,mruList.get(ix));
        }
        installMRULists();
    }
    
    void getMRUList () {
        mruList.clear();
        Preferences prefs = Preferences.userNodeForPackage(Jampal.class);
        int ix;
        for (ix = 0; ix < mruLimit; ix++) {
            String entry = prefs.get("MRU"+ix,null);
            if (entry == null) 
                break;
            if (mruList.indexOf(entry) == -1)
                mruList.add(ix,entry);
        }
    }
    

    void installMRULists() {
        Set mainframes = mainFrameMap.entrySet();
        Iterator it = mainframes.iterator();
        while(it.hasNext()){
            Map.Entry entry = (Map.Entry)it.next();
            MainFrame mf = (MainFrame)entry.getValue();
            mf.installMRUList();
        }
    }
    
    void installMRUList() {
        for (Component item : mruComponents) {
            fileMenu.remove(item);
        }
        mruComponents.clear();
        Component [] components = fileMenu.getMenuComponents();
        int ix;
        for (ix = 0; ix < components.length; ix++) {
            if (components[ix] == fileSeparator) {
                break;
            }
        }
        
        for (String mruItem : mruList) {
            JMenuItem menuItem = new JMenuItem(mruItem);
            fileMenu.add(menuItem,++ix);
            mruComponents.add(menuItem);
            menuItem.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    mruActionPerformed(evt);
                }
            });
            menuItem.setActionCommand(mruItem);
        }
        
    }
    
    void mruActionPerformed(ActionEvent evt) {
        String s = evt.getActionCommand();
        if (s.endsWith(".jampal"))
            s=s.substring(0,s.length()-7);
        new MainFrame(s);
        MainFrame newFrame = (MainFrame)mainFrameMap.get(s);
        if (newFrame!=null && newFrame.parentFrame==null)
            newFrame.parentFrame=this;
    }
    
    
    

    static void timeFormat(long days,String name,StringBuffer timeStr) {
        if (days>0) {
            if (timeStr.length()>0)
                timeStr.append(", ");
            timeStr.append(thousandsFormatter.format(days));
            timeStr.append(' ');
            timeStr.append(name);
            if (days>1) 
                timeStr.append('s');
        }
    }

    public Font getSelectedFont() {
        Preferences prefs = Preferences.userNodeForPackage(Jampal.class);
        String name = prefs.get("fontName",null);
        int style = prefs.getInt("fontStyle",0);
        int size = prefs.getInt("fontSize",0);
        if (name != null)
            selectedFont = new Font(name,style,size);
        else
            selectedFont = UIManager.getFont("Label.font");
        return selectedFont;
    }

    public Font getStrikeoutFont() {
        strikeoutFont = getSelectedFont();
        Map<TextAttribute, Object> attributes = new HashMap<TextAttribute, Object>();
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        strikeoutFont = strikeoutFont.deriveFont(attributes);
        return strikeoutFont;
    }
    

    
    void onFont() {
        try {
            FontChooser fontChooser = new FontChooser(frame,true);
            Font font = getSelectedFont();
            int style = font.getStyle();
            int size = font.getSize();
            String fontName = font.getFontName();
            SimpleAttributeSet att = new SimpleAttributeSet();
            StyleConstants.setFontFamily(att, fontName);
            StyleConstants.setFontSize(att, size);
            StyleConstants.setBold(att, (style&Font.BOLD)!=0);
            StyleConstants.setItalic(att, (style&Font.ITALIC)!=0);
            StyleConstants.setForeground(att, Color.BLACK);
            fontChooser.setAttributes(font);
            fontChooser.enableOptions(true, true, false,
                false, false, false,
                false);
            fontChooser.setVisible(true);
            if (fontChooser.getOption() == JOptionPane.OK_OPTION) {
                AttributeSet att2 = fontChooser.getAttributes();
                String name2 = StyleConstants.getFontFamily(att2);
                int size2 = StyleConstants.getFontSize(att2);
                boolean isItalic = StyleConstants.isItalic(att2);
                boolean isBold = StyleConstants.isBold(att2);
                int style2 = 0;
                if (isItalic)
                    style2 |= Font.ITALIC;
                if (isBold)
                    style2 |= Font.BOLD;
                if (!name2.equals(fontName) || style2 != style || size2 != size) {
                    Preferences prefs = Preferences.userNodeForPackage(Jampal.class);
                    prefs.put("fontName",name2);
                    prefs.putInt("fontStyle",style2);
                    prefs.putInt("fontSize",size2);
                    setCustomFontsAll();
                }

            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Error setting font ",
            JOptionPane.ERROR_MESSAGE);
        }

    }

    void onResetFont() {
        try {
                    Preferences prefs = Preferences.userNodeForPackage(Jampal.class);
                    prefs.remove("fontName");
                    prefs.remove("fontStyle");
                    prefs.remove("fontSize");
                    setCustomFontsAll();

        }
        catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(frame,
            ex.toString(),
            "Error setting font ",
            JOptionPane.ERROR_MESSAGE);
        }

    }


    void setCustomFontsAll() throws Exception {
        Set <Map.Entry<String,MainFrame>> mainframes = mainFrameMap.entrySet();
        for (Map.Entry<String,MainFrame> entry : mainframes) {
            MainFrame mf = entry.getValue();
            mf.setCustomFonts();
        }
    }
        
        
    void setCustomFonts() throws Exception {
        Font newFont = getSelectedFont();
        getStrikeoutFont();
        mainTable.setFont(newFont);
        playLabel.setFont(newFont);
        Component comp = tableCellEditor.getComponent();
        comp.setFont(newFont);
        if (trackEditor!=null)
            trackEditor.dialog.dispose();
        if (searchDialog != null) {
            searchDialog.dispose();
            searchDialog=null;
        }
    }

    /**
     * Moved from TrackUpdater finish method
     * This can not run in a worker thread.
     */

    void fireChangeKeepSelection() {
        Set <Map.Entry<String,MainFrame>> mainframes = mainFrameMap.entrySet();
        for (Map.Entry<String,MainFrame> entry : mainframes) {
            MainFrame mf = entry.getValue();

            int [] selection;
            selection = mf.library.selectedRows;
            mf.model.fireTableDataChanged();
            int iy;
            for (iy=0;iy<selection.length;iy++){
                if (selection[iy]!= -1)
                    mf.mainTable.addRowSelectionInterval(selection[iy],selection[iy]);
            }
        }
            
    }



}
