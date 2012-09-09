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

# input and output file names are required
# if output file name does not exist works like mv
# if output file exists creates a subdirectory 00x for
# the new version

test=

infile="$1"
outfile="$2"

if [ ! \( -f "$infile" \) ]
then
    echo "File not found: $infile"
    exit
fi

# outfile=`cygpath -u "$outfile"`

if [ -f "$outfile" ]
then
    name=`basename "$outfile"`
    dir=`dirname "$outfile"`
fi
counter=1000
while [ -f "$outfile" ]
do
    let counter=counter+1
    $test mkdir -p "$dir/${counter:1}"
    outfile="$dir/${counter:1}/$name"
done

rc=0
$test mv -i "$infile" "$outfile" || rc=$?
if [ $rc = 0 ]
then
    echo "Done: $outfile"
fi