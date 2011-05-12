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



DATETIME=`date "+%Y%m%d_%H%M%S"`

progpath=/usr/local/bin
backuppath="$TAGBKUP/tagbkup_cd"
rejectpath="$TAGBKUP/tagbkup_cd_reject"
logpath="$TAGBKUP/logs"

listfile="$logpath/${DATETIME}_tagbkup_cd_full_list.txt"
outfile="$logpath/${DATETIME}_tagbkup_cd.out"
logfile="$logpath/${DATETIME}_tagbkup_cd.log"

if [[ ! -d "$backuppath" ]]; then
    mkdir -p "$backuppath"
fi

if [[ ! -d "$rejectpath" ]]; then
    mkdir -p "$rejectpath"
fi
if [[ ! -d "$logpath" ]]; then
    mkdir -p "$logpath"
fi


rm -f "$listfile"

OS=`uname -s`

tagbkup=tagbkup
#if [[ $OS = CYGWIN* ]]; then
#    tagbkup=tagbkup_cygwin
#fi

if [[ $OS = CYGWIN* ]]; then
    drive=`cygpath -w $CDROM`
    drive=${drive%\\}
fi

while [[ 1 = 1 ]]; do
    if [[ $OS != CYGWIN* ]]; then
        umount $CDROM 2>/dev/null
    fi
    echo mount a cd on $CDROM and press enter. When done type N
    read -e a
    if [[ "$a" = N ]]; then
        break;
    fi
    prev=vol
    if [[ $OS = CYGWIN* ]]; then
        vol=`label $drive </dev/null |grep "in drive .:"|sed "s/Volume in drive .: is *//"`
    else
        ret=0
        mount $CDROM || ret=$?
        if [[ $ret != 0 ]]; then
            continue
        fi
        cddev=`( cd $CDROM && here="$(df -h . | grep /dev)"; echo ${here/ */} )`
        vol=`volname $cddev`
    fi

    if [[ "$vol" = "" ]]; then
        continue
    fi
    if [[ "$vol" = "$prev" ]]; then
        continue
    fi
    echo
    echo volume label is $vol
    echo Press enter or type corrected volume label
    read -e repair
    if [[ "$repair" != "" ]]; then
        vol="$repair"
    fi
    echo "Processing Volume $vol ..."
    rm -f /tmp/$vol
    ln -s $CDROM /tmp/$vol

    opt="$1"
    rm -f $listfile
    find  /tmp/$vol -follow -name '*.mp3' >>"$listfile"

    $tagbkup -b -t -u -y -n -x -f "$listfile" \
      "$backuppath" 2>&1 |tee $outfile
    rm -f /tmp/$vol
done
if [[ $OS != CYGWIN* ]]; then
    umount $CDROM 2>/dev/null
fi
cp -f "$backuppath/tagbkup.log" "$logfile"

