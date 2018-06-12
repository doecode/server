DOE CODE Search API
==================

Introduction
------------
Description of services provided by the API back end of DOE CODE.  Searching is based around
the [Apache SOLR](http://lucene.apache.org/solr/) search product.  The HTTP
verb `GET` is used to retrieve single records, while a `POST` request may be
issued to perform a general search request.

> The API is available based on `/doecodeapi/services/docs/search` on the DOE CODE server.

HTTP Request Methods
--------------------

| Method | Description |
| --- | --- |
| `GET` | Used to retrieve a single document or issue search requests |
| `POST` | Used to issue search requests |
| `PUT` | *Not currently used* |
| `DELETE` | *Not currently used* |

Service Endpoints
-----------------

## General Information

Generates JSON listings of accepted or recommended values for various metadata
attributes.  Common objects in these lists contain attributes "label" for a display
label value (such as a drop-down listing), and the valid "value" of the field that
should be sent to the back-end.  The "value" attribute may be assumed to be
unique within each list.

| Response Code | Description |
| --- | --- |
| 200 | OK, JSON metadata or search information returned |
| 404 | Record is not on file |

### get a single record

`GET /doecodeapi/services/search/{codeId}`

Retrieve a single record from the search index by its unique identifier.  Optionally,
you may specify a query parameter of "format" to retrieve the record in either
YAML ("yaml") or XML ("xml") formats.  JSON is the default is not specified.

> Request:
```html
GET /doecodeapi/services/search/234
Content-Type: application/json
```
> Response:
```html
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"metadata":{"code_id":234, "software_title":"Sample Record Data", ... } }
```

### search request (GET)

`GET /doecodeapi/services/search`

Send a search request, defining parameters and search terms in the GET request
URL.  Valid parameters are listed below.

> Request:
```html
GET /doecodeapi/services/search?all_fields=test&rows=10
```
```
> Response:
```html
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{
  "responseHeader":{
    "status":0,
    "QTime":0,
    "params":{
      "q":"_text_:(test)",
      "sort":"",
      "rows":"10"}},
  "response":{"numFound":15,"start":0,"docs":[
      {
        "codeId":25818,
        "siteOwnershipCode":"OSTI",
        "openSource":true,
        "landingPage":"https://en.wikipedia.org/wiki/Anarchist_Protected_Area",
        "accessibility":"ON",
        "accessLimitations":["UNL"],
        "developers.name":["Last, test "],
        "_names":["Last, test ",
          "test, test "],
        "contributors.name":["test, test "]
      }
 ]
}
```

### search request (POST)

`POST /doecodeapi/services/search`

Send a search request, defining parameters and search terms in the POST request
body.  Valid parameters are listed below.

> Request:
```html
POST /doecodeapi/services/search
Content-Type: application/json
```
```json
{
"all_fields":"test", "rows":10 
}
```
> Response:
```html
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{
  "responseHeader":{
    "status":0,
    "QTime":0,
    "params":{
      "q":"_text_:(test)",
      "sort":"",
      "rows":"10"}},
  "response":{"numFound":15,"start":0,"docs":[
      {
        "codeId":25818,
        "siteOwnershipCode":"OSTI",
        "openSource":true,
        "landingPage":"https://en.wikipedia.org/wiki/Anarchist_Protected_Area",
        "accessibility":"ON",
        "accessLimitations":["UNL"],
        "developers.name":["Last, test "],
        "_names":["Last, test ",
          "test, test "],
        "contributors.name":["test, test "]
      }
 ]
}
```

| Name | Description |
| --- | --- |
| all_fields | Search for value in all the available search fields |
| software_title | Search for value in the software title |
| developers_contributors | Search for the value in developer or contributor names |
| identifiers | Search within identifying numbers |
| date_earliest | Starting date range for release date |
| date_latest | Ending date range for release date |
| accessibility | An array of accessibility types; one or more of "OS", "ON", or "CS" |
| licenses | An array of matching license values, such as the ones at the following <a href='/doecodeapi/services/docs/types#doecode-types-api-valid-types-information-licenses'>endpoint</a>. |
| orcid | Search developer or contributor ORCID values |
| sort | Specify a sorting field, e.g., "softwareTitle" or "releaseDate" (relevance is the default) |
| rows | Desired number of rows to return (default 20) |
| start | Offset row number to start for pagination (0 based) |


