#!/bin/bash

# Must be run from project directory.

VERSION=$(tr -d '\n' < ./src/main/resources/version.txt)
echo "haveno-pricenode version = $VERSION"
JAR="./build/libs/haveno-pricenode-$VERSION.jar"
echo "haveno-pricenode jar = $JAR"

while true
do
echo `date`  "(Re)-starting haveno-pricenode"
java -jar $JAR 2 2
echo `date` "Node terminated unexpectedly!!"
sleep 3
done
