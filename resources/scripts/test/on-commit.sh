#!/bin/bash

echo "****************************"
echo "Starting on-commit tests...."
echo "****************************"
echo ""

SCRIPTPATH=$(cd ${0%/*} && echo $PWD/${0##*/})
SCRIPTDIR=`dirname "$SCRIPTPATH"`

$SCRIPTDIR/sanity.sh java5

if [ $? -ne 0 ]; then
  echo ""
  echo "ERROR: On-commit tests failed; see above"
  exit 1
fi

echo ""
echo "***************************************"
echo "Completed on-commit tests successfully!"
echo "***************************************"

