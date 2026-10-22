#!/usr/bin/env sh
set -eu
mvn clean package
printf '\nJAR generado en target/UnboundProtection-1.20.1.jar\n'
