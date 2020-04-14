#!/bin/sh
if [ -z "$CONFIG_PASSWORD" ] && [ ! -z "$CONFIG_PASSWORD_FILE" ] && [ -e $CONFIG_PASSWORD_FILE ]; then
  export CONFIG_PASSWORD="$(cat $CONFIG_PASSWORD_FILE)"
fi
if [ -z "$ACTUATOR_USER_PASSWORD" ] && [ ! -z "$ACTUATOR_USER_PASSWORD_FILE" ] && [ -e $ACTUATOR_USER_PASSWORD_FILE ]; then
  export ACTUATOR_USER_PASSWORD="$(cat $ACTUATOR_USER_PASSWORD_FILE)"
fi
if [ -z "$ADMIN_USER_PASSWORD" ] && [ ! -z "$ADMIN_USER_PASSWORD_FILE" ] && [ -e $ADMIN_USER_PASSWORD_FILE ]; then
  export ADMIN_USER_PASSWORD="$(cat $ADMIN_USER_PASSWORD_FILE)"
fi
java -Djava.security.egd=file:/dev/./urandom -jar /opt/app.jar
