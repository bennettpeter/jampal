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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import pgbennett.jampal.AudioPlayer;

public class ESpeakSpeaker implements SpeechInterface {

    int rateWPM = 165;
    int volume = 100;
    String voice;
    Process eSpeakProcess;
    Process mbrolaProcess;
    ReaderThread errThread;
    ReaderThread errThread2;
    ReaderThread outThread;
    ReaderThread pipeThread;
    PlayThread playThread;
    String espeakProg = "espeak";
    String mbrolaProg = "mbrola";
    String mbrolaBase = "/opt/mbrola";
    String espeakDataPath = "/usr/share/espeak-data";
    String mixerName;
    String mixerGain;
    // voice to be added through xml
    String xmlVoice = null;

    /** Creates a new instance of CepstralSpeaker */
    public ESpeakSpeaker() {
    }

    public boolean close() {
        outWriter.flush();
        outWriter.close();
        return true;
    }
    PrintWriter outWriter;

    public void setPaths(String espeakProg, String espeakDataPath,
            String mbrolaProg, String mbrolaBase, String mixerName,
            String mixerGain) {
        this.espeakProg = espeakProg;
        this.mbrolaProg = mbrolaProg;
        this.mbrolaBase = mbrolaBase;
        this.espeakDataPath = espeakDataPath;
        this.mixerName = mixerName;
        this.mixerGain = mixerGain;
    }

    public boolean init() {
        String command = espeakProg;
        Vector cmdVec = new Vector();
        cmdVec.add(command);
        if (voice != null && voice.length() > 0) {
            if (voice.startsWith("mb-")) {
                String esVoice = voice;
                String[] split = voice.split(" ");
                esVoice = split[0];
                cmdVec.add("-v");
                cmdVec.add(esVoice);
            } else {
                xmlVoice = voice;
            }
        } else {
            xmlVoice = null;
        }
        cmdVec.add("-m");
        cmdVec.add("-s" + Integer.toString(rateWPM));
        cmdVec.add("-a" + volume);
        // leave out the stdin - including it delays speech until end of file
        // cmdVec.add("--stdin");
        String[] cmdArray = new String[cmdVec.size()];
        cmdArray = (String[]) cmdVec.toArray(cmdArray);
        if (eSpeakProcess != null) {
            close();
        }
        try {
            eSpeakProcess = Runtime.getRuntime().exec(cmdArray);
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        outWriter = new PrintWriter(eSpeakProcess.getOutputStream(), true);
        errThread = new ReaderThread(eSpeakProcess.getErrorStream(), System.err, "espeak-err", false);
        errThread.start();

        if (voice != null && voice.startsWith("mb-")) {
            try {

//            mbrolalang="${voice#mb-}"
//            "${espeak[@]}" | "$MBROLA_PROGRAM" -e "$MBROLA_HOME/$mbrolalang/$mbrolalang" - "$wavefile"
                String mbrolaLang = voice.substring(3, 6);
                String voiceFileName = mbrolaBase + "/" + mbrolaLang + "/" + mbrolaLang;
                File mbVoiceFile = new File(voiceFileName);
                if (!mbVoiceFile.exists()) {
                    voiceFileName = mbrolaBase + "/" +  mbrolaLang;
                }

                ProcessBuilder pb = new ProcessBuilder(
                        mbrolaProg,
                        "-e",
                        "-t", "0.8",
                        voiceFileName,
                        "-",
                        "-.au");
                mbrolaProcess = pb.start();
            } catch (IOException ex) {
                Logger.getLogger(ESpeakSpeaker.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            // Pipe the processes
            pipeThread = new ReaderThread(eSpeakProcess.getInputStream(), mbrolaProcess.getOutputStream(), "espeak-mbrola", true);
            pipeThread.start();
            errThread2 = new ReaderThread(mbrolaProcess.getErrorStream(), System.err, "mbrola-err", false);
            errThread2.start();
            playThread = new PlayThread(mbrolaProcess.getInputStream());
            playThread.start();
        } else {
            outThread = new ReaderThread(eSpeakProcess.getInputStream(), System.out, "espeak-out", false);
            outThread.start();
        }
        return true;
    }

    class ReaderThread extends Thread {
//        BufferedReader reader;
//        PrintStream printStream;

        InputStream inStream;
        OutputStream outStream;
        boolean eof = false;
//        Vector lines = new Vector();
//        String line;
        byte[] buffer;
        int count;
        boolean close;

        ReaderThread(InputStream in, OutputStream out, String name, boolean close) {
//            this.reader=new BufferedReader(new InputStreamReader(inputStream));
//            this.printStream=printStream;
            super("espeak-reader-" + name);
            this.close = close;
            inStream = in;
            outStream = out;
            buffer = new byte[1024];
        }

        public void run() {
            while (!eof) {
                try {
//                    line = reader.readLine();
                    count = inStream.read(buffer);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    count = -1;
                }
                if (count == -1) {
                    eof = true;
                    break;
                }
//                lines.add(line);
//                printStream.println(line);
                try {
                    outStream.write(buffer, 0, count);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (close) {
                try {
                    outStream.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    class PlayThread extends Thread {

        InputStream inStream;

        PlayThread(InputStream in) {
            super("espeak-play");
            inStream = in;
        }

        @Override
        public void run() {
            AudioFormat audioFormat = null;
            long bytesPerSecond = 44100 * 4;
            int nSampleSizeInBits = 16;
            String strMixerName = null;
            int nInternalBufferSize = AudioSystem.NOT_SPECIFIED;
            int bufferSecs = 1;
            SourceDataLine line = null;
            try {
                AudioInputStream audioInputStream;
                audioInputStream = AudioSystem.getAudioInputStream(inStream);
                /*
                 *  From the AudioInputStream, i.e. from the sound file,
                 *  we fetch information about the format of the
                 *  audio data.
                 *  These information include the sampling frequency,
                 *  the number of
                 *  channels and the size of the samples.
                 *  These information
                 *  are needed to ask Java Sound for a suitable output line
                 *  for this audio stream.
                 */
                audioFormat = audioInputStream.getFormat();

                bytesPerSecond = (long) audioFormat.getSampleRate()
                        * audioFormat.getChannels() * (nSampleSizeInBits / 8);
                // Use 5 seconds to avoid problems with gc
                nInternalBufferSize = (int) bytesPerSecond * bufferSecs;
                // nInternalBufferSize = 100;
                DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                        audioFormat, nInternalBufferSize);

                boolean bIsSupportedDirectly = AudioSystem.isLineSupported(info);
                if (!bIsSupportedDirectly) {
                    AudioFormat sourceFormat = audioFormat;
                    AudioFormat targetFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            sourceFormat.getSampleRate(),
                            nSampleSizeInBits,
                            sourceFormat.getChannels(),
                            sourceFormat.getChannels() * (nSampleSizeInBits / 8),
                            sourceFormat.getSampleRate(),
                            false);
                    audioInputStream = AudioSystem.getAudioInputStream(targetFormat, audioInputStream);
                    audioFormat = audioInputStream.getFormat();
                }
                line = null;
                line = AudioPlayer.getSourceDataLine(mixerName, audioFormat, nInternalBufferSize);
                /*
                 *  Still not enough. The line now can receive data,
                 *  but will not pass them on to the audio output device
                 *  (which means to your sound card). This has to be
                 *  activated.
                 */
                line.start();
                FloatControl volumeControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                String gainstr = mixerGain;
                float gain = 0;
                if (gainstr != null) {
                    try {
                        gain = Float.parseFloat(gainstr);
                        // convert Gain (percentage) to decibels
                        gain = (float) 10.0 * (float) Math.log10((double) gain * 0.01);
                        // convert speech Gain (percentage) to decibels
                        float speechGain = (float) 10.0 * (float) Math.log10((double) volume * 0.01);
                        gain += speechGain;
                    } catch (NumberFormatException ex) {
                        gain = 0;
                        System.err.println("Invalid gain value: " + gain + ", set to 0");
                    }
                    if (gain > volumeControl.getMaximum()) {
                        System.err.println("Gain value: " + gain + " too high, set to Maximum value: "
                                + volumeControl.getMaximum());
                        gain = volumeControl.getMaximum();
                    }
                    if (gain < volumeControl.getMinimum()) {
                        System.err.println("Gain value: " + gain + " too low, set to Minimum value: "
                                + volumeControl.getMinimum());
                        gain = volumeControl.getMinimum();
                    }
                }
                volumeControl.setValue(gain);

                /*
                 *  Ok, finally the line is prepared. Now comes the real
                 *  job: we have to write data to the line. We do this
                 *  in a loop. First, we read data from the
                 *  AudioInputStream to a buffer. Then, we write from
                 *  this buffer to the Line. This is done until the end
                 *  of the file is reached, which is detected by a
                 *  return value of -1 from the read method of the
                 *  AudioInputStream.
                 */
                int nBytesRead = 0;

                // Normally it seems to only get 4608 bytes with each read
                // so buffer bigger than that is a waste
                byte[] abData;
                abData = new byte[4608];
                nBytesRead = audioInputStream.read(abData, 0, abData.length);
                while (nBytesRead != -1) {
                    if (nBytesRead >= 0) {
                        line.write(abData, 0, nBytesRead);
                    }
                    nBytesRead = audioInputStream.read(abData, 0, abData.length);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (line != null) {
                    line.drain();
                    line.close();
                }
            }
        }
    }

    public boolean setRate(int rate) {
        rateWPM = (rate + 11) * 15;
        return true;
    }

    public void setVoice(String voiceName) {
        voice = voiceName;
    }

    public boolean setVolume(int volume) {
        this.volume = volume;
        return true;
    }

//     xml format - 
//    <speak>
//    <voice name="afrikaans">
//    my pen is in my hand
//    </voice>
//    </speak>
    public boolean speak(String strInput) {
        if (eSpeakProcess == null || outWriter == null) //  || !outThread.isAlive())
        {
            init();
        }
        if (eSpeakProcess == null || outWriter == null) //  || !outThread.isAlive())
        {
            return false;
        }
//        outWriter.println("<speak>");
        if (xmlVoice != null) {
            outWriter.println("<voice name=\"" + voice + "\">");
        }
        outWriter.println(strInput);
        outWriter.println("\n");
        if (xmlVoice != null) {
            outWriter.println("</voice>");
        }
//        outWriter.println("</speak>");
        close();
        int ret = 999;
        try {
            ret = eSpeakProcess.waitFor();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (ret != 0) {
            Exception ex = new Exception("ESpeak process failed, ret=" + ret);
            ex.printStackTrace();
        }
        eSpeakProcess = null;
        if (mbrolaProcess != null) {
            ret = 999;
            try {
                ret = mbrolaProcess.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (ret != 0) {
                Exception ex = new Exception("mbrola process failed, ret=" + ret);
                ex.printStackTrace();
            }
            mbrolaProcess = null;
        }
        if (playThread != null) {
            try {
                playThread.join(60000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return true;
    }

    public String[] getVoiceList() {
        if (espeakDataPath != null && espeakDataPath.length() > 0) {

            File voicesDir = new File(espeakDataPath, "voices");
            Vector voicesVec = new Vector();
            processDirectory(voicesDir, voicesVec);
            if (voicesVec.size() == 0) {
                return null;
            }
            String[] voicesArray = new String[voicesVec.size()];
            voicesArray = (String[]) voicesVec.toArray(voicesArray);
            return voicesArray;
        } else {
            return null;
        }
    }

    void processDirectory(File dir, Vector voicesVec) {
        String dirName = dir.getName();
        File[] dirArray = dir.listFiles();
        if (dirArray == null) {
            return;
        }
        int ix;
        for (ix = 0; ix < dirArray.length; ix++) {
            if (dirArray[ix].isDirectory()) {
                String subDirName = dirArray[ix].getName();
                if (!"!v".equals(subDirName)) {
                    processDirectory(dirArray[ix], voicesVec);
                }
            } else {
                // find voice name
                String name = null;
                String mbVoice = null;
                if ("mb".equals(dirName)) {
                    mbVoice = dirArray[ix].getName();
                    String mbrolaLang = mbVoice.substring(3, 6);
                    File mbVoiceFile = new File(mbrolaBase + "/" + mbrolaLang + "/" + mbrolaLang);
                    if (!mbVoiceFile.exists()) {
                        mbVoiceFile = new File(mbrolaBase + "/" +  mbrolaLang);
                        if (!mbVoiceFile.exists())
                            continue;
                    }
                }
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(dirArray[ix]));
                    while (name == null) {
                        String inLine = reader.readLine();
                        if (inLine == null) {
                            break;
                        }
                        inLine = inLine.trim();
                        String[] fields = inLine.split(" ", 2);
                        if ("name".equals(fields[0])) {
                            if (mbVoice != null) {
                                name = mbVoice + " " + fields[1];
                            } else {
                                name = fields[1];
                            }
                        }
                    }
                } catch (IOException io) {
                    io.printStackTrace();
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(ESpeakSpeaker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if (name != null) {
                    voicesVec.add(name);
                }
            }
        }

    }
}



