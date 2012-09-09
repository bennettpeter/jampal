#!/bin/bash

# export VCHOME="C:\Program Files\Microsoft Visual Studio\VC98"
export VSHOME="C:\Program Files\Microsoft Visual Studio 8"
export VCHOME="C:\Program Files\Microsoft Visual Studio 8\VC"

scriptpath=`dirname $0`
cd $scriptpath
base=$PWD

rm -f Release/ptts.exe
rm -f pttsjni___Win32_Release/pttsjni.dll

mkdir -p ~/temp

cat <<EOJ > ~/temp/make.bat
@set OS=Windows_NT
@call "$VCHOME\\bin\\VCVARS32"
@echo on
VCBUILD /rebuild ptts.sln "Release|Win32"
@IF ERRORLEVEL 1 EXIT 2
@rem NMAKE CLEAN /f "ptts.mak" CFG="ptts - Win32 Release"
@rem NMAKE /f "ptts.mak" CFG="ptts - Win32 Release"
@rem @IF ERRORLEVEL 1 EXIT 2
@rem NMAKE CLEAN /f "pttsjni.mak" CFG="pttsjni - Win32 Release"
@rem NMAKE /f "pttsjni.mak" CFG="pttsjni - Win32 Release"
@rem @IF ERRORLEVEL 1 EXIT 2
EOJ

cmd /c `cygpath -w ~/temp/make.bat`
if [[ "$?" != 0 ]]; then echo ERROR; exit 2; fi

echo

