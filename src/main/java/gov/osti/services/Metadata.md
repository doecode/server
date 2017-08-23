DOECode Metadata Services
==================

Introduction
------------

Description of services provided by the API back end of DOECode.  The HTTP `GET` verb is used to retrieve information in various desired formats, and the `POST` verb used to send new and updated metadata information to the persistence back end.

> The API is available via `/doecodeapi/services/metadata` on the DOECode server.

[DOECode on GitHub >](https://github.com/doecode/doecode)

HTTP Request Methods
--------------------

| Method | Description |
| --- | --- |
| `GET` | Used to retrieve resources |
| `POST` | Create or update resources |
| `PUT` | *Not currently used* |
| `DELETE` | *Not currently used* |

Service Endpoints
-----------------

## Metadata Retrieval

Information retrieval API for obtaining records already posted to DOECode or
general repository information.

### read published record

`GET /{codeId}`

Retrieve the metadata by its *{codeId}* value.  Values returned as single JSON Objects.  See [metadata example below](#json_example) for metadata JSON format.
Optionally, you may specify the query path parameter "format=yaml" to retrieve this information in YAML format.  JSON is the default output format.  Only retrieves
PUBLISHED records.

> Request:
> ```html
> GET /doecodeapi/services/metadata/234
> Content-Type: application/json
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json 
> { "metadata" : 
>   { "software_title" : "Sample Data", "code_id": 234 ... } 
> }
>```

| Response Code | Information |
| --- | --- |
| 200 | OK, response includes JSON |
| 403 | Access to unpublished metadata is not permitted |

### /edit/{codeId}

`GET /services/metadata/edit/{codeId}`

Retrieve JSON for a given metadata.  User must be authenticated and be the owner of the given metadata, or a site administrator account for the site.

| Response Code | Information |
| --- | --- |
| 200 | OK, JSON returned |
| 400 | Bad request, no code ID specified |
| 401 | Authentication is required for this service endpoint |
| 403 | Logged-in user is not permitted to access this metadata |
| 404 | Metadata CODE ID is not on file |

### /projects

`GET /services/metadata/projects`

Requires authenticated login.  Retrieve all metadata projects owned by the current logged-in user account in JSON format.

| Response Code | Information |
| --- | --- |
| 200 | OK, JSON array returned |
| 401 | Unauthorized, user login is required |

> Request:
> ```html
> GET /doecodeapi/services/metadata/projects
> Content-Type: application/json
> Authorization: Basic *user-api-key*
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> { "records":[{"code_id":234,"software_title":"Test Project", ...}, ... ] }
> ```

### /projects/pending

`GET /services/metadata/projects/pending`

Requires authentication, and special administrative privileges. Retrieve all metadata projects currently pending 
approval (that is, Published records), optionally from a given *site code*.  You may specify the optional URL
parameters of "start" (beginning row number to retrieve, from 0), "rows" (the number of rows desired at once, 0
being all of them), and "site" (only records from a given site code).  If not specified, all rows from all sites
are returned.

Responses will contain the requested number of rows (or total if unlimited), a total count, and the starting
row number of the request.

| Response Code | Information |
| --- | --- |
| 200 | OK, JSON array returned |
| 401 | Unauthorized, user login is required |
| 403 | Forbidden, insufficient user access |

> Request:
> ```html
> GET /doecodeapi/services/metadata/projects/pending?start=20&rows=10
> Content-Type: application/json
> Authorization: Basic *user-api-key*
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> { "records":[{"code_id":234,"software_title":"Test Project", ...}, ... ],
> "total":45, "start":20, "rows":10 }
> ```


### autopopulate

`GET /services/metadata/autopopulate?repo={url}`

Attempt to read information from the given *repository URL* value.  Supports github.com, bitbucket.org, and sourceforge.com. 
Any relevant information from the repository API will be returned in JSON metadata format.
Mapped repository information varies according to service API-supplied metadata.  Optionally, you may specify
a query parameter of "format=yaml" to receive YAML file suitable for download.  

If a DOECode YAML file is present
in the source repository at the base URL (named either "metadata.yml" or "doecode.yml") that file will
be read for more complete repository information.

## Metadata Submission

### save

`POST /services/metadata`

Send JSON metadata to be persisted in the back-end.  This service persists the data in the *Saved* work-flow state. Returns metadata information in JSON format, if successful, with
*code_id* value for reference.

> Request:
> ```html
> POST /services/metadata
> Content-Type: application/json
> ```
> ```json
> { "software_title" : "Sample Data", ... }
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> { "metadata" : { "code_id" : 123, "software_title" : "Sample Data", ... } }
> ```
> Error Response:
> ```html
> HTTP/1.1 500
> Content-Type: application/json
> ```
> ```json
> { "status" : 500, "errors" : ["Error saving record for \"Sample Data\": database failure." ] }
> ```

| Response Code | Information |
| --- | --- |
| 200 | Metadata saved, JSON metadata information returned including code_id reference. |
| 401 | Authentication is required to POST |
| 403 | User is not permitted to alter this metadata |
| 500 | Persistence error or unable to parse JSON information. |

### publish

`POST /services/metadata/publish`

Send JSON metadata to be persisted in the *Published* work-flow state.  Validation on required metadata fields is performed, and any errors preventing 
this operation will be returned.  

Validation rules are:

* source accessibility is required:
  * "OS" (Open Source), also requires a valid accessible repository link
  * "ON" (Open Source, Not Publically Available), requires a landing page
  * "CS" (Closed Source), also requires a landing page
* software title
* description
* at least one license
* at least one developer
  * each developer must have a first and last name
  * if email is provided, it must be valid
* if DOI is specified, release date is required

> Request:
> ```html
> POST /services/metadata/publish
> Content-Type: application/json
> Authorization: Basic user-api-key
> ```
> ```json
> { "code_id":123, "software_title":"Sample Data", ... }
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> { "metadata" : { "code_id" : 123, "software_title" : "Sample Data", ... } }
> ```
> Error Response:
> ```html
> HTTP/1.1 400 BAD REQUEST
> Content-Type: application/json
> ```
> ```json
> { "status" : 400, "errors":[ "Title is required", "Developers are required", "Provided email address is invalid" ] }
> ```

| HTTP Response Code | Information |
| --- | --- |
| 200 | Metadata published successfully to DOECode.  JSON metadata information returned with code_id reference. |
| 400 | One or more validation errors occurred during publication, error information in the response. |
| 401 | Authentication is required to POST |
| 403 | User is not permitted to alter this record |
| 500 | Persistence layer error or unable to process JSON submission information. |

### submit

`POST /doecodeapi/services/metadata/submit`

Send JSON formatted metadata to DOECode for a software project that is considered fully complete and ready to 
be submitted to DOE.  Additional validations are required for final submission:

* All above Publish validations apply
* A release date is required
* At least one sponsoring organization is required
  * each organization must have a name
  * if DOE, must also have a valid primary award number
* At least one research organization is required
  * each organization must have a name
* Contact information is required
  * email must be valid
  * phone number must be valid
  * organization name is required
* If project is not Open Source ("OS") availability, a file upload is required

> Request:
> ```html
> POST /services/metadata/submit
> Content-Type: application/json
> Authorization: Basic user-api-key
> ```
> ```json
> { "code_id":123, "software_title":"Sample Data", ... }
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> { "metadata" : { "code_id" : 123, "software_title" : "Sample Data", ... } }
> ```
> Error Response:
> ```html
> HTTP/1.1 400 BAD REQUEST
> Content-Type: application/json
> ```
> ```json
> { "status" : 400, "errors":[ "Title is required", "Developers are required", "Provided email address is invalid" ] }
> ```

| HTTP Response Code | Information |
| --- | --- |
| 200 | Metadata published successfully to DOECode.  JSON metadata information returned with code_id reference. |
| 400 | One or more validation errors occurred during publication, error information in the response. |
| 401 | Authentication is required to POST |
| 403 | User is not permitted to alter this record |
| 500 | Persistence layer error or unable to process JSON submission information. |

DOECode Metadata
===============
A DOECode metadata Object is expressed as a JSON entity.  Each of the fields making up the entity are defined below, and an example record is provided in JSON format.
A full JSON example is [provided below.](#json_example)

## Metadata Field Information

| Field Name | Description |
| --- | --- |
| code_id | The unique value given to a particular DOECode Project record once stored.  Should be *null* or not provided for new entries, and will be returned once a record is saved or published successfully. |
| accessibility | Project source code accessibility value; must be one of "OS" (open source), "ON" (open source, not public) or "CS" (closed source) |
| repository_link | If the software project is available via public hosting service, such as github.com, bitbucket.org, etc. the public repository URL should be provided here. |
| landing_page | If the project is not available via open source hosting site, provide a URL describing the project and contact information for obtaining binary or source |
| developers | An array of Objects, providing information about a project's developers or creators. Fields are [specified below.](#persons_fields) |
| contributors | An array of information about project contributors. Fields are [specified below.](#persons_fields). Contributors must specify a [type of contribution](#contributor_types) made to the project. |
| sponsoring_organizations | (Array) Information about the project sponsoring organizations, including any funding identifier information. Fields are [specified below.](#organization_fields) |
| contributing_organizations | (Array) Information about any contributing organizations providing support for the software project. Fields are [specified below.](#organization_fields)  As with contributors, organizations must specify a [type of contribution](#contributor_types). |
| research_organizations | (Array) Information about organizations providing research information for the project. Fields are [specified below.](#organization_fields) |
| related_identifiers | (Array) Any related links, such as DOIs to published works, additional versions, or documentation information relevant to the software project. |
| description | An abstract about the software project. |
| licenses | Any software licenses or rights information about the software project, may have multiple values. |
| doi | A [Digital Object Identifier](http://doi.org/) assigned to this software project. |
| acronym | A short descriptive acronym or abbreviation for this software project. |
| date_of_issuance | The date the software project was made available or published. |
| software_title | The software title. |

### <a name="persons_fields"></a>Developers and Contributors
Developers and Contributors are one-to-many Objects within a software project's metadata information.  
Developers are usually required information, while contributors may be optional.  Each are
arrays, so multiple values may be specified.

> ```json
> "developers": [ 
> { "first_name" : "John", 
>   "last_name" : "Jones",
>   "email" : "jjones@someplace.com",
>   "affiliations" : ["My Company, Inc."] },
> ... ],
> "contributors": [ 
> { "first_name" : "Testy",  
>   "last_name" : "McTesterson",
>   "email" : "testy@testing.com",
>   "affiliations" : ["Testing Company"],
>   "contributor_type" : "DataCurator" },
> ...  ]
> ```

| Field Name | Description |
| --- | --- |
| first_name | Person's first, or given, name. |
| middle_name | Person's middle name or initial, if provided. |
| last_name | Person's last, or family, name. |
| email | An email address for the person. |
| affiliations | Any organizational affiliations for this person. |
| contributor_type | (For Contributors only) The [type of contribution](#contributor_types) made by this person to the software project. |

### <a name="organization_fields"></a>Organizations
The software project may specify many different types of organizations, such as Sponsoring, Research, and Contributing Organizations, but each contains similar field name information, as defined below.
Organizations are distinguished by particular information:  Sponsors contain one or more
funding identifiers or award numbers, while Contributing organizations provide the type
of contribution made to the project.  DOE sponsoring organizations are required to send a valid 
DOE contract number as a "primary_award" field.

> ```json
> "sponsoring_organizations":[
> {"organization_name":"Payments, Inc.",
>  "funding_identifiers":[
> { "identifier_type":"AwardNumber", 
>  "identifier_value":"AWARD-001" },
> ... ] },
> {"organization_name":"DOE Lab",
>  "DOE":true,
>  "primary_award":"award-number"},
> ... 
> ],
> "contributing_organizations":[
> {"organization_name":"Boilerplate Code Productions",
>  "contributor_type":"Producer"},
> ... ],
> ...
> ],
> "research_organizations":[
> {"organization_name":"ACME, Inc."},
> ...
> ]
> ```

| Field Name | Description |
| ---  | --- |
| organization_name | The name of the organization. |
| funding_identifiers | (Sponsoring organizations only) Define a set of funding information data, consisting of identifier_type and identifier_value fields. |
| contributor_type | (Contributing organizations only) The contribution made by this organization to the software project. |

### <a name="contributor_types"></a>Contributor Types
Suggested values for Contributor Types, both for Contributors and Contributing 
Organizations. This information helps distinguish each contributor or organization's role
in the software project.

| Contributor Type | Description |
| --- | --- |
| ContactPerson	| Person with knowledge of how to access, troubleshoot, or otherwise field issues related to the resource. |
| DataCollector	| Person/institution responsible for finding or gathering data under the guidelines of the author(s) or Principal Investigator. |
| DataCurator | Person tasked with reviewing, enhancing, cleaning, or standardizing metadata and the associated data submitted. |
| DataManager | Person (or organisation with a staff of data managers, such as a data centre) responsible for maintaining the finished resource. |
| Distributor | Institution tasked with responsibility to generate/disseminate copies of the resource in either electronic or print form. |
| Editor | A person who oversees the details related to the publication format of the resource. |
| HostingInstitution | The organisation allowing the resource to be available on the internet. |
| Producer | Typically a person or organisation responsible for the artistry and form of a media product. |
| ProjectLeader | Person officially designated as head of project team instrumental in the work necessary to development of the resource. |
| ProjectManager| Person officially designated as manager of a project. Project may consist of one or many project teams and sub-teams. |
| ProjectMember | Person on the membership list of a designated project/project team. |
| RegistrationAgency | Institution officially appointed by a Registration Authority to handle specific tasks within a defined area of responsibility. |
| RegistrationAuthority	| A standards-setting body from which Registration Agencies obtain official recognition and guidance. |
| RelatedPerson	| Person with no specifically defined role in the development of the resource, but who is someone the author wishes to recognize. |
| Researcher	| A person involved in analyzing data or the results of an experiment or formal study. |
| ResearchGroup	| Refers to a group of individuals with a lab, department, or division; the group has a particular, defined focus of activity. |
| RightsHolder	| Person or institution owning or managing property rights, including intellectual property rights over the resource. |
| Sponsor	| Person or organisation that issued a contract or under the auspices of which a work has been performed. |
| Supervisor	| Designated administrator over one or more groups working to produce a resource or over one or more steps of development process. |
| WorkPackageLeader	| A Work Package is a recognized data product, not all of which is included in publication. |
| Other	| Any person or institution making a significant contribution, but whose contribution does not "fit". |

## <a name="json_example"></a>Example Metadata JSON

Example metadata information supplied in JSON format, utilizing all the indicated
metadata fields.

```json
{
"code_id":2651,
"site_ownership_code":"OSTI",
"accessibility":"OS",
"repository_link":"https://github.com/doecode/doecode",
"developers":[
 {"first_name":"Project",
  "middle_name":"A.",
  "last_name":"Lead",
  "affiliations":["DOE Programming Department"],
  "email":"leadguy@infosystems.doe.gov"},
  {"first_name":"A.",
  "last_name":"Developer",
  "email":"codemonkey@someplace.gov"}],
"contributors":[
 {"email":"testguy@testing.com",
  "affiliations":["Testing Services, Inc."],
  "first_name":"Tester",
  "contributor_type":"DataCurator"}],
"sponsoring_organizations":[
  {"organization_name":"OSTI",
   "primary_award":"DE-OR-111",
   "DOE":true,
   "funding_identifiers":[
    {"identifier_type":"AwardNumber",
     "identifier_value":"DE-OR-1234"},
    {"identifier_type":"BRCode",
     "identifier_value":"BR-549"}]},
  {"organization_name":"University of Tennessee, Knoxville",
   "funding_identifiers":[
     {"identifier_type":"AwardNumber",
      "identifier_value":"UTK-2342"},
     {"identifier_type":"AwardNumber",
      "identifier_value":"NE-2017-2342"}]},
  {"organization_name":"ORNL",
   "DOE":true,
   "primary_award":"ORNL-FG-2034",
   "funding_identifiers":[
     {"identifier_type":"AwardNumber",
      "identifier_value":"ORNL-IDNO-001"}]}],
"contributing_organizations":[
  {"organization_Name":"ORNL",
   "contributor_type":"DataManager"},
  {"organization_name":"DOE OSTI",
   "contributor_type":"HostingService"}],
"research_organizations":[
  {"organization_name":"University of Washington, Computer Sciences Department"},
  {"organization_name":"Tester Services, Inc."}],
"related_identifiers":[
  {"identifier_type":"DOI",
   "identifier_value":"10.5072/OSTI/2017/1",
   "relation_type":"IsSourceOf"}],
"date_of_issuance":"2016-02-03",
"software_title":"Department of Energy DOECode Project",
"acronym":"doecode",
"doi":"10.5072/DOECode2017/7174",
"description":"Main repository for managing the new DOE Code site from the DOE Office of Scientific and Technical Information (OSTI)",
"workflow_status":"Published"
}
```
