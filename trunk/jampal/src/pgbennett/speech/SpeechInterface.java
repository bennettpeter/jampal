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

public interface SpeechInterface {

    public boolean init();
    
    /**
     * Converts the given input text to speech. Text can optionally
     * contain XML tags for controlling the speech. To use
     * XML tags there must be an XML tag at the front of the string
     * even if it is a dummy field name or an empty tag.
     * @param strInput String with words to convert to speech
     */    
    public boolean speak(String strInput);
    /**
     * Sets the rate (speed) of output speech
     * @param rate Rate, valid values are from -10 to 10
     */    
    public boolean setRate(int rate);
    /**
     * Set output volume for text, default is 100
     * @param volume Volume from 0 to 100
     */    
    public boolean setVolume(int volume);
    /**
     * Close file or sound output device
     */    
    public boolean close();

    public void setVoice(String voiceName);

    // non-static version of getVoices
    public String[] getVoiceList();
    
}
