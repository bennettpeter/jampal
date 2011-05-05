
# FREETTS is only used in Cygwin builds or non-debian unix
FREETTS_HOME=/opt/freetts

all: jampal.jar

clean:
	rm -rf build
	rm -f jampal.jar make_basic OS
	rm -f src/pgbennett/jampal/*.jmp

distclean: clean

install: all
	basename `uname -o` > OS
	install -d ${DESTDIR}/usr/share/jampal ${DESTDIR}/usr/bin
#	LINK for executable
	ln -fs ../share/jampal/scripts/jampal.sh ${DESTDIR}/usr/bin/jampal
	install -m644 jampal.jar ${DESTDIR}/usr/share/jampal/
	install -m644 -T jampal_environment.properties_unix \
	    ${DESTDIR}/usr/share/jampal/jampal_environment.properties
	if [ `cat OS` = Cygwin ] ; then \
       install -m644 -T jampal_initial.properties_windows \
       ${DESTDIR}/usr/share/jampal/jampal_initial.properties ; \
    else \
       install -m644 -T jampal_initial.properties_unix \
       ${DESTDIR}/usr/share/jampal/jampal_initial.properties ; \
    fi
	install -d ${DESTDIR}/usr/share/pixmaps/
	install -m644 desktop/jampal.xpm ${DESTDIR}/usr/share/pixmaps/
	install -m644 desktop/jampal.png ${DESTDIR}/usr/share/pixmaps/
	install -d ${DESTDIR}/usr/share/applications/
	install -m644 desktop/jampal.desktop ${DESTDIR}/usr/share/applications/
	install -d ${DESTDIR}/usr/share/menu/
	install -m644 desktop/menu/jampal ${DESTDIR}/usr/share/menu/
	install -d ${DESTDIR}/usr/share/mime/packages/
	install -m644 desktop/mime/jampal.xml ${DESTDIR}/usr/share/mime/packages/
	cp -r  desktop/icons ${DESTDIR}/usr/share/icons
	chmod 644 `find ${DESTDIR}/usr/share/icons -name 'application-jampal.png'`

uninstall:
	rm -f ${DESTDIR}/usr/share/jampal/jampal.jar
	rm -f ${DESTDIR}/usr/bin/jampal
	rm -f ${DESTDIR}/usr/share/pixmaps/jampal.xpm
	rm -f ${DESTDIR}/usr/share/pixmaps/jampal.png
	rm -f ${DESTDIR}/usr/share/applications/jampal.desktop
	rm -f ${DESTDIR}/usr/share/menu/jampal
	rm -f ${DESTDIR}/usr/share/mime/packages/jampal.xml
	rm -f `find ${DESTDIR}/usr/share/icons -name 'application-jampal.png'`

jampal.jar: make_basic
	# Remove java files
	rm -f `find build -name '*.java'`
	# Create jampal.jar
	rm -f jampal.jar
	cd build/classes && "${JAVA_PATH}"jar cmf META-INF/MANIFEST.MF "../../jampal.jar" *

make_basic: src/*/*/*.java src/*/*/*/*.java src/*/*/*/*/*.java
	mkdir -p build/classes
	rm -f src/pgbennett/jampal/*.jmp
	# Copy Source
	cp -r src/* build/classes
	# remove CVS directories
	rm -rf `find build -name 'CVS'`
	rm -rf `find build -name 'cvs'`
	# java compile
	# Note it may seem we are just building 5 classes but in fact the java 
	# compiler will pull in needed other modules from the source tree and compile
	# them as well
	"${JAVA_PATH}"java -version
	"${JAVA_PATH}"javac -g -d build/classes -sourcepath build/classes build/classes/pgbennett/jampal/Jampal.java
	"${JAVA_PATH}"javac -g -d build/classes -sourcepath build/classes build/classes/pgbennett/id3/TagUpdate.java
	"${JAVA_PATH}"javac -g -d build/classes -sourcepath build/classes \
       build/classes/org/tritonus/sampled/convert/javalayer/MpegFormatConversionProvider.java
	"${JAVA_PATH}"javac -g -d build/classes -sourcepath build/classes \
       build/classes/org/tritonus/sampled/file/mpeg/MpegAudioFileReader.java
	if [ "${DEBIAN_BUILD}" != Y -a -f "${FREETTS_HOME}/lib/freetts.jar" ] ; then \
       "${JAVA_PATH}"javac -g -d build/classes \
       -sourcepath build/classes -classpath "${FREETTS_HOME}/lib/freetts.jar" \
       build/classes/pgbennett/speech/BatchSpeaker.java ; fi
	if [ "${DEBIAN_BUILD}" != Y -a -f "${FREETTS_HOME}/lib/freetts.jar" ] ; then \
       "${JAVA_PATH}"javac -g -d build/classes \
       -sourcepath build/classes -classpath ${FREETTS_HOME}/lib/freetts.jar \
       build/classes/pgbennett/speech/FreeTTSSpeaker.java ; fi
	echo yes > make_basic
