#   Makefile for tagbkup
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

# bindir = where binaries get installed (default = /usr/local/bin)
# mandir = where the manual page gets installed (default = /usr/local/man/man1)


prefix=${DESTDIR}/usr
bindir=${prefix}/bin
mandir = ${DESTDIR}/usr/man/man1

# No changes necessary below this line

PROG =	tagbkup
SRCS =	tagbkup.cpp nullmp3.cpp
OBJS =  tagbkup.o nullmp3.o
RM = /bin/rm
INSTALL = /usr/bin/install
STRIP = strip

#LIBS = -lmsvcrt40
CC = g++
#CFLAGS =  -O2 -Wall -I /usr/include/mingw32
CFLAGS =  -O2 -Wall ${option}

all: tagbkup

unix: all
	mkdir -p ../unix_build/tagbkup
	cp -p Makefile tagbkup \
      ../unix_build/tagbkup/
	cd ../unix_build/tagbkup && touch --date=19990101 dummy.cpp dummy.c


tagbkup: *.cpp *.c
	basename `uname -o` > OS
	sed -e "s/\*\*UNSTABLE\*\*/`cat ../VERSION`/" tagbkup.cpp > tagbkupxx.cpp
	$(CC) $(CFLAGS) -D OS_`cat OS` -D CPU_`uname -m` -o tagbkup tagbkupxx.cpp nullmp3.cpp mp3tech.c $(LIBS)


#tagbkup: $(OBJS)
#	$(CC) $(CFLAGS) -o $@ $(OBJS) $(LIBS)

clean: 
	$(RM) -f $(OBJS) tagbkup tagbkupxx.cpp tagbkup.exe core OS libgcc_s_dw2-1.dll
	rm -rf ../unix_build/tagbkup

distclean: clean

install: tagbkup
# do not strip per GNU standard
#	$(STRIP) tagbkup
	$(INSTALL) -d $(bindir)
	$(INSTALL) tagbkup $(bindir)/tagbkup

uninstall:
	rm -f $(bindir)/tagbkup

