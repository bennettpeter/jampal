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

doWaveFile = False
doMultWaveFiles = False
pFileName = Null            ' Name of output wav file
pUnicodeFileName = Null     ' name of input text file
rate=-999
volume=-999
samples=44100
channels=2
pVoice=Null
pEncoding = Null
needFileName = False
needRate = False
needVolume = False
needWord = False
needPron = False
needUnicodeFileName = False
needSamples = False
needChannels = False
needVoice = False
needEncoding = False
doInstruct = False
doLexiconAdd = False
doListVoices = False


Dim IsOK
IsOK = True

Dim argv
Set argv = WScript.Arguments

printErr("ptts Version 1.28 (c) 2011 Peter G. Bennett")

For Each arg in argv
    If needFileName Then
        pFileName=arg
        needFileName = False
    ElseIf needUnicodeFileName Then
        pUnicodeFileName=arg
        needUnicodeFileName = False
    ElseIf needRate Then
        rate=CInt(arg)
        needRate = False
    ElseIf needVolume Then
        volume=CInt(arg)
        needVolume = False
    ElseIf needSamples Then
        samples=CLng(arg)
        needSamples = False
    ElseIf needChannels Then
        channels=CInt(arg)
        needChannels = False
    ElseIf needVoice Then
        pVoice=arg
        needVoice = False
    ElseIf needEncoding Then
        pEncoding=arg
        needEncoding = False
        pEncoding = UCase(pEncoding)
        If pEncoding <> "ASCII" And pEncoding <> "UTF-16LE" Then
            IsOK=False
        End If
    ElseIf arg = "-w" Then
        If (doWaveFile Or doMultWaveFiles) Then
            IsOK=False
        End If
        doWaveFile = True
        needFileName = True
    ElseIf arg = "-m" Then
        If (doWaveFile Or doMultWaveFiles) Then
            IsOK=False
        End If
        doMultWaveFiles = True
        needFileName = True
    ElseIf arg = "-r" Then
        needRate = True
    ElseIf arg = "-v" Then
        needVolume = True
    ElseIf arg = "-s" Then
        needSamples = True
    ElseIf arg = "-c" Then
        needChannels = True
    ElseIf arg = "-u" Then
        needUnicodeFileName = True
    ElseIf arg = "-vl" Then
        doListVoices = True
    ElseIf arg = "-voice" Then
        needVoice = True
    ElseIf arg = "-e" Then
        needEncoding = True
    ElseIf Mid(arg,1,1) = "-" Then
        IsOK=False
    Else 
        IsOK=False
    End If
'    printErr("Arg:"&arg& " IsOK:"&IsOK&" doWaveFile:"& _
'    doWaveFile&" doMultWaveFiles:"&doMultWaveFiles& _
'    " needFileName:"&needFileName)
Next


' Release
set argv = Nothing


if needFileName Or needVolume Or needRate  _
     Or needUnicodeFileName Or needSamples Or needChannels Or needVoice Then
    IsOK=False
End If
if IsOK And doListVoices then
    Set hSpeaker = createSpeaker(Null)
    For Each strVoice in hSpeaker.GetVoices
        printOut(strVoice.GetDescription)
    Next
    Call closeSpeaker(hSpeaker)
End If
if IsOK And Not IsNull(pFileName) Then
    If Len(pFileName) > 4 _
        And LCase(Mid(pFileName,Len(pFilename) -3)) = ".wav" Then
        pFileName = Mid(pFileName,1,Len(pFilename) -4)
    End If
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
    eof = False
    ' int waveSeq=0;
    ' hSpeaker is the SAPI.SpVoice
    hSpeaker = Null
    IsOK=True
    ' FILE *pInFile;
    ' wchar_t fileMode[256];

    if  Not doMultWaveFiles Then
        if IsNull(pFileName) Then
            Set hSpeaker = createSpeaker(Null)
        else
            Set hSpeaker = createSpeaker(pFileName & ".wav")
        End If
        if IsNull(hSpeaker) Then
            printErr("ERROR hSpeaker is Null")
            doit = False
            Exit Function
        else 
            if rate <> -999 Then
                IsOK=setRate(hSpeaker, rate)
            End If
            if IsOK And volume <> -999 Then
                IsOK=setVolume(hSpeaker, volume)
            End If
            if Not IsOK Then
                printErr("Set rate " & rate & _
                    " or volume " & volume & " failed." & IsOK)
                doit = False
                Exit Function
            End If
            if IsOK And Not IsNull(pVoice) Then
                setVoice hSpeaker,pVoice
            End If
        End If
    End If

    Const ForReading = 1, ForWriting = 2, ForAppending = 8

    if Not IsNull(pUnicodeFileName) Then
        Set fso = CreateObject("Scripting.FileSystemObject")
        If pEncoding = "UTF-16LE" Then
            fileMode = -1
        ElseIf pEncoding = "ASCII" Then
            fileMode = 0
        Else
            fileMode = -2 'System Default
        End If
        Set pInFile = fso.OpenTextFile(pUnicodeFileName, ForReading, False, fileMode)
        If IsNull(pInFile) Then
            PrintErr("Unable to open input file "& pUnicodeFileName)
            doit = False
            Exit Function
        End If
    Else
        Set pInFile = WScript.StdIn
    End If

    While Not pInFile.AtEndOfStream
        textLine = pInFile.ReadLine
'        // We now have something to say
        If doMultWaveFiles Then
            waveSeq = waveSeq + 1
            fnNumber = "00000" & CStr(waveSeq)
            fnNumber = Mid (fnNumber, Len(fnNumber) - 4)
            fileName = pFileName & fnNumber & ".wav"
            Set hSpeaker = createSpeaker(fileName)
            if IsNull(hSpeaker) Then
                printErr("ERROR hSpeaker is Null")
                doit = False
                Exit Function
            Else 
                if rate <> -999 Then
                    IsOK=setRate(hSpeaker, rate)
                End If
                if IsOK And volume <> -999 Then
                    IsOK=setVolume(hSpeaker, volume)
                End If
                if Not IsOK Then
                    printErr("Set rate " & rate & _
                        " or volume " & volume & " failed." & IsOK)
                    doit = False
                    Exit Function
                End If
            End If
        End If
        Call Speak(hSpeaker,textLine)
        If doMultWaveFiles Then
            Call closeSpeaker(hSpeaker)
        End If
    Wend
    If Not IsNull(hSpeaker) Then
        Call closeSpeaker(hSpeaker)
    End If

'    closeDown();
    doit = True
End Function

outputFile = Null
Const SVSFlagsAsync = 1 
const SVSFPurgeBeforeSpeak = 2 

Function createSpeaker(filename)
    ' printErr("createSpeaker " & filename)
    Dim voice
    outputFile = Null
    Set voice = CreateObject("SAPI.SpVoice")
    If Not IsNull(filename) Then
        Set outputFile = CreateObject("SAPI.SpFileStream")
        Set format = CreateObject("SAPI.SpAudioFormat")
        ' Need to fix this for samples and channels
        format.Type = 35
        Set outputFile.Format = format
        outputFile.Open filename, 3
        Set voice.AudioOutputStream = outputFile
    End If
    Set createSpeaker = voice
End Function

Function Speak(hSpeaker, text)
    printErr("Speak " & text)
    On Error Resume Next
    hSpeaker.Speak text, SVSFlagsAsync + SVSFPurgeBeforeSpeak
    Do
            Sleep 100
    Loop Until hSpeaker.WaitUntilDone(10)

    Speak = True
End Function

Function setRate(hSpeaker, rate)
    setRate = True
End Function

Function setVolume(hSpeaker, volume)
    setVolume = True
End Function

Function setVoice(hSpeaker, voice)
    setVoice = True
End Function


Function closeSpeaker(hSpeaker)
    Set hSpeaker = Nothing
    hSpeaker = Null
    Set outputFile = Nothing
    outputFile = Null
    closeSpeaker = True
End Function
