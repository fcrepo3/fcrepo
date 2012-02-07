@echo off
setlocal

if not "%FEDORA_HOME%" == "" goto gotFedoraHome
echo ERROR: The FEDORA_HOME environment variable is not defined.
exit /B 1
:gotFedoraHome

set LAUNCHER="%FEDORA_HOME%\client\bin\env-client.bat"

if not "%5"=="" goto enoughArgs
echo ERROR: Not enough arguments.
echo Usage:
echo   fedora-ingest-demos host port user password protocol [context]
echo Example:
echo   fedora-ingest-demos localhost 8080 fedoraAdmin fedoraAdmin http my-fedora
exit /B 1
:enoughArgs

set DEMO_PATH="%FEDORA_HOME%\client\demo\foxml\local-server-demos"
set DEMO_FORMAT=info:fedora/fedora-system:FOXML-1.1

set ARGS=d
set ARGS=%ARGS% %DEMO_PATH%
set ARGS=%ARGS% %DEMO_FORMAT%
set ARGS=%ARGS% "%1:%2" %3 %4 %5 "" %6

call %LAUNCHER% org.fcrepo.client.utility.ingest.Ingest %ARGS%
