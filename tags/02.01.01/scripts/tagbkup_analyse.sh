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


if [[ $# = 0 ]]; then
    CDM=1
    HDD=1
fi

while (( $# > 0 )); do
    case $1 in 
    HDM)
        HDM=1
        ;;
    CDM)
        CDM=1
        ;;
    HDD)
        HDD=1
        ;;
    CDD)
        CDD=1
        ;;
    *)
        error=true
        break;
    esac
    shift
done


if [[ "$error" = true ]]; then
    echo This script compares the results of tagbkup and tagbkup_cd
    echo usage:
    echo $0 [HDM] [CDM] [HDD] [CDD]
    echo HDM - files missing from Hard Drive
    echo CDM - files missing from CD
    echo HDD - files duplicate on Hard Drive
    echo CDD - files duplicate on CD
    echo Default if there is no parameter - CDM HDD
    exit 2
fi

hdbackuppath="$TAGBKUP/tagbkup"
cdbackuppath="$TAGBKUP/tagbkup_cd"

mkdir -p $TEMPDIR

sort -u "$hdbackuppath/tagbkup.log" "$cdbackuppath/tagbkup.log"  > "$TEMPDIR/tagbkup_combo.sort"

# 1 extra record to make sure last entry is processed
echo >> "$TEMPDIR/tagbkup_combo.sort"

awk '
{
    key = substr($1,1,16)
    if (prevkey != key) {
        if (HDM && hdcount == 0 && cdcount > 0)
            print "Missing from HD: " cd_record
        if (CDM && hdcount > 0 && cdcount == 0)
            print "Missing from CD: " hd_record
    }
    else {
        if (substr($2,1,4) == "/tmp") {
            iscd=1
            cdcount++
        }
        else {
            iscd=0
            hdcount++
        }
        if (HDD && hdcount > 1 && !iscd) {
            if (hdcount==2)
                print "Duplicate on HD: " hd_record
            print "Duplicate on HD: " $0
        }
        if (CDD && cdcount > 1 && iscd) {
            if (cdcount==2)
                print "Duplicate on CD: " cd_record
            print "Duplicate on CD: " $0
        }
    }
#    record = $0
    if (prevkey != key) {
        prevkey = key
        hdcount=0
        cdcount=0
        if (substr($2,1,4) == "/tmp") {
            iscd=1
            cdcount++
        }
        else {
            iscd=0
            hdcount++
        }
    }
    record = $0
    if (iscd)
        cd_record = $0
    else
        hd_record = $0

} ' HDD=$HDD CDD=$CDD HDM=$HDM CDM=$CDM "$TEMPDIR/tagbkup_combo.sort" > "$TEMPDIR/unmatched.out"

sort < "$TEMPDIR/unmatched.out" > "$TEMPDIR/unmatched.sort"

while [[ true ]]; do 
    echo "Results in $TEMPDIR/unmatched.sort"
    echo Type vi or name of editor to edit it, or enter to exit.
    read -e resp
    if [[ "$resp" = "" ]]; then
        exit
    fi
    "$resp" "$TEMPDIR/unmatched.sort"
done
