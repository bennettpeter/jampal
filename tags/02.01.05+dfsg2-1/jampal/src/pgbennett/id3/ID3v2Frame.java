package pgbennett.id3;

import java.io.UnsupportedEncodingException;

/*
 *  Copyright (C) 2001,2002 Jonathan Hilliker
 *
    This file is part of the jd3lib library.

    This copy of jd3lib has been incorporated into Jampal under the 
    GNU general Public License.

    Modifications to the file Copyright 2004 Peter Bennett.

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
 *
 *   Modified By Peter Bennett <pgbennett@users.sourceforge.net>,
 *   April 2004 and incorporated into Jampal.
 */
/**
 * This class holds the information found in an id3v2 frame. At this point at
 * least, the information in the tag header is read-only and can only be set set
 * in the constructor. This is for simplicity's sake. This object doesn't
 * automatically unsynchronise, encrypt, or compress the data. <br />
 *
 * <dl>
 *   <dt> <b>Version History</b> </dt>
 *   <dt> 1.3.1 - <small>2002.1023 by gruni</small></dt>
 *   <dd> -Made Sourcecode compliant to the Sun CodingConventions</dd>
 *   <dt> 1.3 - <small>2002/01/13 by helliker</small> </dt>
 *   <dd> -Added isEmpty method</dd>
 *   <dt> 1.2 - <small>2001/10/19 by helliker</small> </dt>
 *   <dd> -All set for release.</dd>
 * </dl>
 *
 *
 *@author    Jonathan Hilliker
 *@version   1.3.1
 */

public class ID3v2Frame {
    
    /**
     * Frame Header Size of 10 Bytes
     *
     *@deprecated   will be replaced with dynamic Definitions
     */
    private final int FRAME_HEAD_SIZE = 10;
    
    /**
     * Flags Size of two 2 Bytes
     *
     *@deprecated   will be replaced with dynmaic Definitions
     */
    private final int FRAME_FLAGS_SIZE = 2;
    
    /**
     * ???? WHAT IS THIS
     *
     *@deprecated
     */
    private final int MAX_EXTRA_DATA = 5;
    
    /**
     * Has nothing to do with the fram itself not every frame needs that This is
     * part of the Fields
     *
     *@deprecated
     */
    static final String[] ENC_TYPES = {"ISO-8859-1", "UTF16",
    "UTF-16BE", "UTF-8"};
    /**
     * The Frame ID will be kept may init diffrent
     */
    private String id = null;
    // Peter Bennett
    /**
     * Extended id includes language and description where applicable
     */
    private String extendedId = null;
    /**
     * a - Tag alter preservation.<br/>
     * <br/>
     * This flag tells the tag parser what to do with this frame if it is unknown
     * and the tag is altered in any way. This applies to all kinds of
     * alterations, including adding more padding and reordering the frames.<br/>
     *
     * <tableborder="0">
     *
     *   <tr>
     *
     *     <td>
     *       0 -(false)
     *     </td>
     *
     *     <td>
     *       Frame should be preserved.
     *     </td>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <td>
     *       1 - (true)
     *     </td>
     *
     *     <td>
     *       Frame should be discarded.
     *     </td>
     *
     *   </tr>
     *
     * </table>
     *
     */
    private boolean tagAlterDiscard;
    /**
     * b - File alter preservation.</br> <br/>
     * This flag tells the tag parser what to do with this frame if it is unknown
     * and the file, excluding the tag, is altered. This does not apply when the
     * audio is completely replaced with other audio data.</br>
     * <tableborder="0">
     *
     *   <tr>
     *
     *     <td>
     *       0 (false)
     *     </td>
     *
     *     <td>
     *       Frame should be preserved.
     *     </td>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <td>
     *       1 (true)
     *     </td>
     *
     *     <td>
     *       Frame should be discarded.
     *     </td>
     *
     *   </tr>
     *
     * </table>
     *
     */
    private boolean fileAlterDiscard;
    /**
     * c - Read only.<br/>
     * <br/>
     * This flag, if set, tells the software that the contents of this frame are
     * intended to be read only. Changing the contents might break something, e.g.
     * a signature. If the contents are changed, without knowledge of why the
     * frame was flagged read only and without taking the proper means to
     * compensate, e.g. recalculating the signature, the bit MUST be cleared.
     */
    private boolean readOnly;
    /**
     * h - Grouping identity.</br> <br/>
     * This flag indicates whether or not this frame belongs in a group with other
     * frames. If set, a group identifier byte is added to the frame. Every frame
     * with the same group identifier belongs to the same group.<br/>
     *
     * <tableborder="0">
     *
     *   <tr>
     *
     *     <td>
     *       0 (false)
     *     </td>
     *
     *     <td>
     *       Frame does not contain group information
     *     </td>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <td>
     *       1 (true)
     *     </td>
     *
     *     <td>
     *       Frame contains group information
     *     </td>
     *
     *   </tr>
     *
     * </table>
     *
     */
    private boolean grouped;
    /**
     * k - Compression.<br/>
     * <br />
     * This flag indicates whether or not the frame is compressed. A 'Data Length
     * Indicator' byte MUST be included in the frame.<br/>
     *
     * <table>
     *
     *   <tr>
     *
     *     <td>
     *       0 (false)
     *     </td>
     *
     *     <td>
     *       Frame is not compressed.
     *     </td>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <tdvalign="top">
     *       1 (true)
     *     </td>
     *
     *     <td>
     *       Frame is compressed using zlib <a
     *       href="ftp://ftp.isi.edu/in-notes/rfc1950.txt" title="P. Deutsch,
     *       Aladdin Enterprises & J-L. Gailly, 'ZLIB Compressed Data Format
     *       Specification version 3.3', RFC 1950, May 1996.">[ZLIB]</a> deflate
     *       method. If set, this requires the 'Data Length Indicator' bit to be
     *       set as well.
     *     </td>
     *
     *   </tr>
     *
     * </table>
     *
     */
    private boolean compressed;
    /**
     * m - Encryption.<br/>
     * <br/>
     * This flag indicates whether or not the frame is encrypted. If set, one byte
     * indicating with which method it was encrypted will be added to the frame.
     * See description of the ENCR frame for more information about encryption
     * method registration. Encryption should be done after compression. Whether
     * or not setting this flag requires the presence of a 'Data Length Indicator'
     * depends on the specific algorithm used.<br/>
     *
     * <table>
     *
     *   <tr>
     *
     *     <td>
     *       0 (false)
     *     </td>
     *
     *     <td>
     *       Frame is not encrypted.
     *     </td>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <td>
     *       1 (true)
     *     </td>
     *
     *     <td>
     *       Frame is encrypted.
     *     </td>
     *
     *   </tr>
     *
     * </table>
     *
     */
    private boolean encrypted;
    /**
     * n - Unsynchronisation<br />
     * <br />
     * This flag indicates whether or not unsynchronisation was applied to this
     * frame. See section 6 for details on unsynchronisation. If this flag is set
     * all data from the end of this header to the end of this frame has been
     * unsynchronised. Although desirable, the presence of a 'Data Length
     * Indicator' is not made mandatory by unsynchronisation.
     * <table>
     *
     *   <tr>
     *
     *     <td>
     *       0 (false)
     *     </td>
     *
     *     <td>
     *       Frame has not been unsynchronised.
     *     </td>
     *
     *   </tr>
     *
     *   <tr>
     *
     *     <td>
     *       1 (true)
     *     </td>
     *
     *     <td>
     *       Frame has been unsyrchronised.
     *     </td>
     *
     *   </tr>
     *
     * </table>
     *
     */
    private boolean unsynchronised;
    /**
     * p - Data length indicator<br />
     * <br />
     * This flag indicates that a data length indicator has been added to the
     * frame. The data length indicator is the value one would write as the 'Frame
     * length' if all of the frame format flags were zeroed, represented as a 32
     * bit synchsafe integer.<br/>
     *
     * <table>
     *
     *   <tr>
     *
     *     <td>
     *       0 (false)
     *     </td>
     *
     *     <td>
     *       There is no Data Length Indicator.
     *     </td>
     *
     *     <tr>
     *
     *       <td>
     *         1 (true)
     *       </td>
     *
     *       <td>
     *         A data length Indicator has been added to the frame.
     *       </td>
     *
     *     </tr>
     *
     *   </table>
     *
     */
    private boolean lengthIndicator;
    /**
     * The Group Bit ???
     */
    private byte group;
    /**
     * The Encryption Tpe Byte
     */
    private byte encrType;
    /**
     * The Data Length
     */
    private int dataLength;
    /**
     * The Frame Data in Byte[]
     */
    private byte[] frameData;
    
    /**
     * Picture data for APIC frame
     */
    private ID3v2Picture picture;
    
    private int majorVersion = 3;
    
    /**
     * Create an ID3v2Frame with a specified id, a byte array containing the frame
     * header flags, and a byte array containing the data for this frame.
     *
     *@param id                        the id of this frame
     *@param flags                     the flags found in the header of the frame
     *      (2 bytes)
     *@param data                      the data found in this frame
     *@exception ID3v2FormatException  if an error occurs
     */
    public ID3v2Frame(String id, byte[] flags, byte[] data, int majorVersion)
    throws ID3v2FormatException, UnsupportedEncodingException {
        this.id = id;
        this.majorVersion = majorVersion;
        parseFlags(flags);
        parseData(data);
        
    }
    
    
    /**
     * Create and ID3v2 frame with the specified id and data. All the flag bits
     * are set to false.
     *
     *@param id    the id of this frame
     *@param data  the data for this frame
     */
    public ID3v2Frame(String id, byte[] data) throws UnsupportedEncodingException {
        this.id = id;
        
        tagAlterDiscard = false;
        fileAlterDiscard = checkDefaultFileAlterDiscard();
        readOnly = false;
        grouped = false;
        compressed = false;
        encrypted = false;
        unsynchronised = false;
        lengthIndicator = false;
        group = '\0';
        encrType = '\0';
        dataLength = -1;
        
        parseData(data);
    }

    /**
     * Create and ID3v2 frame with the specified id and data. All the flag bits
     * are set to false.
     *
     *@param id    the id of this frame
     *@param data  the data for this frame
     */
    public ID3v2Frame(ID3v2Picture pic) throws UnsupportedEncodingException {
        this.id = "APIC";
        
        tagAlterDiscard = false;
        fileAlterDiscard = checkDefaultFileAlterDiscard();
        readOnly = false;
        grouped = false;
        compressed = false;
        encrypted = false;
        unsynchronised = false;
        lengthIndicator = false;
        group = '\0';
        encrType = '\0';
        dataLength = -1;
        
        setPicture(pic);
        createExtendedId();
    }
    
    
    static final byte [] emptyByteArray = new byte[0];
    public ID3v2Frame(String id, String lang, String desc, String data) 
            throws UnsupportedEncodingException {
        
        String baseId;
        if (id.length() > 4) {
            baseId = id.substring(0, 4);
        } else {
            baseId = id;
        }
        this.id = baseId;
        
        tagAlterDiscard = false;
        fileAlterDiscard = checkDefaultFileAlterDiscard();
        readOnly = false;
        grouped = false;
        compressed = false;
        encrypted = false;
        unsynchronised = false;
        lengthIndicator = false;
        group = '\0';
        encrType = '\0';
        dataLength = -1;
        
        
        FrameAttributes att = FrameDictionary.getSingleton().frameTypes.get(baseId);
        int idPos = 4;
        if (att.langReq) {
            if (lang == null || lang.length() == 0) {
                lang = id.substring(idPos, idPos+3);
            }
            idPos += 3;
        }
        else
            lang = null;
        if (att.descReq) {
            if (desc == null || desc.length() == 0) {
                if (id.length() > idPos)
                    desc = id.substring(idPos);
                else
                    desc = "";
            }
        }
        else
            desc = null;
        
        boolean unicodeSupport;
        char first = id.charAt(0);
        if (first=='W' || first == 'P')
            unicodeSupport=false;
        else
            unicodeSupport=true;
        
        byte encodingB[];
        if (unicodeSupport || "WXXX".equals(baseId)) {
            encodingB = new byte[1];
            encodingB[0] = 0;
        }
        else
            encodingB = emptyByteArray;
        byte langB[];
        if (att.langReq) {
            langB = lang.getBytes(ENC_TYPES[0]);
        }
        else 
            langB = emptyByteArray;
        byte descB[];
        byte descEncoding = 0;
        if (att.descReq) {
            descB = encodeString(desc + "\0",encodingB);
            descEncoding = encodingB[0];
        }
        else
            descB = emptyByteArray;
        byte [] dataB;
        if (unicodeSupport) {
            dataB = encodeString(data,encodingB);
        }
        else
            dataB = data.getBytes(ENC_TYPES[0]);
        
        if (att.descReq && descEncoding != encodingB[0]) {
            descB = encodeString(desc + "\0",encodingB);
        }
        
        int totalSize = encodingB.length + langB.length 
                + descB.length + dataB.length;
        
        byte[] resultB = new byte[totalSize];
        int bytesCopied = 0;
        
        System.arraycopy(encodingB, 0, resultB, bytesCopied, encodingB.length);
        bytesCopied += encodingB.length;

        System.arraycopy(langB, 0, resultB, bytesCopied, langB.length);
        bytesCopied += langB.length;

        System.arraycopy(descB, 0, resultB, bytesCopied, descB.length);
        bytesCopied += descB.length;

        System.arraycopy(dataB, 0, resultB, bytesCopied, dataB.length);
        bytesCopied += dataB.length;

        setFrameData(resultB);
        
        createExtendedId();
        
    }
    
    // Attempt to encode in encoding 0, if not possibe use encoding 1
    static public byte [] encodeString(String source, byte[] encodingB) 
            throws UnsupportedEncodingException {
        byte [] result = source.getBytes(ENC_TYPES[encodingB[0]]);
        if (encodingB[0] == 0) {
            String checkResult = new String(result,ENC_TYPES[encodingB[0]]);
            if (!source.equals(checkResult)) {
                encodingB[0] = 1;
                result = source.getBytes(ENC_TYPES[encodingB[0]]);
            }
        }
        return result;
        
    }
    
    
    
    /**
     * Create an ID3v2Frame with the specified id, data, and flags set. It is
     * expected that the corresponing data for the flags that require extra data
     * is found in the data array in the standard place.
     *
     *@param id                the id for this frame
     *@param data              the data for this frame
     *@param tagAlterDiscard   the tag alter preservation flag
     *@param fileAlterDiscard  the file alter preservation flag
     *@param readOnly          the read only flag
     *@param grouped           the grouping identity flag
     *@param compressed        the compression flag
     *@param encrypted         the encryption flag
     *@param unsynchronised    the unsynchronisation flag
     *@param lengthIndicator   the data length indicator flag
     */
    public ID3v2Frame(String id, byte[] data, boolean tagAlterDiscard,
    boolean fileAlterDiscard, boolean readOnly,
    boolean grouped, boolean compressed,
    boolean encrypted, boolean unsynchronised,
    boolean lengthIndicator) throws UnsupportedEncodingException {
        
        this.id = id;
        this.tagAlterDiscard = tagAlterDiscard;
        this.fileAlterDiscard = fileAlterDiscard;
        this.readOnly = readOnly;
        this.grouped = grouped;
        this.compressed = compressed;
        this.encrypted = encrypted;
        this.unsynchronised = unsynchronised;
        this.lengthIndicator = lengthIndicator;
        
        group = '\0';
        encrType = '\0';
        dataLength = -1;
        
        parseData(data);
    }
    
    
    /**
     * Set the data for this frame. This does nothing if this frame is read only.
     *
     *@param newData  a byte array containing the new data
     */
    public void setFrameData(byte[] newData) {
        if (!readOnly) {
            frameData = newData;
        }
    }
    
    
    /**
     * Returns the data for this frame
     *
     *@return   the data for this frame
     */
    public byte[] getFrameData() {
        return frameData;
    }
    
    
    /**
     * Return the length of this frame in bytes, including the header.
     *
     *@return   the length of this frame
     */
    public int getFrameLength() {
        int length = frameData.length + FRAME_HEAD_SIZE;
        
//        if (grouped) {
//            length += 1;
//        }
//        if (encrypted) {
//            length += 1;
//        }
//        if (lengthIndicator) {
//            length += 4;
//        }
        
        return length;
    }
    
    
    /**
     * Returns a byte array representation of this frame that can be written to a
     * file. Includes the header and data.
     *
     *@return   a binary representation of this frame to be written to a file
     */
    public byte[] getFrameBytes() {
        int length = frameData.length;
        int bytesWritten = 0;
        byte[] flags = new byte[2];
//        byte[] flags = getFlagBytes();
//        byte[] extra = getExtraDataBytes();
        byte[] b;
        
//        if (grouped) {
//            length += 1;
//        }
//        if (encrypted) {
//            length += 1;
//        }
//        if (lengthIndicator) {
//            length += 4;
//        }
        
        b = new byte[length + FRAME_HEAD_SIZE];
        
        System.arraycopy(id.getBytes(), 0, b, 0, id.length());
        bytesWritten += id.length();
        System.arraycopy(BinaryParser.convertToBytes(length), 0, b,
        bytesWritten, 4);
        bytesWritten += 4;
        System.arraycopy(flags, 0, b, bytesWritten, flags.length);
        bytesWritten += flags.length;
//        System.arraycopy(extra, 0, b, bytesWritten, extra.length);
//        bytesWritten += extra.length;
        System.arraycopy(frameData, 0, b, bytesWritten, frameData.length);
        bytesWritten += frameData.length;
        
        return b;
    }
    
    
    /**
     * Returns true if the tag alter preservation bit has been set. If set then
     * the frame should be discarded if it is altered and the id is unknown.
     *
     *@return   true if the tag alter preservation bit has been set
     */
    public boolean getTagAlterDiscard() {
        return tagAlterDiscard;
    }
    
    
    /**
     * Returns true if the file alter preservation bit has been set. If set then
     * the frame should be discarded if the file is altered and the id is unknown.
     *
     *@return   true if the file alter preservation bit has been set
     */
    public boolean getFileAlterDiscard() {
        return fileAlterDiscard;
    }
    
    
    /**
     * Returns true if this frame is read only
     *
     *@return   true if this frame is read only
     */
    public boolean getReadOnly() {
        return readOnly;
    }
    
    
    /**
     * Returns true if this frame is a part of a group
     *
     *@return   true if this frame is a part of a group
     */
    public boolean getGrouped() {
        return grouped;
    }
    
    
    /**
     * Returns true if this frame is compressed
     *
     *@return   true if this frame is compressed
     */
    public boolean getCompressed() {
        return compressed;
    }
    
    
    /**
     * Returns true if this frame is encrypted
     *
     *@return   true if this frame is encrypted
     */
    public boolean getEncrypted() {
        return encrypted;
    }
    
    
    /**
     * Returns true if this frame is unsynchronised
     *
     *@return   true if this frame is unsynchronised
     */
    public boolean getUnsynchronised() {
        return unsynchronised;
    }
    
    
    /**
     * Returns true if this frame has a length indicator added
     *
     *@return   true if this frame has a length indicator added
     */
    public boolean getLengthIndicator() {
        return lengthIndicator;
    }
    
    
    /**
     * Returns the group identifier if added. Otherwise the null byte is returned.
     *
     *@return   the groupd identifier if added, null byte otherwise
     */
    public byte getGroup() {
        return group;
    }
    
    
    /**
     * If encrypted, this returns the encryption method byte. If it is not
     * encrypted, the null byte is returned.
     *
     *@return   the encryption method if set and the null byte if not
     */
    public byte getEncryptionType() {
        return encrType;
    }
    
    
    /**
     * If a length indicator has been added, the length of the data is returned.
     * Otherwise -1 is returned.
     *
     *@return   the length of the data if a length indicator is present or -1
     */
    public int getDataLength() {
        return dataLength;
    }
    
    
    /**
     * If possible, this method attempts to convert textual part of the data into
     * a string. If this frame does not contain textual information, an empty
     * string is returned.
     *
     *@return                          the textual portion of the data in this
     *      frame
     *@exception ID3v2FormatException  if an error occurs
     */
    public String getDataString() throws ID3v2FormatException {
        String str = new String();
        
        if (frameData.length > 1) {
            try {
                if ((id.charAt(0) == 'T') || id.equals(ID3v2Frames.OWNERSHIP_FRAME)
                // Peter Bennett Mar 2004
                // WXXX was treated wrongly
                //|| id.equals("WXXX")
                || id.equals("IPLS")) {
                    
                    str = getDecodedString(frameData, 0, 1);
                } else if (id.equals(ID3v2Frames.USER_DEFINED_URL)) {
                    int encType = (int) frameData[0];
                    byte[] desc = new byte[frameData.length];
                    boolean done = false;
                    int i = 0;
                    boolean isUnicode = (encType==1||encType==2);
                    while (!done && (i < desc.length)) {
                        if (isUnicode)
                            done = (frameData[i+1] == '\0'
                                    && frameData[i+2] == '\0');
                        else
                            done = (frameData[i+1] == '\0');
                        desc[i] = frameData[i+1];
                        i++;
                        if (isUnicode) {
                            desc[i] = frameData[i+1];
                            i++;
                        }
                    }
                    str = new String(desc, 0, i, ENC_TYPES[encType]);
                    
//                    str += "\n";
                    str = str.concat(
                    new String(frameData, i, frameData.length - i));
                } else if (id.charAt(0) == 'W') {
                    str = new String(frameData);
                } else if (id.equals(ID3v2Frames.UNSYNCHRONISED_LYRIC_TRANSCRIPTION)
                || id.equals(ID3v2Frames.COMMENTS)
                || id.equals(ID3v2Frames.TERMS_OF_USE)) {
                    
                    str = getDecodedString(frameData, 0, 4);
                }
            } catch (java.io.UnsupportedEncodingException e) {
                throw new ID3v2FormatException("Frame: " + id + " has "
                + " not specified a valid encoding type.");
            }
        }
        
        return str;
    }
    
    
    /**
     * Returns true if there is no data in the frame.
     *
     *@return   true if there is no data in the frame
     */
    public boolean isEmpty() {
        return frameData.length <= 1;
    }
    
    
    /**
     * Return a string representation of this object that contains all the
     * information contained within it.
     *
     *@return   a string representation of this object
     */
    public String toString() {
        String str = null;
        
        try {
            str = id + "\nExtended Id:    \t\t" + extendedId
            //+ "\nTagAlterDiscard:\t\t" + tagAlterDiscard
            //+ "\nFileAlterDiscard:\t\t" + fileAlterDiscard
            //+ "\nReadOnly:\t\t\t" + readOnly
            //+ "\nGrouped:\t\t\t" + grouped
            //+ "\nCompressed:\t\t\t" + compressed
            //+ "\nEncrypted:\t\t\t" + encrypted
            //+ "\nUnsynchronised:\t\t\t" + unsynchronised
            //+ "\nLengthIndicator:\t\t" + lengthIndicator
            + "\nData:\t\t\t\t" + getDataString().toString();
        } catch (Exception e) {
            // Do nothing, this is just toString, errors irrelevant
        }
        
        return str;
    }
    
    
    /**
     * A helper function for the getFrameBytes method that processes the info in
     * the frame and returns the 2 byte array of flags to be added to the header.
     *
     *@return   a value of type 'byte[]'
     */
    private byte[] getFlagBytes() {
        byte flags[] = {0x00, 0x00};
        
        if (tagAlterDiscard) {
            flags[0] = BinaryParser.setBit(flags[0], 6);
        }
        if (fileAlterDiscard) {
            flags[0] = BinaryParser.setBit(flags[0], 5);
        }
        if (readOnly) {
            flags[0] = BinaryParser.setBit(flags[0], 4);
        }
        if (grouped) {
            flags[1] = BinaryParser.setBit(flags[1], 6);
        }
        if (compressed) {
            flags[1] = BinaryParser.setBit(flags[1], 3);
        }
        if (encrypted) {
            flags[1] = BinaryParser.setBit(flags[1], 2);
        }
        if (unsynchronised) {
            flags[1] = BinaryParser.setBit(flags[1], 1);
        }
        if (lengthIndicator) {
            flags[1] = BinaryParser.setBit(flags[1], 0);
        }
        
        return flags;
    }
    
    
    /**
     * A helper function for the getFrameBytes function that returns an array of
     * all the data contained in any extra fields that may be present in this
     * frame. This includes the group, the encryption type, and the length
     * indicator. The length of the array returned is variable length.
     *
     *@return   an array of bytes containing the extra data fields in the frame
     */
    private byte[] getExtraDataBytes() {
        byte[] buf = new byte[MAX_EXTRA_DATA];
        byte[] ret;
        int bytesCopied = 0;
        
        if (grouped) {
            buf[bytesCopied] = group;
            bytesCopied += 1;
        }
        if (encrypted) {
            buf[bytesCopied] = encrType;
            bytesCopied += 1;
        }
        if (lengthIndicator) {
            byte[] num = BinaryParser.convertToBytes(dataLength);
            System.arraycopy(num, 0, buf, bytesCopied, num.length);
            bytesCopied += num.length;
        }
        
        ret = new byte[bytesCopied];
        System.arraycopy(buf, 0, ret, 0, bytesCopied);
        
        return ret;
    }
    
    
    /**
     * Converts the byte array into a string based on the type of encoding. One
     * byte in the array should contain the type of encoding. Where it is located
     * is specifed by the eIndex parameter. If an invalid type of encoding is
     * found, the empty string is returned.
     *
     *@param b                                         the array of bytes to
     *      convert/decode
     *@param eIndex                                    the index in the array
     *      where the encoding type resides
     *@param offset                                    where in the array to start
     *      the string
     *@return                                          the decoded string or an
     *      empty string if the encoding is wrong
     *@exception java.io.UnsupportedEncodingException  if an error occurs
     */
    private String getDecodedString(byte[] b, int eIndex, int offset)
    throws java.io.UnsupportedEncodingException {
        return getDecodedString( b,  eIndex,  offset, b.length);
        
    }
    
    private String getDecodedString(byte[] b, int eIndex, int offset, int endoffset)
    throws java.io.UnsupportedEncodingException {
        
        String str = new String();
        
        int encType = 0;
        if (eIndex>=0)
            encType = (int) b[eIndex];
        if ((encType >= 0) && (encType < ENC_TYPES.length)) {
            str = new String(b, offset, endoffset - offset,
            ENC_TYPES[encType]);
        }
        
        return str;
    }
    
    
    /**
     * Returns true if this frame should have the file alter preservation bit set
     * by default.
     *
     *@return   true if the file alter preservation should be set by default
     */
    private boolean checkDefaultFileAlterDiscard() {
        return id.equals(ID3v2Frames.AUDIO_SEEK_POINT_INDEX)
        || id.equals(ID3v2Frames.AUDIO_ENCRYPTION)
        || id.equals(ID3v2Frames.EVENT_TIMING_CODES)
        || id.equals(ID3v2Frames.EQUALISATION)
        || id.equals(ID3v2Frames.MPEG_LOCATION_LOOKUP_TABLE)
        || id.equals(ID3v2Frames.POSITION_SYNCHRONISATION_FRAME)
        || id.equals(ID3v2Frames.SEEK_FRAME)
        || id.equals(ID3v2Frames.SYNCHRONISED_LYRIC)
        || id.equals(ID3v2Frames.SYNCHRONISED_TEMPO_CODES)
        || id.equals(ID3v2Frames.RELATIVE_VOLUME_ADJUSTMENT)
        || id.equals(ID3v2Frames.ENCODED_BY)
        || id.equals(ID3v2Frames.LENGTH);
    }
    
    
    /**
     * Read the information from the flags array.
     *
     *@param flags                     the flags found in the frame header
     *@exception ID3v2FormatException  if an error occurs
     */
    private void parseFlags(byte[] flags) throws ID3v2FormatException {
        
        switch (majorVersion) {
            case 3:
                tagAlterDiscard = BinaryParser.bitSet(flags[0], 7);
                fileAlterDiscard = BinaryParser.bitSet(flags[0], 6);
                readOnly = BinaryParser.bitSet(flags[0], 5);
                grouped = BinaryParser.bitSet(flags[1], 5);
                compressed = BinaryParser.bitSet(flags[1], 7);
                encrypted = BinaryParser.bitSet(flags[1], 6);
                lengthIndicator = compressed;
                break;
            case 4:
                tagAlterDiscard = BinaryParser.bitSet(flags[0], 6);
                fileAlterDiscard = BinaryParser.bitSet(flags[0], 5);
                readOnly = BinaryParser.bitSet(flags[0], 4);
                grouped = BinaryParser.bitSet(flags[1], 6);
                compressed = BinaryParser.bitSet(flags[1], 3);
                encrypted = BinaryParser.bitSet(flags[1], 2);
                unsynchronised = BinaryParser.bitSet(flags[1], 1);
                lengthIndicator = BinaryParser.bitSet(flags[1], 0);
                break;
        }
        
        if (compressed && !lengthIndicator) {
            throw new ID3v2FormatException("Error parsing flags of frame: "
            + id + ".  Compressed bit set  " + "without data length bit set.");
        }
    }
    
    
    /**
     * Pulls out extra information inserted in the frame data depending on what
     * flags are set.
     *
     *@param data  the frame data
     */
    private void parseData(byte[] data) throws UnsupportedEncodingException {
        int bytesRead = 0;
        
        if (grouped) {
            group = data[bytesRead];
            bytesRead += 1;
        }
        if (encrypted) {
            encrType = data[bytesRead];
            bytesRead += 1;
        }
        if (lengthIndicator) {
            byte[] num = new byte[4];
            System.arraycopy(data, bytesRead, num, 0, num.length);
            dataLength = BinaryParser.convertToInt(num);
            bytesRead += num.length;
        }
        
        if (compressed) {
            try {
                 java.util.zip.Inflater decompresser = new java.util.zip.Inflater();
                 decompresser.setInput(data, bytesRead, data.length - bytesRead);
                 frameData = new byte[dataLength];
                 int resultLength = decompresser.inflate(frameData);
                 decompresser.end();
            }
             catch (Exception ex) {
                 ex.printStackTrace();
                 compressed=false;
             }
        }
        if (!compressed) {
            frameData = new byte[data.length - bytesRead];
            System.arraycopy(data, bytesRead, frameData, 0, frameData.length);
        }

        // tags that support encoding - validate correct encoding
        //TXXX WXXX IPLS USLT COMM APIC USER OWNE COMR 
        //[not supported]
        //GEOB SYLT 
        
        if (id.charAt(0) == 'T' // includes TXXX
            || "IPLS".equals(id) 
            || "USER".equals(id)
                                    ) {   
            if (frameData[0] > 1) {
                String frameString = getDecodedString( frameData,  0,  1, frameData.length);
                byte [] encodingB = new byte[1];
                byte [] recodedData = encodeString(frameString, encodingB);
                byte [] newFrameData = new byte[recodedData.length+1];
                newFrameData[0] = encodingB[0];
                System.arraycopy(recodedData, 0, newFrameData, 1, recodedData.length);
                frameData = newFrameData;
            }
        }
        if ("WXXX".equals(id)) {
            if (frameData[0] > 1) {
                boolean isUnicode = (frameData[0]==1||frameData[0]==2);
                int descEnd;
                if (isUnicode) {
                    for (descEnd = 1; descEnd < frameData.length; descEnd+=2) {
                        if (frameData[descEnd]==0 && frameData[descEnd+1]==0) {
                            descEnd+=2;
                            break;
                        }
                    }
                }
                else {
                    for (descEnd = 1; descEnd < frameData.length; descEnd++) {
                        if (frameData[descEnd]==0) {
                            descEnd++;
                            break;
                        }
                    }
                }
                String descString = getDecodedString( frameData,  0,  1, descEnd);
                byte [] encodingB = new byte[1];
                byte [] recodedData = encodeString(descString, encodingB);
                byte [] newFrameData = new byte[recodedData.length+1+frameData.length-descEnd];
                newFrameData[0] = encodingB[0];
                System.arraycopy(recodedData, 0, newFrameData, 1, recodedData.length);
                System.arraycopy(frameData, descEnd, newFrameData, recodedData.length+1, frameData.length-descEnd);
                frameData = newFrameData;
            }            
        }

        if ("USLT".equals(id)
                || "COMM".equals(id) ) {
            if (frameData[0] > 1) {
                String frameString = getDecodedString( frameData,  0,  4, frameData.length);
                byte [] encodingB = new byte[1];
                byte [] recodedData = encodeString(frameString, encodingB);
                byte [] newFrameData = new byte[recodedData.length+4];
                newFrameData[0] = encodingB[0];
                System.arraycopy(frameData, 1, newFrameData, 1, 3);
                System.arraycopy(recodedData, 0, newFrameData, 4, recodedData.length);
                frameData = newFrameData;
            }
            
        }

        if ("APIC".equals(id)) {
            
            if (frameData[0] > 1) {
                // correct the encoding by resetting the frame
                setPicture(picture);
            }
        }
        
// no support - "OWNE".equals(id)
// no support - "COMR".equals(id)
// no support - "GEOB".equals(id)
// no support - "SYLT".equals(id)        
        // Peter Bennett Feb 2004
        createExtendedId();
    }
    
    // Peter Bennett Feb 2004
    /**
     * Create extended id for cases where two occurrences of the same
     * id are allowed with different descriptors (e.g. COMM)
     */
    private void createExtendedId() {
        extendedId = id;
        boolean langReq=false;
        try {
            if ("COMM".equals(id)
            || "USLT".equals(id))
                langReq=true;
            
            if ("COMM".equals(id)
            || "UFID".equals(id)
            || "TXXX".equals(id)
            || "WXXX".equals(id)
            || "USLT".equals(id)
            || "SYLT".equals(id)
            || "POPM".equals(id)
            || "RVA2".equals(id)
            || "AENC".equals(id)
            || "EQU2".equals(id)
            || "PRIV".equals(id)) {
                int startoffset = 1;
                int encodingPos=0;
                if ("PRIV".equals(id)||"UFID".equals(id)||"POPM".equals(id)
                        ||"RVA2".equals(id)||"AENC".equals(id)) {
                    startoffset = 0;
                    encodingPos = -1;
                }
                if ("EQU2".equals(id)) {
                    startoffset = 1;
                    encodingPos = -1;
                }
                // for efficiency only allow up to 512 byte descriptions
                int endoffset = 512;
                if (frameData.length < endoffset)
                    endoffset = frameData.length;
//                for (endoffset=startoffset;endoffset<frameData.length && frameData[endoffset]!=0;endoffset++);
                
                String langStr;
                if (langReq) {
                    langStr =  getDecodedString(frameData, -1, startoffset, startoffset+3);
                    startoffset+=3;
                }
                else
                    langStr="";

                String dataString = getDecodedString(frameData, encodingPos, startoffset, endoffset);
                int startFrom=0;
//                if (langReq && dataString.length()>3)
//                    startFrom=3;
                int delimPos=dataString.indexOf(0,startFrom);
                if (delimPos>-1)
                    extendedId=extendedId+langStr+dataString.substring(0,delimPos);
            }
            if ("APIC".equals(id)) {
//                 <Header for 'Attached picture', ID: "APIC">
//                 Text encoding      $xx
//                 MIME type          <text string> $00
//                 Picture type       $xx
//                 Description        <text string according to encoding> $00 (00)
//                 Picture data       <binary data>
                if (picture==null) {
                    picture = new ID3v2Picture();
                    int ix;
                    for (ix=1;frameData[ix]!=0;ix++) {}
                    picture.mimeType = getDecodedString(frameData, -1, 1,ix);
                    picture.pictureType = frameData[ix+1];
                    ix=ix+2;
                    int ix2;
                    int picStart;
                    if (frameData[0] == 1 ||frameData[0] == 2) {
                        for (ix2=ix; frameData[ix2]!=0 || frameData[ix2+1]!=0; ix2+=2) {}
                        picStart = ix2+2;
                    }
                    else {
                        for (ix2=ix;frameData[ix2]!=0;ix2++) {}
                        picStart = ix2+1;
                    }
                    picture.Description = getDecodedString(frameData, 0, ix, ix2);
                    picture.pictureData = new byte[frameData.length - picStart];
//                    ix = 0;
                    System.arraycopy(frameData, picStart, picture.pictureData, 0, frameData.length - picStart);
//                    for (;ix2<frameData.length;ix2++,ix++) {
//                        picture.pictureData[ix]=frameData[ix2];
//                    }
                }
                String hexPictureType=Integer.toHexString(picture.pictureType);
                if (picture.pictureType < 16)
                    hexPictureType="0"+hexPictureType;
                extendedId = extendedId+hexPictureType+picture.Description;
            }
        }
        catch (java.io.UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        // note these tags may have dups but do not
        // have an extended id
        // WCOM
        // WOAR
        // LINK
        // USER
        // GEOB
        // COMR
        // SIGN
    }
    
    // Peter Bennett Feb 2004
    // Get Picture data
    /**
     * Get Picture data for APIC frame
     */
    public ID3v2Picture getPicture() {
        return picture;
    }
    
    
    public void setPicture(ID3v2Picture pic) throws UnsupportedEncodingException {
        // Text encoding      $xx
        // MIME type          <text string> $00
        // Picture type       $xx
        // Description        <text string according to encoding> $00 (00)
        // Picture data       <binary data>

//        arraycopy(Object src,
//                             int srcPos,
//                             Object dest,
//                             int destPos,
//                             int length)        
        
        
        byte [] encodingB = new byte[1];
        byte [] descBytes = encodeString(pic.Description + "\0", encodingB);
        byte [] data = new byte[1+pic.mimeType.length()+2+descBytes.length+pic.pictureData.length];
        data[0]=encodingB[0];
        int ix = 1;
        int iy;
        byte []mimeTypeBytes = pic.mimeType.getBytes();
        System.arraycopy(mimeTypeBytes, 0, data, ix, mimeTypeBytes.length);
        ix+=mimeTypeBytes.length;
//        for (iy=0;iy<mimeTypeBytes.length;iy++)
//            data[ix++]=mimeTypeBytes[iy];
        data[ix++]=0;
        data[ix++]=pic.pictureType;
        // mimeTypeBytes = pic.Description.getBytes();
        System.arraycopy(descBytes, 0, data, ix, descBytes.length);
        ix+=descBytes.length;
//        for (iy=0;iy<descBytes.length;iy++)
//            data[ix++]=descBytes[iy];
        System.arraycopy(pic.pictureData, 0, data, ix, pic.pictureData.length);
//        for (iy=0;iy<pic.pictureData.length;iy++)
//            data[ix++]=pic.pictureData[iy];

        frameData = data;
        picture = pic;
    }
    
    
    
    // Peter Bennett Feb 2004
    /**
     * Get extended id for cases where two occurrences of the same
     * id are allowed with different descriptors (e.g. COMM)
     */
    public String getExtendedId() {
        return extendedId;
    }
    
    
}

