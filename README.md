# DOECode Web Application

Consists of the "back-end" services and JAX/RS API calls for DOE Code, to be
accessed by the front-end or presentation layer.  This application is targeted
at a non-EE Java container such as Tomcat, using JPA and JAX/RS (Jersey implementation)
for persistence layer and web API implementation.

## Running the back-end

The application will run on most back-end Java EE platforms, tested 
specifically on Jetty and Tomcat.  This assumes one already has a persistence
store up-and-running; define your access information via a local maven
profile, ensure you define:

Parameter | Definition
--- | ---
${database.driver} | the JDBC database driver to use 
${database.url} | the JDBC URL to access
${database.username} | the database user (with create/alter schema permission)
${database.password} | the user's password
${serviceapi.host} | base URL for validation services
${publishing.host} | base URL for submitting final metadata to OSTI (via /submit API)

Execute the back-end via 

mvn jetty:run
or
mvn tomcat:run

as you prefer.  Services by default will be available on localhost port 8080.

Note that log4j assumes tomcat as a basis for its files; simply include the
command line switch to override:

mvn -P *your-profile* -Dcatalina.base=$HOME jetty:run

to have logs in $HOME/logs/doecode.log via log4j default configuration.

## API services

GET /services/metadata/{ID}

Retrieves a specified Metadata by its unique ID value, in JSON format.

GET /services/metadata/autopopulate?repo={URL}

Calls the Connector services to attempt to scrape/auto-populate metadata information
if possible by deriving the appropriate repository from the URL.  Empty JSON is
returned if the determination cannot be made or the project does not exist or
is otherwise inaccessible.

POST /services/metadata

Store a given metadata JSON to the DOECode persistence layer in an incomplete or 
pending status.  The resulting JSON information is returned as the JSON object "metadata",
including the generated unique IDs as appropriate if the operation was successful.

POST /services/metadata/submit

Post the metadata to OSTI, attempt to register a DOI if possible, and persist
the information on DOECode.  If workflow validations pass, the JSON will be returned
with appropriate unique identifier information and DOI values posted in the
JSON object "metadata".

POST /services/validation

Send JSON detailing a set of award number values or DOIs to validate.

    { "values":["10.5072/2134", "10.5072/238923", ...],
      "validations":["DOI"] }

Each value will be checked, and JSON "errors" array returned.  Each value of the 
array should correspond with the passed-in "values" items.  If the position in the
"errors" array is blank, that value may be assumed valid; otherwise, an error
message will be returned.

    { "errors":["10.5072/2134 is not a valid DOI.", "", ...] }
