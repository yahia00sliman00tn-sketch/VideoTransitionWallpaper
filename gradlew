#!/bin/sh
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
MAX_FD="maximum"
die () { echo; echo "ERROR: $*"; echo; exit 1; } >&2
warn () { echo "$*"; } >&2
if [ "$APP_HOME" = "" ] ; then
    APP_HOME="`pwd -P`"
fi
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
exec gradle "$@"
