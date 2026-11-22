; Instalator Zorza Launcher (Inno Setup) – budowany automatycznie na GitHubie
#define AppVer GetEnv("ZORZA_VERSION")

[Setup]
AppId={{6A1F2C77-6C3B-4E0E-9B7E-5F3A2B9C0D11}
AppName=Zorza Launcher
AppVersion={#AppVer}
AppPublisher=Zorza
AppPublisherURL=https://github.com/Lizqk054319/zorza
DefaultDirName={localappdata}\Programs\Zorza Launcher
DefaultGroupName=Zorza Launcher
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=..\dist-installer
OutputBaseFilename=ZorzaSetup
SetupIconFile=zorza.ico
UninstallDisplayIcon={app}\Zorza Launcher.exe
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
Source: "..\dist\Zorza Launcher\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\Zorza Launcher"; Filename: "{app}\Zorza Launcher.exe"
Name: "{autodesktop}\Zorza Launcher"; Filename: "{app}\Zorza Launcher.exe"; Tasks: desktopicon

[Run]
Filename: "{app}\Zorza Launcher.exe"; Description: "{cm:LaunchProgram,Zorza Launcher}"; Flags: nowait postinstall skipifsilent
