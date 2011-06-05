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

# Check jampal library for duplicate songs
# This productes a report plus a m3u file to load into the 
# dups jampal library.
# If songs are not actually duplicates but different versions that should be kept
# put a number from 1 up into the dup field

set -e
scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
. "$scriptpath/mp3.profile"


echo "Find duplicate songs in main library"
echo "Option: --NODUP to suppress use of Dup test tag"
echo "Option: --NEXTFILES to include files in $NEXTFILES directory"

DUP=Y
while (( $# > 0 )) ; do 
    case $1 in 
        --NODUP)
            DUP=N
            ;;
        --NEXTFILES)
            NEXTFILES=""
            ;;
        "")
            ;;
        *)
            echo "Invalid option $1"
            exit 2
            ;;
    esac
    shift
done

template=duplicate
templatefile="$LIBDIR/$template"

libfile="$MAINLIBNAME"
if [[ "$libfile" = `basename "$libfile"` ]]; then
    libfile="$LIBDIR/$libfile"
fi
libname=`basename $libfile`
"$scriptpath/make_library_custom_files.sh" "$libfile"

. $TEMPDIR/$libname.profile

if [[ "$LIBRARYDATANAME" = `basename "$LIBRARYDATANAME"` ]]; then
    LIBRARYDATANAME=`dirname $libfile`/"$LIBRARYDATANAME"
fi

if [[ "$TITLE" = "" ]]; then echo "ERROR - TITLE is required in the $libname library"; exit 2; fi
if [[ "$ARTIST" = "" ]]; then echo "ERROR - ARTIST is required in the $libname library"; exit 2; fi
if [[ "$JFILENAME" = "" ]]; then echo "ERROR - FILENAME is required in the $libname library"; exit 2; fi
if [[ "$ID3V2TAGTXXXDup" = "" ]]; then echo "WARNING - ID3V2TAGTXXXDup would be helpful in the $libname library"; fi

if [[ ! -f $templatefile.jampal ]]; then
    cp -f "$scriptpath/$template.jampal" "$templatefile.jampal"
fi

"$scriptpath/make_library_custom_files.sh" "$template"



txtfile=$TEMPDIR/dups.txt
playlist=$TEMPDIR/dups.m3u
rm -f $txtfile $playlist
mkdir -p $TEMPDIR


echo "Remove diacritics ..."
"$JAVA_COMMAND" -cp "$JAMPAL_CLASSPATH" pgbennett.utility.TextSimplifier \
    -i UTF-8 "$LIBRARYDATANAME" -o US-ASCII "$LIBRARYDATANAME.simplify"


echo "Create Annotated Library ..."

awk '

BEGIN { 
    FS="\t"
    OFS="\t"
}


/\r$/ {
    # Remove any trailing carriage return
    sub(/\r$/,"")
}

{
    getline utfrecord < utflibrary
    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'

    if (substr($1,1,1) != "A") {
        print "Library needs to be saved: " JFILENAME
        exit 2
    }
}

substr(JFILENAME,1,length(NEXTFILES)) == NEXTFILES {
    if (NEXTFILES != "")
        next
}

$1 == "A" || $1 == "A0" {
    artist = tolower(ARTIST)
    # Remove special chars, the word #a# and the word #the#
    sub(/^[^a-z0-9]*a[^a-z0-9]+|^[^a-z0-9]*the[^a-z0-9]+|^[^a-z0-9]+/,"",artist)
    gsub(/ & /," and ",artist);
    gsub(/[^a-z0-9]/,"",artist)

    if (artist == "maggiereilly")
        artist = "mikeoldfield"
    if (artist == "johnnyclegg")
        artist = "juluka"

    title = tolower(TITLE)
    # Remove parenthesized part at start of title
    sub(/^[^a-z0-9]*\([^\)]*\)/,"",title)

    # Remove other parenthesized part 
    gsub(/[\(\[][^\)\[]*[\)\]]/,"",title)

    # Change singin to singing, etc. 
    gsub(/in'"'"' /,"ing ",title)
    gsub(/in'"'"'$/,"ing",title)

    # Remove special chars, the word #a# and the word #the# at beginning
    sub(/^[^a-z0-9]*a[^a-z0-9]+|^[^a-z0-9]*the[^a-z0-9]+|^[^a-z0-9]+/,"",title)

    gsub(/oughta/,"ought to",title)
    gsub(/gonna/,"going to",title)

    gsub(/ & /," and ",title)
    gsub(/[^a-z0-9]/,"",title)

#    if (DUP == "Y") 
#        duppart = ID3V2TAGTXXXDup
    
    $0 = utfrecord
    # When concatenating include extra spaces before the pipe.
    # This prevents mixing up 
    # wrong  ----
    # the apple tree|
    # the apple|
    # right ----
    # the apple   |
    # the apple tree   |
    $1 = artist "   |" title "   |" ID3V2TAGTXXXDup "   |" $1

    print $0 > outfile
} ' "NEXTFILES=$NEXTFILES" "outfile=$TEMPDIR/dupcheck.dat" DUP=$DUP "utflibrary=$LIBRARYDATANAME" "$LIBRARYDATANAME.simplify"



echo "Sort Annotated Library ..."
sort -d -o "$TEMPDIR/dupcheck.sort" "$TEMPDIR/dupcheck.dat"

. "$TEMPDIR/$template.profile"

if [[ "$LIBRARYDATANAME" = `basename "$LIBRARYDATANAME"` ]]; then
    LIBRARYDATANAME=`dirname $libfile`/$LIBRARYDATANAME
fi
if [[ "$TITLE" = "" ]]; then echo "ERROR - TITLE is required in the $template library"; exit 2; fi
if [[ "$ARTIST" = "" ]]; then echo "ERROR - ARTIST is required in the $template library"; exit 2; fi
if [[ "$JFILENAME" = "" ]]; then echo "ERROR - FILENAME is required in the $template library"; exit 2; fi
if [[ "$ID3V2TAGTXXXDup" = "" ]]; then echo "WARNING - ID3V2TAGTXXXDup would be helpful in the $libname library"; fi

echo "Create Report ..."

awk '
# values in first field
# 1 = artist normalized
# 2 = title normalized
# 3 = dup
# 4 = status

BEGIN { 
    FS="\t"
    OFS="\t"
    priorartistnorm=""
    priortitlenorm=""
    priorfile=""
}

/\r$/ {
    # Remove any trailing carriage return
    sub(/\r$/,"")
}

{
    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'

    keycount = split($1,keyflds,"|")
    status = keyflds[keycount]
    thisartistnorm = keyflds[1]

    # trim
    gsub(/^ */,"",thisartistnorm)
    gsub(/ *$/,"",thisartistnorm)

    thistitlenorm = keyflds[2]

    # trim
    gsub(/^ */,"",thistitlenorm)
    gsub(/ *$/,"",thistitlenorm)

    thisdup = ID3V2TAGTXXXDup
    if (DUP == "Y")
        dupcheck = ID3V2TAGTXXXDup

    thisartist = ARTIST
    thistitle = TITLE
    thisfile = JFILENAME

    if (thisartistnorm != "various" \
    && thisartistnorm == priorartistnorm) {
        # If prior was abc and this is abc 3 then drop the  3
        # So that it is reported.
        if (thistitlenorm == priortitlenorm && priordup == "") {
            dupcheck = ""
        }
        if (thistitlenorm == priortitlenorm && dupcheck == priordup) {
            if (priorfile != "") {
                songnum++
                dupnum=1
                print priorfile > playlist
                saverec=$0
                $0=priorrec
                # assign field names in database
                '"`cat "$TEMPDIR/$libname.awkset"`"'
                $0=""
                $1=priorstatus
                DUMMY=sprintf("%5.5d:%3.3d",songnum,dupnum)
                # build output record
                '"`cat "$TEMPDIR/$template.awkset2"`"'
                print $0 > duplib
                $0=saverec
                # restore field names in database
                '"`cat "$TEMPDIR/$libname.awkset"`"'
#                print ""
#                print priorartist " - " priortitle
            }
            dupnum++
            print thisfile > playlist
            $0=""
            $1=status
            # build output record
            DUMMY=sprintf("%5.5d:%3.3d",songnum,dupnum)
            '"`cat "$TEMPDIR/$template.awkset2"`"'
            print $0 > duplib
#            duplist=""
#            if (thisdup != "")
#                duplist = " ---> " thisdup
#            print thisartist " - " thistitle duplist
            thisfile=""
            thisartist=""
            thistitle=""
        }
    }

    priorartistnorm = thisartistnorm
    priortitlenorm = thistitlenorm
    priorartist = thisartist
    priortitle = thistitle
    priorfile = thisfile
    priordup = dupcheck
    priorstatus = status
    priorrec=$0
}

END {
    print "Number of Duplicate Songs: " songnum
}
' "playlist=$playlist" "duplib=$LIBRARYDATANAME" DUP=$DUP \
    "$TEMPDIR/dupcheck.sort" | tee "$txtfile"
echo Results are in library $templatefile.

"$JAVA_COMMAND"  -Xincgc -Xmx256M $JAMPAL_JVM_OPTIONS \
 -classpath "$JAMPAL_CLASSPATH" \
 "-Djampal.libdir=$LIBDIR" pgbennett.jampal.Jampal \
 "$LIBDIR/$MAINLIBNAME" "$templatefile" &

