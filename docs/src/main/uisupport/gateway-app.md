# ESW Gateway App

A command line application that facilitates starting ESW Gateway Server

## Prerequisite

- Location server should be running.
- CSW AAS should be running.

## Running esw gateway server using Coursier

### 1. Add TMT Apps channel to your local Coursier installation using below command

Channel needs to be added to install application using `cs install`

For developer machine setup,

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

For production machine setup,

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

### 2. Install gateway-server app

Following command creates an executable file named gateway-server in the default installation directory.

```bash
cs install gateway-server:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    gateway-server:<version | SHA>
```
Note: If you don't provide the version or SHA in above command, `gateway-server` will be installed with the latest tagged binary of `esw-gateway-server`

### 3. Run gateway server app

Once gateway-server is installed, one can simply run gateway-server by executing start command

Start command supports following arguments:

 * `--port` , `-p` : Optional argument: HTTP server will be bound to this port. If a value is not provided, port will be picked up from configuration
 * `-l`, `--local` : optional argument (true if config is to be read locally or false if from remote server) default value is false
 * `-c`, `--commandRoleConfigPath` : specifies command role mapping file path which gets fetched from config service or local file system based on --local option
 * `-m`, `--metrics` : optional argument: If true, enable gateway metrics. If not provided, default value is false and metrics will be disabled


@@@notes
On starting gateway server app, it will be registered in location service as `ESW.EswGateway` as HttpRegistration. This prefix is picked up
from application.conf file
@@@

This command starts Gateway Server.

Example 1:
```bash
//cd to installation directory
cd /tmt/apps

// run gateway server with provided port, local command role config file and with metrics enabled
./gateway-server start -p 8090 -l -c command-role-mapping.conf -m
```

Example 2:
```bash
//cd to installation directory
cd /tmt/apps

// run gateway server with remote command role config file
./gateway-server start -c command-role-mapping.conf
```

@@@notes
Refer supported arguments section or `./gateway-server start --help` for starting gateway server with specific arguments
@@@

## Setting the default log level

The default log level for any component is specified in the `application.conf` file of the component.
Use the java -J-D option to override configuration values at runtime.  For log level, the format is:

```
-J-Dcsw-logging.component-log-levels.<Subsystem>.<ComponentName>=<LEVEL>
```

For example, using the example above:

```bash
//cd to installation directory
cd /tmt/apps

// run sequence manager
./gateway-server -J-Dcsw-logging.component-log-levels.ESW.EswGateway=TRACE start -p 8090 -l -c command-role-mapping.conf
```
