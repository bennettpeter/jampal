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

import java.io.*;
import java.util.*;

public class MicrosoftSpeaker implements SpeechInterface {
    
    int rateWPM = 165;
    int volume = 100;
    String voice;
    Process process;
    
    String path;

    
    
    /** Creates a new instance of CepstralSpeaker */
    public MicrosoftSpeaker()  {
    }
    
    public void setWOW64(boolean useWOW64) {
        String windowsDir = System.getenv("windir");
        if (useWOW64)
            this.path = windowsDir + "\\SysWOW64";
        else
            this.path = null;
    }

    public boolean close() {
        outWriter.flush();
        outWriter.close();
        return true;
    }
    
    PrintWriter outWriter;

    public boolean init() {
        String command;
        if (path==null)
            command="cscript";
        else
            command=path+File.separator+"cscript";
        Vector cmdVec = new Vector();
        cmdVec.add(command);
        cmdVec.add("ptts.vbs");
        if (voice != null && voice.length()>0) {
            cmdVec.add("-n");
            cmdVec.add(voice);
        }
        cmdVec.add("-p");
        cmdVec.add("audio/volume="+volume+",speech/rate="+rateWPM);
        cmdVec.add("-f");
        cmdVec.add("-");
        String [] cmdArray = new String[cmdVec.size()];
        cmdArray = (String[])cmdVec.toArray(cmdArray);
        if (process!=null)
            close();
        try {
            process = Runtime.getRuntime().exec(cmdArray);
        }
        catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
        
        errThread = new ReaderThread(process.getErrorStream(), System.err);
        errThread.start();
        outThread = new ReaderThread(process.getInputStream(), System.out);
        outThread.start();
        outWriter = new PrintWriter(process.getOutputStream(),true);
        return true;
    }
    
    ReaderThread errThread;
    ReaderThread outThread;
    
    class ReaderThread extends Thread {
        BufferedReader reader;
        PrintStream printStream;
        boolean eof = false;
        Vector lines = new Vector();
        String line;
        
        ReaderThread(InputStream inputStream, PrintStream printStream) {
            super("cepstral-reader");
            this.reader=new BufferedReader(new InputStreamReader(inputStream));
            this.printStream=printStream;
        }
        public void run() {
            while(!eof) {
                try {
                    line = reader.readLine();
                }
                catch(Exception ex) {
                    ex.printStackTrace();
                    line=null;
                }
                if (line==null) {
                    eof=true;
                    break;
                }
                lines.add(line);
                printStream.println(line);
            }
        }
        
    }

    public boolean setRate(int rate) {
        rateWPM = (rate+11)*15;
        return true;
    }

    public void setVoice(String voiceName) {
        voice = voiceName;
    }

    public boolean setVolume(int volume) {
        this.volume = volume;
        return true;
    }

    public boolean speak(String strInput) {
        if (process==null || outWriter == null)  //  || !outThread.isAlive())
            init();
        if (process==null || outWriter == null)  //  || !outThread.isAlive())
            return false;
        outWriter.println(strInput);
        outWriter.println("\n");
        close();
        int ret=999;
        try {
            ret=process.waitFor();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (ret!=0) {
            Exception ex = new Exception("process failed, ret="+ret);
            ex.printStackTrace();
        }
        process=null;
        // start up another process so it will be ready next time
        // to reduce delay
        init();
        return true;
    }

    public String [] getVoiceList() {
        if (path != null && path.length() > 0) {
            File voicesDir = new File(path, "voices");
            File [] dirArray = voicesDir.listFiles();
            if (dirArray==null)
                return null;
            Vector voicesVec = new Vector();
            int ix;
            for (ix=0;ix<dirArray.length;ix++) {
                if (dirArray[ix].isDirectory()) {
                    voicesVec.add(dirArray[ix].getName());
                }
            }
            String [] voicesArray = new String[voicesVec.size()];
            voicesArray = (String[]) voicesVec.toArray(voicesArray);
            return voicesArray;
        }
        else return null;
    }
    
    
}
