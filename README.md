# DRAGON Stack
This is the repository for the Oracle DRAGON Stack.

## Goal
This project aims to simplify the deployment of applications using an Oracle Autonomous Database Always Free.

## Installation
Run 
```
mvn package
```
command to create the dragon-1.0.0-jar-with-dependencies.jar

## Execution
Run 
```
java -jar dragon-1.0.0-jar-with-dependencies.jar
```
or
```
dragon-x64-1.0.0.exe
```
with the config.txt file in the current directory.

![Run](/www/dragon.gif)

## Configuration file
The file config.txt must contain the following information:

```
[DEFAULT]
user=ocid1.user.oc1..aa...
fingerprint=e1:...
key_file=<file path to your private key>
tenancy=ocid1.tenancy.oc1..aa...
compartment_id=ocid1.compartment.oc1..aa...
region=EU_FRANKFURT_1
auth_token=<authentication token>
database_password=5uPeR_5tRoNg_PaSsWoRd
collections=<list of comma separated collection name(s)>
```
