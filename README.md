# Indigo Monitoring Framework

The Monitoring Framewor is a set of tools which allow performing several monitoring operations in the platform resulting from the INDIGO-Datacloud project (https://www.indigo-datacloud.eu/). The Monitoring Framework is based on Zabbix, as the collector of the monitoring information coming from different sources, due to its maturity, its community support and its flexibility for different environments.

The Monitoring Framework is divided in several main parts:
* The Zabbix server (with the corresponding configuration and some support scripts);
* The Zabbix wrapper, created for enabling a REST API for Zabbix;
* Several probes, with different monitoring purposes (OCCI, Heapster, etc.).

This repository contains the supporting scripts for the Zabbix server (in order to perform automatic backups of the Zabbix database and configuration), the wrapper to be deployed with Zabbix (as a way to facilitate integration) and the probes released in the first version: a probe for monitoring OCCI interfaces of Infrastructure Providers and a probe for monitoring the Kubernetes cluster where the Indigo platform is deployed (by means of the Heapster tool).

1. Zabbix Scripts
=================

1.1 Main Features
-----------------


1.2 Pre-Requisites
------------------

1.3 Installation
----------------

1.4 Configuration
----------------- 

2. Zabbix Wrapper
=================

2.1 Main Features
-----------------
The first release of Zabbix Wrapper provides a Restful version of zabbix (natively JSON-RPC 2.0 protocol) APIs and potentially allows to develop a wrapper for another product APIs behaving as an adapter.
For indigo project purposes is meant to be the middle layer between Monitoring Framework and zabbix so that it can: 

* Create a host on zabbix platform (corresponding to, from Indigo point of view, a specific Cloud Provider)
* Get information about hosts (in indigo corresponding to specific providers)
* Get information about hostgroups (in indigo groups of providers)
* Get information about metrics based on configuration setup on zabbix

All these information are just returned in form of a REST response API which wrap zabbix ones.

2.2 Pre-Requisites
------------------
In order to get information and successfully monitor a specific cloud provider it has to be both registered on zabbix (via "create host" wrapper API) and there must be a zabbix agent installed and properly configured (for communicate with zabbix server) on board of a machine otherwise an exception will be thrown.


2.3 Installation
----------------

When having the war at disposal starting from a clean VM with Ubuntu install the docker manager:
```
sudo apt-get update
```
```
sudo apt-get install wget
```
```
sudo wget -qO- https://get.docker.com/ | sh
```

Install the application server (Wildfly 9.x) right from directory into which there is the docker file for giving the proper instructions and deploy the webapp
```
docker build -t indigodatacloud/zabbixwrapper .
```
```
docker logs -f `sudo docker run --name zabbixwrapper -h zabbixwrapper -p 80:8080 -d indigodatacloud/zabbixwrapper`
```

The deploy will be successfull if the endpoints written in the property file are correct and the wrapper can reach the server itself

#### In case wrapper is not a war --> Compile the code

To compile the project you need to be in the same folder as the `pom.xml` file and type:
```
mvn clean install -DskipTests
```
This command compiles the code and skip the tests. If you want to compile the code running the tests too you can use:
```
mvn clean install
```

At compilation completed, the `MonitoringPillar.war` file will be put inside the `target` folder.

#### Build the Docker image

The generated war must then be placed in the docker folder.

You can build the docker image with the command
```
docker build -t indigodatacloud/zabbixwrapper /path/to/the/docker/folder
```

2.4 Configuration
----------------- 
This project has been created with maven 3.3.3 and Java 1.8. Maven will take care of downloading the extra dependencies needed for the project but this project dependes on im-java-api also. To run the warpper you need docker and a MySQL Server on your machine. See next section to have details.

3. Zabbix Probes
================

3.1 Main Features
-----------------
The first release of the Monitoring Framework provides two probes for monitoring concrete aspects of the Indigo Platform:
* A OCCI probe, which checks whether the OCCI API exposed by an Infrastructure Provider works as expected;
* A Heapster probe, which retrieves information about the containers and pods running in a Kubernetes cluster.

In the case of the OCCI probe, the list of available providers is retrieved and, for each OCCI API available, a VM is creted, inspected and deleted, as a way to confirm that the main operations to be done are working as expected in the provider under evaluation. The probe is able to monitor several providers concurrently and it sends all the gathered metrics to the Zabbix server collecting all the information.

The Heapster probe, on the other hand, access to the Heapster API in order to list the pods and the containers available per pod, retrieving several metrics at the pod and container level, since they are complementary. These metrics, later on, are sent to the Zabbix server.

3.2 Pre-Requisites
------------------
Each probe has different requirements, since they rely on existing infrastructure to be monitored. Not fulfilling these requisites will have a negative impact in the execution of the probes.

The OCCI Probe has the following requisites:
* It requires a CMDB available, so it will be possible to retrieve the list of available providers. Having no access to a CMDB means that the probe will not be able to retrieve a list of providers to monitor, therefore not doing anything;
* It requires a Zabbix agent already installed, since the probe needs to run scripts provided by the Zabbix agent in order to send the metrics to the Zabbix server;
* It requires providers exposing OCCI APIs, otherwise, it will not be possible to monitor anything.

In the case of the Heapster probe, the requirements are the following:
* It is necessary to have a Heapster deployed in the corresponding Kubernetes cluster to be monitored, since the metrics are obtained from its APIs;
* It requires a Zabbix agent already installed, since the probe needs to run scripts provided by the Zabbix agent in order to send the metrics to the Zabbix server.

Since the implementation of the probes is in Java, both of them require, at least, a Java7 JVM to be already installed.

3.3 Installation
----------------


3.4 Configuration
----------------- 
The probes require different parameters to be configured in order to enable their operation.

The OCCI probe requires to modify the occiprobe.properties file in order to set the following parameters:
*openstack.user - Set here the user to be used for accessing OCCI APIs
*openstack.password - Set here the password to be used for accessing the OCCI APIs
*java.keystore - Set here the full location of the security certificates keystore
*zabbix.ip - Provide the IP address of the Zabbix server where metrics will be sent
*zabbix.sender.location - Configure the location where the Zabbix agent was installed, indicating the zabbix sender path
*cmdb.location - Provide the full URL of the CMDB component, providing the information about the available providers

The Heapster probe, on the other hand, requires the heapsterprobe.properties file to be adapted:
*heapster.url - Provide the URL where the Heapster API is available
*java.keystore - Set here the full location of the security certificates keystore
*zabbix.ip - Provide the IP address of the Zabbix server where metrics will be sent
*zabbix.sender.location - Configure the location where the Zabbix agent was installed, indicating the zabbix sender path

3.5 Potential Issues
--------------------
In the case of providers requiring some communication using SSL, if the provider certificate is not signed by a known entity, the JVM may throw exceptions. In such case, it is necessary to register the corresponding certificate with the following command:

keytool -importcert -trustcacerts -alias infnkeystone -file infnkeystone.cer -keystore "%JAVA_HOME%/jre/lib/security/cacerts"