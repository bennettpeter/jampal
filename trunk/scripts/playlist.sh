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

# Create playlist that can be used for making CDs

set -e
scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
. "$scriptpath/mp3.profile"


if [[ "$1" = "" ]]; then
    echo Parameters:
    echo " 1" condition list
    echo " 2" sort field default existingseq
    echo " 3" sort option -r for reverse
    echo " 4" announce Y or N
    echo " 5" "tracks per directory (default 100)"
    echo " 6" "start dir (default 2)"
    echo " 7" "library-name (default main library)"
    echo " 8" "Announcements merge Y or N default Y"
    echo " 9" 'speak. Default is TITLE ". " ARTIST'
    echo "10" separator default '". "'
    echo "11" directory change condition, default blank
    echo "12" Append sequence number to song name in tag, Y or N. Default N
    echo "13" Maximum size per CD in MB. Default 690
    echo "14" Staging Directory. Default from mp3.profile $CDSTAGING
    echo "15" File Number Start in each directory. Default 1.
    echo "16" Optional Name of playlist to append to directory name.
    echo "17" Optional custom tag setting.
    echo "18" Optional starting disk number, default 1.
    exit 2
fi

template=cdsource
templatefile="$LIBDIR/$template"

# for conditions and sort use awk fields and expressions
sort=existingseq
# use "-r" for reverse order
sortopt=
# Whether you want announcements
announce=N
# Announcements merged into files
ann_merge=Y
tracksperdir=100
startdir=1

speak='TITLE ". " ARTIST'
speaksep='".  "'

if [[ "$1" != "" ]];then
    conditions="$1"
fi
if [[ "$2" != "" ]];then
    sort="$2"
fi
if [[ "$3" != "" ]];then
    sortopt="$3"
fi
if [[ "$4" != "" ]];then
    announce="$4"
fi
if [[ "$5" != "" ]];then
    tracksperdir="$5"
fi
if [[ "$6" != "" ]];then
    startdir="$6"
fi

libfile="$MAINLIBNAME"

if [[ "$7" != "" ]];then
    libfile=$7
fi

if [[ "$8" != "" ]];then
    ann_merge="$8"
fi
if [[ "$9" != "" ]];then
    speak=$9
fi
if [[ "${10}" != "" ]];then
    speaksep=${10}
fi

if [[ "$libfile" = `basename "$libfile"` ]]; then
    libfile="$LIBDIR/$libfile"
fi

dirchange='""'
if [[ "${11}" != "" ]];then
    dirchange=${11}
fi

appendseq=N
if [[ "${12}" != "" ]];then
    appendseq=${12}
fi

maxsize=690
if [[ "${13}" != "" ]];then
    maxsize=${13}
fi

cdstaging=$CDSTAGING
if [[ "${14}" != "" ]];then
    cdstaging=${14}
fi

filenumstart=1
if [[ "${15}" != "" ]];then
    filenumstart=${15}
fi

playlistname=
if [[ "${16}" != "" ]];then
    playlistname="_${16}"
fi

customtag=
if [[ "${17}" != "" ]];then
    customtag="${17}"
fi

start_disknum=1
if [[ "${18}" != "" ]];then
    start_disknum="${18}"
fi

echo "Playlist generated from songs in library $libfile"

libname=`basename $libfile`
"$scriptpath/make_library_custom_files.sh" "$libfile"

. "$TEMPDIR/$libname.profile"

if [[ "$LIBRARYDATANAME" = `basename "$LIBRARYDATANAME"` ]]; then
    LIBRARYDATANAME=`dirname $libfile`/"$LIBRARYDATANAME"
fi

if [[ "$TITLE" = "" ]]; then echo "ERROR - TITLE is required in the $libname library"; exit 2; fi
if [[ "$ARTIST" = "" ]]; then echo "ERROR - ARTIST is required in the $libname library"; exit 2; fi
if [[ "$ALBUM" = "" ]]; then echo "ERROR - ALBUM is required in the $libname library"; exit 2; fi
if [[ "$TRACK" = "" ]]; then echo "ERROR - TRACK is required in the $libname library"; exit 2; fi
if [[ "$JFILENAME" = "" ]]; then echo "ERROR - FILENAME is required in the $libname library"; exit 2; fi
if [[ "$FILESIZE" = "" ]]; then
    if [[ "$PLAYINGTIMEMS" = "" || "$BITRATE" = "" ]]; then 
        echo 'ERROR - FILESIZE or (PLAYINGTIMEMS and BITRATE) are required in the '$libname' library'
        exit 2
    fi
fi


docdir="$PLAYLISTCD"
mkdir -p "$docdir"

datetime=`date +%y%m%d_%H%M`
diskdir=$docdir/${datetime}${playlistname}
mkdir -p "$diskdir"

trap 'echo "ERROR - removing directory $diskdir" ; rm -rf "$diskdir"' ERR

echo "$*" >"$diskdir/parameters.txt"
echo announce=$announce > "$diskdir/cdparms.profile"
echo ann_merge=$ann_merge >> "$diskdir/cdparms.profile"
echo libfile='"'$libfile'"' >> "$diskdir/cdparms.profile"
echo LIBRARYDATANAME='"'$LIBRARYDATANAME'"' >> "$diskdir/cdparms.profile"
echo datetime=$datetime >> "$diskdir/cdparms.profile"
echo speak="'"$speak"'" >> "$diskdir/cdparms.profile"
echo speaksep="'"$speaksep"'" >> "$diskdir/cdparms.profile"
echo dirchange="'"$dirchange"'" >> "$diskdir/cdparms.profile"
echo appendseq="'"$appendseq"'" >> "$diskdir/cdparms.profile"
echo cdstaging="'"$cdstaging"'" >> "$diskdir/cdparms.profile"
echo customtag="'"$customtag"'" >> "$diskdir/cdparms.profile"

libsort=$diskdir/libsort.jmp
libnum=$diskdir/libnum.jmp
extract=$diskdir/extract.jmp
extsort=$diskdir/extsort.jmp
cdsource=$diskdir/$template.jmp
listing=$diskdir/listing.txt
newlib=$diskdir/playlist-$datetime${playlistname}

# Set increment to 2 if including title audio files
# Otherwise 1

if [[ "$announce" = Y && "$ann_merge" != Y ]]; then
    increment=2
else
    increment=1
fi

if [[ ! -f "$templatefile.jampal" ]]; then
    cp -f "$scriptpath/$template.jampal" "$templatefile.jampal"
fi

"$scriptpath/make_library_custom_files.sh" "$template"


(
    . "$TEMPDIR/$template.profile"
    if [[ "$TITLE" = "" ]]; then echo "ERROR - TITLE is required in the $template library"; exit 2; fi
    if [[ "$ARTIST" = "" ]]; then echo "ERROR - ARTIST is required in the $template library"; exit 2; fi
    if [[ "$ALBUM" = "" ]]; then echo "ERROR - ALBUM is required in the $template library"; exit 2; fi
    if [[ "$TRACK" = "" ]]; then echo "ERROR - TRACK is required in the $template library"; exit 2; fi
    if [[ "$JFILENAME" = "" ]]; then echo "ERROR - FILENAME is required in the $template library"; exit 2; fi
)



sed "s/^playlist=.*/playlist=playlist-$datetime${playlistname}/" \
     "$templatefile.jampal" > "$newlib.jampal"


echo playlistlib="'"./playlist-$datetime${playlistname}"'" >> "$diskdir/cdparms.profile"

#############################################
# Insert existing record sequence as field 1#
#############################################
rm -f "$libnum"
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
    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'

    if (substr($1,1,1) != "A") {
        print "Library needs to be saved: " FILENAME
        exit 2
    }
    status = $1
    if (sequence == "")
        sequence = 1000000
    $1 = ++sequence "  /" status
    print $0 > extract
}' "extract=$libnum" "$LIBRARYDATANAME"


#######################
# Sort by album, track#
#######################

## Tab character 09
echo Sort ...
sort -t $'\t' -f -d -k $ALBUM,$ALBUM -k $TRACK,${TRACK}n $libnum > $libsort


#######################
# Selection of tracks #
#######################
cat /dev/null > "$extract"

echo select ...
echo $conditions
rc=0
echo $conditions | grep "[^=]=[^=]" || rc=$?

if [[ "$rc" = 0 ]] ; then
    echo "ERROR use == not = for testing equal values"
    rm -rf "$diskdir" || rc=$?
    exit 2
fi

awk '

function trim(trimmit) {
    gsub(/^ */,"",trimmit)
    gsub(/ *$/,"",trimmit)
    return trimmit
}

function wrandom(value,starting,weight) {
    if (value > starting)
        return rand()*100000*weight +100000 random2
    else
        return rand()*100000+100000 random2
}

BEGIN { 
    FS="\t"
    OFS="\t"
    error=0
    srand()
    sortkey=""
    sortext=0
    '"$PLAYLIST_SEARCH_SETUP"'
}

{

    prevaccept=accept
    prevAlbum=ALBUM
    prevTrack=TRACK

    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'

    statussplitnum=split($1,statussplit,"/")
    status = statussplit[statussplitnum]

    existingseq=statussplit[1]
    if (ID3V2TAGTXXXDirectory == "")
        DIRALBUM=ALBUM
    else
        DIRALBUM=ID3V2TAGTXXXDirectory
    random1=rand()*100000+100000
    random2=rand()*100000+100000
    random=random1 random2
    random1=rand()*100000+100000
    random2=rand()*100000+100000
    randomv=rand()
    count=split(FILENAME,filepart,"/")
    filename=filepart[count]
    cdname=filepart[count-2]
    accept=0
    sortextc="x00"

    if (follow && prevaccept) {
        if (ALBUM==prevAlbum) {
            if (TRACK==prevTrack)
                next
            if (TRACK==prevTrack+1) {
                accept=1
                sortext+=1
                sortextc = sprintf("x%2.2d", sortext)
            }
        }
    }
    if (!accept)
        if ('"$conditions"') {
        sortkey = '"$sort"'
        accept=1
        sortext=0
    }
    if (accept) {
        $1 = sortkey sortextc "  /" status
        print $0 > extract
    }
    jampal = tolower(ID3V2TAGTXXXjampal)
    follow=match(jampal,/f/)
}' extract=$extract $libsort

if (( `cat "$extract" | wc -w` == 0 )) ; then
    echo "No songs are selected"
    rm -rf "$diskdir" || rc=$?
    exit 2
fi


###############
# Sort tracks #
###############

echo sort ...
sort  $sortopt -f -d  $extract > $extsort



###############
# Setup CDs   #
###############

echo Setup CD ...
awk -v increment=$increment -v startdir=$startdir \
    -v filenumstart=$filenumstart \
'

function trim(trimmit) {
    gsub(/^ */,"",trimmit)
    gsub(/ *$/,"",trimmit)
    return trimmit
}


function process(record,counter) {
    save=$0
    if (record!="") {
        $0=record
        # assign field names in database
        '"`cat "$TEMPDIR/$libname.awkset"`"'
    }
    statussplitnum=split($1,statussplit,"/")
    status = statussplit[statussplitnum]
    if (counter==0||counter==1)
        fileNumber+=increment
    else
        fileNumber++
    DUMMY=sprintf("%3.3d:%3.3d:%3.3d:%3.3d",disknum,dirnum,fileNumber,counter)
    # Clear out the record so we can build it up from scratch
    $0=""
    $1=status
    # build record
    '"`cat "$TEMPDIR/$template.awkset2"`"'
    print $0 > cdsource
    print newseq "\t" TITLE "\t" ARTIST "\t" FILENAME > listing
    $0=save
    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'
    save=""
}

BEGIN { 
    FS="\t"
    OFS="\t"
    error=0
    totalsize=0
#    disknum=1
    followIndex=1
    fileNumber = filenumstart - 1
    dirnum=startdir
    first="Y"
}


{
    prevAlbumT=ALBUM
    prevTrackT=TRACK

    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'

    if (ID3V2TAGTXXXDirectory == "")
        DIRALBUM=ALBUM
    else
        DIRALBUM=ID3V2TAGTXXXDirectory

    '"$PLAYLIST_FINAL_CUSTOMIZE"'

    prevAlbum = prevAlbumT
    prevTrack = prevTrackT

    dirtitleprev = dirtitle
    dirtitle = '"$dirchange"'
    if (first == "Y") {
        dirtitleprev = dirtitle
        first = "N"
    }


    pos=match(FILESIZE,/K$/)
    if (pos != 0) {
        filesize = substr(FILESIZE,1,pos)
        filesize = filesize / 1024
    }
    else {
        # average tag = 7500 bytes
        filesize=PLAYINGTIMEMS*BITRATE/8388608 + 0.00715
    }
    if (announce == "Y")
        filesize+=0.06
    totalsize = totalsize + filesize
    if (totalsize > maxsize) {
        print "CD " disknum " " totalsize " Mb"
        disknum++
        dirnum=startdir
        filesInDirectory=0
        fileNumber = filenumstart - 1
        totalsize=0
    }


    prevFollow=follow
    jampal=tolower(ID3V2TAGTXXXjampal)
    follow=match(jampal,/f/)
    if (prevFollow) 
        filesInDirectory++
    else
        filesInDirectory+=increment
    if ((filesInDirectory > tracksperdir || dirtitle != dirtitleprev) && fileNumber > (filenumstart - 1) ) {
        if (filesInDirectory <= 2)
            print "WARNING - Only 1 file in directory " dirnum " " dirtitleprev
        filesInDirectory=increment
        fileNumber = filenumstart - 1
        dirnum++
    }

    if (prevFollow) {
        if (prevAlbum!=ALBUM || prevTrack+1!=TRACK || !follow) {
            for (i=1;;i++) {
                if (i in followArray) {
                    process(followArray[i],i)
                    delete followArray[i]
                }
                else
                    break
            }
            saveFollow=followIndex
            followIndex=1
        }
        if (prevAlbum==ALBUM && prevTrack+1==TRACK && !follow) {
            process("",saveFollow)
            next
        }
    }
    if (follow) {
        followArray[followIndex]=$0
        followIndex++
        next
    }

    process("",0)

}

END {
    print "CD " disknum " " totalsize " Mb"
}
'  \
    cdsource=$cdsource  \
    template=$template \
    tracksperdir=$tracksperdir listing=$listing  \
    maxsize=$maxsize \
    announce=$announce ann_merge=$ann_merge \
    disknum=$start_disknum \
    "$extsort" 





(
    . "$TEMPDIR/$template.profile"
    if [[ "$LIBRARYDATANAME" = `basename "$LIBRARYDATANAME"` ]]; then
        LIBRARYDATANAME=$diskdir/"$LIBRARYDATANAME"
    fi
    cp -f "$cdsource" "$LIBRARYDATANAME"
#     echo cdsource="'"$LIBRARYDATANAME"'" >> "$diskdir/cdparms.profile"

)
echo "results in $diskdir"
echo "Launching jampal ..."

"$scriptpath/jampal.sh" open "$newlib"

if [[ "$DEBUG" = Y ]]; then
    mv "$libsort" "${libsort}_XXXX"
    mv "$extract" "${extract}_XXXX"
    mv "$extsort" "${extsort}_XXXX"
    mv "$listing" "${listing}_XXXX"
    mv "$libnum"  "${libnum}_XXXX"
else
    rm -f "$libsort" 
    rm -f "$extract" 
    rm -f "$extsort" 
    rm -f "$listing" 
    rm -f "$libnum" 
fi

rm -f $TEMPDIR/$libname.profile $TEMPDIR/$libname.awkset $TEMPDIR/$libname.awkset2
rm -f $TEMPDIR/$template.profile $TEMPDIR/$template.awkset $TEMPDIR/$template.awkset2

cd "$diskdir"

disknum=X
while [[ "$disknum" != "" ]]; do
    echo " "
    echo To create a disk, type the number, to exit press enter
    read -e disknum
    if [[ "$disknum" != "" ]]; then
        playlistcd.sh $disknum
        echo "Disk Create complete"
    fi
done
