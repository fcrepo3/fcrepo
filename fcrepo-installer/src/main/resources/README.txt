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
Download and deploy the distribution as a third-party artifact:
mvn deploy:deploy-file -Durl=https://m2.duraspace.org/content/repositories/thirdparty \
-DrepositoryId=duraspace-thirdparty -Dfile=tomcat-X.Y.Z.zip \
-DgroupId=org.apache.tomcat -DartifactId=tomcat -Dversion=X.Y.Z -Dpackaging=zip

Update resources/server/org/fcrepo/utilities/install/OptionDefinition.properties
Update fcrepo-server/src/main/resources/properties/resources/install.properties
Update fcrepo-installer/src/main/assembly/fedora-installer.xml
Update fcrepo-installer/src/main/java/org/fcrepo/utilities/install/container/BundledTomcat.java
Update src/main/java/org/fcrepo/utilities/install/container as necessary (micro version
updates should not require any code updates; major version updates, which, would
correspond to updated servlet spec support will likely require updates to 
src/main/java/org/fcrepo/server/config/webxml).
