#!/usr/bin/env sh

# Intentar encontrar Java
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Ejecutar Gradle pasándole todos los argumentos de la compilación
exec "$JAVACMD" "-version"
