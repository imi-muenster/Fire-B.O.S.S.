This is a mirror of https://imigitlab.uni-muenster.de/MeDIC/fhir-facade

# FHIR B.O.S.S. (BaseX-Oriented Storage System)
FHIR B.O.S.S. is an implementation of a FHIR conformant Server, that sits in front of a BaseX database and performs all operations with XQuery. This allows for the server to be used with FHIR dependent subsystems while making it possible for the user to perform powerful queries on the underlying BaseX database.

## Motivation
FHIR (Fast Healthcare Interoperability Resources) is a standard developed by [HL7](https://www.hl7.org/) to assist data exchange for medical data. It combines the advantages of previous standards by HL7 while using common web-technologies to make it easy to implement. One of its core features is [FHIR-Search](http://hl7.org/fhir/search.html) to find data of interest in the database. While being good enough to create simple conditions for FHIR Resources it lacks the complexity to perform multi-step filtering in a single operation often requiring the implementation of additional processing layers for specific use cases. 

### Example
A simple application scenario: 

*Find all patients that had an assessment of passive range of motion where the assessment was not longer than 4 years ago.*

Finding assessments with the correct code could be done by performing a `_has` search: 
```
/Patient?_has:Procedure:patient:code=http://snomed.info/sct|710830005
```
however FHIR lacks the possibility to perform computations mid-search. So the time period has to be computed before added to the query (assume 09/07/2022 as today): 
```
/Patient?_has:Procedure:patient:code=http://snomed.info/sct|710830005&_has:Procedure:patient:date=ge2018-09-07
```
Thus having to adjust the query every time it is performed. 

<br>
XQuery is a much more flexible querying language that allows to implement functions to perform computations from within the query. 

With the following XQuery in BaseX:
```xquery
declare function local:isDateWithinYearRange($date as xs:date?, $year as xs:integer?) as xs:boolean? 
{
    (: format as yyyy-MM-dd :)
	let $boundary_date := xs:date(fn:concat(
        fn:string(year-from-date(current-date()) -$year), 
        '-', 
        fn:string(fn:format-number(month-from-date(current-date()), '00')), 
        '-', 
        fn:string(fn:format-number(day-from-date(current-date()), '00'))))
    
    return $date >= $boundary_date 
};

let $result := 

<results>
{
	for $x in db:open("Patient") 
    let $id := $x/Patient/id/@value
    
    let $refs := 
    <results>
    {
    	for $x in db:open("Procedure")
      
        where (
        	($x/Procedure/subject/reference[contains(@value, $id)]) and 
        	($x/Procedure/code/coding/code[contains(@value, "710830005")]) and
            local:isDateWithinYearRange($x/Procedure/performedDateTime/@value, 4)
        )
        return element result {$x}
    }
    </results>
   
   	for $procedure in $refs/result/Procedure
   	
    return $x
    
}
</results>

return $result
```
the query can be reperformed while not being dependent on the date. The correct range is calculated while performing the query thus making it easier to automate the complete querying process. 
 
## Building and deployment 

To start the Server create a `default.properties` file within a folder `settings` in the root of the project. The file has to contain the following information: 
```
basex.host=""
basex.port=
basex.username=""
basex.password=""
```
Build the project with the gradle commands `./gradlew clean build` to create a .war file under `build/libs/fhirBoss.war`. Deploy the file on a Tomcat server.

Adjust the server.xml configuration to allow certain query characters.

Change 
```xml
<Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" />
```
to 
```xml
<Connector port="8080" protocol="HTTP/1.1" connectionTimeout="20000" redirectPort="8443" relaxedQueryChars='^{}[]|&quot;' />
```

The server will be accessible from the URL `http://[base]/fhir/...`, with `[base]` being dependent on the tomcat deployment path.

**Important:** FHIR B.O.S.S. itself lacks security features, there is no implementation of authentication protocols built in. Do not use the server in its current form with sensitive patient data, unless you are able to ensure that no unauthorized user has access to your server.

### Source generation
During the build process, the [ResourceProviders](https://hapifhir.io/hapi-fhir/docs/server_plain/resource_providers.html) are generated by HAPI-FHIRs [hapi-tinder-plugin](https://github.com/hapifhir/hapi-fhir/tree/master/hapi-tinder-plugin) with a customized velocity template.
That way this server will be easily adjustable to changes to the FHIR standard and allows creating different versions for different FHIR versions (DSTU2, DSTU3, R4, R5).
The template generator uses the information from the official Resource Java classes.

### BaseX test server setup
To setup a test server you can use a local BaseX instance. Look [here](https://basex.org/) how to set it up correctly.
After that adjust the `default.properties` file and start the FHIR facade locally. Download the example FHIR resources in JSON format from the [official](https://hl7.org/fhir/downloads.html) website.
Put them into `src/main/resources/fhirResources/example-json` and run the main method of `BaseXPreparationUtil` in `src/main/kotlin/utils`. This will use the facade to create all necessary databases and create all example resources on the local BaseX instance. 

## Features and capabilities 
FHIR B.O.S.S is carefully modeled to conform to the [FHIR RESTful API](http://hl7.org/fhir/http.html) and the [FHIR Search](http://hl7.org/fhir/search.html) specifications.
To understand how this server operates read those specifications throughout.

### REST API
| Parameter    | Support (Limitations)                        |
|--------------|----------------------------------------------|
| read         | ✅️                                           |
| vread        | ✅️                                           |
| update       | ✅️                                           |
| patch        | ✅️ (only XML-Patch and JSON-Patch supported) |
| delete       | ✅️                                           |
| create       | ✅️                                           |
| capabilities | ✅️                                           |
| history      | ✅️                                           |

### Searchparameter types 
| Parameter       | Support                                                                                                                                                                                                        |
|-----------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Number          | ✅                                                                                                                                                                                                                         |
| Date / DateTime | ✅                                                                                                                                                                                                                         |
| String          | ✅                                                                                                                                                                                                                         |
| Token           | ✅                                                                                                                                                                                                                         |
| Reference       | 🆗 Only limited chaining capabilities supported. Will be updated in the future<br>- Chained Query on versioned references not supported yet<br>- Hierarchy not supported by server: `:above` and `:below` not implemented |
| Composite       | ✅                                                                                                                                                                                                                         |
| Quantity        | 🆗 No unit conversion currently implemented                                                                                                                                                                               | 
| URI             | ✅                                                                                                                                                                                                                         |
| Special         | ❌ `_filter` parameter is not supported yet                                                                                                                                                                                |

### Searchparameters on all resourcetypes
| Parameter      | Support                             |
|----------------|-------------------------------------|
| `_id`          | ✅                                   |
| `_lastUpdated` | ✅                                   |
| `_tag`         | ✅                                   |
| `_profile`     | ✅                                   |
| `_security`    | ✅                                   |
| `_source`      | ✅                                   |
| `_text`        | ✅                                   |
| `_content`     | ✅                                   |
| `_list`        | ✅                                   |
| `_has`         | ❌ `_has` chaining not supported yet |
| `_type`        | ✅                                   |

Note that the search result parameters are not yet implemented. They will be realised in a possible future version of this server.
Assume that every operation that is not limited in the above capabilities is fully conformant with the FHIR specifications. If you notice a flawed behaviour, feel free to contact us. 


