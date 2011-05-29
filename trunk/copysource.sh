#!/bin/bash

set -e

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath


export VERSION=`cat VERSION`
echo Copy new version of jampal $VERSION from host to local

# do not make source - download it
# make source

rm -rf ~/proj/jampal/jampal-$VERSION*
rm -rf  ~/proj/jampal/jampal_$VERSION*
rm -f  ~/proj/jampal/*.deb
mkdir -p ~/proj/jampal

# cp package/source/jampal-$VERSION.tar.gz ~/proj/jampal/jampal-$VERSION.tar.gz
cp package/source/jampal-$VERSION.tar.gz ~/proj/jampal/jampal_$VERSION.orig.tar.gz
cd ~/proj/jampal
tar xf jampal_$VERSION.orig.tar.gz

rsync -aC ~/proj/jampal.svn.sourceforge.net/svnroot/jampal/trunk/debian \
    ~/proj/jampal/jampal-$VERSION/

