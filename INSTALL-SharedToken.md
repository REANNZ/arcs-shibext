arcs-shibext
============

ARCS Shibboleth extensions - sharedToken implementation

Imported from http://projects.arcs.org.au/trac/systems/wiki/HowTo/PilotAAF/SharedToken

# auEduPersonSharedToken Installation Guide (Draft)

This document describes how to enable auEduPersonSharedToken on Shibboleth IdP 2.x using ARCS Shibboleth extension.

## Acronyms
 * IMAST - Institution Managed auEduPersonSharedToken
 * aEPST - auEduPersonSharedToken

## Introduction
 * ```auEduPersonSharedToken:``` aEPST is a unique identifier enabling federation spanning services such as Grid and Repositories. The value of the identifier is unique, non-reassigned, persistent and portable.
 * ```IMAST:``` IMAST is one approach for IdPs in the AAF to provide the aEPST for their user. The aEPST is generated and managed by home IdP rather than the Federated aEPST service.
 * ```ARCS Shibboleth Extension:``` This is a java library extending Shibboleth2 data connectors. It provides the implementations of IMAST auEduPersonSharedToken. The SharedToken is generated when it does not exist, then persisted in the institutional LDAP or the database.

*IMPORTANT*: The generation of the SharedToken relies on the user's identifier, the IdP's Identifier and the private seed. Change of the inputs will change the SharedToken value. This is likely to happen due to the change of the user's identifier, the home institution, the upgrade of the IdP and so on. Therefore, in production environment, the SharedToken must be only generated once and persisted in the institution LDAP or the database for future uses. The administrator should also ensure all selected inputs available and stable for user's Ldap entries.

## Prerequisites
 * A running shibboleth IdP 2.1
 * A LDAP server with write access
 * Exported environment variable IDP_HOME, defaults to /opt/shibboleth-idp (default value can be configured in src/main/resources/conf/shareddtoken.properties in the source code)

## Install ARCS Shibboleth Extension

Find the correct directory to install into, you will need to find the WEB-INF directory of you IdP source package. (If no lib directory please create it)
```
export IDP_SRC_HOME=/opt/shibboleth-identityprovider-2.1.2
```

We'll install the latest version in this instruction. You can find older versions at https://github.com/REANNZ/arcs-shibext/releases/

### Install from binary

Remove the old version if any and download jar into place
```
cd $IDP_SRC_HOME/src/main/edit-webapp/WEB-INF/lib
rm arcs-shibext-*.jar
wget https://github.com/REANNZ/arcs-shibext/raw/master/download/arcs-shibext-1.5.5.jar
```

```
cd $IDP_SRC_HOME/lib
rm arcs-shibext-*.jar
wget https://github.com/REANNZ/arcs-shibext/raw/master/download/arcs-shibext-1.5.5.jar
```
Run IdP install script again. '''(make sure to answer NO to "DO you want to overwrite your configuration")'''
```
cd $IDP_SRC_HOME
./install.sh
```

### Install from source
Apache Maven2 is needed to build the source. Find the Maven2 installation guide in http://maven.apache.org/download.html

Download the source package
```
cd /tmp
wget https://github.com/REANNZ/arcs-shibext/raw/master/download/arcs-shibext-1.5.5-src.tar.gz
tar xzvf arcs-shibext-1.5.5-src.tar.gz
cd arcs-shibext-1.5.5
```
Build with maven:
```
mvn package
```
Remove the old version if any
```
rm $IDP_SRC_HOME/src/main/edit-webapp/WEB-INF/lib/arcs-shibext-*.jar
rm $IDP_SRC_HOME/lib/arcs-shibext-*.jar
```
Copy the jar
```
cp target/arcs-shibext-1.5.5.jar $IDP_SRC_HOME/src/main/edit-webapp/WEB-INF/lib
cp target/arcs-shibext-1.5.5.jar $IDP_SRC_HOME/lib
```
Run IdP install script again. '''(make sure to answer NO to "DO you want to overwrite your configuration")'''
```
cd $IDP_SRC_HOME
./install.sh
```
## Configure IdP

* Configure $IDP_HOME/conf/attribute-resolver.xml
  * Add schema arcs-shibext-dc.xsd

                   xsi:schemaLocation="...
                                       urn:mace:arcs.org.au:shibboleth:2.0:resolver:dc classpath:/schema/arcs-shibext-dc.xsd">

  * Define the connector

        <!-- ==================== auEduPersonSharedToken data connector ================== -->

        <DataConnector xsi:type="st:SharedToken" xmlns:st="urn:mace:arcs.org.au:shibboleth:2.0:resolver:dc"
                            id="sharedToken"
                            sourceAttributeID="uid"
                            salt="ThisIsRandomText">
            <InputDataConnector ref="myLDAP" attributeNames="uid"/>
        </DataConnector>
    * Attributes
      * `id`: a unique identifier for the data connector.
      * `sourceAttributeID`: a set of attributes (comma separated) from the dependency used for computing the sharedToken value. It must be a unique and non-reassigned value.
      * `generatedAttributeID`: a name generated by the connector. optional, defaults to auEduPersonSharedToken.
      * `idpIdentifier`: an identifier of the IdP, used for computing the sharedToken. optional, defaults to IdP entityID. Note: this attribute is not recommended to set unless you really need to.
      * `storeLdap`: a boolean value to indicate whether to persist the sharedToken in the depended Ldap. optional, defaults to true. '''Note: false means the sharedToken is generated on the fly which does not guarantee persistence and portability. Must not used in production environment.'''
      * `ldapConnectorId`: ID of the LDAPDataConnector to use if storing values in LDAP.<p/>Required with `storeLdap="true"`.<p/>With botn `storeLdap="false"` and `storeDatabase="false"` (value generated on the fly), the connector will attempt to fetch the value from LDAP first if `ldapConnectorId` is provided.
      * `storedAttributeName`: name of the LDAP attribute to use with the LDAP connector (for reading and writing).  Defaults to `"auEduPersonSharedToken"`
      * `storeDatabase`: a boolean value to indicate whether to persist the sharedToken in the database. optional, defaults to false. if set to true, storeLdap will be ignored. See the section [Database Support][1].
      * `databaseConnectionID`: reference to an existing DataSource bean to configure the database connection - also see section [Database Support][1].
      * `idpHome`: the path of IdP home directory. optional, defaults to the value configured in imast.properties.
      * `salt`: a string of random data; must be at least 16 characters. Be sure to write down this salt value somewhere safe so that the sharedToken are not lost if you delete your configuration file! Here is an example to get the salt with openssl:

            openssl rand -base64 36 2>/dev/null
    * Element
      * `InputAttributeDefinition`
        * the attribute specified by sourceAttributeID
      * `InputDataConnector`:
        * the Ldap data connector where the sharedToken is written to.
  * Define the attribute 

        <!-- ==================== auEduPersonSharedToken attribute definition ================== -->

        <AttributeDefinition id="auEduPersonSharedToken" xsi:type="Simple">
            <InputDataConnector ref="sharedToken" attributeNames="auEduPersonSharedToken"/>
            <AttributeEncoder xsi:type="SAML1String" name="urn:mace:federation.org.au:attribute:auEduPersonSharedToken" />
            <AttributeEncoder xsi:type="SAML2String" name="urn:oid:1.3.6.1.4.1.27856.1.2.5" friendlyName="auEduPersonSharedToken" />
        </AttributeDefinition>
    * `InputDataConnector`
      * `sourceAttributesID`: the ID of the attribute used for the sharedToken in the data connector. It must match the `generatedAttributeID` in the data connector.
      * `ref`: the sharedToken data connector id.
* Configure $IDP_HOME/conf/attribute-filter.xml to release the auEduPersonSharedToken
* Restart Tomcat server

## Configure Ldap Server

Import auEduPersonSharedToken Schema to Ldap if you haven't done so. Download the attached schema file and put it into ldap schema directory. Take openldap for example:
```
$ cd /etc/openldap/schema
$ wget https://wiki.caudit.edu.au/confluence/download/attachments/4587731/auEduPerson.schema?version=1
```
Edit slapd.conf and add the following line:
```
include         /etc/openldap/schema/auEduPerson.schema
```
You will also need to give write access to the auEduPersonSharedToken attribute to the user you have configured in the attribute-resolver.xml file

Example slapd.conf ACL
```
# Allow write access of sharedToken to dn=proxyuser,dc=arcs,dc=org,dc=au
access to attrs=auEduPersonSharedToken
        by dn="uid=proxyuser,dc=arcs,dc=org,dc=au" write
        by users read

# Allow write access to your own entry, read access to all
access to *
        by self write
        by users read
        by anonymous auth

```
Restart Ldap
```
$ service ldap restart
```

In every user's entry, add the attribute objectClass with the value "auEduPerson"

## Restart tomcat
Restart the Tomcat server again if your have restarted the Ldap server:
```
$ service tomcat5 restart
```

## Debug 

Edit $IDP_HOME/conf/logging.xml and add the following contents
```
    <!-- Logs ARCS Shibboleth Extension at DEBUG level -->
    <logger name="au.org.arcs.shibext">
        <level value="DEBUG" />
    </logger>
```
Set proper level. The log information will be output to the specified file, defaults to $IDP_HOME/logs/idp-process.log

Please remember the SharedToken is only generated when it does not exist. To regenerate it, you need remove the existing SharedToken from the user's Ldap entry.

The quick way to grab the related log information:
```
grep au.org.arcs $IDP_HOME/logs/idp-process.log
```

## Database Support

From this mdoule supports storing sharedToken values in a relational database.  This should be primarily used where it's not feasible to store the values directly into the institutional LDAP.

We assume the IdP is already configured with a DataSource bean - please see [IDP30 Storage Configuration][2].

To avoid duplication, instead of providing a means for defining a DataSource, this module will reuse a DataSource defined in the main IdP configuration.  Please pass the reference to the DataSource bean in the `databaseConnectionID` attribute.

 * Configuration Sample

Edit $IDP_HOME/conf/attribute-resolve.xml, set the attributes ```storeDatabase="true"``` ```databaseConnectionID="<ID of dataSource bean>"```. It may look like this:

```
    <!-- ==================== auEduPersonSharedToken data connector ================== -->

    <DataConnector xsi:type="st:SharedToken" xmlns:st="urn:mace:arcs.org.au:shibboleth:2.0:resolver:dc"
                        id="sharedToken"
                        sourceAttributeID="uid"
                        salt="ThisIsRandomText"
                        storeDatabase="true"
                        databaseConnectionID="shibboleth.JPAStorageService.DataSource"
                        >
        <InputDataConnector ref="myLDAP" attributeNames="uid"/>
        
    </DataConnector>
```

 * Set up the database. 
It's been tested under MySQL, but should also works with other types of database system. Sample Database Scripts:

```
CREATE USER 'idp_admin'@'localhost' IDENTIFIED BY 'idp_admin';
CREATE DATABASE idp_db;
GRANT SELECT,INSERT ON idp_db.tb_st to 'idp_admin'@'localhost';

CREATE TABLE tb_st (
uid VARCHAR(100) NOT NULL,
sharedToken VARCHAR(50),
PRIMARY KEY  (uid)
);
```
[1]: #database-support "Database Support"
[2]: https://wiki.shibboleth.net/confluence/display/IDP30/StorageConfiguration "IDP30 StorageConfiguration"
