#------------------------------------------------------------------------------
# Common environment settings and launcher for Fedora server scripts.
#
# Environment Variables
#   FEDORA_HOME  : Required.  Used to determine the location of misc
#                  server resources required to run the utilities.
#   CATALINA_HOME: Required.  Used to determine the location of server
#                  classes required to run the utilities.
#   JAVA_HOME    : Optional.  Used to determine the location of java.
#                  If JAVA_HOME is unspecified, will use FEDORA_JAVA_HOME.
#                  If FEDORA_JAVA_HOME is unspecified, will use java in PATH. 
#   FEDORA_WEBAPP_HOME:  Optional.  Used to determine the location of the
#                  Fedora web application.  If FEDORA_WEBAPP_HOME is
#                  unspecified, then will use CATALINA_HOME/webapps/$webapp_name.
#------------------------------------------------------------------------------

if [ -z "$WEBAPP_NAME" ]; then
  webapp_name="fedora"
else 
  webapp_name=$WEBAPP_NAME
fi

if [ -z "$FEDORA_HOME" ]; then
  echo "ERROR: The FEDORA_HOME environment variable is not defined."
  exit 1
fi

if [ -z "$CATALINA_HOME" ]; then
  echo "ERROR: The CATALINA_HOME environment variable is not defined."
  exit 1
fi

if [ -z "$FEDORA_WEBAPP_HOME" ]; then
  FEDORA_WEBAPP_HOME="$CATALINA_HOME"/webapps/$webapp_name
fi

webinf="$FEDORA_WEBAPP_HOME"/WEB-INF

if [ ! -d "$webinf" ]; then
	echo "ERROR: Fedora could not be found in the specified path, please set the environment variable FEDORA_WEBAPP_HOME"
    echo "to the location of your Fedora web application directory, or set WEBAPP_NAME to the context Fedora is installed in."
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
    common="$CATALINA_HOME"/common
    exec_cmd="exec \"$java\" -server -Xmn64m -Xms256m -Xmx256m \
            -cp \"$webinf\"/classes:\"$FEDORA_HOME\"/server/bin:\"$webinf\"/lib/* \
            -Djava.endorsed.dirs=\"$common\"/endorsed:\"$common\"/lib \
            -Djavax.net.ssl.trustStore=\"$FEDORA_HOME\"/server/truststore \
            -Djavax.net.ssl.trustStorePassword=tomcat \
            -Djavax.xml.parsers.DocumentBuilderFactory=org.apache.xerces.jaxp.DocumentBuilderFactoryImpl \
            -Djavax.xml.parsers.SAXParserFactory=org.apache.xerces.jaxp.SAXParserFactoryImpl \
            -Dcom.sun.xacml.PolicySchema=\"$FEDORA_HOME\"/server/xsd/cs-xacml-schema-policy-01.xsd \
            -Dfedora.home=\"$FEDORA_HOME\" \
            -Dfedora.web.inf.lib=\"$webinf\"/lib \
            $1 $2"
    eval $exec_cmd
    return $?
}
