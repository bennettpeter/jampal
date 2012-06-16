;Packaging script for Jampal

[Setup]
AppName=Jampal
AppVerName=Jampal
DefaultDirName={pf}\Jampal
UsePreviousAppDir=yes
DefaultGroupName=Jampal
ArchitecturesInstallIn64BitMode=x64 ia64
ArchitecturesAllowed=x86 x64 ia64

[Types]
Name: "full"; Description: "Full installation"
Name: "compact"; Description: "Compact installation"
Name: "custom"; Description: "Custom installation"; Flags: iscustom


[Components] 
Name: "jampal"; Description: "Jampal mp3 Library and Jukebox"; Types: full compact
Name: "utilities"; Description: "Text to Speech and Tag Backup"; Types: full
Name: "looks"; Description: "Skins / Look and Feel Options"; Types: full
;2000 Name: "tts"; Description: "Microsoft English Text to Speech Engine"; Types: full; MinVersion: 4.1.1998,4.0; OnlyBelowVersion: 5.01,5.01
;XP Name: "voices"; Description: "Microsoft Mike and Mary Voices"; Types: full; MinVersion: 5.01,5.01


[Files]
Source: "jampal.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: jampal
Source: "jampal_environment.properties_windows"; DestDir: "{app}"; DestName: "jampal_environment.properties"; Flags: ignoreversion; Components: jampal
Source: "jampal_initial.properties_windows"; DestDir: "{app}"; DestName: "jampal_initial.properties"; Flags: ignoreversion; Components: jampal
Source: "liquidlnf.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: looks
Source: "looks-2.3.0.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: looks
Source: "napkinlaf-1.2.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: looks
Source: "squareness.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: looks
Source: "InfoNodeilf-gpl.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: looks
Source: "lipstikLF-1.1.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: looks
Source: "nimrodlf.j16.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: looks
Source: "PgsLookAndFeel.jar"; DestDir: "{app}"; Flags: ignoreversion; Components: looks
Source: "jampal.ico"; DestDir: "{app}"; Flags: ignoreversion; Components: jampal
;Source: "jampal2.ico"; DestDir: "{app}"; Flags: ignoreversion; Components: jampal
Source: "ptts.vbs"; DestDir: "{app}"; Flags: ignoreversion; Components: utilities
;Source: "pttsjni.dll"; DestDir: "{app}"; Flags: ignoreversion; Components: jampal utilities
Source: "mbrola*"; DestDir: "{app}"; Flags: ignoreversion; Components: jampal utilities
Source: "COPYING"; DestDir: "{app}"; Flags: ignoreversion; Components: jampal utilities
Source: "tagbkup.exe"; DestDir: "{app}"; Flags: ignoreversion; Components: utilities
Source: "*.dll"; DestDir: "{app}"; Flags: ignoreversion; Components: utilities
;Source: "tagbkup_cygwin.exe"; DestDir: "{app}"; Flags: ignoreversion; Components: utilities
Source: "doc\*"; DestDir: "{app}\doc"; Flags: recursesubdirs ignoreversion; Components: jampal utilities
;Source: "scripts\*"; DestDir: "{app}\scripts"; Flags: recursesubdirs ignoreversion; Components: jampal utilities
;2000 Source: "Microsoft-English-TTS-51.msi"; DestDir: "{app}"; Flags: deleteafterinstall ignoreversion; Components: tts; MinVersion: 4.1.1998,4.0; OnlyBelowVersion: 5.01,5.01
;XP Source: "MarMike5.msi"; DestDir: "{app}"; Flags: deleteafterinstall ignoreversion; Components: voices; MinVersion: 5.01,5.01

[Icons]
Name: "{group}\Jampal"; Filename: "{code:GetJavaw}"; Parameters: "-Xincgc -Xmx256M -jar jampal.jar"; WorkingDir: "{app}"; IconFilename: "{app}\jampal.ico"; Components: jampal
Name: "{group}\Jampal Documentation"; Filename: "{app}\doc\summary.html"; Components: jampal
Name: "{group}\Uninstall Jampal"; Filename: "{uninstallexe}"

[Registry]
Root: HKCR; Subkey: ".jampal"; ValueType: string; ValueName: ""; ValueData: "Jampal.Document"; Flags: uninsdeletekey ; Components: jampal
Root: HKCR; Subkey: "Jampal.Document"; ValueType: string; ValueName: ""; ValueData: "Jampal mp3 Library"; Flags: uninsdeletekey ; Components: jampal
Root: HKCR; Subkey: "Jampal.Document\DefaultIcon"; ValueType: string; ValueName: ""; ValueData: "{app}\jampal.ico" ; Flags: uninsdeletekey; Components: jampal
Root: HKCR; Subkey: "Jampal.Document\shell\open\command"; ValueType: string; ValueName: ""; ValueData: """{code:GetJavaw}"" -Xincgc -Xmx256M -jar ""{app}\jampal.jar"" ""%1""" ; Flags: uninsdeletekey; Components: jampal


;[Run]
;2000 Filename: "msiexec.exe"; Parameters: "/i Microsoft-English-TTS-51.msi"; WorkingDir: "{app}"; Components: tts; StatusMsg: "Installing Microsoft English TTS 51"
;XP Filename: "msiexec.exe"; Parameters: "/i MarMike5.msi"; WorkingDir: "{app}"; Components: voices; StatusMsg: "Installing Microsoft Voices"

[Code]
var
  JavaHome: String;
  JavaQuestionPage : TInputOptionWizardPage;
  JavaFindPage : TInputFileWizardPage;
  JavaWaitPage : TOutputMsgWizardPage;
  JavaFound, HardDriveOption, DownloadOption : Boolean;

procedure InitializeWizard();
begin
  JavaHome := '';
  JavaFound := False;
  HardDriveOption := False;
  DownloadOption := False;
  JavaQuestionPage := CreateInputOptionPage(wpSelectComponents,
    'Jampal Requires Java Runtime Version 1.6 or later',
    'Please Select an Option for Java Runtime',
    '',
    True, False);
  JavaQuestionPage.Add('Locate a Java Runtime on your Hard Drives');
  JavaQuestionPage.Add('Download a Java Runtime from the Internet');
  JavaFindPage := CreateInputFilePage(JavaQuestionPage.ID,
    'Jampal Requires Java Runtime Version 1.6 or later',
    'Please Locate the Java Runtime on Your Hard Drive',
    '');
  JavaFindPage.Add('Please Select the Java Runtime File javaw.exe',
  'Java Runtime(javaw.exe)|javaw.exe',
  '.exe');
  JavaWaitPage := CreateOutputMsgPage(JavaFindPage.ID,
  'Java Runtime Download',
  '',
  'Please Click NEXT When Java Has Been Installed');

end;

function GetJavaHome(): Boolean;
var
  JavaVersion : String;

begin
  JavaHome := '';
  JavaFound := False;
  JavaFound := RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\JavaSoft\Java Runtime Environment', 'CurrentVersion', JavaVersion);
  if JavaFound then begin
    if  JavaVersion < '1.6' then
      JavaFound := False
    else
      JavaFound := RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\JavaSoft\Java Runtime Environment\' + JavaVersion, 'JavaHome', JavaHome);
  end;
  if (not JavaFound) and Is64BitInstallMode then begin
    JavaFound := RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\Wow6432Node\JavaSoft\Java Runtime Environment', 'CurrentVersion', JavaVersion);
    if JavaFound then begin
        if  JavaVersion < '1.6' then
            JavaFound := False
        else
            JavaFound := RegQueryStringValue(HKEY_LOCAL_MACHINE, 'SOFTWARE\Wow6432Node\JavaSoft\Java Runtime Environment\' + JavaVersion, 'JavaHome', JavaHome);
    end;
  end;
  if JavaFound then
    JavaHome := JavaHome + '\bin\javaw.exe';
  Result := JavaFound;
end;

function NextButtonClick(CurPage: Integer): Boolean;
begin
  Result := True;
  if CurPage = wpSelectComponents
  then begin
    GetJavaHome();
    if WizardSelectedComponents(False) = '' then begin
      MsgBox('Please Select at least one Component to Install', mbError, MB_OK);
      Result := False
    end;
    if IsComponentSelected('looks') and not IsComponentSelected('jampal')  then begin
      MsgBox('If you select Skins you must also select Jampal', mbError, MB_OK);
      Result := False
    end;
  end;
  if CurPage = JavaQuestionPage.ID 
  then begin
    HardDriveOption := False;
    DownloadOption := False;
    if JavaQuestionPage.Values[0] then
      HardDriveOption := True;
    if JavaQuestionPage.Values[1] then
      DownloadOption := True;
    Result := HardDriveOption or DownLoadOption
  end;
  if CurPage = JavaFindPage.ID
  then begin
    JavaFound := False;
    JavaHome := JavaFindPage.Values[0];
    if (ExtractFileName(JavaHome) <> 'javaw.exe') then begin
      MsgBox('Please Select the File Named javaw.exe', mbError, MB_OK);
      JavaHome := '';
      Result := False;
    end
    else if not FileExists(JavaHome) then begin
      MsgBox('File '+JavaHome+' Not Found', mbError, MB_OK);
      JavaHome := '';
      Result := False;
    end
    else
      JavaFound := True
  end;
  if CurPage = JavaWaitPage.ID then begin
    GetJavaHome();
    if not JavaFound then begin
      MsgBox('Java Download failed' + #13'Please download java from www.java.com', mbError, MB_OK);
      Result := False;
    end;
  end;
end;


function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := False;
  if (PageID = JavaQuestionPage.ID) and not IsComponentSelected('jampal') then
    Result := True;
  if (PageID = JavaFindPage.ID) and not IsComponentSelected('jampal') then
    Result := True;
  if (PageID = JavaWaitPage.ID) and not IsComponentSelected('jampal') then
    Result := True;
  if (PageID = JavaQuestionPage.ID) and JavaFound then
    Result := True;
  if (PageID = JavaFindPage.ID) and not HardDriveOption then
    Result := True;
  if (PageID = JavaWaitPage.ID) and not DownloadOption then
    Result := True;

end;

procedure CurPageChanged(CurPageID: Integer);
var
  ErrorCode : Integer;
begin
  if (CurPageID = JavaWaitPage.ID) then
//    if not ShellExec('open','http://www.java.com/en/download/windows_automatic.jsp', '', '', SW_SHOWNORMAL, ewNoWait, ErrorCode) then
    if not ShellExec('open','http://java.com/en/download/manual.jsp', '', '', SW_SHOWNORMAL, ewNoWait, ErrorCode) then
//    if not ShellExec('', ExpandConstant('{pf}\Internet Explorer\iexplore.exe'),'http://www.java.com/en/download/windows_automatic.jsp',        '', SW_SHOWNORMAL, ewNoWait, ErrorCode) then
        MsgBox('Java Download failed' #13 + SysErrorMessage(ErrorCode) + #13'Please download java from www.java.com', mbError, MB_OK)
end;

function GetJavaw(Default: String): String;
begin
  if JavaHome = '' then
      MsgBox('Did Not Find Java Home', mbError, MB_OK);
  Result := JavaHome;
end;


