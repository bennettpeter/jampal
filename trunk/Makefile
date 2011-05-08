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
VERSION=`cat VERSION`

all: 
	cd jampal && make all
	cd tagbkup && make all
	cd html && make all

clean: 
	cd jampal && make clean
	cd tagbkup && make clean
	cd html && make clean
	rm -rf debian/tmp
	rm -rf tmp OS
	rm -rf build_doc
	rm -f utility/*.jmp
	rm -rf unix_build
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
	install -m644 misc/COPYING ${DESTDIR}/usr/share/jampal/
#	If not building a debian package add the looks files
	if [ "${DEBIAN_BUILD}" != Y ] ; then \
        install -m644 looks/*.jar ${DESTDIR}/usr/share/jampal/ ; fi
#	CYGWIN
	basename `uname -o` > OS
	if [ `cat OS` = Cygwin ] ; then \
        install -m644 jampal/jampal.ico ${DESTDIR}/usr/share/jampal/ ; \
        install -m755 misc/windows-32/mbrola.exe ${DESTDIR}/usr/share/jampal/ ; \
        install -m644 misc/mbrola*.txt ${DESTDIR}/usr/share/jampal/ ; \
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

source: clean get_version
	tar -c -z --exclude-vcs -f  jampal-$(VERSION).tar.gz html jampal man \
      nbproject ptts scripts tagbkup debian utility looks \
      Makefile jampal_package.sh nbbuild.xml misc VERSION

unix:
#	Create cygwin or generic unix installer.
	mkdir -p unix_build
	cd jampal && make unix
	cd tagbkup && make unix
	cd html && make unix
	cp -rp man scripts utility looks \
        Makefile misc unix_build
	basename `uname -o` > OS
	cd unix_build && tar -c -z --exclude-vcs \
        -f  ../jampal-build-`cat ../OS`-`arch`-`cat ../VERSION`.tar.gz *

get_version:
	grep "Jampal Version" jampal/src/pgbennett/jampal/MainFrame.java
	grep "ptts Version" ptts/ptts.cpp
	grep "tagbkup Version" tagbkup/tagbkup.cpp
	grep "TagUpdate Version" jampal/src/pgbennett/id3/TagUpdate.java
	echo Enter Version

