# Upload to Imgur

A simple Java program to upload pictures to Imgur with their native API.

## Features

- Fast (native API used)
- Small (5.75KB, unless you don't have JRE 12+ installed)
- Supported uploaded from
    - copied files
    - copied image data
    - drag and drop (batch files needed on windows 10)

## Build (create a jar)

The application is built and executed under [JDK Eclipse Termurim 17.0.3](https://adoptium.net/temurin/releases), but JDK 12+ (JRE 12+ for execution) should work as well. (Not tested)

    javac -d out src\module-info.java src\Main.java
    jar -c -v -f build\main.jar -e com.carrieforle.uploadtoimgur.Main -C out .

To execute the jar

    java -jar build\main.jar
    
## jlink

The following snippets create a JRE to run in `jre/`. (45.5MB)

    jlink -v --compress=2 -p out --add-modules uploadtoimgur --output jre

## Note

If the jar does not open a console on Windows, create a `batch` snippet with the following

    java -jar build\main.jar %*
