/*
    Copyright 2004 Peter Bennett

    ptts - Peter's Text to Speech

    ptts is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; version 2 of the License.

    ptts is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with ptts; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/


// ptts.cpp : Defines the entry point for the console application.
//

#include <objbase.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <io.h>
#include <sphelper.h>
#include <sapi.h>
#include "ptts.h"


int doWaveFile = 0;
int doMultWaveFiles = 0;
wchar_t *pFileName = 0;
wchar_t *pUnicodeFileName = 0;
#define BUFFER_SIZE 10000
int rate=-999;
int volume=-999;
int samples=44100;
int channels=2;
WCHAR *pVoice=0;
wchar_t *pEncoding = 0;

int doit();
int lexAdd(wchar_t* pWord, wchar_t* pPron);
int wmain(int argc, wchar_t* argv[])
{

    int IsOK=1;
    int i;      
    int needFileName=0;
    int needRate=0;
    int needVolume=0;
    int needWord=0;
    int needPron=0;
	int needUnicodeFileName=0;
	int needSamples=0;
	int needChannels=0;
	int needVoice=0;
	int needEncoding=0;
    int doInstruct=0;
    int doLexiconAdd=0;
    int doListVoices=0;
    wchar_t *pWord=0;
    wchar_t *pPron=0;
                   
    wprintf(L"ptts Version 1.17 (c) 2004, 2008 Peter G. Bennett\n");


    for (i=1;i<argc && IsOK;i++) {

        if (needFileName) {
            pFileName=_wcsdup(argv[i]);
            needFileName=0;
        }
        else if (needUnicodeFileName) {
            pUnicodeFileName=_wcsdup(argv[i]);
            needUnicodeFileName=0;
        }
        else if (needRate) {
            rate=_wtoi(argv[i]);
            needRate=0;
        }
        else if (needVolume) {
            volume=_wtoi(argv[i]);
            needVolume=0;
        }
        else if (needSamples) {
            samples=_wtoi(argv[i]);
            needSamples=0;
        }
        else if (needChannels) {
            channels=_wtoi(argv[i]);
            needChannels=0;
        }
        else if (needWord) {
            pWord=argv[i];
            needWord=0;
            if (doLexiconAdd)
                needPron=1;
        }
        else if (needPron) {
            pPron=argv[i];
            needPron=0;
        }
        else if (needVoice) {
            pVoice=argv[i];
            needVoice=0;
        }
        else if (needEncoding) {
            pEncoding=argv[i];
            needEncoding=0;
            _wcsupr(pEncoding);
            if (wcscmp(pEncoding,L"UTF-8")!=0
                && wcscmp(pEncoding,L"UTF-16LE")!=0)
                IsOK=0;
        }

        else if (wcscmp(argv[i],L"-w") == 0) {
            if (doWaveFile || doMultWaveFiles)
                IsOK=0;
            doWaveFile = 1;
            needFileName=1;
        }
        else if (wcscmp(argv[i],L"-m") == 0) {
            if (doWaveFile || doMultWaveFiles)
                IsOK=0;
            doMultWaveFiles = 1;
            needFileName=1;
        }
        else if (wcscmp(argv[i],L"-r") == 0) {
            needRate=1;
        }
        else if (wcscmp(argv[i],L"-v") == 0) {
            needVolume=1;
        }
        else if (wcscmp(argv[i],L"-s") == 0) {
            needSamples=1;
        }
        else if (wcscmp(argv[i],L"-c") == 0) {
            needChannels=1;
        }
        else if (wcscmp(argv[i],L"-u") == 0) {
            needUnicodeFileName=1;
        }
        else if (wcscmp(argv[i],L"-la") == 0) {
            doLexiconAdd=1;
            needWord=1;
        }
        else if (wcscmp(argv[i],L"-vl") == 0) {
            doListVoices=1;
        }
        else if (wcscmp(argv[i],L"-voice") == 0) {
            needVoice=1;
        }
        else if (wcscmp(argv[i],L"-e") == 0) {
            needEncoding=1;
        }
        else if (argv[i][0]==L'-') {
            IsOK=0;
        }
        else {
            IsOK=0;
        }
    }
    if (needFileName||needVolume||needRate||needWord||needPron
		||needUnicodeFileName||needSamples||needChannels||needVoice)
        IsOK=0;
	if (IsOK && doListVoices) {
		WCHAR **ppszDescription;
		UINT count = listVoices(&ppszDescription,NULL);
		UINT ix;
		for (ix=0;ix < count; ix++) {
			fwprintf(stdout,L"%s\n",ppszDescription[ix]);	
		}
		delete [] ppszDescription;
	}
    if (IsOK && pFileName) {
        wchar_t * ext = wcsrchr(pFileName,L'.');
        if (ext) {
            if (_wcsicmp(ext,L".wav")==0) {
                *ext=L'\0';
            }
        }
    }
    if (IsOK)
        if (doLexiconAdd)
            IsOK=lexAdd(pWord,pPron);
        else
            IsOK=doit();
    else {
        fwprintf(stderr,L"Usage: ptts [options]\n");
        fwprintf(stderr,L"\n");
        fwprintf(stderr,L"Reads from standard input and generates speech\n");
        fwprintf(stderr,L"\n");
        fwprintf(stderr,L"Options:\n");
        fwprintf(stderr,L"-w filename  create wave file instead of outputting sound\n");
        fwprintf(stderr,L"-m filename  multiple wave files\n");
        fwprintf(stderr,L"             new wave file after each empty input line\n");
        fwprintf(stderr,L"             appends nnnnn.wav to the filename.\n");
        fwprintf(stderr,L"-r rate      Speech rate -10 to +10, default is 0.\n");
        fwprintf(stderr,L"-v volume    Volume as a percentage, default is 100.\n");
        fwprintf(stderr,L"-s samples   Samples per sec for wav file, default is 44100.\n");
        fwprintf(stderr,L"-c channels  Channels (1 or 2) for wav file, default is 2.\n");
        fwprintf(stderr,L"-u filename  Read text from file instead of stdin.\n");
        fwprintf(stderr,L"-e encoding  File encoding. UTF-8, UTF-16LE.\n");
		fwprintf(stderr,L"             Default ANSI or as indicated by BOM in file.\n");
        fwprintf(stderr,L"-voice xxxx  Voice to be used.\n");
        fwprintf(stderr,L"-vl          List voices.\n");
        fwprintf(stderr,L"XML can be included in the input text to control the speech.\n");
        fwprintf(stderr,L"For details see the Microsoft speech API.\n");
        fwprintf(stderr,L"Update Lexicon - \n");
        fwprintf(stderr,L"-la word pronounciation   add word.\n");
        fwprintf(stderr,L"-la word \"\"               delete word.\n");
    }

    return !IsOK;    
}

int doit() {

    wchar_t fileName[256];
    wchar_t *pBuffer;
    pBuffer = (wchar_t*)malloc(BUFFER_SIZE * sizeof (wchar_t));
    int bufferUsed=0;
    int prevBufferUsed=0;
    int eof=0;
    int waveSeq=0;
    int hSpeaker=0;
    int IsOK=1;
	FILE *pInFile;
	wchar_t fileMode[256];


    if (!doMultWaveFiles) {
        if (pFileName == 0)
            hSpeaker = createSpeaker(0);
        else {
            swprintf(fileName,L"%s.wav",pFileName);
            hSpeaker = createSpeaker(fileName);
        }
        if (hSpeaker == 0) {
            fprintf(stderr,"hSpeaker is 0.\n");
            return 0;
        }
        else {
            if (rate != -999)
                IsOK=setRate(hSpeaker, rate);
            if (IsOK && volume != -999)
                IsOK=setVolume(hSpeaker, volume);
            if (!IsOK) {
                fprintf(stderr,"Set rate %d or volume %d failed.\n",rate,volume);
                return 0;
            }
			if (IsOK && pVoice != NULL)
				setVoice(hSpeaker,pVoice);
        }
    }

	if (pUnicodeFileName != 0) {
		wcscpy(fileMode,L"r, ccs=");
		if (pEncoding == 0) 
    		wcscat(fileMode, L"UNICODE");
        else
    		wcscat(fileMode, pEncoding);
		pInFile = _wfopen(pUnicodeFileName, fileMode);
		if (pInFile==0) {
			wprintf(L"Unable to open input file %s\n",pUnicodeFileName);
			return 0;
		}
	}
	else
		pInFile=stdin;

    while (!eof) {
        bufferUsed=0;
        prevBufferUsed=0;
        wchar_t* endBufferUsed;
        while(bufferUsed<BUFFER_SIZE) {
            pBuffer[bufferUsed]=0;
            wchar_t *ret = fgetws(pBuffer+bufferUsed,BUFFER_SIZE-bufferUsed,pInFile);
            endBufferUsed = wcschr(pBuffer, L'\n');
            if (endBufferUsed == 0) 
                endBufferUsed = wcschr(pBuffer, L'\0');
            if (endBufferUsed == 0 || ret == 0) {
                eof=1;
                break;
            }
            // replace newline with space
            if (*endBufferUsed == L'\n')
                *endBufferUsed=L' ';
            prevBufferUsed = bufferUsed;
            bufferUsed = endBufferUsed - pBuffer;
            if (bufferUsed==prevBufferUsed)
                break;
            bufferUsed++;
        }
        // Check if there is anything to say
        wchar_t *pChar=pBuffer;
        while (!iswalnum(*pChar)&&*pChar!=0)
            pChar++;
        if (*pChar==0)
            continue;
        // We now have something to say
        if (doMultWaveFiles) {
            swprintf(fileName,L"%s%5.5d.wav",pFileName,++waveSeq);
            hSpeaker = createSpeaker(fileName);
            if (hSpeaker == 0)
                return 0;
            else {
                if (rate != -999)
                   IsOK=setRate(hSpeaker, rate);
                if (IsOK && volume != -999)
                    IsOK=setVolume(hSpeaker, volume);
                if (!IsOK)
                    return 0;
            }
        }
        Speak(hSpeaker,pBuffer);
        if (doMultWaveFiles) {
            closeSpeaker(hSpeaker);
            hSpeaker=0;
        }
    }
    if (hSpeaker) {
        closeSpeaker(hSpeaker);
        hSpeaker=0;
    }

    closeDown();
    return 1;

}

struct Speaker {
    ISpVoice * pVoice;
    ISpStream *pWavStream;
    ISpStreamFormat *pOldStreamFormat;
};

static int coInitialized = 0;

int createSpeaker(const wchar_t *fileName) {

    if (FAILED(::CoInitialize(NULL)))
    {
        fprintf(stderr,"CoInitialize failed\n");
        return 0;
    }
    coInitialized++;
    Speaker *pSpeaker = new Speaker;
    pSpeaker->pVoice=0;
    pSpeaker->pWavStream=0;
    pSpeaker->pOldStreamFormat=0;
    WAVEFORMATEX waveFormat;

    HRESULT hr = CoCreateInstance(CLSID_SpVoice, NULL, CLSCTX_ALL, IID_ISpVoice, 
        (void **)&(pSpeaker->pVoice));
    if( SUCCEEDED( hr ) )
    {

        CSpStreamFormat OriginalFmt;
        if (fileName != 0) {
            hr = pSpeaker->pVoice->GetOutputStream( &pSpeaker->pOldStreamFormat );
            if (hr == S_OK)
            {
                hr = OriginalFmt.AssignFormat(pSpeaker->pOldStreamFormat);
            }
            else
            {
                hr = E_FAIL;
                fprintf(stderr,"pSpeaker->pVoice->GetOutputStream failed\n");
            }
            // User SAPI helper function in sphelper.h to create a wav file
            if (SUCCEEDED(hr))
            {
//                hr = SPBindToFile( fileName, SPFM_CREATE_ALWAYS, &pSpeaker->pWavStream, 
//                    &OriginalFmt.FormatId(), OriginalFmt.WaveFormatExPtr() ); 
                waveFormat.wFormatTag = WAVE_FORMAT_PCM;
                waveFormat.nChannels = channels;        
                waveFormat.nSamplesPerSec = samples;   
                waveFormat.nAvgBytesPerSec = samples*channels*2;  
                waveFormat.nBlockAlign = channels*2;      
                waveFormat.wBitsPerSample = 16;   
                waveFormat.cbSize = 0;           

                hr = SPBindToFile( fileName, SPFM_CREATE_ALWAYS, &pSpeaker->pWavStream, 
                    &SPDFID_WaveFormatEx, &waveFormat ); 
            }
            if( SUCCEEDED( hr ) )
            {
                // Set the voice's output to the wav file instead of the speakers
                hr = pSpeaker->pVoice->SetOutput(pSpeaker->pWavStream, TRUE);
            }
            else
            fprintf(stderr,"SPBindToFile failed\n");
        }
        else 
            pSpeaker->pVoice->SetOutput(NULL,TRUE);

        return (int)pSpeaker;
    }
    else
        fprintf(stderr,"CoCreateInstance(CLSID_SpVoice) failed\n");

    return 0;
}



int lexAdd(wchar_t* pWord, wchar_t* pPron) {
    // adding a word to lexicon

    // example ptts -la qntal "k w ih n t ao 1 l"

    if (FAILED(::CoInitialize(NULL)))
    {
        fprintf(stderr,"CoInitialize failed\n");
        return 0;
    }
    coInitialized++;
    
    ISpLexicon * pLexicon;
    ISpPhoneConverter * pConverter;

    LANGID lang = SpGetUserDefaultUILanguage ();


    HRESULT hr = SpCreatePhoneConverter(lang, NULL, NULL, &pConverter);


    if (FAILED(hr)) {
        fprintf(stderr,"Create CLSID_SpPhoneConverter failed\n");
        return 0;
    }
    SPPHONEID phones[100];    

    hr = pConverter->PhoneToId(pPron, phones);

    if (FAILED(hr)) {
        fprintf(stderr,"PhoneToId failed\n");
        return 0;
    }

    hr = CoCreateInstance(CLSID_SpLexicon, NULL, CLSCTX_ALL, IID_ISpLexicon, 
        (void **)&(pLexicon));
    if( SUCCEEDED( hr ) )  {

        hr = pLexicon->RemovePronunciation(pWord, lang, SPPS_Unknown, 0);
        hr = pLexicon->AddPronunciation(pWord, lang, SPPS_Unknown, phones);
    }
    else {
        fprintf(stderr,"Create CLSID_SpLexicon failed\n");
        return 0;
    }

    if(!  SUCCEEDED( hr ) )  {
        fprintf(stderr,"AddPronunciation failed\n");
        return 0;
    }



    int hSpeaker=0;
    hSpeaker = createSpeaker(0);
    if (hSpeaker == 0)
        return 0;
    Speak(hSpeaker,pWord);
    if (hSpeaker) {
        closeSpeaker(hSpeaker);
        hSpeaker=0;
    }

    closeDown();
    return 1;

}



int Speak(int hSpeaker, const wchar_t *pText) {
    
    Speaker *pSpeaker = (Speaker *)hSpeaker;

//    int rate;
//    pSpeaker->pVoice->GetRate(&rate);
//    unsigned short volume;
//    pSpeaker->pVoice->GetVolume(&volume);
//    fwprintf(stderr,L"Rate: %ld, Volume: %hu\n",rate,volume);

    HRESULT hr = pSpeaker->pVoice->Speak(pText,0,NULL);

    pSpeaker->pVoice->WaitUntilDone( INFINITE );

    return hr == S_OK;
}    
   

// Speaking rate - valid values -10 to +10, default 0
int setRate(int hSpeaker, int rate) {
    Speaker *pSpeaker = (Speaker *)hSpeaker;
    HRESULT hr = pSpeaker->pVoice->SetRate((int)rate);
    return hr == S_OK;
}    

// Volume - percentage - valid values 1 to 100, default 100
int setVolume(int hSpeaker, int volume) {
    Speaker *pSpeaker = (Speaker *)hSpeaker;
    HRESULT hr = pSpeaker->pVoice->SetVolume((USHORT)volume);
    return hr == S_OK;
}    

// Voice - must pass in exact description
int setVoice(int hSpeaker, const WCHAR *pszVoice) {
    Speaker *pSpeaker = (Speaker *)hSpeaker;
	WCHAR **ppszDescription;
	ISpObjectToken **ppVoiceToken;
	UINT count = listVoices(&ppszDescription, &ppVoiceToken);
	ISpObjectToken *pVoiceToken=NULL;
	UINT ix;
	for (ix=0;ix<count;ix++) {
		if (_wcsicmp(pszVoice,ppszDescription[ix])==0) {
			pVoiceToken = ppVoiceToken[ix];
		}
		else
			ppVoiceToken[ix]->Release();
	}

	if (pVoiceToken==NULL) {
		fwprintf(stderr,L"Voice does not exist - %s\n",pszVoice);
		return 0;
	}

    HRESULT hr = pSpeaker->pVoice->SetVoice(pVoiceToken);
	pVoiceToken->Release();
    return hr == S_OK;
}    

    

int closeSpeaker(int hSpeaker) {

    Speaker *pSpeaker = (Speaker *)hSpeaker;

    if (pSpeaker->pWavStream != NULL) {
        pSpeaker->pWavStream->Release();
    
        // Reset output
        pSpeaker->pVoice->SetOutput( pSpeaker->pOldStreamFormat, FALSE );

    }

    pSpeaker->pVoice->Release();
    pSpeaker = NULL;

    ::CoUninitialize();
    coInitialized--;

    return TRUE;
}

UINT listVoices(WCHAR ***pppszReturn, ISpObjectToken ***pppVoiceToken) {
	ISpVoice * pVoice;

    WCHAR **ppszDescription;
	ISpObjectToken **ppVoiceToken;

	if (FAILED(::CoInitialize(NULL)))
    {
        fprintf(stderr,"CoInitialize failed\n");
        return 0;
    }
    coInitialized++;

	// Create the SAPI voice
	HRESULT	hr = CoCreateInstance(CLSID_SpVoice, NULL, CLSCTX_ALL, IID_ISpVoice, 
        (void **)&pVoice);

	IEnumSpObjectTokens *cpEnum;

	//Enumerate the available voices 
	if(SUCCEEDED(hr))
		hr = SpEnumTokens(SPCAT_VOICES, NULL, NULL, &cpEnum);

	ULONG ulCount;
	ISpObjectToken *cpVoiceToken;
	//Get the number of voices
	if(SUCCEEDED(hr))
		hr = cpEnum->GetCount(&ulCount);

	ppszDescription = new WCHAR*[ulCount];
	if (pppszReturn != NULL)
		*pppszReturn = ppszDescription;
	if (pppVoiceToken != NULL) {
		ppVoiceToken = new ISpObjectToken * [ulCount];
		*pppVoiceToken = ppVoiceToken;
	}

	UINT ix = 0;
	// Obtain a list of available voice tokens, 
	for (ix=0;ix<ulCount;ix++) {
		if(SUCCEEDED(hr))
			hr = cpEnum->Next( 1, &cpVoiceToken, NULL );

		if(SUCCEEDED(hr)) {
			SpGetDescription(cpVoiceToken, &ppszDescription[ix],NULL);
//			hr = cpVoice->SetVoice(cpVoiceToken);

//		if(SUCCEEDED(hr))
//			hr = cpVoice->Speak( L"How are you?", SPF_DEFAULT, NULL); 
			if (pppVoiceToken!=NULL)
				ppVoiceToken[ix]=cpVoiceToken;
			else
				cpVoiceToken->Release();
		}

	}

	pVoice->Release();

    ::CoUninitialize();
    coInitialized--;

	if(SUCCEEDED(hr))
		return (UINT)ulCount;
	else {
        fprintf(stderr,"Failed to get voice list.\n");
		return FALSE;
	}

}

void closeDown() {
    while (coInitialized--)
        ::CoUninitialize();
    coInitialized=0;
}

    
