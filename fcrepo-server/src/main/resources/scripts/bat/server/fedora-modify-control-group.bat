@echo off
setlocal

if not "%FEDORA_HOME%" == "" goto gotFedoraHome
echo ERROR: The FEDORA_HOME environment variable is not defined.
exit /B 1
:gotFedoraHome

set LAUNCHER="%FEDORA_HOME%\server\bin\env-server.bat"

call %LAUNCHER% org.fcrepo.server.utilities.ServerUtility migrateDatastreamControlGroup %*