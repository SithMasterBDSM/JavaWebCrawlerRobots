#!/bin/bash
cd src
javac -Dfile.encoding=UTF-8 -d ../classes -classpath ../lib/10_VitralLib_Desktop.jar:../lib/80_RobotsLib_Desktop.jar:../lib/commons-logging-1.2.jar:../lib/httpclient-4.4.1.jar:../lib/httpcore-4.4.1.jar:../lib/httpmime-4.4.1.jar:../lib/json-20141113.jar:../lib/mongo-java-driver-2.13.2.jar `find -L . -type f`
