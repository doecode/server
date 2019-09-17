DOE CODE Site Services
=====================

Introduction
------------
Description of services provided by the API back end of DOE CODE.  The HTTP `GET` verb is used to retrieve information in various desired formats,
and the `POST` verb used to send new and updated information to the persistence back end.

> The API is available based on `/doecodeapi/services/docs/site` on the DOE CODE server.

HTTP Request Methods
--------------------

| Method | Description |
| --- | --- |
| `GET` | Used to retrieve resources |
| `POST` | Create or update resources |
| `PUT` | *Not currently used* |
| `DELETE` | *Not currently used* |

HTTP Response Codes
-------------------

Most relevant service endpoints share common HTTP response codes, with the most
common ones with typical reasons included below.

| Response Code | Description |
| --- | --- |
| 200 | OK, request was processed successfully |
| 400 | Bad Request, such as validation error or bad JSON |
| 401 | User is not authenticated |
| 403 | User lacks sufficient privileges for the action |
| 404 | Requested resource not found |
| 500 | Internal error or database issue |

Service Endpoints
-----------------

## General Site Information API

General or non-account-specific informational API calls.

### info

`GET /doecodeapi/services/site/info`

Authenticated OSTI role end point, retrieves a list of Site information.

> Request:
```html
GET /doecodeapi/services/site/info
Content-Type: application/json
Authorization: Basic user-api-key
```

> Response:
```html
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 27
Date: Mon, 14 Aug 2017 14:55:04 GMT
```
```json
[{"site_code": "ABCD","email_domains": ["@abcd.gov"],"poc_emails": ["smith@abcd.gov","jones@abcd.gov"],"lab": "Alpha Bravo Charlie Delta","isStandardUsage":true,"isHqUsage":false},{"site_code": "EFGH","email_domains": ["@efgh.gov"],"poc_emails": [],"lab": "Echo Foxtrot Golf Hotel","isStandardUsage":true,"isHqUsage":false}]
```

### info/{site}

`GET /doecodeapi/services/site/info/{site}`

Authenticated OSTI role end point, retrieves Site information for a specific site code.

> Request:
> ```html
> GET /doecodeapi/services/site/info/SITE
> Content-Type: application/json
> Authorization: Basic myapikey
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> {"site_code": "SITE","email_domains": ["@site.gov"],"poc_emails": [],"lab": "The Site"}
> ```

## Site Management

API calls to manage site information; update information.

### update

 `POST /doecodeapi/services/site/update`

Updates a Site in DOE CODE.  User must be verified OSTI role.  Primarily intended to support client front-end Site management.
Currently only supports POC updates.  Empty value/array will erase the data.  If successful, simply returns the new data.

> Request:
```
POST /doecodeapi/services/site/update
Content-Type: application/json
```
```json
[{"site_code": "ABCD","poc_emails": []},{"site_code": "EFGH","poc_emails": ["new@email.gov"]}]
```

> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
[{"site_code": "ABCD","poc_emails": []},{"site_code": "EFGH","poc_emails": ["new@email.gov"]}]
```

<p id='site-services-on-update-site-on-success'>
On success, JSON containing the updated Site information is returned.
</p>
