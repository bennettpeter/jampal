package pgbennett.jampal;

/*
    Jampal - Java Mp3 Library
 */

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


import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.net.*;

/** 
 */
public class Jampal {
    static String [] initialLibraryNames = { null };
    static File initialPropertiesFile;
    public static Properties initialProperties;
    public static Properties environmentProperties;
    static boolean isException=false;
    static String exceptionMsg="";
    public static HashMap <String, String[]> voiceSettings;
    static String baseDirectory = null;

    // debug java -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000 -jar "C:\Program Files\Jampal\jampal.jar"

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() throws Exception {
        
        setSystemProperties();
        
        if (isException) {
            String msg="Unable to start java";
            String ver=System.getProperty("java.version");
            if (ver.compareTo("1.6")<0)
                msg="Java version 1.6 or later required, found "+ver;
            JOptionPane.showMessageDialog(null,
            msg,
            exceptionMsg,
            JOptionPane.ERROR_MESSAGE);
            System.exit(2);

        }
    
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        int ix;
        for (ix=0;ix<initialLibraryNames.length;ix++) {
            new MainFrame(initialLibraryNames[ix]);
        }
        if (MainFrame.mainFrameMap.isEmpty() && !MainFrame.isOpening) {
            new MainFrame(null);
        }
        if (MainFrame.mainFrameMap.isEmpty())
            System.exit(0);

    }


    static void setSystemProperties () {
        String mbrolaProg=initialProperties.getProperty("mbrola-prog");
        if (mbrolaProg!=null) 
            System.setProperty("mbrola.prog", mbrolaProg);
        String mbrolaPath=initialProperties.getProperty("mbrola-path");
        if (mbrolaPath!=null) 
            System.setProperty("mbrola.base", mbrolaPath);
        String cepstralPath=initialProperties.getProperty("cepstral-path");
        if (cepstralPath!=null) 
            System.setProperty("cepstral.home", cepstralPath);
        // ESPEAK PATH
        String currentValue = initialProperties.getProperty("espeak-prog");
        currentValue = getESpeakProg(currentValue);
        if (currentValue != null) {
            initialProperties.setProperty("espeak-prog", currentValue);
            System.setProperty("espeak.prog", currentValue);
        }
        // ESPEAK DATA
        currentValue = Jampal.initialProperties.getProperty("espeak-data");
        currentValue = getESpeakData(currentValue);
        if (currentValue != null) {
            initialProperties.setProperty("espeak-data", currentValue);
            System.setProperty("espeak.data", currentValue);
        }
    }
    
    public static String getESpeakProg(String currentValue) {
        if (currentValue == null || currentValue.length() == 0) {
            if (File.separatorChar == '/')
                currentValue = "espeak";
            else if (File.separatorChar == '\\')
                currentValue = "C:\\Program Files\\eSpeak\\command_line\\espeak";
        }
        return currentValue;
    }
    
    public static String getESpeakData(String currentValue) {
        if (currentValue == null || currentValue.length() == 0){
            if (File.separatorChar == '/')
                currentValue = "/usr/share/espeak-data";
            else if (File.separatorChar == '\\')
                currentValue = "C:\\Program Files\\eSpeak\\espeak-data";
            initialProperties.setProperty("espeak-data", currentValue);
        }
        return currentValue;
    }
    
    public static void updateLibNamesProperties(String[]libraryNames) {
        StringBuffer parm = new StringBuffer();
        int ix;
        for (ix=0;ix<libraryNames.length;ix++) {
            if (ix>0)
                parm.append(File.pathSeparator);
            parm.append(libraryNames[ix]);
        }
        initialProperties.setProperty("open-libraries", parm.toString());
    }

    public static void save() {
        try {
            FileOutputStream file = new FileOutputStream(initialPropertiesFile);
            initialProperties.store(file,null);
            file.close();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    static String jampalDirectory;
    public static String gainVersion="";
    
    public static void main(String[] args) {
        try {
            jampalDirectory = System.getProperty("user.home")+File.separator+".jampal"+File.separator;
            File dir = new File(jampalDirectory);
            if (!dir.isDirectory())
                dir.mkdir();

            loadInitialProperties();
            
            if (checkPreviousInstance(args))
                return;
            
            if (System.getProperty("DEBUG")==null) {
                PrintStream out = new PrintStream
                (new FileOutputStream(jampalDirectory+"jampal_out.txt"));
                System.setOut(out);
                PrintStream err = new PrintStream
                (new FileOutputStream(jampalDirectory+"jampal_err.txt"));
                System.setErr(err);
            }
            
            // Load voice Settings
            File voiceFile = new File(jampalDirectory,"voices.txt");
            voiceSettings = new HashMap <String, String[]>();
            if (voiceFile.canRead()) {
                InputStream inStream = new FileInputStream(voiceFile);
                Reader reader = new InputStreamReader(inStream,"UTF-8");
                BufferedReader bufferedReader = new BufferedReader(reader);
                String inLine = "";
                while (inLine != null) {
                    inLine = bufferedReader.readLine();
                    if (inLine != null && inLine.length()>3 && !inLine.startsWith("#")) {
                        // sample input lines
                        //|eSpeak||100|
                        //other|eSpeak||200|
                        //eng|eSpeak|english-us|100|
                        String [] fields = inLine.split("\\|", -1);
                        if (fields.length >= 4)
                            voiceSettings.put(fields[0], new String[] {fields[1],fields[2],fields[3]});
                    }
                }
            }            
            
            String lookAndFeel = initialProperties.getProperty("look-and-feel");
            if (lookAndFeel==null) {
                // lookAndFeel="com.birosoft.liquid.LiquidLookAndFeel";
                lookAndFeel=UIManager.getSystemLookAndFeelClassName();
                initialProperties.setProperty("look-and-feel",lookAndFeel);
            }
            try {
                UIManager.setLookAndFeel(lookAndFeel);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                lookAndFeel=UIManager.getSystemLookAndFeelClassName();
                UIManager.setLookAndFeel(lookAndFeel);
            }
//            JFrame.setDefaultLookAndFeelDecorated(false);
//            JDialog.setDefaultLookAndFeelDecorated(false);
            String ver=System.getProperty("java.version");
            String initial="300";
            if (ver.compareTo("1.5")>=0) {
                gainVersion="v5";
                initial="100";
            }
            
            String mixerGain = initialProperties.getProperty("mixer-gain-percent");
            if (mixerGain==null) {
                initialProperties.setProperty("mixer-gain-percent",initial);
                save();
            }
            String speechRate = initialProperties.getProperty("speech-rate");
            if (speechRate==null) {
                initialProperties.setProperty("speech-rate","0");
                save();
            }
            
            if (args.length > 0)
                initialLibraryNames = args;
            else {
                String propInitialNames = initialProperties.getProperty("open-libraries");
                if (propInitialNames!=null)
                    initialLibraryNames = propInitialNames.split(File.pathSeparator);
            }
        }
        catch(Throwable ex) {
            isException=true;
            exceptionMsg=ex.toString();
            ex.printStackTrace();
        }

        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {
                    try {
                        createAndShowGUI();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        );
        
    }

    // Called if initial properties are not found
    static void loadInitialProperties()
    throws IOException {

        // url will have one of these
        // Protocol "jar"  file "file:/C:/Program%20Files/.../pbackup.jar!/pgbennett/pbackup/Mainframe.class"
        // Protocol "file" file "/C:/proj/cvs/pbackup/pbackup_j/build/classes/pgbennett/pbackup/Mainframe.class"
        String className = "pgbennett/jampal/Jampal.class";
        URL url =  ClassLoader.getSystemClassLoader().getResource(className);
        if (url!=null) {
            if ("jar".equals(url.getProtocol())) {
                String file = url.getFile();
                int end = file.lastIndexOf('!');
                end = file.lastIndexOf('/',end);
                baseDirectory = file.substring(5, end);
            }
            else if ("file".equals(url.getProtocol())) {
                String file = url.getFile();
                baseDirectory = file.substring(0, file.length()-className.length()-1);
            }
            baseDirectory = URLDecoder.decode(baseDirectory, "UTF-8");

        }
        initialProperties = new Properties();
        environmentProperties = new Properties();
        if (baseDirectory != null) {
            String propFileName = baseDirectory+"/jampal_initial.properties";
            initialPropertiesFile = new File(propFileName);
            if (initialPropertiesFile.exists()) {
                InputStream input = new FileInputStream(initialPropertiesFile);
                initialProperties.load(input);
                input.close();
            }
            propFileName = baseDirectory+"/jampal_environment.properties";
            initialPropertiesFile = new File(propFileName);
            if (initialPropertiesFile.exists()) {
                InputStream input = new FileInputStream(initialPropertiesFile);
                environmentProperties.load(input);
                input.close();
            }

        }
        String propFileName = jampalDirectory+"jampal_initial.properties";
        initialPropertiesFile = new File(propFileName);
        if (initialPropertiesFile.exists()) {
            InputStream input = new FileInputStream(initialPropertiesFile);
            initialProperties.load(input);
            input.close();
        }
        String mbrolaProg=initialProperties.getProperty("mbrola-prog");
        if (mbrolaProg != null && mbrolaProg.startsWith(".")) {
            mbrolaProg = baseDirectory + "/" + mbrolaProg;
            File abstractPath = new File(mbrolaProg);
            mbrolaProg = abstractPath.getCanonicalPath();
            initialProperties.setProperty("mbrola-prog", mbrolaProg);
        }
    }


    static ServerSocket serverSocket;

    /**
     * Check if this application is already running.
     * Jampal listens on a port. If it is already running on this machine
     * under the same user id, it simply activates the previous version
     * and opens any libraries that were specified on the command line.
     * @param args Input parameters for the program.
     * @return true means a previous instance was found and activated
     */
    static boolean  checkPreviousInstance(String [] args) {
        String portStr = initialProperties.getProperty("listen-port");
        int listenPort=0;
        try {
            if (portStr!=null)
                listenPort = Integer.parseInt(portStr);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        if (listenPort!=0) {
            try {
                InetAddress address = InetAddress.getByName("localhost");
                Socket socket = new Socket(address,listenPort);
                socket.setSoTimeout(10000);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in =  new ObjectInputStream(socket.getInputStream());
                out.writeObject("###Jampal###");
                out.writeObject(args);
                out.flush();
                String resp = (String)in.readObject();
                if ("###Jampal###".equals(resp)) {
                    socket.close();
                    return true;
                }
            }
            catch (java.net.ConnectException connEx) {
                
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        try {
            InetAddress address = InetAddress.getByName("localhost");
            for (listenPort=25000;listenPort<25050;listenPort++) {
                try {
                    serverSocket = new ServerSocket(listenPort,50,address);
                    break;
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            initialProperties.setProperty("listen-port", String.valueOf(listenPort));
            save();
            Thread listenThread = new Thread("jampal-socket-listener") {
                public void run() {
                    for (;;) {
                        Socket socket = null;
                        try {
                            socket = serverSocket.accept();
                            socket.setSoTimeout(10000);
                            ObjectInputStream in =  new ObjectInputStream(socket.getInputStream());
                            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                            String inStr = (String)in.readObject();
                            if ("###Jampal###".equals(inStr)) {
                                String [] args = (String [])in.readObject();
                                LibraryOpener opener = new LibraryOpener(args);
                                SwingUtilities.invokeLater(opener);
                            }
                            out.writeObject("###Jampal###");
                        }
                        catch(Exception ex) {
                            ex.printStackTrace();
                        }
                        finally {
                            try {
                                if (socket!=null)
                                    socket.close();
                            }
                            catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            
                        }
                    }
                    
                }
            };
            listenThread.setDaemon(true);
            listenThread.start();
            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;

    }
    
    static class LibraryOpener implements Runnable{
        
        String [] initialLibraryNames;
        LibraryOpener(String[] args) {
            initialLibraryNames=args;
        }
        public void run() {

            // If there were no parameters show the existing windows
            if (initialLibraryNames.length==0) {
                if (MainFrame.mainFrameMap.isEmpty())
                    return;
                Collection frames = MainFrame.mainFrameMap.values();
                Iterator it = frames.iterator();
                while (it.hasNext()) {
                    MainFrame already = (MainFrame)it.next();
                    if (already != null) {
                        already.frame.setVisible(true);
                        already.frame. setState(Frame.NORMAL);
                    }
                }
            }
            
            //Create and set up the window(s) if there were parameters
            int ix;
            for (ix=0;ix<initialLibraryNames.length;ix++) {
                new MainFrame(initialLibraryNames[ix]);
            }
            
        }
        
    }
}



                                               