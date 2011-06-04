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
option="$1"

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
. "$scriptpath/mp3.profile"

if [[ ! -x `which lame` ]] ; then
    echo "ERROR This operation requires lame installed and on the path" >&2
    echo "Please install lame" >&2
    exit 2
fi

if [[ "$option" = recode || "$option" = recode-even ]]; then
    dest="recode/"
    opt="--mp3input"
    mkdir -p recode
    pattern='*.mp3'
else
    dest=
    opt=
    pattern='*.wav *.WAV'
fi

echo Encode to mp3 all wav files that are in current directory.
echo option "recode" to re-encode mp3 files that are above 128kbps
echo option "recode-even" to re-encode all mp3 files

num_of_processors=`cat /proc/cpuinfo | grep processor | awk '{a++} END {print a}'`

tagbkup=tagbkup
#if [[ `uname -s` = CYGWIN* ]]; then
#    tagbkup=tagbkup_cygwin
#fi

let count=0 1
rm -f ErrorReport.txt
for file in $pattern 
do
    if (( count >= $num_of_processors )) ; then
        let count=0 1
        wait
    fi
    let count=count+1 1
    bname="${file%.*}"
    if [[ -f "$dest$bname.mp3" ]]; then
        echo "$file already done ***"
        mv "$file" "${file}.done"
        continue
    fi
    if [[ ! -f "${file}" ]]; then
        echo "$file not found ***"
        continue
    fi
    if [[ "$option" = recode ]]; then
        "$scriptpath/tagupdate.sh" -DISPLAY "$file" > "$TEMPDIR"/temp.txt
        set -- `grep -a "^BitRate:" "$TEMPDIR"/temp.txt`
        bitrate=${2%kbps}
        if (( bitrate <= 128 )); then 
            echo "$file is already $bitrate kbps"
            continue
        fi
    fi
    (
        echo "Encoding $file"
        lame -t -b 128 -h $opt --quiet "${file}" "${dest}$bname.mp3.incomplete" 
        sleep 0.2
        mv "${dest}$bname.mp3.incomplete" "${dest}$bname.mp3"
        if [[ "$option" = recode ]]; then
            "$tagbkup"  -c -v "${file}" "${dest}$bname.mp3"
        fi
        mv "${file}" "${file}.done"
    ) &
done
wait
echo done

