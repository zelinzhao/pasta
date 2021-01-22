#! /bin/bash

PWD=$( pushd $( dirname $BASH_SOURCE[0] ) >> /dev/null && pwd && popd >> /dev/null )
CP=${PWD}/dpg.jar

for JAR in ${PWD}/lib/*.jar
do
    CP=$CP:$JAR
done

java -cp $CP org.javelus.dpg.DynamicPatchGenerator -u
