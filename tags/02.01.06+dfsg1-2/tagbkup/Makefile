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
GCC_CMD ?= g++
CC = $(GCC_CMD)
#CFLAGS =  -O2 -Wall -I /usr/include/mingw32
CFLAGS =  -O2 -Wall ${option}

all: tagbkup

unix: all
	mkdir -p ../unix_build/tagbkup
	install -p -m 644 Makefile \
      ../unix_build/tagbkup/
	install -p -m 755 tagbkup \
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
	rm  -f libgcc_s_dw2-1.dll libstdc++-6.dll

distclean: clean

install: tagbkup
# do not strip per GNU standard
# Do strip as required by debian
	$(STRIP) --remove-section=.comment --remove-section=.note tagbkup
	$(INSTALL) -d $(bindir)
	$(INSTALL) tagbkup $(bindir)/tagbkup

uninstall:
	rm -f $(bindir)/tagbkup

mingw_cygwin_cmd := /Products/mingw/bin/g++
mingw_cygwin_path := /Products/mingw/bin
mingw_linux_cmd := "wine ~/.wine/drive_c/MinGW/bin/g++"
mingw_linux_path := ~/.wine/drive_c/MinGW/bin
windows:
	basename `uname -o` > OS
	if [ `cat OS` = Cygwin ] ; then \
        GCC_CMD=$(mingw_cygwin_cmd) ; \
        GCC_PATH=$(mingw_cygwin_path) ; \
    else \
        GCC_CMD=$(mingw_linux_cmd) ; \
        GCC_PATH=$(mingw_linux_path) ; \
    fi ; \
    export GCC_CMD ; \
    make clean && make all; \
    cp "$$GCC_PATH/libgcc_s_dw2-1.dll" "$$GCC_PATH/libstdc++-6.dll" .

