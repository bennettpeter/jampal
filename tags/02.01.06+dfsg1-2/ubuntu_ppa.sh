#!/bin/bash
echo This is obsolete - use copysource.sh and dpkg-buildpackage instead
exit 2

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
# do not make source - download it
# make source
# mkdir -p ~/proj/jampal/jampal-$VERSION
#cp package/source/jampal-$VERSION.tar.gz \
#  ~/proj/jampal/jampal_$VERSION.orig.tar.gz
# cd ~/proj/jampal/jampal-$VERSION
cd ~/proj/jampal
rm -rf sourcepackage jampal-$VERSION
versionsuffix="-"
if [[ "$list" = "" ]] ; then
#    list="unstable lucid maverick natty oneiric"
    list="maverick natty oneiric"
fi
for distro in $list ; do
    if [[ "$distro" == unstable ]] ; then
        distrosuffix=
        versionsuffix=
        reason=debian
        system=debian
    else
        clogversion=`head -1 $scriptpath/ubuntu/changelog| sed 's/.*(// ; s/).*//'`
        if [[ ${clogversion} != ${VERSION}* ]] ; then 
            echo VERSION file $VERSION does not match changelog $clogversion
            exit 2
        fi
        clogversionsuffix=`echo $clogversion | sed "s/^$VERSION//"`
        versionsuffix=${clogversionsuffix}ppa`printf %02d $ppaversion`
        distrosuffix="~$distro"
        reason=ppa
        system=ubuntu
    fi
    rm -rf jampal-$VERSION
    rm -rf jampal_$VERSION*
    rm -rf tagbkup_$VERSION*
    cp $scriptpath/package/source/jampal-$VERSION.tar.gz \
      jampal_$VERSION.orig.tar.gz
    tar xf jampal_$VERSION.orig.tar.gz
    rsync -aC $scriptpath/$system \
      jampal-$VERSION/
    cd jampal-$VERSION
    if [[ "$system" != debian ]] ; then
      mv $system debian
    fi
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
    debuild -S -sa
    dirname=../sourcepackage/$distro
    mkdir -p $dirname
    mv -f ../jampal_$VERSION* $dirname
    cd ..
done

