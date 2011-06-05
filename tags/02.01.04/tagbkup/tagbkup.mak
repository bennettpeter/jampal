# Microsoft Developer Studio Generated NMAKE File, Based on tagbkup.dsp
!IF "$(CFG)" == ""
CFG=tagbkup - Win32 Debug
!MESSAGE No configuration specified. Defaulting to tagbkup - Win32 Debug.
!ENDIF 

!IF "$(CFG)" != "tagbkup - Win32 Release" && "$(CFG)" != "tagbkup - Win32 Debug"
!MESSAGE Invalid configuration "$(CFG)" specified.
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "tagbkup.mak" CFG="tagbkup - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "tagbkup - Win32 Release" (based on "Win32 (x86) Console Application")
!MESSAGE "tagbkup - Win32 Debug" (based on "Win32 (x86) Console Application")
!MESSAGE 
!ERROR An invalid configuration is specified.
!ENDIF 

!IF "$(OS)" == "Windows_NT"
NULL=
!ELSE 
NULL=nul
!ENDIF 

!IF  "$(CFG)" == "tagbkup - Win32 Release"

OUTDIR=.\Release
INTDIR=.\Release
# Begin Custom Macros
OutDir=.\Release
# End Custom Macros

ALL : "$(OUTDIR)\tagbkup.exe"


CLEAN :
	-@erase "$(INTDIR)\mp3tech.obj"
	-@erase "$(INTDIR)\nullmp3.obj"
	-@erase "$(INTDIR)\tagbkup.obj"
	-@erase "$(INTDIR)\vc60.idb"
	-@erase "$(OUTDIR)\tagbkup.exe"

"$(OUTDIR)" :
    if not exist "$(OUTDIR)/$(NULL)" mkdir "$(OUTDIR)"

CPP=cl.exe
CPP_PROJ=/nologo /MD /W3 /GX /O2 /D "WIN32" /D "NDEBUG" /D "_CONSOLE" /D "_MBCS" /Fp"$(INTDIR)\tagbkup.pch" /YX /Fo"$(INTDIR)\\" /Fd"$(INTDIR)\\" /FD /c 

.c{$(INTDIR)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cpp{$(INTDIR)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cxx{$(INTDIR)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.c{$(INTDIR)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cpp{$(INTDIR)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cxx{$(INTDIR)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

RSC=rc.exe
BSC32=bscmake.exe
BSC32_FLAGS=/nologo /o"$(OUTDIR)\tagbkup.bsc" 
BSC32_SBRS= \
	
LINK32=link.exe
LINK32_FLAGS=kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /incremental:no /pdb:"$(OUTDIR)\tagbkup.pdb" /machine:I386 /out:"$(OUTDIR)\tagbkup.exe" 
LINK32_OBJS= \
	"$(INTDIR)\mp3tech.obj" \
	"$(INTDIR)\nullmp3.obj" \
	"$(INTDIR)\tagbkup.obj" \
	"..\..\..\..\Program Files\Microsoft Visual Studio\VC98\Lib\SETARGV.OBJ"

"$(OUTDIR)\tagbkup.exe" : "$(OUTDIR)" $(DEF_FILE) $(LINK32_OBJS)
    $(LINK32) @<<
  $(LINK32_FLAGS) $(LINK32_OBJS)
<<

!ELSEIF  "$(CFG)" == "tagbkup - Win32 Debug"

OUTDIR=.\Debug
INTDIR=.\Debug
# Begin Custom Macros
OutDir=.\Debug
# End Custom Macros

ALL : "$(OUTDIR)\tagbkup.exe"


CLEAN :
	-@erase "$(INTDIR)\mp3tech.obj"
	-@erase "$(INTDIR)\nullmp3.obj"
	-@erase "$(INTDIR)\tagbkup.obj"
	-@erase "$(INTDIR)\vc60.idb"
	-@erase "$(INTDIR)\vc60.pdb"
	-@erase "$(OUTDIR)\tagbkup.exe"
	-@erase "$(OUTDIR)\tagbkup.ilk"
	-@erase "$(OUTDIR)\tagbkup.pdb"

"$(OUTDIR)" :
    if not exist "$(OUTDIR)/$(NULL)" mkdir "$(OUTDIR)"

CPP=cl.exe
CPP_PROJ=/nologo /MDd /W3 /Gm /GX /ZI /Od /D "WIN32" /D "_DEBUG" /D "_CONSOLE" /D "_MBCS" /Fp"$(INTDIR)\tagbkup.pch" /YX /Fo"$(INTDIR)\\" /Fd"$(INTDIR)\\" /FD /GZ /c 

.c{$(INTDIR)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cpp{$(INTDIR)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cxx{$(INTDIR)}.obj::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.c{$(INTDIR)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cpp{$(INTDIR)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

.cxx{$(INTDIR)}.sbr::
   $(CPP) @<<
   $(CPP_PROJ) $< 
<<

RSC=rc.exe
BSC32=bscmake.exe
BSC32_FLAGS=/nologo /o"$(OUTDIR)\tagbkup.bsc" 
BSC32_SBRS= \
	
LINK32=link.exe
LINK32_FLAGS=kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /subsystem:console /incremental:yes /pdb:"$(OUTDIR)\tagbkup.pdb" /debug /machine:I386 /out:"$(OUTDIR)\tagbkup.exe" /pdbtype:sept 
LINK32_OBJS= \
	"$(INTDIR)\mp3tech.obj" \
	"$(INTDIR)\nullmp3.obj" \
	"$(INTDIR)\tagbkup.obj" \
	"..\..\..\..\Program Files\Microsoft Visual Studio\VC98\Lib\SETARGV.OBJ"

"$(OUTDIR)\tagbkup.exe" : "$(OUTDIR)" $(DEF_FILE) $(LINK32_OBJS)
    $(LINK32) @<<
  $(LINK32_FLAGS) $(LINK32_OBJS)
<<

!ENDIF 


!IF "$(NO_EXTERNAL_DEPS)" != "1"
!IF EXISTS("tagbkup.dep")
!INCLUDE "tagbkup.dep"
!ELSE 
!MESSAGE Warning: cannot find "tagbkup.dep"
!ENDIF 
!ENDIF 


!IF "$(CFG)" == "tagbkup - Win32 Release" || "$(CFG)" == "tagbkup - Win32 Debug"
SOURCE=.\mp3tech.c

"$(INTDIR)\mp3tech.obj" : $(SOURCE) "$(INTDIR)"


SOURCE=.\nullmp3.cpp

"$(INTDIR)\nullmp3.obj" : $(SOURCE) "$(INTDIR)"


SOURCE=.\tagbkup.cpp

"$(INTDIR)\tagbkup.obj" : $(SOURCE) "$(INTDIR)"



!ENDIF 

