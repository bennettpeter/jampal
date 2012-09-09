/*
 * BatchSpeaker.java
 *
 *  Created on February 7, 2006, 9:39 PM
 *
    Copyright Peter Bennett 2006
 
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
import java.io.*;
import com.sun.speech.freetts.*;
import com.sun.speech.freetts.audio.*;
import javax.sound.sampled.*;
/**
 *
 * @author peter
 */
public class BatchSpeaker {
    
    /** Creates a new instance of BatchSpeaker */
    public BatchSpeaker() {
    }
    
    public static void main(String[] args) throws Exception {
        String fileName=null;
        String voiceName="kevin16";
        Voice currentVoice = null;
        AudioPlayer audioPlayer = null;
        boolean playerUsed=false;
        float speechRate=-1;
        float speechVolume=-1;
        System.out.println("BatchSpeaker Version 1.09a (c) 2006 Peter G. Bennett");
        if (args.length!=0) {
            System.out.println("The program operates on commands to standard input.");
            System.out.println("Input Commands:");
            System.out.println("#listvoices      Print a list of voices");
            System.out.println("#file filename   Direct audio to a wav file");
            System.out.println("#closefile       Close the wave file and revert to sound card");
            System.out.println("#voice voicename Select output voice");
            System.out.println("#rate value      Select output rate, words per minute, decimals allowed");
            System.out.println("#volume value    Select output volume, 0 to 1, decimals allowed");
            System.out.println("#echo value      Write value to standard output");
            System.out.println("#exit            exit");
            System.out.println("Anything else represents text to be spoken");
            Voice[] voices = VoiceManager.getInstance().getVoices(); 
            System.out.println("Available Voices:");
            for (int i = 0; i < voices.length; i++) {
                String domain = voices[i].getDomain();
                if ("general".equals(domain))
                    System.out.println(voices[i].getName());
            }
        }
        
        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
        for (;;) {
            String inputString = inputReader.readLine();
            if (inputString==null)
                break;
            if (inputString.trim().startsWith("#closefile")) {
                if (audioPlayer != null) {
                    if (playerUsed) {
                        audioPlayer.drain();
                        audioPlayer.close();
                    }
                    audioPlayer=null;
            		audioPlayer = new JavaClipAudioPlayer();
                    playerUsed=false;
                    if (currentVoice!=null)
                        currentVoice.setAudioPlayer(audioPlayer);
                }
            }
            else if (inputString.trim().startsWith("#file ")) {
                fileName=inputString.substring(6).trim();
                if (fileName.toLowerCase().endsWith(".wav"))
                    fileName=fileName.substring(0, fileName.length()-4);
                if (audioPlayer != null) {
                    if (playerUsed) {
                        audioPlayer.drain();
                        audioPlayer.close();
                    }
                    audioPlayer=null;
                }
                audioPlayer = new SingleFileAudioPlayer(fileName,
                         AudioFileFormat.Type.WAVE);
                playerUsed=false;

                if (currentVoice!=null) 
                    currentVoice.setAudioPlayer(audioPlayer);
            }
            else if (inputString.trim().startsWith("#voice ")) {
                voiceName=inputString.substring(7).trim();
            }
            else if (inputString.trim().equals("#exit")) {
                break;
            }
            else if (inputString.trim().startsWith("#rate ")) {
                String rateStr=inputString.substring(6).trim();
                speechRate=Float.parseFloat(rateStr);
                if (currentVoice!=null && speechRate>=0)
                    currentVoice.setRate(speechRate);
            }
            else if (inputString.trim().startsWith("#volume ")) {
                String volStr=inputString.substring(8).trim();
                speechVolume=Float.parseFloat(volStr);
                if (currentVoice!=null && speechVolume>=0)
                    currentVoice.setVolume(speechVolume);
            }
            else if (inputString.trim().startsWith("#echo ")) {
                String echoStr=inputString.substring(6).trim();
                System.out.println(echoStr);
            }
            else if (inputString.trim().equals("#listvoices")) {
                String [] voices = FreeTTSSpeaker.getVoices();
                int ix;
                for (ix=0;ix<voices.length;ix++) {
                    System.out.println(voices[ix]);
                }
            }
            else {
                // Now say something
                if (currentVoice!=null && !currentVoice.getName().equals(voiceName)) {
                    currentVoice.deallocate();
                    currentVoice=null;
                }
                if (currentVoice==null) {
                    currentVoice=VoiceManager.getInstance().getVoice(voiceName);
                    if (currentVoice==null) {
                        System.err.println("ERROR - Invalid voice");
                        continue;
                    }
                    currentVoice.allocate();
                    if (audioPlayer!=null) {
                        currentVoice.setAudioPlayer(audioPlayer);
                    }
                if (speechRate>=0)
                    currentVoice.setRate(speechRate);
                if (speechVolume>=0)
                    currentVoice.setVolume(speechVolume);
                    
                }
                currentVoice.speak(inputString);
                playerUsed=true;
            }
            
        }
        if (audioPlayer != null) {
            if (playerUsed) {
                audioPlayer.drain();
                audioPlayer.close();
            }
            audioPlayer=null;
        }
        if (currentVoice!=null) {
            currentVoice.deallocate();
            currentVoice=null;
        }
        
    }        
        
        

    
    
}
