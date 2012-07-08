/*
    Copyright 2006 Peter Bennett

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


package pgbennett.speech;

import com.sun.speech.freetts.*;
import com.sun.speech.freetts.audio.*;
import javax.sound.sampled.*;
import java.util.*;
import java.io.*;

/**
 *
 * @author Peter Bennett
 */
public class FreeTTSSpeaker implements SpeechInterface {
    
    String voiceName;
    String fileName;
    Voice voice;
    
    /** Creates a new instance of FreeTTSSpeaker */
    public FreeTTSSpeaker()  {
    }

    public static String [] getVoices() {
        Vector result = new Vector();
        VoiceManager voiceManager = VoiceManager.getInstance();
        Voice[] voices = voiceManager.getVoices(); 
        for (int i = 0; i < voices.length; i++) {
            String domain = voices[i].getDomain();
            if ("general".equals(domain))
                result.add(voices[i].getName());
        }
        String [] resultStr = new String[result.size()];
        return (String []) result.toArray(resultStr);
    }    
    
    
    /**
     * Creates a Speaker object that outputs to a wave file.
     * @param fileName File name for output wave file.
     */    
    public void setFileName(String fileName) {
    // throws javax.sound.sampled.UnsupportedAudioFileException
    //    init(fileName);
        this.fileName = fileName;
            
    }
    public void setVoice(String voiceName) {
    // throws javax.sound.sampled.UnsupportedAudioFileException
    //    init(fileName);
        this.voiceName = voiceName;
            
    }

    public boolean init() {
        VoiceManager voiceManager = VoiceManager.getInstance();
        if (voiceName==null)
            voiceName="kevin16";
        voice = voiceManager.getVoice(voiceName);
        if (voice==null)
            return false;
        if (fileName!=null) {
            AudioFileFormat.Type type = getAudioType(fileName);
            AudioPlayer audioPlayer = new
                SingleFileAudioPlayer(getBasename(fileName), type);
            voice.setAudioPlayer(audioPlayer);
        }
        voice.allocate();
        return true;
    }

    /**
     * Given a filename returns the basename for the file
     *
     * @param path the path to extract the basename from
     * 
     * @return the basename of the file
     */
    private static String getBasename(String path) {
        int index = path.lastIndexOf(".");
        if (index == -1) {
            return path;
        } else {
            return path.substring(0, index);
        }
    }
    
    /**
     * Returns the audio type based upon the extension of the given
     * file
     *
     * @param file the file of interest
     * 
     * @return the audio type of the file or null if it is a
     *     non-supported type
     */
    private AudioFileFormat.Type getAudioType(String file) {
	AudioFileFormat.Type[] types =
	    AudioSystem.getAudioFileTypes();
       String extension = getExtension(file);

	for (int i = 0; i < types.length; i++) {
	    if (types[i].getExtension().equals(extension)) {
		return types[i];
	    }
	}
	return null;
    }


    /**
     * Given a filename returns the extension for the file
     *
     * @param path the path to extract the extension from
     * 
     * @return the extension or <code>null</code> if none
     */
    private static String getExtension(String path) {
        int index = path.lastIndexOf(".");
        if (index == -1) {
            return null;
        } else {
            return path.substring(index + 1);
        }
    }
    
    
    public boolean close() {
        voice.deallocate();
        voice=null;
        return true;
    }

    public boolean setRate(int rate) {
        if (voice != null) {
            voice.setRate((rate+11)*15);
            return true;
        }
        else
            return false;
    }

    public boolean setVolume(int volume) {
        if (voice != null) {
            voice.setVolume((float)volume/100);
            return true;
        }
        else
            return false;
    }

    public boolean speak(String strInput) {
        if (voice==null) {
            if (!init())
                return false;
        }
        voice.speak(strInput);
        return true;
    }

    public String [] getVoiceList() {
        return getVoices();
    }
    
    
}
