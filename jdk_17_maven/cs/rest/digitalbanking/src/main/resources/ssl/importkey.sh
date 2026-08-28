keytool -import \
  -keystore ./client-truststore.p12 \
  -file ./proxy_cert.pem \
  -alias redis-cluster-crt
