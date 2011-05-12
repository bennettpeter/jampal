#!/bin/bash
set -e
ln -fs "C:/Documents and Settings" "${DESTDIR}/Documents and Settings"
ln -fs "C:/Users" "${DESTDIR}/Users"
ln -fs "C:/Program Files" "${DESTDIR}/Program Files"
ln -fs "C:/Program Files (x86)" "${DESTDIR}/Program Files (x86)"

# set up for creating shortcut
path1=`cygpath -u "C:/ProgramData/Microsoft/Windows/Start Menu/Programs"`
path2=`cygpath -u "C:/Documents and Settings/All Users/Start Menu/Programs"`

if [[ -d "$path1" ]] ; then
    spath="$path1"
elif [[ -d "$path2" ]] ; then
    spath="$path2"
else
    echo "Cannot locate path for shortcut, using Windows XP setting"
    spath="$path2"
fi

spath="${DESTDIR}$spath/Jampal"
mkdir -p "$spath"
mkdir -p tmp

rm -f tmp/shortcut.vbs
echo 'Set oWS = WScript.CreateObject("WScript.Shell")'$'\r' >> tmp/shortcut.vbs
echo 'sLinkFile = "'`cygpath -w "$spath/Jampal.LNK"`'"'$'\r' >> tmp/shortcut.vbs
echo 'Set oLink = oWS.CreateShortcut(sLinkFile)'$'\r' >> tmp/shortcut.vbs
javaprog=`which "${JAVA_PATH}"javaw`
echo 'oLink.TargetPath = "'`cygpath -w "${javaprog}"`'.exe"'$'\r' >> tmp/shortcut.vbs
echo 'oLink.Arguments = "-Xincgc -Xmx256M -jar jampal.jar"'$'\r' >> tmp/shortcut.vbs
echo 'oLink.Description = "Jampal"'$'\r' >> tmp/shortcut.vbs
echo 'oLink.WorkingDirectory = "'`cygpath -w /usr/share/jampal`'"'$'\r' >> tmp/shortcut.vbs
echo 'oLink.IconLocation = "'`cygpath -w /usr/share/jampal/jampal.ico`', 0"'$'\r' >> tmp/shortcut.vbs
echo 'oLink.Save'$'\r' >> tmp/shortcut.vbs

cscript 'tmp\shortcut.vbs'

rm -rf tmp
