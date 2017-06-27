DOECode Services
==================

#### Introduction
Description of services provided by the API back end of DOECode.  The HTTP `GET` verb is used to retrieve information in various desired formats, and the `POST` verb used to send new and updated metadata information to the persistence back end.

> The API is available via requests sent to `/doecode/services/` on the DOECode server.

#### HTTP Request Methods
| Method | Description |
| --- | --- |
| `GET` | Used to retrieve resources |
| `POST` | Create or update resources |
| `PUT` | *Not currently used* |
| `DELETE` | *Not currently used* |

#### Metadata Service Endpoints
`GET` /services/metadata/{codeId}
> Retrieve the metadata by its *{codeId}* value.  Values returned as single JSON Objects.  See [metadata example below.](#json_example)
```json 
{ "metadata" : 
  { "software_title" : "Sample Data", "code_id": 234 ... } 
}
```

`GET` /services/metadata/yaml/{codeId}
> Retrieve the indicated metadata by its *{codeId}* value, in YAML format.

`GET` /services/metadata/autopopulate?repo={url}
> Attempt to read information from the given *repository URL* value.  Supports github.com, bitbucket.org, and sourceforge.com. Any relevant information from the repository API will be returned as metadata JSON Objects.

`POST` /services/metadata
> Send JSON metadata to be persisted in the back-end.  This service persists the data in the *Saved* work-flow state.

`POST` /services/metadata/publish
> Send JSON metadata to be persisted in the *Published* work-flow state.  Validation on required metadata fields is performed, and any errors preventing this operation will be returned.  

#### Metadata Fields
A DOECode metadata Object is expressed as a JSON entity.  Each of the fields making up the entity are defined below, and an example record is provided in JSON format.

| Field Name | Description |
| --- | --- |
| codeId | The unique value given to a particular DOECode Project record once stored.  Should be *null* or not provided for new entries, and will be returned once a record is saved or published successfully. |
| open_source | Boolean value indicating whether or not the source code is available to the public. |
| site_ownership_code | The site or office submitting this project information. |
| repository_link | If the software project is available via public hosting service, such as github.com, bitbucket.org, etc. the public repository URL should be provided here. |
| developers | An array of Objects, providing information about a project's developers or creators. Fields are [specified below.](#persons_fields) |
| contributors | An array of information about project contributors. Fields are [specified below.](#persons_fields) |
| sponsoring_organizations | (Array) Information about the project sponsoring organizations, including any funding identifier information. Fields are [specified below.](#organization_fields) |
| contributing_organizations | (Array) Information about any contributing organizations providing support for the software project. Fields are [specified below.](#organization_fields) |
| research_organizations | (Array) Information about organizations providing research information for the project. Fields are [specified below.](#organization_fields) |
| related_identifiers | (Array) Any related links, such as DOIs to published works, additional versions, or documentation information relevant to the software project. |
| description | An abstract about the software project. |
| licenses | Any software licenses or rights information about the software project. |
| doi | A [Digital Object Identifier](http://doi.org/) assigned to this software project. |
| acronym | A short descriptive acronym or abbreviation for this software project. |
| date_of_issuance | The date the software project was made available or published. |
| software_title | The software title. |
| workflow_status | The state of the software project in DOECode; either "Saved" in a temporary submission state, or "Published" if the work has been formally submitted to DOE. |

#### <a name="persons_fields"></a>Developers and Contributors Fields
Developers and Contributors are one-to-many Objects within a software project's metadata information.  The fields for each are defined below.
| Field Name | Description |
| --- | --- |
| first_name | Person's first, or given, name. |
| middle_name | Person's middle name or initial, if provided. |
| last_name | Person's last, or family, name. |
| email | An email address for the person. |
| affiliations | Any organizational affiliations for this person. |
| contributor_type | (For Contributors only) The type of contribution made by this person to the software project. |

#### <a name="organization_fields"></a>Organization Fields
The software project may specify many different types of organizations, such as Sponsoring, Research, and Contributing Organizations, but each contains similar field name information, as defined below.
| Field Name | Description |
| ---  | --- |
| organization_name | The name of the organization. |
| funding_identifiers | (Sponsoring organizations only) Define a set of funding information data, consisting of identifier_type and identifier_value fields. |
| contributor_type | (Contributing organizations only) The contribution made by this organization to the software project. |

#### <a name="json_example"></a>Example Metadata JSON
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
