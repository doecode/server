DOE CODE Types API
==================

Introduction
------------
Description of services provided by the API back end of DOE CODE.  The HTTP `GET` verb is used to retrieve information about various
types and lists of acceptable values.

> The API is available based on `/doecodeapi/services/docs/types` on the DOE CODE server.

HTTP Request Methods
--------------------

| Method | Description |
| --- | --- |
| `GET` | Used to retrieve resources |
| `POST` | *Not currently used* |
| `PUT` | *Not currently used* |
| `DELETE` | *Not currently used* |

Service Endpoints
-----------------

## Valid Types Information

Generates JSON listings of accepted or recommended values for various metadata
attributes.  Common objects in these lists contain attributes "label" for a display
label value (such as a drop-down listing), and the valid "value" of the field that
should be sent to the back-end.  The "value" attribute may be assumed to be
unique within each list.

| Response Code | Description |
| --- | --- |
| 200 | OK, JSON containing value lists is returned |

### contributortypes

`GET /doecodeapi/services/types/contributortypes`

Set of valid contributor type values as a JSON listing.

> Request:
```html
GET /doecodeapi/services/types/contributortypes
Content-Type: application/json
```
> Response:
```html
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"contributorTypes":[
{"label":"Contact Person","value":"ContactPerson"},
{"label":"Data Collector","value":"DataCollector"},
{"label":"Data Curator","value":"DataCurator"},
{"label":"Data Manager","value":"DataManager"},
{"label":"Distributor","value":"Distributor"},
{"label":"Editor","value":"Editor"},
{"label":"Hosting Institution","value":"HostingInstitution"},
{"label":"Producer","value":"Producer"},
{"label":"Project Leader","value":"ProjectLeader"},
{"label":"Project Manager","value":"ProjectManager"},
{"label":"Project Member","value":"ProjectMember"},
{"label":"Registration Agency","value":"RegistrationAgency"},
{"label":"Registration Authority","value":"RegistrationAuthority"},
{"label":"Related Person","value":"RelatedPerson"},
{"label":"Researcher","value":"Researcher"},
{"label":"Research Group","value":"ResearchGroup"},
{"label":"Rights Holder","value":"RightsHolder"},
{"label":"Sponsor","value":"Sponsor"},
{"label":"Supervisor","value":"Supervisor"},
{"label":"Work Package Leader","value":"WorkPackageLeader"},
{"label":"Other","value":"Other"}]}
```

### licenses

`GET /doecodeapi/services/types/licenses`

A set of valid, acceptable Licenses for DOE CODE.

> Request:
```html
GET /doecodeapi/services/types/licenses
Content-Type: application/json
```
> Response:
```html
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"licenses":[
{"label":"Other","value":"Other","key":"Other"},
{"label":"Apache License 2.0","value":"Apache License 2.0","key":"Apache"},
{"label":"GNU General Public License v3.0","value":"GNU General Public License v3.0","key":"GNU3"},
{"label":"MIT License","value":"MIT License","key":"MIT"},
{"label":"BSD 2-clause \"Simplified\" License","value":"BSD 2-clause \"Simplified\" License","key":"BSD2"},
{"label":"BSD 3-clause \"New\" or \"Revised\" License","value":"BSD 3-clause \"New\" or \"Revised\" License","key":"BSD3"},
{"label":"Eclipse Public License 1.0","value":"Eclipse Public License 1.0","key":"Eclipse1"},
{"label":"GNU Affero General Public License v3.0","value":"GNU Affero General Public License v3.0","key":"GNUAffero3"},
{"label":"GNU General Public License v2.0","value":"GNU General Public License v2.0","key":"GNUpublic2"},
{"label":"GNU General Public License v2.1","value":"GNU General Public License v2.1","key":"GNUpublic21"},
{"label":"GNU Lesser General Public License v2.1","value":"GNU Lesser General Public License v2.1","key":"GNUlesser21"},
{"label":"GNU Lesser General Public License v3.0","value":"GNU Lesser General Public License v3.0","key":"GNUlesser3"},
{"label":"Mozilla Public License 2.0","value":"Mozilla Public License 2.0","key":"MOZ2"},
{"label":"The Unlicense","value":"The Unlicense","key":"Unlicense"}]}
```

### relationtypes

 `GET /doecodeapi/services/types/relationtypes`

Listing of accepted related identifier relation type values.

> Request:
```
GET /doecodeapi/services/types/relationtypes
Content-Type: application/json
```
> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"relationTypes":[
{"label":"Is Cited By","value":"IsCitedBy"},
{"label":"Cites","value":"Cites"},
{"label":"Is Supplement To","value":"IsSupplementTo"},
{"label":"Is Supplemented By","value":"IsSupplementedBy"},
{"label":"Is Continued By","value":"IsContinuedBy"},
{"label":"Continues","value":"Continues"},
{"label":"Has Metadata","value":"HasMetadata"},
{"label":"Is Metadata For","value":"IsMetadataFor"},
{"label":"Is New Version Of","value":"IsNewVersionOf"},
{"label":"Is Previous Version Of","value":"IsPreviousVersionOf"},
{"label":"Is Part Of","value":"IsPartOf"},
{"label":"Has Part","value":"HasPart"},
{"label":"Is Referenced By","value":"IsReferencedBy"},
{"label":"References","value":"References"},
{"label":"Is Documented By","value":"IsDocumentedBy"},
{"label":"Documents","value":"Documents"},
{"label":"Is Compiled By","value":"IsCompiledBy"},
{"label":"Compiles","value":"Compiles"},
{"label":"Is Variant Form Of","value":"IsVariantFormOf"},
{"label":"Is Original Form Of","value":"IsOriginalFormOf"},
{"label":"Is Identical To","value":"IsIdenticalTo"},
{"label":"Is Reviewed By","value":"IsReviewedBy"},
{"label":"Reviews","value":"Reviews"},
{"label":"Is Derived From","value":"IsDerivedFrom"},
{"label":"Is Source Of","value":"IsSourceOf"}]},
{"label":"Is Described By","value":"IsDescribedBy"}]},
{"label":"Describes","value":"Describes"}]},
{"label":"Has Version","value":"HasVersion"}]},
{"label":"Is Version Of","value":"IsVersionOf"}]},
{"label":"Is Required By","value":"IsRequiredBy"}]},
{"label":"Requires","value":"Requires"}]}
```

### relatedidentifiertypes 

`GET /doecodeapi/services/types/relatedidentifiertypes`

Listing of accepted related identifier type values.

> Request:
```
GET /doecodeapi/services/types/relatedidentifiertypes
Content-Type: application/json
```
> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"relatedIdentiferTypes":[
{"label":"DOI","value":"DOI"},
{"label":"URL","value":"URL"}]}
```

### fundingidentifiertypes

`GET /doecodeapi/services/types/fundingidentifiertypes`

A listing of accepted funding identifier types for sponsoring organizations.

> Request:
```
GET /doecodeapi/services/types/fundingidentifiertypes
Content-Type: application/json
```
> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"fundingIdentifierTypes":[
{"label":"Award Number","value":"AwardNumber"},
{"label":"BR Code","value":"BRCode"},
{"label":"FWP Number","value":"FWPNumber"}]}
```
### accessibility

`GET /doecode/services/types/accessibility`

A listing of valid accessibility codes and descriptions.

> Request:
> ```
> GET /doecodeapi/services/types/accessibility
> ```
> Response:
> ```
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> {"accessibility":[
> {"label":"Open Source","value":"OS"},
> {"label":"Open Source, No Public Access","value":"ON"},
> {"label":"Closed Source","value":"CS"},
> {"label":"Closed Source, OSTI Hosted","value":"CO"}]}
> ```
