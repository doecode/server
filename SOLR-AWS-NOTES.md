Installing SOLR on AWS EC2
==========================

Steps for creation of a SOLR instance running on its own EC2 AWS instance.

1. Create a new EC2 instance of the desired size, using the "Amazon Linux" 
64-bit AMI as a model.
2. Upon starting the instance, ensure that an access key is generated (the
wizard will prompt to do so).  Save the .pem locally for accessing via SSH.
3. After the instance is started, log in via ssh using the identity .pem file.
From a Linux machine or OSX terminal, use the command-line ssh client.
```bash
$ ssh -i path/to/file.pem ec2-user@public-ip-of-ec2-instance
```
4. The default user should have full sudo privileges to install software
needed.  In this case, the Amazon Linux instance comes with Java 1.7.0
pre-installed, but SOLR will required 1.8.  In order to get that updated,
perform the following from the EC2 command-line.  Install "git" command-line
for convenience for later as well.
```bash
$ sudo yum install java-1.8.0
$ sudo yum remove java-1.7.0-openjdk
$ sudo yum install git
```
Ensure the new Java is installed successfully.
```bash
$ java -version
openjdk version "1.8.0_131"
OpenJDK Runtime Environment (build 1.8.0_131-b11)
OpenJDK 64-Bit Server VM (build 25.131-b11, mixed mode)
```
5. Install the SOLR software, which is downloaded via the main Apache SOLR
web site: https://lucene.apache.org/solr/.  Follow the "Download" link and
obtain a URL to the latest tarball, at time of this writing, something like
{apache-solr-mirror-host}/lucene/solr/6.6.0/solr-6.6.0.tgz
6. Copy that download URL in order to download from the command-line on the
EC2 instance.  Paste in the following:
```bash
$ curl -o solr-6.6.0.tgz {apache-solr-mirror-host}/lucene/solr/6.6.0/solr-6.6.0.tgz
```
7. Unpack the resulting tarball instance locally in the user's home folder. 
The service installation script is already part of the distribution, and may
be run as super-user locally to install SOLR as a service.  By default, this
will install SOLR in /opt/solr, and the indexes and configuration files under
/var/solr, along with creating a new user account "solr" to run the service
as.  Examine the shell script parameters and customize to your needs.  Specify
a port on which SOLR will run, and note this for later configuration and 
security settings.
```bash
$ cd solr-6.6.0
$ sudo bin/install_solr_service.sh -p {port}
```
Once this script runs, SOLR should be up and running on the {port} you specify.

Creation of the search core and customization
---------------------------------------------
Once SOLR is running, we will need to create a search and index core to use,
and customize it according to the DOECode specific schema and configuration,
which are available in the root of https://github.com/doecode/server.

Switch to the newly-created "solr" user and issue the SOLR commands:
```bash
$ sudo -u solr -i
[solr] $ cd /opt/solr
[solr] $ bin/solr create -c doecode -p {port}
```
This will create the unconfigured search core and start it.  To customize, 
acquire the solrconfig.xml and schema.xml from the server repository, and
place them appropriately.  You can use the git command-line tool to get the
latest files from github:
```bash
[solr] $ git clone https://github.com/doecode/server
[solr] $ cp server/schema.xml /var/solr/data/doecode/conf/
[solr] $ cp server/solrconfig.xml /var/solr/data/doecore/conf/
```
Issue the command to reload the SOLR core with the new configuration.
```bash
[solr] $ curl http://localhost:{port}/solr/admin/cores?action=RELOAD\&core=doecode
```

The DOECode SOLR search core should be up and ready to use on the indicated
port.  All that remains is to configure the EC2 instance TCP access rules
to allow inbound connections on the port to the desired group(s) or application
server IPs for the "server" back-end process, which will allow it to index
and search.

