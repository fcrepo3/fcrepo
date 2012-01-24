#!/bin/bash

#
# CHANGE THESE SETTINGS AS APPROPRIATE FOR YOUR TEST ENVIRONMENT
# NOTE: You should also change the environment-specific setttings
#       in the Config*.properties files.
#

# Comfortable memory settings for Tomcat and Maven
JAVA_OPTS="-Xms384m -Xmx384m -XX:PermSize=192m -XX:MaxPermSize=192m -Dfedora.fesl.pep_nocache=true"
MAVEN_OPTS="$JAVA_OPTS"

# Where is JDK 1.6 installed?
JAVA6_HOME=/usr/lib/jvm/java-6-sun

# Where is maven installed?
M2_HOME=/usr/share/maven2

# Where is the Fedora source distribution to be tested?
if [ -z "$BUILD_HOME" ]; then
  BUILD_HOME=`pwd`
fi

# Where should test instances of Fedora be installed?
# This will be created and cleared out by test scripts as necessary.
FEDORA_HOME=$HOME/fedora-home

# When installed, what port will non-secure http requests be on?
HTTP_PORT=9080

# What hostname will the Fedora webapp be accessable on?
FEDORA_HOSTNAME=localhost

#
# DON'T CHANGE BELOW THIS LINE
#
CATALINA_HOME=$FEDORA_HOME/tomcat

export BUILD_HOME
export JAVA_OPTS
export MAVEN_OPTS
export FEDORA_HOME
export CATALINA_HOME
