Download files 

tagbkup_02.01.05-1_amd64.deb
jampal_02.01.05-1_all.deb
Install both of these on debian/ubuntu 64 bit systems. 

tagbkup_02.01.05-1_i386.deb
jampal_02.01.05-1_all.deb
Install both of these on debian/ubuntu 32 bit systems. 

jampal-02.01.05.jar
Java executable jar file. 

jampal-02.01.05.tar.gz
Full source code. To install on Unix run make install. 

jampal-build-Cygwin-i686-02.01.05.tar.gz
Install package for Cygwin and Windows (32 and 64 bit). If you want to use the command line features with Windows you need Cygwin installed. Then this package will install the scripts for Cygwin as well as the Windows components. Run make install under Cygwin (on Windows Vista or Windows 7 run as administrator). 

jampal-build-Linux-i686-02.01.05.tar.gz
Install package for generic Linux 32 bit. On Ubuntu or Debian systems it is better to download the Ubuntu package. To install run make install under root. 

jampal-build-Linux-x86-64-02.01.05.tar.gz
Install package for generic Linux 64 bit. On Ubuntu or Debian systems it is better to download the Ubuntu package. To install run make install under root. 

jampal-doc-02.01.05.zip
Documentation (html files). Copy of the web site http://jampal.sf.net. These are also included in all other install downloads. 

jampal-generic-setup-02.01.05.zip
Zip file install containing all components. If possible it is better to use the specific install for your system. Use this for systems not covered by other install packages. 

jampal-windows-setup-02.01.05.exe
Windows setup for all versions of Windows. This does not support the command line utilities, although you can use the tagbkup stand alone command and the tagupdate command via the java command line. To use the full command line commands you need Cygwin or a version of unix. 

This is a list of the new features and changes in Jampal 

jampal (02.01.05)

  * ptts

    The ptts.exe utility has been rewritten. It is now a vbs script, run
    with the cscript command. It now supports windows 32 bit as well
    as 64 bit versions and voices.

  * Announcements

    Announcements using the windows speech engine are now fixed to work with
    32 bit and 64 bit JVM, and support 32 bit and 64 bit voices.

jampal (02.01.04)

  * Memory leak

    Fixed memory leak that occurs when closing a library or making certain
    changes with the edit-customize dialog.

  * Man pages

    Added clarification about requirement for lame and Libre Office.

  * Error Handling

    Improved error handling for when components are not installed, and for
    commands run against an empty library.

jampal (02.01.04)

* Debian support
Moving debian directory out of the source archive to make sure this is not
a native debian package.

Version 02.01.01 

Packaged for Debian / Ubuntu
Debian source and install packages, ready for inclusion in the Debian or Ubuntu repository, or download from sourceforge. 

Documentation
Created man pages. Generate html documentation from man pages. Automatic generation of html pages from fragments. 

Debian support
Consolidated scripts under the jampal command and created man page documentation for them
Enabled compilation without freetts, since freetts is not available in a Debian package.
Changed build process to use make instead of shell scripts.
Player can now support a sound system that does not allow master gain control.
Default libraries support columns needed for command line commands.
Support for help file in a different place in the directory structure, using environment properties file.
Support for relative directories in environment files.
Default library is now in directory specified in configuration file.
Improved naming of some menu items.
Fixed to support the directory structure that is used by mbrola under debian. 

Windows build
Removed unix scripts from Windows install.
Added a cygwin install. This includes scripts.
Included mbrola.exe in windows and cygwin builds, since it is no longer on the mbrola web site. 

General
Removed some junk files from source distribution. 

Version 1.26 

Tagbkup
Fixed problem where -f option only works on a unix-formatted file, even in Windows. 

Version 1.25 

Copy / Paste
Fixed problem where paste menu option was not working. 

Version 1.24 

64 Bit Linux Support
A 64 bit linux version of tagbkup is now included. Both 32 bit and 64 bit versions are in the install package so that it can be installed on 32 bit or 64 bit Linux systems. The same java versions of the jampal program work in 32 bit or 64 bit Linux. 

Version 1.23 

64 Bit Support
The install script is updated so that Jampal will support a 32bit or 64bit version of Java on 64bit Windows systems. If a 64bit Java is used, announcements using the Microsoft speech engine currently will not work. Other speech options will work. 

Icons and files
There is a new icon for Jampal. Also jampal libraries will show the icon in Windows explorer, and you can open the library by double clicking the library icon. 

Release notes from earlier versions are in the changelog and the documentation.