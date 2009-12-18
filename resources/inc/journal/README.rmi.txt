RMI Journal Receiver
--------------------

This distribution contains an executable jar file that functions as a 
receiver for journal events sent from a Fedora server via RMI.  For
more information, please see http://fedora-commons.org/confluence/x/04qv

USAGE:
java -jar RmiReceiver.jar DIRPATH [registryPort] [serverPort] [logLevel]

  RmiReceiver.jar: The RMI receiver jar file.  The RMI receiver is non-interactive
    and intended to be run as a daemon/service.
    
  DIRPATH (required): Full path to directory where journal files will be created
    from received journal events
  
  [registryPort] (optional): RMI registry port.  Default is 1099
  
  [serverPort] (optional): RMI server port.  Default is 1100
  
  [logLevel] (optional) : Level of logging to System.out console.  Default is WARN.  
    Uses log4j log level vocabulary.