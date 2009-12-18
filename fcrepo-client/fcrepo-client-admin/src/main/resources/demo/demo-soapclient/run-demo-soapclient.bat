@echo off

goto checkEnv
:envOk

echo Starting Demo SOAP Client...

set OLD_JAVA_HOME=%JAVA_HOME%
set JAVA_HOME=%THIS_JAVA_HOME%

"%JAVA_HOME%\bin\java" -cp %FEDORA_HOME%;%FEDORA_HOME%\client;%FEDORA_HOME%\client\${fedora-client-jar} -Djava.endorsed.dirs="%FEDORA_HOME%\client\lib" -Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.Log4jFactory -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4jLoggerr -Djavax.net.ssl.trustStore="%FEDORA_HOME%\client\truststore" -Djavax.net.ssl.trustStorePassword=tomcat -Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl -Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl -Dfedora.home=%FEDORA_HOME% demo.soapclient.DemoSOAPClient %1 %2 %3 %4 %5

set JAVA_HOME=%OLD_JAVA_HOME%

goto end

:checkEnv
if "%FEDORA_HOME%" == "" goto noFedoraHome
if not exist "%FEDORA_HOME%\client\${fedora-client-jar}" goto clientNotFound
if "%FEDORA_JAVA_HOME%" == "" goto tryJavaHome
set THIS_JAVA_HOME=%FEDORA_JAVA_HOME%
:checkJava
if not exist "%THIS_JAVA_HOME%\bin\java.exe" goto noJavaBin
if not exist "%THIS_JAVA_HOME%\bin\orbd.exe" goto badJavaVersion
goto envOk

:tryJavaHome
if "%JAVA_HOME%" == "" goto noJavaHome
set THIS_JAVA_HOME=%JAVA_HOME%
goto checkJava

:noFedoraHome
echo ERROR: Environment variable, FEDORA_HOME must be set.
goto end

:clientNotFound
echo ERROR: FEDORA_HOME does not appear correctly set.
echo Client cannot be found at %FEDORA_HOME%\client\${fedora-client-jar}
goto end

:noJavaHome
echo ERROR: FEDORA_JAVA_HOME was not defined, nor was (the fallback) JAVA_HOME.
goto end

:noJavaBin
echo ERROR: java.exe was not found in %THIS_JAVA_HOME%
echo Make sure FEDORA_JAVA_HOME or JAVA_HOME is set correctly.
goto end

:badJavaVersion
echo ERROR: java was found in %THIS_JAVA_HOME%, but it was not version 1.4
echo Make sure FEDORA_JAVA_HOME or JAVA_HOME points to a 1.4JRE/JDK base.
goto end

:end
