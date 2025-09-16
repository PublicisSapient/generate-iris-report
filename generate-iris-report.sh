#!/bin/bash

if [ "$#" -ne "2" ]
then
    echo "Usage: $(basename "$0") <input-video> <output-pdf>"
    exit 1
fi

mvn exec:java -Dexec.args="useGui=false videoPath=$1 irisPath=../IRIS/bin/build/macos-release/example/IrisApp     pdfName=$2"
