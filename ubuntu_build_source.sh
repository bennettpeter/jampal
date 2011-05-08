#!/bin/bash

set -e

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath


export VERSION=`cat VERSION`
if [[ ! -f package/PPAVERSION ]] ; then
    echo 1 > package/PPAVERSION
    ppaversion=1
else 
    let ppaversion=`cat package/PPAVERSION`+1
    echo $ppaversion > package/PPAVERSION
fi
echo PPA Version $ppaversion
echo press enter if ok, ctrl-c if not
read xxx
make source
mkdir -p ~/proj/local/jampal/jampal-$VERSION
cp package/source/jampal-$VERSION.tar.gz ~/proj/local/jampal/
cd ~/proj/local/jampal/jampal-$VERSION
rm -rf ../sourcepackage
for distro in lucid maverick natty oneiric ; do
    rm -rf *
    rm -rf ../jampal_$VERSION*
    tar xf ../jampal-$VERSION.tar.gz
    cd debian
    echo "jampal (${VERSION}ubuntu1ppa`printf %02d $ppaversion`~$distro) $distro; urgency=low" > changelogxxx
    echo >> changelogxxx
    echo "  * Build for ppa" >> changelogxxx
    echo " -- Peter Bennett <pgbennett@users.sourceforge.net>  `date -R`" >> changelogxxx
    cat changelog >> changelogxxx
    mv -f changelogxxx changelog
    cd ..
    echo build PPA Version $ppaversion for $distro
    dpkg-buildpackage -S
    dirname=../sourcepackage/$distro
    mkdir -p $dirname
    mv -f ../jampal_$VERSION* $dirname
done

