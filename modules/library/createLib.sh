#!/bin/bash

root=$(pushd $(dirname "$BASH_SOURCE[0]") > /dev/null && pwd -P && popd > /dev/null)

library=$(realpath $root/../../.library)
if [ ! -f "$library" ]; then
    touch $library
fi

JUNIT_JAR=$(realpath $root/../../target/lib/junit-*.jar)
VERIFY_JAR=$(realpath $root/../../target/pasta-*-full.jar)
HAMCREST_JAR=$(realpath $root/../../target/lib/hamcrest-core*.jar)
XSTREAM_LIB=$(realpath $root/../xstream/xstream/alllib)
XSTREAM_JAR=$(realpath $root/../xstream/xstream/target/xstream-1.4.11.1.jar)
AGENT_JAR=$(realpath $root/../agent/target/agent-*.jar)

echo "" > $library
{
    echo "export JUNIT_JAR=$JUNIT_JAR"
    echo "export HAMCREST_JAR=$HAMCREST_JAR"
    echo "export XSTREAM_JAR=$XSTREAM_JAR"
    echo "export XSTREAM_LIB=$XSTREAM_LIB"
    echo "export AGENT_JAR=$AGENT_JAR"
    echo "export VERIFY_JAR=$VERIFY_JAR"
} >> $library