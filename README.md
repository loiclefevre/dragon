# DRAGON Stack
This is the repository for the DRAGON Stack.

## Goal
This project aims to simplify the deployment of applications using an Oracle Autonomous Database (__Always Free Autonomous Transaction Processing__ (aka Converged Database), Autonomous JSON Database, Autonomous Transaction Processing, and Autonomous Data Warehouse).

## 30 seconds installation steps

### Download
Linux and OCI Cloud shell:
```
wget -N https://github.com/loiclefevre/dragon/releases/download/v1.0.2/dragon-linux-x86_64-1.0.2
chmod +x dragon-linux-*
```
Windows:
```
powershell wget https://github.com/loiclefevre/dragon/releases/download/v1.0.2/dragon-windows-x86_64-1.0.2.exe -OutFile dragon-windows-x86_64-1.0.2.exe
```
MAC OS:
```
curl -L -O https://github.com/loiclefevre/dragon/releases/download/v1.0.2/dragon-osx-x86_64-1.0.2
chmod +x dragon-osx-*
```

## Run
*(with the config.txt file in the current directory)*

Linux and OCI Cloud shell:
```
$ ./dragon-linux-x86_64-1.0.2
```

Windows:
```
> dragon-windows-x86_64-1.0.2.exe
```

MAC OS:
```
$ ./dragon-osx-x86_64-1.0.2
```

Run example:

![Run](/www/dragon_cloud_shell.gif)

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
database_collections=<list of comma separated collection name(s)>
```

### Build from sources
Run 
```
mvn package
```
command to create the dragon-1.0.2-jar-with-dependencies.jar

Run
```
mvn verify
```
command to generate the native images
