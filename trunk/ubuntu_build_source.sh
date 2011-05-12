#!/bin/bash

set -e

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath
echo optional parameter list of distros
list="$1"

export VERSION=`cat VERSION`
mkdir -p package
if [[ ! -f package/PPAVERSION ]] ; then
    echo 1 > package/PPAVERSION
    ppaversion=1
else 
    let ppaversion=`cat package/PPAVERSION`+1
    echo $ppaversion > package/PPAVERSION
fi
echo PPA Version $ppaversion
echo press enter if ok, or enter value
read -e xxxin
if [[ "$xxxin" != "" ]] ; then
    ppaversion=$xxxin
    echo $ppaversion > package/PPAVERSION
fi
make source
mkdir -p ~/proj/local/jampal/jampal-$VERSION
cp package/source/jampal-$VERSION.tar.gz ~/proj/local/jampal/
cd ~/proj/local/jampal/jampal-$VERSION
rm -rf ../sourcepackage
versionsuffix="-"
if [[ "$list" = "" ]] ; then
    list="unstable lucid maverick natty oneiric"
fi
for distro in $list ; do
    if [[ "$distro" == unstable ]] ; then
        distrosuffix=
        versionsuffix=
        reason=debian
    else
        versionsuffix=ubuntu1ppa`printf %02d $ppaversion`
        distrosuffix="~$distro"
        reason=ppa
    fi
    rm -rf *
    rm -rf ../jampal_$VERSION*
    tar xf ../jampal-$VERSION.tar.gz
    cd debian
    echo "jampal (${VERSION}${versionsuffix}$distrosuffix) $distro; urgency=low" > changelogxxx
    echo >> changelogxxx
    echo "  * Build for $reason" >> changelogxxx
    echo >> changelogxxx
    echo " -- Peter Bennett <pgbennett@comcast.net>  `date -R`" >> changelogxxx
    echo >> changelogxxx
    cat changelog >> changelogxxx
    mv -f changelogxxx changelog
    cd ..
    echo build Suffix Version $ppaversion for $distro
    dpkg-buildpackage -S
    dirname=../sourcepackage/$distro
    mkdir -p $dirname
    mv -f ../jampal_$VERSION* $dirname
done

