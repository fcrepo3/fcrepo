Instructions for creating the keystore and truststore.
------------------------------------------------------

Generate the certificate keystore for Tomcat. 
    keytool -genkey -alias tomcat -keyalg RSA -validity 365 -keystore keystore
When prompted for the keystore password, enter "changeit" (without the quotes).

Generate the client truststore
    keytool -export -keystore keystore -alias tomcat -file tomcat.cer
    keytool -import -alias tomcat -keystore truststore -trustcacerts -file tomcat.cer
When prompted for the truststore password, use "tomcat"

References:
http://tomcat.apache.org/tomcat-6.0-doc/ssl-howto.html

Updating the Bundled Tomcat
---------------------------
Replace resources/inc/apache-tomcat-NEW-VERSION.zip
Update fcrepo-server/src/main/properties/resources/install.properties
Update fcrepo-installer/src/main/resources/assemblies/fedora-installer.xml
Update resources/server/fedora/utilities/install/OptionDefinition.properties
Update fcrepo-server/src/main/java/fedora/utilities/install/container/BundledTomcat.java
Update src/java/fedora/utilities/install/container as necessary (micro version
updates should not require any code updates; major version updates, which, would
correspond to updated servlet spec support will likely require updates to 
src/java/fedora/server/config/webxml).