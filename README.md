# FIX file parser

This example implements a very simple FIX parser, emphasis is on performance.

## Prerequisites

1. Maven 3.5 or newer
2. Java SE 8 or newer


## Build and run tests

```
mvn clean install
```

### Run the program with standard settings (as specified)

Run with default input file "example-fix-data.bin"
```
java -jar target/fixparser-1.0.jar
```

### Run with default settings, any input file
```
java -jar target/fixparser-1.0.jar FILENAME
```

### Run in max performance mode

To maximize performance we need to have a pretty big input file.
The supplied script 'generate-big-data-file.sh' wil generate a 314MB data file by 
appending the default data file many times to a new 'output' file.

```
./generate-big-data-file.sh
```

You should now have a pretty big file called 'data-314MB.txt'
The command below will try to parse the big file.
The additional parameters 0 1:
* 0, disables logging of each line
* 1, makes the program use a Fork Join pool to parse messages.

```
java -jar target/fixparser-1.0.jar data-314MB.txt 0 1
```

### Run in high perf mode with logging

To get max performance and logging at the same time, a random file appender should be used instead of a console logger.
To enable this, edit the src/main/resources/log4j2.xml and comment out console logger and comment in the file logger.
Only the random file appender should be active.
Then compile and run program. (with logging enabled and Fork Join pool)

```
mvn clean install
java -jar target/fixparser-1.0.jar data-314MB.txt 1 1
```