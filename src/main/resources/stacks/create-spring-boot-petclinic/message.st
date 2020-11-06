Success! Created <name> at <path>
Inside that directory, you can run several commands:

  mvnw spring-javaformat:apply
    Apply Spring conventions in terms of style to the project source code.

  mvnw verify
    Use Maven to build and package your Spring Boot <name> application.

  java -jar <applicationPath>
    To run the Spring Boot <name> application.

We suggest that you begin by typing:

  [1mcd <name>[0m
  [1mmvnw spring-javaformat:apply -Dmaven.test.skip=true verify[0m
  [1mjava -jar <applicationPath>[0m

You'll then be able to access Spring Boot <name> here: [1;4mhttp://localhost:8080/[0m

Happy hacking!
