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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pgbennett.jampal.customize;

import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import pgbennett.jampal.Jampal;
import pgbennett.speech.ESpeakSpeaker;
import pgbennett.speech.*;

/**
 *
 * @author peter
 */
class VoicePage {
    
    String [] engines = {"eSpeak", "Microsoft", "Cepstral", "None"};
    // FreeTTS removed - it hangs the system and does not work
//    String [] engines = {"eSpeak", "Microsoft", "Cepstral", "FreeTTS", "None"};

    JComboBox langComboBox;
    JComboBox engineComboBox;
    JComboBox [] voiceComboBox = new JComboBox[engines.length];
    // Vector <String[]> voiceMatrix = new Vector<String[]>();
    
    CustomizeDialog dialog;
    VoiceTableModel model;
    boolean isDirty;
    
    

    
    VoicePage(CustomizeDialog dialog) {
        this.dialog = dialog;
        model = new VoiceTableModel(dialog);
    }
    
  
    void initializeValues() {
        Font font=dialog.mainFrame.getSelectedFont();
        dialog.voiceTestTextField.setFont(font);

        setColumnWidths(dialog.voicesTable, model.colWidths);

        // .. Language
        TableColumn langColumn = dialog.voicesTable.getColumnModel().getColumn(model.COL_LANGUAGE);
        langComboBox = new JComboBox(dialog.attributes.langNames);
        langComboBox.insertItemAt("", 0);
        langComboBox.insertItemAt("other", 1);
        langColumn.setCellEditor(new DefaultCellEditor(langComboBox));
        
        // .. Engine
        TableColumn engineColumn = dialog.voicesTable.getColumnModel().getColumn(model.COL_ENGINE);
        engineComboBox = new JComboBox(engines);
        engineColumn.setCellEditor(new DefaultCellEditor(engineComboBox));
        
        // Voices
        
        loadVoices();
        
        // .. Voice Combo Box
        TableColumn voiceColumn = dialog.voicesTable.getColumnModel().getColumn(model.COL_VOICE);
        voiceColumn.setCellEditor(new VoiceCellEditor(model));
        
        ListSelectionModel selModel = dialog.voicesTable.getSelectionModel();
        selModel.setSelectionMode(selModel.SINGLE_SELECTION);
        // Default string columns
        DefaultCellEditor tableCellEditor =
                (DefaultCellEditor) dialog.voicesTable.getDefaultEditor(String.class);
        tableCellEditor.setClickCountToStart(1);

        // .. Put values in
        
        Set <Map.Entry<String, String[]>> mapEntries = Jampal.voiceSettings.entrySet();
        for (Map.Entry<String, String[]> mapEntry : mapEntries) {
//        Map.Entry<String, String[]> mapEntry = Jampal.voiceSettings.firstEntry();
//         while (mapEntry != null) {
            VoiceEntry entry = new VoiceEntry();
            model.entries.add(entry);
            entry.language = mapEntry.getKey();
            entry.engine = mapEntry.getValue()[0];
            entry.voice = mapEntry.getValue()[1];
            entry.volume = mapEntry.getValue()[2];
//            mapEntry = Jampal.voiceSettings.higherEntry(entry.language);
         }
         isDirty=false;
    }    

    void loadVoices() {
        
        String voices [];
        // eSpeak
        ESpeakSpeaker espeakSpeaker = new ESpeakSpeaker();
        espeakSpeaker.setPaths(
                dialog.eSpeakProgTextField.getText(), 
                dialog.eSpeakDataTextField.getText(),
                dialog.mbrolaProgTextField.getText(),
                dialog.mbrolaPathTextField.getText(), 
                (String)dialog.mixerNameComboBox.getSelectedItem(), 
                dialog.gainText.getText());
        voices = espeakSpeaker.getVoiceList();
        if (voices == null)
            voices = new String[0];
        Arrays.sort(voices);
        voiceComboBox[0] = new JComboBox(voices);
        voiceComboBox[0].insertItemAt("", 0);
        
        // Microsoft
        try {
            MicrosoftSpeaker microsoftSpeaker = new MicrosoftSpeaker();
            voices = microsoftSpeaker.getVoiceList();
        }
        catch (Throwable th) {
            // th.printStackTrace();
            System.err.println("VoicesPage.loadVoices " + th.toString());
            voices=new String[0];
        }
        if (voices == null)
            voices = new String[0];
        voiceComboBox[1] = new JComboBox(voices);
        voiceComboBox[1].insertItemAt("", 0);
        //voiceMatrix.add(voices);
        
        // Cepstral
        CepstralSpeaker cepstralSpeaker = new CepstralSpeaker();
        cepstralSpeaker.setPath(dialog.cepstralPathTextField.getText());
        voices = cepstralSpeaker.getVoiceList();
        if (voices == null)
            voices = new String[0];
        voiceComboBox[2] = new JComboBox(voices);
        voiceComboBox[2].insertItemAt("", 0);
        //voiceMatrix.add(voices);

        // FreeTTS
//        try {
////            voices = FreeTTSSpeaker.getVoices();
//            Class freeTTSSpeakerClass = Class.forName("pgbennett.speech.FreeTTSSpeaker");
//            SpeechInterface speaker = (SpeechInterface)freeTTSSpeakerClass.newInstance();
//            voices = speaker.getVoiceList();
//        }
//        catch (Throwable th) {
//            // th.printStackTrace();
//            System.err.println("VoicesPage.loadVoices " + th.toString());
//            voices = new String[0];
//        }
//        voiceComboBox[3] = new JComboBox(voices);
//        voiceComboBox[3].insertItemAt("", 0);
//        //voiceMatrix.add(voices);
        
        // None
        voices = new String[0];
        voiceComboBox[3] = new JComboBox(voices);
        voiceComboBox[3].insertItemAt("", 0);
        //voiceMatrix.add(voices);
    
    }
    

    void setColumnWidths(JTable table, int [] colWidths) {
        TableColumnModel colModel = table.getColumnModel();
        int count = colModel.getColumnCount();
        int i;
        for (i = 0; i < count; i++) {
            TableColumn column = colModel.getColumn(i);
            column.setPreferredWidth(colWidths[i]);
        }
    }
    
    class VoiceCellEditor extends AbstractCellEditor implements TableCellEditor {

        JComboBox currentComboBox;
        VoiceTableModel model;

        VoiceCellEditor(VoiceTableModel model) {
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
            currentComboBox = voiceComboBox [voiceComboBox.length - 1];
            String engine =  (model.entries.get(row)).engine;
            for (int ix = 0; ix < engines.length; ix++) {
                if (engines[ix].equals(engine)) {
                    currentComboBox = voiceComboBox [ix];
                    break;
                }
            }
            currentComboBox.setSelectedItem(model.getValueAt(row, column));
            return currentComboBox;
        }
    }
    
    
    boolean processValues(boolean doUpdate) {
        
        if (!isDirty)
            return true;

        HashMap <String, String[]> newVoiceSettings = new HashMap <String, String[]>();
        
        // VOICES TAB
        try {
//            Jampal.voiceSettings.clear();
            PrintStream voicesStream = null;
            if (doUpdate) {
                String jampalDirectory = System.getProperty("user.home")+File.separator+".jampal";
                File voiceFile = new File(jampalDirectory,"voices.txt");
                voicesStream = new PrintStream(voiceFile,"UTF-8");
            }
            String errormessage=null;
            for (VoiceEntry entry : model.entries) {
                boolean volOK = false;
                try {
                    int n = Integer.parseInt(entry.volume);
                    if (n >= 0 && n <= 2500) 
                        volOK = true;
                } catch (Exception ex) {
                    
                }
                if (!volOK)
                    errormessage = "Invalid volume - must be numeric between 0 and 2500 for language ";
                if (errormessage == null) {
                    String [] prev = newVoiceSettings.put(entry.language, new String[] {entry.engine,entry.voice,entry.volume});
                    if (prev != null) {
                        errormessage = "Duplicate entry for language ";
                    }
                }
                if (errormessage != null) {
                    dialog.customizePane.setSelectedIndex(dialog.VOICES_PANEL);
                    String value = entry.language;
                    if (value != null && value.length() > 0 && !"other".equals(value)) 
                        value = dialog.attributes.langCodeProps.getProperty(value);
                    JOptionPane.showMessageDialog(dialog,
                            errormessage + "<" + value + ">",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                // sample lines
                //|eSpeak||
                //other|eSpeak||
                //eng|eSpeak|english-us|
                if (doUpdate) {
                    voicesStream.println(entry.language+"|"+entry.engine+"|"+entry.voice+"|"+entry.volume+"|");
                    Jampal.voiceSettings = newVoiceSettings;
                }
            }
            if (doUpdate) 
                voicesStream.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            dialog.customizePane.setSelectedIndex(dialog.VOICES_PANEL);
            JOptionPane.showMessageDialog(dialog,
                    "Problem with Voices:" + ex.toString(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }

    void voiceTestButtonActionPerformed() {
        try {
            int row = dialog.voicesTable.getSelectedRow();
            String announcement = dialog.voiceTestTextField.getText();
            if (row >= 0) {
                VoiceEntry entry = model.entries.get(row);
                SpeechInterface speaker = null;
                if ("FreeTTS".equals(entry.engine))  {
//                    speaker = new FreeTTSSpeaker();
                    Class freeTTSSpeakerClass = Class.forName("pgbennett.speech.FreeTTSSpeaker");
                    speaker = (SpeechInterface)freeTTSSpeakerClass.newInstance();
                }
                else if ("Microsoft".equals(entry.engine)) 
                    speaker = new MicrosoftSpeaker();
                else if ("Cepstral".equals(entry.engine)) {
                    CepstralSpeaker cepstralSpeaker = new CepstralSpeaker();
                    speaker = cepstralSpeaker;
                    cepstralSpeaker.setPath(dialog.cepstralPathTextField.getText());
                }
                else if ("eSpeak".equals(entry.engine)) {
                    ESpeakSpeaker espeakSpeaker = new ESpeakSpeaker();
                    speaker = espeakSpeaker;
                    espeakSpeaker.setPaths(
                            dialog.eSpeakProgTextField.getText(), 
                            dialog.eSpeakDataTextField.getText(),
                            dialog.mbrolaProgTextField.getText(),
                            dialog.mbrolaPathTextField.getText(), 
                            (String)dialog.mixerNameComboBox.getSelectedItem(), 
                            dialog.gainText.getText());
                }
                int volume = Integer.parseInt(entry.volume);
                int rate = Integer.parseInt(Jampal.initialProperties.getProperty("speech-rate"));

                if (speaker!= null) {
                    // Some engines required these to be done befores init
                    if (!"default voice".equals(entry.voice) && !"".equals(entry.voice))
                        speaker.setVoice(entry.voice);
                    speaker.setRate(rate);
                    speaker.setVolume(volume);
                    speaker.init();
                    // Some engines required these to be done after init
                    if (!"default voice".equals(entry.voice) && !"".equals(entry.voice))
                        speaker.setVoice(entry.voice);
                    speaker.setRate(rate);
                    speaker.setVolume(volume);
                    speaker.speak(announcement);
                    speaker.close();
                }

            }
        } catch (Throwable th) {
            th.printStackTrace();
            dialog.customizePane.setSelectedIndex(dialog.VOICES_PANEL);
            JOptionPane.showMessageDialog(dialog,
                    "Unable to test selected voice. " + th.toString(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        
    }
    

}
