TITLEL UPDATE FIRST-PIPELINE
@ECHO OFF
CLS

set _mydate=%DATE%
set _mydate=%_mydate:* =%
set datum=%_mydate:~6,8%-%_mydate:~3,2%-%_mydate:~0,2%

set startDir=%cd%
set pipelineDir="F:\UHOH\ARCHIVE_LOCAL_FIRST_DB\CompleteJSIKnowledgeBasedCrispClassificationReceiver_*"
set zipInstallationDir="C:\Program Files\7-Zip"

REM -- Read Commandlineparameter->ZipFile with Local Path
SET ZipFilePath=%1
IF [%ZipFilePath%]==[] (
	ECHO Value: Path to zip File Missing
	GOTO ENDE
)

FOR /d %%d IN (%pipelineDir%) DO (
	cd %%d
	ECHO READING PROCESSNUMBER OF CURRENT PROCESS: %%d
	set /p PID=<PID.txt
	ECHO KILL PID: %PID%
	TASKKILL /F /PID %PID% /T
	
	ECHO backup current resources
	set backupFile="resources_%datum%.zip"
	%zipInstallationDir%\7z a -tzip %backupFile% resources\
	ECHO finished with backup to file: %backupFile%
	
	ECHO UNZIP resources
	%zipInstallationDir%\7z x -y %ZipFilePath% -r
	
	ECHO restarting !!!
	START run.bat
)

:ENDE
cd %startDir%
PAUSE
