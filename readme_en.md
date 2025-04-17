### README.md

# TCP Port Multiplexer

This is a TCP port multiplexer implemented based on Netty.

## Features

- Listens on an external port and distributes different types of traffic to different internal ports.

## Usage Instructions

1. Compile the project:
    ```bash
    mvn clean package
    ```

   After compilation, a `tar.gz` file will be generated in the `target` directory.

2. Extract the file:
   The directory structure after extraction is as follows:
    ```
    tcp-multiplexer
     bin
        start_for_linux.sh
        stop_for_linux.sh
        ...
     lib
     classes
         conf.yaml
         ...
     ...
    ```

   The `bin` directory contains startup and shutdown scripts, and the `classes` directory contains configuration files.
3. Start the service:
    ```bash
    ./bin/start_for_linux.sh --java_home /opt/jre # Specify the JRE path
    ```


## License

This project is open-sourced under the MIT License. See the `LICENSE` file for more details.