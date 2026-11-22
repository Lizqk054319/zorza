; Instalator Zorza Visuals (Inno Setup) – budowany automatycznie na GitHubie
#define AppVer GetEnv("ZORZA_VERSION")

[Setup]
AppId={{6A1F2C77-6C3B-4E0E-9B7E-5F3A2B9C0D11}
AppName=Zorza Visuals
AppVersion={#AppVer}
AppPublisher=Zorza
AppPublisherURL=https://github.com/Lizqk054319/zorza
DefaultDirName={localappdata}\Programs\Zorza Visuals
DefaultGroupName=Zorza Visuals
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\dist-installer
OutputBaseFilename=ZorzaSetup
SetupIconFile=zorza.ico
UninstallDisplayIcon={app}\Zorza Visuals.exe
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
CloseApplications=yes

[Languages]
Name: "polish"; MessagesFile: "compiler:Languages\Polish.isl"
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"

[Files]
Source: "..\dist\Zorza Visuals\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[InstallDelete]
; stara nazwa programu (Zorza Launcher)
Type: files; Name: "{app}\Zorza Launcher.exe"
Type: files; Name: "{group}\Zorza Launcher.lnk"
Type: files; Name: "{autodesktop}\Zorza Launcher.lnk"

[Icons]
Name: "{group}\Zorza Visuals"; Filename: "{app}\Zorza Visuals.exe"
Name: "{autodesktop}\Zorza Visuals"; Filename: "{app}\Zorza Visuals.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\Zorza Visuals.exe"; Description: "{cm:LaunchProgram,Zorza Visuals}"; Flags: nowait postinstall skipifsilent
