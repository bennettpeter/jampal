'    Copyright 2011 Peter Bennett
'
'    This file is part of Jampal.
'
'    Jampal is free software: you can redistribute it and/or modIfy
'    it under the terms of the GNU General Public License as published by
'    the Free Software Foundation, either version 3 of the License, or
'    (at your option) any later version.
'
'    Jampal is distributed in the hope that it will be useful,
'    but WITHOUT ANY WARRANTY without even the implied warranty of
'    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
'    GNU General Public License for more details.
'
'    You should have received a copy of the GNU General Public License
'    along with Jampal.  If not, see <http://www.gnu.org/licenses/>.
'

doWaveFile = 0
doMultWaveFiles = 0
pFileName = Null
pUnicodeFileName = Null
rate=-999
volume=-999
samples=44100
channels=2
pVoice=Null
pEncoding = Null

Dim IsOK
IsOK = 1

Dim argv
Set argv = WScript.Arguments

printErr("ptts Version 1.28 (c) 2011 Peter G. Bennett")

For Each arg in argv
    If needFileName Then
        pFileName=arg
        needFileName=0
    ElseIf needUnicodeFileName Then
        pUnicodeFileName=arg
        needUnicodeFileName=0
    ElseIf needRate Then
        rate=CInt(arg)
        needRate=0
    ElseIf needVolume Then
        volume=CInt(arg)
        needVolume=0
    ElseIf needSamples Then
        samples=CInt(arg)
        needSamples=0
    ElseIf needChannels Then
        channels=CInt(arg)
        needChannels=0
'    ElseIf needWord Then
'        pWord=arg
'        needWord=0
'        If doLexiconAdd Then
'            needPron=1
'        End If
'    ElseIf needPron Then
'        pPron=arg
'        needPron=0
    ElseIf needVoice Then
        pVoice=arg
        needVoice=0
    ElseIf needEncoding Then
        pEncoding=arg
        needEncoding=0
        pEncoding = UCase(pEncoding)
        If pEncoding <> "UTF-8" And pEncoding <> "UTF-16LE" Then
            IsOK=0
        End If
    ElseIf arg = "-w" Then
        If (doWaveFile Or doMultWaveFiles) Then
            IsOK=0
        doWaveFile = 1
        needFileName=1
        End If
    ElseIf arg = "-m" Then
        If (doWaveFile Or doMultWaveFiles) Then
            IsOK=0
        End If
        doMultWaveFiles = 1
        needFileName=1
    ElseIf arg = "-r" Then
        needRate=1
    ElseIf arg = "-v" Then
        needVolume=1
    ElseIf arg = "-s" Then
        needSamples=1
    ElseIf arg = "-c" Then
        needChannels=1
    ElseIf arg = "-u" Then
        needUnicodeFileName=1
'    ElseIf arg = "-la" Then
'        doLexiconAdd=1
'        needWord=1
    ElseIf arg = "-vl" Then
        doListVoices=1
    ElseIf arg = "-voice" Then
        needVoice=1
    ElseIf arg = "-e" Then
        needEncoding=1
    ElseIf Mid(arg,1,1) = "-" Then
        IsOK=0
    Else 
        IsOK=0
    End If
Next


' Release
set argv = Nothing

if needFileName Or needVolume Or needRate  _
     Or needUnicodeFileName Or needSamples Or needChannels Or needVoice Then
    IsOK=0
End If
if IsOK And doListVoices then
    print ("ListVoices")
    ' count = listVoices(&ppszDescription,NULL)
    ' for (ix=0ix < count ix++) {
    '     fwprintf(stdout,L"%s",ppszDescription[ix])    
    ' }
End If

If IsOK Then
    IsOK = doit
Else 
    printErr("Usage: ptts [options]")
    printErr("")
    printErr("Reads from standard input and generates speech")
    printErr("")
    printErr("Options:")
    printErr("-w filename  create wave file instead of outputting sound")
    printErr("-m filename  multiple wave files")
    printErr("             new wave file after each empty input line")
    printErr("             appends nnnnn.wav to the filename.")
    printErr("-r rate      Speech rate -10 to +10, default is 0.")
    printErr("-v volume    Volume as a percentage, default is 100.")
    printErr("-s samples   Samples per sec for wav file, default is 44100.")
    printErr("-c channels  Channels (1 or 2) for wav file, default is 2.")
    printErr("-u filename  Read text from file instead of stdin.")
    printErr("-e encoding  File encoding. UTF-8, UTF-16LE.")
    printErr("             Default ANSI or as indicated by BOM in file.")
    printErr("-voice xxxx  Voice to be used.")
    printErr("-vl          List voices.")
    printErr("XML can be included in the input text to control the speech.")
    printErr("For details see the Microsoft speech API.")
End If

if IsOK Then
    rc = 0
Else
    rc = 99
End If

WScript.Quit (rc)


Sub printOut (text)
    WScript.StdOut.WriteLine(text)
End Sub

Sub printErr (text)
    WScript.StdErr.WriteLine(text)
End Sub

Function doit

    ' wchar_t fileName[256];
    ' wchar_t *pBuffer;
    ' pBuffer = (wchar_t*)malloc(BUFFER_SIZE * sizeof (wchar_t));
    ' int bufferUsed=0;
    ' int prevBufferUsed=0;
    ' int eof=0;
    ' int waveSeq=0;
    ' int hSpeaker=0;
    ' int IsOK=1;
    ' FILE *pInFile;
    ' wchar_t fileMode[256];
' 
' 
    ' if (!doMultWaveFiles) {
        ' if (pFileName == 0)
            ' hSpeaker = createSpeaker(0);
        ' else {
            ' swprintf(fileName,L"%s.wav",pFileName);
            ' hSpeaker = createSpeaker(fileName);
        ' }
        ' if (hSpeaker == 0) {
            ' fprintf(stderr,"hSpeaker is 0.\n");
            ' return 0;
        ' }
        ' else {
            ' if (rate != -999)
                ' IsOK=setRate(hSpeaker, rate);
            ' if (IsOK && volume != -999)
                ' IsOK=setVolume(hSpeaker, volume);
            ' if (!IsOK) {
                ' fprintf(stderr,"Set rate %d or volume %d failed.\n",rate,volume);
                ' return 0;
            ' }
            ' if (IsOK && pVoice != NULL)
                ' setVoice(hSpeaker,pVoice);
        ' }
    ' }
' 
    ' if (pUnicodeFileName != 0) {
        ' wcscpy(fileMode,L"r, ccs=");
        ' if (pEncoding == 0) 
            ' wcscat(fileMode, L"UNICODE");
        ' else
            ' wcscat(fileMode, pEncoding);
        ' pInFile = _wfopen(pUnicodeFileName, fileMode);
        ' if (pInFile==0) {
            ' wprintf(L"Unable to open input file %s\n",pUnicodeFileName);
            ' return 0;
        ' }
    ' }
    ' else
        ' pInFile=stdin;
' 
    ' while (!eof) {
        ' bufferUsed=0;
        ' prevBufferUsed=0;
        ' wchar_t* endBufferUsed;
        ' while(bufferUsed<BUFFER_SIZE) {
            ' pBuffer[bufferUsed]=0;
            ' wchar_t *ret = fgetws(pBuffer+bufferUsed,BUFFER_SIZE-bufferUsed,pInFile);
            ' endBufferUsed = wcschr(pBuffer, L'\n');
            ' if (endBufferUsed == 0) 
                ' endBufferUsed = wcschr(pBuffer, L'\0');
            ' if (endBufferUsed == 0 || ret == 0) {
                ' eof=1;
                ' break;
            ' }
            ' // replace newline with space
            ' if (*endBufferUsed == L'\n')
                ' *endBufferUsed=L' ';
            ' prevBufferUsed = bufferUsed;
            ' bufferUsed = endBufferUsed - pBuffer;
            ' if (bufferUsed==prevBufferUsed)
                ' break;
            ' bufferUsed++;
        ' }
        ' // Check if there is anything to say
        ' wchar_t *pChar=pBuffer;
        ' while (!iswalnum(*pChar)&&*pChar!=0)
            ' pChar++;
        ' if (*pChar==0)
            ' continue;
        ' // We now have something to say
        ' if (doMultWaveFiles) {
            ' swprintf(fileName,L"%s%5.5d.wav",pFileName,++waveSeq);
            ' hSpeaker = createSpeaker(fileName);
            ' if (hSpeaker == 0)
                ' return 0;
            ' else {
                ' if (rate != -999)
                   ' IsOK=setRate(hSpeaker, rate);
                ' if (IsOK && volume != -999)
                    ' IsOK=setVolume(hSpeaker, volume);
                ' if (!IsOK)
                    ' return 0;
            ' }
        ' }
        ' Speak(hSpeaker,pBuffer);
        ' if (doMultWaveFiles) {
            ' closeSpeaker(hSpeaker);
            ' hSpeaker=0;
        ' }
    ' }
    ' if (hSpeaker) {
        ' closeSpeaker(hSpeaker);
        ' hSpeaker=0;
    ' }
' 
    ' closeDown();
    ' return 1;

End Function
