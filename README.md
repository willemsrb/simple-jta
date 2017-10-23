# Simple JMX [![Build Status](https://travis-ci.org/willemsrb/simple-jmx.svg?branch=master)](https://travis-ci.org/willemsrb/simple-jmx) [![Quality Gate](https://sonarqube.com/api/badges/gate?key=nl.future-edge:simple-jmx)](https://sonarqube.com/dashboard/index?id=nl.future-edge%3Asimple-jmx) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/nl.future-edge/simple-jmx/badge.svg)](https://maven-badges.herokuapp.com/maven-central/nl.future-edge/simple-jmx)
*Simple JMX protocol that works without RMI.*

Have you ever created a java application running in a Docker container and wanted to connect using JMX? Have you ever struggled configuring JMX to work through a firewall? Then you have probably cursed the RMI protocol for sending a server and a data port to the client, slamming into your firewall or completely ignoring your Docker port mapping.

Simple JMX solves this problem by using a simple protocol to support JMX. Simple JMX does not require any data channel or callback to work and only opens a connection directly to the server port.

## Usage
Simple JMX implements the JMX server and client provider interfaces and implements the `simple` procotol. Clients can, as an example, connect using the standaard JMX Connector factory `javax.management.remote.JMXConnectorFactory` and a JMX service url `service:jmx:simple://localhost:3481` to locate the server. A server connector can be instantiated using the standaard JMX Server Connector Factory `javax.management.remote.JMXConnectorServerFactory` and a JMX servier url `service:jmx:simple://0.0.0.0:0` to configure which interface and port (0 for any free emphemeral port) should be used.

## Configuration
JMX can be fully configured and extended using the environment supplied to the (server) connector.

### Authentication (server-side only)
Authentication is done through a JAAS login module. Simple JMX supports three authentication schemes out of the box:
- Static authentication (all credentials are accepted) ****default***\* 
- Properties based authentication (credentials are stored as username=password)
- Externally configured login module

Any authentication on the server must be configured using the `jmx.remote.authenticaton` key in the environment; the value should be an object instance that implements the `javax.management.remote.JMXAuthenticator` interface:

- The static authentication implementation (`nl.futureedge.simple.jmx.authenticator.StaticAuthenticator`) can optionally have static principals (passed to the constructor) that will be added to all subjects.
- The properties based implementation (`nl.futureedge.simple.jmx.authenticator.PropertiesAuthenticator`) must be configured by passing a `java.util.Properties` to the constructor or a `java.lang.String` that identifies the location of the properties file on the file system. The property keys identify the usernames and the property values identify the passwords. No encryption or hasing is supported; so only use this implementation for development purposes. When authenticated succesfully a `javax.management.remote.JMXPrincipal` containing the username will be added to the subject.
- The external configuration implementation (`nl.futureedge.simpl.jmx.authenticator.ExternalAuthenticator`) must be configured by passing a `java.lang.String` that identifies the JAAS configuration to use.
- Applications can also implement the `javax.management.remote.JMXAuthenticator` interface instead of using the supplied implementations to fully customize their authentication.

A client should supply credentials via the `jmx.remote.credentials` key in the environment. The credentials (for the default implementations) should be configured as a `String[]` with two elements; the first being the username and the second the password.

### Authorization (server-side only)
Authorization is done using the authenticated subject. Simple JMX supports three authentication schemes out of the box:
- Default authentication (gives access to all readonly and notification operations) ****default***\*
- All access authentication (unlimited access)
- Properties file based access control (readonly, readwrite, create and unregister access can be configured in a property file as princpal=access)

Any authorization on the server must be configured using the `jmx.remote.accesscontroller` key in the environment; the value should be an object instance that implements the `nl.futureedge.simple.jmx.access.JMXAccessController` interface:

- The default authentication implementation (`nl.futureedge.simple.jmx.access.DefaultAccessController`) does not have any specific configuration.
- The all access authentication implementation (`nl.futureedge.simple.jmx.access.AllAccessController`) does not have any specific configuration.
- The properties based implementation (`nl.futureedge.simple.jmx.access.PropertiesAccessController`) must be configured by passing a `java.util.Properties` to the constructor or a `java.lang.String` that identifies the location of the properties file on the file system.  The property keys identify the principal name that should be used to check the authentication. The property values identify the access granted to that principal. Access is configured using the `readonly` or `readwrite` keywords. `readwrite` access can be further extended by using the `create my.package.*` or `unregister` keywords.
- Applications can also implement the `nl.futureedge.simple.jmx.access.JMXAccessController` interface instead of using the supplied implementations to fully customize their access control.

### Connections (client-side and server-side)
Connections are made via standaard Java sockets. Simple JMX provides two connection provider out of the box:
- Anonymous SSL (allows operation without certificates) ****default***\*
- System SSL (uses the default Java SSL provider)

Any connection configuration (on the client or the server) must be configured using the `jmx.remote.socketfactory` key in the environment; the value should be an object instance that implements the `nl.futureedge.jmx.socket.JMXSocketFactory` interface:

- The anonymous SSL implementation (`nl.futureedge.simple.jmx.socket.AnonymousSslSocketFactory`) does not have any specific configuration.
- The system SSL implementation (`nl.futureedge.simple.jmx.socket.SystemSslSocketFactory`) uses the system default SSLContext and should be configured using the [system configuration](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/JSSERefGuide.html).
- Applications can also implement the `nl.futureedge.simple.jmx.socket.JMXSocketFactory` interface to fully customize their connections.

When a client issues a request via the JMX connection a timeout is used to determine a timely response (default is 15 seconds). Configure the timeout using the `jmx.remote.requesttimeout` key in the environment.

Threads created by the server components are given 'normal' priority by default. Configure the priority using the `jmx.remote.threadpriority` key in the environment.

## Examples
Simple JMX can be started via any means that can start and configure (set an environment) a standaard JMX (Server) Connector. See the following examples:

- [Pure Java](example-java.md)
- [Spring](example-spring.md)
- [Java Agent](example-agent.md) (server connector)
- [JConsole](example-jconsole.md) (client connector)

