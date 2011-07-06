#   Project Makefile for jampal
#
#   Copyright 2006-2011 Peter G Bennett
#
#   This file is part of Jampal.
#
#   Jampal is free software: you can redistribute it and/or modify
#   it under the terms of the GNU General Public License as published by
#   the Free Software Foundation, either version 3 of the License, or
#   (at your option) any later version.
#
#   Jampal is distributed in the hope that it will be useful,
#   but WITHOUT ANY WARRANTY; without even the implied warranty of
#   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#   GNU General Public License for more details.
#
#   You should have received a copy of the GNU General Public License
#   along with Jampal.  If not, see <http://www.gnu.org/licenses/>.
#
# ***************************************************************************
#
#

# VERSION ?= $(shell read VERSION && echo $$VERSION)
VERSION := $(shell cat VERSION)

all: 
	cd jampal && make all
	cd tagbkup && make all
	cd html && make all

clean: 
	cd jampal && make clean
	cd tagbkup && make clean
	cd html && make clean
	rm -rf tmp OS
	rm -rf build_doc
	rm -f utility/*.jmp
	rm -rf unix_build
	rm -rf package/source
	# files from testing target
	rm -f jampal.jar jampal_environment.properties jampal_initial.properties
	# Files created by Netbeans
	rm -rf build dist


install: 
	cd jampal && make install
	cd tagbkup && make install
	cd html && make install
#	MAN PAGES
	install -d ${DESTDIR}/usr/share/man/man1/
	install -m644 man/*.1 ${DESTDIR}/usr/share/man/man1/
	gzip -f9 ${DESTDIR}/usr/share/man/man1/jampal.1 \
      ${DESTDIR}/usr/share/man/man1/tagbkup.1
#	SCRIPTS
	mkdir -p ${DESTDIR}/usr/share/jampal/scripts/examples
	install -m644 -p scripts/*.* ${DESTDIR}/usr/share/jampal/scripts/
	install -m644 -p scripts/examples/*.* ${DESTDIR}/usr/share/jampal/scripts/examples/
	chmod 755 ${DESTDIR}/usr/share/jampal/scripts/*.sh \
      ${DESTDIR}/usr/share/jampal/scripts/examples/*.sh
#	LINK for executable
	ln -fs ../share/jampal/scripts/jampal.sh ${DESTDIR}/usr/bin/jampal
#	UTILITY
	install -d ${DESTDIR}/usr/share/jampal/utility/
	rm -f utility/*.jmp
	install -m644 -p utility/* ${DESTDIR}/usr/share/jampal/utility/
#	MISC
#	If not building a debian package add the looks files and COPYING
	if [ "${DEBIAN_BUILD}" != Y ] ; then \
		install -m644 looks/*.jar ${DESTDIR}/usr/share/jampal/ ; \
		install -m644 misc/COPYING ${DESTDIR}/usr/share/jampal/ ; fi
#	CYGWIN
	basename `uname -o` > OS
	if [ `cat OS` = Cygwin ] ; then \
        install -m644 jampal/jampal.ico ${DESTDIR}/usr/share/jampal/ ; \
        install -m755 misc/windows-32/mbrola.exe ${DESTDIR}/usr/share/jampal/ ; \
        install -m644 misc/mbrola*.txt ${DESTDIR}/usr/share/jampal/ ; \
        install -m644 -p jampal/src/pgbennett/speech/ptts.vbs ${DESTDIR}/usr/share/jampal/ ; \
        scripts/setup_cygwin.sh ; fi

uninstall:
	cd jampal && make uninstall
	cd tagbkup && make uninstall
	cd html && make uninstall
	rm -rf ${DESTDIR}/usr/share/doc/jampal
	rm -rf ${DESTDIR}/usr/share/jampal
	rm -f ${DESTDIR}/usr/share/man/man1/jampal.1* ${DESTDIR}/usr/share/man/man1/tagbkup.1*
	basename `uname -o` > OS
	if [ `cat OS` = Cygwin ] ; then rm -fr \
       "${DESTDIR}`cygpath -u \"C:/ProgramData/Microsoft/Windows/Start Menu/Programs/Jampal\"`" \
       "${DESTDIR}`cygpath -u \"C:/Documents and Settings/All Users/Start Menu/Programs/Jampal\"`" ; fi

distclean: clean
	cd jampal && make distclean
	cd tagbkup && make distclean
	cd html && make distclean

source: clean
	# Make source appear under a jampal-version directory
	# Exclude debian ubuntu and package directories
	mkdir -p package/source
	ln -fs ../.. package/source/jampal-source-$(VERSION)
	cd package/source && echo jampal-source-${VERSION}/* | \
           sed "s@ jampal-source-${VERSION}/package @ @;\
           s@ jampal-source-${VERSION}/debian @ @;\
           s@ jampal-source-${VERSION}/ubuntu @ @;\
           s@ jampal-source-${VERSION}/notes.txt @ @" \
          > source_filelist.txt
	cd package/source && \
		tar -c -z --exclude-vcs --exclude=**/notes.txt --exclude=misc/windows-32 --exclude=misc/windows-32 -f jampal-source-$(VERSION).tar.gz \
		`cat source_filelist.txt`
	tar -c -z --exclude-vcs -f package/source/jampal-source-$(VERSION)_debian.tar.gz \
		debian ubuntu
	tar -c -z --exclude-vcs -f package/source/jampal-source-$(VERSION)_windows32.tar.gz \
		misc/windows-32


unix:
#	Create cygwin or generic unix installer.
	mkdir -p unix_build
	cd jampal && make unix
	cd tagbkup && make clean && make unix
	cd html && make unix
	rsync -aC man scripts utility looks \
		Makefile misc VERSION unix_build/
	mkdir -p unix_build/jampal/src/pgbennett/speech/
	cp jampal/src/pgbennett/speech/ptts.vbs unix_build/jampal/src/pgbennett/speech/
	basename `uname -o` > OS
	mkdir -p package/generic
	cd unix_build && tar -c -z --exclude-vcs \
        -f  ../package/generic/jampal-build-`cat ../OS`-`arch`-$(VERSION).tar.gz *

testing: all
	# Installs jar and properties into source directory for debug and test
	# needs root
	cp jampal/jampal.jar .
	install -m644 -T jampal/jampal_environment.properties_testing \
        ./jampal_environment.properties
	basename `uname -o` > OS
	if [ `cat OS` = Cygwin ] ; then \
       install -m644 -T jampal/jampal_initial.properties_windows \
       ./jampal_initial.properties ; \
    else \
       install -m644 -T jampal/jampal_initial.properties_unix \
       ./jampal_initial.properties ; \
    fi
	if [ `readlink -e /usr/bin/jampal` != $$PWD/scripts/jampal.sh ]; then \
		ln -fs $$PWD/scripts/jampal.sh /usr/bin/jampal; \
	fi

# check if we are logged in as root
checkroot:
	test $$(id -u) = 0

