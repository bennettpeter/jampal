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

pattern='*.wav *.WAV'
dest=trim/

echo Trim all wav files that are in current directory.

num_of_processors=`cat /proc/cpuinfo | grep processor | awk '{a++} END {print a}'`
mkdir -p $dest
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
    if [[ -f "$dest$bname.wav" ]]; then
        echo "$file already done ***"
        mv "$file" "${file}.done"
        continue
    fi
    if [[ ! -f "${file}" ]]; then
        echo "$file not found ***"
        continue
    fi
    (
        echo "Trim $file"
        sox "$file" "$dest$bname.wav" silence 1 5 0.1% reverse silence 1 5 0.1% reverse
        sleep 0.2
        mv "${file}" "${file}.done"
    ) &
done
wait
echo done

