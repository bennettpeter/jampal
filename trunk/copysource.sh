#!/bin/bash

set -e

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath

if [[ "$1" != ubuntu && "$1" != debian ]] ; then
    echo "Please specify debian or ubuntu"
    exit 2;
fi

system="$1"

export VERSION=`cat VERSION`
echo Copy new version of jampal $VERSION from host to local

# do not make source - download it
# make source

rm -rf ~/proj/jampal/jampal-$VERSION*
rm -rf ~/proj/jampal/jampal-source-$VERSION*
rm -rf ~/proj/jampal/jampal_$VERSION*
rm -f  ~/proj/jampal/*.deb
mkdir -p ~/proj/jampal

UPSTRMVERSION1=`dpkg-parsechangelog|egrep '^Version:'|cut -f2 -d' '|cut -f1 -d+`
UPSTRMVERSION2=`dpkg-parsechangelog|egrep '^Version:'|cut -f2 -d' '|cut -f1 -d-`
tarfile=jampal-$UPSTRMVERSION2.orig.tar.gz
sourcedir=jampal-$UPSTRMVERSION1-orig

if [[ ! -f package/source/jampal-$UPSTRMVERSION2.orig.tar.gz ]] ; then
    debian/rules get-orig-source
    mv jampal-$UPSTRMVERSION2.orig.tar.gz package/source
fi

cp package/source/jampal-$UPSTRMVERSION2.orig.tar.gz \
  ~/proj/jampal/jampal_$UPSTRMVERSION2.orig.tar.gz
cd ~/proj/jampal

tar xf jampal_$UPSTRMVERSION2.orig.tar.gz

rsync -aC $scriptpath/$system $sourcedir/

if [[ "$system" != debian ]] ; then
    rm -rf $sourcedir/debian
    mv $sourcedir/$system $sourcedir/debian
fi

