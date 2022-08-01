#!/bin/bash

# Must be run from project directory.

VERSION=$(tr -d '\n' < ./src/main/resources/version.txt)
echo "bisq-pricenode version = $VERSION"
JAR="./build/libs/bisq-pricenode-$VERSION.jar"
echo "bisq-pricenode jar = $JAR"

while true
do
echo `date`  "(Re)-starting bisq-pricenode"
java -jar $JAR 2 2
echo `date` "Node terminated unexpectedly!!"
sleep 3
done
