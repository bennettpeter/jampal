#!/bin/bash

#   Copyright 2006-2011 Peter G Bennett
#
#   This file is part of Jampal.
#
#   Jampal is free software: you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation, either version 3 of the License, or
#   (at your option) any later version.
#
#   Jampal is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#   along with Jampal.  If not, see <http://www.gnu.org/licenses/>.

set -e
scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
. "$scriptpath/mp3.profile"


mkdir -p "$TEMPDIR"
streamfile=$TEMPDIR/ptts_input_stream.txt
paramfile=$TEMPDIR/ptts_input_stream.profile

engine=$SPEECH_ENGINE
# See if a speech engine has been provided

if [[ "$1" = "-engine" ]] ; then
    engine="$2"
    shift; shift
fi

case $1 in 
    -batch)
        shift
        ;;
    -nobatch)
        shift
        ;;
    -start)
        ;;
    -stop)
        ;;
    *)
        "$scriptpath/ptts.sh" -engine "$engine" -start
        "$scriptpath/ptts.sh" -engine "$engine" -nobatch "$@"
        "$scriptpath/ptts.sh" -engine "$engine" -stop
        exit
        ;;
esac

if [[ "$1" = -start ]]; then
    case $engine in
        FreeTTS)
            cat /dev/null > "$streamfile"
            cat /dev/null > "$paramfile"
            ;;
    esac
    exit
fi

if [[ "$1" = -stop ]]; then
    case $engine in
        FreeTTS)
            echo "#exit" >> $streamfile
            . "$paramfile"
            "$JAVA_COMMAND" \
                "-Dmbrola.base=$MBROLA_HOME" \
                $encoding \
                -cp "$JAMPAL_CLASSPATH" \
                pgbennett.speech.BatchSpeaker < $streamfile
            
            echo BatchSpeaker complete
            ;;
    esac
    exit
fi

case $engine in
    FreeTTS)
        do_cat=Y
        while (( $# != 0 )); do
            case $1 in
            -w)
                rm -rf "$2"
                if [[ -f "$2" ]]; then
                    echo "Unable to delete old file $2"
                    exit 2
                fi
                echo "#file $2" >> "$streamfile"
                shift;shift
                ;;
            -r)
                echo "#rate $2" >> "$streamfile"
                shift;shift
                ;;
            -v)
                echo "#volume $2" >> "$streamfile"
                shift;shift
                ;;
            -voice)
                echo "#voice $2" >> "$streamfile"
                shift;shift
                ;;
            -vl)
                echo "#listvoices" >> "$streamfile"
                do_cat=N
                break
                ;;
            -e)
                echo encoding="-Dfile.encoding=$2" >> "$paramfile"
                shift;shift
                ;;
            *)
                echo "ptts.sh - Invalid parameter $1"
                echo "Use -engine xxx as first parameter to change engine"
                echo "Valid engines are eSpeak, Microsoft, Cepstral, FreeTTS"
                echo "$0 usage for engine $engine:"
                echo "-start       start process"
                echo "-stop        stop process"
                echo "-w filename  create wave file instead of outputting sound"
                echo "-r rate      Speech rate -10 to +10, default is 0."
                echo "-v volume    Volume as a percentage, default is 100."
                echo "-voice xxxx  Voice to be used."
                echo "-e xxxx      input file encoding."
                echo "-vl          List voices."
                echo "After this, for all except -start and -stop, read standard input"
                echo "for text to be spoken"
                exit 2
                ;;
            esac
        done

        if [[ "$do_cat" = Y ]]; then
            cat  >> "$streamfile"
        fi
        echo "#closefile"  >> "$streamfile"
        ;;
    Microsoft)
        command[0]="cscript"
        command[1]=`cygpath -w "$JAMPAL_HOME/ptts.vbs"`
        index=1
        encoding=
        while (( $# != 0 )); do
            case $1 in
            -voice)
                command[++index]="$1"
                if [[ "${2#\*\*}" != "$2" ]] ; then
                    command[++index]="${2#\*\*}"
                    command[0]="$SYSTEMROOT/SysWOW64/cscript"
                else
                    command[++index]="$2"
                fi
                shift;shift
                ;;
            -e)
                encoding="$2"
                command[++index]="-e"
                command[++index]="UTF-16LE"
                shift;shift
                ;;
            *)
                command[++index]="$1"
                shift
                ;;
            esac
        done
        streamfile1=$TEMPDIR/ptts_input_stream1.$$.txt
        streamfile2=$TEMPDIR/ptts_input_stream2.$$.txt
        cat > "$streamfile1"
        echo >> "$streamfile1"
        echo >> "$streamfile1"
        iconv -f "$encoding" -t UTF-16LE "$streamfile1" > "$streamfile2"
        command[++index]="-u"
        command[++index]="$streamfile2"
        "${command[@]}"
        rm -f "$streamfile1" "$streamfile2"
        ;;
    Cepstral)
        cepstral[0]="$CEPSTRAL_HOME/bin/swift"
        index=0
        cparm=
        while (( $# != 0 )); do
            case $1 in
            -w)
                rm -rf "$2"
                if [[ -f "$2" ]]; then
                    echo "Unable to delete old file $2"
                    exit 2
                fi
                cepstral[++index]="-o"
                cepstral[++index]="$2"
                shift;shift
                if [[ "$cparm" = "" ]]; then
                    cparm="audio/channels=2"
                else
                    cparm="$cparm,audio/channels=2"
                fi
                ;;
            -s)
                if [[ "$cparm" = "" ]]; then
                    cparm="audio/sampling-rate=$2"
                else
                    cparm="$cparm,audio/sampling-rate=$2"
                fi
                shift;shift
                ;;
            -r)
                let rateWPM="($2+11)*15"
                if [[ "$cparm" = "" ]]; then
                    cparm="speech/rate=$rateWPM"
                else
                    cparm="$cparm,speech/rate=$rateWPM"
                fi
                shift;shift
                ;;
            -v)
                if [[ "$cparm" = "" ]]; then
                    cparm="audio/volume=$2"
                else
                    cparm="$cparm,audio/volume=$2"
                fi
                shift;shift
                ;;
            -voice)
                cepstral[++index]="-n"
                cepstral[++index]="$2"
                shift;shift
                ;;
            -vl)
                "$CEPSTRAL_HOME/bin/swift" --voices
                exit
                ;;
            -e)
                cepstral[++index]="-e"
                cepstral[++index]="$2"
                shift;shift
                ;;
            *)
                echo "ptts.sh - Invalid parameter $1"
                echo "Use -engine xxx as first parameter to change engine"
                echo "Valid engines are eSpeak, Microsoft, Cepstral, FreeTTS"
                echo "$0 usage for engine $engine:"
                echo "-start       start process"
                echo "-stop        stop process"
                echo "-w filename  create wave file instead of outputting sound"
                echo "-r rate      Speech rate -10 to +10, default is 0."
                echo "-s xxxx      Sample rate."
                echo "-v volume    Volume as a percentage, default is 100."
                echo "-voice xxxx  Voice to be used."
                echo "-vl          List voices."
                echo "-e xxxx      input file encoding."
                echo "After this, for all except -start and -stop, read standard input"
                echo "for text to be spoken"
                exit 2
                ;;
            esac
        done
        if [[ "$cparm" != "" ]]; then
            cepstral[++index]="-p"
            cepstral[++index]="$cparm"
        fi
        streamfile=$TEMPDIR/ptts_input_stream.$$.txt
        cat > "$streamfile"
        echo >> "$streamfile"
        echo >> "$streamfile"
        cepstral[++index]="-f"
        cepstral[++index]="$streamfile"
        "${cepstral[@]}"
        rm -f "$streamfile"
        ;;
    eSpeak)
        espeak[0]="$ESPEAK_PROGRAM"
        espeak[1]="-m"
        index=1
        wavefile=
        while (( $# != 0 )); do
            case $1 in
            -w)
                rm -rf "$2"
                if [[ -f "$2" ]]; then
                    echo "Unable to delete old file $2"
                    exit 2
                fi
                wavefile="$2"
                shift;shift
                ;;
            -r)
                let rateWPM="($2+11)*15"
                espeak[++index]="-s"
                espeak[++index]="$rateWPM"
                shift;shift
                ;;
            -v)
                espeak[++index]="-a"
                espeak[++index]="$2"
                shift;shift
                ;;
            -voice)
                voice="$2"
                shift;shift
                ;;
            -vl)
                "$ESPEAK_PROGRAM" --voices
                exit
                shift
                ;;
            -e)
                shift;shift
                ;;
            -s)
                shift;shift
                ;;
            *)
                echo "ptts.sh - Invalid parameter $1"
                echo "Use -engine xxx as first parameter to change engine"
                echo "Valid engines are eSpeak, Microsoft, Cepstral, FreeTTS"
                echo "$0 usage for engine $engine:"
                echo "-start       start process"
                echo "-stop        stop process"
                echo "-w filename  create wave file instead of outputting sound"
                echo "-r rate      Speech rate -10 to +10, default is 0."
                echo "-v volume    Volume as a percentage, default is 100."
                echo "-voice xxxx  Voice to be used."
                echo "-vl          List voices."
                echo "input file encoding is UTF-8."
                echo "After this, for all except -start and -stop, read standard input"
                echo "for text to be spoken"
                exit 2
                ;;
            esac
        done
        streamfile=$TEMPDIR/ptts_input_stream.$$.txt
        rm -f "$streamfile"
        if [[ "$voice" != "" && "$voice" != mb-* ]]; then
            echo '<voice name="'"$voice"'">' >> "$streamfile"
        fi
        cat >> "$streamfile"
        echo >> "$streamfile"
        echo >> "$streamfile"
        espeak[++index]="-f"
        espeak[++index]="$streamfile"
        if [[ "$voice" = mb-* ]]; then
            fixvoice="${voice%% *}"
            espeak[++index]="-v"
            espeak[++index]="$fixvoice"
            mbrolalang="${voice:3:3}"
            "${espeak[@]}" | "$MBROLA_PROGRAM" -e "$MBROLA_HOME/$mbrolalang/$mbrolalang" - "$wavefile"
        else
            #if [[ "$voice" != "" ]]; then
            #    espeak[++index]="-v"
            #    espeak[++index]="$voice"
            #fi
            if [[ "$wavefile" != "" ]] ; then
                espeak[++index]="-w"
                espeak[++index]="$wavefile"
            fi
            "${espeak[@]}"
        fi
        rm -f "$streamfile"
        ;;
    *)
        echo "ERROR Unknown Speech Engine $engine"
        echo "Use -engine xxx as first parameter to change engine"
        echo "Valid engines are eSpeak, Microsoft, Cepstral, FreeTTS"
        echo "$0 usage:"
        echo "-engine name"
        echo "-start       start process"
        echo "-stop        stop process"
        echo "-w filename  create wave file instead of outputting sound"
        echo "-r rate      Speech rate -10 to +10, default is 0."
        echo "-v volume    Volume as a percentage, default is 100."
        echo "-voice xxxx  Voice to be used."
        echo "-vl          List voices."
        echo "-e xxxx      input file encoding."
        echo "After this, for all except -start and -stop, read standard input"
        echo "for text to be spoken"
        echo "for ESpeak input file encoding is always UTF-8."
        echo "After this, for all except -start and -stop, read standard input"
        echo "for text to be spoken"
        exit 2
        ;;
esac
