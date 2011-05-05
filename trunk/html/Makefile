#   Documentation Makefile for jampal
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


all: make_user_doc

web_doc: make_web_doc

make_user_doc: ../man/* *.html cssdef.css favicon.ico images/*
	# MAN PAGES IN HTML
	rman -f html -r 'man_%s.html' -l '%s' ../man/jampal.1 > man_jampal.html
	rman -f html -r 'man_%s.html' -l '%s' ../man/tagbkup.1 > man_tagbkup.html
	# HTML DOCUMENTATION
	mkdir -p user_doc
	( set -e ; while [ true ] ; do \
      read filename description ; \
      if [ "$$filename" = "XXX" ] ; then exit 0; fi ; \
      echo processing $$filename ... ; \
      sed "s/XXXHEADINGXXX/$$description/g" \
          framework_start.html > user_doc/$$filename ; \
      sed 's@<!DOCTYPE[^>]*>@<!--DOCTYPE-->@;'\
's@<html>@<!--html@;s@<body[^>]*>@body-->@;'\
's@</body>@<!--body@;s@</html>@/html-->@' \
          $$filename >> user_doc/$$filename ; \
      cat framework_end.html >> user_doc/$$filename ; \
    done ) < buildlist.txt
	cp -r cssdef.css index.html favicon.ico user_doc/
	mkdir -p user_doc/images/
	cp images/* user_doc/images/
	echo done > make_user_doc

make_web_doc: make_user_doc
	mkdir -p web_doc
	cd user_doc && for file in *.html; do \
      sed 's!images/sourceforge.gif!http://sflogo.sourceforge.net/sflogo.php?group_id=107584\&amp;type=15!g' \
      $$file > ../web_doc/$$file ; done
	cp -r cssdef.css index.html favicon.ico web_doc/
	mkdir -p web_doc/images
	cp  images/* web_doc/images/
	echo done > make_web_doc

clean: 
	rm -rf make_user_doc user_doc \
      man_jampal.html man_tagbkup.html make_web_doc web_doc

install: make_user_doc
#	HTML DOCUMENTATION
	install -d ${DESTDIR}/usr/share/doc/jampal/html/
	cp -pr user_doc/* ${DESTDIR}/usr/share/doc/jampal/html/

uninstall:
	rm -rf ${DESTDIR}/usr/share/doc/jampal/html

distclean: clean


