#!/bin/bash

set -e
echo use option -S to build source only

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath


export VERSION=`cat VERSION`
echo Copy new version of jampal $VERSION from host to local

make source
mkdir -p ~/proj/local/jampal/jampal-$VERSION
cp package/source/jampal-$VERSION.tar.gz ~/proj/local/jampal/
rm -rf ~/proj/local/jampal/jampal-$VERSION/*
rm -rf  ~/proj/local/jampal/jampal_$VERSION*
cd ~/proj/local/jampal/jampal-$VERSION
tar xvf ../jampal-$VERSION.tar.gz

dpkg-buildpackage $1

if [[ "$1" == "-S" ]] ; then
    arch=source
else
    arch=`arch`
fi

dirname=$scriptpath/package/ubuntu-$arch
mkdir -p $dirname
cp ~/proj/local/jampal/jampal_$VERSION* $dirname


