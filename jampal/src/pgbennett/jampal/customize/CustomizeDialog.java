
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
package pgbennett.jampal.customize;

import java.util.logging.Level;
import java.util.logging.Logger;
import pgbennett.jampal.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.awt.*;
import java.io.*;
import pgbennett.id3.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.net.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

/**
 *
 * @author  peter
 */
public class CustomizeDialog extends javax.swing.JDialog {

    MainFrame mainFrame;
    LibraryAttributes attributes;
    Properties libraryProperties;
    static final int GENERAL_PANEL = 0;
    static final int VOICES_PANEL  = 1;
    static final int LIBRARY_PANEL = 2;
    static final int LIBRARY_FIELDS_PANEL = 3;
    static final int DISPLAY_COLUMNS_PANEL = 4;
    static final int TAG_EDITOR_PANEL = 5;
    TagEditTableModel libraryTableModel = new TagEditTableModel(this);
    TagEditTableModel tagEditTableModel = new TagEditTableModel(this);
    DisplayColumnsTableModel displayColumnsTableModel = new DisplayColumnsTableModel(this);
    JComboBox langComboBox;
    JComboBox picTypeComboBox;
    boolean mustReopen;
    boolean mustReload;
    boolean mustReopenAll;
    boolean mustSaveJampal;
    VoicePage voicePage;

    // Jampal Specific type extensions
    static final String [] typeExtensions = { "*JAMPAL", "*ALTERNATE" } ;

    

    /** Creates new form CustomizeDialog */
    public CustomizeDialog(MainFrame mainFrame) {
        super(mainFrame.frame, "Customize - " + mainFrame.shortName);
        this.mainFrame = mainFrame;
        attributes = mainFrame.library.attributes;
        libraryProperties = attributes.libraryProperties;
        try {
            voicePage = new VoicePage(this);
            initComponents();
            // Remove Voices Panel until the coding for it is ready
            //customizePane.remove(voicesPanel);
            initializeValues();
            voicePage.initializeValues();
        }
        catch(Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(mainFrame.frame,
            ex.toString(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
        }
    }
    static final String[] resizeModesText = {
        "Off",
        "Next Column",
        "Subsequent Columns",
        "Last Column",
        "All Columns"
    };
    public static final int[] resizeModesInt = {
        JTable.AUTO_RESIZE_OFF,
        JTable.AUTO_RESIZE_NEXT_COLUMN,
        JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS,
        JTable.AUTO_RESIZE_LAST_COLUMN,
        JTable.AUTO_RESIZE_ALL_COLUMNS
    };
    UIManager.LookAndFeelInfo lookAndFeelValues[];

    void initializeValues() {
        // GAIN
        String gainStr = Jampal.initialProperties.getProperty("mixer-gain-percent");
        if (gainStr == null)
            gainStr = "100";
        gainText.setText(gainStr);
        // LOOK AND FEEL
//        lookAndFeelValues = UIManager.getInstalledLookAndFeels();
        getLookAndFeels();
        String currentValue = Jampal.initialProperties.getProperty("look-and-feel");
        int ix;
        boolean found = false;
        int currentIndex = -1;
        for (ix = 0; ix < lookAndFeelValues.length; ix++) {
            if (lookAndFeelValues[ix]!= null) {
                landfComboBox.addItem(lookAndFeelValues[ix].getName());
                if (lookAndFeelValues[ix].getClassName().equals(currentValue)) {
                    currentIndex = landfComboBox.getItemCount()-1;
                    found = true;
                }
            }
        }
        if (found) {
            landfComboBox.setSelectedIndex(currentIndex);
        } else {
            landfComboBox.addItem(currentValue);
            landfComboBox.setSelectedItem(currentValue);
        }
        // MIXER
        Mixer.Info[] aInfos = AudioSystem.getMixerInfo();
        currentValue = Jampal.initialProperties.getProperty("mixer-name");
        if (currentValue == null) {
            Mixer defaultMixer = AudioSystem.getMixer(null);
            currentValue = defaultMixer.getMixerInfo().getName();
        }
        currentIndex = -1;
        int iy = -1;
        for (ix = 0; ix < aInfos.length; ix++)
        {
            Mixer mixer = AudioSystem.getMixer(aInfos[ix]);
            Line.Info lineInfo = new Line.Info(SourceDataLine.class);
            if (mixer.isLineSupported(lineInfo))
            {
                String value = aInfos[ix].getName();
                mixerNameComboBox.addItem(value);
                iy ++;
                if (value.equals(currentValue)) {
                    currentIndex = iy;
                }
            }
        }
        mixerNameComboBox.setSelectedIndex(currentIndex);
        
        // SPEECH RATE
        currentValue = Jampal.initialProperties.getProperty("speech-rate");
        rateTextField.setText(currentValue);
//        // SPEECH VOLUME
//        currentValue = Jampal.initialProperties.getProperty("speech-volume");
//        speechVolumeTextField.setText(currentValue);
        // MBROLA PROGRAM
        currentValue = Jampal.initialProperties.getProperty("mbrola-prog");
        mbrolaProgTextField.setText(currentValue);
        // MBROLA PATH
        currentValue = Jampal.initialProperties.getProperty("mbrola-path");
        mbrolaPathTextField.setText(currentValue);
        // CEPSTRAL PATH
        currentValue = Jampal.initialProperties.getProperty("cepstral-path");
        cepstralPathTextField.setText(currentValue);
        // ESPEAK PROGRAM
         currentValue = Jampal.initialProperties.getProperty("espeak-prog");
        currentValue = Jampal.getESpeakProg(currentValue);
        eSpeakProgTextField.setText(currentValue);
        // ESPEAK DATA
        currentValue = Jampal.initialProperties.getProperty("espeak-data");
        currentValue = Jampal.getESpeakData(currentValue);
        eSpeakDataTextField.setText(currentValue);
        // ANNOUNCE WAIT
        currentValue = Jampal.initialProperties.getProperty("announce-wait");
        announceWaitTextField.setText(currentValue);

        // SPEECH ENGINE
//        currentValue = Jampal.initialProperties.getProperty("speech-engine");
//        if (currentValue == null) {
//            currentValue = "None";
//        }
//        speechEngineComboBox.setSelectedItem(currentValue);
        // VOICE - do not ned to do because the above combo box action does it
//        setupVoice();
        
        // SPEECHTEMPLATE
        currentValue = libraryProperties.getProperty("speechtemplate");
        speechTemplate.setText(currentValue);
        // SPEECH
        StringBuffer speechTxtA = new StringBuffer();
        StringBuffer speechTxtB = new StringBuffer();
        for (ix = 0;; ix++) {
            currentValue = libraryProperties.getProperty("speech" + (ix + 1) + "a");
            if (currentValue == null) {
                break;
            }
            if (speechTxtA.length() > 0) {
                speechTxtA.append('/');
                speechTxtB.append('/');
            }
            speechTxtA.append(currentValue);
            currentValue = libraryProperties.getProperty("speech" + (ix + 1) + "b");
            if (speechTxtB != null) {
                speechTxtB.append(currentValue);
            }
        }
        speechA.setText(speechTxtA.toString());
        speechB.setText(speechTxtB.toString());
        // UNIX STYLE PATHS
        currentValue = libraryProperties.getProperty("unix-path-translate");
        unixPathCheckBox.setSelected("Y".equals(currentValue));
        // CASE SENSITIVE
        currentValue = libraryProperties.getProperty("filename-case-sensitive");
        caseDefaultRadioButton.setSelected(true);
        caseYesRadioButton.setSelected("Y".equals(currentValue));
        caseNoRadioButton.setSelected("N".equals(currentValue));
        caseDefaultRadioButton.setSelected("Y".equals(currentValue));
        // LIBRARY TYPE
        currentValue = libraryProperties.getProperty("type");
        libraryRadioButton.setSelected("L".equals(currentValue));
        playlistRadioButton.setSelected("P".equals(currentValue));
        // LIBRARY DATA FILE
        currentValue = libraryProperties.getProperty("libraryname");
        libraryDataNameText.setText(currentValue);
        // PLAYLIST NAME
        currentValue = libraryProperties.getProperty("playlist");
        playlistNameText.setText(currentValue);
        // WINDOW STYLE
        currentValue = libraryProperties.getProperty("window-style");
        styleNormalRadioButton.setSelected("normal".equals(currentValue));
        styleCompactRadioButton.setSelected("compact".equals(currentValue));
        // RESIZE MODE
        currentValue = libraryProperties.getProperty("resize-mode");
        int iValue;
        try {
            iValue = Integer.parseInt(currentValue);
        } catch (Exception ex) {
            iValue = JTable.AUTO_RESIZE_OFF;
        }
        for (ix = 0; ix < resizeModesText.length; ix++) {
            resizeModeComboBox.addItem(resizeModesText[ix]);
        }
        resizeModeComboBox.setSelectedIndex(iValue);

        // DELETE FRAME - must be done after library fields

        // LIBRARY FIELDS
        libraryTableModel.isLibrary = true;
        setLibraryTableModel(libraryTableModel, libraryTable,
                attributes.colType, attributes.colId, attributes.colHeading);
        libraryTableModel.frameComboBox.removeItem("Attached picture");

        // DELETE FRAME
//        populateDeleteFrame();

        // DISPLAY COLUMNS FIELDS
        setColumnWidths(displayColumnsTable,libraryColWidths);
        // add the rows that are displayed
        int dCount;
        for (dCount = 0; dCount < attributes.numDisplayCol; dCount++) {
            DisplayEntry dEntry = new DisplayEntry();
            displayColumnsTableModel.entries.add(dEntry);
            dEntry.libraryEntry = (LibraryEntry) libraryTableModel.entries.get(attributes.displayCol[dCount]);
            dEntry.isSelected = true;
            dEntry.libraryEntry.displayEntry = dEntry;
        }
        // now add the rows not displayed
        for (ix = 0; ix < attributes.numLibraryCol; ix++) {
            LibraryEntry entry = (LibraryEntry) libraryTableModel.entries.get(ix);
            if (entry.displayEntry == null) {
                DisplayEntry dEntry = new DisplayEntry();
                displayColumnsTableModel.entries.add(dEntry);
                dEntry.libraryEntry = entry;
                dEntry.isSelected = false;
                dEntry.libraryEntry.displayEntry = dEntry;
            }
        }

        // TAG EDITOR FIELDS
        setLibraryTableModel(tagEditTableModel, tagEditorTable,
                attributes.editColType, attributes.editColId, attributes.editColHeading);
        // exclude types that have no frame
        for (ix = 0; ix < MP3File.columnTypeDescs.length; ix++) {
            String type = MP3File.columnTypeDescs[ix];
            if ("ID3V2TAG".equals(type)) {
                continue;
            }
            String frameName = FrameDictionary.getSingleton().frameProperties.getProperty("FRAME-" + type);
            if (frameName == null) {
                tagEditTableModel.typeComboBox.removeItem(type);
            }
        }

    }
    
    void getLookAndFeels() {
        UIManager.LookAndFeelInfo values1[] = UIManager.getInstalledLookAndFeels();
        Properties extraLooks = new Properties();
        InputStream stream;
        stream = ClassLoader.getSystemResourceAsStream(("pgbennett/jampal/customize/laf.properties"));
        ClassLoader classLoader = getClass().getClassLoader();
        try {
            extraLooks.load(stream);
        }
        catch(IOException ex) {
            Logger.getLogger(CustomizeDialog.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally {
            try {
                stream.close();
            } catch (IOException ex) {
                Logger.getLogger(CustomizeDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        String [] names = extraLooks.getProperty("laf.names","").split(",");
        String [] classes = extraLooks.getProperty("laf.classes","").split(",");
        int limits = names.length;
        if (limits > classes.length)
            limits = classes.length;
        lookAndFeelValues = new UIManager.LookAndFeelInfo [values1.length + limits];
        int ix;
        for (ix = 0 ; ix < values1.length ; ix++ ) {
            lookAndFeelValues [ix] = values1 [ix];
        }
        for (int ix2 = 0; ix2 < limits ; ix2++ ) {
            try {
                classLoader.loadClass(classes[ix2]);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(CustomizeDialog.class.getName()).log(Level.INFO, ex.toString());
                continue;
            }
            catch (NoClassDefFoundError ex) {
                Logger.getLogger(CustomizeDialog.class.getName()).log(Level.INFO, ex.toString());
                continue;
            }
            lookAndFeelValues [ix++] = new UIManager.LookAndFeelInfo(names[ix2],classes[ix2]);
        }
    }

//    ArrayList <String> deleteFrameList;
//    void populateDeleteFrame() {
//        int currentix = deleteFrameComboBox.getSelectedIndex();
//        String currentEntry = null;
//        if (currentix >= 0 && currentix < deleteFrameList.size())
//            currentEntry = deleteFrameList.get(currentix);
//        deleteFrameComboBox.removeAllItems();
//        deleteFrameList = new ArrayList<String>();
//        deleteFrameList.add("TXXXjampal");
//        deleteFrameComboBox.addItem("Default Value");
//        for (LibraryEntry entry : libraryTableModel.entries) {
//            if (!"ID3V2TAG".equals(entry.type)) {
//                continue;
//            }
//            String displayEntry;
//            String internalEntry;
//            FrameAttributes attrib =  FrameDictionary.getSingleton().frameTypes.get(entry.frame);
//            displayEntry =  entry.title;
//            internalEntry=entry.frame;
//            if (entry.language != null) {
//                internalEntry += entry.language;
//            }
//            if (entry.description != null) {
//                internalEntry += entry.description;
//            }
//            deleteFrameList.add(internalEntry);
//            deleteFrameComboBox.addItem(displayEntry);
//        }
//        // select current entry
//        if (currentEntry == null)
//            currentEntry = attributes.libraryProperties.getProperty("delete-frame");
//        currentix = deleteFrameList.indexOf(currentEntry);
//        if (currentix == -1)
//            currentix = 0;
//        deleteFrameComboBox.setSelectedIndex(currentix);
//    }


    void setLibraryTableModel(TagEditTableModel model,
            JTable table,
            int[] colType, String[] colTag, String[] colHeading) {

        setColumnWidths(table, libraryColWidths);

        // .. Type
        TableColumn typeColumn = table.getColumnModel().getColumn(model.COL_TYPE);
        model.typeComboBox = new JComboBox(MP3File.columnTypeDescs);
        model.typeComboBox.addItem(typeExtensions[0]);
        if (!model.isLibrary)
            // Dont encourage adding alternates directly to library, only to tag editor
            model.typeComboBox.addItem(typeExtensions[1]);
        typeColumn.setCellEditor(new DefaultCellEditor(model.typeComboBox));
        // .. Frame
        TableColumn frameColumn = table.getColumnModel().getColumn(model.COL_FRAME);
        model.frameComboBox = new JComboBox(FrameDictionary.getSingleton().frameNames);
        frameColumn.setCellEditor(new DefaultCellEditor(model.frameComboBox));
        // .. Languange / Pic Type
        TableColumn langColumn = table.getColumnModel().getColumn(model.COL_LANGUAGE);
        langComboBox = new JComboBox(attributes.langNames);
        picTypeComboBox = new JComboBox(TrackEditor.pictureTypes);
        langColumn.setCellEditor(new LangPicTypeCellEditor(model));
        ListSelectionModel selModel = table.getSelectionModel();
        selModel.setSelectionMode(selModel.SINGLE_SELECTION);
        // Default string columns
        DefaultCellEditor tableCellEditor =
                (DefaultCellEditor) table.getDefaultEditor(String.class);
        tableCellEditor.setClickCountToStart(1);

        // .. Put values in
        int ix;
        for (ix = 0; ix < colType.length; ix++) {
            LibraryEntry entry = new LibraryEntry();
            model.entries.add(entry);
            String columnTypeDesc = "***ERROR***";
            int iy;
            for (iy = 0; iy < MP3File.columnTypes.length; iy++) {
                if (colType[ix] == MP3File.columnTypes[iy]) {
                    columnTypeDesc = MP3File.columnTypeDescs[iy];
                    break;
                }
            }
            entry.type = columnTypeDesc;
            String tag = colTag[ix];
            if (tag.length() >= 4) {
                String frame = colTag[ix].substring(0, 4);
                FrameAttributes frameAtt = (FrameAttributes) FrameDictionary.getSingleton().frameTypes.get(frame);
                // Store actual frame id in the model, then translate to a heading when
                // displaying
                entry.frame = frame;
                int nextOffset = 4;
                if (frameAtt.langReq) {
                    String langCode;
                    if (colTag[ix].length() >= 7) {
                        langCode = colTag[ix].substring(4, 7);
                        nextOffset = 7;
                    } else {
                        langCode = "eng";
                        nextOffset = 4;
                    }
                    entry.language = langCode;
                } else if (frameAtt.type == 'I') {
                    String picType = colTag[ix].substring(4, 6);
                    entry.language = picType;
                    nextOffset = 6;
                }
                //else 
                String desc = colTag[ix].substring(nextOffset);
                entry.description = desc;
            }
            String title = colHeading[ix];
            entry.title = title;
        }
    }


    class LangPicTypeCellEditor extends AbstractCellEditor implements TableCellEditor {

        JComboBox currentComboBox;
        TagEditTableModel model;

        LangPicTypeCellEditor(TagEditTableModel model) {
            this.model = model;
        }

        public Object getCellEditorValue() {
            return currentComboBox.getSelectedItem();

        }

        public Component getTableCellEditorComponent(JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            String frameId = ((LibraryEntry) (model.entries.get(row))).frame;
            FrameAttributes frameAttributes = (FrameAttributes) FrameDictionary.getSingleton().frameTypes.get(frameId);
            if (frameAttributes.type == 'I') {
                currentComboBox = picTypeComboBox;
            } else {
                currentComboBox = langComboBox;
            }
            currentComboBox.setSelectedItem(model.getValueAt(row, column));
            return currentComboBox;
        }
    }

    // Napkin can only be selected once. If selected again without shutting
    // down between, hangs on creating JFrame
    static boolean napkinWasSelected = false;

    boolean processValues(boolean doUpdate) {
        mustReopen = false;
        mustReopenAll = false;
        mustSaveJampal = false;

        int ix;

        String newValue;
        String currentValue;
        // GAIN
        //float gain = 0;
        int gainPercent = 100;
        currentValue = Jampal.initialProperties.getProperty("mixer-gain-percent");
        newValue = gainText.getText();
        if (newValue.length() == 0) {
            newValue = "100";
        }
        try {
            //gain = Float.parseFloat(newValue);
            gainPercent = Integer.parseInt(newValue);
        } catch (NumberFormatException ex) {
            customizePane.setSelectedIndex(GENERAL_PANEL);
            JOptionPane.showMessageDialog(this,
                    "Invalid value for Gain, must be numeric.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (doUpdate) {
            if (!newValue.equals(currentValue)) {
                Jampal.initialProperties.setProperty("mixer-gain-percent", newValue);
                mustSaveJampal = true;
            }
        }
        // LOOK AND FEEL
        ix = landfComboBox.getSelectedIndex();
        if (ix >= 0) {
            newValue = lookAndFeelValues[ix].getClassName();
        } else {
            newValue = (String) landfComboBox.getSelectedItem();
        }
        currentValue = Jampal.initialProperties.getProperty("look-and-feel");
        if ("net.sourceforge.napkinlaf.NapkinLookAndFeel".equals(currentValue))
            napkinWasSelected=true;
        //LookAndFeel oldlandf = UIManager.getLookAndFeel();
        if (!newValue.equals(currentValue)) {
            mustReopenAll = true;
            if (napkinWasSelected && "net.sourceforge.napkinlaf.NapkinLookAndFeel".equals(newValue)) {
                customizePane.setSelectedIndex(GENERAL_PANEL);
                JOptionPane.showMessageDialog(this,
                        "Please close Jampal and restart before selecting Napkin again",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            try {
                UIManager.setLookAndFeel(newValue);
            } catch (Exception ex) {
                customizePane.setSelectedIndex(GENERAL_PANEL);
                JOptionPane.showMessageDialog(this,
                        "Invalid value for Look And Feel:" + ex.toString(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (doUpdate) {
                Jampal.initialProperties.setProperty("look-and-feel", newValue);
                mustSaveJampal = true;
            } else {
                try {
                    UIManager.setLookAndFeel(currentValue);
//                    JFrame.setDefaultLookAndFeelDecorated(false);
//                    JDialog.setDefaultLookAndFeelDecorated(false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        // MIXER
        newValue = (String) mixerNameComboBox.getSelectedItem();
//        if (newValue==null) {
//            customizePane.setSelectedIndex(GENERAL_PANEL);
//            JOptionPane.showMessageDialog(this,
//                    "Please Select a value for Mixer",
//                    "Error",
//                    JOptionPane.ERROR_MESSAGE);
//            return false;
//        }
        currentValue = Jampal.initialProperties.getProperty("mixer-name");
        if (newValue != null && !newValue.equals(currentValue)) {
            if (doUpdate) {
                Jampal.initialProperties.setProperty("mixer-name", newValue);
                mustSaveJampal = true;
            }
        }
//        //SPEECH ENGINE
//        newValue = (String) speechEngineComboBox.getSelectedItem();
//        if (newValue == null) {
//            newValue = "None";
//        }
//        currentValue = Jampal.initialProperties.getProperty("speech-engine");
//        if (doUpdate) {
//            if (!newValue.equals(currentValue)) {
//                Jampal.initialProperties.setProperty("speech-engine", newValue);
//                mustSaveJampal = true;
//            }
//        }
//        // VOICE
//        if (voiceComboBox.isEnabled()) {
//            newValue = (String) voiceComboBox.getSelectedItem();
//            currentValue = Jampal.initialProperties.getProperty("voice");
//            if (doUpdate) {
//                if (newValue != null && !newValue.equals(currentValue)) {
//                    Jampal.initialProperties.setProperty("voice", newValue);
//                    mustSaveJampal = true;
//                }
//            }
//        }

        // SPEECH RATE
        if (rateTextField.isEnabled()) {
            currentValue = Jampal.initialProperties.getProperty("speech-rate");
            newValue = rateTextField.getText();
            // Check for numeric and in range

            try {
                int value = Integer.parseInt(newValue);
                if (value < -10 || value > 10) {
                    throw new Exception();
                }
            } catch (Exception ex) {
                customizePane.setSelectedIndex(GENERAL_PANEL);
                JOptionPane.showMessageDialog(this,
                        "Invalid value for Speech Rate, must be numeric, -10 to 10.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                return false;
            }
            if (doUpdate) {
                if (newValue != null && !newValue.equals(currentValue)) {
                    Jampal.initialProperties.setProperty("speech-rate", newValue);
                    mustSaveJampal = true;
                }
            }
        }

        // SPEECH VOLUMES

//        if (speechVolumeTextField.isEnabled()) {
//            currentValue = Jampal.initialProperties.getProperty("speech-volume");
//            newValue = speechVolumeTextField.getText();
//            // Check for numeric and in range
//
//            try {
//                int value = Integer.parseInt(newValue);
//                if (value < 0) { //  || value > 100) {
//                    throw new Exception();
//                }
//            } catch (Exception ex) {
//                customizePane.setSelectedIndex(GENERAL_PANEL);
//                JOptionPane.showMessageDialog(this,
//                        "Invalid value for Speech Volume, must be numeric, 0 to 100.",
//                        "Error",
//                        JOptionPane.ERROR_MESSAGE);
//                return false;
//            }
//            if (doUpdate) {
//                if (newValue != null && !newValue.equals(currentValue)) {
//                    Jampal.initialProperties.setProperty("speech-volume", newValue);
//                    mustSaveJampal = true;
//                }
//            }
//        }
        // FREETTSPATH

        // MBROLAPATH
//        if (mbrolaPathTextField.isEnabled()) {
        currentValue = Jampal.initialProperties.getProperty("mbrola-path");
        newValue = mbrolaPathTextField.getText();
        if (doUpdate) {
            if (newValue != null && !newValue.equals(currentValue)) {
                Jampal.initialProperties.setProperty("mbrola-path", newValue);
                mustSaveJampal = true;
                System.setProperty("mbrola.base", newValue);
            }
        }
//        }

        // MBROLAPROG
        currentValue = Jampal.initialProperties.getProperty("mbrola-prog");
        newValue = mbrolaProgTextField.getText();
        if (doUpdate) {
            if (newValue != null && !newValue.equals(currentValue)) {
                Jampal.initialProperties.setProperty("mbrola-prog", newValue);
                mustSaveJampal = true;
                System.setProperty("mbrola.prog", newValue);
            }
        }
        
        // CEPSTRAL PATH
//        if (cepstralPathTextField.isEnabled()) {
            currentValue = Jampal.initialProperties.getProperty("cepstral-path");
            newValue = cepstralPathTextField.getText();
            if (doUpdate) {
                if (newValue != null && !newValue.equals(currentValue)) {
                    Jampal.initialProperties.setProperty("cepstral-path", newValue);
                    mustSaveJampal = true;
                    System.setProperty("cepstral.home", newValue);
                }
            }
//        }

        // ESPEAK PROG
//        if (eSpeakProgTextField.isEnabled()) {
            currentValue = Jampal.initialProperties.getProperty("espeak-prog");
            newValue = eSpeakProgTextField.getText();
            if (doUpdate) {
                if (newValue != null && !newValue.equals(currentValue)) {
                    Jampal.initialProperties.setProperty("espeak-prog", newValue);
                    mustSaveJampal = true;
                    System.setProperty("espeak.prog", newValue);
                }
            }
//        }

        // ESPEAK DATA
//        if (eSpeakDataTextField.isEnabled()) {
            currentValue = Jampal.initialProperties.getProperty("espeak-data");
            newValue = eSpeakDataTextField.getText();
            if (doUpdate) {
                if (newValue != null && !newValue.equals(currentValue)) {
                    Jampal.initialProperties.setProperty("espeak-data", newValue);
                    mustSaveJampal = true;
                    System.setProperty("espeak.data", newValue);
                }
            }
//        }

        // ANNOUNCE WAIT
        currentValue = Jampal.initialProperties.getProperty("announce-wait");
        newValue = announceWaitTextField.getText();
        if (doUpdate) {
            if (newValue != null && !newValue.equals(currentValue)) {
                Jampal.initialProperties.setProperty("announce-wait", newValue);
                mustSaveJampal = true;
                System.setProperty("espeak.data", newValue);
            }
        }
            
        // LIBRARY - SPEECH TEMPLATE
        currentValue = libraryProperties.getProperty("speechtemplate");
        newValue = speechTemplate.getText();
        if (!newValue.equals(currentValue)) {
            mustReopen = true;
            if (doUpdate) {
                libraryProperties.setProperty("speechtemplate", newValue);
            }
        }

        // LIBRARY - SPEECH
        String speechTxtA = speechA.getText();
        String speechTxtB = speechB.getText();
        String[] partsA = speechTxtA.split("/", -1);
        String[] partsB = speechTxtB.split("/", -1);
        if (partsA.length != partsB.length) {
            customizePane.setSelectedIndex(LIBRARY_PANEL);
            JOptionPane.showMessageDialog(this,
                    "Both From and To must include same number of entries (slash separated)",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        for (ix = 0; ix < partsA.length; ix++) {
            Object prev = libraryProperties.getProperty("speech" + (ix + 1) + "a");
            if (!partsA[ix].equals(prev)) {
                mustReopen = true;
                if (doUpdate) {
                    libraryProperties.setProperty("speech" + (ix + 1) + "a", partsA[ix]);
                }
            }
            prev = libraryProperties.getProperty("speech" + (ix + 1) + "b");
            if (!partsB[ix].equals(prev)) {
                mustReopen = true;
                if (doUpdate) {
                    libraryProperties.setProperty("speech" + (ix + 1) + "b", partsB[ix]);
                }
            }
        }

        if (doUpdate) {
            for (;; ix++) {
                Object prev1 = libraryProperties.remove("speech" + (ix + 1) + "a");
                Object prev2 = libraryProperties.remove("speech" + (ix + 1) + "b");
                if (prev1 == null && prev2 == null) {
                    break;
                } else {
                    mustReopen = true;
                }
            }
        }
        // LIBRARY - UNIX PATHS
        currentValue = libraryProperties.getProperty("unix-path-translate");
        newValue = unixPathCheckBox.isSelected() ? "Y" : "N";
        if (!newValue.equals(currentValue)) {
            mustReload = true;
            if (doUpdate) {
                libraryProperties.setProperty("unix-path-translate", newValue);
            }
        }

        // CASE SENSITIVE
        currentValue = libraryProperties.getProperty("filename-case-sensitive");
        if (caseYesRadioButton.isSelected()) {
            newValue = "Y";
        } else if (caseNoRadioButton.isSelected()) {
            newValue = "N";
        } else // caseDefaultRadioButton
        {
            newValue = "";
        }
        if (!newValue.equals(currentValue)) {
            mustReopen = true;
            if (doUpdate) {
                libraryProperties.setProperty("filename-case-sensitive", newValue);
            }
        }

        // LIBRARY TYPE
        currentValue = libraryProperties.getProperty("type");
        if (playlistRadioButton.isSelected()) {
            newValue = "P";
        } //if (libraryRadioButton.isSelected())
        else {
            newValue = "L";
        }

        if (!newValue.equals(currentValue)) {
            if ("L".equals(newValue)) {
                for (LibraryTrack track : mainFrame.library.trackVec) {
                    if (track.hasAlternates) {
                        mustReload = true;
                        break;
                    }
                }
            }
            mustReopen = true;
            if (doUpdate) {
                libraryProperties.setProperty("type", newValue);
            }
        }

        // LIBRARY DATA FILE
        currentValue = libraryProperties.getProperty("libraryname");
        newValue = libraryDataNameText.getText();
        if (!newValue.equals(currentValue)) {
            mustReload = true;
            if (doUpdate) {
                libraryProperties.setProperty("libraryname", newValue);
            }
        }
        // PLAYLIST NAME
        currentValue = libraryProperties.getProperty("playlist");
        newValue = playlistNameText.getText();
        if (!newValue.equals(currentValue)) {
            mustReopen = true;
            if (doUpdate) {
                libraryProperties.setProperty("playlist", newValue);
            } else {
                int resp = JOptionPane.showConfirmDialog(this,
                        "Jampal will not create the new playlist. You must take care of that.",
                        "Warning",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (resp != JOptionPane.OK_OPTION) {
                    return false;
                }
            }
        }
        // WINDOW STYLE
        currentValue = libraryProperties.getProperty("window-style");
        if (styleCompactRadioButton.isSelected()) {
            newValue = "compact";
        } else {
            newValue = "normal";
        }
        if (!newValue.equals(currentValue)) {
            mustReopen = true;
            if (doUpdate) {
                libraryProperties.setProperty("window-style", newValue);
            }
        }
        // RESIZE MODE
        currentValue = libraryProperties.getProperty("resize-mode");
        int iValue = resizeModeComboBox.getSelectedIndex();
        newValue = Integer.toString(iValue);
        if (!newValue.equals(currentValue)) {
            mustReopen = true;
            if (doUpdate) {
                libraryProperties.setProperty("resize-mode", newValue);
            }
        }
//        // DELETE FRAME
//        currentValue = libraryProperties.getProperty("delete-frame");
//        iValue = deleteFrameComboBox.getSelectedIndex();
//        newValue=null;
//        if (iValue != -1)
//            newValue = deleteFrameList.get(iValue);
//        if (newValue == null) {
//            customizePane.setSelectedIndex(LIBRARY_PANEL);
//            JOptionPane.showMessageDialog(this,
//                    "Please select delete frame",
//                    "Error",
//                    JOptionPane.ERROR_MESSAGE);
//            return false;
//        }
//        if (!newValue.equals(currentValue)) {
//            if (doUpdate) {
//                libraryProperties.setProperty("delete-frame", newValue);
//            }
//        }

        // LIBRARY
        // Set up display sequences
        int dPos = 0;
        boolean foundDisplayField = false;
        for (ix = 0; ix < displayColumnsTableModel.getRowCount(); ix++) {
            DisplayEntry dEntry = (DisplayEntry) displayColumnsTableModel.entries.get(ix);
            LibraryEntry lEntry = dEntry.libraryEntry;
            if (dEntry.isSelected) {
                foundDisplayField = true;
                lEntry.displaySequence = ++dPos;
            } else {
                lEntry.displaySequence = 0;
            }
        }
        if (!foundDisplayField) {
            customizePane.setSelectedIndex(DISPLAY_COLUMNS_PANEL);
            JOptionPane.showMessageDialog(this,
                    "At least one display column must be checked",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        currentValue = libraryProperties.getProperty("library-cols");
        StringBuffer newBuff = new StringBuffer();
        boolean foundFilename = false;
        for (ix = 0; ix < libraryTableModel.getRowCount(); ix++) {
            LibraryEntry entry = (LibraryEntry) libraryTableModel.entries.get(ix);
            if (newBuff.length() > 0) {
                newBuff.append(",");
            }
            newBuff.append(entry.type);
            if (entry.type.equals("FILENAME")) {
                foundFilename = true;
            }
            newBuff.append(",");
            if (entry.frame != null) {
                newBuff.append(entry.frame);
            }
            if (entry.language != null) {
                newBuff.append(entry.language);
            }
            if (entry.description != null) {
                newBuff.append(entry.description);
            }
            newBuff.append(",");
            if (entry.title != null) {
                newBuff.append(entry.title);
            }
            newBuff.append(",");
            newBuff.append(entry.displaySequence);
        }
        newValue = newBuff.toString();
        if (!newValue.equals(currentValue)) {
            mustReopen = true;
            if (doUpdate) {
                libraryProperties.setProperty("library-cols", newValue);
            }
        }

        if (!foundFilename) {
            customizePane.setSelectedIndex(LIBRARY_FIELDS_PANEL);
            JOptionPane.showMessageDialog(this,
                    "Library fields must include FILENAME",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }


        // TAG EDITOR
        currentValue = libraryProperties.getProperty("tag-editor");
        newBuff = new StringBuffer();
        for (ix = 0; ix < tagEditTableModel.getRowCount(); ix++) {
            LibraryEntry entry = (LibraryEntry) tagEditTableModel.entries.get(ix);
            if (newBuff.length() > 0) {
                newBuff.append(",");
            }
            newBuff.append(entry.type);
            newBuff.append(",");
            if (entry.frame != null) {
                newBuff.append(entry.frame);
            }
            if (entry.language != null) {
                newBuff.append(entry.language);
            }
            if (entry.description != null) {
                newBuff.append(entry.description);
            }
            newBuff.append(",");
            if (entry.title != null) {
                newBuff.append(entry.title);
            }
        }
        newValue = newBuff.toString();
        if (!newValue.equals(currentValue)) {
            mustReopen = true;
            if (doUpdate) {
                libraryProperties.setProperty("tag-editor", newValue);
            }
        }
        return true;
    }
    static final String[] libraryTableTitles = new String[]{
        "#", "Type", "Frame", "Language", "Description", "Title", ""
    };
    static final int[] libraryColWidths = {35, 100, 200, 100, 100, 200, 15};

    void setColumnWidths(JTable table, int [] colWidths) {
        TableColumnModel colModel = table.getColumnModel();
        int count = colModel.getColumnCount();
        int i;
        for (i = 0; i < count; i++) {
            TableColumn column = colModel.getColumn(i);
            column.setPreferredWidth(colWidths[i]);
        }
    }
    
    
    void terminateEditing() {
        CellEditor editor = libraryTable.getCellEditor();
        if (editor != null)
            editor.stopCellEditing();
        editor = tagEditorTable.getCellEditor();
        if (editor != null)
            editor.stopCellEditing();
        editor = voicesTable.getCellEditor();
        if (editor != null)
            editor.stopCellEditing();
    }    

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        libraryTypebuttonGroup = new javax.swing.ButtonGroup();
        windowStyleButtonGroup = new javax.swing.ButtonGroup();
        caseSensitiveButtonGroup = new javax.swing.ButtonGroup();
        customizePane = new javax.swing.JTabbedPane();
        generalPanel = new javax.swing.JPanel();
        gainLabel = new javax.swing.JLabel();
        gainText = new javax.swing.JTextField();
        landfLabel = new javax.swing.JLabel();
        landfComboBox = new javax.swing.JComboBox();
        fillerLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        mixerNameComboBox = new javax.swing.JComboBox();
        jLabel6 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        rateTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        mbrolaPathTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        cepstralPathTextField = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        eSpeakProgTextField = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
        eSpeakDataTextField = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        mbrolaProgTextField = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        announceWaitTextField = new javax.swing.JTextField();
        voicesPanel = new javax.swing.JPanel();
        jLabel16 = new javax.swing.JLabel();
        voicesScrollPane = new javax.swing.JScrollPane();
        voicesTable = new javax.swing.JTable();
        languageAddButton = new javax.swing.JButton();
        languageDeleteButton = new javax.swing.JButton();
        voiceTestTextField = new javax.swing.JTextField();
        voiceTestButton = new javax.swing.JButton();
        libraryPanel = new javax.swing.JPanel();
        speechLabel = new javax.swing.JLabel();
        speechA = new javax.swing.JTextField();
        speechB = new javax.swing.JTextField();
        unixPathLabel = new javax.swing.JLabel();
        unixPathCheckBox = new javax.swing.JCheckBox();
        caseSensitiveFileNameLabel = new javax.swing.JLabel();
        libraryTypeLabel = new javax.swing.JLabel();
        libraryRadioButton = new javax.swing.JRadioButton();
        playlistRadioButton = new javax.swing.JRadioButton();
        playlistNameLabel1 = new javax.swing.JLabel();
        playlistNameText = new javax.swing.JTextField();
        libraryDataNameLabel = new javax.swing.JLabel();
        libraryDataNameText = new javax.swing.JTextField();
        windowStyleLabel = new javax.swing.JLabel();
        styleNormalRadioButton = new javax.swing.JRadioButton();
        styleCompactRadioButton = new javax.swing.JRadioButton();
        fillerLabel6 = new javax.swing.JLabel();
        resizeModeLabel = new javax.swing.JLabel();
        resizeModeComboBox = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        caseYesRadioButton = new javax.swing.JRadioButton();
        caseNoRadioButton = new javax.swing.JRadioButton();
        caseDefaultRadioButton = new javax.swing.JRadioButton();
        jLabel12 = new javax.swing.JLabel();
        speechTemplate = new javax.swing.JTextField();
        libraryFieldsPanel = new javax.swing.JPanel();
        libraryUpButton = new javax.swing.JButton();
        libraryDownButton = new javax.swing.JButton();
        libraryTableScrollPane = new javax.swing.JScrollPane();
        libraryTable = new javax.swing.JTable();
        libraryAddButton = new javax.swing.JButton();
        libraryDeleteButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        diskplayColumnsPanel = new javax.swing.JPanel();
        displayColumnsScrollPane = new javax.swing.JScrollPane();
        displayColumnsTable = new javax.swing.JTable();
        displayColumnsUpButton = new javax.swing.JButton();
        displayColumnsDownButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        tagEditorPanel = new javax.swing.JPanel();
        tagEditorScrollPane = new javax.swing.JScrollPane();
        tagEditorTable = new javax.swing.JTable();
        tagEditorAddButton = new javax.swing.JButton();
        tagEditorDeleteButton = new javax.swing.JButton();
        tagEditorUpButton = new javax.swing.JButton();
        tagEditorDownButton = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        fillerLabel = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        getContentPane().setLayout(new java.awt.GridBagLayout());

        customizePane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                customizePaneStateChanged(evt);
            }
        });

        generalPanel.setLayout(new java.awt.GridBagLayout());

        gainLabel.setText("Audio Gain"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(gainLabel, gridBagConstraints);

        gainText.setToolTipText("Audio gain: percentage of normal (100 = normal volume) values 0 - 2500");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(gainText, gridBagConstraints);

        landfLabel.setText("Look and Feel"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(landfLabel, gridBagConstraints);

        landfComboBox.setEditable(true);
        String lookAndFeel = Jampal.initialProperties.getProperty("look-and-feel");
        landfComboBox.setToolTipText("Select a value or type a class name"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(landfComboBox, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 99.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(fillerLabel1, gridBagConstraints);

        jLabel5.setText("Settings here affect all libraries"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel5, gridBagConstraints);

        jLabel13.setText("Mixer");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel13, gridBagConstraints);

        mixerNameComboBox.setToolTipText("If sound quality is poor or sound does not play, select a different mixer.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(mixerNameComboBox, gridBagConstraints);

        jLabel6.setText("Speech Settings follow:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel6, gridBagConstraints);

        jLabel8.setText("Speech Rate"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel8, gridBagConstraints);

        rateTextField.setToolTipText("Speech Rate: Values -10 to 10"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(rateTextField, gridBagConstraints);

        jLabel11.setText("Mbrola Path"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel11, gridBagConstraints);

        mbrolaPathTextField.setToolTipText("Disk Directory where mbrola executable is located."); // NOI18N
        mbrolaPathTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mbrolaPathTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(mbrolaPathTextField, gridBagConstraints);

        jLabel10.setText("Cepstral Path"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel10, gridBagConstraints);

        cepstralPathTextField.setToolTipText("Cepstral Home Directory"); // NOI18N
        cepstralPathTextField.setPreferredSize(new java.awt.Dimension(320, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(cepstralPathTextField, gridBagConstraints);

        jLabel14.setText("eSpeak Program");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel14, gridBagConstraints);

        eSpeakProgTextField.setToolTipText("eSpeak command including path if necessary.");
        eSpeakProgTextField.setPreferredSize(new java.awt.Dimension(320, 28));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(eSpeakProgTextField, gridBagConstraints);

        jLabel15.setText("espeak-data path");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel15, gridBagConstraints);

        eSpeakDataTextField.setToolTipText("espeak-data directory.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(eSpeakDataTextField, gridBagConstraints);

        jLabel17.setText("Mbrola Program");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel17, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(mbrolaProgTextField, gridBagConstraints);

        jLabel7.setText("Wait before speech (ms)");
        jLabel7.setMaximumSize(new java.awt.Dimension(168, 18));
        jLabel7.setMinimumSize(new java.awt.Dimension(168, 18));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(jLabel7, gridBagConstraints);

        announceWaitTextField.setToolTipText("Number of milliseconds to wait before generating speech");
        announceWaitTextField.setMaximumSize(null);
        announceWaitTextField.setMinimumSize(null);
        announceWaitTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                announceWaitTextFieldActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        generalPanel.add(announceWaitTextField, gridBagConstraints);

        customizePane.addTab("General", generalPanel);

        voicesPanel.setLayout(new java.awt.GridBagLayout());

        jLabel16.setText("Select voice for each language. Settings affect all libraries.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        voicesPanel.add(jLabel16, gridBagConstraints);

        voicesScrollPane.setPreferredSize(new java.awt.Dimension(0, 0));

        voicesTable.setModel(voicePage.model);
        voicesScrollPane.setViewportView(voicesTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 99.0;
        gridBagConstraints.weighty = 99.0;
        voicesPanel.add(voicesScrollPane, gridBagConstraints);

        languageAddButton.setText("Add");
        languageAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                languageAddButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        voicesPanel.add(languageAddButton, gridBagConstraints);

        languageDeleteButton.setText("Delete");
        languageDeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                languageDeleteButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        voicesPanel.add(languageDeleteButton, gridBagConstraints);

        voiceTestTextField.setText("1 2 3 4 5.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 5.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        voicesPanel.add(voiceTestTextField, gridBagConstraints);

        voiceTestButton.setText("Test:");
        voiceTestButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                voiceTestButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        voicesPanel.add(voiceTestButton, gridBagConstraints);

        customizePane.addTab("Voices", voicesPanel);

        libraryPanel.setLayout(new java.awt.GridBagLayout());

        speechLabel.setText("Speech Replacement from / to:"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(speechLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(speechA, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(speechB, gridBagConstraints);

        unixPathLabel.setText("Use Unix Style Paths:"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(unixPathLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(unixPathCheckBox, gridBagConstraints);

        caseSensitiveFileNameLabel.setText("mp3 File Names are Case Sensitive"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(caseSensitiveFileNameLabel, gridBagConstraints);

        libraryTypeLabel.setText("This Library's Type"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(libraryTypeLabel, gridBagConstraints);

        libraryTypebuttonGroup.add(libraryRadioButton);
        libraryRadioButton.setText("Library"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(libraryRadioButton, gridBagConstraints);

        libraryTypebuttonGroup.add(playlistRadioButton);
        playlistRadioButton.setText("Playlist"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(playlistRadioButton, gridBagConstraints);

        playlistNameLabel1.setText("Playlist Name (Playlist .jampal file name)"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(playlistNameLabel1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(playlistNameText, gridBagConstraints);

        libraryDataNameLabel.setText("Library Index Data File name"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(libraryDataNameLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(libraryDataNameText, gridBagConstraints);

        windowStyleLabel.setText("Window Style for this library"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(windowStyleLabel, gridBagConstraints);

        windowStyleButtonGroup.add(styleNormalRadioButton);
        styleNormalRadioButton.setText("Normal"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(styleNormalRadioButton, gridBagConstraints);

        windowStyleButtonGroup.add(styleCompactRadioButton);
        styleCompactRadioButton.setText("Compact"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(styleCompactRadioButton, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 99.0;
        libraryPanel.add(fillerLabel6, gridBagConstraints);

        resizeModeLabel.setText("Resize Mode"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(resizeModeLabel, gridBagConstraints);

        resizeModeComboBox.setToolTipText("Select how you want the table to behave when resized.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(resizeModeComboBox, gridBagConstraints);

        jLabel1.setText("Settings on this page only affect this library"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(jLabel1, gridBagConstraints);

        caseSensitiveButtonGroup.add(caseYesRadioButton);
        caseYesRadioButton.setText("Yes"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        libraryPanel.add(caseYesRadioButton, gridBagConstraints);

        caseSensitiveButtonGroup.add(caseNoRadioButton);
        caseNoRadioButton.setText("No"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        libraryPanel.add(caseNoRadioButton, gridBagConstraints);

        caseSensitiveButtonGroup.add(caseDefaultRadioButton);
        caseDefaultRadioButton.setText("Default"); // NOI18N
        caseDefaultRadioButton.setToolTipText("Operating System Default");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        libraryPanel.add(caseDefaultRadioButton, gridBagConstraints);

        jLabel12.setText("Announcement template"); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(jLabel12, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        libraryPanel.add(speechTemplate, gridBagConstraints);

        customizePane.addTab("Library", libraryPanel);

        libraryFieldsPanel.setLayout(new java.awt.GridBagLayout());

        libraryUpButton.setText("Move Up"); // NOI18N
        libraryUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                libraryUpButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        libraryFieldsPanel.add(libraryUpButton, gridBagConstraints);

        libraryDownButton.setText("Move Down"); // NOI18N
        libraryDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                libraryDownButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        libraryFieldsPanel.add(libraryDownButton, gridBagConstraints);

        libraryTableScrollPane.setPreferredSize(new java.awt.Dimension(0, 0));
        libraryTableScrollPane.setRequestFocusEnabled(false);

        libraryTable.setModel(libraryTableModel);
        libraryTable.setRequestFocusEnabled(false);
        libraryTableScrollPane.setViewportView(libraryTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 99.0;
        libraryFieldsPanel.add(libraryTableScrollPane, gridBagConstraints);

        libraryAddButton.setText("Add"); // NOI18N
        libraryAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                libraryAddButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        libraryFieldsPanel.add(libraryAddButton, gridBagConstraints);

        libraryDeleteButton.setText("Delete"); // NOI18N
        libraryDeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                libraryDeleteButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        libraryFieldsPanel.add(libraryDeleteButton, gridBagConstraints);

        jLabel2.setText("Values to be stored in this Library Index Data File are defined here."); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        libraryFieldsPanel.add(jLabel2, gridBagConstraints);

        customizePane.addTab("Library Fields", libraryFieldsPanel);

        diskplayColumnsPanel.setLayout(new java.awt.GridBagLayout());

        displayColumnsScrollPane.setPreferredSize(new java.awt.Dimension(0, 0));

        displayColumnsTable.setModel(displayColumnsTableModel);
        displayColumnsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        displayColumnsScrollPane.setViewportView(displayColumnsTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 99.0;
        diskplayColumnsPanel.add(displayColumnsScrollPane, gridBagConstraints);

        displayColumnsUpButton.setText("Move Up"); // NOI18N
        displayColumnsUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayColumnsUpButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        diskplayColumnsPanel.add(displayColumnsUpButton, gridBagConstraints);

        displayColumnsDownButton.setText("Move Down"); // NOI18N
        displayColumnsDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                displayColumnsDownButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        diskplayColumnsPanel.add(displayColumnsDownButton, gridBagConstraints);

        jLabel3.setText("Values to be displayed on the library page and their sequence are selected here."); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        diskplayColumnsPanel.add(jLabel3, gridBagConstraints);

        customizePane.addTab("Display Columns", diskplayColumnsPanel);

        tagEditorPanel.setLayout(new java.awt.GridBagLayout());

        tagEditorScrollPane.setPreferredSize(new java.awt.Dimension(0, 0));

        tagEditorTable.setModel(tagEditTableModel);
        tagEditorScrollPane.setViewportView(tagEditorTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 99.0;
        tagEditorPanel.add(tagEditorScrollPane, gridBagConstraints);

        tagEditorAddButton.setText("Add"); // NOI18N
        tagEditorAddButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagEditorAddButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        tagEditorPanel.add(tagEditorAddButton, gridBagConstraints);

        tagEditorDeleteButton.setText("Delete"); // NOI18N
        tagEditorDeleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagEditorDeleteButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        tagEditorPanel.add(tagEditorDeleteButton, gridBagConstraints);

        tagEditorUpButton.setText("Move Up"); // NOI18N
        tagEditorUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagEditorUpButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        tagEditorPanel.add(tagEditorUpButton, gridBagConstraints);

        tagEditorDownButton.setText("Move Down"); // NOI18N
        tagEditorDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tagEditorDownButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        tagEditorPanel.add(tagEditorDownButton, gridBagConstraints);

        jLabel4.setText("Values defined here are always included in the tag editor."); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        tagEditorPanel.add(jLabel4, gridBagConstraints);

        customizePane.addTab("Tag Editor", tagEditorPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 99.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        getContentPane().add(customizePane, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 99.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(fillerLabel, gridBagConstraints);

        jPanel1.setLayout(new java.awt.GridLayout(1, 0));

        okButton.setText("OK"); // NOI18N
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });
        jPanel1.add(okButton);

        cancelButton.setText("Cancel"); // NOI18N
        cancelButton.setMaximumSize(new java.awt.Dimension(75, 23));
        cancelButton.setMinimumSize(new java.awt.Dimension(75, 23));
        cancelButton.setPreferredSize(new java.awt.Dimension(75, 23));
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        jPanel1.add(cancelButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        getContentPane().add(jPanel1, gridBagConstraints);

        pack();
    }// </editor-fold>//GEN-END:initComponents
    private void mbrolaPathTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mbrolaPathTextFieldActionPerformed
// TODO add your handling code here:
    }//GEN-LAST:event_mbrolaPathTextFieldActionPerformed

    private void displayColumnsDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayColumnsDownButtonActionPerformed
        displayColumnsTableModel.MoveDown(displayColumnsTable);
    }//GEN-LAST:event_displayColumnsDownButtonActionPerformed

    private void displayColumnsUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_displayColumnsUpButtonActionPerformed
        displayColumnsTableModel.MoveUp(displayColumnsTable);
    }//GEN-LAST:event_displayColumnsUpButtonActionPerformed

    private void tagEditorDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagEditorDownButtonActionPerformed
        terminateEditing();
        tagEditTableModel.MoveDown(tagEditorTable);
    }//GEN-LAST:event_tagEditorDownButtonActionPerformed

    private void tagEditorUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagEditorUpButtonActionPerformed
        terminateEditing();
        tagEditTableModel.MoveUp(tagEditorTable);
    }//GEN-LAST:event_tagEditorUpButtonActionPerformed

    private void tagEditorDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagEditorDeleteButtonActionPerformed
        terminateEditing();
        tagEditTableModel.DeleteRow(tagEditorTable);
    }//GEN-LAST:event_tagEditorDeleteButtonActionPerformed

    private void tagEditorAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tagEditorAddButtonActionPerformed
        terminateEditing();
        tagEditTableModel.AddRow(tagEditorTable);
    }//GEN-LAST:event_tagEditorAddButtonActionPerformed

    private void libraryDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_libraryDownButtonActionPerformed
        terminateEditing();
        libraryTableModel.MoveDown(libraryTable);
        mustReload = true;
    }//GEN-LAST:event_libraryDownButtonActionPerformed

    private void libraryUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_libraryUpButtonActionPerformed
        terminateEditing();
        libraryTableModel.MoveUp(libraryTable);
        mustReload = true;
    }//GEN-LAST:event_libraryUpButtonActionPerformed

    private void libraryDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_libraryDeleteButtonActionPerformed
        terminateEditing();
        libraryTableModel.DeleteRow(libraryTable);
        mustReload = true;        
    }//GEN-LAST:event_libraryDeleteButtonActionPerformed

    private void libraryAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_libraryAddButtonActionPerformed
        terminateEditing();
        libraryTableModel.AddRow(libraryTable);
        mustReload = true;        
    }//GEN-LAST:event_libraryAddButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        try {
            terminateEditing();
            boolean isOK = processValues(false);
            if (isOK)
                isOK = voicePage.processValues(false);
            int ix;
            if (isOK) {
                if (mustReload && mainFrame.mainTable.getRowCount() > 0) {
                    int resp = JOptionPane.showConfirmDialog(this,
                            "Changing the library structure requires reloading the library. " +
                            "This may take some time.\n" +
                            "If you interrupt it you will have to manually re-create or restore " +
                            "the library",
                            "Warning",
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (resp != JOptionPane.OK_OPTION) {
                        return;
                    }
                }
                processValues(true);
                voicePage.processValues(true);
                // save, reload, etc.
                if (mustSaveJampal) {
                    Jampal.save();
                }
                if (mustReopen || mustReload) {
                    OutputStream stream = new FileOutputStream(attributes.propFileName);
                    try {
                        libraryProperties.store(stream, null);
                    } finally {
                        stream.close();
                    }
                }
                TrackTransferable transferable = null;
                if (mustReload) {
                    int rowCount = mainFrame.mainTable.getRowCount();
                    if (rowCount > 0) {
                        mainFrame.mainTable.clearSelection();
                        mainFrame.mainTable.addRowSelectionInterval(0, rowCount - 1);
                        transferable = new TrackTransferable(mainFrame);
                        // clear out library name so it does not try to do moves within one library
                        transferable.transferObject.mfSource = "";
                        mainFrame.library.clearLibrary();
                        mainFrame.timer.removeActionListener(mainFrame.editorUpdateActionListener);
                    }

                }
                if (mustReopenAll || mustReopen || mustReload) {
                    Vector mainFrames = new Vector();
                    Vector parents = new Vector();
                    Vector playlists = new Vector();
                    Set set = MainFrame.mainFrameMap.entrySet();
                    Iterator it = set.iterator();
                    while (it.hasNext()) {
                        Map.Entry mapEntry = (Map.Entry) it.next();
                        MainFrame frame = (MainFrame) mapEntry.getValue();  // it.next();
                        mainFrames.add(frame.name);
                        if (frame.parentFrame == null) {
                            parents.add(null);
                        } else {
                            parents.add(frame.parentFrame.name);
                        }
                        if (frame.playListFrame == null) {
                            playlists.add(null);
                        } else {
                            playlists.add(frame.playListFrame.name);
                        }
                    }
                    MainFrame.isOpening = true;
                    if (mustReopenAll) {
                        for (ix = 0; ix < mainFrames.size(); ix++) {
                            MainFrame frame = (MainFrame) MainFrame.mainFrameMap.get(mainFrames.get(ix));
                            if (frame == null) {
                                continue;
                            }
                            WindowEvent e1 = new WindowEvent(frame.frame, WindowEvent.WINDOW_CLOSING);
                            frame.windowClosing(e1);
                            frame.windowClosed(e1);
                            frame.frame.dispose();
                        }
                        for (ix = 0; ix < mainFrames.size(); ix++) {
                            new MainFrame((String) mainFrames.get(ix));
                        }
                    } else {
                        WindowEvent e1 = new WindowEvent(mainFrame.frame, WindowEvent.WINDOW_CLOSING);
                        mainFrame.windowClosing(e1);
                        mainFrame.windowClosed(e1);
                        mainFrame.frame.dispose();
                        MainFrame newMainFrame = new MainFrame(mainFrame.name);
                        if (mainFrame.playListFrame != null) {
                            newMainFrame.openPlayList();
                        }
                    }
                    for (ix = 0; ix < mainFrames.size(); ix++) {
                        MainFrame frame = (MainFrame) MainFrame.mainFrameMap.get(mainFrames.get(ix));
                        if (frame != null) {
                            MainFrame parentFrame = (MainFrame) MainFrame.mainFrameMap.get(parents.get(ix));
                            frame.parentFrame = parentFrame;
                            MainFrame playListFrame = (MainFrame) MainFrame.mainFrameMap.get(playlists.get(ix));
                            frame.playListFrame = playListFrame;
                        }
                    }
                    MainFrame.isOpening = false;
                }
                if (mustReload && transferable != null) {
                    MainFrame newMainFrame = (MainFrame) MainFrame.mainFrameMap.get(mainFrame.name);
                    TransferHandler handler = newMainFrame.mainTable.getTransferHandler();
                    handler.importData(newMainFrame.mainTable, transferable);
                }

                dispose();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    ex.toString(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_okButtonActionPerformed

    private void languageAddButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_languageAddButtonActionPerformed
        terminateEditing();
        voicePage.model.AddRow(voicesTable);
    }//GEN-LAST:event_languageAddButtonActionPerformed

    private void languageDeleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_languageDeleteButtonActionPerformed
        terminateEditing();
        voicePage.model.DeleteRow(voicesTable);
    }//GEN-LAST:event_languageDeleteButtonActionPerformed

    private void voiceTestButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_voiceTestButtonActionPerformed
        terminateEditing();
        voicePage.voiceTestButtonActionPerformed();
    }//GEN-LAST:event_voiceTestButtonActionPerformed

    private void customizePaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_customizePaneStateChanged
        terminateEditing();
        if (customizePane.getSelectedIndex() == VOICES_PANEL)
            voicePage.loadVoices();
    }//GEN-LAST:event_customizePaneStateChanged

    private void announceWaitTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_announceWaitTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_announceWaitTextFieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.JTextField announceWaitTextField;
    javax.swing.JButton cancelButton;
    javax.swing.JRadioButton caseDefaultRadioButton;
    javax.swing.JRadioButton caseNoRadioButton;
    javax.swing.ButtonGroup caseSensitiveButtonGroup;
    javax.swing.JLabel caseSensitiveFileNameLabel;
    javax.swing.JRadioButton caseYesRadioButton;
    javax.swing.JTextField cepstralPathTextField;
    javax.swing.JTabbedPane customizePane;
    javax.swing.JPanel diskplayColumnsPanel;
    javax.swing.JButton displayColumnsDownButton;
    javax.swing.JScrollPane displayColumnsScrollPane;
    javax.swing.JTable displayColumnsTable;
    javax.swing.JButton displayColumnsUpButton;
    javax.swing.JTextField eSpeakDataTextField;
    javax.swing.JTextField eSpeakProgTextField;
    javax.swing.JLabel fillerLabel;
    javax.swing.JLabel fillerLabel1;
    javax.swing.JLabel fillerLabel6;
    javax.swing.JLabel gainLabel;
    javax.swing.JTextField gainText;
    javax.swing.JPanel generalPanel;
    javax.swing.JLabel jLabel1;
    javax.swing.JLabel jLabel10;
    javax.swing.JLabel jLabel11;
    javax.swing.JLabel jLabel12;
    javax.swing.JLabel jLabel13;
    javax.swing.JLabel jLabel14;
    javax.swing.JLabel jLabel15;
    javax.swing.JLabel jLabel16;
    javax.swing.JLabel jLabel17;
    javax.swing.JLabel jLabel2;
    javax.swing.JLabel jLabel3;
    javax.swing.JLabel jLabel4;
    javax.swing.JLabel jLabel5;
    javax.swing.JLabel jLabel6;
    javax.swing.JLabel jLabel7;
    javax.swing.JLabel jLabel8;
    javax.swing.JPanel jPanel1;
    javax.swing.JComboBox landfComboBox;
    javax.swing.JLabel landfLabel;
    javax.swing.JButton languageAddButton;
    javax.swing.JButton languageDeleteButton;
    javax.swing.JButton libraryAddButton;
    javax.swing.JLabel libraryDataNameLabel;
    javax.swing.JTextField libraryDataNameText;
    javax.swing.JButton libraryDeleteButton;
    javax.swing.JButton libraryDownButton;
    javax.swing.JPanel libraryFieldsPanel;
    javax.swing.JPanel libraryPanel;
    javax.swing.JRadioButton libraryRadioButton;
    javax.swing.JTable libraryTable;
    javax.swing.JScrollPane libraryTableScrollPane;
    javax.swing.JLabel libraryTypeLabel;
    javax.swing.ButtonGroup libraryTypebuttonGroup;
    javax.swing.JButton libraryUpButton;
    javax.swing.JTextField mbrolaPathTextField;
    javax.swing.JTextField mbrolaProgTextField;
    javax.swing.JComboBox mixerNameComboBox;
    javax.swing.JButton okButton;
    javax.swing.JLabel playlistNameLabel1;
    javax.swing.JTextField playlistNameText;
    javax.swing.JRadioButton playlistRadioButton;
    javax.swing.JTextField rateTextField;
    javax.swing.JComboBox resizeModeComboBox;
    javax.swing.JLabel resizeModeLabel;
    javax.swing.JTextField speechA;
    javax.swing.JTextField speechB;
    javax.swing.JLabel speechLabel;
    javax.swing.JTextField speechTemplate;
    javax.swing.JRadioButton styleCompactRadioButton;
    javax.swing.JRadioButton styleNormalRadioButton;
    javax.swing.JButton tagEditorAddButton;
    javax.swing.JButton tagEditorDeleteButton;
    javax.swing.JButton tagEditorDownButton;
    javax.swing.JPanel tagEditorPanel;
    javax.swing.JScrollPane tagEditorScrollPane;
    javax.swing.JTable tagEditorTable;
    javax.swing.JButton tagEditorUpButton;
    javax.swing.JCheckBox unixPathCheckBox;
    javax.swing.JLabel unixPathLabel;
    javax.swing.JButton voiceTestButton;
    javax.swing.JTextField voiceTestTextField;
    javax.swing.JPanel voicesPanel;
    javax.swing.JScrollPane voicesScrollPane;
    javax.swing.JTable voicesTable;
    javax.swing.ButtonGroup windowStyleButtonGroup;
    javax.swing.JLabel windowStyleLabel;
    // End of variables declaration//GEN-END:variables
}
