#!/bin/bash

set -e

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath

if [[ "$1" == "" ]] ; then
    echo "Please specify debian, debian_sf, debian_ppa, etc."
    exit 2;
fi

system="$1"

export VERSION=`cat VERSION`
echo Copy new version of jampal $VERSION from host to local

# do not make source - download it
# make source

rm -rf ~/proj/jampal/jampal-$VERSION/*
# rm -rf ~/proj/jampal/jampal-source-$VERSION*
# rm -rf ~/proj/jampal/jampal_$VERSION*
mkdir -p ~/proj/jampal

# UPSTRMVERSION1 does not have the dfsg1 in it e.g. jampal_02.01.06
UPSTRMVERSION1=`dpkg-parsechangelog -l$system/changelog|egrep '^Version:'|cut -f2 -d' '|cut -f1 -d+`
# UPSTRMVERSION2 does have the dfsg1 in it e.g. jampal_02.01.06+dfsg1
UPSTRMVERSION2=`dpkg-parsechangelog -l$system/changelog|egrep '^Version:'|cut -f2 -d' '|cut -f1 -d-`
tarfile=jampal_$UPSTRMVERSION2.orig.tar.gz
sourcedir=jampal-$UPSTRMVERSION1

if [[ -f package/source/jampal-source-$UPSTRMVERSION1.tar.gz ]] ; then
    cp package/source/jampal-source-$UPSTRMVERSION1.tar.gz ./
fi
#debian/rules get-orig-source
#mkdir -p package/source
#cp jampal_$UPSTRMVERSION2.orig.tar.gz package/source

#cp package/source/jampal_$UPSTRMVERSION2.orig.tar.gz \
#  ~/proj/jampal/jampal_$UPSTRMVERSION2.orig.tar.gz
# cd ~/proj/jampal

rm -rf ~/proj/jampal/$sourcedir/

mkdir -p ~/proj/jampal/$sourcedir/debian/
cp -fpr debian/* ~/proj/jampal/$sourcedir/debian/
cp -fpr $system/* ~/proj/jampal/$sourcedir/debian/
cd ~/proj/jampal/$sourcedir
./debian/rules get-orig-source
mv jampal_$UPSTRMVERSION2.orig.tar.gz ..

cd ..
tar xf jampal_$UPSTRMVERSION2.orig.tar.gz
mv -f ${sourcedir}-orig/* ${sourcedir}/
rmdir ${sourcedir}-orig

# rsync -aC $scriptpath/$system $sourcedir/

if [[ "$system" == debian_sf ]] ; then
    rsync -aC $scriptpath/looks $sourcedir/
fi

#if [[ "$system" != debian ]] ; then
#    rsync -aC $scriptpath/debian $sourcedir/
#    cp -rfp $sourcedir/$system/* $sourcedir/debian/
#    rm -rf $sourcedir/$system
#fi

