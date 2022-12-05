# How to upgrade the Gradle version

Visit the [Gradle website](https://gradle.org/releases) and decide the:

 - desired version
 - desired distribution type
 - what is the sha256 for the version and type chosen above

Adjust the following command with tha arguments above and execute it twice:

```asciidoc
$ ./gradlew wrapper --gradle-version 7.5 \
    --distribution-type bin \
    --gradle-distribution-sha256-sum cb87f222c5585bd46838ad4db78463a5c5f3d336e5e2b98dc7c0c586527351c2
```

The first execution should automatically update:

- `haveno-pricenode/gradle/wrapper/gradle-wrapper.properties`

The second execution should then update:

- `haveno-pricenode/gradle/wrapper/gradle-wrapper.jar`
- `haveno-pricenode/gradlew`
- `haveno-pricenode/gradlew.bat`

The four updated files are ready to be committed.
