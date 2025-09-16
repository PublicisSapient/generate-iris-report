#!/bin/bash

mvn exec:java -Dexec.args="useGui=true videoPath=$1 irisPath='../IRIS/bin/build/macos-release/example/IrisApp' pdfName=$2"
