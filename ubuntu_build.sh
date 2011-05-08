#!/bin/bash

set -e

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`

cd $scriptpath


export VERSION=`cat VERSION`
echo Copy new version of jampal $VERSION from host to local

make source
mkdir -p ~/proj/local/jampal/jampal-$VERSION
cp package/source/jampal-$VERSION.tar.gz ~/proj/local/jampal/
rm -rf ~/proj/local/jampal/jampal-$VERSION/*
cd ~/proj/local/jampal/jampal-$VERSION
tar xvf ../jampal-$VERSION.tar.gz

dpkg-buildpackage

dirname=$scriptpath/package/ubuntu-`arch`
mkdir -p $dirname
cp ~/proj/local/jampal/jampal_$VERSION* $dirname


