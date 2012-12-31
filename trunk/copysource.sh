#!/bin/bash

set -e

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath

buildtype=$1
ubuntuversion=$2

if [[ "$buildtype" == "" || "$buildtype$ubuntuversion" == P ]] ; then
    echo "Parameters:"
    echo "1 Build type (M=Debian mentors, S=Debian Sourceforge, P=Ubuntu PPA)"
    echo "2 Ubuntu version required if P was selected"
    echo "To prevent downloading the source run make source before this"
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
UPSTRMVERSION1=`dpkg-parsechangelog |egrep '^Version:'|cut -f2 -d' '|cut -f1 -d+`
# UPSTRMVERSION2 does have the dfsg1 in it e.g. jampal_02.01.06+dfsg1
UPSTRMVERSION2=`dpkg-parsechangelog |egrep '^Version:'|cut -f2 -d' '|cut -f1 -d-`
# DEBIANVERSION does have the dfsg1-x in it e.g. jampal_02.01.06+dfsg1-2
DEBIANVERSION=`dpkg-parsechangelog |egrep '^Version:'|cut -f2 -d' '`
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
rm -rf ~/proj/jampal/${sourcedir}-orig

mkdir -p ~/proj/jampal/$sourcedir/debian/
cp -fpr debian/* ~/proj/jampal/$sourcedir/debian/
# cp -fpr $system/* ~/proj/jampal/$sourcedir/debian/
case $buildtype in
S)
    cp -fpr debian_sf/* ~/proj/jampal/$sourcedir/debian/
    ;;
P)
    newfile=~/proj/jampal/$sourcedir/debian/changelog_new
    echo "jampal (${DEBIANVERSION}ubuntu0) $ubuntuversion; urgency=low" > $newfile
    echo "" >> $newfile
    echo "  * Update release for ppa" >> $newfile
    echo "" >> $newfile
    echo " -- Peter Bennett <pgbennett@comcast.net>  `date -R`" >> $newfile
    echo "" >> $newfile
    cat ~/proj/jampal/$sourcedir/debian/changelog >> $newfile
    mv -f $newfile ~/proj/jampal/$sourcedir/debian/changelog
    ;;
esac
if [[ -f package/source/jampal-source-${VERSION}.tar.gz ]] ; then
    cp package/source/jampal-source-${VERSION}.tar.gz ~/proj/jampal/$sourcedir
fi

cd ~/proj/jampal/$sourcedir
./debian/rules get-orig-source

mv jampal_$UPSTRMVERSION2.orig.tar.gz ..

cd ..
tar xf jampal_$UPSTRMVERSION2.orig.tar.gz

mv -f ${sourcedir}-orig/* ${sourcedir}/
rmdir ${sourcedir}-orig

if [[ "$buildtype" == S ]] ; then
    rsync -aC $scriptpath/looks $sourcedir/
fi


