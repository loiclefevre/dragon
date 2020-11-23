# v2.0.7 - 2020.11.23
## New features
- upgraded React based stacks to use react-scripts v4.0.1
- added links to documentation in ORDS.js
- display information about the possible upgrade version in the help
- allows to run DRAGON commands inside a stack folder using a specific redirect operator
- added stack environment requirements infrastructure
  - added JDK v11 requirements including manual installation steps
  - added Node.js v14.15.1 requirements including manual installation steps

# v2.0.6 - 2020.11.18
## Fixes
- creating stacks or loading JSON data is possible after the database has been provisioned

## New features
- stop and start the database
- using GraalVM 20.3.0 CE LTS (allowing for compression of Linux executable)

# v2.0.5 - 2020.11.13
## Fixes
- better error messaging for creation and termination of databases

## New features
- infrastructure for overriding React based stacks
- Always Free databases can now be deployed as Autonomous Transaction Processing (database_type=atpfree) or Autonomous Data Warehouse (database_type=adwfree)

# v2.0.4 - 2020.11.09
## Fixes
- key_file from dragon.config is now expanded in the case it starts with the ~ char (user home directory)
- MAC OS title colors should now be yellow, as well as on Windows when ran from VSCode terminal

## New feature
- Using the -create-keys from the -config-template command now generates the SSH key pairs (the config file is also filled properly with the path and fingerprint)

# v2.0.3 - 2020.11.06
## Fixes
- pass_phrase parameter for SSH key
- downloads now use the pre-defined proxy

## New features
- create the pre-configured Spring Boot Petclinic web application connected to your Autonomous database

# v2.0.2 - 2020.11.04
## New features
- renamed -load command to -loadjson
- you can now load JSON files containing JSON documents spreading several lines (no comma to separate them, no array)
- colors now working in Windows console (starting from Windows 10)
- updated create-react-app to React v4.0.0 (best to run with Node.js v14+)
- added -upgrade command to ease even more upgrading the DRAGON Stack Manager

# v2.0.1 - 2020.10.30
## Fixes
- A major bug fixed (thank you the EMEA team!!)

## New features
- More control of the dragon.config file
- The -load argument can be used to load data later on

# v2.0.0 - 2020.10.29
## New features
- The v2.0.0 marks the beginning of DRAGON being able to manage application stacks.
The very first stack allows creating a REACT frontend that connects to an Autonomous database.
Please use the new -create-react-app [name] flag and follow the instructions...

# v1.0.2 - 2020.10.14
## New features
- Load local JSON files into SODA collections
- Locally persist database information access
- Create custom user, other than dragon
- Manage ATP/ADW/AJD (not Always Free)
- Configure manual backup for not Always Free database

# v1.0.1 - 2020.09.30
## Fixes
- minor glitches

# v1.0.0 - 2020.09.26
The first release of the DRAGON stack manager.

