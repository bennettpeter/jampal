#!/bin/bash
scriptpath=`dirname $0`
cd $scriptpath
base=$PWD
OS=`uname -s`

echo Build Tagbkup

if [[ $OS = CYGWIN* ]]; then
    rm -f tagbkup.exe
fi

mkdir -p ../package

echo Tagbkup Cygwin or Linux
make clean
make
if [[ "$?" != 0 ]]; then echo ERROR; exit 2; fi


if [[ $OS = CYGWIN* ]]; then
    if [[ "$WIN_MINGW" == "" ]]; then
        export WIN_MINGW=/Products/mingw
    fi    
    cp -f tagbkup.exe ../package/tagbkup_cygwin.exe
    echo Build Tagbkup for Windows MINGW
    if [[ ! -f "$WIN_MINGW/bin/gcc" ]] ; then
        echo ERROR - mingw is not found at $WIN_MINGW/bin/gcc
        exit 2
    fi
    PATHSAVE="$PATH"
    PATH="$WIN_MINGW/bin:$PATH"
    export option=-D_MSC_VER
    make clean
    make
    if [[ "$?" != 0 ]]; then echo ERROR; exit 2; fi
    cp -fp $WIN_MINGW/bin/libgcc_s_dw2-1.dll ../package
    if [[ "$?" != 0 ]]; then echo ERROR; exit 2; fi
    cp -f tagbkup.exe ../package/tagbkup.exe
    export option=
    echo Tagbkup Win32
fi
if [[ $OS = Linux ]]; then
    echo Tagbkup Linux
    linuxver=`uname -m`
    cp  tagbkup ../package/tagbkup_linux_$linuxver
    chmod 755 ../package/tagbkup_linux_$linuxver
fi

if [[ "$?" != 0 ]]; then echo ERROR; exit 2; fi
