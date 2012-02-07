#!/bin/sh

scriptdir=`dirname "$0"`
. "$scriptdir"/env-server.sh

execWithCmdlineArgs org.fcrepo.server.utilities.ServerUtility reloadPolicies

exit $?
