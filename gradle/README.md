# How to upgrade the Gradle version

Visit the [Gradle website](https://gradle.org/releases) and decide the:

 - desired version
 - desired distribution type
 - what is the sha256 for the version and type chosen above

Adjust the following command with tha arguments above and execute it twice:

```asciidoc
$ ./gradlew wrapper --gradle-version 7.4.2 \
    --distribution-type bin \
    --gradle-distribution-sha256-sum 29e49b10984e585d8118b7d0bc452f944e386458df27371b49b4ac1dec4b7fda
```

The first execution should automatically update:

- `bisq-pricenode/gradle/wrapper/gradle-wrapper.properties`

The second execution should then update:

- `bisq-pricenode/gradle/wrapper/gradle-wrapper.jar`
- `bisq-pricenode/gradlew`
- `bisq-pricenode/gradlew.bat`

The four updated files are ready to be committed.
