# DRAGON Stack
This is the repository for the Oracle DRAGON Stack.

## Goal
This project aims to simplify the deployment of applications using an Oracle Autonomous Database Always Free.

## Installation

### Build from sources
Run 
```
mvn package
```
command to create the dragon-1.0.1-jar-with-dependencies.jar

Run
```
mvn verify
```
command to generate the native images

### Download
Windows:
```
powershell wget https://github.com/loiclefevre/dragon/releases/download/v1.0.1/dragon-windows-x86_64-1.0.1.exe
```
Linux
```
wget https://github.com/loiclefevre/dragon/releases/download/v1.0.1/dragon-linux-x86_64-1.0.1
```
macOS
```
curl -L -O https://github.com/loiclefevre/dragon/releases/download/v1.0.1/dragon-osx-x86_64-1.0.1
```

## Execution
Run 
```
java -jar dragon-1.0.1-jar-with-dependencies.jar
```
or
```
dragon-windows-x86_64-1.0.1.exe
```
or
```
dragon-linux-x86_64-1.0.1
```
or
```
dragon-osx-x86_64-1.0.1
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
region=eu-frankfurt-1
auth_token=<authentication token>
database_password=5uPeR_5tRoNg_PaSsWoRd
collections=<list of comma separated collection name(s)>
```
