# IDM Interceptor
This IDM interceptor has the following features:
* Logging requests to connectors that can be queried later, all connector types are supported
* Dynamically replacing connector implementation to use the Dummy Connector

# Table of Contents
1. [Installation](#installation)
1. [Configuration](#configuration)

# Installation

Build this repo with `mvn clean install` and copy these files to your IDM bundle directory, eg: `openidm/bundle/`:

* dummy-connector-api-1.0.0.jar
* idm-osgi-interceptor-1.0.0.jar

Copy `dummy-connector-1.0.0.jar` to the IDM connectors directory, eg: `openidm/connectors/`.

Find these jars on maven and copy to your IDM bundle directory, eg: `openidm/bundle/`:

* org.apache.felix.dependencymanager.runtime-4.0.7.jar
* org.apache.felix.dependencymanager-4.6.0.jar

In `bin/launcher.json` make the following changes:

Add:
```json
                    "**/org.apache.felix.dependencymanager*.jar",
                    "**/idm-osgi-interceptor*.jar",
                    "**/dummy-connector-api-*.jar"
```

to the includes of `"start-level":4` and the excludes of the bundle between start level 9 and 11.

This diff shows the context.

```diff
--- launcher.orig.json  2020-07-02 10:47:06.000000000 +1000
+++ launcher.json       2020-09-04 11:30:57.659125000 +1000
@@ -20,41 +20,44 @@
             },
             {
                 "location":"bundle",
                 "includes":[
                     "**/openidm-security-jetty*.jar",
                     "**/openidm-jetty-fragment*.jar",
                     "**/openidm-quartz-fragment*.jar",
                     "**/openidm-httpclient-fragment*.jar",
                     "**/openidm-config*.jar",
                     "**/openidm-datasource*.jar",
                     "**/groovy-*.jar"
                 ],
                 "start-level":3,
                 "action":"install.start"
             },
             {
                 "location":"bundle",
                 "includes":[
                     "**/openidm-repo-jdbc*.jar",
                     "**/openidm-repo-opendj*.jar",
-                    "**/org.apache.felix.scr-*.jar"
+                    "**/org.apache.felix.scr-*.jar",
+                    "**/org.apache.felix.dependencymanager*.jar",
+                    "**/idm-osgi-interceptor*.jar",
+                    "**/dummy-connector-api-*.jar"
                 ],
                 "start-level":4,
                 "action":"install.start"
             },
             {
                 "location":"bundle",
                 "includes":[
                     "**/org.apache.felix.configadmin*.jar",
                     "**/org.apache.felix.fileinstall*.jar"
                 ],
                 "start-level":5,
                 "action":"install.start"
             },
             {
                 "location":"bundle",
                 "includes":[
                     "**/openidm-cluster*.jar",
                     "**/openidm-httpcontext-*.jar"
                 ],
                 "start-level":9,
@@ -66,41 +69,44 @@
                 "excludes":[
                     "**/openidm-system-*.jar",
                     "**/openidm-security-jetty*.jar",
                     "**/openidm-jaas-loginmodule-repo*.jar",
                     "**/openidm-jetty-fragment*.jar",
                     "**/openidm-quartz-fragment*.jar",
                     "**/openidm-cluster*.jar",
                     "**/openidm-config*.jar",
                     "**/openidm-datasource*.jar",
                     "**/openidm-repo-jdbc*.jar",
                     "**/openidm-repo-opendj*.jar",
                     "**/org.apache.felix.scr-*.jar",
                     "**/org.apache.felix.configadmin*.jar",
                     "**/org.apache.felix.fileinstall*.jar",
                     "**/org.apache.felix.log*.jar",
                     "**/openidm-httpcontext-*.jar",
                     "**/pax-web-jetty-bundle*.jar",
                     "**/openidm-scheduler*.jar",
                     "**/groovy-*.jar",
                     "**/openidm-httpclient-fragment*.jar",
-                    "**/org.apache.aries.spifly*.jar"
+                    "**/org.apache.aries.spifly*.jar",
+                    "**/org.apache.felix.dependencymanager*.jar",
+                    "**/idm-osgi-interceptor*.jar",
+                    "**/dummy-connector-api-*.jar"
                 ]
             },
             {
                 "location":"bundle",
                 "includes":[
                     "**/pax-web-jetty-bundle*.jar"
                 ],
                 "start-level":11,
                 "action":"install.start"
             },
             {
                 "location":"bundle",
                 "includes":[
                     "**/openidm-scheduler*.jar"
                 ],
                 "start-level":12,
                 "action":"install.start"
             }
         ],
         "default":{

```

Copy the connector events provisioner from `conf/provisioner.openicf-connectorevents.json` to your IDM `conf` directory.

# Configuration

In the provisioner config you can prevent a particular connector from being replaced with the Dummy Connector, by adding this configuration:

```json
    "dummyConnectorProperties" : {
        "bypass": true
    },
```

