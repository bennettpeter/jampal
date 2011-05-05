#!/bin/bash
set -e
export OS=`uname -s`

package=$HOME/package

if [[ $OS = CYGWIN* ]]; then
    INNO_SETUP_PATH='C:/Program Files (x86)/Inno Setup 5'
    setup_packages='C:/Products/setup_packages'
    export JAVA_PATH='C:/Program Files/Java/jdk1.6.0_18/bin/'
#    export JAVA_HOME='C:/Progra~1/Java/jdk1.5.0_14'
    export FREETTS_HOME=/opt/freetts
    export WIN_MINGW=/Products/mingw
else
    export JAVA_PATH=
    export FREETTS_HOME=/opt/freetts

fi

scriptname=`readlink -e "$0"`
scriptpath=`dirname "$scriptname"`
cd $scriptpath
base=$PWD

grep "Jampal Version" jampal/src/pgbennett/jampal/MainFrame.java
grep "ptts Version" ptts/ptts.cpp
grep "tagbkup Version" tagbkup/tagbkup.cpp
grep "TagUpdate Version" jampal/src/pgbennett/id3/TagUpdate.java

echo Enter Version
#read -e VERSION
VERSION=`cat VERSION`
export VERSION

rm -f source.zip

rm -rf $package

make clean

#tar -c -v -z --exclude-vcs -f  jampal-$VERSION.tar.gz html jampal nbproject ptts scripts tagbkup Makefile jampal_package.sh nbbuild.xml setup.sh
make source

# zip -D  jampal_$VERSION.source.zip html jampal nbproject ptts scripts tagbkup Makefile jampal_package.sh nbbuild.xml setup.sh -x CVS

mkdir -p $package/doc/images
mkdir -p package
# jampal/build.sh
make


# if [[ $OS = CYGWIN* ]]; then
#     ptts/build.sh
# fi
cp -p misc/windows-32/pttsjni.dll \
   misc/windows-32/ptts.exe \
   $package


cp -p jampal/jampal.jar \
   jampal/jampal.ico \
   jampal/jampal_environment.properties_* \
   jampal/jampal_initial.properties_* \
   $package


tagbkup/build.sh

cp -p package/tagbkup.exe $package/tagbkup.exe
cp -p package/libgcc_s_dw2-1.dll $package/libgcc_s_dw2-1.dll
cp  -p package/tagbkup_cygwin.exe $package/tagbkup_cygwin.exe
if [[ ! -f package/tagbkup_linux_i686 || ! -f package/tagbkup_linux_x86_64 ]] ; then
    echo "Please use Linux to run tagbkup/build.sh on 32 & 64 to create tagbkup_linux_i686 and tagbkup_linux_x86_64"
    exit 2
fi
cp  -p package/tagbkup_linux* $package/

ls -l $package/tagbkup_cygwin.exe $package/tagbkup.exe $package/tagbkup_linux*
echo 'are tagbkups up to date ?'
echo 'Continue (Y/N) ?'
read -e ans
if [[ "$ans" != Y ]]; then exit 2; fi

cp -p jampal/jampal.jar jampal-$VERSION.jar

# zip -D source.zip  *.sh *.txt tagbkup/*.h tagbkup/*.cpp tagbkup/*.c tagbkup/*.dep tagbkup/Makefile tagbkup/*.mak tagbkup/*.dsp tagbkup/*.sh \
#    ptts/*.h ptts/*.cpp ptts/*.c ptts/*.dep ptts/*.mak ptts/*.dsp ptts/*.sh \
#    html/* html/images/* scripts/* scripts/examples/* -x scripts/mp3.profile


# This to prevent error moving the zip file under Windows
# sleep 1
# mv -f source.zip jampal-source-$VERSION.zip



cp -p misc/windows-32/mbrola.exe $package/
cp -p misc/mbrola*.txt $package/
cp -p misc/COPYING $package/

cp -upr html/user_doc/* $package/doc/
rm -rf `find $package/doc -name 'CVS'`

mkdir -p $package/scripts/examples
# cp -up scripts/mp3.profile scripts/mp3_base.profile
#cp -up scripts/* $package/scripts
cp -upr scripts/* $package/scripts
#rm -f $package/scripts/*.frag
rm -rf `find $package/scripts -name 'CVS'`

#cp -up scripts/examples/* $package/scripts/examples
#rm -f $package/scripts/mp3.profile

mkdir -p $package/utility
cp -up utility/* $package/utility

cp -up looks/*.jar $package

rm -f jampal-doc-$VERSION.zip
cd $package
zip -r  $base/jampal-doc-$VERSION.zip doc
cd $base

if [[ $OS = CYGWIN* ]]; then

#    cp -up $setup_packages/Microsoft-English-TTS-51.msi $package
#    cp -up $setup_packages/MarMike5.msi $package

    cp -up jampal/jampal.iss $package/jampal.iss
    unix2dos $package/jampal.iss
    "$INNO_SETUP_PATH"/iscc `cygpath -w $package/jampal.iss`
    mv -f $package/Output/setup.exe jampal-windows-setup-$VERSION.exe

#    sed "s!^;XP !!g" jampal/jampal.iss > $package/jampal.iss
#    unix2dos $package/jampal.iss
#    "$INNO_SETUP_PATH"/iscc `cygpath -w $package/jampal.iss`
#    mv -f $package/Output/setup.exe jampal-xp-setup-$VERSION.exe

#    sed "s!^;2000 !!g" jampal/jampal.iss > $package/jampal.iss
#    unix2dos $package/jampal.iss
#    "$INNO_SETUP_PATH"/iscc `cygpath -w $package/jampal.iss`
#    mv -f $package/Output/setup.exe jampal-2000-setup-$VERSION.exe

fi


# generic
chmod -R g+w,o+r $package/*
chmod 755 $package/doc $package/doc/images $package/scripts \
  $package/scripts/examples $package/utility
chmod 755 $package/scripts/*.sh

# Setup generic properties
cp $package/jampal_environment.properties_generic \
    $package/jampal_environment.properties
cp $package/jampal_initial.properties_unix \
    $package/jampal_initial.properties

mkdir -p $HOME/temp
cd $HOME/temp
rm -rf jampal
ln -s $package jampal
rm -f $base/jampal-generic-setup-$VERSION.zip
zip -Dr $base/jampal-generic-setup-$VERSION.zip jampal \
    -x jampal\*.msi

cd $base

make unix

ls -l jampal-windows-setup-$VERSION.exe \
      jampal-$VERSION.tar.gz \
      jampal-doc-$VERSION.zip \
      jampal-$VERSION.jar \
      jampal-generic-setup-$VERSION.zip \
      jampal-cygwin-$VERSION.tar.gz
