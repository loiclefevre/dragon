Success! [1mOverrided[0m <name> with [1m<override>[0m at <path>
Inside that directory, you can run several commands:

  mvn verify
    Use Maven to build and package the micronaut microservice.

  java -Xmx2G -Xms2G -jar <applicationPath> \<autonomous database service name> \<user> \<password> [wallet path: ./wallet*] [collection: purchase_orders*] [async: true|false*] [batch size: 1-50000, 10000*] [threads: 1-200, VCPUs*] [append: true*|false] [start with truncate: true|false*] [random docs generated per thread: 10-100000, 10000*]
    To generate random Purchase Orders JSON data.
<envRequirement>
We suggest that you begin by typing:

  [1mcd <name>[0m
  [1mmvnw -Dmaven.test.skip=true verify[0m
  [1mjava -Xmx2G -Xms2G -jar <applicationPath> <dbNameLower>_low <config.dbUserName> <config.dbUserPassword> <config.extractedWallet> purchase_orders false 100 2 false false 1000[0m

Happy hacking!
