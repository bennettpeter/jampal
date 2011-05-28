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


package pgbennett.speech;

/**
 * Implements a text to speech interface
 */
public class Speaker implements SpeechInterface
{
    static
    {
        System.loadLibrary("pttsjni");
    }
    /**
     * Used by the native code. Do not touch this variable.
     */    
    private int hSpeaker;
    /**
     * main method runs a test of all functions
     */    
    public static void main(String args[])
    throws Exception {

        Speaker t = new Speaker();
        System.out.println("Hello World");
        boolean blnResult = t.speak("Hello World");
        if (!blnResult)
            throw new Exception("false return");
        System.out.println("Hello World slower");
        blnResult = t.setRate(-10);
        if (!blnResult)
            throw new Exception("false return");
        blnResult = t.speak("Hello World slower");
        if (!blnResult)
            throw new Exception("false return");
        System.out.println("Hello World slower fainter");
        blnResult = t.setVolume(25);
        if (!blnResult)
            throw new Exception("false return");
        blnResult = t.speak("Hello World slower fainter");
        if (!blnResult)
            throw new Exception("false return");
        blnResult = t.close();
        if (!blnResult)
            throw new Exception("false return");
        t = new Speaker("test.wav");
        System.out.println("Hello World to test.wav");
        blnResult = t.speak("Hello World to test.wav");
        if (!blnResult)
            throw new Exception("false return");
        blnResult = t.close();
        if (!blnResult)
            throw new Exception("false return");

    }
    /**
     * Creates a Speaker object that outputs to a sound card,
     * on Windows systems only.
     */    
    public Speaker() 
    throws javax.sound.sampled.UnsupportedAudioFileException{
        this(null);
    }
    /**
     * Creates a Speaker object that outputs to a wave file.
     * @param fileName File name for output wave file.
     */    
    public Speaker(String fileName) 
    throws javax.sound.sampled.UnsupportedAudioFileException{
        if (!init(fileName))
            throw new javax.sound.sampled.UnsupportedAudioFileException
                ("Unable to initialize ptts");
            
    }
    private native boolean init(String fileName);
    
    public boolean init() {
        return init(null);
    }
    
    /**
     * Converts the given input text to speech. Text can optionally
     * contain XML tags for controlling the speech. To use
     * XML tags there must be an XML tag at the front of the string
     * even if it is a dummy field name or an empty tag.
     * @param strInput String with words to convert to speech
     */    
    public native boolean speak(String strInput);
    /**
     * Sets the rate (speed) of output speech
     * @param rate Rate, valid values are from -10 to 10
     */    
    public native boolean setRate(int rate);
    /**
     * Set output volume for text, default is 100
     * @param volume Volume from 0 to 100
     */    
    public native boolean setVolume(int volume);
    /**
     * Close file or sound output device
     */    
    public native boolean close();
    
    protected void finalize() {
        close();
    }

    public native void setVoice(String voiceName);

    public native static String [] getVoices();
    
    public String [] getVoiceList() {
        return getVoices();
    }

}