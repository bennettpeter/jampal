/*
    Copyright 2011 Peter Bennett

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
    
    int rate = 0;
    int volume = 100;
    String voice;
    Process process;
    
    String path;
    static File speakText;
    static File pttsVbs;

    
    
    /** Creates a new instance of MicrosoftSpeaker */
    public MicrosoftSpeaker() throws IOException {
        synchronized(MicrosoftSpeaker.class) {
            if (speakText == null) {
                    speakText = File.createTempFile("jampal", ".txt");
            }
            InputStream inStream = null;
            OutputStream outStream = null;
            if (pttsVbs == null) {
                try {
                    pttsVbs = File.createTempFile("jampal", ".vbs");
                    inStream = ClassLoader.getSystemResourceAsStream(("pgbennett/speech/ptts.vbs"));
                    outStream = new FileOutputStream(pttsVbs);
                    byte buffer[] = new byte[1024];
                    int len = 0;
                    while (len != -1) {
                        len = inStream.read(buffer);
                        if (len != -1)
                            outStream.write(buffer, 0, len);
                    }
                }
                finally {
                    if (inStream != null)
                        inStream.close();
                    if (outStream != null)
                        outStream.close();
                }
            }
        }
    }
    
    public void setWOW64(boolean useWOW64) {
        String windowsDir = System.getenv("windir");
        if (useWOW64)
            this.path = windowsDir + "\\SysWOW64";
        else
            this.path = null;
    }

    public boolean close() {
        return true;
    }
    
    PrintWriter outWriter;

    public boolean init() {
        return true;
    }
    
    enum invokeOption {SPEAK, VOICELIST }
    
    boolean invoke(invokeOption option) {
        String command;
        if (path==null)
            command="cscript";
        else
            command=path+File.separator+"cscript";
        ArrayList cmdVec = new ArrayList<String>();
        cmdVec.add(command);
        cmdVec.add(pttsVbs.getPath());
        switch (option) {
            case VOICELIST:
                cmdVec.add("-vl");
                break;
            case SPEAK:
            if (voice != null && voice.length()>0) {
                cmdVec.add("-n");
                cmdVec.add(voice);
            }
            cmdVec.add("-v");
            cmdVec.add(volume);
            cmdVec.add("-r");
            cmdVec.add(rate);
            cmdVec.add("-e");
            cmdVec.add("UTF-16LE");
            cmdVec.add("-u");
            cmdVec.add(speakText.getPath());
        }
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
        outWriter.flush();
        outWriter.close();

        int ret=999;
        boolean IsOK = true;
        try {
            ret=process.waitFor();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            IsOK=false;
        }
        if (ret!=0) {
            Exception ex = new Exception("process failed, ret="+ret);
            ex.printStackTrace();
            IsOK=false;
        }
        process=null;
        
        return IsOK;
    }
    
    ReaderThread errThread;
    ReaderThread outThread;
    
    class ReaderThread extends Thread {
        BufferedReader reader;
        PrintStream printStream;
        boolean eof = false;
        ArrayList<String> lines = new ArrayList<String> ();
        String line;
        
        ReaderThread(InputStream inputStream, PrintStream printStream) {
            super("microsoft-reader");
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
        this.rate = rate;
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
        PrintWriter textWriter = null;
        boolean IsOK=true;
        synchronized(speakText) {
            try {
                textWriter = new PrintWriter(speakText,"UTF-16LE");
                textWriter.println(strInput);
                textWriter.println("\n");
                textWriter.close();
                textWriter = null;
                IsOK=invoke(invokeOption.SPEAK);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                IsOK=false;
            }
            finally {
                if (textWriter != null)
                    textWriter.close();
            }
        }
        return IsOK;
    }

    public String [] getVoiceList() {
        boolean IsOK=true;
        IsOK=invoke(invokeOption.VOICELIST);
        if (IsOK) {
            String [] voicesArray = new String[outThread.lines.size()];
            voicesArray = (String[]) outThread.lines.toArray(voicesArray);
            return voicesArray;
        }
        else return null;
    }
    
    
}
