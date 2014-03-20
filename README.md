# MIRROR Spaces SDK for Java
This SDK provides high-level interfaces for the services of the [MIRROR Spaces Framework (MSF)][1]. It allows developers without deeper knowledge of the XMPP protocol to connect their java applications to the MSF with only a few lines of code.

The implementation is build on top of the [Smack XMPP library][2].

## Build and Deploy
To build the SDK, run the build task with ant:

    $ ant build

To integrate the SDK in your application, add the following JARs provided in the "dist" directory to your classpath:

    jdom-2.0.5.jar
    smack-3.3.1-MODIFIED.jar
    smackx-3.3.1.jar
    spaces-sdk-api-1.3.jar
    spaces-sdk-java-1.3.1.jar

The database connectors are only required if you want to persist you local cache, see [DatabaseConfig][3] for details.

## Usage
The SDK for Java implements the [Space SDK API][4]. Three major handlers are provided to use the MSF:

1. **[ConnectionHandler][5]**
  A handler to establish and manage the connection to the XMPP server.
2. **[SpaceHandler][6]**
  This handler provides a set of methods to manage the spaces you can access.
  There are methods for the access, creation, management, and deletion of
  spaces. For space management operations the application has to be online.
  Retrieving space information is also possible in offline mode, as long as the
  information is available in the handler's cache. 
3. **[DataHandler][7]**
  The data handler provides methods to register for notifications about newly
  published data, and with methods to send and retrieve it. All items published
  and received while being online are stored and therefore also available when
  the handler is offline. Additionally, the allows queries to the [persistence service][8].

Details about the usage of the handlers are available of the SDK API documentation.

The complete API documentation for the Java SDK is available here:
http://docs.mirror-demo.eu/spaces-sdk/java/1.3.1/

The general API description is also available:
http://docs.mirror-demo.eu/spaces-sdk/api/1.3/

## License
The Spaces SDK for Java is provided under the [Apache License 2.0][9].
License information for third party libraries is provided with the related JAR files.

## Changelog

v1.3.1 - March 20, 2014

* [NEW] Added experimental (non-API) call for synchronous publishing of data objects: DataHandler.publishAndRetrieveDataObject().
* [FIX] Fixed bug causing a NullPointerException thrown if data is retrieved from the pubsub node.
* [FIX] Minor fixes.

v1.3.0 - January 10, 2014

* [NEW] Implements Spaces SDK API 1.3.
* [NEW] Full support for CDM 2.0.
* [FIX] Fixed usage of XML namespaces.
* [FIX] Fixed a bug in DataObjectBuilder that prevented added elements to be parsed correctly.
* [UPDATE] Updated to Smack 3.3.1.
* [UPDATE] Updated to JDOM 2.0.5.
 
v1.2.2 - May 28, 2013

* [FIX] Fixed a bug causing a NullPointerException when the DataHandler is initialized before the connection to the server is established.
* [NEW] Added JAR compiled with debug information.

v1.2.1 - April 19, 2013

* [FIX] The data handler now handles data objects without identifier correctly.
* [FIX] OrgaSpace: The list of supported data models is now returned correctly. 

v1.2.0 - April 15, 2013

* [FIX] DataObjectBuilder: Text content of a data object child element no longer requires to be valid XML.
* [FIX] Removed unnecessary requests when publishing data objects.
* [UPDATE] Accepts Interop Data Models as MIRROR Data Models.
* [NEW] Model classes (CDMData*, DataModel, DataObject, *Space, SpaceChannel, SpaceMember) are now serializable.
* [NEW] Added compatibility with MIRROR Spaces Service version 0.5.
* [NEW] Removed dependency for Simple XML framework. 

v1.1.0 - November 8, 2012

* Implements Spaces SDK API 1.1.
* Added XMPP connection handler.
* Several updates to existing handlers.

v1.0.0 - October 26, 2012

* Initial version.
* Implements MIRROR Spaces Service 0.4.x.

  [1]: https://github.com/MirrorIP
  [2]: http://www.igniterealtime.org/projects/smack/
  [3]: http://docs.mirror-demo.eu/spaces-sdk/java/1.3.1/index.html?de/imc/mirror/sdk/java/data/DatabaseConfig.html
  [4]: https://github.com/MirrorIP/msf-spaces-sdk-api
  [5]: http://docs.mirror-demo.eu/spaces-sdk/java/1.3.1/index.html?de/imc/mirror/sdk/java/ConnectionHandler.html
  [6]: http://docs.mirror-demo.eu/spaces-sdk/java/1.3.1/index.html?de/imc/mirror/sdk/java/SpaceHandler.html
  [7]: http://docs.mirror-demo.eu/spaces-sdk/java/1.3.1/index.html?de/imc/mirror/sdk/java/DataHandler.html
  [8]: https://github.com/MirrorIP/msf-persistence-service
  [9]: http://www.apache.org/licenses/LICENSE-2.0.html