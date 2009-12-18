#------------------------------------------------------------------------------
# Common environment checks and launcher for Fedora client scripts.
#
# Environment Variables:
#   FEDORA_HOME: Required.  Used to determine the location of client classes
#                and other resources required to run the utilities.
#   JAVA_HOME  : Optional.  Used to determine the location of java.
#                If JAVA_HOME is unspecified, will use FEDORA_JAVA_HOME.
#                If FEDORA_JAVA_HOME is unspecified, will use java in PATH. 
#------------------------------------------------------------------------------

if [ -z "$FEDORA_HOME" ]; then
  echo "ERROR: The FEDORA_HOME environment variable is not defined."
  exit 1
fi

if [ ! -f "$FEDORA_HOME/client/${fedora-client-jar}" ]; then
  echo "ERROR: ${fedora-client-jar} not found in $FEDORA_HOME/client"
  exit 1
fi

if [ -z "$JAVA_HOME" ]; then
  if [ -z "$FEDORA_JAVA_HOME" ]; then
    java="java"
  else
    java="$FEDORA_JAVA_HOME"/bin/java
  fi
else
  java="$JAVA_HOME"/bin/java
fi

cmdline_args=
for arg in "$@" ; do
  cmdline_args="$cmdline_args \"$arg\""
done

execWithCmdlineArgs() {
    execWithTheseArgs $1 "$cmdline_args"
    return $?
}

execWithTheseArgs() {
    exec_cmd="exec \"$java\" -Xms64m -Xmx96m \
            -cp \"$FEDORA_HOME\"/client:\"$FEDORA_HOME\"/client/${fedora-client-jar} \
            -Djava.endorsed.dirs=\"$FEDORA_HOME\"/client/lib \
            -Djavax.net.ssl.trustStore=\"$FEDORA_HOME\"/client/truststore \
            -Djavax.net.ssl.trustStorePassword=tomcat \
            -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl \
            -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl \
            -Dorg.apache.commons.logging.LogFactory=org.apache.commons.logging.impl.Log4jFactory \
            -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4jLogger \
            -Dfedora.home=\"$FEDORA_HOME\" \
            $1 $2"
    eval $exec_cmd
    return $?
}
