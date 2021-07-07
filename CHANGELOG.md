# v2.2.0 - 2021.07.07
## New features
- added support for GraphStudio and Oracle Machine Learning for the database user 
- added configuration checks against tenancy, compartment, and user ocid ensuring the values start with the expected patterns
- added Maven 3.X requirement and installation steps
  
## Fixes
- now database password is checked as it must not contain the database username or admin
- SSH key passphrase is checked to ensure it doesn't contain any hashtag
- object storage bucket existence is checked across the whole tenant to ensure there is no duplication error


# v2.1.1 - 2021.05.31
## New features
- added support for microservice Stacks
- added support for **Overrides** (allow to override a base Stack template)
  - added LiveLabs #2 (-cra#lab2) override: get a React-Table to display JSON documents
  - added micro-service override that loads random JSON documents into Oracle Autonomous databases (-cms#json-po-generator)
- added DRAGON based LiveLabs link in the README.md file
- added support for Linux on ARM processors

## Fixes
- fixed private key configuration when local path is being used (specifically for data loading from stacks)


# v2.1.0 - 2021.05.26
## New features
- added support for Oracle JavaScript Extension Toolkit with the -create-jet-app command line option - huge thanks to Paolo Bellardone!
- added support for Autonomous Database 21c
- added support for Autonomous JSON Database Always Free
- added support for Autonomous Application Express (APEX) Always Free
- added support for Autonomous JSON Database
- added support for Autonomous Application Express (APEX)
- added support for Oracle Cloud Infrastructure Limits
  - helps checking the remaining number of Always Free Database available for a given tenant
- upgraded development infrastructure to GraalVM 21.1.0
- upgraded Node.js v14 version for React based frontend
- upgraded React Scripts to v4.0.3
- upgraded Axios to v0.21.1
- improved local configuration with Graph Studio URL, APEX and ORDS version

## Fixes
- fixed stack environment requirements
  - GraalVM and Node.js versions
  - tar command
  - Node.js path for Windows
- fixed SODA collection creation using SODA for REST


# v2.0.8 - 2020.12.08
## New features
- added OCI group and policies information in case you encounter related issue
- added -loadcsv capability: loads a CSV file into a table, if the table doesn't exist, the table DDL is deduced from the actual CSV content; note that the header is mandatory
- added database_tables parameter for dragon.config file to list CSV files to be loaded in
- improved error message in case of OCI authentication problem


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

