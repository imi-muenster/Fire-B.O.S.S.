# Fhir Facede

This is a first version of a facede implemented with HAPI FHIR that sits in front of a BaseX Server to perform all Operations following the FHIR standard, but also allow powerful queries performed with XQuery. 

## Setup 

To start the Facade create a `default.properties` file within a folder `settings` in the root of the project. The file has to contain the following information: 
```
basex.host=""
basex.port=
basex.username=""
basex.password=""
```
Build the project to create a .war file. Deploy the file on a Tomcat server. 