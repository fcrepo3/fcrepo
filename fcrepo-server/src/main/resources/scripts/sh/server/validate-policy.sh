#!/bin/sh

scriptdir=`dirname "$0"`
. "$scriptdir"/env-server.sh

execWithCmdlineArgs fedora.server.security.PolicyParser

exit $?
