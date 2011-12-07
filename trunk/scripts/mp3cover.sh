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

if [[ "$1" = "" ]]; then
    echo ""
    echo "Print CD envelopes. Usage:"
    echo "$0 [--artist|--artistalbum|--none] [--nosort] library-name diskser desc"
    echo "-artist says to group titles by artist, otherwise they are grouped by album"
    echo "-artistalbum says to group and print titles by artist and album"
    echo "-none says to not group and print titles"
    echo "-nosort Do not sort library. This is used for a playlist that consists of tracks"
    echo "        in the library, for example from the playlist.sh scripts"
    exit 2
fi

if [[ "$OPENOFFICE" != "" && ! -x `which "$OPENOFFICE"` ]] ; then
    echo "This requires Open Office or Libre Office installed and on the path" >&2
    echo "Please install soffice or update ~/.jampal/jampal.conf" >&2
    exit 2
fi

cover=
if [[ "$1" = "--artist" ]]; then
    cover=artist
    shift 1
elif [[ "$1" = "--artistalbum" ]]; then
    cover=artistalbum
    shift 1
elif [[ "$1" = "--none" ]]; then
    cover=none
    shift 1
elif [[ "$1" = "--album" ]]; then
    cover=album
    shift 1
fi
sort=Y
if [[ "$1" = "--nosort" ]]; then
    sort=N
    shift 1
fi
libfile=$1
if [[ "$libfile" = `basename "$libfile"` ]]; then
    libfile="$LIBDIR/$libfile"
fi
libfile=${libfile%.jampal}
if (( $# < 2 ))
then
    echo "Enter disk number"
    read -e diskser
else
    diskser="$2"
fi
if [[ "$3" != "" ]]
then
    desc=$3
fi

echo Choose a model from these:
(cd "$scriptpath";ls -1 cover_*.odt|sed "s/^cover_//
s/\.odt$//")
read -e model
model=cover_$model.odt

libname=`basename "$libfile"`
"$scriptpath/make_library_custom_files.sh" "$libfile"

. "$TEMPDIR/$libname.profile"

# convert to absolute path
libdir=`dirname $libfile`
libdir=`(cd $libdir;pwd)`
if [[ "$LIBRARYDATANAME" = `basename "$LIBRARYDATANAME"` ]]; then
    LIBRARYDATANAME=$libdir/$LIBRARYDATANAME
fi

playlist_datetime=
if [[ -a "$libdir/cdparms.profile" ]] ; then
    playlist_datetime=`(. "$libdir/cdparms.profile"; echo $datetime)`
fi

if [[ "$playlist_datetime" != "" ]] ; then
    sort=N
    echo Setting -nosort for playlist
    if [[ "$cover" == album ]] ; then
        cover=
    else
        cover=none
        echo Setting -none for playlist
    fi
fi

if [[ "$LIBRARYTYPE" != L && "$LIBRARYTYPE" != P ]]; then echo "ERROR - invalid library type $LIBRARYTYPE"; exit 2; fi
if [[ "$TITLE" = "" ]]; then echo "ERROR - TITLE is required in the $libname library"; exit 2; fi
if [[ "$ARTIST" = "" ]]; then echo "ERROR - ARTIST is required in the $libname library"; exit 2; fi
if [[ "$ALBUM" = "" ]]; then echo "ERROR - ALBUM is required in the $libname library"; exit 2; fi
if [[ "$TRACK" = "" ]]; then echo "ERROR - TRACK is required in the $libname library"; exit 2; fi
if [[ "$JFILENAME" = "" ]]; then echo "ERROR - FILENAME is required in the $libname library"; exit 2; fi

rm -fr "$TEMPDIR/index"
mkdir -p "$TEMPDIR/index"
cd "$TEMPDIR/index"
unzip -q "$scriptpath/$model"
cd

# Frag files
# styles.xml - 
# Beginning to first serial num - S01serialnum.frag
# Description to second serial num - S02serialnum.frag
# Second description to end - S03end.frag
# content.xml
# Beginning to first ** - C01start.frag
# Between track num and name - Cfld_sep.frag
# Between track name and next track num - Cindex_sep.frag
# Between last track name and First number on envelope - C04cover.frag
# Between track num and name on envelope - Cfld_sep_cover.frag
# Between track name and next track num on envelope - Ccover_sep.frag
# After last name on cover - C06end.frag

# Sort on file name

if [[ "$sort" = Y ]]; then
    sort -k "$JFILENAME" -t $'\t' -o "$TEMPDIR/library.sort" "$LIBRARYDATANAME"
else
    cp -f "$LIBRARYDATANAME" "$TEMPDIR/library.sort"
fi

# rm -f $TEMPDIR/library.sort.cp1252
# iconv -f UTF-8 -t CP1252 --unicode-subst=X $TEMPDIR/library.sort \
#     > $TEMPDIR/library.sort.cp1252


rm -f "$TEMPDIR/styles.xml"
rm -f "$TEMPDIR/content.xml"
cd "$scriptpath"

# First create the track listing pages

awk -v "outdir=$TEMPDIR" -v "diskser=$diskser" -v "desc=$desc" -v "cover=$cover" \
 -v modeldir=$TEMPDIR/index -v playlist_datetime=$playlist_datetime '

function copyfile(filename) {
    recnum=0
    while(getline frag < (modeldir "/" filename) == 1) {
        if (recnum > 0)
            printf "\n" >> outfile
        printf "%s", frag >> outfile
        recnum++
    }
    close(modeldir "/" filename)
}

BEGIN { 
    FS="\t"
    OFS="\t"
    gtracknum=1
    priordir=""
    prioralbum=""
    priorartist=""
    tracknum=1
    albumnum=1
    outfile = outdir "/styles.xml"
    copyfile("S01serialnum.frag")
    if (desc == "") { 
        print "Please enter a CD Description"
        getline desc < "/dev/tty"
        close("/dev/tty")
    }
    if (playlist_datetime != "")
        playlist_datetime = playlist_datetime "_"
    printf "%s", playlist_datetime diskser "<text:tab/>" desc > outfile
    copyfile("S02serialnum.frag")
    printf "%s", playlist_datetime diskser "<text:tab/>" desc > outfile
    copyfile("S03end.frag") 
    outfile = outdir "/content.xml"
    copyfile("C01start.frag") 
}

/\r$/ {
    # Remove any trailing carriage return
    sub(/\r$/,"")
}

{
    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'
}

substr($1,1,1) != "A" {
    print "Library needs to be saved: " $'$JFILENAME'
    exit 2
}

$1 == "A" || $1 == "A0" || LIBRARYTYPE == "P" {
    if ((playlist_datetime != "" && match(DUMMY,"^" diskser)!=0) \
    || (playlist_datetime == "" && match(JFILENAME,diskser)!=0) \
    || diskser=="") {
        album=ALBUM
        if (ID3V2TAGTXXXDirectory != "")
            album = ID3V2TAGTXXXDirectory
        if (cover=="artistalbum")
            album=ARTIST " - " album
        if (cover=="artist")
            album=ARTIST
        if (cover=="none")
            album=""
        # trim
        gsub(/^ */,"",album)
        gsub(/ *$/,"",album)
        gsub(/&/,"&amp;",album)
        gsub(/</,"&lt;",album)
        gsub(/>/,"&gt;",album)
        if (gtracknum > 1)
            copyfile("Cindex_sep.frag")
        if (prioralbum != album) {
            printf "%s", "**" > outfile
            copyfile("Cfld_sep.frag") 
            printf "%s", albumnum " " album > outfile
            albumnum++
            copyfile("Cindex_sep.frag")
            tracknum=1
            prioralbum=album
            priorartist=""
        }
        printf("%d",gtracknum) > outfile
        copyfile("Cfld_sep.frag") 
        artist = ARTIST
        gsub(/^ */,"",artist)
        gsub(/ *$/,"",artist)
        gsub(/&/,"&amp;",artist)
        gsub(/</,"&lt;",artist)
        gsub(/>/,"&gt;",artist)
        title = TITLE
        gsub(/^ */,"",title)
        gsub(/ *$/,"",title)
        gsub(/&/,"&amp;",title)
        gsub(/</,"&lt;",title)
        gsub(/>/,"&gt;",title)
        artistout=""
        if (priorartist != artist) {
            artistout=" - " artist
            priorartist=artist
        }
        printf("%02.2d %s%s",tracknum,title,artistout) > outfile
        tracknum++
        gtracknum++
    }

}


END {
    print "Number of tracks: " gtracknum-1
    if (gtracknum==1)
        exit 2
}

' LIBRARYTYPE=$LIBRARYTYPE \
    "$TEMPDIR/library.sort"

# Next create the envelope page

awk  -v "outdir=$TEMPDIR" -v "diskser=$diskser" -v "cover=$cover" \
  -v modeldir=$TEMPDIR/index -v playlist_datetime=$playlist_datetime '

function copyfile(filename) {
    recnum=0
    while(getline frag < (modeldir "/" filename) == 1) {
        if (recnum > 0)
            printf "\n" >> outfile
        printf "%s", frag >> outfile
        recnum++
    }
    close(modeldir "/" filename)
}

function albumOut() {
    if (prioralbum != album) {
        if (gtracknum > 1)
            copyfile("Ccover_sep.frag")
        if (gtracknum==99999)
            gtracknum=" "
        printf "%d", gtracknum >> outfile
        copyfile("Cfld_sep_cover.frag")
        printf("%02d ",albumnum) >>outfile 
        printf "%s", album >> outfile
        albumnum++
        prioralbum=album
    }
}

BEGIN { 
    FS="\t"
    OFS="\t"
    gtracknum=1
    priordir=""
    prioralbum=""
    albumnum=1
    outfile=outdir "/content.xml"
    copyfile("C04cover.frag") 
}

/\r$/ {
    # Remove any trailing carriage return
    sub(/\r$/,"")
}

$1 == "A" || $1 == "A0" || LIBRARYTYPE == "P" {

    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'

    if ((playlist_datetime != "" && match(DUMMY,"^" diskser)!=0) \
    || (playlist_datetime == "" && match(JFILENAME,diskser)!=0) \
    || diskser=="") {
        album=ALBUM
        if (ID3V2TAGTXXXDirectory != "")
            album = ID3V2TAGTXXXDirectory
        if (cover=="artistalbum")
            album=ARTIST " - " album
        if (cover=="artist")
            album=ARTIST
        if (cover=="none")
            album = ""
        # trim
        gsub(/^ */,"",album)
        gsub(/ *$/,"",album)
        gsub(/&/,"&amp;",album)
        gsub(/</,"&lt;",album)
        gsub(/>/,"&gt;",album)
        albumOut()
        gtracknum++
        fullfilename=JFILENAME
    }
}

END {
    print "Number of tracks: " gtracknum-1
    if (gtracknum==1)
        exit 2
    sub(/\\[^\\]*\\[^\\]*$/,"/ZCombo",fullfilename)
    resp=system("test -d \"" fullfilename "\" ")
    if (resp==0) {
        gtracknum=99999
        album="Combo"
        albumOut()
    }
    copyfile("C06end.frag")
}

' LIBRARYTYPE=$LIBRARYTYPE \
    "$TEMPDIR/library.sort"

# iconv -f CP1252 -t UTF-8 "$TEMPDIR/styles.xml" > "$TEMPDIR/index/styles.xml"
cp "$TEMPDIR/styles.xml" "$TEMPDIR/index/styles.xml"

# iconv -f CP1252 -t UTF-8 "$TEMPDIR/content.xml" > "$TEMPDIR/index/content.xml"
cp "$TEMPDIR/content.xml" "$TEMPDIR/index/content.xml"

cd "$TEMPDIR/index"
rm -f "$TEMPDIR/index.odt"
zip -rq "$TEMPDIR/index.odt" *

filename=$TEMPDIR/index.odt

if [[ `uname -s` = CYGWIN* ]]; then
    filename=`cygpath -w "$filename"`
fi

echo "The result is in $filename" 

if [[ "$OPENOFFICE" != "" ]]; then
    echo "Launching open office ..."
    nohup "$OPENOFFICE" -o "$filename" >/dev/null 2>/dev/null &
fi