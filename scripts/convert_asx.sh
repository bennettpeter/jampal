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

# Convert MS Media player playlist to csv file
# Input format xml
# Output Format
# Album	Artist	Composer	Genre	Track Title	Track #	Time
# use with asx_audacity.ods to generate a tag update script


scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
. "$scriptpath/mp3.profile"

if [[ "$1" = "" ]]; then
    echo ""
    echo "Convert media player playlist asx format. Usage:"
    echo "$0 inputfile"
    echo "output file name will be input file name followed by encoding and .csv"
    exit 2
fi

# UTF-8 file indicated by <Param Name = "encoding" Value = "UTF-8" />

awk -v infile="$1" '

BEGIN { 
    main_fs = ">|<|\""
    FS=main_fs
    OFS="\t"
    outfile = infile ".cp1252.csv"
}

/<Param *Name *= *"encoding" *Value *= *"UTF-8" *\/>/ {
    outfile =  infile ".utf-8.csv"
}


/<\/Entry>/ {
    if (entryfound) {
        if (!titlePrinted) {
            print "Album \t Artist \t Composer \t Genre \t Title \t TrackNo \t Time \t Year" > outfile
            titlePrinted = 1
        }
        print Album "\t" Artist "\t" Composer "\t" Genre "\t" Title "\t" TrackNo "\t" Time  "\t" Year > outfile
        entryfound=0
        Album=""
        Artist=""
        Composer=""
        Genre=""
        Title=""
        TrackNo=""
        Year=""
    }
    entryfound=0
}

/<Entry>/ {
    entryfound=1
}

# <Param Name = "WM/AlbumTitle" Value = "Carte Blanche" />
/WM\/AlbumTitle/ {
#    print "composer" 1 $1 2 $2 3 $3 4 $4 5 $5 6 $6
    Album=$5
}


# <Author > Carte De Sejour & Rachid Taha</Author>
/<Author *>/ {
    FS=">|<"
    gsub(/"/,"'"'"'")
    Artist=$3
    FS=main_fs
}

# <Param Name = "WM/Composer" Value = "Rachid Taha" />
/WM\/Composer/ {
#    print "composer" 1 $1 2 $2 3 $3 4 $4 5 $5 6 $6
    Composer=$5
}

# <Param Name = "WM/Genre" Value = "Pop" />
/WM\/Genre/ {
    Genre=$5
}

# <Title > Bleu De Marseille</Title>
/<Title *>/ {
    FS=">|<"
    gsub(/"/,"'"'"'")
    Title=$3
    FS=main_fs
}

# <Param Name = "WM/TrackNumber" Value = "3" />
/WM\/TrackNumber/ {
#    print "composer" 1 $1 2 $2 3 $3 4 $4 5 $5 6 $6
    TrackNo=$5
}

# <Duration value = "00:04:33.000" />
/<Duration/ {
#    print "time" 1 $1 2 $2 3 $3 4 $4 5 $5 6 $6
    Time=$3
}


# <Param Name = "WM/Year" Value = "2007" />
/WM\/Year/ {
#    print "composer" 1 $1 2 $2 3 $3 4 $4 5 $5 6 $6
    Year=$5
}


'   \
   "$1"

