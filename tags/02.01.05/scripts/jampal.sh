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

ret=0
case $1 in
    open)
        shift||rc=$?||rc=$?
        "$JAVA_COMMAND"  -Xincgc -Xmx256M $JAMPAL_JVM_OPTIONS \
            -classpath "$JAMPAL_CLASSPATH" \
            "-Djampal.libdir=$LIBDIR" pgbennett.jampal.Jampal "$@" >/dev/null 2>/dev/null &
        ;;
    cleanup)
        shift||rc=$?
        "$scriptpath/mp3lib.sh" "$@"
        exit
        ;;
    playlist)
        shift||rc=$?
        unset sort reverse announce tracksperdir startdir library ann_merge
        unset speak speak_sep dirchange appendseq cdsize stagingdir
        unset filenumberstart playlistname tagupdate startdisknum
        condition="$1"
        shift||rc=$?
        while (( "$#" >= 1 )) ; do
            case $1 in
                --sort)
                    sort="$2"
                    shift||rc=$?
                    ;;
                --reverse)
                    reverse="-r"
                    ;;
                --announce)
                    announce=Y
                    ;;
                --tracksperdir)
                    tracksperdir="$2"
                    shift||rc=$?
                    ;;
                --startdir)
                    startdir="$2"
                    shift||rc=$?
                    ;;
                --library)
                    library="$2"
                    shift||rc=$?
                    ;;
                --ann_separate)
                    ann_merge=N
                    ;;
                --speak)
                    speak="$2"
                    shift||rc=$?
                    ;;
                --speak_sep)
                    speak_sep="$2"
                    shift||rc=$?
                    ;;
                --dirchange)
                    dirchange="$2"
                    shift||rc=$?
                    ;;
                --appendseq)
                    appendseq=Y
                    ;;
                --cdsize)
                    cdsize="$2"
                    shift||rc=$?
                    ;;
                --stagingdir)
                    stagingdir="$2"
                    shift||rc=$?
                    ;;
                --filenumberstart)
                    filenumberstart="$2"
                    shift||rc=$?
                    ;;
                --playlistname)
                    playlistname="$2"
                    shift||rc=$?
                    ;;
                --tagupdate)
                    tagupdate="$2"
                    shift||rc=$?
                    ;;
                --startdisknum)
                    startdisknum="$2"
                    shift||rc=$?
                    ;;
            esac
            shift||rc=$?
        done
        if [[ "$condition" = "" ]] ; then
            echo "Build a customized playlist with options for burning"
            echo "to a CD or transferring to a player"
            echo "jampal playlist criteria [options] ..."
            echo "Options (defaults in parens):"
            echo "--sort sort-field"
            echo "--reverse"
            echo "--announce"
            echo "--tracksperdir nnn (100)"
            echo "--startdir nnn (2)"
            echo "--library library_name (MAINLIBNAME from jampal.conf)"
            echo "--ann_separate"
            echo '--speak (TITLE ". " ARTIST)'
            echo "--speak_sep"
            echo "--dirchange condition"
            echo "--appendseq"
            echo "--cdsize nnn (690)"
            echo "--stagingdir (CDSTAGING from jampal.conf)"
            echo "--filenumberstart nnn (1)"
            echo "--playlistname name"
            echo "--tagupdate tag_setting"
            echo "--startdisknum nnn (1)"
            exit 2
        fi
        "$scriptpath/playlist.sh" "$condition" "$sort" "$reverse" "$announce" \
            "$tracksperdir" "$startdir" "$library" "$ann_merge" "$speak" \
            "$speak_sep" "$dirchange" "$appendseq" "$cdsize" "$stagingdir" \
            "$filenumberstart" "$playlistname" "$tagupdate" "$startdisknum"
        ;; 
    fieldcodes)
        shift||rc=$?
        "$scriptpath/make_library_custom_files.sh" "$1"
        libname="$MAINLIBNAME"
        if [[ "$1" != "" ]]; then
            libname=$1
            if [[ "$1" = `basename "$1"` ]]; then
                libname="$LIBDIR/$1"
            fi
        fi
        outputname=$TEMPDIR/`basename "$libname"`
        echo "Below are the field codes for your library."
        echo "Use the capitalized names after the colons in the playlist command."
        cat "$outputname".fieldcodes
        ;;
    playlistcd)
        shift||rc=$?
        "$scriptpath/playlistcd.sh" "$@"
        ;;
    envelope)
        shift||rc=$?
        "$scriptpath/mp3cover.sh" "$@"
        ;;
    duplicates)
        shift||rc=$?
        "$scriptpath/mp3dup.sh" "$@"
        ;;
    tagbkup)
        shift||rc=$?
        "$scriptpath/tagbkup.sh" "$@"
        ;;
    tagupdate)
        shift||rc=$?
        "$JAVA_COMMAND" -cp "$JAMPAL_CLASSPATH" pgbennett.id3.TagUpdate "$@"
        ;;
    *)
        command="$1"
        if [[ "$command" = "" ]] ; then 
            ret=2;
        else 
            shift||rc=$?
            ret=0
            "$scriptpath/$command.sh" "$@" || ret=$?
        fi
        ;;
esac
if [[ "$ret" != 0 ]] ; then
    echo "Usage"
    echo "jampal open [ library ] ..."
    echo "jampal cleanup [ -reorder ] library [ moved-file-dir ]"
    echo "jampal playlist  condition [ options ] ..."
    echo "jampal fieldcodes [ library ]"
    echo "jampal playlistcd  disknum [ staging-dir ]"
    echo "jampal duplicates [ --NODUP ] [ --NEXTFILES ]"
    echo "jampal tagbkup  [ bak ]"
    echo "jampal tagupdate options filename [ filename ] ..."
    echo "jampal mp3lame [ -recode|-recode-even ]"
    exit $ret
fi
