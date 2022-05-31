# Upload to Imgur

A simple Java program to upload pictures to Imgur with their native API.

# Features

- Fast (native API used)
- Supported uploaded from
    - copied files
    - copied image data
    - drag and drop (batch files needed on windows 10)

# Build (create a jar)

The application is built under `JDK 17.0.3`, but `JDK 9+` should be fine. (Not tested)

    javac src\module-info.java src\Main.java
    jar -c -v -f build\main.jar -e com.carrieforle.uploadtoimgur.Main -C out .

To execute the jar

    java -jar build\main.jar
    
## jlink

The following snippets create a JRE to run in `jre`. (45.5MB)

    jlink -v --compress=2 -p out --add-modules uploadtoimgur --output jre