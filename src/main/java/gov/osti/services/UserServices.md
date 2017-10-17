DOECode User Services
=====================

Introduction
------------
Description of services provided by the API back end of DOE CODE.  The HTTP `GET` verb is used to retrieve information in various desired formats, 
and the `POST` verb used to send new and updated information to the persistence back end.

> The API is available based on `/doecodeapi/services/user` on the DOE CODE server.

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

## General User Information API

General or non-account-specific informational API calls.

### load

`GET /doecodeapi/services/user/load`

Authenticated end point, retrieves the logged-in User's email address if already logged in.

> Request:
```html
GET /doecodeapi/services/user/load
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
{"email":"useremail@domain.com"}
```

### hasrole

`GET /doecodeapi/services/user/hasrole/{role}`

Authenticated end point, determines whether or not the currently logged-in User has
the specified role code.

> Request:
> ```html
> GET /doecodeapi/services/user/hasrole/ROLE
> Content-Type: application/json
> Authorization: Basic myapikey
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> { "status":"success" }
> ```

### getsitecode

`GET /doecodeapi/services/user/getsitecode/{email}`

Determine the site code associated with a given email address, or whether or not it will be considered a "Contractor" ("CONTR") account.

> Request:
```html
GET /doecodeapi/services/user/getsitecode/myaddress@domain.com
```
> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"email":"myaddress@domain.com","site_code":"CONTR"}
```

## User Session Management

API calls to manage user session state; log in and out of authenticated sessions.

### login

 `POST /doecodeapi/services/user/login`

Logs a user session into DOE CODE.  User account must be verified and active to successfully log in.  Primarily intended to support client front-end and 
HTTP session management.   Requests may log in via email and password OR confirmation code (in case of forgotten passwords) for one time token use.
Repeated login attempts via password authentication WILL result in the account being locked. 
Locked accounts will receive an email message indicating administrative intervention is required to
unlock the account.  Additionally, if the password expiration date is due, the login will fail, and
a password change will be required to proceed.

> Request:
```
POST /doecodeapi/services/user/login
Content-Type: application/json
```
```json
{ "email":"myaccount@domain.com", "password":"mypassword" }
```

> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"email":"myaccount@domain.com",
 "xsrfToken":"some-token-value",
 "site":"CONTR",
 "roles":["role1", "role2"],
 "pending_roles":["role3"],
 "first_name":"User",
 "last_name":"Name"}
```

<p id='user-services-login-block'>
The returned JSON object will include the user's email address, first and last name,
a site code (or "CONTR" if contractor), and an array of role codes, if any, along 
with an "xsrfToken" value for protection against cross-site request forgery attempts.  This token
value should be transmitted back with each subsequent API request when using DOE CODE services
in conjunction with a web-based UI or user login methods.
</p>


### logout 

`GET /doecodeapi/services/user/logout`

Closes any logged-in Session in the application context.

## User Registration and Account Maintenance

API calls to register, confirm, or otherwise manage specific user account attributes.

### register

`POST /doecodeapi/services/user/register`

Request new user registration for an account on DOE CODE.  JSON included in the POST body should contain the account email, a requested password, first and last name, and the confirmation of the same password.  If the account is for a contractor, a valid DOE contract number is also required for user registration.

> Request:
```
POST /doecodeapi/services/user/register
Content-Type: application/json
```
```json
{"email":"myemail@domain.com", 
 "password":"mypassword", 
 "first_name":"User",
 "last_name":"Name",
 "confirm_password":"mypassword",
 "contract_number":"mycontractnumber"}
```

### confirm

`GET /doecodeapi/services/user/confirm?confirmation=account-token`

Request confirmation of a DOE CODE user account.  Query parameter "confirmation" should contain the generated token in the account registration email.

> Request:
```
GET /doecodeapi/services/user/confirm?confirmation=ACCOUNT_TOKEN_VALUE
```

> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"apiKey":"your-api-key"}
```

### forgotpassword

`POST /doecodeapi/services/user/forgotpassword`

Send a password reset request to a given email address.  JSON should contain the email address associated with the account.

> Request:
```
POST /doecodeapi/services/user/forgotpassword
Content-Type: application/json
```
```json
{ "email":"youraccount@domain.com" }
```

### newapikey

`GET /doecodeapi/services/user/newapikey`

Authenticated request, requests a new API key be generated and associated with the currently-logged-in User account.  
Note that any API key will be replaced by this operation, and become invalid going forward, and any logged-in
user session will become invalidated.

> Request:
```
GET /doecodeapi/services/user/newapikey
Content-Type: application/json
Authorization: Basic api-user-key
```
> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"apiKey":"sample-api-key-value"}
```

### requestadmin

`GET /doecodeapi/services/user/requestadmin`

Request site administration privileges for your account.  Requests must be approved by
administrative personnel prior to activation.  This request simply marks the account
as desiring such privileges.  Returns 200-OK if permission has already been requested,
or 201-CREATED response if request has been made.

> Request:
> ```
> GET /doecodeapi/services/user/requestadmin
> Authorization: Basic api-user-key
> ```
> Response:
> ```
> HTTP/1.1 201 CREATED
> Content-Type: application/json
> ```
> ```json
> { "status":"success" }
> ```

### update

`POST /doecodeapi/services/user/update`

Modify current user login first and last name attributes.  Requires authentication access.

> Request:
> ```
> POST /doecodeapi/services/user/update
> Content-Type: application/json
> ```
> ```json
> { "first_name":"Some", "last_name":"User" }
> ```
> Response:
> ```
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> { "email":"youremail@domain.com","first_name":"Some","last_name":"User" }
> ```

### changepassword

`POST /doecodeapi/services/user/changepassword`

Set a new password on logged in user account.  Requires authentication.  Password must conform to allowed standards.
Account must be currently logged in to change password.

> Request:
```
POST /doecodeapi/services/user/changepassword
Content-Type: application/json
```
```json
{ "password":"Mypassword", "confirm_password":"Mypassword" }
```

## Account Administrative Tasks and Roles

Special role-based administrative tasks for managing user account information.

### requests

`GET /doecodeapi/services/user/requests`

Requires authentication, and administrative access roles.  Return a listing of user accounts containing pending, requested roles for administrative processing.

>  Request:
```
GET /doecodeapi/services/user/requests
Content-Type: application/json
Authorization: Basic user-api-key
```

> Response:
```
HTTP/1.1 200 OK
Content-Type: application/json
```
```json
{"requests":[{"user":"useraccount", "roles":["one", "two"]}, {"user":"usertwo", "roles":["role"]}]}
```

### get user information

`GET /doecodeapi/services/user/{email}`

Requires administrative access.  Retrieve User information based on email address
in JSON.

### get all users

`GET /doecodeapi/services/user/users`

Requires administrative access.  Returns array of all User account information.

### update (admin)

`POST /doecodeapi/services/user/update/{email}`

Modify another user's account information.  Requires administrative access.  JSON
sent should contain only the attributes you wish to change.  Anything not specified
will remain the same.  Requests to change password must also contain "new_password"
containing the new desired password and "confirm_password" attribute matching the 
requested password, which must also conform to any password validation rules to be accepted.

> Request:
> ```html
> POST /doecodeapi/services/user/update/myaccount@domain.com
> Content-Type: application/json
> ```
> ```json
> { "first_name":"Testing", "last_name":"Person", "active":false }
> ```
> Response:
> ```html
> HTTP/1.1 200 OK
> Content-Type: application/json
> ```
> ```json
> { "email":"myaccount@domain.com", "first_name":"Testing", "last_name":"Person", "active":false, 
> "verified":true, "date_record_added":"2017-08-02", "date_record_updated":"2017-08-03",
> "date_password_changed":"2017-08-03","failed_count":0 }
> ```

<p id='user-services-on-update-user-on-success'>
On success, JSON containing the updated User account information is returned.
</p>
