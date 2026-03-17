openssl req -x509 -nodes -days 3650 -newkey rsa:2048 \
  -keyout projector.key \
  -out projector.crt \
  -subj "/C=CN/ST=JS/L=NJ/O=FH/CN=localhost"

openssl pkcs12 -export \
  -out projector.p12 \
  -inkey projector.key \
  -in projector.crt \
  -name projector \
  -passout pass:projector@2026

