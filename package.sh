#!/bin/bash

set -e

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath

export VERSION=`git describe --dirty|sed s/^v//`
echo Package binary package jampal $VERSION
make clean

newfile=debian/changelog_new
echo "jampal (${VERSION}) unstable; urgency=low" > $newfile
echo "" >> $newfile
echo "  * Update release for package" >> $newfile
echo "" >> $newfile
echo " -- Peter Bennett <pgbennett@comcast.net>  `date -R`" >> $newfile
echo "" >> $newfile
cat debian/changelog >> $newfile
mv -f $newfile debian/changelog

dpkg-buildpackage -b
