DOECode Services
==================

Introduction
------------

Description of services provided by the API back end of DOECode.  The HTTP `GET` verb is used to retrieve information in various desired formats, and the `POST` verb used to send new and updated metadata information to the persistence back end.

> The API is available via `/doecode/services/` on the DOECode server.

[DOECode on GitHub >](https://github.com/doecode/doecode)

HTTP Request Methods
--------------------

| Method | Description |
| --- | --- |
| `GET` | Used to retrieve resources |
| `POST` | Create or update resources |
| `PUT` | *Not currently used* |
| `DELETE` | *Not currently used* |

Metadata Service Endpoints
--------------------------

### Retrieve metadata in JSON format
`GET` /services/metadata/{codeId}

Retrieve the metadata by its *{codeId}* value.  Values returned as single JSON Objects.  See [metadata example below](#json_example) for metadata JSON format.

```json 
{ "metadata" : 
  { "software_title" : "Sample Data", "code_id": 234 ... } 
}
```

### Retrieve metadata in YAML format
`GET` /services/metadata/yaml/{codeId}

Retrieve the indicated metadata by its *{codeId}* value, in YAML format.

### Retrieve information from repository API
`GET` /services/metadata/autopopulate?repo={url}

Attempt to read information from the given *repository URL* value.  Supports github.com, bitbucket.org, and sourceforge.com. 
Any relevant information from the repository API will be returned in JSON metadata format.
Mapped repository information varies according to service API-supplied metadata.

### Save metadata information
`POST` /services/metadata

Send JSON metadata to be persisted in the back-end.  This service persists the data in the *Saved* work-flow state. Returns metadata information in JSON format, if successful, with
*code_id* value for reference.

> Incoming JSON format:
```json
{ "software_title" : "Sample Data", ... }
```
> Successful Response:
```json
{ "metadata" : { "code_id" : 123, "software_title" : "Sample Data", ... } }
```
> Error Response:
```json
{ "status" : 500, "message" : "Error saving record for \"Sample Data\": database failure." }
```

| HTTP Response Code | Information |
| --- | --- |
| 200 | Metadata saved, JSON metadata information returned including code_id reference. |
| 500 | Persistence error or unable to parse JSON information. |

### Publish metadata information
`POST` /services/metadata/publish

Send JSON metadata to be persisted in the *Published* work-flow state.  Validation on required metadata fields is performed, and any errors preventing this operation will be returned.  

| HTTP Response Code | Information |
| --- | --- |
| 200 | Metadata published successfully to DOECode.  JSON metadata information returned with code_id reference. |
| 400 | One or more validation errors occurred during publication, error information in the response. |
| 500 | Persistence layer error or unable to process JSON submission information. |

DOECode Metadata
===============
A DOECode metadata Object is expressed as a JSON entity.  Each of the fields making up the entity are defined below, and an example record is provided in JSON format.
A full JSON example is [provided below.](#json_example)

### Metadata Information

| Field Name | Description |
| --- | --- |
| code_id | The unique value given to a particular DOECode Project record once stored.  Should be *null* or not provided for new entries, and will be returned once a record is saved or published successfully. |
| open_source | Boolean value indicating whether or not the source code is available to the public. |
| site_ownership_code | The site or office submitting this project information. |
| repository_link | If the software project is available via public hosting service, such as github.com, bitbucket.org, etc. the public repository URL should be provided here. |
| developers | An array of Objects, providing information about a project's developers or creators. Fields are [specified below.](#persons_fields) |
| contributors | An array of information about project contributors. Fields are [specified below.](#persons_fields). Contributors must specify a [type of contribution](#contributor_types) made to the project. |
| sponsoring_organizations | (Array) Information about the project sponsoring organizations, including any funding identifier information. Fields are [specified below.](#organization_fields) |
| contributing_organizations | (Array) Information about any contributing organizations providing support for the software project. Fields are [specified below.](#organization_fields)  As with contributors, organizations must specify a [type of contribution](#contributor_types). |
| research_organizations | (Array) Information about organizations providing research information for the project. Fields are [specified below.](#organization_fields) |
| related_identifiers | (Array) Any related links, such as DOIs to published works, additional versions, or documentation information relevant to the software project. |
| description | An abstract about the software project. |
| licenses | Any software licenses or rights information about the software project. |
| doi | A [Digital Object Identifier](http://doi.org/) assigned to this software project. |
| acronym | A short descriptive acronym or abbreviation for this software project. |
| date_of_issuance | The date the software project was made available or published. |
| software_title | The software title. |
| workflow_status | The state of the software project in DOECode; either "Saved" in a temporary submission state, or "Published" if the work has been formally submitted to DOE. |

### <a name="persons_fields"></a>Developers and Contributors
Developers and Contributors are one-to-many Objects within a software project's metadata information.  
Developers are usually required information, while contributors may be optional.  Each are
arrays, so multiple values may be specified.

```json
"developers": [ 
  { "first_name" : "John", 
    "last_name" : "Jones",
    "email" : "jjones@someplace.com",
    "affiliations" : "My Company, Inc." },
    ... ],
"contributors": [ 
    { "first_name" : "Testy",  
      "last_name" : "McTesterson",
      "email" : "testy@testing.com",
      "affiliations" : "Testing Company",
      "contributor_type" : "DataCurator" },
     ...  ]
```

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
of contribution made to the project.

```json
"sponsoring_organizations":[
  {"organization_name":"Payments, Inc.",
   "funding_identifiers":[
    { "identifier_type":"AwardNumber", 
      "identifier_value":"AWARD-001" },
    ... ] },
    ... 
  ],
"contributing_organizations":[
   {"organization_name":"Boilerplate Code Productions",
    "contributor_type":"Producer"},
    ... ],
  ...
 ],
"research_organizations":[
  {"organization_name":"ACME, Inc."},
  ...
 ]
```

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

<a name="json_example"></a>Example Metadata JSON
==================
Example metadata information supplied in JSON format, utilizing all the indicated
metadata fields.

```json
{
"code_id":2651,
"site_ownership_code":"OSTI",
"open_source":true,
"repository_link":"https://github.com/doecode/doecode",
"developers":[
 {"first_name":"Project",
  "middle_name":"A.",
  "last_name":"Lead",
  "affiliations":"DOE Programming Department",
  "email":"leadguy@infosystems.doe.gov"},
  {"first_name":"A.",
  "last_name":"Developer",
  "email":"codemonkey@someplace.gov"}],
"contributors":[
 {"email":"testguy@testing.com",
  "affiliations":"Testing Services, Inc.",
  "first_name":"Tester",
  "contributor_type":"DataCurator"}],
"sponsoring_organizations":[
  {"organization_name":"OSTI",
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
