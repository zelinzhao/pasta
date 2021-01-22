#!/bin/bash
root=$(pushd $(dirname "$BASH_SOURCE[0]") > /dev/null && pwd -P && popd > /dev/null)

example=$1
examplePath=$root/$example
logTar=$examplePath/.pasta
dpgTar=$logTar/dpg


if [ -z "$example" ]; then
    echo "No example specified"
    exit
fi
if [ ! -d "$examplePath" ]; then
    echo "Not a correct example name"
    exit
fi

pastaPath=$(realpath $root/../target/pasta-*-full.jar)
if [ ! -f "$pastaPath" ]; then
    echo "Please build PASTA before using it"
    exit
fi
dpgFile=$(realpath $dpgTar/javelus.xml)
pcFile=$(realpath $logTar/config.xml)
if [ ! -f "$dpgFile" ] || [ ! -f "$pcFile" ]; then
    echo "Please init $example before distilling gadgets"
    exit
fi

java -jar $pastaPath synthesizer -u $dpgFile -pc $pcFile
