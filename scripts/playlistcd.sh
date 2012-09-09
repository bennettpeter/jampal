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

# Create a CD from a playlist extract
# Must be run from the directory where the extract is stored

set -e
scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
. "$scriptpath/mp3.profile"


if [[ "$1" = "" ]]; then
    echo This must be run from the directory containing the playlist library
    echo Parameters:
    echo disk number
    echo [output path] optional - can specify the mount point for an mp3 player
    exit 2
fi

while [[  $(ps -e | egrep awk)  ]] ; do 
    echo Please make sure no other playlistcd is running.
    echo Press enter when you are sure.
    read x
done

if [[ "$SOXPATH" != "" ]]; then
    PATH="$PATH:$SOXPATH"
fi

disknum=$1
diskdir=$PWD

cdstagingp=$2

. "$diskdir/cdparms.profile"

if [[ "$announce" == Y && ! -x `which lame` ]] ; then
    echo "Announcements require lame installed and on the path" >&2
    echo "Please install lame or try again without announcements" >&2
    exit 2
fi


if [[ "$cdstaging" = "" ]]; then
    cdstaging=$CDSTAGING
fi

mkdir -p "$TEMPDIR"

# Cater for overridden staging directory
if [[ "$cdstagingp" != "" ]]; then
    cdstaging="$cdstagingp"
fi

# Get main library data name
"$scriptpath/make_library_custom_files.sh" "$MAINLIBNAME"

. $TEMPDIR/$MAINLIBNAME.profile
mainlibdata=$LIBDIR/$LIBRARYDATANAME

libname=`basename $libfile`
"$scriptpath/make_library_custom_files.sh" "$libfile"

. $TEMPDIR/$libname.profile

if [[ "$LIBRARYDATANAME" = `basename "$LIBRARYDATANAME"` ]]; then
    LIBRARYDATANAME=`dirname $libfile`/"$LIBRARYDATANAME"
fi

"$scriptpath/make_library_custom_files.sh" "$playlistlib"

playlistlibname=`basename "$playlistlib"`

. "$TEMPDIR/$playlistlibname.profile"


cdsource="$diskdir/$LIBRARYDATANAME"

disknumd=`printf %3.3d $disknum`
staging="$cdstaging/${datetime}_$disknumd"
logfile=$diskdir/playlistcd_$disknumd.log
errlogfile=$diskdir/playlistcd.$$.errlog
rm -f $errlogfile

if [[ -d "$staging" ]]; then
    echo Directory $staging already exists. Remove? Y/N
    read -e ans
    case $ans in
        Y|y)
            rm -rf "$staging"
            
            ;;
        N|n)
            exit 2
            ;;
        *)
            echo Invalid Response
            exit 2
            ;;
    esac
fi


mkdir -p "$staging"


cp -f "$scriptpath/autorun_suppress.inf" "$staging/autorun.inf"


passes=2

if [[ "$SPEECH_ENGINE" = FreeTTS || "$passes" = 2 ]]; then
    pass1="pass1=1"
    pass2="pass1= pass2=2"
    pass2fn="$cdsource"
else
    pass1="pass1=1 pass2=2"
    pass2=
    pass2fn=
fi

rm -f "$TEMPDIR"/*-title.wav

tagbkup=tagbkup
#if [[ `uname -s` = CYGWIN* ]]; then
#    tagbkup=tagbkup_cygwin
#fi

# Language options
voicesfile=
if [[ -f "$JAMPAL_USER_DIR/voices.txt" ]] ; then
    voicesfile="$JAMPAL_USER_DIR/voices.txt"
else
    if [[ -f "$scriptpath/voices.txt" ]] ; then
        voicesfile="$scriptpath/voices.txt"
    fi
fi

SPEECH_RATE=`grep '^speech-rate' "$JAMPAL_USER_DIR/jampal_initial.properties" |sed 's/^.*=//'`
if [[ "$SPEECH_RATE" == "" ]]; then
    echo "ERROR Speech Rate not found. Please run Jampal GUI and select a speech rate"
    exit 99
fi

####################
# awk program begins
####################

ret=0
awk -v "tempdir=$TEMPDIR" -v disknum=$disknum \
    -v tagbkup=$tagbkup -v "voicesfile=$voicesfile" -v "languagesfile=$scriptpath/ISO-639-2_utf-8.txt" \
    -v "customtag=$customtag" '

# This program is called twice, pass1=1 and pass2=2

function trim(trimmit) {
    gsub(/^ */,"",trimmit)
    gsub(/ *$/,"",trimmit)
    return trimmit
}

# This removes diacritics and also slashes and reverse slashes
function removeDiacritics(theString) {
    gsub(/[^0-9A-Za-z ,._'"'"'&\(\)#\[\]!{}=;@-]/,   "",theString)
    return theString
}

function process(record,counter) {
    if (record!="") {
        save=$0
        $0=record
    }

    # assign field names in database
    '"`cat "$TEMPDIR/$playlistlibname.awkset"`"'

    if (ID3V2TAGTXXXDirectory == "")
        DIRALBUM=ALBUM
    else
        DIRALBUM=ID3V2TAGTXXXDirectory

    split(DUMMY,keyflds,":")

    if ( dirnum != keyflds[2] ) {
        dirnum=keyflds[2]
# Bug - tagupdate cannoty handle diacritics in file name
        dirname = trim(dirnum  " " '"$dirchange"')
#       dirname = dirnum
        # Drop dots 
        gsub(/\./," ",dirname)
        # Drop quotes
        gsub(/\"/,"",dirname)
        # Duplicate spaces
        gsub(/  */," ",dirname)
        # Change colons or slashes to commas
        gsub(/:|\//,", ",dirname)
        # Trim
        gsub(/^ */,"",dirname)
        gsub(/ *$/,"",dirname)
        # Question marks
        gsub(/\?/,"",dirname)
        dirname = substr(dirname,1,64)
        print "mkdir -p \"" staging "/" dirname "\"" | "/bin/sh"
        shIsOpen=1
    }

    filenum=keyflds[3]
    subfile=keyflds[4]

    saveTITLE=TITLE
    saveARTIST=ARTIST

    musicfile=JFILENAME

    while (system("test -f \"" musicfile "\"")) {
        searchforfile()
        if (musicfile=="") {
            print saveTITLE " - " saveARTIST " **** Excluded ****"
            if (save!="") {
                $0=save
                save=""
            }
            return
        }
    }


    print "PASS " pass1 " " pass2 " " dirnum "-" filenum "-" subfile " " TITLE " - " ARTIST 


#    pathsplitnum=split(musicfile,filepart,"/")
#    musicname=filepart[pathsplitnum]
#    sub(/^[A-Z0-9][0-9][0-9] /,"",musicname)
    musicname = substr(TITLE,1,24) " - " substr(ARTIST,1,25) ".mp3"
    musicname = removeDiacritics(musicname)
    outfilename=staging "/" dirname "/" sprintf("%3.3d ",filenum) musicname

    musicdone=0
    if (announce == "Y") {
        mylang = substr(ID3V2TAGTLAN,length(ID3V2TAGTLAN)-3+1)
        mylangfixed = langfix [mylang]
        if (mylangfixed != "")
            mylang = mylangfixed
        engine = engines [mylang]
        voice = voices [mylang]
        speechVolume = volumes [mylang]
        if (engine == "") {
            engine = engines ["other"]
            voice = voices ["other"]
            speechVolume = volumes ["other"]
        }
        if (engine == "") {
            print "ERROR No speech engine found. Language " mylang " or other."
            print "Select a speech engine in Jampal GUI."
            exit 99
        }
        if (engine == "eSpeak" && voice == "" && mylang != "")
            voice = shortlangs[mylang]
    }
    if (announce == "Y" && engine != "None" && engine != "") {
        if (counter == 0) {
            announcement = '"$speak"'
        }
        if (announcement != "") {
            if (SAMPLERATE == "" )
                SAMPLERATE=44100
            # Get rid of quotes which mess up the command line
            gsub(/"/,"",announcement)
            wavefilename= tempdir "/" dirnum "-" filenum "-" subfile "-title.wav"
            if (pass1==1) {
                if (voice != "")
                    voiceparm = "-voice \"" voice "\" "
                else
                    voiceparm=""
                pttsVolume = speechVolume
                if (pttsVolume >= 100)
                    pttsVolume = 100
                rateparm = ""
                bgParm = ""
                if (engine != "FreeTTS") {
                    rateparm = " -s " SAMPLERATE
                    if (pass2!=2)
                        bgParm = " &"
                }
                encoding = " -e utf-8 "
                # removed " >> " errlogfile
                command = "echo \"" announcement "\" | \"" scriptpath "/ptts.sh\" -engine " engine " -batch " voiceparm           encoding " -r " speechRate "  -w " wavefilename " -v " pttsVolume  rateparm  bgParm
                # This sometimes hangs for unknown reason so using code below instead
                #if (system(command)) {
                #    print command > "/dev/stderr"
                #    print "ptts error" > "/dev/stderr"
                #    exitcode=2
                #}
                # This instead of system() call
                print command | "/bin/sh"
                shIsOpen=1
                # close("/bin/sh")
            }
            if (pass2==2) {
                if (engine == "FreeTTS" || engine == "eSpeak"  || speechVolume > 100) {
                    sox_ok = "Y"
                    # Find length of announcement in seconds
                    command = "sox " wavefilename " " tempdir "/fixed-title.wav stat 2>&1 | grep Length " 
                    FS=" "
                    $0 = ""
                    command | getline
                    lengthSeconds = $3
                    trimparm = ""
                    FS="\t"
                    if (lengthSeconds == "" || lengthSeconds == 0) {
                        print "Command:" command > "/dev/stderr"
                        print "sox error" > "/dev/stderr"
                        print "result was " $0 > "/dev/stderr"
                        lengthSeconds = ""
                        sox_ok = "N"
                        exitcode=2
                    }
                    else {
                        # remove 0.1 seconds because otherwise sox picks up some garbage sound and puts it at the end
                        trimparm = " trim 0 "
                        lengthSeconds = lengthSeconds - 0.1
                    }

                    command = "sox " wavefilename "  -r " SAMPLERATE " -c 2 " tempdir "/fixed-title.wav vol " speechVolume/100 trimparm lengthSeconds " 2>> " errlogfile " 1>&2 "
                    #print command | "/bin/sh"
                    if (system(command)) {
                        print "Command:" command > "/dev/stderr"
                        print "sox error" > "/dev/stderr"
                        sox_ok = "N"
                        exitcode=2
                    }
                    if (sox_ok == "Y")
                        wavefilename = tempdir "/fixed-title.wav"
                }

                # removed  2>> " errlogfile " 1>&2 "
                command = "rm -f " tempdir "/title.mp3"
                #print command | "/bin/sh"
                if (system(command)) {
                    print "Command failed:" command > "/dev/stderr"
                }
                command = "lame -t --quiet -f " wavefilename " " tempdir "/title.mp3 >> " errlogfile " 2>&1 "
                #print command | "/bin/sh"
                if (system(command)) {
                    print "Command:" command > "/dev/stderr"
                    print "lame error" > "/dev/stderr"
                    exitcode=2
                }
                if (ann_merge == "Y") {
                    #command = "\"" tagbkup "\" -c2 \"" musicfile "\" \"" outfilename "\" 2>> " errlogfile
                    #print command | "/bin/sh"
                    if (system("\"" tagbkup "\" -c2 \"" musicfile "\" \"" outfilename "\" 2>> " errlogfile)) {
                        print "tagbkup -c2 error" > "/dev/stderr"
                        exitcode=2
                        exit
                    }
                    #command = "\"" tagbkup "\" -cm " tempdir  "/title.mp3 \"" outfilename "\" 2>> " errlogfile
                    #print command | "/bin/sh"
                    if (system("\"" tagbkup "\" -cm " tempdir  "/title.mp3 \"" outfilename "\" 2>> " errlogfile)) {
                        print "tagbkup -cm error" > "/dev/stderr"
                        exitcode=2
                    }
                    #command = "\""  tagbkup "\" -cm -c1 \"" musicfile "\" \"" outfilename "\" 2>> " errlogfile
                    #print command | "/bin/sh"
                    if (system("\""  tagbkup "\" -cm -c1 \"" musicfile "\" \"" outfilename "\" 2>> " errlogfile)) {
                        print "tagbkup -cm -c1 error" > "/dev/stderr"
                        exitcode=2
                        exit
                    }
                    musicdone=1
                }
                else {
                    #command = "mv " tempdir  "/title.mp3 \"" staging "/" dirname "/" sprintf("%3.3d ",filenum-1) "title.mp3\""
                    #print command | "/bin/sh"
                    if (system( "mv " tempdir  "/title.mp3 \"" staging "/" dirname "/" sprintf("%3.3d ",filenum-1) "title.mp3\"")) {
                        print "mv title.mp3 error" > "/dev/stderr"
                        exitcode=2
                    }
                }
            }
        }
    }
    if (pass2==2 && !musicdone) {
        #command = "cp \"" musicfile  "\" \"" outfilename "\""
        #print command | "/bin/sh"
        if (system( "cp \"" musicfile  "\" \"" outfilename "\"")) {
            print "cp musicfile error" > "/dev/stderr"
            exitcode=2
            exit
        }
    }

    if (pass2==2) {
        tagset=""
        if (appendseq=="Y") {
            nextseq = (++outputcount) + (disknum-1) * 1000
            tagset = " -TITLE \"" sprintf("%5.5d",nextseq) " " TITLE "\" "
        }
        if (customtag!="")
            tagset = tagset " " '"$customtag"'
        if (tagset != "") {
            tagset = tagset " \"" outfilename "\""
            print tagset > tempdir "/playlistcd_tag.data"
            command = "\""  JAVA_COMMAND "\" -cp \"" JAMPAL_CLASSPATH "\" pgbennett.id3.TagUpdate " \
            "-OPTIONFILE " tempdir "/playlistcd_tag.data -ENCODING utf-8"
            close(tempdir "/playlistcd_tag.data")
#                tagset " \"" outfilename "\""
            #print command | "/bin/sh"
            if (system(command)) {
                print command > "/dev/stderr"
                print "tagUpdate error" > "/dev/stderr"
                exitcode=2
                exit
            }
        }
    }

    announcement=""

    if (save!="") {
        $0=save
        save=""
    }
}

function searchforfile() {
    print "Searching " mainlibdata " " TITLE " - " ARTIST ," Not found: " JFILENAME " ..." > "/dev/stderr"
    title=removeDiacritics(tolower(TITLE))
    artist=removeDiacritics(tolower(ARTIST))
    $0=""
    while ($0=="") {

        ret=1;
        TITLE=""
        ARTIST=""
        while (ret==1 && (title!=removeDiacritics(tolower(TITLE)) || artist!=removeDiacritics(tolower(ARTIST)))) {
            ret = getline < mainlibdata
            if (ret!=1)
                break
            # assign field names in database
            '"`cat "$TEMPDIR/$libname.awkset"`"'
            if (substr($1,1,1) != "A") {
                print "Library needs to be saved: " JFILENAME
                exitcode=2
                exit
            }
        }
        ret2=1
        while (ret2==1) {
            ret2 = getline junk < mainlibdata
            if (ret2!=1)
                break
            if (!match(junk,/^A/)) {
                print "Library needs to be saved " 
                ret=-1
            }
        }
        if (ret==1) {
            musicfile=JFILENAME
            print "found " musicfile
        }
        close(mainlibdata)
        if (ret!=1 || system("test -f \"" musicfile "\"")) {
            $0=""
            musicfile=""
            print "No match for " title " by " artist >  "/dev/stderr"
            print "Enter Title, C to cancel, R to retry" > "/dev/stderr"
            getline ttitle < "/dev/stdin"
            ttitle=removeDiacritics(tolower(ttitle))
            if (ttitle=="c") {
                exitcode=2
                exit
            }
            if (ttitle=="r"||ttitle=="")
                continue
            title=ttitle
            print "Enter Artist" > "/dev/stderr"
            getline artist < "/dev/stdin"
            artist=removeDiacritics(tolower(artist))
        }
    }
}

BEGIN {
    FS="\t"
    dirnum=""
    # Load voices file
    if (voicesfile != "") {
        # sample input lines
        #|eSpeak||
        #other|eSpeak||
        #eng|eSpeak|english-us|
        rc = 1
        while (rc > 0) {
            rc = getline inputLine < voicesfile
            if (rc <= 0)
                break
            if (substr(inputLine,1,1) != "#" && length(inputLine)>0) {
                split(inputLine,parts,/\|/)
                # Language/Engine/Voice/ 
                engines[parts[1]] = parts[2]
                voices[parts[1]] = parts[3]
                volumes[parts[1]] = parts[4]
            }
        }
    }
    # Load languages file
    if (languagesfile != "") {
        # sample input lines
        #ger|deu|de|German|allemand
        #eng||en|English|anglais
        rc = 1
        while (rc > 0) {
            rc = getline inputLine < languagesfile
            if (rc <= 0)
                break
            if (substr(inputLine,1,1) != "#" && length(inputLine)>0) {
                split(inputLine,parts,/\|/)
                # Biblio/Terminol/Name/French Name 
                thelang = parts[1]
                if (length(parts[2]) == 3) {
                    langfix[parts[1]] = parts[2]
                    thelang = parts[2]
                }
                if (length(parts[3]) == 2)
                    shortlangs[thelang] = parts[3]
            }
        }
    }
}

END {
    for (i=1;;i++) {
        if (i in followArray) {
            process(followArray[i],i)
            delete followArray[i]
        }
        else
            break;
    }
    exit exitcode
}

/\r$/ {
    # Remove any trailing carriage return
    sub(/\r$/,"")
}


pass1 == 1 && started == 0 {
    #command = "\"" scriptpath "/ptts.sh\" -start"
    #print command | "/bin/sh"
    system("\"" scriptpath "/ptts.sh\" -start")
    started = 1
}

pass2 == 2 && started == 1 {
    #command = "\"" scriptpath "/ptts.sh\" -stop"
    #print command | "/bin/sh"
    system("\"" scriptpath "/ptts.sh\" -stop")
    proc = ""
    if (announce == "Y") {
#        if (engine == "Cepstral")
#            proc = "swift"
#        if (engine == "Microsoft")
#            proc = "ptts"
#        if (engine == "eSpeak")
#            proc = "espeak"
        proc = "swift|ptts|espeak"
        if (proc != "") {
            if (shIsOpen) {
                close("/bin/sh")
                shIsOpen = 0
            }
            while( system("sleep 2s ; ps | egrep \"" proc "\" >/dev/null ")==0) {
                print "Waiting for " proc
            }
        }
    }
    started = 2
}

{

    # assign field names in database
    '"`cat "$TEMPDIR/$playlistlibname.awkset"`"'

    # Make sure playlist is still sorted
    if (DUMMY < prevDUMMY) {
        print "ERROR - Playlist file is out of sequence: " DUMMY " - "  TITLE " - " ARTIST
        exitcode=2
        exit
    }

    $ Get components of disk number
    split(DUMMY,keyflds,":")
    if ( disknum+0 != keyflds[1]+0 )
        next

    ttsubfile=keyflds[4]
    ttspeak = '"$speak"'

# print DUMMY " prev " prev_sub_file
    if (ttsubfile+0 <= prev_sub_file+0) {
# print "Spit it out"
        for (i=1;;i++) {
            if (i in followArray) {
                process(followArray[i],i)
                delete followArray[i]
            }
            else
                break;
        }
    }
    prev_sub_file = ttsubfile
    if (ttsubfile+0 > 0) {
# print "saving"
        print "[" TITLE " - " ARTIST "]"
        followArray[ttsubfile+0]=$0
        if (announce == "Y") {
            if (announcement != "")
                announcement = announcement '"$speaksep"'
            announcement = announcement " " ttspeak
        }
        next
    }
#    else {
#        for (i=1;;i++) {
#            if (i in followArray) {
#                process(followArray[i],i)
#                delete followArray[i]
#            }
#            else
#                break;
#        }
#    }

    process("",0)

}'  mainlibdata="$mainlibdata" \
    "staging=$staging"  \
    announce=$announce ann_merge=$ann_merge \
    "errlogfile=$errlogfile" \
     speechRate="$SPEECH_RATE" \
     scriptpath="$scriptpath" \
     JAVA_COMMAND="$JAVA_COMMAND" JAMPAL_HOME="$JAMPAL_HOME" appendseq=$appendseq \
     JAMPAL_CLASSPATH="$JAMPAL_CLASSPATH" \
     cdsource="$cdsource" \
     $pass1  "$cdsource" \
     $pass2 dirnum= "$pass2fn" || ret=$?
if [[ "$ret" != 0 ]]; then echo ERROR $ret, see $errlogfile; exit 2; fi
rm -f $TEMPDIR/*-title.wav
rm -f $TEMPDIR/title.mp3
rm -f $TEMPDIR/$MAINLIBNAME.profile $TEMPDIR/$MAINLIBNAME.awkset $TEMPDIR/$MAINLIBNAME.awkset2
rm -f $TEMPDIR/$libname.profile $TEMPDIR/$libname.awkset $TEMPDIR/$libname.awkset2
rm -f $TEMPDIR/$playlistlibname.profile $TEMPDIR/$playlistlibname.awkset $TEMPDIR/$playlistlibname.awkset2
rm -f $errlogfile
