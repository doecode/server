DOECode User Services
=====================

Introduction
------------
Description of services provided by the API back end of DOECode.  The HTTP `GET` verb is used to retrieve information in various desired formats, 
and the `POST` verb used to send new and updated information to the persistence back end.

> The API is available based on `/doecodeapi/services/user` on the DOECode server.

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

### General User Information API

General or non-account-specific informational API calls.

`GET /load`

Authenticated end point, retrieves the logged-in User's email address if already logged in.

| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, JSON contains user's email address |
| 401 | Unauthorized, no user session logged in |

> Request:
>> GET /doecodeapi/services/user/load
> Content-Type: application/json
> Authorization: Basic user-api-key
> 
> Response:
> 
> >HTTP/1.1 200 OK
> >Content-Type: application/json
> > Content-Length: 27
> > Date: Mon, 14 Aug 2017 14:55:04 GMT
> > {"email":"useremail@domain.com"}

`POST /getsitecode`

Determine the site code associated with a given email address, or whether or not it will be considered a "Contractor" ("CONTR") account.

| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, JSON contains email address and site code |

> Request:
> > POST /doecodeapi/services/user/getsitecode
> Content-Type: application/json
> { "email":"myaddress@domain.com" }

> Response:
> > HTTP/1.1 200 OK
> Content-Type: application/json
> {"email":"myaddress@domain.com","site_code":"CONTR"}

### User Session Management

API calls to manage user session state; log in and out of authenticated sessions.

`GET /login`

Logs a user session into DOECode.  User account must be verified and active to successfully log in.  Primarily intended to support client front-end and HTTP session management.   Requests may log in via password OR confirmation code (in case of forgotten passwords) for one time token use.

| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, JSON contains XSRF token, boolean value indicating contractor status, user email address, and first and last name |
| 401 | Authorized, account is not valid, login failure, or not active |
| 403 | Forbidden, confirmation or password is not valid |
| 500 | Internal service error |

> Request:
> > POST /doecodeapi/services/user/login
> Content-Type: application/json
> { "email":"myaccount@domain.com", "password":"mypassword" }

> Response:
> > HTTP/1.1 200 OK
> Content-Type: application/json
> { "email":"myaccount@domain.com",
> "xsrfToken":"some-token-value",
> "hasSite":false,
> "first_name":"User",
> "last_name":"Name"}

`GET /logout`

Closes any logged-in Session in the application context.
| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, session logged out |

### User Registration and Account Maintenance

API calls to register, confirm, or otherwise manage specific user account attributes.

`POST /register`

Request new user registration for an account on DOECode.  JSON included in the POST body should contain the account email, a requested password, first and last name, and the confirmation of the same password.  If the account is for a contractor, a valid DOE contract number is also required for user registration.
| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, account request created, email with confirmation link sent |
| 400 | Bad request, invalid information provided or account already on file, or passwords do not match or are not acceptable, or missing valid contract number or first/last name|
| 500 | Unexpected or internal system error occurred |

> Request:
> > POST /doecodeapi/services/user/register
> Content-Type: application/json
> {"email":"myemail@domain.com", 
> "password":"mypassword", 
> "first_name":"User",
> "last_name":"Name",
> "confirm_password":"mypassword",
> "contract_number":"mycontractnumber"}
> 
> Response:
> >HTTP/1.1 200 OK
> Content-Type: application/json
> No Content

`GET /confirm?confirmation=account-token`

Request confirmation of a DOECode user account.  Query parameter "confirmation" should contain the generated token in the account registration email.
| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, account is confirmed, JSON containing API key is returned |
| 400 | Bad request, required information not provided |
| 401 | Unauthorized, confirmation codes not valid |
| 404 | User account is not on file |
| 500 | Unexpected or internal system error |
> Request:
> > GET /doecodeapi/services/user/confirm?confirmation=ACCOUNT_TOKEN_VALUE

> Response:
> > HTTP/1.1 200 OK
> Content-Type: application/json
> {"apiKey":"your-api-key"}

`POST /forgotpassword`

Send a password reset request to a given email address.  JSON should contain the email address associated with the account.

| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, password reset email sent |
| 400 | Bad request, email not found or account not yet verified |
| 500 | Internal processing error |

> Request:
> > POST /doecodeapi/services/user/forgotpassword
> Content-Type: application/json
> { "email":"youraccount@domain.com" }

`GET /newapikey`

Authenticated request, requests a new API key be generated and associated with the currently-logged-in User account.  Note that any API key will be replaced by this operation, and become invalid going forward.
| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, JSON contains newly created API key |
| 401 | Unauthorized, no currently-logged-in User session |
| 500 | An unexpected service error occurred |

> Request:
> > GET /doecodeapi/services/user/newapikey
> Host: apihost:port
> Accept: */*
> Content-Type: application/json
> Authorization: Basic api-user-key
> 
> Response:
> > HTTP/1.1 200 OK
> Content-Type: application/json
> Content-Length: 27
> Date: Mon, 14 Aug 2017 15:01:11 GMT
> {"apiKey":"sample-api-key-value"}

`GET /requestadmin`

Requires authentication.  Requests site administrative access for the logged-in User account.  Such requests must be approved by an OSTI administrative user.

| HTTP Response Code | Description |
| --- | --- |
| 200 | Request already pending or on file |
| 201 | Request created pending approval |
| 401 | User account not logged in |
| 403 | Forbidden, unable to process request (contractor account) |
| 500 | Internal service error |

`POST /update`

Modify current user login first and last name attributes.  Requires authentication access.

| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, account attributes updated |
| 400 | Missing required first or last name values |
| 401 | User is not logged in |
| 500 | Internal service error |

> Request:
> > POST /doecodeapi/services/user/update
> Content-Type: application/json
> { "first_name":"Some", "last_name":"User" }

`POST /changepassword`

Set a new password on logged in user account.  Requires authentication.  Password must conform to allowed standards.

| HTTP Response Code | Description |
| --- | --- |
| 200 | Password changed successfully |
| 400 | Passwords not acceptable or do not match |
| 401 | User not logged in |
| 500 | Internal service error |

> Request:
> > POST /doecodeapi/services/user/changepassword
> Content-Type: application/json
> { "password":"Mypassword", "confirm_password":"Mypassword" }


### Account Administrative Tasks and Roles

Special role-based administrative tasks for managing user account information.

`GET /requests`

Requires authentication, and administrative access roles.  Return a listing of user accounts containing pending, requested roles for administrative processing.
| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, JSON returns listing of user information with pending requested roles |
| 401 | Unauthorized, no current user login context |
| 403 | Insufficient privileges to access this function |
| 500 | Unexpected error or internal service error |
>  Request:
> > GET /doecodeapi/services/user/requests
> Content-Type: application/json
> Authorization: Basic user-api-key
>
> Response:
> > HTTP/1.1 200 OK
> Content-Type: application/json
> Date: Mon, Aug 14 2017 15:27:12 GMT
> {"requests":[{"user":"useraccount", "roles":["one", "two"]}, {"user":"usertwo", "roles":["role"]}]}

`POST /admin`

Requires authentication, and administrative role user.  Activate or deactivate an indicated user account by email address, preventing or allowing login or API access.

| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, state processed, JSON containing email address and current state returned |
| 401 | User account not logged in |
| 403 | Access is forbidden |
| 404 | User email not on file |
| 500 | Internal service error |

> Request:
> > POST /doecodeapi/services/user/admin
> Content-Type: application/json
> { "email":"user@account.com", "activate":false }

> Response:
> > HTTP/1.1 200 OK
> Content-Type: application/json
> { "email":"user@account.com", "active":false }

`POST /approve`

Requires authentication, and administrative roles.  Approve pending user request for roles to be added to a user account.
| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, JSON contains "success":"success" response |
| 401 | Unauthorized, user is not logged in |
| 403 | Forbidden, insufficient privileges to access function |
| 500 | Unexpected or system error occurred |

> Request:
> > POST /doecodeapi/services/user/approveroles
> Content-Type: application/json
> {"email":"address@approved.com"}

> Response:
> > HTTP/1.1 200 OK
> Content-Type: application/json
> {"success":"success"}

`POST /disapprove`

Requires authentication and administrative roles to access.  Denies requested user roles pending approval.  Pass in user email for account information to deny role requests.
| HTTP Response Code | Description |
| --- | --- |
| 200 | OK, JSON contains "success":"success" response |
| 401 | Unauthorized, user is not logged in |
| 403 | Forbidden, insufficient privileges to access function |
| 500 | Unknown or internal server error occurred |

> Request:
> > POST /doecodeapi/services/user/disapprove
> Content-Type: application/json
> {"email":"disapprove@domain.com"}

