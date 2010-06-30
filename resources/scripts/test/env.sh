#!/bin/bash

#
# CHANGE THESE SETTINGS AS APPROPRIATE FOR YOUR TEST ENVIRONMENT
# NOTE: You should also change the environment-specific setttings
#       in the Config*.properties files
#

# Comfortable memory settings for Tomcat and Maven
JAVA_OPTS="-Xms384m -Xmx384m -XX:PermSize=192m -XX:MaxPermSize=192m"
MAVEN_OPTS="$JAVA_OPTS"

# Where is JDK 1.6 installed?
JAVA6_HOME=/usr/lib/jvm/java-6-sun

# Where is maven installed?
M2_HOME=/usr/share/maven2

# Where is the Fedora source distribution to be tested?
BUILD_HOME=$HOME/bamboo-agent-home/xml-data/build-dir/FCREPO-LINUXSAN

# Where should test instances of Fedora be installed?
# This will be created and cleared out by test scripts as necessary.
FEDORA_HOME=$HOME/fedora-home

# When installed, what port will non-secure http requests be on?
HTTP_PORT=9080

# Special test case situation where PEP caching needs to be disabled.
PEP_NOCACHE=true

#
# DON'T CHANGE BELOW THIS LINE
#
CATALINA_HOME=$FEDORA_HOME/tomcat

export JAVA_OPTS
export MAVEN_OPTS
export FEDORA_HOME
export CATALINA_HOME
