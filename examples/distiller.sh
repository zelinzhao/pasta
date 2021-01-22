#!/bin/bash

root=$(pushd $(dirname "$BASH_SOURCE[0]") > /dev/null && pwd -P && popd > /dev/null)

example=$1
examplePath=$root/$example
logTar=$examplePath/.pasta

if [ -z "$example" ]; then
    echo "No example specified"
    exit
fi

if [ ! -d "$examplePath" ]; then
    echo "Not a correct example name"
    exit
fi
if [ -d $logTar ]; then
    rm -rf $logTar
fi
echo "Init $example"


oldTar=$logTar/target/old
newTar=$logTar/target/new
testTar=$logTar/target/test

if [ ! -d $oldTar ]; then
    mkdir -p $oldTar
fi

if [ ! -d $newTar ]; then
    mkdir -p $newTar
fi

if [ ! -d $testTar ]; then
    mkdir -p $testTar
fi

junitPath=$(realpath $root/../target/lib/junit-*.jar)
pastaPath=$(realpath $root/../target/pasta-*-full.jar)
hamcrestPath=$(realpath $root/../target/lib/hamcrest-core*.jar)

if [ ! -f "$junitPath" ] || [ ! -f "$pastaPath" ] || [ ! -f "$hamcrestPath" ]; then
    echo "Please build PASTA before running any example"
    exit
fi

javac $examplePath/old/*.java -d $oldTar
javac $examplePath/new/*.java -d $newTar
javac $examplePath/test/*.java -cp $junitPath:$pastaPath:$oldTar -d $testTar

dpgPath=$(realpath $root/../modules/dpg/dist/run.sh)
if [ ! -f "$dpgPath" ]; then
    echo "Please build DPG before running any example"
    exit
fi

dpgTar=$logTar/dpg
if [ ! -d $dpgTar ]; then
    mkdir -p $dpgTar
fi

bash $dpgPath -o $oldTar -n $newTar -d $dpgTar
bash $dpgPath -o $oldTar -n $newTar -d $dpgTar -x

xstreamLib=$(realpath $root/../modules/xstream/xstream/alllib)
xstreamPath=$(realpath $root/../modules/xstream/xstream/target/xstream-1.4.11.1.jar)
agentPath=$(realpath $root/../modules/agent/target/agent-*.jar)

if [ ! -f "$xstreamPath" ] || [ ! -d "$xstreamLib" ] || [ ! -f "$agentPath" ]; then
    echo "Please build XStream and agent before running any example"
    exit
fi

cp -f $examplePath/config-default.xml $logTar/config.xml
cp -f $root/jars-default.xml $logTar/jars.xml

sed -i 's|<commitRoot>.*$|<commitRoot>'$examplePath'</commitRoot>|g' $logTar/config.xml

sed -i 's|<junitJar>.*$|<junitJar>'$junitPath'</junitJar>|g' $logTar/jars.xml
sed -i 's|<hamcrestJar>.*$|<hamcrestJar>'$hamcrestPath'</hamcrestJar>|g' $logTar/jars.xml
sed -i 's|<xstreamJar>.*$|<xstreamJar>'$xstreamPath'</xstreamJar>|g' $logTar/jars.xml
sed -i 's|<xstreamLibs>.*$|<xstreamLibs>'$xstreamLib'</xstreamLibs>|g' $logTar/jars.xml
sed -i 's|<agentJar>.*$|<agentJar>'$agentPath'</agentJar>|g' $logTar/jars.xml
sed -i 's|<verifyJar>.*$|<verifyJar>'$pastaPath'</verifyJar>|g' $logTar/jars.xml

echo "Distill gadgets for $example"

dpgFile=$(realpath $dpgTar/javelus.xml)
pcFile=$(realpath $logTar/config.xml)
if [ ! -f "$dpgFile" ] || [ ! -f "$pcFile" ]; then
    echo "Please init $example before distilling gadgets"
    exit
fi

java -jar $pastaPath distiller -u $dpgFile -pc $pcFile 

echo "Gadgets are written into $logTar/gadget"