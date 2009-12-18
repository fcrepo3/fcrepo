#!/bin/bash

echo "---------------------"
echo "Installing Fedora...."
echo "---------------------"
echo ""

SCRIPTPATH=$(cd ${0%/*} && echo $PWD/${0##*/})
SCRIPTDIR=`dirname "$SCRIPTPATH"`
. $SCRIPTDIR/common.sh

# Basic check of arguments
if [ $# -ne 2 ]; then
  echo "ERROR: Expected 2 args; java5|java6 installprops"
  exit 1
fi

INSTALLPROPS=$SCRIPTDIR/$2

CATALINA_HOME=$FEDORA_HOME/tomcat
export CATALINA_HOME
echo "CATALINA_HOME = $CATALINA_HOME"
echo ""

# Determine the installer path
INSTALLER=`find $BUILD_HOME/fcrepo-installer/target/ -name *installer*.jar`
if [ ${#INSTALLER} -lt 5 ]; then
  echo "ERROR: Installer not found in $BUILD_HOME/fcrepo-installer/target"
  exit 1
fi

echo "Removing $FEDORA_HOME"
rm -rf $FEDORA_HOME

if [ $? -ne 0 ]; then
  echo "ERROR: Failed to remove $FEDORA_HOME"
  exit 1
fi

echo "Installing Fedora using $INSTALLPROPS"
$JAVA_HOME/bin/java -jar $INSTALLER $INSTALLPROPS

if [ $? -ne 0 ]; then
  echo "ERROR: Failed to install Fedora"
  exit 1
fi

echo ""
echo "-----------------------------"
echo "Successfully installed Fedora"
echo "-----------------------------"

