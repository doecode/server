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
${datacite.username} | (optional) DataCite user account name for registering DOIs
${datacite.password} | (optional) DataCite account password for DOI registration

Execute the back-end via 

mvn jetty:run
or
mvn tomcat:run

as you prefer.  Services by default will be available on localhost port 8080.

Note that log4j assumes tomcat as a basis for its files; simply include the
command line switch to override:

mvn -P *your-profile* -Dcatalina.base=$HOME jetty:run

to have logs in $HOME/logs/doecode.log via log4j default configuration.

The value of ${database.driver} is org.apache.derby.jdbc.EmbeddedDriver for Derby.

## API services

GET /services/metadata/{ID}

Retrieves a specified Metadata by its unique ID value, in JSON format.

GET /services/metadata/yaml/{ID}

Retrieve a specified Metadata by its unique ID value, in YAML format.

GET /services/metadata/autopopulate?repo={URL}

Calls the Connector services to attempt to scrape/auto-populate metadata information
if possible by deriving the appropriate repository from the URL.  Empty JSON is
returned if the determination cannot be made or the project does not exist or
is otherwise inaccessible.

GET /services/metadata/autopopulate/yaml?repo={URL}

As above, but returns YAML.

POST /services/metadata

Store a given metadata JSON to the DOECode persistence layer in an incomplete or 
pending status.  The resulting JSON information is returned as the JSON object "metadata",
including the generated unique IDs as appropriate if the operation was successful. Record
is placed in the "Saved" work flow.

POST /services/metadata/yaml

Takes in JSON format metadata information, and returns that information in the YAML 
format.  Does not persist any data.

POST /services/metadata/publish

Store the metadata information to the DOECode persistence layer with a "Published" 
work flow.  JSON is returned as with the "Saved" service above, and this record
is marked as available to the DOECode search output services.  If DataCite information
has been configured, this step will attempt to register any DOI entered and update
metadata information with DataCite.

POST /services/metadata/submit

Post the metadata to OSTI, attempt to register a DOI if possible, and persist
the information on DOECode.  If workflow validations pass, the JSON will be returned
with appropriate unique identifier information and DOI values posted in the
JSON object "metadata".  Data is placed in "Published" state.

POST /services/validation

Send JSON detailing a set of award number values or DOIs to validate.

    { "values":["10.5072/2134", "10.5072/238923", ...],
      "validations":["DOI"] }

Each value will be checked, and JSON "errors" array returned.  Each value of the 
array should correspond with the passed-in "values" items.  If the position in the
"errors" array is blank, that value may be assumed valid; otherwise, an error
message will be returned.

    { "errors":["10.5072/2134 is not a valid DOI.", "", ...] }

## Creating a Derby Database in Eclipse

It is often useful to have a simple database for testing that is not your institutions fully deployed database. The following steps outline how to create such a database in Eclipse.
1) Install Eclipse Data Platform from the Help->Install New Software Menu if you do not already have it. The full list of update sites is available at http://www.eclipse.org/datatools/downloads.php.
2) Install Apache Derby (either by downloading it manually or installing it via a package manager).
3) In Eclipse, open the "Database Development" perspective.
4) Follow the [Eclipse Documentation](http://help.eclipse.org/kepler/index.jsp?topic=%2Forg.eclipse.datatools.common.doc.user%2Fdoc%2Fhtml%2Fasc1229700387729.html) to create a Derby Connector, create a connection profile, and connect to Derby.
