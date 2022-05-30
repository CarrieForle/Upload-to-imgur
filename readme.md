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

    javac ..\src\module-info.java ..\src\Main.java  
    jar -c -v -f ..\build.jar -e com.carrieforle.uploadtoimgur.Main -C ..\out

To execute the jar

    java -jar ..\build.jar