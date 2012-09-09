package pgbennett.id3;

import java.io.*;
import java.util.Arrays;
import java.util.regex.*;

/*
   Copyright (C) 2001,2002 Jonathan Hilliker
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
 * Description: This class is a container of all the tags and data that can be
 * extracted from the mp3 specified by the file. If there are no id3 tags
 * present, tags will be created by using mutators and saving the data.
 * <dl>
 *   <dt> <b>Version History</b> </dt>
 *   <dt> 1.8.1 - <small>2002.1023 by gruni</small> </dt>
 *   <dd> -Made Sourcecode compliant to the Sun CodingConventions</dd>
 *   <dt> 1.8 - <small>2002.0131 by helliker</small> </dt>
 *   <dd> Added methods to get tag sizes.</dd>
 *   <dt> 1.7 - <small>2002.0127 by helliker</small> </dt>
 *   <dd> -Added methods to access additional MPEG data.</dd>
 *   <dd> -Added methods to parse track data.</dd>
 *   <dd> -Comparisons with compareTo uses pathnames now.</dd>
 *   <dt> 1.6 - <small>2002.0124 by helliker</small> </dt>
 *   <dd> -Avoids a divide by zero possibility.</dd>
 *   <dt> 1.5 - <small>2002.0121 by helliker</small> </dt>
 *   <dd> -Added support for VBR files.</dd>
 *   <dt> 1.4 - <small>2002.0120 by helliker</small>
 *   <dd> Change to ID3v2Tag ctor</dd>
 *   <dt> 1.3 - <small>2001.1028 by helliker</small> </dt>
 *   <dd> -Writes id3v2 tags before id3v1 tags. This fixes a bug where if the
 *   filesize is changed when the id3v2 tag is written out then the id3v1 tag
 *   will not be at the end of the file and won't be recognized.</dd>
 *   <dt> 1.2 - <small>2001.1019 by helliker</small> </dt>
 *   <dd> All set for release.</dd>
 * </dl>
 *
 *
 *@author    Jonathan Hilliker
 *@version   1.8.1
 */

public class MP3File implements Comparable {
    
    /**
     * Write ID3v1 and ID3v2 tags whether or not they exist. Precedence for
     * reading will be given to id3v2 tags.
     */
    public static final int BOTH_TAGS = 0;
    
    /**
     * Write and read from ID3v2 tags only. An ID3v2 tag will be created if an
     * attempt is made to write.
     */
    public static final int ID3V2_ONLY = 1;
    
    /**
     * Write and read from ID3v1 tags only. An ID3v1 tag will be created if an
     * attempt is made to write.
     */
    public static final int ID3V1_ONLY = 2;
    
    /**
     * Do not write or read from any id3 tags.
     */
    public static final int NO_TAGS = 3;
    
    /**
     * Only write and read tags that already exist. Existing tags can be updated
     * but new tags will not be created.
     */
    public static final int EXISTING_TAGS_ONLY = 4;
    
    /**
     * The ID3v1 Tag position ???
     */
    private final int ID3V1 = 5;
    /**
     * Another int for ID3v2 ???
     */
    private final int ID3V2 = 6;
    
    /**
     * The ID3v1 Tag
     */
    private ID3v1Tag id3v1 = null;
    /**
     * The ID3v2 Tag
     */
    private ID3v2Tag id3v2 = null;
    /**
     * The MPEG-Audio frameheader
     */
    private MPEGAudioFrameHeader head = null;
    /**
     * The MP3 File
     */
    private File mp3 = null;
    /**
     * The Tag Type
     */
    private int tagType = 0;
    
    int avgBitRate;
    
    
    /**
     * Create an MP3File object that reads and writes to the file with the
     * filename fn. This assumes that you only want to read and write id3 tags
     * that already exist in the file.
     *
     *@param fn                          the filename of the mp3
     *@exception FileNotFoundException   if an error occurs
     *@exception NoMPEGFramesException   if an error occurs
     *@exception IOException             if an error occurs
     *@exception ID3v2FormatException    if an error occurs
     *@exception CorruptHeaderException  if an error occurs
     */
    public MP3File(String fn) throws FileNotFoundException,
    NoMPEGFramesException, IOException, ID3Exception {
        
        this(new File(fn));
    }
    
    
    /**
     * Create an MP3File object that reads and writes to the specified file. This
     * assumes that you only want to read and write id3 tags tha already exist in
     * the file.
     *
     *@param mp3                         the file of the mp3
     *@exception FileNotFoundException   if an error occurs
     *@exception NoMPEGFramesException   if an error occurs
     *@exception IOException             if an error occurs
     *@exception ID3v2FormatException    if an error occurs
     *@exception CorruptHeaderException  if an error occurs
     */
    public MP3File(File mp3) throws FileNotFoundException,
    NoMPEGFramesException, IOException, ID3Exception {
    
        
        this(mp3, EXISTING_TAGS_ONLY);
    }
    
    
    /**
     * Create an MP3File object that reads and writes to the file with the
     * filename fn. The id3 tags that are read from and written are dependant upon
     * the tagType argument. This could be either: BOTH_TAGS, ID3V2_ONLY,
     * ID3V1_ONLY, NO_TAGS, or EXISTING_TAGS_ONLY.
     *
     *@param fn                          the filename of the mp3
     *@param tagType                     determines what type of tags to write and
     *      read from
     *@exception FileNotFoundException   if an error occurs
     *@exception NoMPEGFramesException   if an error occurs
     *@exception IOException             if an error occurs
     *@exception ID3v2FormatException    if an error occurs
     *@exception CorruptHeaderException  if an error occurs
     */
    public MP3File(String fn, int tagType) throws FileNotFoundException,
    NoMPEGFramesException, IOException, ID3Exception {
        
        this(new File(fn), tagType);
    }
    

    public MP3File() {
    }
    
    /**
     * Create and MP3File object that reads and writes to the specified file. The
     * id3 tags that are read from and written are dependant upon the tagType
     * argument. This could be either: BOTH_TAGS, ID3V2_ONLY, ID3V1_ONLY, NO_TAGS,
     * or EXISTING_TAGS_ONLY.
     *
     *@param mp3                         the file of the mp3
     *@param tagType                     determines what type of tags to write and
     *      read from
     *@exception FileNotFoundException   if an error occurs
     *@exception NoMPEGFramesException   if an error occurs
     *@exception IOException             if an error occurs
     *@exception ID3v2FormatException    if an error occurs
     *@exception CorruptHeaderException  if an error occurs
     */
    public MP3File(File mp3, int tagType) throws FileNotFoundException,
    NoMPEGFramesException, IOException, ID3Exception {
        init(mp3, tagType);
    }

    public void init(File mp3, int tagType) throws FileNotFoundException,
    NoMPEGFramesException, IOException, ID3v2FormatException,
    CorruptHeaderException, ID3Exception {
        init( mp3,  tagType,  0);
    }
    
    public void init(File mp3, int tagType, int offset) throws FileNotFoundException,
    NoMPEGFramesException, IOException, ID3v2FormatException,
    CorruptHeaderException, ID3Exception {
        
        this.mp3 = mp3;
        this.tagType = tagType;
        
        // Start looking at the beginning of the file instead of the end of
        // the id3v2 tag because of padding
        head = new MPEGAudioFrameHeader(mp3, offset);
        if (tagType == BOTH_TAGS || tagType == ID3V1_ONLY || tagType == EXISTING_TAGS_ONLY)
            id3v1 = new ID3v1Tag(mp3);
        if (tagType == BOTH_TAGS || tagType == ID3V2_ONLY || tagType == EXISTING_TAGS_ONLY) {
            try {
                id3v2 = new ID3v2Tag(mp3, head.getLocation());
            }
            catch (ID3Exception ex) {
                id3v2 = new ID3v2Tag(mp3, head.getLocation(), true);
                throw ex;
            }
        }
    }
    
    public long getMpegLocation() {
        return head.getLocation();
    }
    
    
    /**
     * Returns the length (in seconds) of the playing time of this mp3. This will
     * not return an accurate value for VBR files.
     *
     *@return   the playing time (in seconds) of this mp3
     */
    public long getPlayingTime() {
        long time = 0;
        
        if (head.isVBR()) {
            time = head.getVBRPlayingTime();
        } else {
            long datasize = mp3.length() - id3v2.getSize()
            - id3v1.getSize();
//            long bps = head.getBitRate() * 1000;
            long bps = getBitRate() * 1000;
            
            // Avoid divide by zero error
            if (bps == 0) {
                time = 0;
            } else {
                time = datasize * 8 / bps;
            }
        }
        
        return time;
    }


    /**
     * Returns the length (in milliseconds) of the playing time of this mp3. This will
     * not return an accurate value for VBR files.
     *
     *@return   the playing time (in mseconds) of this mp3
     */
    public long getPlayingTimeMs() {
        long time = 0;
        
        if (head.isVBR()) {
            time = head.getVBRPlayingTime() * 1000;
        } else {
            long datasize = mp3.length() - id3v2.getSize()
            - id3v1.getSize();
            long kbps = getBitRate();
            
            // Avoid divide by zero error
            if (kbps == 0) {
                time = 0;
            } else {
                time = datasize * 8 / kbps;
            }
        }
        
        return time;
    }
    
    
    /**
     * Return a formatted version of the getPlayingTime method. The string will be
     * formated "m:ss" where 'm' is minutes and 'ss' is seconds.
     *
     *@return   a formatted version of the getPlayingTime method
     */
    public String getPlayingTimeString() {
        String str;
        long time = getPlayingTime();
        long mins = time / 60;
        long secs = Math.round((((double) time / 60) - (long) (time / 60)) * 60);
        
        str = mins + ":";
        
        if (secs < 10) {
            str += "0" + secs;
        } else {
            str += "" + secs;
        }
        
        return str;
    }
    
    
    /**
     * Return the absolute path of this MP3File.
     *
     *@return   the absolute path of this MP3File
     */
    public File getMp3File() {
        return mp3;
    }

    
    /**
     * Return the absolute path of this MP3File.
     *
     *@return   the absolute path of this MP3File
     */
    public String getPath() {
        return mp3.getAbsolutePath();
    }
    
    
    /**
     * Returns the parent directory of this MP3File.
     *
     *@return   the parent directory of this MP3File
     */
    public String getParent() {
        return mp3.getParent();
    }
    
    
    /**
     * Returns the filename of this MP3File.
     *
     *@return   the filename of this MP3File
     */
    public String getFileName() {
        return mp3.getName();
    }
    
    
    /**
     * Return the filesize of this MP3File (in bytes).
     *
     *@return   the filesize of this MP3File (in bytes)
     */
    public long getFileSize() {
        return mp3.length();
    }
    
    
    /**
     * Returns true if an id3v2 tag currently exists.
     *
     *@return   true if an id3v2 tag currently exists
     */
    public boolean id3v2Exists() {
        return id3v2.tagExists();
    }
    
    
    /**
     * Returns true if an id3v1 tag currently exists.
     *
     *@return   true if an id3v1 tag currently exists
     */
    public boolean id3v1Exists() {
        return id3v1.tagExists();
    }
    
    
    /**
     * Returns true if this file is an mp3. This means simply that an
     * MPEGAudioFrameHeader was found and the layer is 3.
     *
     *@return   true if this file is an mp3
     */
    public boolean isMP3() {
        return head.isMP3();
    }
    
    
    /**
     * Returns true if this mp3 is a variable bitrate file.
     *
     *@return   true if this mp3 is a variable bitrate file.
     */
    public boolean isVBR() {
        return head.isVBR();
    }
    
    
    /**
     * Returns the bitrate of this mp3 in kbps. If the file is a VBR file then the
     * average bitrate is returned.
     *
     *@return   the bitrate of this mp3 in kbps
     */
    final int numSamples=20;
    public int getBitRate() {
//        return head.getBitRate();
//    }
    
        if (avgBitRate != 0)
            return avgBitRate;

        int headBitRate = head.getBitRate();
        // if xing header is present that should give you the bitrate
        if (head.isVBR())
            return headBitRate;
        
        long datasize = mp3.length() - getTagSize();
        long startLocation = head.getLocation();
        long increment = datasize / numSamples;
        int i;
		long offset = startLocation;
        float bitrate = 0;
        int sameRate=0;
        boolean vbr=false;
        try {
            for (i=1;i<numSamples && sameRate < 2;i++) {
                offset += increment;
                MPEGAudioFrameHeader frameHead = new MPEGAudioFrameHeader(mp3, offset);
                if (headBitRate == frameHead.getBitRate() && !vbr)
                    sameRate++;
                else
                    vbr=true;
                
                bitrate += frameHead.getBitRate();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
            avgBitRate = head.getBitRate();
            return avgBitRate;
        }
        if (!vbr)
            return headBitRate;
        avgBitRate = (int)(bitrate / (numSamples-1));
        return avgBitRate;
    }

    
    
    
    /**
     * Returns the sample rate of this mp3 in Hz.
     *
     *@return   the sample reate of this mp3 in Hz
     */
    public int getSampleRate() {
        return head.getSampleRate();
    }
    
    
    /**
     * Returns the emphasis of this mp3.
     *
     *@return   the emphasis of this mp3
     */
    public String getMPEGEmphasis() {
        return head.getEmphasis();
    }
    
    
    /**
     * Returns a string specifying the layer of the mpeg. Ex: Layer III
     *
     *@return   a string specifying the layer of the mpeg
     */
    public String getMPEGLayer() {
        return head.getLayer();
    }
    
    
    /**
     * Returns a string specifying the version of the mpeg. This can either be
     * 1.0, 2.0, or 2.5.
     *
     *@return   a string specifying the version of the mpeg
     */
    public String getMPEGVersion() {
        return head.getVersion();
    }
    
    
    /**
     * Return the channel mode of the mpeg. Ex: Stereo
     *
     *@return   the channel mode of the mpeg
     */
    public String getMPEGChannelMode() {
        return head.getChannelMode();
    }
    
    
    /**
     * Returns true if this mpeg is copyrighted.
     *
     *@return   true if this mpeg is copyrighted
     */
    public boolean isMPEGCopyrighted() {
        return head.isCopyrighted();
    }
    
    
    /**
     * Returns true if this mpeg is the original.
     *
     *@return   true if this mpeg is the original
     */
    public boolean isMPEGOriginal() {
        return head.isOriginal();
    }
    
    
    /**
     * Returns true if this mpeg is protected by CRC.
     *
     *@return   true if this mpeg is protected by CRC
     */
    public boolean isMPEGProtected() {
        return head.isProtected();
    }
    
    
    /**
     * Returns true if the private bit is set in this mpeg.
     *
     *@return   true if the private bit is set in this mpeg
     */
    public boolean isMPEGPrivate() {
        return head.privateBitSet();
    }
    
    
    /**
     * Removes id3 tags from the file. The argument specifies which tags to
     * remove. This can either be BOTH_TAGS, ID3V1_ONLY, ID3V2_ONLY, or
     * EXISTING_TAGS_ONLY.
     *
     *@param type                       specifies what tag(s) to remove
     *@exception FileNotFoundException  if an error occurs
     *@exception IOException            if an error occurs
     */
    public void removeTags(int type)
    throws FileNotFoundException, IOException {
        
        if (allow(ID3V1, type)) {
            id3v1.removeTag();
        }
        if (allow(ID3V2, type)) {
            id3v2.removeTag();
        }
    }
    
    
    /**
     * Writes the current state of the id3 tags to the file. What tags are written
     * depends upon the tagType passed to the constructor.
     *
     *@exception FileNotFoundException  if an error occurs
     *@exception IOException            if an error occurs
     */
    public void writeTags()
    throws FileNotFoundException, IOException {
        
        // Write out id3v2 first because if the filesize is changed when an
        // id3v2 is written then the id3v1 may be moved away from the end
        // of the file which would cause it to not be recognized.
        if (allow(ID3V2)) {
            id3v2.writeTag();
        }
        if (allow(ID3V1)) {
            id3v1.writeTag();
        }
    }
    
    
    /**
     * Set the title of this mp3.
     *
     *@param title  the title of the mp3
     */
    public void setTitle(String title) {
        if (allow(ID3V1)) {
            id3v1.setTitle(title);
        }
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.TITLE, title);
        }
    }
    
    
    /**
     * Set the album of this mp3.
     *
     *@param album  the album of the mp3
     */
    public void setAlbum(String album) {
        if (allow(ID3V1)) {
            id3v1.setAlbum(album);
        }
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.ALBUM, album);
        }
    }
    
    
    /**
     * Set the artist of this mp3.
     *
     *@param artist  the artist of the mp3
     */
    public void setArtist(String artist) {
        if (allow(ID3V1)) {
            id3v1.setArtist(artist);
        }
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.LEAD_PERFORMERS, artist);
        }
    }
    
    
    /**
     * Add a comment to this mp3.
     *
     *@param comment  a comment to add to the mp3
     */
    public void setComment(String comment) {
        if (allow(ID3V1)) {
            id3v1.setComment(comment);
        }
        if (allow(ID3V2)) {
            id3v2.setCommentFrame("", comment);
        }
    }
    
    
    /**
     * Set the genre of this mp3.
     *
     *@param genre  the genre of the mp3
     */
    public void setGenre(String genre) {
        if (allow(ID3V1)) {
            id3v1.setGenreString(genre);
        }
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.CONTENT_TYPE, genre);
        }
    }
    
    
    /**
     * Set the year of this mp3.
     *
     *@param year  of the mp3
     */
    public void setYear(String year) {
        if (allow(ID3V1)) {
            id3v1.setYear(year);
        }
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.YEAR, year);
        }
    }
    
    
    /**
     * Set the track number of this mp3.
     *
     *@param track  the track number of this mp3
     */
    public void setTrack(int track) {
        if (allow(ID3V1)) {
            id3v1.setTrack(track);
        }
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.TRACK_NUMBER,
            String.valueOf(track));
        }
    }
    
    
    /**
     * Set the track number with a String.
     *
     *@param track                      the track number of this mp3
     *@exception NumberFormatException  if the String can't be parsed as an
     *      integer
     */
    public void setTrack(String track) throws NumberFormatException {
        if (allow(ID3V1)) {
            id3v1.setTrack(Integer.parseInt(track));
        }
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.TRACK_NUMBER,
            track);
        }
    }
    
    
    /**
     * Set the composer of this mp3 (id3v2 only).
     *
     *@param composer  the composer of this mp3
     */
    public void setComposer(String composer) {
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.COMPOSER, composer);
        }
    }
    
    
    /**
     * Set the original artist of this mp3 (id3v2 only).
     *
     *@param artist  the original artist of this mp3
     */
    public void setOriginalArtist(String artist) {
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.ORIGINAL_ARTIST, artist);
        }
    }
    
    
    /**
     * Add some copyright information to this mp3 (id3v2 only).
     *
     *@param copyright  copyright information related to this mp3
     */
    public void setCopyrightInfo(String copyright) {
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.COPYRIGHT_MESSAGE, copyright);
        }
    }
    
    
    /**
     * Add a link to this mp3 (id3v2 only). This includes a description of the url
     * and the url itself.
     *
     *@param desc  a description of the url
     *@param url   the url itself
     */
    public void setUserDefinedURL(String desc, String url) {
        if (allow(ID3V2)) {
            id3v2.setUserDefinedURLFrame(desc, url);
        }
    }
    
    
    /**
     * Add a field of miscellaneous text (id3v2 only). This includes a description
     * of the text and the text itself.
     *
     *@param desc  a description of the text
     *@param text  the text itself
     */
    public void setUserDefinedText(String desc, String text) {
        if (allow(ID3V2)) {
            id3v2.setUserDefinedTextFrame(desc, text);
        }
    }
    
    
    /**
     * Set who encoded the mp3 (id3v2 only).
     *
     *@param encBy  who encoded the mp3
     */
    public void setEncodedBy(String encBy) {
        if (allow(ID3V2)) {
            id3v2.setTextFrame(ID3v2Frames.ENCODED_BY, encBy);
        }
    }
    
    
    /**
     * Set the text of the text frame specified by the id (id3v2 only). The id
     * should be one of the static strings specifed in ID3v2Frames class. All id's
     * that begin with 'T' (excluding "TXXX") are considered text frames.
     *
     *@param id    the id of the frame to set the data for
     *@param data  the data to set
     */
    public void setTextFrame(String id, String data) {
        if (allow(ID3V2)) {
            id3v2.setTextFrame(id, data);
        }
    }
    
    
    /**
     * Set the data of the frame specified by the id (id3v2 only). The id should
     * be one of the static strings specified in ID3v2Frames class.
     *
     *@param id    the id of the frame to set the data for
     *@param data  the data to set
     */
    public void setFrameData(String id, byte[] data) throws UnsupportedEncodingException {
        if (allow(ID3V2)) {
            id3v2.updateFrameData(id, data);
        }
    }
    
    
    /**
     * Returns the artist of the mp3 if set and the empty string if not.
     *
     *@return                          the artist of the mp3
     *@exception ID3v2FormatException  if the data of the field is incorrect
     */
    public String getArtist() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.LEAD_PERFORMERS);
        }
        if (allow(ID3V1) && str.length()==0) {
            str = id3v1.getArtist();
        }
        
        return str;
    }
    
    
    /**
     * Returns the album of the mp3 if set and the empty string if not.
     *
     *@return                          the album of the mp3
     *@exception ID3v2FormatException  if the data of the field is incorrect
     */
    public String getAlbum() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.ALBUM);
        }
        if (allow(ID3V1) && str.length()==0) {
            str = id3v1.getAlbum();
        }
        
        return str;
    }
    
    
    /**
     * Returns the comment field of this mp3 if set and the empty string if not.
     *
     *@return                          the comment field of this mp3
     *@exception ID3v2FormatException  if the data of the field is incorrect
     */
    public String getComment() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.COMMENTS);
            // Feb 2004 Peter Bennett
            // Allow for english language code
            if (str.length()==0)
                str = id3v2.getFrameDataString(ID3v2Frames.COMMENTS+"eng");
        }
        if (allow(ID3V1) && str.length()==0) {
            str = id3v1.getComment();
        }
        
        return str;
    }
    
    
    /**
     * Returns the genre of this mp3 if set and the empty string if not.
     *
     *@return                          the genre of this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getGenre() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.CONTENT_TYPE);
            // Feb 2004 Peter Bennett
            // Cater for genre of the form (nnn)
            // where nnn is an id3 genre number
            if (str.startsWith("(") && str.endsWith(")") && str.length() <=5) {
                try {
                    int i=Integer.parseInt(str.substring(1,str.length()-1));
                    if (i<=255)
                        str=NullsoftID3GenreTable.getGenre(i);
                }
                catch (NumberFormatException ex) {
                }
            }
        }
        if (allow(ID3V1) && str.length()==0) {
            str = id3v1.getGenreString();
        }
        
        return str;
    }
    
    
    /**
     * Returns the title of this mp3 if set and the empty string if not.
     *
     *@return                          the title of this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getTitle() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.TITLE);
        }
        if (allow(ID3V1) && str.length()==0) {
            str = id3v1.getTitle();
        }
        
        return str;
    }
    
    
    /**
     * Returns the track exactly as the track field of the id3 tag reads.
     *
     *@return                          the track of this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getTrackString() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.TRACK_NUMBER);
        }
        if (allow(ID3V1) && str.length()==0) {
            str = String.valueOf(id3v1.getTrack());
        }
        
        return str;
    }
    
    
    /**
     * Returns an integer value of the track number. If a track field of an id3v2
     * tag has a '/' the number before the '/' will be returned. Returns -1 if
     * there is an error parsing the track field.
     *
     *@return                          an int value of the track number
     *@exception ID3v2FormatException  if an error occurs
     */
    public int getTrack() throws ID3v2FormatException {
        String str = getTrackString();
        int track = -1;
        int loc = str.indexOf("/");
        
        try {
            if (loc != -1) {
                str = str.substring(0, loc);
            }
            
            track = Integer.parseInt(str);
        } catch (NumberFormatException e) {
            // Do nothing
        }
        
        return track;
    }
    
    
    /**
     * Although not a standard, sometimes track numbers are expressed as "x/y"
     * where x is the track number and y is the total number of tracks on an
     * album. This method will attempt to return the y value. Returns -1 if there
     * is an error parsing the field or if there is no id3v2 tag.
     *
     *@return                          the total number of tracks of the related
     *      album
     *@exception ID3v2FormatException  if an error occurs
     */
    public int getNumTracks() throws ID3v2FormatException {
        String str = getTrackString();
        int track = -1;
        int loc = str.indexOf("/");
        
        try {
            if (loc != -1) {
                str = str.substring(loc + 1, str.length());
                track = Integer.parseInt(str);
            }
        } catch (NumberFormatException e) {
            // Do nothing
        }
        
        return track;
    }
    
    
    /**
     * Returns the year of this mp3 if set and the empty string if not.
     *
     *@return                          the year of this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getYear() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.YEAR);
        }
        if (allow(ID3V1) && str.length()==0) {
            str = id3v1.getYear();
        }
        
        return str;
    }
    
    
    /**
     * Returns the composer of this mp3 if set and the empty string if not (id3v2
     * only).
     *
     *@return                          the composer of this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getComposer() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.COMPOSER);
        }
        
        return str;
    }
    
    
    /**
     * Returns the original artist of this mp3 if set and the empty string if not
     * (id3v2 only).
     *
     *@return                          the original artist of this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getOriginalArtist() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.ORIGINAL_ARTIST);
        }
        
        return str;
    }
    
    
    /**
     * Returns the copyright info of this mp3 if set and the empty string if not
     * (id3v2 only).
     *
     *@return                          the copyright info of this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getCopyrightInfo() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.COPYRIGHT_MESSAGE);
        }
        
        return str;
    }
    
    
    /**
     * Returns the user defined url of this mp3 if set and the empty string if not
     * (id3v2 only).
     *
     *@return                          the user defined url of this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getUserDefinedURL() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.USER_DEFINED_URL);
        }
        
        return str;
    }
    
    
    /**
     * Returns who encoded this mp3 if set and the empty string if not (id3v2
     * only).
     *
     *@return                          who encoded this mp3
     *@exception ID3v2FormatException  if the data of this field is incorrect
     */
    public String getEncodedBy() throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(ID3v2Frames.ENCODED_BY);
        }
        
        return str;
    }
    
    
    /**
     * Returns the textual information contained in the frame specifed by the id.
     * If the frame does not contain any textual information or does not exist,
     * then the empty string is returned (id3v2 only). The id should be one of the
     * static strings defined in the ID3v2Frames class.
     *
     *@param id                        the id of the frame to get data from
     *@return                          the textual information of the frame
     *@exception ID3v2FormatException  if the data of the frame is incorrect
     */
    public String getFrameDataString(String id) throws ID3v2FormatException {
        String str = new String();
        
        if (allow(ID3V2)) {
            str = id3v2.getFrameDataString(id);
        }
        
        return str;
    }
    
    
    /**
     * Returns the data contained in the frame specified by the id (id3v2 only) .
     * If the frame does not exist, a zero length array will be returned. The id
     * should be one of the static strings defined in the ID3v2Frames class.
     *
     *@param id  the id of the frame to get data from
     *@return    the data contained in the frame
     */
    public byte[] getFrameDataBytes(String id) {
        byte[] b = new byte[0];
        
        if (allow(ID3V2)) {
            b = id3v2.getFrameData(id);
        }
        
        return b;
    }
    
    
    /**
     * Returns the currently set tagging type.
     *
     *@return   the current tagging type
     */
    public int getTaggingType() {
        return tagType;
    }
    
    
    /**
     * Set the tagging type. This determines what type of id3 tags are
     * read/written. This should be one of the constants defined by this class:
     * BOTH_TAGS, ID3V1_ONLY, ID3V2_ONLY, EXISTING_TAGS_ONLY, NO_TAGS
     *
     *@param newType  the new tagging type
     */
    public void setTaggingType(int newType) {
        tagType = newType;
    }
    
    
    /**
     * Return the current size of the tag(s) in bytes. What size is returned
     * depends on the type of tagging specified in the constructor.
     *
     *@return   the size of the tag(s) in bytes
     */
    public int getTagSize() {
        return getTagSize(tagType);
    }
    
    
    /**
     * Returns the current size of the tag(s) in bytes. What size is returned
     * depends on the type of tagging specified. The possible values for the type
     * is: BOTH_TAGS, ID3V1_ONLY, ID3V2_ONLY, EXISTING_TAGS_ONLY, NO_TAGS. If
     * BOTH_TAGS or EXISTING_TAGS_ONLY is used and both tags exist, then the size
     * of the two tags added will be returned.
     *
     *@param type  the tagging type
     *@return      the size of the tag(s) in bytes
     */
    public int getTagSize(int type) {
        int size = 0;
        
        if (allow(ID3V1, type)) {
            size += id3v1.getSize();
        }
        if (allow(ID3V2, type)) {
            // Extra check because size will return at least 10
            // because of header, even if the tag doesn't exist
            if (id3v2.tagExists()) {
                size += id3v2.getSize();
            }
        }
        
        return size;
    }
    
    
    /**
     * Checks whether it is ok to read or write from the tag version specified
     * based on the tagType passed to the constructor. The tagVersion parameter
     * should be either ID3V1 or ID3V2.
     *
     *@param tagVersion  the id3 version to check
     *@return            true if it is ok to proceed with the read/write
     */
    private boolean allow(int tagVersion) {
        return this.allow(tagVersion, tagType);
    }
    
    
    /**
     * Checks whether it is ok to read or write from the tag version specified
     * based on the tagType passed to the method. The tagVersion parameter should
     * be either ID3V1 or ID3V2. The type parameter should be either BOTH_TAGS,
     * ID3V1_ONLY, ID3V2_ONLY, NO_TAGS, or EXISTING_TAGS_ONLY.
     *
     *@param tagVersion  the id3 version to check
     *@param type        specifies what conditions the tags are allowed to proceed
     *@return            true if it is ok to proceed with the read/write
     */
    private boolean allow(int tagVersion, int type) {
        boolean retval = false;
        
        if (tagVersion == ID3V1) {
            retval = ((type == EXISTING_TAGS_ONLY) && id3v1.tagExists())
            || (type == ID3V1_ONLY) || (type == BOTH_TAGS);
        } else if (tagVersion == ID3V2) {
            retval = ((type == EXISTING_TAGS_ONLY) && id3v2.tagExists())
            || (type == ID3V2_ONLY) || (type == BOTH_TAGS);
        }
        
        return retval;
    }
    
    
    /**
     * Return a string representation of this object. This includes all the
     * information contained within the mpeg header and id3 tags as well as
     * certain file attributes.
     *
     *@return   a string representation of this object
     */
    public String toString() {
        return "MP3File" + "\nPath:\t\t\t\t" + mp3.getAbsolutePath()
        + "\nFileSize:\t\t\t" + mp3.length()
        + " bytes\nPlayingTime:\t\t\t" + getPlayingTimeString() + "\n"
        + head + "\n" + id3v1 + "\n" +  id3v2;
    }
    
    
    /**
     * Returns true if the object o is equal to this MP3File. This is true if the
     * mp3s share the same path.
     *
     *@param o  the object to compare
     *@return   true if the object o is equal to this MP3File
     */
    public boolean equals(Object o) {
        boolean equal = false;
        
        if (o instanceof MP3File) {
            equal = this.getPath().equals(((MP3File) o).getPath());
        }
        
        return equal;
    }
    
    
    
    /**
     * Compare this MP3File to the specified object. This comparson does a simple
     * compare of the two paths. If the other object is not an mp3, compareTo will
     * always return a positive number.
     *
     *@param o  the object to compare to this one
     *@return   a positive number if this object is greater than the other, 0 if
     *      the object are equal, or a negative number if this object is less than
     *      the other
     */
    public int compareTo(Object o) {
        int cmp = 1;
        
        if (o instanceof MP3File) {
            cmp = this.getPath().compareTo(((MP3File) o).getPath());
        }
        
        return cmp;
    }
    
    // Peter Bennett Feb 2004
    /**
     * Get the ID3V2 tag for this file
     *
     *
     *@return   ID3V2Tag object for this file
     */
    public ID3v2Tag getID3v2Tag() {
        return id3v2;
    }
    
    //Peter Bennett
    public MPEGAudioFrameHeader getHeader() {
        return head;
    }
    
    public ID3v1Tag getID3v1Tag() {
        return id3v1;
    }
    
    
    public void syncV1AndV2() throws ID3v2FormatException{
        setAlbum(getAlbum());
        setArtist(getArtist());
        setGenre(getGenre());
        setTitle(getTitle());
        setYear(getYear());
        
    }


    // From here on - set of methods for getting and setting as used by Jampal
    // NOTE These values must match list in frame.properties

    

    public final static int COL_FILENAME         =  1;
    public final static int COL_ID3V2TAG         =  2;
    public final static int COL_BITRATE          =  3;
    public final static int COL_PLAYINGTIME      =  4;
    public final static int COL_FILESIZE         =  5;
    public final static int COL_MPEGVERSION      =  7;
    public final static int COL_PLAYINGTIMEMS    =  8;
    public final static int COL_SAMPLERATE       =  9;
    public final static int COL_CHANNELMODE      = 10;
    public final static int COL_COPYRIGHTED      = 11;
    public final static int COL_ORIGINAL         = 12;
    public final static int COL_CRC              = 13;
    public final static int COL_EMPHASIS         = 14;
    public final static int COL_PRIVATEBIT       = 16;
    public final static int COL_VBR              = 19;
    // For these it looks at first the v2 tag then the
    // v1 tag
    public final static int COL_TITLE            = 21;
    public final static int COL_ARTIST           = 22;
    public final static int COL_ALBUM            = 23;
    public final static int COL_YEAR             = 24;
    public final static int COL_COMMENT          = 25;
    public final static int COL_TRACK            = 26;
    public final static int COL_GENRE            = 27;

    // Dummy field for leaving a space in the library
    public final static int COL_DUMMY            = 28;

    // Field for getting a list of frames in the tag
    public final static int COL_FRAMES           = 29;

    public final static int [] columnTypes = {
         1, 2, 3, 4, 5, 7, 8, 9,10,11,12,13,14,16,19,21,22,23,24,25,26,27,28,29
    };

    public final static String [] columnTypeDescs = {
        "FILENAME",
        "ID3V2TAG",
        "BITRATE",
        "PLAYINGTIME",
        "FILESIZE",
        "MPEGVERSION",
        "PLAYINGTIMEMS",
        "SAMPLERATE",
        "CHANNELMODE",
        "COPYRIGHTED",
        "ORIGINAL",
        "CRC",
        "EMPHASIS",
        "PRIVATEBIT",
        "VBR",
        "TITLE",
        "ARTIST",
        "ALBUM",
        "YEAR",
        "COMMENT",
        "TRACK",
        "GENRE",
        "DUMMY",
        "FRAMES"
    };
    
    public String getMp3Field(int fieldType, String id) 
        throws Exception {
        int x;
        String s;
        switch (fieldType) {
        case COL_FILENAME:
            s=getPath();
            break;
        case COL_ID3V2TAG:
            s=getFrameDataString(id);
            break;
        case COL_BITRATE:
            s=String.valueOf(getBitRate());
            break;
        case COL_PLAYINGTIME:
            s=getPlayingTimeString();
            break;
        case COL_PLAYINGTIMEMS:
            long time = getPlayingTimeMs();
            s=String.valueOf(time);
            break;
        case COL_FILESIZE:
            s=String.valueOf(getFileSize() / 1024)+"K";
            break;
        case COL_MPEGVERSION:
            s=getMPEGVersion();
            break;
        case COL_SAMPLERATE:
            s=String.valueOf(getSampleRate());
            break;
        case COL_CHANNELMODE:
            s=getMPEGChannelMode();
            break;
        case COL_COPYRIGHTED:
            s=isMPEGCopyrighted()?"Y":"N";
            break;
        case COL_ORIGINAL:
            s=isMPEGOriginal()?"Y":"N";
            break;
        case COL_CRC:
            s=isMPEGProtected()?"Y":"N";
            break;
        case COL_EMPHASIS:
            s=getMPEGEmphasis();
            break;
        case COL_PRIVATEBIT:
            s=isMPEGPrivate()?"Y":"N";
            break;
        case COL_VBR:
            s=isVBR()?"Y":"N";
            break;
        case COL_TITLE:
            s=getTitle();
            if (s.length()==0) {
                s=getFileName();
                // strip extension
                int pos = s.lastIndexOf('.');
                if (pos != -1) 
                    s = s.substring(0,pos);
                s=fileNameReplacements(s);
            }
            break;
        case COL_ARTIST:
            s=getArtist();
            break;
        case COL_ALBUM:
            s=getAlbum();
            break;
        case COL_YEAR:
            s=getYear();
            break;
        case COL_COMMENT:
            s=getComment();
            break;
        case COL_TRACK:
            x=getTrack();
            // When there is no track it returns -1
            if (x==-1)
                x=0;
            s=String.valueOf(x);
            if (s.length()==1)
                s="0"+s;
            break;
        case COL_GENRE:
            s=getGenre();
            break;
        case COL_DUMMY:
            s="";
            break;
        case COL_FRAMES:
            s=getFrameIds();
            break;
        default:
            throw new ID3Exception("Invalid field type:"+fieldType);
        }
        s=s.trim();
        return s;
    }

    static Pattern dollarPattern = Pattern.compile("\\$");
    static Pattern percentPattern = Pattern.compile("\\%");
    static Pattern atPattern = Pattern.compile("\\@");
    static Pattern underscorePattern = Pattern.compile("_");
    
    
    public static String fileNameReplacements(String fileName) {
        String result = fileName;
        // Replace $ with ?, % with /, @ with :
        result=dollarPattern.matcher(result).replaceAll("\\?");
        result=percentPattern.matcher(result).replaceAll("\\/");
        result=atPattern.matcher(result).replaceAll("\\:");
        result=underscorePattern.matcher(result).replaceAll(" ");
        return result;
    }

    
    public String getMp3Integer(String id) 
        throws Exception {
        byte[] byteData;
        if (mp3==null)
            return "";
        byteData=getFrameDataBytes(id);
        if (byteData.length == 0)
            return "";
        java.math.BigInteger big = new java.math.BigInteger(byteData);
        return big.toString();
    }

    
    public String getMp3Binary(String id) 
        throws Exception {
        byte[] byteData;
        if (mp3==null)
            return "";
        byteData=getFrameDataBytes(id);
        if (byteData.length == 0)
            return "";
        StringBuffer buff = new StringBuffer();
        int startPos=id.length()-4+1;
        // Unicode string
        int maxIndex = startPos+400;
        if (maxIndex> byteData.length)
            maxIndex = byteData.length;
//        if (byteData.length%2==0 && byteData.length < 200) {
        int ix;
        for (ix=startPos;ix<maxIndex;ix+=2) {
            char nextCh = (char)(byteData[ix]+byteData[ix+1]*256);
            if (Character.isDefined(nextCh) && nextCh!=0)
                buff.append(nextCh);
        }
        if (maxIndex-startPos == 4) {
            buff.append(" ");
            int iValue = 0;
            for (ix=maxIndex-1;ix>=startPos;ix--) 
                iValue = iValue * 256 + byteData[ix];
            buff.append(Integer.toString(iValue));
        }
        return buff.toString();
    }
    
    
    public void setMp3Integer(String id, String value) 
        throws Exception {
        byte[] byteData = null;
        ID3v2Tag tag = getID3v2Tag();
        if (value.length()==0) {
            // check for zero length data - deleting frame
            tag.removeFrame(id);
            return;
        }
        else {
            java.math.BigInteger big = new java.math.BigInteger(value);
            byteData=big.toByteArray();
            if (byteData.length < 4) {
                byte bytePad;
                if (big.signum() >= 0)
                    bytePad = 0;
                else
                    bytePad = -1;
                byte [] newData = new byte[4];
                int ix;
                for (ix=0;ix<4-byteData.length; ix++){
                    newData[ix]=bytePad;
                }
                int iy;
                for (iy=0;iy<byteData.length;iy++) {
                    newData[ix++]=byteData[iy];
                }
                byteData=newData;
            }
        }
        tag.setGenericFrame(id, byteData);
    }
    
    
    
    public ID3v2Picture getMp3Picture(String id) 
        throws Exception {
        ID3v2Picture pic = null;
        if (mp3==null) {
            pic = new ID3v2Picture();
            pic.pictureData = new byte[0];
            return pic;
        }
        ID3v2Tag tag = getID3v2Tag();
        ID3v2Frames frames = tag.getFrames();
        ID3v2Frame frame = (ID3v2Frame)frames.get(id);
        if (frame!=null)
            pic = frame.getPicture();
        return pic;
    }

    public void setMp3Field(int fieldType, String id, 
        String Language, String Description,String value) 
        throws Exception {
        int x;
        switch (fieldType) {
        case COL_ID3V2TAG:
            ID3v2Tag tag = getID3v2Tag();
            tag.setGenericFrame(id, Language, Description, value);
//            StringBuffer fullValue = new StringBuffer();
//            // new value - Initialize with encoding value (0)
//            if (id.charAt(0) != 'W'||id.startsWith("WXXX"))
//                fullValue.append("\0");
//            // Add language and description to new value
//            if (Language!=null)
//                fullValue.append(Language);
//            if (Description!=null)
//                fullValue.append(Description);
//            ID3v2Tag tag = getID3v2Tag();
//            // check for zero length data - deleting frame
//            if (value.length()==0) {
//                // If this was a new frame just added the 
//                // language and description are not in the 
//                // id yet - get them from the value
//                if (id.length() == 4 && fullValue.length() > 0)
//                    id = id + fullValue.substring(1);
//                tag.removeFrame(id);
//                break;
//            }
//            // continue building up the new value
//            if (Language!=null || Description!=null)
//                fullValue.append("\0");
//            else {
//                // For cases where the language and description
//                // are not on the screen get them from
//                // the id and add them to the value
//                if (id.length() > 4) {
//                    fullValue.append(id.substring(4));
//                    fullValue.append("\0");
//                }
//            }
//            fullValue.append(value);
//            tag.setGenericFrame(id.substring(0,4), fullValue.toString());
            break;
        case COL_TITLE:
            setTitle(value);
            break;
        case COL_ARTIST:
            setArtist(value);
            break;
        case COL_ALBUM:
            setAlbum(value);
            break;
        case COL_YEAR:
            setYear(value);
            break;
        case COL_COMMENT:
            setComment(value);
            break;
        case COL_TRACK:
            setTrack(value);
            break;
        case COL_GENRE:
            setGenre(value);
            break;
        case COL_DUMMY:
            break;
        default:
            throw new ID3Exception("Invalid field type:"+fieldType);
        }
    }
    

    // when deleting a picture by setting null data, 
    // the id may have the picture type already included or not.
    // if not it is got from the picture object
    public void setMp3Picture(String id,ID3v2Picture pic) 
        throws Exception {
        // Text encoding      $xx
        // MIME type          <text string> $00
        // Picture type       $xx
        // Description        <text string according to encoding> $00 (00)
        // Picture data       <binary data>

        ID3v2Tag tag = getID3v2Tag();
        tag.setPictureFrame(pic);
        
    }

    static Pattern fixIdPattern = Pattern.compile("[\0\n\t\r ]");
    
    public String getFrameIds() {
        StringBuilder sBuild = new StringBuilder();
        ID3v2Tag tag = getID3v2Tag();
        ID3v2Frames frames = tag.getFrames();
        Object [] keys = frames.keySet().toArray();
        Arrays.sort(keys);
        sBuild.append(' ');
        for (Object key : keys) {
            String fixedKey = fixIdPattern.matcher(key.toString()).replaceAll("_");
            //sBuild.append(key.toString().replace("\\","\\\\"));
            //String keyStr = key.toString().replace(' ', '_');
            //keyStr = keyStr.replace('\0', '_');
            sBuild.append(fixedKey);
            sBuild.append(' ');
        }
        return sBuild.toString();
    }

    public boolean doesFrameExist(String id) {
        ID3v2Tag tag = getID3v2Tag();
        ID3v2Frames frames = tag.getFrames();
        Object obj = frames.get(id);
        return obj!=null;
    }
    
    
}

