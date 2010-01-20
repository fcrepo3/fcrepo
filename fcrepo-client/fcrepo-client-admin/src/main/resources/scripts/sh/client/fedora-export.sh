#!/bin/sh

scriptdir=`dirname "$0"`
. "$scriptdir"/env-client.sh

execWithCmdlineArgs org.fcrepo.client.utility.export.Export

exit $?
