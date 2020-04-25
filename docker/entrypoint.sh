#!/bin/sh
if [ -z "$CONFIG_USER" ] && [ ! -z "$CONFIG_USER_FILE" ] && [ -e $CONFIG_USER_FILE ]; then
  export CONFIG_USER="$(cat $CONFIG_USER_FILE)"
fi
if [ -z "$CONFIG_PASSWORD" ] && [ ! -z "$CONFIG_PASSWORD_FILE" ] && [ -e $CONFIG_PASSWORD_FILE ]; then
  export CONFIG_PASSWORD="$(cat $CONFIG_PASSWORD_FILE)"
fi
if [ -z "$ACTUATOR_USER_NAME" ] && [ ! -z "$ACTUATOR_USER_NAME_FILE" ] && [ -e $ACTUATOR_USER_NAME_FILE ]; then
  export ACTUATOR_USER_NAME="$(cat $ACTUATOR_USER_NAME_FILE)"
fi
if [ -z "$ACTUATOR_USER_PASSWORD" ] && [ ! -z "$ACTUATOR_USER_PASSWORD_FILE" ] && [ -e $ACTUATOR_USER_PASSWORD_FILE ]; then
  export ACTUATOR_USER_PASSWORD="$(cat $ACTUATOR_USER_PASSWORD_FILE)"
fi
if [ -z "$ADMIN_USER_NAME" ] && [ ! -z "$ADMIN_USER_NAME_FILE" ] && [ -e $ADMIN_USER_NAME_FILE ]; then
  export ADMIN_USER_NAME="$(cat $ADMIN_USER_NAME_FILE)"
fi
if [ -z "$ADMIN_USER_PASSWORD" ] && [ ! -z "$ADMIN_USER_PASSWORD_FILE" ] && [ -e $ADMIN_USER_PASSWORD_FILE ]; then
  export ADMIN_USER_PASSWORD="$(cat $ADMIN_USER_PASSWORD_FILE)"
fi
java -Djava.security.egd=file:/dev/./urandom -jar /opt/app.jar
