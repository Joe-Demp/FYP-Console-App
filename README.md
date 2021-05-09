# MEC Client Console App

This application serves as a sample client for the MEC Framework.

## Build

This application was built with Apache Maven 3.6.3 and Java version 13.0.1. The following is a step by step guide for building the applciation.

1. Navigate to the MEC Framework project. In that project's root directory run `mvn install -pl Core`.
2. Navigate back to the root directory of this project and run `mvn package`.

## Run

1. Navigate to the __target__ directory.
2. Now run the following command:
```
java -jar console-app-1.0-SNAPSHOT-jar-with-dependencies.jar -o <orchestrator WebSocket URI>
``` 
Where `<orchestrator WebSocket URI>` is replaced with the URI pointing to the MEC Framework Orchestrator's WebSocket Server e.g. `ws://csi420-01-vm1.ucd.ie`.
