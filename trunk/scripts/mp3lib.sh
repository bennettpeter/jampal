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
# 

set -e
scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
. "$scriptpath/mp3.profile"



if [[ "$1" = "" ]]; then
    echo ""
    echo "Organize mp3 files on disk. Usage:"
    echo "[-reorder] library [moved-file-dir]"
    echo "The optional moved-file-dir is a directory name"
    echo "to which album directories have been moved."
    echo "Use -reorder to reorder directories based on contents of ZZAlbums.txt"
    echo "export mp3lib_skip=nnn to skip some numbers when naming albums"
    exit 2
fi

REORDER=
while true; do
    case $1 in 
        -reorder)
            REORDER=YES
            shift 1
            ;;
        *)
            break
            ;;
    esac
done


libfile=$1
if [[ "$1" = `basename "$1"` ]]; then
    libfile=$LIBDIR/$1
fi

move_dir=$2

if [[ "$move_dir" != "" && ! -d "$move_dir" ]]; then
    echo ERROR, $move_dir is not a directory
    exit 2
fi

echo "Create custom files..."
libname=`basename "$libfile"`
"$scriptpath/make_library_custom_files.sh" "$libfile"

. $TEMPDIR/$libname.profile

if [[ "$LIBRARYDATANAME" = `basename "$LIBRARYDATANAME"` ]]; then
    LIBRARYDATANAME=`dirname $libfile`/"$LIBRARYDATANAME"
fi

if [[ "$LIBRARYTYPE" != L && "$LIBRARYTYPE" != P ]]; then echo "ERROR - invalid library type $LIBRARYTYPE"; exit 2; fi
if [[ "$TITLE" = "" ]]; then echo "ERROR - TITLE is required in the $libname library"; exit 2; fi
if [[ "$ARTIST" = "" ]]; then echo "ERROR - ARTIST is required in the $libname library"; exit 2; fi
if [[ "$ALBUM" = "" ]]; then echo "ERROR - ALBUM is required in the $libname library"; exit 2; fi
if [[ "$TRACK" = "" ]]; then echo "ERROR - TRACK is required in the $libname library"; exit 2; fi
if [[ "$JFILENAME" = "" ]]; then echo "ERROR - FILENAME is required in the $libname library"; exit 2; fi


mkdir -p "$TEMPDIR"
dirfile=$TEMPDIR/mp3_dirs.txt
true >  "$dirfile"
if [[ "$move_dir" != "" ]]; then
    echo "Finding moved files ..."
    find "$move_dir" -name '*.mp3' >> "$dirfile" 2>/dev/null
fi
#
# Step 1 - set up sort key in database file
# and change names of any files that have moved
# setup sort key as directory / altnum / album / track / filename / col 0
# library: if altnum = 0 or space use space otherwise altnum
# playlist: use space

rm -f "$TEMPDIR/library.reformat" "$TEMPDIR/move_report.txt"
touch "$TEMPDIR/move_report.txt"

echo "Set up sort key..."
awk -v "rptfile=$TEMPDIR/move_report.txt" '

BEGIN { 
    FS="\t"
    OFS="\t"
}

# dir_list is result of find -name *.mp3

stream == "dir_list" {
    count = split($0,fields,"/")
    # Caters for a directory moved from one place to another
     dir_file = fields[count-1] "/" fields[count]
    # More generic - Caters for a file moved from one place to another
    # Remove the prefix
    sub(/^[^ ]* /,"",dir_file)
    if (moved_files[dir_file] != "") {
        print "Error - duplicate file " dir_file
        print $0
        print moved_files[dir_file]
        exit 2
    }
    moved_files[dir_file] = $0
}

/\r$/ {
    # Remove any trailing carriage return
    sub(/\r$/,"")
}

stream == "library" {

    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'

    STATUS = $1

    if (substr(STATUS,1,1) != "A") {
        print "Library needs to be saved: " $'$JFILENAME'
        exit 2
    }

    count = split($'$JFILENAME',fields,"/")
    # Caters for a directory moved from one place to another
     dir_file = fields[count-1] "/" fields[count]
    # More generic - Caters for a file moved from one place to another
    # dir_file = fields[count]
    # Remove the prefix (eg A01 or 001)
    sub(/^[^ ]* /,"",dir_file)
    # dir_file = tolower(dir_file)

    newname = moved_files[dir_file]

    if (newname != "" && newname != $'$JFILENAME') {
        print "Moved:" $'$JFILENAME' > rptfile
        print "To:   " newname > rptfile
        $'$JFILENAME' = newname
    }
    
    # directory
    directory=$'$JFILENAME'
    isok = sub(/\/[^\/]*$/,"",directory)
    if (!isok) {
        print "error extracting directory:" $'$JFILENAME'
        exit 2
    }
    key = directory

    # altnum
    if (LIBRARYTYPE == "P")
        altnum = " "
    else {
        altnum = substr(STATUS,2)
        if (altnum == "" || altnum == 0)
            altnum = " "
    }
    key = key "  /" altnum

    # album
    album = ALBUM
    key = key "  /" album

    # track
    track = sprintf("%5.5d",TRACK)
    key = key "  /" track

    # file
    file = $'$JFILENAME'
    isok = sub(/.*\//,"",file)
    if (!isok) {
        print "error extracting file:" $'$JFILENAME'
        exit 2
    }
    key = key "  /" file "  /" STATUS
    $1 = key
    print $0 > outfile
}

' LIBRARYTYPE=$LIBRARYTYPE \
    outfile=$TEMPDIR/library.reformat \
    stream=dir_list $dirfile \
    stream=library $LIBRARYDATANAME

#
# Step 2
# sort the database
#

echo "Sort ..."
rm -f $TEMPDIR/library.sort
export LC_ALL=C
sort  -f -o $TEMPDIR/library.sort $TEMPDIR/library.reformat


echo "Remove diacritics ..."
"$JAVA_COMMAND" -cp "$JAMPAL_CLASSPATH" pgbennett.utility.TextSimplifier \
    -i UTF-8 $TEMPDIR/library.sort -o US-ASCII $TEMPDIR/library.sort.simplify


#rm -f $TEMPDIR/library.sort.cp1252
#iconv -f UTF-8 -t CP1252 --unicode-subst=X $TEMPDIR/library.sort \
#    > $TEMPDIR/library.sort.cp1252
#

rm -f $TEMPDIR/rename.sh $TEMPDIR/rename.out $TEMPDIR/rename_report.txt

touch $TEMPDIR/rename_report.txt

echo "Main Processing ..."
rc=0
awk -v rptfile=$TEMPDIR/rename_report.txt '
#
# Step 3 -
# Create rename file script
#
BEGIN { 
    FS="\t"
    OFS="\t"
    error=0
    madejunk=0
    savediskdir=""
    albumcode["X"]="X"
    newalbumcode["X"]="X"
    newalbumcodelist["X"]="X"
    albumdirlist["X"]="X"
    delete albumdirlist["X"]
    destfilenamelist["X"]="X"
    diskdirlist["X"]="X"
    delete diskdirlist["X"]
    savepath=""
    delpath=""
    maxfnleng=60
    srand()
    prevtrack="00"
    albumfileseq=0
    actioncount=0
#    print "Report of actions to be taken by cleanup" > rptfile
#    print "" > rptfile
}

function printerror(reason) {
    if (!errorprinted) {
        print ""
        print "**** " $'$JFILENAME' ", " TITLE " - " ARTIST 
        errorprinted=1
    }
    print reason
    error = 1
}

function writePrevAlbumFile() {
    # write out prev album file
    if (refresh) {
        temp=" > \""
        for (i=1;i in newalbumcodelist;i++) {
            print "echo \"" newalbumcodelist[i] "\"" temp  albumfile "\"" > outfile
            temp=" >> \""
        }

    }
    for (inx in newalbumcode) {
        delete newalbumcode[inx]
    }
    for (inx in newalbumcodelist) {
        delete newalbumcodelist[inx]
    }
    refresh=0
}

function delEmptyDir() {
    if (delpath != ""){
        print "rmdir  \"" delpath "\" 2>/dev/null || true" > outfile
        delpath = ""
    }
}

function removeDiacritics(theString) {
    gsub(/[^0-9A-Za-z ,.\/\\_'"'"'&\(\)#\[\]!{}=;@-]/,"",theString)
    return theString
}

{
    # get the corresponding utf format record
    getline utfrecord < utflibrary

    # assign field names in database
    '"`cat "$TEMPDIR/$libname.awkset"`"'

    statussplitnum=split($1,statussplit,"/")
    status = statussplit[statussplitnum]

    fullfilename=$'$JFILENAME'

    # parse file name into path parts
    pathsplitnum=split(fullfilename,pathsplit,"/")
    oldfn = pathsplit[pathsplitnum]
    path=substr(fullfilename,1,length(fullfilename)-length(oldfn))

    if (path != prev_path) {
        for (inx in alt_filenames) {
            delete alt_filenames[inx]
        }
    }

    prev_path = path

    if (status != "A") {
        if (LIBRARYTYPE == "P") {
            if (JFILENAME in alt_filenames)
                printerror("Duplicate file name " JFILENAME)
            alt_filenames[JFILENAME] = JFILENAME
        }
        else {
            if (status == "A0")
                alt_filenames[JFILENAME] = JFILENAME
        }
    }

    # deletions
    # peter = R for reject, D for duplicate
    deleteframe='$DELETEFRAME' ""
    deleteframe = tolower(deleteframe)
    if (match(deleteframe,/d/)!=0) {
## deleteframe=="r"||deleteframe=="d")
        if (status == "A" || status == "A0") {
            if (substr(path,1,length(rejectpath)) != rejectpath) {
                if (!madejunk) {
                    print "mkdir -p " rejectpath > outfile
                    madejunk = 1
                }
                print "mv_safe.sh \"" fullfilename "\" \"" rejectpath oldfn "\"" > outfile
                print fullfilename " ---> REJECT" >rptfile
                print "" >rptfile
            }
        }
        next
    }
}

status == "A" || status == "A0" || LIBRARYTYPE == "P" {
    if (prefixes == "") {
        prefixes="ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        print "#!/bin/bash"  > outfile
        print "set -e"  > outfile
        print "if [[ ! -f \"" LIBRARYDATANAME  ".new\" ]]; then echo ERROR - This was already run; exit 2; fi" > outfile
    }

    errorprinted=0

    track = ""
    if (length(TRACK) == 1)
        track = "0" TRACK
    if (length(TRACK) == 2)
        track = TRACK
    if (track == "" || track == "00" || length(track) != 2) {
        track="00"
    }
    album=ALBUM
    subdirectory=ID3V2TAGTXXXDirectory
    # Trim
    sub(/^ */,"",subdirectory)
    sub(/ *$/,"",subdirectory)
    if (subdirectory != "")
        album=subdirectory

    # Back quotes
    if (match(album,/`/)!=0) 
        printerror("Please Remove backquote character from album")
    # Drop dots 
    gsub(/\./," ",album)
    # Drop quotes
    gsub(/\"/,"",album)
    gsub(/`/,"",album)
    # Duplicate spaces
    gsub(/  */," ",album)
    # Change colons or slashes to commas
    gsub(/:|\//,", ",album)
    # Trim
    sub(/^ */,"",album)
    sub(/ *$/,"",album)
    # Question marks
    gsub(/\?/,"",album)
    if (album == "")
        printerror("Blank album")
    if (match(album,/[a-zA-Z][a-zA-Z]/)!= 0 && (album == toupper(album) || album == tolower(album)))
        printerror("Please fix album case:" album)

    diskpos=match(fullfilename,/[0-9][0-9][0-1][0-9][0-3][0-9]_[0-2][0-9][0-5][0-9][^\/]*\//)
    if (diskpos>0)
        diskdir=substr(fullfilename,1,RSTART+RLENGTH-2)
    else {
        albumdir=pathsplit[pathsplitnum - 1]
        diskdir=substr(fullfilename,1,length(fullfilename)-length(oldfn)-length(albumdir)-2)
    }
    cdnumber = pathsplit[pathsplitnum - 2]
    if (diskpos > 0 && cdnumber < "'$OLDISKFORMATBEFORE'")
        use_old_disknum=1
    else
        use_old_disknum=0
    newdisk=""

    temp=albumdirlist[album]
    if (temp=="")
        albumdirlist[album]=diskdir
    else {
        if (temp != diskdir && newdisk=="") {
            albumdirlist[album]=diskdir
        }
    }
    ldiskdir = tolower(diskdir)
    if (ldiskdir != tolower(savediskdir)) {
        # Check for inconsistent disk dirs
        ldiskdir = ldiskdir "/"
        for (entry in diskdirlist) {
            if (match(ldiskdir,entry)==1)
                printerror("Inconsistent Directory Structure")
        }
        diskdirlist[ldiskdir] = "Y"
        # write out prev album file
        writePrevAlbumFile()

        prefixcount=mp3lib_skip
        savediskdir=diskdir
        savealbum=""

        # read new album file
        for (inx in albumcode) {
            delete albumcode[inx]
        }
        i=0
        albumfile = diskdir "/ZZalbums.txt"
        for(i=1;;i++) {
            ret=getline temp < albumfile
            if (ret<1)
                break;
            # Remove trailing carriage return in case the ZZalbums.txt
            # was msdos formatted
            sub(/\r$/,"",temp)
            if (use_old_disknum)
                albumcode[temp]=substr(prefixes,i,1)
            else
                albumcode[temp]=sprintf("%3.3d",i)
            if (albumcode[temp]=="")
                printerror("Too many albums in album file " albumfile)
        }
        close(albumfile)
    }

    if (savealbum!=album) {
        if (REORDER == "YES") {
            prefix = albumcode[album]
            if (prefix == "") {
                printerror("Album renaming or adding new albums cannot be done with REORDER")
                albumcode[album] = "X"
                prefix="X"
            }
        }
        else {
            # Not reordering, recreating album files
            prefix=newalbumcode[album]
            if (prefix == "") {
                prefixcount++
                if (use_old_disknum)
                    prefix=substr(prefixes,prefixcount,1)
                else
                    prefix=sprintf("%3.3d",prefixcount)
                newalbumcodelist[prefixcount]=album
                if (prefix == "")
                    printerror("Too many albums in this directory")
                newalbumcode[album]=prefix
            }
            if (albumcode[album] != prefix) {
                refresh=1
            }
        }
        savealbum=album
        albumfileseq=0
        prevtrack="00"
        for (inx in destfilenamelist) {
            delete destfilenamelist[inx]
        }
    }

    newpath=diskdir "/" prefix " " substr(album,1,maxfnleng-2)
    newpath = removeDiacritics(newpath)
    # Trim
    sub(/^ */,"",newpath)
    sub(/ *$/,"",newpath)
    newpath = newpath "/"
    if (savepath != newpath) {
        savepath=newpath
        delEmptyDir()
        if (newpath != path) {
            if (tolower(newpath) == tolower(path)) {
                path1 = substr(path,1,length(path)-1)
                newpath1 = substr(newpath,1,length(newpath)-1)
                gsub(/`/,"\"'"'"'`'"'"'\"",path1)
                print "mv -fv \"" path1 "\" \"" newpath1 "\"xxxyyy"  > outfile
                print "mv -fv \"" newpath1 "\"xxxyyy \"" newpath1 "\""  > outfile
            }
            else {
                print "mkdir -p \"" newpath "\"" > outfile
                delpath=path
            }
        }
    }

    prevtrack=track
    albumfileseq++
    if (use_old_disknum)
        fileprefix = prefix sprintf("%2.2d ",albumfileseq)
    else
        fileprefix = sprintf("%3.3d ",albumfileseq)

    artist = ARTIST
    title = TITLE

    # Drop dots 
    gsub(/\./," ",artist)
    gsub(/\./," ",title)
    # Drop quotes
    gsub(/\"/,"",artist)
    gsub(/\"/,"",title)
    # Back quotes
    gsub(/`/,"",artist)
    gsub(/`/,"",title)
    # Question marks
    gsub(/\?/,"",artist)
    gsub(/\?/,"",title)
    # Duplicate spaces
    gsub(/  */," ",artist)
    gsub(/  */," ",title)
    # Change colons or slashes to commas
    gsub(/:/,", ",artist)
    gsub(/:/,", ",title)
    gsub(/\//,", ",artist)
    gsub(/\//,", ",title)
    # trim
    sub(/^ */,"",title)
    sub(/ *$/,"",title)
    # trim
    sub(/^ */,"",artist)
    sub(/ *$/,"",artist)
    # Check for capitalized title
    smarttag=""

    # 10 is for "01 " " - " ".mp3"
    newfnleng = length(title) + length(artist) + length(fileprefix) + 7
#if (DEBUG=="Y")
#  print title "," artist "," fileprefix "," newfnleng
    if (newfnleng > maxfnleng && smarttag != "Y") {
        newlen=length(artist)-newfnleng+maxfnleng
        if (newlen < 20)
            newlen=20
        artist=substr(artist,1,newlen)
    }
    newfnleng = length(title) + length(artist) + length(fileprefix) + 7
    if (newfnleng > maxfnleng && smarttag != "Y")
        title=substr(title,1,length(title)-newfnleng+maxfnleng)
    newfnleng = length(title) + length(artist) + length(fileprefix) + 7
    newfn = fileprefix title " - " artist ".mp3"
    gsub(/  /," ",newfn)
    if (length(newfn) > maxfnleng)
        printerror("Length wrong")
    if (match(newfn,/.*\..*\..*/)!=0)
        printerror("Two dots")
    gsub(/  */," ",newfn)
    if (match(newfn,/  /)!=0)
        printerror("Duplicate spaces:" newfn)

    gsub(/[\*\?\+\"]/,"X",newfn)

    newfn = removeDiacritics(newfn)
    newfullfilename = newpath newfn
    $0 = utfrecord
    if (newfullfilename != fullfilename) {
        if ($'$JFILENAME' != fullfilename)
            printerror("Diacritrics in file name")
        gsub(/`/,"\"'"'"'`'"'"'\"",fullfilename)
        if (destfilenamelist[tolower(fullfilename)] == "Y")
            printerror("File name clash. There are two files needing " \
              "name " fullfilename ". Please change the song titles or remove one file.")
        destfilenamelist[tolower(newfullfilename)] = "Y"
        if (tolower(newfullfilename) == tolower(fullfilename)) {
            print "if [[ -f \"" newfullfilename "\"xxxyyy ]] ; then echo ERROR \"" newfullfilename "\" already exists ; exit 2 ; fi"  > outfile
            print "mv -i -v \"" fullfilename "\" \\" > outfile
            print "       \"" newfullfilename "\"xxxyyy" > outfile

            print "if [[ -f \"" newfullfilename "\" ]] ; then echo ERROR \"" newfullfilename "\" already exists ; exit 2 ; fi"  > outfile
            print "mv -i -v \"" newfullfilename "\"xxxyyy \\" > outfile
            print "       \"" newfullfilename "\"" > outfile

        }
        else {
            print "if [[ -f \"" newfullfilename "\" ]] ; then echo ERROR \"" newfullfilename "\" already exists ; exit 2 ; fi"  > outfile
            print "mv -i -v \"" fullfilename "\" \\" > outfile
            print "       \"" newfullfilename "\"" > outfile
        }
        print fullfilename " --->" > rptfile
        print newfullfilename > rptfile
        print "" > rptfile

        if (status == "A0")
            alt_filenames[JFILENAME] = newfullfilename

        $'$JFILENAME' = newfullfilename
    }
    $1 = status
    print $0 > LIBRARYDATANAME ".new"
}

status >= "A1" && LIBRARYTYPE == "L" {
    newfullfilename = alt_filenames[JFILENAME]
    if (newfullfilename == "")
        printerror("No main track for file:" status " " JFILENAME)
    $0 = utfrecord
    $'$JFILENAME' = newfullfilename
    $1 = status
    print $0 > LIBRARYDATANAME ".new"

}


END {
    writePrevAlbumFile()
    delEmptyDir()
    print ""
    print "rc=x" > outfile
    print "while [[ x$rc = xx ]]; do rc=" > outfile
    print "ret=0" > outfile
    print "mv -fv \"" LIBRARYDATANAME "\"  \"" LIBRARYDATANAME ".old\" || ret=$?" > outfile
    print "if [[ $ret != 0 ]] ; then rc=x; fi" > outfile
    print "if [[ x$rc = x ]] ; then mv -v \"" LIBRARYDATANAME ".new" "\" \"" LIBRARYDATANAME "\" || ret=$? ; fi" > outfile
    print "if [[ $ret != 0 ]] ; then rc=x; fi" > outfile
    print "chmod 666 \"" LIBRARYDATANAME "\" || ret=$?" > outfile
    print "if [[ x$rc = x ]]; then break; fi"  > outfile
    print "echo Press Enter to retry; read -e resp" > outfile
    print "done" > outfile
    print "echo XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" > outfile
    print "echo XXXX Please use the File, Reload Library menu in Jampal to reload your library XXXX" > outfile
    print "echo XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX" > outfile
    exit error
}

'    \
    LIBRARYTYPE=$LIBRARYTYPE \
    outfile=$TEMPDIR/rename.sh \
    "mp3lib_skip=$mp3lib_skip" \
    REORDER=$REORDER \
    rejectpath="$REJECTPATH/" \
    utflibrary="$TEMPDIR/library.sort" \
    "LIBRARYDATANAME=$LIBRARYDATANAME" "$TEMPDIR/library.sort.simplify" > "$TEMPDIR/rename.out" || rc=$?
cat "$TEMPDIR/rename.out"
cat "$TEMPDIR/move_report.txt" "$TEMPDIR/rename_report.txt" > "$TEMPDIR/summary_report.txt"
if [[ "$rc" != 0 ]]; then echo ERROR; exit 2; fi
chmod +x "$TEMPDIR/rename.sh"
# less "$TEMPDIR/summary_report.txt"
while [[ true ]]; do 
    if (( `cat "$TEMPDIR/rename_report.txt" | wc -w` > 0 )) ; then
        echo Type yes to run the moves, no to exit, or enter to view the report.
        read -e resp
        if [[ "$resp" = no || "$resp" = n ]]; then
            exit
        fi
    elif (( `cat "$TEMPDIR/move_report.txt" | wc -w` > 0 )) ; then
        resp=yes
    else
        echo "Everything is already correct. Nothing to do"
        exit
    fi
    if [[ "$resp" = yes ]]; then
        "$TEMPDIR/rename.sh"
        exit
    fi
    if [[ "$resp" = "" ]]; then
        less "$TEMPDIR/summary_report.txt"
    fi
done

