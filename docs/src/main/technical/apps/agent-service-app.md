#Agent Service App

This application will start the AgentService's server.

## Protection on the Agent Service endpoints
To access any protected `AgentService` endpoints, `ESW-user` role is required.

## Prerequisite

 - Location server should be running.

## Running Agent Service App using Coursier

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

### 2. Install agent-service-app

Following command creates an executable file named agent-service-app in the default installation directory.

```bash
cs install agent-service-app:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    agent-service-app:<version | SHA>
```
Note: If you don't provide the version or SHA in above command, `agent-service-app` will be installed with the latest tagged binary of `esw-agent-service-app`

### 3. Run agent-service-app

Once agent-service-app is installed, one can simply run agent-service-app by executing start command

```bash
//cd to installation directory
cd /tmt/apps

// run agent service app
./agent-service-app start
```

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
./agent-service-app -J-Dcsw-logging.component-log-levels.ESW.agent_service=TRACE start
```
