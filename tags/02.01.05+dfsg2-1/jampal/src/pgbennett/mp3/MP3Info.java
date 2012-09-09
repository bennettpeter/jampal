package pgbennett.mp3;

import java.io.*;

/*
    MP3Info.java
 *
    mp3tech.c - Functions for handling MP3 files and most MP3 data
                structure manipulation.
 
    Copyright (C) 2000-2001  Cedric Tefft <cedric@earthling.net>
 
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.
 
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
 
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 
 ***************************************************************************
 
  This file is based in part on:
 
 * MP3Info 0.5 by Ricardo Cerqueira <rmc@rccn.net>
 * MP3Stat 0.9 by Ed Sweetman <safemode@voicenet.com> and
             Johannes Overmann <overmann@iname.com>
 *
 * Translated from c to java by Peter Bennett <pgbennett@users.sourceforge.net>,
 * April 2004
 *
 
 */


public class MP3Info {
    
    /* MIN_CONSEC_GOOD_FRAMES defines how many consecutive valid MP3 frames
       we need to see before we decide we are looking at a real MP3 file */
    public final static int  MIN_CONSEC_GOOD_FRAMES = 4;
    public final static int  FRAME_HEADER_SIZE = 4;
    public final static int  MIN_FRAME_SIZE = 21;
    public final static int  NUM_SAMPLES = 4;
    
    public final static int VBR_VARIABLE = 0;
    public final static int VBR_AVERAGE  = 1;
    public final static int VBR_MEDIAN   = 2;
    
    public final static int SCAN_NONE    = 0;
    public final static int SCAN_QUICK   = 1;
    public final static int SCAN_FULL    = 2;
    
    public final static int EOF    = -1;
    
    public class mp3header implements Cloneable {
        public long   sync;
        public int    version;
        public int    layer;
        public int    crc;
        public int    bitrate;
        public int    freq;
        public int    padding;
        public int    extension;
        public int    mode;
        public int    mode_extension;
        public int    copyright;
        public int    original;
        public int    emphasis;
        public Object myClone()  {
            try {
                return super.clone();
            }
            catch (CloneNotSupportedException ex) {
                return null;
            }
        }
        
/* Get next MP3 frame
   Return codes:
   positive value = Frame Length of this header
   0 = No, we did not retrieve a valid frame header
 */
        
        int get_header(RandomAccessFile file) throws IOException {
            byte buffer[] = new byte[FRAME_HEADER_SIZE];
            int fl;
            
            //    if(fread(&buffer,FRAME_HEADER_SIZE,1,file)<1) {
            if(file.read(buffer)<1) {
                sync=0;
                return 0;
            }
            int buffer0 = (int)buffer[0]&0x00FF;
            int buffer1 = (int)buffer[1]&0x00FF;
            sync=(((int)buffer0<<4) | ((int)(buffer1&0xE0)>>4));
            if((buffer[1] & 0x10)!=0)
                version=(buffer[1] >>> 3) & 1;
            else version=2;
            layer=(buffer[1] >>> 1) & 3;
            if((sync != 0xFFE) || (layer != 1)) {
                sync=0;
                return 0;
            }
            crc=buffer[1] & 1;
            bitrate=(buffer[2] >>> 4) & 0x0F;
            freq=(buffer[2] >>> 2) & 0x3;
            padding=(buffer[2] >>>1) & 0x1;
            extension=(buffer[2]) & 0x1;
            mode=(buffer[3] >>> 6) & 0x3;
            mode_extension=(buffer[3] >>> 4) & 0x3;
            copyright=(buffer[3] >>> 3) & 0x1;
            original=(buffer[3] >>> 2) & 0x1;
            emphasis=(buffer[3]) & 0x3;
            // 20100530 - extra checks
            if (layer < 1
                || bitrate < 1) {
                sync = 0;
                return 0;
            }
            
            try {
                return ((fl=frame_length()) >= MIN_FRAME_SIZE ? fl : 0);
            }
            catch(Exception ex) {
                return 0;
            }
        }
        
        
        public int frame_length() {
            return sync == 0xFFE ?
            (frame_size_index[3-layer]*((version&1)+1)*
            header_bitrate()/header_frequency())+
            padding : 1;
        }
        
        public int header_layer() {return layer_tab[layer];}
        
        public int header_bitrate() {
            return bitrates[version & 1][3-layer][bitrate-1];
        }
        
        public int header_frequency() {
            return frequencies[version][freq];
        }
        
        public String header_emphasis() {
            return emphasis_text[emphasis];
        }
        
        public String header_mode() {
            return mode_text[mode];
        }
        
        public boolean equals(mp3header h2) {
            //    if((*(uint*)h1) == (*(uint*)h2)) return 1;
            //        if(h1.equals(h2)) return 1;
            
            if((version       == h2.version         ) &&
            (layer         == h2.layer           ) &&
            (crc           == h2.crc             ) &&
            (freq          == h2.freq            ) &&
            (mode          == h2.mode            ) &&
            (copyright     == h2.copyright       ) &&
            (original      == h2.original        ) &&
            (emphasis      == h2.emphasis        ))
                return true;
            else return false;
        }
    }
    
    // public mp3 info fields
    public String filename;
    public RandomAccessFile file;
    public int datasize;
    public int header_isvalid;
    public mp3header header;
    public boolean vbr;
    public float vbr_average;
    public int seconds;
    public int frames;
    public int badframes;
    /** Start of valid header in file. -1 if none found. */
    public long valid_start;
    
    static final int layer_tab[]= {0, 3, 2, 1};
    
    static final int frequencies[][] = {
        {22050,24000,16000,50000},  /* MPEG 2.0 */
        {44100,48000,32000,50000},  /* MPEG 1.0 */
        {11025,12000,8000,50000}    /* MPEG 2.5 */
    };
    
    static final int bitrates[][][] = {
        { /* MPEG 2.0 */
            {32,48,56,64,80,96,112,128,144,160,176,192,224,256},  /* layer 1 */
            {8,16,24,32,40,48,56,64,80,96,112,128,144,160},       /* layer 2 */
            {8,16,24,32,40,48,56,64,80,96,112,128,144,160}        /* layer 3 */
        },
        
        { /* MPEG 1.0 */
            {32,64,96,128,160,192,224,256,288,320,352,384,416,448}, /* layer 1 */
            {32,48,56,64,80,96,112,128,160,192,224,256,320,384},    /* layer 2 */
            {32,40,48,56,64,80,96,112,128,160,192,224,256,320}      /* layer 3 */
        }
    };
    
    static final int frame_size_index[] = {24000, 72000, 72000};
    
    
    static final String mode_text[] = {
        "stereo", "joint stereo", "dual channel", "mono"
    };
    
    static final String emphasis_text[] = {
        "none", "50/15 microsecs", "reserved", "CCITT J 17"
    };
    
    //    int get_mp3_info(mp3info mp3,int scantype, int fullscan_vbr) {
    public MP3Info(File javaFile,RandomAccessFile raf,
        int scantype, boolean fullscan_vbr, long startpos,
        int numSamples) 
        throws IOException {
        //        mp3info mp3 = new mp3info();
        this.filename = filename;
        int frame_type[]={0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        float total_rate=0;
        int frame_types=0,frames_so_far=0;
        int l,vbr_median=-1;
        int bitrate,lastrate;
        int counter=0;
        ///  struct stat filestat;
        ///  off_t sample_pos,data_start=0;
        int sample_pos,data_start=0;
        
        ///  stat(mp3->filename,&filestat);
        ///  mp3->datasize=filestat.st_size;
        datasize = (int)javaFile.length();
        ///  get_id3(mp3);
        file = raf;
            if(scantype == SCAN_QUICK) {
                if(get_first_header(startpos)!=0) {
                    //        data_start=ftell(mp3->file);
                    data_start=(int)file.getFilePointer();
                    lastrate=15-header.bitrate;
                    while((counter < numSamples) && lastrate!=0) {
                        sample_pos=(counter*(datasize/numSamples+1))+data_start;
                        if(get_first_header(sample_pos)!=0) {
                            bitrate=15-header.bitrate;
                        } else {
                            bitrate=-1;
                        }
                        
                        if(bitrate != lastrate) {
                            vbr=true;
                            if(fullscan_vbr) {
                                counter=numSamples;
                                scantype=SCAN_FULL;
                            }
                        }
                        lastrate=bitrate;
                        counter++;
                        
                    }
                    if(!(scantype == SCAN_FULL)) {
                        frames=(datasize-data_start)/(l=header.frame_length());
                        seconds = (int)((float)(header.frame_length()*frames)/
                        (float)(header.header_bitrate()*125)+0.5);
                        vbr_average = (float)header.header_bitrate();
                    }
                }
                
            }
            
            mp3header headerCopy;
            frames=0;
            float secondsCount=0;
            if(scantype == SCAN_FULL) {
                if(get_first_header(startpos)!=0) {
                    //        data_start=ftell(mp3->file);
                    data_start=(int)file.getFilePointer();
                    while((bitrate=get_next_header())!=0) {
                        frame_type[15-bitrate]++;
                        frames++;
                    }
                    //        memcpy(&header,&(mp3->header),sizeof(mp3header));
                    headerCopy = (mp3header)header.myClone();
                    for(counter=0;counter<15;counter++) {
                        if(frame_type[counter]!=0) {
                            frame_types++;
                            headerCopy.bitrate=counter;
                            frames_so_far += frame_type[counter];
                            secondsCount += (float)(headerCopy.frame_length()*frame_type[counter])/
                            (float)(headerCopy.header_bitrate()*125);
                            total_rate += (float)((headerCopy.header_bitrate())*frame_type[counter]);
                            if((vbr_median == -1) && (frames_so_far >= frames/2))
                                vbr_median=counter;
                        }
                    }
                    seconds=(int)(secondsCount+0.5);
                    header.bitrate=vbr_median;
                    vbr_average=total_rate/(float)frames;
                    if(frame_types > 1) {
                        vbr=true;
                    }
                }
            }
        
        return ;
    }
    
    
    private int get_first_header(long startpos) throws IOException {
        int k, l=0,c;
        //  mp3header h, h2;
        mp3header h = new mp3header();
        mp3header h2 = new mp3header();
        
        //  fseek(mp3->file,startpos,SEEK_SET);
        file.seek(startpos);
        while (true) {
            //     while((c=fgetc(mp3->file)) != 255 && (c != EOF));
            while((c=file.read()) != 255 && (c != EOF));
            if(c == 255) {
                //        ungetc(c,mp3->file);
                file.seek(file.getFilePointer() - 1);
                //        valid_start=ftell(mp3->file);
                valid_start=file.getFilePointer();
                if((l=h.get_header(file))!=0) {
                    //          fseek(mp3->file,l-FRAME_HEADER_SIZE,SEEK_CUR);
                    file.seek(file.getFilePointer()+l-FRAME_HEADER_SIZE);
                    //          for(k=1; (k < MIN_CONSEC_GOOD_FRAMES) && (mp3->datasize-ftell(mp3->file) >= FRAME_HEADER_SIZE); k++) {
                    for(k=1; (k < MIN_CONSEC_GOOD_FRAMES) && (datasize-file.getFilePointer() >= FRAME_HEADER_SIZE); k++) {
                        if((l=h2.get_header(file))==0)
                            break;
                        //                        if(sameConstant(h,h2)==0)
                        if(!h.equals(h2))
                            break;
                        //            fseek(mp3->file,l-FRAME_HEADER_SIZE,SEEK_CUR);
                        file.seek(file.getFilePointer()+l-FRAME_HEADER_SIZE);
                    }
                    if(k == MIN_CONSEC_GOOD_FRAMES) {
                        //            fseek(mp3->file,valid_start,SEEK_SET);
                        file.seek(valid_start);
                        //            memcpy(&(mp3->header),&h2,sizeof(mp3header));
                        header=h2;
                        header_isvalid=1;
                        return 1;
                    }
                    // 2004-05-24 Peter Bennett
                    else 
                        // bad header - resume where we left off
                        file.seek(valid_start+1); 
                }
            } else {
                valid_start = -1;
                return 0;
            }
        }
        
//        valid_start = -1;
//        return 0;
    }
    
/* get_next_header() - read header at current position or look for
   the next valid header if there isn't one at the current position
 */
    private int get_next_header() throws IOException {
        int l=0,c,skip_bytes=0;
        mp3header h = new mp3header();
        
        while(true) {
            //     while((c=fgetc(mp3->file)) != 255 && (ftell(mp3->file) < mp3->datasize)) skip_bytes++;
            while((c=file.read()) != 255 && (file.getFilePointer() < datasize))
                skip_bytes++;
            if(c == 255) {
                //        ungetc(c,mp3->file);
                file.seek(file.getFilePointer() - 1);
                if((l=h.get_header(file))!=0) {
                    if(skip_bytes!=0)
                        badframes++;
                    //      fseek(mp3->file,l-FRAME_HEADER_SIZE,SEEK_CUR);
                    file.seek(file.getFilePointer()+l-FRAME_HEADER_SIZE);
                    return 15-h.bitrate;
                } else {
                    skip_bytes += FRAME_HEADER_SIZE;
                }
            } else {
                if(skip_bytes!=0)
                    badframes++;
                return 0;
            }
        }
    }
    
    
}