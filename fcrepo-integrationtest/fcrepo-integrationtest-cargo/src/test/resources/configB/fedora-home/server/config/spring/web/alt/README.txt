Alternate security configuration
---------------------------------

The files in this directory are not visible to Fedora by default, and
represent alternate configurations.  

security-complicated.xml

This file contains an alternate security implementation, particularly in SSL
processing rules.  If for some reason tomcat is required to provide ssl for 
api-a or api-m, rest, etc, this file provides more complete/correct control 
in specifying the required ssl or non-ssl channel for a given resource.  In its
classic configuration, Fedora was inconsistent in its application of SSL for 
"secure" or "management operations.

To enable this configuration, replace /config/spring/web/security.xml with with
security-complicated.xml file.