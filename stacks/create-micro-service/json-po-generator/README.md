# Oracle LiveLabs - DRAGON Stack JSON without Limits Lab #1
TL;DR This stack _overrides_ the default create-micro-service and provides:
- a random Purchase Orders JSON data generator to load into the purchase_orders collection

## Details
This microservice aims to provide the very basic component of loading random JSON data. 
The generated JSON documents represent the details of fictive Purchase Orders for an e-commerce shop
selling DVD movies online.

### Purchase Order example
Hereunder, you can find the details of such a _random_ Purchase Order:

```
{
   "requestor": "Alexis Bull",
   "requestedAt": "2020-07-20T10:16:52Z",
   "shippingInstructions": {
      "address": {
         "street": "200 Sequoia Avenue",
         "city": "South San Francisco",
         "zipCode": 94080,
         "geometry": {
             "type": "Point",
             "coordinates": [ 37.662187, -122.440148 ]
         }
      },
      "phone": [
         {
            "type": "Mobile",
            "number": "415-555-1234"
         }
      ]
   },
   "specialInstructions": "Air Mail",
   "allowPartialShipment": false,
   "items": [
      {
         "description": "One Magic Christmas",
         "unitPrice": 19.95,
         "UPCCode": 13131092899,
         "quantity": 1.0
      },
      {
         "description": "Lethal Weapon",
         "unitPrice": 19.95,
         "UPCCode": 85391628927,
         "quantity": 5.0
      }
   ]
}
```

### Generation process

The microservice first initialize an array of random JSON documents per Java thread. 
This array is then used at runtime to pick documents to load into the SODA collection.

The goal is really to figure out the raw performance of data ingestion rather than random 
number generation.

The SODA collection name passed as an argument is created automatically in the case it doesn't
exist inside the database.

### Command line options

The microservice is configurable to test the performance impact of different features.

General usage:
```
java -Xmx4G -Xms4G -jar rtgenerator-1.0.3.jar <database service> <user> <password> <wallet path> <collection name> \
<asynchronous transaction> <batch size> <number of threads> <append hint> <truncate collection at start time> <number of random JSON documents>
```

Where:
- **database service** is the database service to use
  - for Autonomous databases, prefer _low or _tp
  - for non-Autonomous databases, use the following format //<[private] ip>:<port>/<database service name>
- **user** is the user schema to connect to
- **password** is the user password
- **wallet path** is the full path to the folder name where the Autonomous database wallet has been unzipped
  - for non-Autonomous databases, use a fictive value
- **asynchronous transaction** is true or false (default)
  - if this parameter is true then the commit operation is no more synchronous
- **batch size** is the number of JSON documents to ingest in one call to the database, this helps a lot to ingest data faster than sending JSON documents one by one
  - the default is 10,000 but 100 is generally good enough
  - the value range can be from 1 (no batch) to 10,000
  - the average size of the JSON documents is 600 bytes
- **number of threads** is the number of Java threads used to ingest the data
  - the default value is the number of detected VCPUs of the host
- **append hint** is true (default) or false
  - when true, the new JSON documents are inserted at the very end of the collection physical extents
- **truncate collection at start time** is true or false (default)
  - when true, the collection is cleared before any transaction is started
- **number of random JSON documents** is the number of random JSON documents to create at start time
  - the default is 10,000
  - large values will require more memory (see -Xmx and -Xms JVM parameters)


