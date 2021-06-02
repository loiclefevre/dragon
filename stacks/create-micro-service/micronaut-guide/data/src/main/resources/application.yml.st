micronaut:
  application:
    name: micronautguide
  executors:
    io:
      type: fixed
      nThreads: 20
datasources:
  default:
    ocid: <config.ocid>
    walletPassword: <config.dbUserPassword>
    username: <config.dbUserName>
    password: <config.dbUserPassword>
oci:
  config:
    profile: <profile>
    path: <dragonConfigFilename>
flyway:
  datasources:
    default:
      enabled: true
