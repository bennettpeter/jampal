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

# To restore a cd - 
# cd /r (cdrom)
# mkdir -p $MAINDIR/000904_1758_rest
# tagbkup -v -r -x -d $MAINDIR/000904_1758_rest */*.mp3 $TAGBKUP/tagbkup

set -e
scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
. "$scriptpath/mp3.profile"



DATETIME=`date "+%Y%m%d_%H%M%S"`
progpath=/usr/local/bin
backuppath="$TAGBKUP/tagbkup"
rejectpath="$TAGBKUP/tagbkup_reject"
logpath="$TAGBKUP/logs"

libfile="$MAINLIBNAME"

listfile="$logpath/${DATETIME}_tagbkup_full_list.txt"
outfile="$logpath/${DATETIME}_tagbkup.out"
logfile=$logpath/${DATETIME}_tagbkup.log
opt="$1"

if [[ ! -d "$backuppath" ]]; then
    mkdir -p "$backuppath"
fi
if [[ ! -d "$rejectpath" ]]; then
    mkdir -p "$rejectpath"
fi
if [[ ! -d "$logpath" ]]; then
    mkdir -p "$logpath"
fi


if [[ "$libfile" = `basename "$libfile"` ]]; then
    libfile="$LIBDIR/$libfile"
fi

libname=`basename $libfile`
"$scriptpath/make_library_custom_files.sh" "$libfile"

. "$TEMPDIR/$libname.profile"

if [[ "$LIBRARYDATANAME" = `basename "$LIBRARYDATANAME"` ]]; then
    LIBRARYDATANAME=`dirname $libfile`/"$LIBRARYDATANAME"
fi

if [[ "$JFILENAME" = "" ]]; then echo "ERROR - FILENAME is required in the $libname library"; exit 2; fi

if [[ "$opt" = bak ]]; then
    rm -f "$listfile"
    echo "awk ..."
    awk '
    BEGIN { 
        FS="\t"
    }
    /\r$/ {
        # Remove any trailing carriage return
        sub(/\r$/,"")
    }
    
    {

        # assign field names in database
        '"`cat "$TEMPDIR/$libname.awkset"`"'

        if (substr($1,1,1) != "A") {
            print "Library needs to be saved: " JFILENAME
            exit 2
        }
    }
    $1 == "A" || $1 == "A0" {
        print JFILENAME > listfile
    } ' "listfile=$listfile" "$LIBRARYDATANAME"

    tagbkup=tagbkup
#    if [[ `uname -s` = CYGWIN* ]]; then
#        tagbkup=tagbkup_cygwin
#    fi

    echo "tagbkup ..."
	rm -f "$logpath/rc.out"
    (ret=0 ; "$tagbkup" -b -u -y -x -f "$listfile" \
      "$backuppath" 2>&1 || ret=$? ; echo $ret >  "$logpath/rc.out" )|tee $outfile
    if [[ `cat "$logpath/rc.out"` != 0 ]]; then echo "ERROR"; exit 2; fi
    cp -f $backuppath/tagbkup.log $logfile
    date +%Y%U >$logpath/last_run.txt
fi

ret=0
egrep "_00[^0] /" "$backuppath/tagbkup.log" || ret=$?
if [[ "$ret" = 0 ]]; then 
    echo "Duplicate files found"
fi

mkdir -p "$TEMPDIR"
filelist=$TEMPDIR/filelist.log
cat /dev/null > $filelist
sort < "$backuppath/tagbkup.log" > "$TEMPDIR/tagbkup.log.sort"
cd "$backuppath"
find . -name '*.mp3' > "$TEMPDIR/tagbkup_unsorted.lst"
sort <  "$TEMPDIR/tagbkup_unsorted.lst" >  "$TEMPDIR/tagbkup.lst"
echo "" > "$TEMPDIR/rename.sh"
chmod +x "$TEMPDIR/rename.sh"

awk '
BEGIN {
    logentry=""
}
# Main input file is the list of backup files
# e.g. tag_0b2cddac8fa5d896_000.mp3
/tag_................_00.\.mp3/ {
    filename=$0
    while (logentry < filename) {
        rc = getline < logfile
        if (rc == 1)
            logentry = "./" substr($1,1,2) "/tag_" $1 ".mp3"
        else
            logentry = "zzz"
    }
    if (logentry != filename) {
        pathsplitnum=split(filename,pathsplit,"/")
        fn = pathsplit[pathsplitnum]
        print "mv -fv " backuppath "/" filename " " rejectpath "/" fn > outfile
        print backuppath "/" filename > filelist
    }
}' "rejectpath=$rejectpath" "outfile=$TEMPDIR/rename.sh" \
   "logfile=$TEMPDIR/tagbkup.log.sort" "backuppath=$backuppath" \
   "filelist=$filelist" \
   "$TEMPDIR/tagbkup.lst"

while [[ true ]]; do 
    echo "Results in $TEMPDIR/rename.sh, Errors in $TEMPDIR/rename.out"
    echo Script is ready to remove tag backup of rejected songs.
    echo Type yes to run the script, vi or name of editor to edit it, 
    echo names to get the song titles, or enter to exit.
    read -e resp
    case $resp in 
        "")
            exit
            ;;
        yes)
            "$TEMPDIR/rename.sh"
            exit
            ;;
        names)
            echo;echo
            "$JAVA_COMMAND" -cp "$JAMPAL_CLASSPATH" pgbennett.id3.TagUpdate \
                -SHORTDISPLAY `cat "$filelist"` 
            echo;echo
            ;;
        *)
            "$resp" "$TEMPDIR/rename.sh"
            ;;
    esac
done

# echo Now Run tagbkup_analyse to see if there are any files that are on your hard drive but not on CD.
