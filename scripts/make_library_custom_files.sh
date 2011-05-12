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

# parameter - optional library name. Default main library

libname="$MAINLIBNAME"
library="$LIBDIR/$MAINLIBNAME".jampal

reqlib=$1
reqlib=${reqlib%.jampal}
if [[ "$1" != "" ]]; then
    libname=$reqlib
    library=$reqlib.jampal
    if [[ "$reqlib" = `basename "$reqlib"` ]]; then
        libname="$LIBDIR/$reqlib"
        library="$LIBDIR/$reqlib".jampal
    fi
fi

mkdir -p "$TEMPDIR"
outputname=$TEMPDIR/`basename "$libname"`

awk '

BEGIN { 
}

/\r$/ {
    # Remove any trailing carriage return
    sub(/\r$/,"")
}


{
    if (continuation) {
        continuation=0
        equalLoc = match($0,/[^ ]/)
        libraryCols = libraryCols substr($0,equalLoc)
        if ((substr(libraryCols,length(libraryCols))) == "\\") {
            libraryCols = (substr(libraryCols,1,length(libraryCols)-1))
            continuation=1
        }
    }
}


/^library-cols *=/ {
    equalLoc = match($0,/=/)
    libraryCols = substr($0,equalLoc+1)
    equalLoc = match(libraryCols,/[^ ]/)
    libraryCols = substr(libraryCols,equalLoc)
    if ((substr(libraryCols,length(libraryCols))) == "\\") {
        libraryCols = (substr(libraryCols,1,length(libraryCols)-1))
        continuation=1
    }
}

/libraryname *=/ {
    equalLoc = match($0,/=/)
    libraryName = substr($0,equalLoc+1)
    equalLoc = match(libraryName,/[^ ]/)
    libraryName = substr(libraryName,equalLoc)
}

/type *=/ {
    equalLoc = match($0,/=/)
    libraryType = substr($0,equalLoc+1)
    equalLoc = match(libraryType,/[^ ]/)
    libraryType = substr(libraryType,equalLoc)
}



END {
    gsub(/[^A-Za-z0-9,]/,"_",libraryCols)
    count = split(libraryCols,array,",")
    for (ix=1; ix<=count; ix+=4) {
        namePart2 = ""
        description = array[ix+2]
        if (array[ix] == "ID3V2TAG")
            namePart2 = array[ix+1]
        # FILENAME is a reserved word so change it to JFILENAME
        if (array[ix] == "FILENAME")
            array[ix] = "JFILENAME"
        print array[ix] namePart2 "=" 1+(ix+3)/4 > outputname ".profile"
        print array[ix] namePart2 " = $" 1+(ix+3)/4 > outputname ".awkset"
        print "$" 1+(ix+3)/4 " = " array[ix] namePart2  > outputname ".awkset2"
        print description  " : " array[ix] namePart2 > outputname ".fieldcodes"
    }
    print "FIELDCOUNT=" 1+(ix-1)/4 > outputname ".profile"
    print "LIBRARYDATANAME=" libraryName > outputname ".profile"
    print "LIBRARYTYPE=" libraryType > outputname ".profile"
}

' "outputname=$outputname" "$library"


