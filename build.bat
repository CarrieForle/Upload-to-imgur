javac -d out src\*
jar -c -v -f build\main.jar -e com.carrieforle.uploadtoimgur.Main -C out .