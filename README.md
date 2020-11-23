![](./www/dragon_stack.png)
# From Zero to WOW in 5 minutes
![](./www/command-line-example.png)
- Generates the source code for a _pre-configured_ [REACT](https://reactjs.org/) frontend or for the [Spring-Boot](https://spring.io/projects/spring-boot) Petclinic web application 
- Provisions an autonomous backend ([Always Free autonomous database](#why-autonomous-database-for-developers) + REST Data Services)
- (optional) Loads your JSON data into your collection(s)

__... in 5 minutes.__

![React.js frontend](./www/react-logo.png) ![Spring Boot application](./www/spring-boot-logo.png) ![Autonomous Database](./www/adb-logo.png) ![Polyglot GraalVM](./www/graalvm-logo.png) ![REST Data Services](./www/ords-logo.png) ![Node.js](./www/nodejs-logo.png)

# 30 seconds installation steps

- Download [Linux](#linux-and-oci-cloud-shell) | [Windows](#windows) | [MAC OS](#mac-os)
  - Upgrade using the __-upgrade__ command (since v2.0.2)
- [Configure](#configure)
- [Run](#run)
- [Managed Stacks](#stacks)

[![DRAGON Stack - React Frontend / Autonomous Backend](https://img.youtube.com/vi/DzI9yyAiRjY/0.jpg)](https://www.youtube.com/watch?v=DzI9yyAiRjY)

## Download
The latest stable release is v2.0.7.

### Linux and OCI Cloud Shell
```
rm -f ./dragon-linux-x86_64-2.0.7
wget https://github.com/loiclefevre/dragon/releases/download/v2.0.7/dragon-linux-x86_64-2.0.7
chmod +x dragon-linux-*
```
Learn about [OCI Cloud shell](https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/cloudshellintro.htm).

### Windows
```
powershell wget https://github.com/loiclefevre/dragon/releases/download/v2.0.7/dragon-windows-x86_64-2.0.7.exe -OutFile dragon-windows-x86_64-2.0.7.exe
```
### MAC OS
```
curl -L -O https://github.com/loiclefevre/dragon/releases/download/v2.0.7/dragon-osx-x86_64-2.0.7
chmod +x dragon-osx-*
```

### Changelog
See the changelog [here](./CHANGELOG.md).

## Configure

The DRAGON Stack manager is driven by the command line arguments and the configuration file named *dragon.config* present in your __current directory__:

### CLI arguments

![](./www/help.png)

### Configuration file

If no *dragon.config* exist in the __current directory__, then by running the dragon stack CLI, you get a default configuration template to complete and write into the file *dragon.config*. It includes various links to Oracle Cloud Infrastructure documentation to get you started.

__REMARK__: Please notice that (as of now) only __one database__ can be provisioned for a given dragon.config file and hence for a given directory.

_New with v2.0.4_, using the __-create-keys__ parameter along the __-config-template__ command makes the whole process of generating API Keys pair a non-event!  

```
 # DEFAULT profile (case sensitive), you can define others: ASHBURN_REGION or TEST_ENVIRONMENT
 # You can choose a profile using the -profile command line argument
[DEFAULT]

 # OCID of the user connecting to Oracle Cloud Infrastructure APIs. To get the value, see:
 # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#five
user=ocid1.user.oc1..<unique_ID>

 # Full path and filename of the SSH private key (use *solely* forward slashes).
 # /!\ Warning: The key pair must be in PEM format. For instructions on generating a key pair in PEM format, see:
 # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#Required_Keys_and_OCIDs
key_file=<full path to SSH private key file>

 # Uncomment in the case your SSH private key needs a pass phrase.
# pass_phrase=<pass phrase to use with your SSH private key>

 # Fingerprint for the SSH *public* key that was added to the user mentioned above. To get the value, see:
 # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#four
fingerprint=<fingerprint associated with the corresponding SSH *public* key>

 # OCID of your tenancy. To get the value, see:
 # https://docs.cloud.oracle.com/en-us/iaas/Content/API/Concepts/apisigningkey.htm#five
tenancy=ocid1.tenancy.oc1..<unique_ID>

 # An Oracle Cloud Infrastructure region identifier. For a list of possible region identifiers, check here:
 # https://docs.cloud.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm#top
region=eu-frankfurt-1

 # OCID of the compartment to use for resources creation. to get more information about compartments, see:
 # https://docs.cloud.oracle.com/en-us/iaas/Content/Identity/Tasks/managingcompartments.htm?Highlight=compartment%20ocid#Managing_Compartments
compartment_id=ocid1.compartment.oc1..<unique_ID>

 # Authentication token that will be used for OCI Object Storage configuration, see:
 # https://docs.cloud.oracle.com/en-us/iaas/Content/Registry/Tasks/registrygettingauthtoken.htm?Highlight=user%20auth%20tokens
auth_token=<authentication token>

 # Autonomous Database Type:
 # - atpfree: Always Free Autonomous Transaction Processing (default)
 # - adwfree: Always Free Autonomous Data Warehouse
 # - ajd: Autonomous JSON Database
 # - atp: Autonomous Transaction Processing
 # - adw: Autonomous Data Warehouse
# database_type=atpfree

 # Uncomment to specify another database user name than dragon (default)
# database_user_name=<your database user name>

 # The database password used for database creation and dragon user
 # - 12 chars minimum and 30 chars maximum
 # - can't contain the "dragon" word
 # - contains 1 digit minimum
 # - contains 1 lower case char
 # - contains 1 upper case char
database_password=<database password>

 # Uncomment to ask for Bring Your Own Licenses model (doesn't work for Always Free and AJD)
# database_license_type=byol

 # A list of coma separated JSON collection name(s) that you wish to get right after database creation
# database_collections=

 # Path to a folder where data to load into collections can be found (default to current directory)
data_path=.
```


## Run

Example from OCI Cloud Shell (Linux):

![Run](./www/dragon_cloud_shell.gif)

*(you must have a valid dragon.config file in the current directory)*

Linux and OCI Cloud Shell:
```
$ ./dragon-linux-x86_64-2.0.7
```

Windows:
```
> dragon-windows-x86_64-2.0.7.exe
```

MAC OS:
```
$ ./dragon-osx-x86_64-2.0.7
```

### Loading JSON data

If you need to create JSON collections during the provisioning process, you may use the configuration file parameter __database_collections__ (see hereunder). If you also need to load existing JSON data into these collections, you may put your JSON documents in files having the same name as the collection name plus the .json extension. These files must be of JSON dump format with exactly one JSON document per line. No array, no comma separating the documents but carriage returns! __Your files will be loaded only if you ask for it using the -loadjson CLI argument__.  

To load JSON data as well as provisioning (Linux and OCI Cloud Shell):
```
$ ./dragon-linux-x86_64-2.0.7 -loadjson
```

To load JSON data as well as provisioning and finally create a React application (Linux and OCI Cloud Shell):
```
$ ./dragon-linux-x86_64-2.0.7 -loadjson -create-react-app myfrontend
```

### Destroying your database

To destroy your database (Linux and OCI Cloud Shell):
```
$ ./dragon-linux-x86_64-2.0.7 -destroy
```

## Stacks

### React frontend

As of v2.0.1, DRAGON can now generate stacks. The very first stack proposed is a [React](https://reactjs.org/) frontend.

![](./www/VSCode.png)

Giving this result in your browser:

![](./www/react-frontend.png)

#### Accessing ports on OCI Cloud Shell

For OCI Cloud Shell, you may use NGROK (free version) to allow access to your website deployed locally.

```
wget https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-linux-amd64.zip

unzip ngrok-stable-linux-amd64.zip

npm start &
```

For [React](https://reactjs.org/) frontend:
```
./ngrok http 3000
``` 

### Spring-Boot petclinic webapp

As of v2.0.3, DRAGON can now generate a [Spring-Boot](https://spring.io/projects/spring-boot) based stack including the well know petclinic web application.

![](https://cloud.githubusercontent.com/assets/838318/19727082/2aee6d6c-9b8e-11e6-81fe-e889a5ddfded.png)

#### Accessing ports on OCI Cloud Shell

For OCI Cloud Shell, you may use NGROK (free version) to allow access to your web application deployed locally.

```
wget https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-linux-amd64.zip

unzip ngrok-stable-linux-amd64.zip

java -jar target/*.jar &
```

For [Spring-Boot](https://spring.io/projects/spring-boot) web application:
```
./ngrok http 8080
``` 

## Why Autonomous Database for Developers?

Simple to use, it works, it is optimized already, __no__ administrative burdens, develop right away!

__[Converged](https://www.youtube.com/watch?v=yBWgb_oh39U)__, it means, you get the consistency of a _relational_ database, the _flexibility_ of a JSON database, the simplicity of _Machine Learning_ in the database, the location capabilities of a _spatial_ database, the power of a property _graph_ database, the indexing capabilities of a _full-text_ database, the _automatic elasticity_ as well as the costing model (always free version, pay by the second...) of a cloud native database, the _performance_ of the underlying infrastructure Exadata, the strongest _security_ of the database market, and the vast developer friendly ecosystem brought by Oracle Cloud Infrastructure.

 
Autonomous Database can be:
 - __[Always Free](https://signup.oraclecloud.com/?language=en)__ Autonomous Transaction Processing (ATP, aka Converged Database)
 - Autonomous JSON Database (AJD)
 - Autonomous Transaction Processing (ATP)
 - Autonomous Data Warehouse (ADW)


### Thanks
I would like to thank the people that contributed to this project:
- [Paolo Bellardone](https://github.com/paolobellardone): for building the MAC OS native image (and of course testing and reporting bugs)
- [Manu M.](https://github.com/mmanu-gh), and [Davide Burdese](https://github.com/davideburdese): for testing and reporting bugs
- [Jon Russel](https://github.com/jon-russell): for creating the DRAGON logo :)
- [Kay Malcolm](https://github.com/kaymalcolm), [T. McGinn](https://github.com/tmcginn), and [Kamryn V.](https://github.com/kamryn-v): for the energy, the motivation and the Live Labs!!! :)

![](./www/dragon-logo.png)
