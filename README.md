![](./www/dragon_stack.png)

This is the repository for the DRAGON Stack.

## Goal
This project aims to simplify the deployment of applications using an Oracle Autonomous Database (__Always Free Autonomous Transaction Processing__ (aka Converged Database), Autonomous JSON Database, Autonomous Transaction Processing, and Autonomous Data Warehouse).

## 30 seconds installation steps

### Download
Linux and OCI Cloud shell:
```
rm -f ./dragon-linux-x86_64-2.0.0
wget https://github.com/loiclefevre/dragon/releases/download/v2.0.0/dragon-linux-x86_64-2.0.0
chmod +x dragon-linux-*
```
Windows:
```
powershell wget https://github.com/loiclefevre/dragon/releases/download/v2.0.0/dragon-windows-x86_64-2.0.0.exe -OutFile dragon-windows-x86_64-2.0.0.exe
```
MAC OS:
```
curl -L -O https://github.com/loiclefevre/dragon/releases/download/v2.0.0/dragon-osx-x86_64-2.0.0
chmod +x dragon-osx-*
```

## Run
*(with the dragon.config file in the current directory)*

Linux and OCI Cloud shell:
```
$ ./dragon-linux-x86_64-2.0.0
```

Windows:
```
> dragon-windows-x86_64-2.0.0.exe
```

MAC OS:
```
$ ./dragon-osx-x86_64-2.0.0
```

Run example:

![Run](./www/dragon_cloud_shell.gif)

## Configuration file
The file *dragon.config* must contain the following information:

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
command to create the dragon-2.0.0-jar-with-dependencies.jar

Run
```
mvn verify
```
command to generate the native images

### Thanks
I would like to thank the people that contributed to this project:
- @paolobellardone: for building the MAC OS native image
- @mmanu-gh, and @davideburdese: for reporting bugs
- Jon R.: for creating the DRAGON logo :)
- @tmcginn, and @kaymalcolm: for the motivation :) 
