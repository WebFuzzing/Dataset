keytool -genkey \
  -dname "cn=CLIENT_APP_01" \
  -alias truststorekey \
  -keyalg RSA \
  -keystore ./client-truststore.p12 \
  -keypass ${KEYSTORE_PASSWORD} \
  -storepass ${TRUSTSTORE_PASSWORD} \
  -storetype pkcs12
