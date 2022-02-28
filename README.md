<div align="center">
  
# UKT diagnostics ID portlet

[![Build Maven Package](https://github.com/qbicsoftware/ukt-diagnostics-id-portlet/actions/workflows/build_package.yml/badge.svg)](https://github.com/qbicsoftware/ukt-diagnostics-id-portlet/actions/workflows/build_package.yml)
[![Run Maven Tests](https://github.com/qbicsoftware/ukt-diagnostics-id-portlet/actions/workflows/run_tests.yml/badge.svg)](https://github.com/qbicsoftware/ukt-diagnostics-id-portlet/actions/workflows/run_tests.yml)
[![CodeQL](https://github.com/qbicsoftware/ukt-diagnostics-id-portlet/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/qbicsoftware/ukt-diagnostics-id-portlet/actions/workflows/codeql-analysis.yml)
[![release](https://img.shields.io/github/v/release/qbicsoftware/ukt-diagnostics-id-portlet?include_prereleases)](https://github.com/qbicsoftware/ukt-diagnostics-id-portlet/releases)

[![license](https://img.shields.io/github/license/qbicsoftware/ukt-diagnostics-id-portlet)](https://github.com/qbicsoftware/ukt-diagnostics-id-portlet/blob/main/LICENSE)
![language](https://img.shields.io/badge/language-java-blue.svg)

 </div>
  
## Description
This portlet enables user from the UKT diagnostics department to request a new **patient/sample ID pair** and further, the creation of a new **sample ID** to an existing patient (requires a valid patient ID).

## How to Run

First compile the project and build an executable java archive:

```
mvn clean package
```

Note that you will need java 8.
The WAR file will be created in the /target folder:
```
|-target
|---ukt-diagnostics-id-portlet-<version>.war
|---...
```
The created war file can then be deployed on a portal system.

## How to Use 

Test environment
----------------

You can run the application locally in ``testing`` mode:

```
mvn clean jetty:run -Ptesting
```

The default configuration of the app binds to the local port 8080 to the systems localhost:

```
http://localhost:8080
```

#### Configuration

To run the application locally in ``testing`` mode a property file has to provided via ``/etc/openbis.properties``.
In this ``openbis.properties`` file the following properties have to be specified 


| Property    | Description                              | Default Value                      |
|-------------|------------------------------------------|------------------------------------|
| openbisURI  | Connection to API datasource             | https://openbis.domain.de/api/path |
| openbisuser | The user name for the datasource         | myuser                             |
| openbispw   | The password for the datasource          | mypassword                         |

## Process scheme
<img src="./figs/SOP_QBiC_Pathologie_ID_request.png" alt="Process overview">

License
-------

This work is licensed under the `MIT license <https://mit-license.org/>`_.

**Note**: This work uses the `Vaadin Framework <https://github.com/vaadin>`_, which is licensed under `Apache 2.0 <https://www.apache.org/licenses/LICENSE-2.0>`_.
