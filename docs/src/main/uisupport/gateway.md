# User Interface Gateway

This is provided to give access to all CSW and ESW services and components from browser-based user interfaces.

## ESW Gateway with authentication and authorization

Esw Gateway is accessible to public network and exposes API endpoints through Http interface, hence we need to
protect its endpoints from unauthorized access.
  
### Protection on Commands service endpoints on Gateway 
 
There are commands which are more restrictive which need eng or above role and some commands which just need user
level role. Also, these more restrictive eng commands need a fine-grained control mechanism so that they can be
safely executed by authorized person having specific role at subsystem level. To achieve this we need to have a
role hierarchy at subsystem level along with a config table containing the mapping between more restrictive
commands and these roles.
     

#### Role Hierarchy

![Role Hierarchy](../images/gateway/role-hierarchy.png)

This type of role hierarchy is created in Keycloak as one time setup.
As per this hierarchy there should be three roles present for each subsystem which are composed in specific order.

* E.g. TCS-admin -> TCS-eng -> TCS-user. 
    * When you assign a user TCS-eng role, keycloak will automatically add TCS-user role to that user
    * When you assign a user TCS-admin role, keycloak will automatically add TCS-eng and TCS-user role to that user

Also, there are three special roles. OSW-admin, OSW-eng and OSW-user which are composed of all respective subsystem
 level roles. 

* E.g. When you assign a user OSW-eng role, keycloak will automatically add roles TCS-eng, APS-eng and so on to that
user and these roles will automatically add their respective lower level roles TCS-user, APS-user and so on
 
#### Examples:

![User Roles](../images/gateway/user-roles.png)

#### Command Role Mapping
Below shown are example entries in config table with commands and roles who can execute those commands.

```
IRIS.filter.wheel.startExposure: [IRIS-user, APS-eng]
IRIS.filter.wheel.stopExposure: [IRIS-user, APS-eng]
```

We need to create a config containing role mapping entries like shown above and use it when starting esw-gateway server. 

### Protection on Sequencer endpoints on Gateway  

On protected endpoints of sequencer commands in esw-gateway, {subsystem}-user role check is performed. 

* Subsystem is obtained from componentId
* E.g. If current sequence to be executed is for esw.primary then user should have minimum ESW-user role.

## Sample Requests

#### Request without auth token
```http request
curl --location --request POST 'http://<host>:<port>/post-endpoint' \
--header 'Content-Type: application/json' \
--data-raw '{
    "_type": "ComponentCommand",
    "componentId": {
        "prefix": "IRIS.filter.wheel",
        "componentType": "hcd"
    },
    "command": {
        "_type": "Submit",
        "controlCommand": {
            "_type": "Setup",
            "source": "CSW.ncc.trombone",
            "commandName": "startExposure",
            "maybeObsId": [
                "obsId"
            ],
            "paramSet": []
        }
    }'
```

#### Request with auth token
```http request
curl --location --request POST 'http://<host>:<port>/post-endpoint' \
--header 'Content-Type: application/json' \
--header 'Authorization: <bearer token> \
--data-raw '{
    "_type": "SetLogLevel",
    "componentId": {
        "prefix": "CSW.ncc.trombone",
        "componentType": "HCD"
    },
    "level": "ERROR"
}'
```


## Gateway Technical Design

See @ref:[ESW Gateway Technical Documentation](../technical/gateway-tech.md).

