#!/bin/bash
root=$(pushd $(dirname "$BASH_SOURCE[0]") > /dev/null && pwd -P && popd > /dev/null)
verifyPath=$(realpath $root/../../target/pasta-*-full.jar)
if [ ! -f "$verifyPath" ]; then
    echo "Please build PASTA before using it"
    exit
fi

# saner programming env: these switches turn some bugs into errors
set -o errexit -o pipefail -o noclobber -o nounset

# -allow a command to fail with !’s side effect on errexit
# -use return value from ${PIPESTATUS[0]}, because ! hosed $?
! getopt --test > /dev/null 
if [[ ${PIPESTATUS[0]} -ne 4 ]]; then
    echo 'I’m sorry, `getopt --test` failed in this environment.'
    exit 1
fi

LONGOPTS=pc:,jc:,tt:,help

if [[ $# -eq 0 ]]; then
    echo "Use --help to see help message"
    exit 0
fi

# -regarding ! and PIPESTATUS see above
# -temporarily store output to be able to check for errors
# -activate quoting/enhanced mode (e.g. by writing out “--options”)
# -pass arguments only via   -- "$@"   to separate them correctly
! PARSED=$(getopt -o '' -a --longoptions=$LONGOPTS --name "$0" -- "$@")
if [[ ${PIPESTATUS[0]} -ne 0 ]]; then
    # e.g. return value is 1
    #  then getopt has complained about wrong arguments to stdout
    exit 2
fi
# read getopt’s output this way to handle the quoting right:
eval set -- "$PARSED"

pc=- jc=- tt=-
# now enjoy the options in order and nicely split until we see --
while true; do
    case "$1" in
        --pc)
            pc="$2"
            shift 2
            ;;
        --jc)
            jc="$2"
            shift 2
            ;;
        --tt)
            tt="$2"
            shift 2
            ;;
        --help)
            echo "Usage: bash $0 [options]"
            echo "--pc          project config file required by pasta-verifier"
            echo "--jc          jars config file required by pasta-verifier"
            echo "--tt          test timeout in seconds"
            echo "--help        print this help message"
            exit 0
            ;;
        --)
            shift
            break
            ;;
        *)
            echo "Programming error"
            exit 3
            ;;
    esac
done

method=""
while read line
do
    if [ -z "$line" ]; then
        continue
    fi
    
    if [[ "$line" =~ ^\s*\/[*]+\/\s*$ ]]; then
        #transformer starts here
        if [ ! -z "$method" ]; then
            echo ""
            echo "------------------"
            echo "Test transformer:"
            # echo "$method"
            if [ "$tt" == "-" ]; then
                java -jar  $verifyPath verifier -pc $pc -jc $jc -m "$method"
            else
                java -jar  $verifyPath verifier -pc $pc -jc $jc -tt $tt -m "$method"
            fi
            echo "========================================="
            echo ""
            echo "========================================="
            method=""
        fi
        continue
    else
        echo "$line"
        if [ -z "$method" ]; then
            method="${line}%nn"
        else
            method="${method}${line}%nn"
        fi
    fi
done < "${1:-/dev/stdin}"

if [ ! -z "$method" ]; then
    #Extra test for the last transformer
    echo ""
    echo "------------------"
    echo "Test transformer:"
    # echo "$method"
    if [ "$tt" == "-" ]; then
        java -jar  $verifyPath verifier -pc $pc -jc $jc -m "$method"
    else
        java -jar  $verifyPath verifier -pc $pc -jc $jc -tt $tt -m "$method"
    fi    
    echo "========================================="
fi