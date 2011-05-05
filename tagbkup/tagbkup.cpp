/*
    tagbkup - mp3 tag backup and restore

    Copyright (c) 2004 Peter G. Bennett

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


#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#ifndef OS_Linux
    #include <io.h>
#endif
#ifdef OS_Linux
   #include <unistd.h>
   #include <sys/types.h>
#endif

#include <fcntl.h>
#include <sys/stat.h>
#ifdef __CYGWIN32__
	#include <sys/cygwin.h>
#endif
#ifdef _MSC_VER
	#include <direct.h>
	#define strcasecmp stricmp
	#define strncasecmp strnicmp
	#define ftruncate chsize
#endif

#include <time.h>

//#include <windows.h>
//#include "MP3Info.h"
#include "mp3info_u.h"
 
const size_t BufSize = 16384;
const int MaxFName = 260;

int FileProcCount = 0;
int IsBkupGlob = 0;
int IsRestGlob = 0;
int IsCopyGlob = 0;
int IsTestGlob = 0;
int IsOutFileGlob=0;
int IsVerbose = 0;
int IsCopyV1 = 0;
int IsCopyV2 = 0;
int IsCopyMp3 = 0;
int ReportUpdates = 0;
int OverwriteBkup = 1;
char *pRestDir = 0;
char *pFileSpec = 0;
char *pBkupDir = 0;
FILE *fpLog;
FILE *std_err = stderr;
unsigned int (*pSignatureList)[2] = 0;
int countSignature=0;
int Instruct=0;
int bypassErrs = 0;
int signatureOnFront = 0;
int tagSubdirs = 0;

int GetFileSignature(char *pFoundName, unsigned int *pSig, size_t *pData_start);
int getDataStart(char *pFoundName, size_t *pDataStart);
int CopyTag(char *pOutputFile, char *pInputFile);
int CompareTag(char *pOutputFile, char *pInputFile);
int AppendMP3Data(char *pOutputFile, char *pInputFile, size_t data_start);
int GetTagSize(FILE *fpIn, size_t *pTagSize);
// int UpdateTagV1(char *pFile);
int openBackupLog();
int ProcessFile(char *pMP3FileName);
void mkdir_f(char *fileName);
int AppendTagV1(char *pOutFile,char *pInFile);
int countV1Tags(FILE *fpIn, int &v1tags, off_t &dataEnd);

// a useful macro
#define IsID3Tag(s) (s[0] == 'T' && s[1] == 'A' && s[2] == 'G')

extern int nullMp3[];
extern size_t nullMp3size;

int main(int argc, char *argv[], char * /*envp*/ []) {
    int IsOK=1;
    int i;      
    char fileName[512];
                   
    fprintf(stderr,"tagbkup Version **UNKNOWN** (c) 2004-2010 Peter G. Bennett\n");
#ifdef __CYGWIN32__
	fprintf(stderr,"Cygwin version, ");
#endif
#ifdef _MSC_VER
	fprintf(stderr,"Win32 version, ");
#endif
#ifdef OS_Linux
	fprintf(stderr,"Linux version, ");
#endif
#ifdef CPU_x86_64
    fprintf(stderr,"64 bit.\n");
#else
    fprintf(stderr,"32 bit.\n");
#endif
    // Last parameter is backup dir
    pBkupDir = argv[argc-1];
    if (argc < 3 || *pBkupDir == '-'){
        Instruct = 1;
        IsOK=0;
    }

    struct stat fileStat;
    int ret;
    if (IsOK) {
        fileStat.st_mode=0;
        ret=stat(pBkupDir,&fileStat);
        if (ret!=0) {
            char *p=strrchr(pBkupDir,'.');
            if (p && strcasecmp(p,".mp3")==0)
                IsOutFileGlob=1;
            else {

		        #ifdef _MSC_VER
	                mkdir(pBkupDir);
		        #else
			        mkdir(pBkupDir,S_IRWXU|S_IRWXG|S_IRWXO);
		        #endif
                    ret=stat(pBkupDir,&fileStat);
            }
        }
        if (ret==0) {
            if (fileStat.st_mode & S_IFDIR)
                IsOutFileGlob=0;
            else
                IsOutFileGlob=1;
        }
        pSignatureList = new unsigned int [50000][2];
    }

    
    for (i=1;i<argc-1 && IsOK;i++) {
        if (strcmp(argv[i],"-b") == 0) {
            IsBkupGlob = 1;
            fprintf(stderr,"Backup tags to directory %s\n",pBkupDir);
            if (IsOutFileGlob)
                IsOK=0;
        }
        else if (strcmp(argv[i],"-r") == 0) {
            IsRestGlob = 1;
            fprintf(stderr,"Restore tags from directory %s\n",pBkupDir);
            if (IsOutFileGlob)
                IsOK=0;
        }
        else if (strcmp(argv[i],"-c") == 0) {
            IsCopyGlob = 1;
            if (IsOutFileGlob)
                fprintf(stderr,"Copy tags from mp3 file to mp3 file %s\n",pBkupDir);
            else
                fprintf(stderr,"Copy tags from mp3 file to mp3 files in directory %s\n",pBkupDir);
        }
        else if (strcmp(argv[i],"-c1") == 0) {
            IsCopyV1 = 1;
            fprintf(stderr,"Copy V1 tags\n");
        }
        else if (strcmp(argv[i],"-c2") == 0) {
            IsCopyV2 = 1;
            fprintf(stderr,"Copy V2 tags\n");
        }
        else if (strcmp(argv[i],"-cm") == 0) {
            IsCopyMp3 = 1;
            fprintf(stderr,"Copy mp3 data\n");
        }
        else if (strcmp(argv[i],"-t") == 0) {
            IsTestGlob = 1;
            fprintf(stderr,"Test run - nothing will be written\n");
        }
        else if (strcmp(argv[i],"-v") == 0) 
            IsVerbose = 1;
        else if (strcmp(argv[i],"-u") == 0) 
            ReportUpdates = 1;
        else if (strcmp(argv[i],"-n") == 0) 
            OverwriteBkup = 0;
        else if (strcmp(argv[i],"-d") == 0) {
            if (i<argc-2 && argv[i+1][0]!='-') 
                pRestDir=argv[++i];
            else {
                IsOK=0;
                Instruct=1;
            }
        }
        else if (strcmp(argv[i],"-f") == 0) {
            if (IsOutFileGlob) {
                fprintf(std_err,"Output is a file name(%s), cannot use -f option\n",pBkupDir);
                IsOK=0;
                Instruct=1;
            }
            if (IsOK && i<argc-2 && argv[i+1][0]!='-') {
                char *pListFile=argv[++i];
                // Process list from a file
                FILE *fpList=0;
                IsOK=openBackupLog();
                if (IsOK) {
                    // 2010/10/11 change from rb to r
                    fpList = fopen(pListFile,"r");
                    if (fpList==0) {
                        fprintf(std_err,"Unable to open list file %s\n",pListFile);
                        IsOK=0;
                    }
                }
                if (IsOK) {
                    char *pRet;
                    for(;;) {
                        pRet=fgets(fileName, sizeof fileName, fpList);
                        if (pRet==0)
                            break;
                        pRet=strchr(fileName,'\n');
                        if (pRet)
                            *pRet=0;
                        IsOK=ProcessFile(fileName);
                    }
                }
                if (fpList)
                    fclose(fpList);
            }
            else {
                IsOK=0;
                Instruct=1;
            }
        }
        else if (strcmp(argv[i],"-y") == 0) {
            bypassErrs=1;
        }
        else if (strcmp(argv[i],"-s") == 0) {
            signatureOnFront=1;
        }
        else if (strcmp(argv[i],"-x") == 0) {
            tagSubdirs=1;
        }
        else if (argv[i][0] == '-') { 
            IsOK=0;
            Instruct=1;
        }
        else {
            if (IsOutFileGlob) {
                int j;
                for (j=i+1;j<argc-1;j++) {
                    if (argv[j][0] != '-') {
                        fprintf(std_err,"Output is a file name(%s), cannot use more that 1 input file\n",pBkupDir);
                        IsOK=0;
                        Instruct=1;
                    }
                }
            }
            else
                openBackupLog();
            if (IsOK)
                IsOK=ProcessFile(argv[i]);
        }
        if (!IsOK && !IsOutFileGlob) {
            if (bypassErrs) {
                fprintf(std_err,"Error - Continuing with other files\n");
                IsOK=true;
            }
            else {
                int ans=0;
                while (ans != 'y'&& ans != 'Y'&&ans!='n'&&ans!='N') {
                    fprintf(std_err,"Errors - do you wish to continue? (y/n)\n");
                    ans = getchar();
                    if (ans == 'y'|| ans == 'Y')
                        IsOK=true;
                }
            }
        }
    }

    if (Instruct) {
        fprintf(stderr,"Usage: tagbkup [options] filename ... backupdir\n");
        fprintf(stderr,"\n");
        fprintf(stderr,"Backs up and restores mp3 id3v1 and id3v2 tags\n");
        fprintf(stderr,"\n");
        fprintf(stderr,"Options (-b, -r or -c is required):\n");
        fprintf(stderr,"-b       Backup tags from mp3 files to directory\n");
        fprintf(stderr,"-r       Restore tags from directory to mp3 files\n");
        fprintf(stderr,"-c       Copy tags from mp3 files to like named mp3 files in backupdir\n");
        fprintf(stderr,"         or copy tags from an mp3 file to another mp3 file, where the\n");
        fprintf(stderr,"         output mp3 file name is supplied instead of backupdir\n");
        fprintf(stderr,"-c1      Append ID3V1 tag from mp3 file to output file\n");
        fprintf(stderr,"-c2      Copy ID3V2 tag from mp3 file to output file, overwriting existing file\n");
        fprintf(stderr,"-cm      Append mp3 data without tags from mp3 file to output file\n");
        fprintf(stderr,"-d dir   Copy restored mp3 files to this directory\n");
        fprintf(stderr,"-t       Test - no copying of data\n");
        fprintf(stderr,"-v       Verbose\n");
        fprintf(stderr,"-u       Report updates done\n");
        fprintf(stderr,"-n       No overwriting of backup files, append log file\n");
        fprintf(stderr,"-f fn    Input filename list is in file fn\n");
        fprintf(stderr,"-y       Continue if there are errors, skipping failed files\n");
        fprintf(stderr,"-s       File signature based on front part of file instead of end\n");
        fprintf(stderr,"-x       Tags in 256 subdirectories\n");
    }

    if (pSignatureList)
        delete [] pSignatureList;
    if (fpLog)
        fclose(fpLog);
    
    fprintf(stderr,"Number of files processed = %d\n",FileProcCount);

    return !IsOK;    
}


int openBackupLog() {
    int IsOK=1;
    char logFile[256];
    if (IsBkupGlob) {
        if (fpLog==0) {
            strcpy(logFile,pBkupDir);
            strcat(logFile,"/");
            strcat(logFile,"tagbkup.log");
            fprintf(stderr,"Log file %s\n",logFile);
            char *mode = "ab";
            if (OverwriteBkup)
                mode="wb";
            fpLog=fopen(logFile,mode);
            if (fpLog==0) {
                fprintf(stderr,"Unable to open log file %s\n",logFile);
                IsOK=0;
            }
        }
    }
    return IsOK;
}

// Make the directory for a file
void mkdir_f(char *fileName) {
    char dirName[512];
    char *pIn=fileName;
    char *pOut=dirName;
    memset(dirName,0,sizeof dirName);
    for(;*pIn;pIn++,pOut++) {
        if (pOut !=dirName) {
            if (*pIn == '/' || *pIn == '\\') {
				#ifdef _MSC_VER
	                mkdir(dirName);
				#else
					mkdir(dirName,S_IRWXU|S_IRWXG|S_IRWXO);
				#endif
            }
        }
        *pOut=*pIn;
    }
}

int ProcessFile(char *pMP3FileName) {
    char szBkupName[512];
    char szRestName[512];
    char szFinalRestName[512];
    char *pFinalRestName;
    char Signature[20];
    unsigned int sig[2];
    static time_t notifyTime = 0;

    // 5/30/2010 - progress reporting
    if (notifyTime == 0)
        notifyTime = time(0);
    else {
        time_t now = time(0);
        if (difftime(now, notifyTime) >= 60.0) {
            notifyTime = now;
            fprintf(stderr,"Number of files processed so far = %d\n",FileProcCount);
        }
    }
    
    int IsOK=1;
    int ret;
    int copyOpts = 0;
    if (IsCopyV1 || IsCopyV2 || IsCopyMp3)
        copyOpts = 1;

    if (IsBkupGlob + IsRestGlob + IsCopyGlob + copyOpts != 1){
        IsOK=0;
        Instruct=1;
    }

    if (IsBkupGlob)
        pRestDir=0;

    char *p=strrchr(pMP3FileName,'.');
    size_t data_start=0;
    if (p && strcasecmp(p,".mp3")==0
        && strncasecmp(pMP3FileName,"tag_",4)!=0
        && IsOK) {
        int seq;
        if (IsBkupGlob || IsRestGlob) {
            IsOK=GetFileSignature(pMP3FileName,sig,&data_start);
            if (IsOK) {
                sprintf(Signature,"%8.8x%8.8x",sig[0],sig[1]);
                FileProcCount++;
            }
        }
        else
            IsOK=getDataStart(pMP3FileName,&data_start);
        for (seq=0;IsOK;) {
            int i;
            if (IsBkupGlob || IsRestGlob) {
                for (i=0;i<countSignature;i++){
                    if (sig[0]==pSignatureList[i][0]
                        && sig[1]==pSignatureList[i][1]) {
                        seq++;
                    }
                }
				if (tagSubdirs) 
	                sprintf(szBkupName,"%s/%2.2s/tag_%s_%3.3d.mp3",pBkupDir,Signature,Signature,seq);
				else
					sprintf(szBkupName,"%s/tag_%s_%3.3d.mp3",pBkupDir,Signature,seq);
                pSignatureList[countSignature][0]=sig[0];
                pSignatureList[countSignature++][1]=sig[1];
            }
            if (IsBkupGlob){
                ret=CopyTag(szBkupName,pMP3FileName);
                switch (ret) {
                case -1:
                    // Duplicate file found with different data
                    IsOK=AppendMP3Data(szBkupName,0,0);
                    if (IsOK)
                        IsOK=AppendTagV1(szBkupName,pMP3FileName);
                    if (!IsOK) {
                        fprintf(stderr,"%s --> %s - Error\n",pMP3FileName,szBkupName);
                        break;
                    }
                    if (IsVerbose || ReportUpdates)
                        fprintf(stderr,"%s --> %s - data updated\n",
                            pMP3FileName,szBkupName);
                    break;
                case 0:
                    // Copied OK
                    IsOK=AppendMP3Data(szBkupName,0,0);
                    if (IsOK)
                        IsOK=AppendTagV1(szBkupName,pMP3FileName);
                    if (IsOK) {
                        if (IsVerbose || ReportUpdates)
                            fprintf(stderr,"%s --> %s - copied OK\n",
                                pMP3FileName,szBkupName);
                    }
                    else
                        fprintf(stderr,"%s --> %s - Error\n",pMP3FileName,szBkupName);
                    break;
                case 1:
                    // Duplicate file found with same data
                    // OK
                    if (IsVerbose)
                        fprintf(stderr,"%s --> %s - up to date\n",pMP3FileName,szBkupName);
                    break;
                case 2:
                    // error copying tag
                    fprintf(stderr,"%s --> %s - Error\n",pMP3FileName,szBkupName);
                    IsOK=0;
                    break;
                case 3:
                    // Duplicate file, different data not copied
                        fprintf(stderr,"%s --> %s - already exists for a different source - retry\n",
                            pMP3FileName,szBkupName);
                    continue;
                }
                if (fpLog)
                    fprintf(fpLog,"%s_%3.3d %s\n",Signature,seq,pMP3FileName);

                break;
            }
            if (IsRestGlob) {
                if (pRestDir) {
                    strcpy(szRestName,pRestDir);
                    char Last = *(strchr(szRestName,0)-1);
                    if (Last != '/' && Last != '\\') 
                        strcat(szRestName,"/");
                    if (*pMP3FileName == '/' || *pMP3FileName == '\\')
                        strcat(szRestName,pMP3FileName+1);
                    else if (pMP3FileName[1] == ':') {
                        if (pMP3FileName[2] == '\\')
                            strcat(szRestName,pMP3FileName+3);
                        else
                            strcat(szRestName,pMP3FileName+2);
                    }
                    else
                        strcat(szRestName,pMP3FileName);
                    mkdir_f(szRestName);
                    pFinalRestName=szRestName;
                }
                else {
                    ret = CompareTag(pMP3FileName,szBkupName);
                    if (ret==1) {
                        if (IsVerbose) {
                            fprintf(stderr,"%s --> %s",szBkupName,pMP3FileName);
                            fprintf(stderr," - file already exists with correct data\n");
                        }
                        break;
                    }
                    if (ret==2) {
                        IsOK=0;
                        break;
                    }

                    strcpy(szRestName,pMP3FileName);
                    strcat(szRestName,"rest");
                    pFinalRestName=pMP3FileName;
                }
                remove(szRestName);
                ret=CopyTag(szRestName,szBkupName);
//                if (ret==-1) 
                if (ret!=0){
                    fprintf(stderr,"%s --> %s\n",szBkupName,pFinalRestName);
                    // error copying tag
                    fprintf(std_err,"Error Copying tag to %s\n",szRestName);
                    IsOK=0;
                    break;
                }
                IsOK=AppendMP3Data(szRestName,pMP3FileName,data_start);
                if (!IsOK)
                    break;
                IsOK=AppendTagV1(szRestName,szBkupName);
//                IsOK=UpdateTagV1(szRestName);
                if (!IsOK)
                    break;
                if (!IsTestGlob && !pRestDir)
                    IsOK=(remove(pMP3FileName)==0);
                if (!IsOK) {
                    fprintf(stderr,"%s --> %s\n",szBkupName,pFinalRestName);
                    fprintf(std_err,"Error - Unable to delete file %s\n",pMP3FileName);
                    break;
                }
                if (!IsTestGlob && !pRestDir)
                    IsOK=(rename(szRestName,pMP3FileName)==0);
                if (!IsOK) {
                    fprintf(stderr,"%s --> %s\n",szBkupName,pFinalRestName);
                    fprintf(std_err,"Error - Unable to rename file %s to %s\n",szRestName,pMP3FileName);
                    break;
                }
                if (IsVerbose || ReportUpdates) {
                    fprintf(stderr,"%s --> %s",szBkupName,pFinalRestName);
                    fprintf(stderr," - tag restored\n");
                }
            }
            if (IsCopyGlob) {
                if (IsOutFileGlob) {
                    if (FileProcCount > 1) {
                        IsOK=0;
                        fprintf(std_err,"Error - When copying to a file only one input file can be used.");
                        break;
                    }
                    strcpy(szRestName,pBkupDir);
                }
                else {
                    strcpy(szRestName,pBkupDir);
                    char *pFileName = strrchr(pMP3FileName,'/');
                    if (pFileName==0)
                        pFileName = strrchr(pMP3FileName,'\\');
                    if (pFileName==0)
                        pFileName = strrchr(pMP3FileName,':');
                    if (pFileName==0)
                        pFileName=pMP3FileName;
                    else
                        pFileName++;
                    strcat(szRestName,"/");
                    strcat(szRestName,pFileName);
                }
                FileProcCount++;
                strcpy(szFinalRestName,szRestName);
                strcat(szRestName,"copy");

                remove(szRestName);
                ret=CompareTag(szFinalRestName, pMP3FileName);
                if (ret==1) {
                    if (IsVerbose) {
                        fprintf(stderr,"%s --> %s",pMP3FileName,szFinalRestName);
                        fprintf(stderr," - file already exists with correct data\n");
                    }
                    break;
                }
                if (ret==2) {
                    IsOK=0;
                    break;
                }
                int outputFileExists = 1;
                if (ret==0)
                    outputFileExists = 0;
                ret=CopyTag(szRestName,pMP3FileName);
//                if (ret==-1) 
                if (ret!=0){
                    fprintf(stderr,"%s --> %s\n",pMP3FileName,szFinalRestName);
                    // error copying tag
                    fprintf(std_err,"Error Copying tag to %s\n",szRestName);
                    IsOK=0;
                    break;
                } 
                if (outputFileExists) 
                    IsOK=AppendMP3Data(szRestName,szFinalRestName,data_start);
                else
                    IsOK=AppendMP3Data(szRestName,0,0);
                if (!IsOK)
                    break;
                IsOK=AppendTagV1(szRestName,pMP3FileName);
//                IsOK=UpdateTagV1(szRestName);
                if (!IsOK)
                    break;
                if (!IsTestGlob && outputFileExists)
                    IsOK=(remove(szFinalRestName)==0);
                if (!IsOK) {
                    fprintf(stderr,"%s --> %s\n",pMP3FileName,szFinalRestName);
                    fprintf(std_err,"Error - Unable to delete file %s\n",szFinalRestName);
                    break;
                }
                if (!IsTestGlob)
                    IsOK=(rename(szRestName,szFinalRestName)==0);
                if (!IsOK) {
                    fprintf(stderr,"%s --> %s\n",pMP3FileName,szFinalRestName);
                    fprintf(std_err,"Error - Unable to rename file %s to %s\n",szRestName,szFinalRestName);
                    break;
                }
                if (IsVerbose || ReportUpdates) {
                    fprintf(stderr,"%s --> %s",pMP3FileName,szFinalRestName);
                    fprintf(stderr," - tag copied\n");
                }


            }
            if (IsCopyV2){
                if (IsOutFileGlob) {
                    strcpy(szRestName,pBkupDir);
                }
                else {
                    fprintf(stderr,"%s --> %s\n",pMP3FileName,szRestName);
                    // error copying tag
                    fprintf(std_err,"Copy tag V2 requires output file name not directory\n");
                    IsOK=0;
                    break;
                } 

                FileProcCount++;
                remove(szRestName);
                ret=CopyTag(szRestName,pMP3FileName);
                if (ret!=0){
                    fprintf(stderr,"%s --> %s\n",pMP3FileName,szRestName);
                    // error copying tag
                    fprintf(std_err,"Error %d Copying tag to %s\n",ret,szRestName);
                    IsOK=0;
                    break;
                } 
            }
            if (IsCopyMp3){
                if (IsOutFileGlob) {
                    strcpy(szRestName,pBkupDir);
                }
                else {
                    fprintf(stderr,"%s --> %s\n",pMP3FileName,szRestName);
                    // error copying tag
                    fprintf(std_err,"Copy Mp3 data requires output file name not directory\n");
                    IsOK=0;
                    break;
                } 
                FileProcCount++;
                IsOK=AppendMP3Data(szRestName,pMP3FileName,data_start);
            }
            if (IsCopyV1){
                if (IsOutFileGlob) {
                    strcpy(szRestName,pBkupDir);
                }
                else {
                    fprintf(stderr,"%s --> %s\n",pMP3FileName,szRestName);
                    // error copying tag
                    fprintf(std_err,"Copy Tag V1 requires output file name not directory\n");
                    IsOK=0;
                    break;
                } 
                FileProcCount++;
                IsOK=AppendTagV1(szRestName,pMP3FileName);
            }

            // No need to try another seq value if we got here
            break;
        }
    }

    return IsOK;
}


// Create a signature for the music.

int GetFileSignature(char *pFoundName, unsigned int *pSig, size_t *pData_start) {
    FILE *fpIn;
    int IsOK=1;

    fpIn=fopen(pFoundName,"rb");
    if (fpIn==0) {
        fprintf(std_err,"Error - Unable to open file %s\n",pFoundName);
        IsOK=0;
    }   

    //   The ID3v2 tag header, which should be the first information in the
    //   file, is 10 bytes as follows:
    //
    //     ID3v2/file identifier      "ID3"
    //     ID3v2 version              $03 00
    //     ID3v2 flags                %abc00000
    //     ID3v2 size             4 * %0xxxxxxx


    //     The ID3v2 tag size is encoded with four bytes where the most
    //     significant bit (bit 7) is set to zero in every byte, making a total
    //     of 28 bits. The zeroed bits are ignored, so a 257 bytes long tag is
    //     represented as $00 00 02 01.

    //     The ID3v2 tag size is the size of the complete tag after
    //     unsychronisation, including padding, excluding the header but not
    //     excluding the extended header (total tag size - 10). Only 28 bits
    //     (representing up to 256MB) are used in the size description to avoid
    //     the introducuction of 'false syncsignals'.

    
    
    //   An ID3v2 tag can be detected with the following pattern:
    //     I  D  3
    //   $49 44 33 yy yy xx zz zz zz zz
    //     0  1  2  3  4  5  6  7  8  9
    //   Where yy is less than $FF, xx is the 'flags' byte and zz is less than
    //   $80.
    
    mp3info mp3i;
    *pData_start=0;


    struct stat fileStat;
    if (IsOK) {
        int ret=fstat (fileno(fpIn),&fileStat);
        if (ret!=0) {
            fprintf(std_err,"Error calling fstat on file %s\n",pFoundName);
            IsOK=0;
        }
    }

    size_t tagSize = 0;
    if (IsOK) {
        IsOK=GetTagSize(fpIn,&tagSize);
        memset(&mp3i,0,sizeof(mp3info));
        mp3i.filename=pFoundName;
        mp3i.file=fpIn;
        mp3i.datasize=fileStat.st_size;
        if(get_first_header(&mp3i,tagSize)) {
//        if(get_first_header(&mp3i,0))
            *pData_start=ftell(fpIn);
        }
        else {
            fprintf(std_err,"Unable to access mp3 frame in file %s\n",pFoundName);
            IsOK=0;
        }
//        fprintf(stderr,"tagsize = %d\n",tagSize); 
//        fprintf(stderr,"pData_start = %d\n",*pData_start); 
    }

    unsigned int sig[2]={0,0};
    unsigned int buf[2]={0,0};
    off_t numread = 2048;       // approx 1 second
    off_t start;
    int count;
    int i;    
    off_t dataEnd;
    int v1tags;

    if (IsOK) {
        if (signatureOnFront) {
            start = (*pData_start)+16*1024*15;  // approx 15 seconds

            if (fileStat.st_size < start + ((off_t)sizeof buf) * numread + 512)
                start = *pData_start;
        }
        else {
//            off_t dataEnd = fileStat.st_size;
//            int v1tags=0;
//            for (;;) {
//                IsOK=(fseek(fpIn,dataEnd-128,SEEK_SET)==0);
//                if (IsOK) {
//                    count=fread(v1tag,1,sizeof v1tag,fpIn);
//                    if (count!=3)
//                        IsOK=0;
//                }
//                if (IsOK) {
//                    if (IsID3Tag(v1tag)) {
//                        dataEnd-=128;
//                        v1tags++;
//                    }
//                    else
//                        break;
//                }
//            }
            IsOK=countV1Tags(fpIn, v1tags, dataEnd);
            if (IsOK) {
                if (v1tags==0)
                    fprintf(std_err,"Warning - No ID3v1 tag on file %s\n",pFoundName);
                else if (v1tags!=1)
                    fprintf(std_err,"Warning - %d ID3v1 tags on file %s\n",v1tags,pFoundName);
                start = dataEnd - (((off_t)sizeof buf) * numread) - 16*1024*30; // approx 30 seconds
                if (start < (off_t)*pData_start) {
                    start = dataEnd - (((off_t)sizeof buf) * numread) - 16*1024*10; // approx 10 seconds
                }
                if (start < (off_t)*pData_start) {
                    start = dataEnd - (((off_t)sizeof buf) * numread) - 16*1024*5; // approx 5 seconds
                }
                if (start < (off_t)*pData_start) {
                    fprintf(std_err,"Error - file is too small %s\n",pFoundName);
                    IsOK=0;
                }
            }
        }
    }

    if (IsOK)
        IsOK=(fseek(fpIn,start,SEEK_SET)==0);

    for (i=0;i<numread&&IsOK;i++) {
        count=fread(buf,1,sizeof buf,fpIn);
        if (count != sizeof sig)
            break;
        sig[0]^=buf[0];
        sig[1]^=buf[1];
    }

    if (fpIn)
        fclose(fpIn);

    pSig[0]=sig[0];
    pSig[1]=sig[1];
    return IsOK;
}


int countV1Tags(FILE *fpIn, int &v1tags, off_t &dataEnd) {
    int IsOK=1;
    char v1tag[3];
    v1tags=0;
    dataEnd=0;
    int count;
    IsOK=(fseek(fpIn,0,SEEK_END)==0);
    if (IsOK)
        dataEnd = ftell(fpIn);
    for (;IsOK;) {
        IsOK=(fseek(fpIn,dataEnd-128,SEEK_SET)==0);
        if (IsOK) {
            count=fread(v1tag,1,sizeof v1tag,fpIn);
            if (count!=3)
                IsOK=0;
        }
        if (IsOK) {
            if (IsID3Tag(v1tag)) {
                dataEnd-=128;
                v1tags++;
            }
            else
                break;
        }
    }

    if (!IsOK) 
        fprintf(std_err,"Error calculating number of v1 tags\n");
    
    return IsOK;
}


int getDataStart(char *pFoundName, size_t *pDataStart) {
    FILE *fpIn;
    int IsOK=1;

    fpIn=fopen(pFoundName,"rb");
    if (fpIn==0) {
        fprintf(std_err,"Error - Unable to open file %s\n",pFoundName);
        IsOK=0;
    }   

    struct stat fileStat;
    if (IsOK) {
        int ret=fstat (fileno(fpIn),&fileStat);
        if (ret!=0) {
            fprintf(std_err,"Error calling fstat on file %s\n",pFoundName);
            IsOK=0;
        }
    }
                 
    mp3info mp3i;
    size_t tagSize = 0;
    *pDataStart = 0;
    if (IsOK) {
        IsOK=GetTagSize(fpIn,&tagSize);
        memset(&mp3i,0,sizeof(mp3info));
        mp3i.filename=pFoundName;
        mp3i.file=fpIn;
        mp3i.datasize=fileStat.st_size;
        if(get_first_header(&mp3i,tagSize)) {
            *pDataStart=ftell(fpIn);
        }
        else {
            fprintf(std_err,"Unable to access mp3 frame in file %s\n",pFoundName);
            IsOK=0;
        }
    }
    if (fpIn)
        fclose(fpIn);
    return IsOK;
}



// return code
// -1 Duplicate file found with different data, copied
// 0  Data copied 
// 1  File found with same data
// 2  Error
// 3  Duplicate File found with different data not copied

int CopyTag(char *pOutputFile, char *pInputFile) {
    FILE *fpIn=0;
    FILE *fpOut=0;
    int IsOK=1;
    size_t tagsize;
    char *pInputBuff=0;
    size_t count;
    size_t count2;
    int result=0;
    pInputBuff=new char[BufSize];
    size_t readlen;

    // check if the output file exists and
    // has the same contents
    if (IsOK) {
        result=CompareTag(pOutputFile, pInputFile);
        if (result==2)
            IsOK=0;
    }
    if (result == -1 && !OverwriteBkup)
        result=3;

    if (result<=0 && IsOK) {
        fpIn=fopen(pInputFile,"rb");
        if (fpIn==0) {
            fprintf(std_err,"Error opening input file %s\n",pInputFile);
            IsOK=0;
        }
    }

    if (result<=0 && IsOK) 
        IsOK=GetTagSize(fpIn,&tagsize);

    if (tagsize == 0)
        fprintf(std_err,"Warning - no tag on file %s\n",pInputFile);

    if (result<=0 && IsOK) {
        fseek(fpIn,0,SEEK_SET);
        if (!IsTestGlob) {
            fpOut=fopen(pOutputFile,"wb");
            if (fpOut==0 && tagSubdirs) {
				// Check if directory does not exist - create it
				mkdir_f(pOutputFile);
		        fpOut=fopen(pOutputFile,"wb");
			}
            if (fpOut==0) {
                fprintf(std_err,"Error opening output file %s\n",pOutputFile);
                IsOK=0;
            }
        }
    }
    if (result<=0 && IsOK) {
        size_t remains=tagsize;
        while(remains>0) {
            readlen=remains;
            if (readlen>BufSize)
                readlen=BufSize;
            count = fread(pInputBuff,1,readlen,fpIn);
            if (count != readlen) {
                IsOK=0;
                fprintf(std_err,"Error reading file %s\n",pInputFile);
                break;
            }
            if (!IsTestGlob) {
                count2 = fwrite(pInputBuff,1,count,fpOut);
                if (count2 != count) {
                    IsOK=0;
                    fprintf(std_err,"Error writing file %s\n",pOutputFile);
                    break;
                }
            }
            remains-=readlen;
        }
    }

    if (pInputBuff) {
        delete [] pInputBuff;
        pInputBuff=0;
    }
    
    if (fpIn)
        fclose(fpIn);
    if (fpOut) 
        fclose(fpOut);

    if (!IsOK)
        result=2;

    return result;

}


// return code
// -1 different data
// 0  No output file
// 1  same data
// 2  Error

int CompareTag(char *pOutputFile, char *pInputFile) {
    FILE *fpIn=0;
    FILE *fpOut=0;
    int IsOK = 1;
    size_t tagsize;
    char *pInputBuff=0;
    char *pOutputBuff=0;
    size_t count;
    int result=0;

    fpIn=fopen(pInputFile,"rb");
    if (fpIn==0) {
        fprintf(std_err,"Error opening input file %s\n",pInputFile);
        IsOK=0;
    }
    if (IsOK)
        IsOK=GetTagSize(fpIn,&tagsize);
    pInputBuff=new char[BufSize];
    size_t readlen;
    // check if the output file exists and
    // has the same contents
    if (IsOK) {
        fseek(fpIn,0,SEEK_SET);
        fpOut=fopen(pOutputFile,"rb");
        if (fpOut!=0) {
            size_t tagoutsize;
            IsOK=GetTagSize(fpOut,&tagoutsize);
            fseek(fpOut,0,SEEK_SET);
            result=1; // assume data will be the same
            if (IsOK) {
                if (tagsize!=tagoutsize)
                    // file exists with incorrect data
                    result=-1;
                pOutputBuff=new char[BufSize];
                size_t remains=tagsize;
                while(remains>0 && result==1) {
                    readlen=remains;
                    if (readlen>BufSize)
                        readlen=BufSize;
                    count = fread(pInputBuff,1,readlen,fpIn);
                    if (count != readlen) {
                        IsOK=0;
                        fprintf(std_err,"Error reading file %s\n",pInputFile);
                        break;
                    }
                    count = fread(pOutputBuff,1,readlen,fpOut);
                    if (count == readlen) {
                        if (memcmp(pInputBuff,pOutputBuff,readlen)!=0) {
                            // file exists with incorrect data
                            result=-1;
                            break;
                        }
                    }
                    else {
                        // file is short - means wrong data
                        result=-1;
                        break;
                    }
                    remains-=readlen;
                }
            }
        }
    }

    if (pInputBuff) {
        delete [] pInputBuff;
        pInputBuff=0;
    }
    if (pOutputBuff) {
        delete [] pOutputBuff;
        pOutputBuff=0;
    }
    
    if (fpIn)
        fclose(fpIn);
    if (fpOut) 
        fclose(fpOut);

    if (!IsOK)
        result=2;

    return result;

}




int AppendMP3Data(char *pOutputFile, char *pInputFile, size_t data_start) {

    FILE *fpIn=0;
    FILE *fpOut=0;
    int IsOK=1;
//    size_t tagsize;
    char *pInputBuff=0;
    size_t count;
    off_t dataEnd;
    int v1tags;

    if (pInputFile) {
        fpIn=fopen(pInputFile,"rb");
        if (fpIn==0) {
            fprintf(std_err,"Error opening input file %s\n",pInputFile);
            IsOK=0;
        }
//        if (IsOK)
//            IsOK=GetTagSize(fpIn,&tagsize);
        pInputBuff=new char[BufSize];
        if (IsOK) {
            IsOK=countV1Tags(fpIn, v1tags, dataEnd);
            IsOK=(fseek(fpIn,data_start,SEEK_SET)==0);
            if (!IsOK) 
                fprintf(std_err,"Error seeking on file %s\n",pInputFile);
        }

    }
    size_t readlen;
    if (IsOK && !IsTestGlob) {
        fpOut=fopen(pOutputFile,"ab");
        if (fpOut==0) {
            fprintf(std_err,"Error opening output file %s\n",pOutputFile);
            IsOK=0;
        }
    }
    if (IsOK) {
        if (pInputFile) {
            off_t pos = data_start;

            while(!feof(fpIn) && pos < dataEnd) {
                size_t maxReadLen = dataEnd - pos;
                if (maxReadLen > BufSize)
                    maxReadLen = BufSize;
                readlen = fread(pInputBuff,1,maxReadLen,fpIn);
                if (ferror(fpIn)) {
                    IsOK=0;
                    fprintf(std_err,"Error reading file %s\n",pInputFile);
                    break;
                }
                pos += readlen;
                if (!IsTestGlob) {
                    count = fwrite(pInputBuff,1,readlen,fpOut);
                    if (count != readlen) {
                        IsOK=0;
                        fprintf(std_err,"Error writing file %s\n",pOutputFile);
                        break;
                    }
                }
            }
        }
        else {
            if (!IsTestGlob) {
                count = fwrite(nullMp3,1,nullMp3size,fpOut);
                if (count != nullMp3size) {
                    IsOK=0;
                    fprintf(std_err,"Error writing file %s\n",pOutputFile);
                }
            }
        }
    }

    if (pInputBuff) {
        delete [] pInputBuff;
        pInputBuff=0;
    }
    
    if (fpIn)
        fclose(fpIn);
    if (fpOut) 
        fclose(fpOut);

    return IsOK;

}

int GetTagSize(FILE *fpIn, size_t *pTagSize) {
    *pTagSize=0;
    unsigned char work[20];
    int IsOK=1;
    size_t count;

    if (IsOK){
        count=fread(work,1,10,fpIn);
        if (count != 10) {
            fprintf(std_err,"Error reading file\n");
            IsOK=0;
        }
    }

    if (memcmp(work,"ID3",3)==0
        && work[3] != 255
        && work[4] != 255
        && work[6] < 128
        && work[7] < 128
        && work[8] < 128
        && work[9] < 128) {
        // We have a tag
        *pTagSize = (size_t) work[9] 
                + (size_t) work[8] * 128 
                + (size_t) work[7] * 128 * 128
                + (size_t) work[6] * 128 * 128 * 128 + 10;
    }
    return IsOK;

}


// If 2 or more V1 tags replace them all
int AppendTagV1(char *pOutFile,char *pInFile) {

    FILE *fpIn=0;
    FILE *fpOut=0;
    int ret;
    int IsOK=1;
    char newtag[128];
    int count;
    off_t dataEnd;
    int v1tags;
    

    if (!IsTestGlob) {
        if (IsOK) {
            fpOut=fopen(pOutFile,"r+b");
            if (fpOut==0) {
                fprintf(std_err,"Error opening r+b file %s\n",pOutFile);
                IsOK=0;
            }
        }

        if (IsOK) {
            IsOK=countV1Tags(fpOut, v1tags, dataEnd);
            if (!IsOK) {
                fprintf(std_err,"Error counting v1 tags on %s\n",pOutFile);
            }
        }
        if (IsOK) {
            ret=fseek(fpOut,dataEnd,SEEK_SET);
            if (ret!=0) {
                fprintf(std_err,"Error seeking dataEnd on %s\n",pOutFile);
                IsOK=0;
            }
        }
    }

    if (IsOK) {
        fpIn=fopen(pInFile,"rb");
        if (fpIn==0) {
            fprintf(std_err,"Error opening r+b file %s\n",pInFile);
            IsOK=0;
        }
    }
    if (IsOK) {
        IsOK=countV1Tags(fpIn, v1tags, dataEnd);
        if (!IsOK) {
            fprintf(std_err,"Error counting v1 tag on %s\n",pInFile);
        }
    }
    if (IsOK) {
        if (v1tags > 1)
            fprintf(std_err,"Warning - %d id3v1 tags on %s\n",v1tags,pInFile);
        if (v1tags < 1)
            fprintf(std_err,"Warning - no id3v1 tags on %s\n",pInFile);
        ret=fseek(fpIn,-128,SEEK_END);
        if (ret!=0) {
            fprintf(std_err,"Error seeking end-128 on %s\n",pInFile);
            IsOK=0;
        }
    }
    if (IsOK && v1tags >= 1) {
        count=fread(newtag,1,128,fpIn);
        if (count != 128) {
            fprintf(std_err,"Error reading file %s\n",pInFile);
            IsOK=0;
        }
        if (IsOK && !IsTestGlob) {
            if (IsID3Tag(newtag)) {
    //            fprintf(stderr,"debug id3v1 = %s\n",newtag);
                count = fwrite(newtag,1,128,fpOut);
                if (count != 128) {
                    IsOK=0;
                    fprintf(std_err,"Error writing file %s\n",pOutFile);
                }
            }
        }
    }
    if (IsOK && !IsTestGlob) {
        ret=ftruncate(fileno(fpOut),ftell(fpOut));
        if (ret != 0) {
            fprintf(std_err,"Error truncating %s\n",pOutFile);
            IsOK=0;
        }
    }

    if (fpIn)
        fclose(fpIn);
    if (fpOut)
        fclose(fpOut);

    return IsOK;
}


