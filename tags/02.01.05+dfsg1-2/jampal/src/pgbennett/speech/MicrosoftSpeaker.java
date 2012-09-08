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
    boolean useWOW64;
    static boolean wow64Exists;
    static boolean nativeExists;
    static String windowsDir;
    static File speakText;
    static File pttsVbs;

    
    
    /** Creates a new instance of MicrosoftSpeaker */
    public MicrosoftSpeaker() throws IOException {
        initSystemStatics();
        setWOW64(false);
    }
    
    private synchronized static void initSystemStatics() throws IOException {
        if (windowsDir == null) {
            windowsDir = System.getenv("windir");
            File wow64File = new File(windowsDir + File.separator + "SysWOW64");
            wow64Exists = wow64File.exists();
            File nativeFile = new File(windowsDir + File.separator + "Sysnative");
            nativeExists = nativeFile.exists();
            InputStream inStream = null;
            OutputStream outStream = null;
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
                speakText = File.createTempFile("jampal", ".txt");
            }
            catch(IOException io) {
                windowsDir = null;
                throw io;
            }
            finally {
                if (inStream != null)
                    inStream.close();
                if (outStream != null)
                    outStream.close();
            }
        }
    } 
    
    private boolean setWOW64(boolean useWOW64) {
        if (useWOW64) {
            if (wow64Exists) 
                this.path = windowsDir + File.separator + "SysWOW64";
            else {
                this.path = null;
                this.useWOW64 = false;
                return false;
            }
        }
        else {
            if (nativeExists) 
                this.path = windowsDir + File.separator + "Sysnative";
            else
                this.path = null;
        }
        this.useWOW64 = useWOW64;
        return true;
    }

    public boolean close() {
        return true;
    }
    
    PrintWriter outWriter;

    public boolean init() {
        return true;
    }
    
    enum invokeOption {SPEAK, VOICELIST }
    
    private boolean invoke(invokeOption option) {
        String command;
        if (path==null)
            command="cscript";
        else
            command=path+File.separator+"cscript";
        ArrayList cmdVec = new ArrayList<String>();
        cmdVec.add(command);
        cmdVec.add(pttsVbs.getPath());
        // cmdVec.add("-debug");
        switch (option) {
            case VOICELIST:
                cmdVec.add("-vl");
                break;
            case SPEAK:
            if (voice != null && voice.length()>0) {
                cmdVec.add("-voice");
                cmdVec.add(voice);
            }
            cmdVec.add("-v");
            cmdVec.add(String.valueOf(volume));
            cmdVec.add("-r");
            cmdVec.add(String.valueOf(rate));
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
        if (voiceName.startsWith("**")) {
            setWOW64(true);
            voiceName = voiceName.substring(2);
        }
        else
            setWOW64(false);
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

    // Voices supported by wow64 get ** in front of the names
    public String [] getVoiceList() {
        boolean saveWow64 = useWOW64;
        boolean IsOK=true;
        boolean wowOptions [] = {false,true};
        ArrayList<String> voicesList = new ArrayList<String>();
        for (boolean wowOption : wowOptions) {
            if (setWOW64(wowOption)) {
                IsOK=invoke(invokeOption.VOICELIST);
                if (IsOK) {
                    boolean isVoices = false;
                    for (String voiceInstance : outThread.lines) {
                        if (isVoices)
                            voicesList.add(useWOW64 ? "**" + voiceInstance : voiceInstance);
                        else
                            isVoices = "--Voice List--".equals(voiceInstance);
                    }
                }
            }
        }
        useWOW64 = saveWow64;
        if (voicesList.isEmpty())
            return null;
        String [] voicesArray = new String[voicesList.size()];
        voicesArray = (String[]) voicesList.toArray(voicesArray);
        return voicesArray;
    }
}
