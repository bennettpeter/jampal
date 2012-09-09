
/*
    Copyright 2007 Peter Bennett

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
 
 
 
package pgbennett.utility;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.Normalizer;

/**
 *
 * @author peter
 */
public class TextSimplifier {
    public static void main(String[] args) throws Exception {
        int i;
        String inputEncoding = null;
        String outputEncoding = null;
        String inFile = null;
        String outFile = null;
        InputStream inStream = System.in;
        OutputStream outStream = System.out;
        boolean lowerCase = false;
        
        for (i=0;i<args.length;i++) {
            if (args[i].equals("-i") && ++i < args.length) {
                inputEncoding = args[i];
            }
            else if (args[i].equals("-o") && ++i < args.length) {
                outputEncoding = args[i];
            }
            else if (args[i].equals("-l")) {
                lowerCase = true;
            }
            else if (!args[i].startsWith("-") && inFile == null) {
                inFile = args[i];
                inStream = new FileInputStream(inFile);
            }
            else if (!args[i].startsWith("-") && outFile == null) {
                outFile = args[i];
                outStream = new FileOutputStream(outFile);
            }
            else {
                System.err.println("Simplify text file - remove diacritics and optionally change to lowercase");
                System.err.println("Usage (all fields optional):");
                System.err.println("-i input-encoding");
                System.err.println("-o output-encoding");
                System.err.println("-l");
                System.err.println("input file name");
                System.err.println("output file name");
                System.exit(-1);
            }
        }
            
        InputStreamReader inputStreamReader;
        if (inputEncoding == null)
            inputStreamReader = new InputStreamReader(inStream);
        else
            inputStreamReader = new InputStreamReader(inStream, inputEncoding);

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        
        PrintStream printStream;
        if (outputEncoding == null)
            printStream = new PrintStream(outStream, false);
        else
            printStream = new PrintStream(outStream, false, outputEncoding);
        for (;;) {
            String trackStr = bufferedReader.readLine();
            if (trackStr==null)
                break;
            trackStr = simplifyString(trackStr, lowerCase);
            printStream.println(trackStr);
        }
        
    }

    
//    static final char translate[] = {
//        ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     
//        ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     
//
//        ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     
//        ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     ' ',     
//
//        '\u0020','\u0021','\u0022','\u0023','\u0024','\u0025','\u0026','\'',
//        '\u0028','\u0029','\u002a','\u002b','\u002c','\u002d','\u002e','\u002f',
//
//        '\u0030','\u0031','\u0032','\u0033','\u0034','\u0035','\u0036','\u0037',
//        '\u0038','\u0039','\u003a','\u003b','\u003c','\u003d','\u003e','\u003f',
//
//        '\u0040','a',     'b',     'c',     'd',     'e',     'f',     'g',     
//        'h',     'i',     'j',     'k',     'l',     'm',     'n',     'o',     
//
//        'p',     'q',     'r',     's',     't',     'u',     'v',     'w',     
//        'x',     'y',     'z',     '\u005b','\\',    '\u005d','\u005e','\u005f',
//
//        '\u0060','\u0061','\u0062','\u0063','\u0064','\u0065','\u0066','\u0067',
//        '\u0068','\u0069','\u006a','\u006b','\u006c','\u006d','\u006e','\u006f',
//
//        '\u0070','\u0071','\u0072','\u0073','\u0074','\u0075','\u0076','\u0077',
//        '\u0078','\u0079','\u007a','\u007b','\u007c','\u007d','\u007e','\u007f',
//
//        '\u0080','\u0081','\u0082','\u0083','\u0084','\u0085','\u0086','\u0087',
//        '\u0088','\u0089','\u008a','\u008b','\u008c','\u008d','\u008e','\u008f',
//
//        '\u0090','\u0091','\u0092','\u0093','\u0094','\u0095','\u0096','\u0097',
//        '\u0098','\u0099','\u009a','\u009b','\u009c','\u009d','\u009e','\u009f',
//
//        '\u00a0','\u00a1','\u00a2','\u00a3','\u00a4','\u00a5','\u00a6','\u00a7',
//        '\u00a8','\u00a9','\u00aa','\u00ab','\u00ac','\u00ad','\u00ae','\u00af',
//
//        '\u00b0','\u00b1','\u00b2','\u00b3','\u00b4','\u00b5','\u00b6','\u00b7',
//        '\u00b8','\u00b9','\u00ba','\u00bb','\u00bc','\u00bd','\u00be','\u00bf',
//
//        'a',     'a',     'a',     'a',     'a',     'a',     '\u00c6','c',     
//        'e',     'e',     'e',     'e',     'i',     'i',     'i',     'i',     
//
//        '\u00d0','n',     'o',     'o',     'o',     'o',     'o',     '\u00d7',
//        'o',     'u',     'u',     'u',     'u',     'y',     '\u00de','s',     
//
//        'a',     'a',     'a',     'a',     'a',     'a',     '\u00e6','c',     
//        'e',     'e',     'e',     'e',     'i',     'i',     'i',     'i',     
//
//        '\u00f0','n',     'o',     'o',     'o',     'o',     'o',     '\u00f7',
//        'o',     'u',     'u',     'u',     'u',     'y',     '\u00fe','y',
//
//        'a','a','a','a','a','a',
//        'c','c','c','c','c','c','c','c',
//        'd','d','d','d',
//        'e','e','e','e','e','e','e','e','e','e',
//        'g','g','g','g','g','g','g','g',
//        'h','h','h','h',
//        'i','i','i','i','i','i','i','i','i','i','i','i',
//        'j','j',
//        'k','k','k',
//        'l','l','l','l','l','l','l','l','l','l',
//        'n','n','n','n','n','n','n','n','n',
//        'o','o','o','o','o','o','o','o',
//        'r','r','r','r','r','r',
//        's','s','s','s','s','s','s','s',
//        't','t','t','t','t','t',
//        'u','u','u','u','u','u','u','u','u','u','u','u',
//        'w','w',
//        'y','y','y',
//        'z','z','z','z','z','z'
//    };
//    static final int translateLeng = translate.length;
//        
//    
//    // translate string to lower case and also remove diacritics
//    static String simplifyString1(String strData) {
//        if (strData == null)
//            return null;
//        char charData[] = strData.toCharArray();
//        int leng = charData.length;
//        
//        int ix;
//        for (ix=0;ix < leng; ix++){
//            char theChar = charData[ix];
//            if (theChar < translateLeng)
//                charData[ix] = translate[theChar];
//        }
//        return new String(charData);
//    }

    // Remove diacritics after normalizing to NFKD
    static Pattern diacritics = Pattern.compile(
    "[\u0300-\u036F\u1DC0-\u1DFF\u20D0-\u20FF\uFE20-\uFE2F]");

    // These doubtful characters are not handled  (oe) (smart quote) (slash o) (German SS looks like a B)  
    
    public static String simplifyString(String strData, boolean lowerCase) {
        if (strData == null)
            return null;
        String s = Normalizer.normalize(strData,  Normalizer.Form.NFKD);
        Matcher matcher = diacritics.matcher(s);
        s = matcher.replaceAll("");
        
        if (lowerCase)
            s = s.toLowerCase();
        return s;
    }
    
    
    
    // Remove parenthesized parts () and []
    static Pattern normalizePattern1 = Pattern.compile(
        "\\([^\\)]*\\) *|\\[[^\\]]*\\] *");
//        "^[^a-z0-9]*\\([^\\)]*\\)");
    // Remove the word 'a' and the word 'the' at the beginning, also leading blanks
    static Pattern normalizePattern2 = Pattern.compile(
        "^a +|^the +|^ *");
//        "^[^a-z0-9]*a[^a-z0-9]+|^[^a-z0-9]*the[^a-z0-9]+|^[^a-z0-9]+");
    // Remove special chars
    static Pattern normalizePattern3 = Pattern.compile(
        "[^a-z0-9 ]");
    public static String normalize(String s) {
        if (s==null)
            return null;
        Matcher matcher = normalizePattern1.matcher(s);
        s = matcher.replaceAll("");
        matcher = normalizePattern2.matcher(s);
        s = matcher.replaceFirst("");
        matcher = normalizePattern3.matcher(s);
        s = matcher.replaceAll("");
        s = s.trim();
        return s;
    }
    // Normalize without removing parens
    public static String albumNormalize(String s) {
        if (s==null)
            return null;
        Matcher matcher = normalizePattern2.matcher(s);
        s = matcher.replaceFirst("");
        matcher = normalizePattern3.matcher(s);
        s = matcher.replaceAll("");
        s = s.trim();
        return s;
    }

    
    
    

}
