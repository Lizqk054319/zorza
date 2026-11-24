; Instalator Zora Client (Inno Setup) – budowany automatycznie na GitHubie
#define AppVer GetEnv("ZORZA_VERSION")

[Setup]
AppId={{6A1F2C77-6C3B-4E0E-9B7E-5F3A2B9C0D11}
AppName=Zora Client
AppVersion={#AppVer}
AppPublisher=Zorza
AppPublisherURL=https://github.com/Lizqk054319/zorza
DefaultDirName={localappdata}\Programs\Zora Client
DefaultGroupName=Zora Client
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\dist-installer
OutputBaseFilename=ZorzaSetup
SetupIconFile=zorza.ico
UninstallDisplayIcon={app}\Zora Client.exe
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
CloseApplications=no

[Languages]
Name: "polish"; MessagesFile: "compiler:Languages\Polish.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "..\dist\Zora Client\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[InstallDelete]
; stara nazwa programu (Zorza Launcher)
Type: files; Name: "{app}\Zorza Launcher.exe"
Type: files; Name: "{app}\Zorza Visuals.exe"
Type: files; Name: "{group}\Zorza Visuals.lnk"
Type: files; Name: "{autodesktop}\Zorza Visuals.lnk"
Type: files; Name: "{group}\Zorza Launcher.lnk"
Type: files; Name: "{autodesktop}\Zorza Launcher.lnk"

[Icons]
Name: "{group}\Zora Client"; Filename: "{app}\Zora Client.exe"
Name: "{autodesktop}\Zora Client"; Filename: "{app}\Zora Client.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\Zora Client.exe"; Description: "{cm:LaunchProgram,Zora Client}"; Flags: nowait postinstall skipifsilent
; po automatycznej aktualizacji z launchera – uruchom nową wersję
Filename: "{app}\Zora Client.exe"; Parameters: "--updated"; Flags: nowait; Check: IsAutoUpdate

[Code]
function IsAutoUpdate(): Boolean;
begin
  Result := ExpandConstant('{param:AUTOUPDATE|0}') = '1';
end;

// Launcher działa w tle bez okna, więc zamykamy go sami przed instalacją (stare nazwy też)
function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  Code: Integer;
begin
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/F /IM "Zora Client.exe"', '', SW_HIDE, ewWaitUntilTerminated, Code);
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/F /IM "Zorza Visuals.exe"', '', SW_HIDE, ewWaitUntilTerminated, Code);
  Exec(ExpandConstant('{sys}\taskkill.exe'), '/F /IM "Zorza Launcher.exe"', '', SW_HIDE, ewWaitUntilTerminated, Code);
  Sleep(800);
  Result := '';
end;
