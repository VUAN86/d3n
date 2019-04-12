RSA 2048bit certificate (key.cer)
Passphrase: f4mkey

Generate keystore for Java test services:
keytool -genkey -alias ascendro.de -keyalg RSA -sigalg SHA256withRSA -keystore keystore.jks -keypass f4mkey -storepass f4mkey -validity 3652 -dname "cn=Test User, ou=Ascendro, o=Ascendro, l=Germany, st=Germany, c=DE"

Generate keystore for test client (will be known to test services and vice versa):
keytool -genkey -alias test_client.de -keystore test_client.jks -keyalg RSA -sigalg SHA256withRSA -keypass f4mkey -storepass f4mkey -validity 3652 -dname "cn=Test Client, ou=Ascendro, o=Ascendro, l=Germany, st=Germany, c=DE"

:: Generate keystore for test client unknown to test services (with knowledge of services inside client):
keytool -genkey -alias unknown_client.de -keystore unknown_client.jks -keyalg RSA -sigalg SHA256withRSA -keypass f4mkey -storepass f4mkey -validity 3652 -dname "cn=Unknown Test Client, ou=Ascendro, o=Ascendro, l=Germany, st=Germany, c=DE"

:: Generate keystore for client known to test services who doesn't recognize services:
keytool -genkey -alias misconfigured_client.de -keystore misconfigured_client.jks -keyalg RSA -sigalg SHA256withRSA -keypass f4mkey -storepass f4mkey -validity 3652 -dname "cn=Misconfigured Test Client, ou=Ascendro, o=Ascendro, l=Germany, st=Germany, c=DE"

:: Export all certificates for importing into according keystores 
keytool -export -alias ascendro.de -file test_user.crt -keystore keystore.jks -storepass f4mkey
keytool -export -alias test_client.de -file test_client.crt -keystore test_client.jks -storepass f4mkey
keytool -export -alias unknown_client.de -file unknown_client.crt -keystore unknown_client.jks -storepass f4mkey
keytool -export -alias misconfigured_client.de -file misconfigured_client.crt -keystore misconfigured_client.jks -storepass f4mkey

:: Import into keystores
keytool -import -trustcacerts -alias test_client.de -file test_client.crt -keystore keystore.jks -storepass f4mkey -noprompt
keytool -import -trustcacerts -alias misconfigured_client.de -file misconfigured_client.crt -keystore keystore.jks -storepass f4mkey -noprompt

keytool -import -trustcacerts -alias ascendro.de -file test_user.crt -keystore test_client.jks -storepass f4mkey -noprompt

keytool -import -trustcacerts -alias ascendro.de -file test_user.crt -keystore unknown_client.jks -storepass f4mkey -noprompt


:: Import onesignal.com into Java test services:
keytool -import -trustcacerts -alias onesignal.com -file onesignal.crt -keystore keystore.jks -storepass f4mkey -noprompt

:: Clean-up
del misconfigured_client.crt
del test_client.crt
del test_user.crt
del unknown_client.crt
del onesignal.crt

:: Verify contents of keystore
keytool -list -keystore keystore.jks -storepass f4mkey
