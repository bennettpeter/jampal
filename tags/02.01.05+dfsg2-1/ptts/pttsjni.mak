# Microsoft Developer Studio Generated NMAKE File, Based on pttsjni.dsp
!IF "$(CFG)" == ""
CFG=pttsjni - Win32 Debug
!MESSAGE No configuration specified. Defaulting to pttsjni - Win32 Debug.
!ENDIF 

!IF "$(CFG)" != "pttsjni - Win32 Release" && "$(CFG)" != "pttsjni - Win32 Debug"
!MESSAGE Invalid configuration "$(CFG)" specified.
!MESSAGE You can specify a configuration when running NMAKE
!MESSAGE by defining the macro CFG on the command line. For example:
!MESSAGE 
!MESSAGE NMAKE /f "pttsjni.mak" CFG="pttsjni - Win32 Debug"
!MESSAGE 
!MESSAGE Possible choices for configuration are:
!MESSAGE 
!MESSAGE "pttsjni - Win32 Release" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE "pttsjni - Win32 Debug" (based on "Win32 (x86) Dynamic-Link Library")
!MESSAGE 
!ERROR An invalid configuration is specified.
!ENDIF 

!IF "$(OS)" == "Windows_NT"
NULL=
!ELSE 
NULL=nul
!ENDIF 

!IF  "$(CFG)" == "pttsjni - Win32 Release"

OUTDIR=.\pttsjni___Win32_Release
INTDIR=.\pttsjni___Win32_Release
# Begin Custom Macros
OutDir=.\pttsjni___Win32_Release
# End Custom Macros

ALL : "$(OUTDIR)\pttsjni.dll"


CLEAN :
	-@erase "$(INTDIR)\ptts.obj"
	-@erase "$(INTDIR)\Speaker.obj"
	-@erase "$(INTDIR)\vc60.idb"
	-@erase "$(OUTDIR)\pttsjni.dll"
	-@erase "$(OUTDIR)\pttsjni.exp"
	-@erase "$(OUTDIR)\pttsjni.lib"

"$(OUTDIR)" :
    if not exist "$(OUTDIR)/$(NULL)" mkdir "$(OUTDIR)"

CPP=cl.exe
CPP_PROJ=/nologo /MT /W3 /GX /O2 /I "C:\Program Files\Microsoft Speech SDK 5.1\Include" /I "C:\Products\j2sdk1.4.2_13\include" /I "C:\Products\j2sdk1.4.2_13\include\win32" /D "WIN32" /D "NDEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "PTTSJNI_EXPORTS" /Fp"$(INTDIR)\pttsjni.pch" /YX /Fo"$(INTDIR)\\" /Fd"$(INTDIR)\\" /FD /c 

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

MTL=midl.exe
MTL_PROJ=/nologo /D "NDEBUG" /mktyplib203 /win32 
RSC=rc.exe
BSC32=bscmake.exe
BSC32_FLAGS=/nologo /o"$(OUTDIR)\pttsjni.bsc" 
BSC32_SBRS= \
	
LINK32=link.exe
LINK32_FLAGS=kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /incremental:no /pdb:"$(OUTDIR)\pttsjni.pdb" /machine:I386 /out:"$(OUTDIR)\pttsjni.dll" /implib:"$(OUTDIR)\pttsjni.lib" /libpath:"C:\Program Files\Microsoft Speech SDK 5.1\Lib\i386" 
LINK32_OBJS= \
	"$(INTDIR)\ptts.obj" \
	"$(INTDIR)\Speaker.obj"

"$(OUTDIR)\pttsjni.dll" : "$(OUTDIR)" $(DEF_FILE) $(LINK32_OBJS)
    $(LINK32) @<<
  $(LINK32_FLAGS) $(LINK32_OBJS)
<<

!ELSEIF  "$(CFG)" == "pttsjni - Win32 Debug"

OUTDIR=.\pttsjni___Win32_Debug
INTDIR=.\pttsjni___Win32_Debug
# Begin Custom Macros
OutDir=.\pttsjni___Win32_Debug
# End Custom Macros

ALL : "$(OUTDIR)\pttsjni.dll"


CLEAN :
	-@erase "$(INTDIR)\ptts.obj"
	-@erase "$(INTDIR)\Speaker.obj"
	-@erase "$(INTDIR)\vc60.idb"
	-@erase "$(INTDIR)\vc60.pdb"
	-@erase "$(OUTDIR)\pttsjni.dll"
	-@erase "$(OUTDIR)\pttsjni.exp"
	-@erase "$(OUTDIR)\pttsjni.ilk"
	-@erase "$(OUTDIR)\pttsjni.lib"
	-@erase "$(OUTDIR)\pttsjni.pdb"

"$(OUTDIR)" :
    if not exist "$(OUTDIR)/$(NULL)" mkdir "$(OUTDIR)"

CPP=cl.exe
CPP_PROJ=/nologo /MTd /W3 /Gm /GX /ZI /Od /I "C:\Program Files\Microsoft Speech SDK 5.1\Include" /I "C:\Products\j2sdk1.4.2_13\include" /I "C:\Products\j2sdk1.4.2_13\include\win32" /D "WIN32" /D "_DEBUG" /D "_WINDOWS" /D "_MBCS" /D "_USRDLL" /D "PTTSJNI_EXPORTS" /Fp"$(INTDIR)\pttsjni.pch" /YX /Fo"$(INTDIR)\\" /Fd"$(INTDIR)\\" /FD /GZ /c 

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

MTL=midl.exe
MTL_PROJ=/nologo /D "_DEBUG" /mktyplib203 /win32 
RSC=rc.exe
BSC32=bscmake.exe
BSC32_FLAGS=/nologo /o"$(OUTDIR)\pttsjni.bsc" 
BSC32_SBRS= \
	
LINK32=link.exe
LINK32_FLAGS=kernel32.lib user32.lib gdi32.lib winspool.lib comdlg32.lib advapi32.lib shell32.lib ole32.lib oleaut32.lib uuid.lib odbc32.lib odbccp32.lib /nologo /dll /incremental:yes /pdb:"$(OUTDIR)\pttsjni.pdb" /debug /machine:I386 /out:"$(OUTDIR)\pttsjni.dll" /implib:"$(OUTDIR)\pttsjni.lib" /pdbtype:sept /libpath:"C:\Program Files\Microsoft Speech SDK 5.1\Lib\i386" 
LINK32_OBJS= \
	"$(INTDIR)\ptts.obj" \
	"$(INTDIR)\Speaker.obj"

"$(OUTDIR)\pttsjni.dll" : "$(OUTDIR)" $(DEF_FILE) $(LINK32_OBJS)
    $(LINK32) @<<
  $(LINK32_FLAGS) $(LINK32_OBJS)
<<

!ENDIF 


!IF "$(NO_EXTERNAL_DEPS)" != "1"
!IF EXISTS("pttsjni.dep")
!INCLUDE "pttsjni.dep"
!ELSE 
!MESSAGE Warning: cannot find "pttsjni.dep"
!ENDIF 
!ENDIF 


!IF "$(CFG)" == "pttsjni - Win32 Release" || "$(CFG)" == "pttsjni - Win32 Debug"
SOURCE=.\ptts.cpp

"$(INTDIR)\ptts.obj" : $(SOURCE) "$(INTDIR)"


SOURCE=.\Speaker.cpp

"$(INTDIR)\Speaker.obj" : $(SOURCE) "$(INTDIR)"



!ENDIF 

