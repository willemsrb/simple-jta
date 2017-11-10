# Simple JTA [![Build Status](https://travis-ci.org/willemsrb/simple-jta.svg?branch=master)](https://travis-ci.org/willemsrb/simple-jta) [![Quality Gate](https://sonarqube.com/api/badges/gate?key=nl.future-edge:simple-jta)](https://sonarcloud.io/dashboard?id=nl.future-edge%3Asimple-jta) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/nl.future-edge/simple-jta/badge.svg)](https://maven-badges.herokuapp.com/maven-central/nl.future-edge/simple-jta)
*Simple JTA transaction manager, without all the bells and whistles*

Sometimes you just need to have a database and message queue share in a single transaction. Wouldn't you wish that was a simple thing? Sometimes you would add a simple component to your application and you would have to complement your backup strategy, because that component saves stuff directly as files. Do you wish you had a choice in that?

Just for that purpose, Simple JTA was created.

## What does it do?
Simple JTA, is just that: a simple JTA transaction manager. It does not come with any bells and whistles, it just does the job it was designed to do: it manages the transactions for a single (local) application (component).

### What is not supported?
Because Simple JTA just implements the base requirements some features have been skipped:
- Enlisting arbitrary (eg. not via the Simple JTA datasource or connection factory adapters) XA resources (`javax.transaction.Transaction#enlistResource`) is not supported.
- Nested transactions (`javax.transaction.TransactionManager#begin` when another transaction is already started) are not supported
- JMS (durable) connection consumers (`javax.jms.Connection#createConnectionConsumer` and `javax.jms.Connection#createDurableConnectionConsumer`) are not supported

### Undocumented features
The following undocumented features (or expected inner workings) were introduced during testing:
- Reusing physical database connections within a transaction; when `java.sql.Connection#close` has been called and the transaction has not been ended, a call to `java.sql.DataSource#getConnection` will 're-open' and return the same database connection.
- Joining JTA resources (`javax.transaction.xa.XAResource#start(Xid,TMJOIN)`) is disabled by default

## How does it work?
Simple JTA is designed to work in a [Spring](https://spring.io) powered application. The transaction manager is started as a simple bean and the XA resources (database or messaging) are wrapped using simple beans. From there on you can just configure the Spring transaction manager and use the Spring transaction code!

### Configuring the Simple JTA transaction manager
The Simple JTA transaction manager needs two things configured to be able to work correctly:
1. Unique name(s) to identify it (and the XA resources) and distinguish its transactions; without a unique name two different transaction managers could use the same transaction id and a XA resource (database or messaging store) would become tangled up. When doing recovery after a crash event it would use that unique name to determine if partial transactions should be committed or rolled back.
2. A transaction store to 'stably' store transaction information; without a guaranteed store of transaction information a transaction manager would never be able to reliably determine the transaction status when doing recovery.
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <bean name="simpleJtaTransactionManager" class="nl.futureedge.simple.jta.JtaTransactionManager">
        <property name="uniqueName" value="test"/>
        <property name="jtaTransactionStore">
            <bean class="nl.futureedge.simple.jta.store.jdbc.JdbcTransactionStore">
                <property name="create" value="true"/>
                <property name="url" value="jdbc:hsqldb:hsql://localhost:${test.database.port}/trans"/>
                <property name="user" value="sa"/>
                <property name="password" value=""/>
            </bean>
        </property>
    </bean>

    <!-- Spring transaction manager -->
    <bean name="transactionManager" class="org.springframework.transaction.jta.JtaTransactionManager">
        <property name="transactionManager" ref="simpleJtaTransactionManager" />
    </bean>
</beans>
```

The transaction store of choice for Simple JTA is a database. Why? Because all (our) applications already have a fully functioning, reliable and properly backed up database any way. We don't want to complicate thing by having a set of files (on each application server) that needs to be backed up (in sync) with that.


##### nl.futureedge.simple.jta.JtaTransactionManager properties
| Property | Explanation | Required |
|---|---|---|
| uniqueName | The unique name to use for this transaction manager | Yes |
| jtaTransactionStore | Transaction store to 'stably' store transaction information | Yes (Autowired) |

##### nl.futureedge.simple.jta.store.jdbc.JdbcTransactionStore properties
| Property | Explanation | Required |
|---|---|---|
| create | If true, the transaction store will try to create the database objects on startup | No (default false) |
| driver | The JDBC Driver classname to load (not needed for JDBC 4.0 drivers) | No |
| url | The JDBC url to connect to the database | Yes |
| user | The username to use when connecting to the database | No |
| password | The password to use when connecting to the database | No |
| sqlTemplate | SQL template to use; if left empty the transaction store will try to detect the database type based on the JDBC url and else use a SQL-2003 compatible default | No |
| storeAll | If true, the transaction store will record all transaction states; else, the store will only record the minimum state | No (default false) |

##### nl.futureedge.simple.jta.store.file.FileTransactionStore properties
| Property | Explanation | Required |
|---|---|---|
| baseDirectory | The base directory for the transaction logs | Yes |
| storeAll | If true, the transaction store will record all transaction states; else, the store will only record the minimum state | No (default false) |

### Configuring the database connection
A JDBC datasource is not suited to participate in distributed (JTA) transactions. To handle that an application needs to use a JDBC XA datasource. However, most application frameworks only work on datasources and not on XA datasources. Fortunately a JDBC XA datasource only exposes some methods that only need to be called by the transaction manager and ultimately exposes a 'normal' JDBC connection. Therefor Simple JTA provides an adapter that wraps a XA datasource, handles the transaction manager methods and exposes a 'normal' JDBC datasource.
```
    <!-- Vendor provided XA DataSource -->
    <bean name="xaDataSource" class="org.hsqldb.jdbc.pool.JDBCXADataSource">
        <property name="url" value="jdbc:hsqldb:hsql://localhost:${test.database.port}/test"/>
        <property name="user" value="sa"/>
        <property name="password" value=""/>
    </bean>

    <!-- Simple JTA DataSource wrapper -->
    <bean name="dataSource" class="nl.futureedge.simple.jta.jdbc.XADataSourceAdapter">
        <property name="uniqueName" value="database1" />
        <property name="xaDataSource" ref="xaDataSource" />
        <property name="supportsJoin" value="false" />
        <property name="supportsSuspend" value="false" />
        <property name="allowNonTransactedConnections" value="warn" />
    </bean>
```

##### nl.futureedge.simple.jta.jdbc.XaDataSourceAdapter properties
| Property | Explanation | Required |
|---|---|---|
| uniqueName | The unique name to use for this resource manager | Yes |
| xaDataSource | The vendor provided XA DataSource to adapt | Yes |
| jtaTransactionManager | The JtaTransactionManager this datasource is managed by (for recovery) | Yes (Autowired) |
| supportsJoin | Set to true if this resource correctly supports joining partial transactions | No (default false) |
| supportsSuspend | Set to true if this resource supports transaction suspension | No (default false) |
| allowNonTransactedConnections | Allow connections outside a transaction (yes, no or warn) | No (default warn) |

##### Enlisting the resource
The XA Resource is enlisted in the transaction when `DataSource#getConnection` is called to open a connection.
When a connection is requested 'outside' a transaction the adapter will return an unmanaged connection. A connection is closed when to transaction is committed or rolled back


### Configuring the messaging connection
As with the database connection a JMS connection factory is not suited to participate in distributed (JTA) transactions. Simple JTA provides an adapter that wraps a JMS XA connection factory, handles the transaciton manager methods and exposes a 'normal' JMS connection factory.
```
    <!-- Vendor provided XA ConnectionFactory -->
    <bean name="xaConnectionFactory" class="org.apache.activemq.ActiveMQXAConnectionFactory">
        <property name="brokerURL" value="tcp://localhost:${test.broker.port}"/>
    </bean>

    <!-- Simple JTA ConnectionFactory wrapper -->
    <bean name="connectionFactory" class="nl.futureedge.simple.jta.jms.XAConnectionFactoryAdapter">
        <property name="uniqueName" value="message1" />
        <property name="xaConnectionFactory" ref="xaConnectionFactory" />
        <property name="supportsJoin" value="false" />
        <property name="supportsSuspend" value="false" />
    </bean>
```

##### nl.futureedge.simple.jta.jms.XAConnectionFactoryAdapter properties
| Property | Explanation | Required |
|---|---|---|
| uniqueName | The unique name to use for this resource manager | Yes |
| xaConnectionFactory | The vendor provided XA ConnectionFactory to adapt | Yes |
| jtaTransactionManager | The JtaTransactionManager this datasource is managed by (for recovery) | Yes (Autowired) |
| supportsJoin | Set to true if this resource correctly supports joining partial transactions | No (default false) |
| supportsSuspend | Set to true if this resource supports transaction suspension | No (default false) |

##### Enlisting the resource
The XA Resource is enlisted in the transaction when `Connection#createSession` is called to create a session with the argument `transacted` set to `true`.
When a session is created with the argument `transacted` set to `false` an unmanaged session will be returned; when a session is created with the argument `transacted` set to `true` outside a transaction the connection will throw an exception. 

### Configuring using the Simple JTA namespace
Using the simple-jta namespace this spring configuration can be compressed considerably:
```
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:simple-jta="http://www.future-edge.nl/schema/simple/jta"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
                           http://www.future-edge.nl/schema/simple/jta http://www.future-edge.nl/schema/simple/jta/simple-jta.xsd">

    <!-- TransactionManager -->
    <simple-jta:transaction-manager name="transactionManager" unique-name="test">
        <simple-jta:jdbc-transaction-store create="true" driver="org.hsqldb.jdbc.JDBCDriver"
                                           url="jdbc:hsqldb:hsql://localhost:${test.database.port}/trans" user="sa" password=""
                                           store-all-states="true" />
    </simple-jta:transaction-manager>

    <!-- DataSource -->
    <bean name="xaDataSource" class="org.hsqldb.jdbc.pool.JDBCXADataSource">
        <property name="url" value="jdbc:hsqldb:hsql://localhost:${test.database.port}/test"/>
        <property name="user" value="sa"/>
        <property name="password" value=""/>
    </bean>

    <simple-jta:data-source name="dataSource" unique-name="database1" xa-data-source="xaDataSource"
                            supports-join="false" supports-suspend="false" allow-non-transacted-connections="warn"/>

    <!-- ConnectionFactory -->
    <bean name="xaConnectionFactory" class="org.apache.activemq.ActiveMQXAConnectionFactory">
        <property name="brokerURL" value="tcp://localhost:${test.broker.port}"/>
    </bean>

    <simple-jta:connection-factory name="connectionFactory" unique-name="message1" xa-connection-factory="xaConnectionFactory"
                                   supports-join="false" supports-suspend="false" />
</beans>
```

### Transaction suspension
A transaction can be suspended by calling `TransactionManager#suspend`; the transaction can be resumed by calling `TransactionManager#resume` with the transaction received from the `suspend` method.

*note: resources cannot be reused between suspended transactions; Simple JTA only calls end(TMSUSPEND) and start (TMRESUME) to preserve resources on the server-side.*

##### Enlisting resources
When a transaction has been suspended and a new transaction 


### Distributed transactions and recovery
How does the one and only responsibility of Simple JTA work?
- Whenever an action is taken that involves the transaction manager (eg. using a database connection, using a messaging connection, calling commit, calling rollback), it is stored in the transaction store using the (global or branch) transaction id and unique resource name.
- When commit is requested: first all enlisted XA resources are asked to prepare its partial transaction; on success, the transaction manager stores its decision to commit (this signifies the final decision and cannot be changed) and all XA resources are asked to commit; on failure all XA resources are asked to rollback.
- When rollback is requested: the transaction manager stores its decision to rollback and all XA resources are asked to rollback

The one and only reason to store the transaction info is to be able to reliably recover from a failure. During the startup of the application, whenever a XA resource is 'adapted' the recovery protocol is executed after the datasource is configured. The recovery protocol executes the following steps:
- Retrieve the list of prepared (partial) transactions from the XA resource
- Filter the list using the unique name of the transaction manager to identify the (partial) transactions to handle
- Determine the state of each (partial) transaction (has the transaction manager decided to commit this transaction?) and process accordingly:
    - if committing, then commit the partial transaction
    - else, rollback the partial transaction
- Store the 'new ' (partial) transaction information using the unique resource name.

After recovery the transaction store is cleaned; fully committed or rolledback transactions are removed from the store.

#### Transaction status information
The following states are always stored for transactions (globally or per branch, where a branch is specific for a XA resource):

| Global / Branch | State | Explanation |
|---|---|---|
| Global | PREPARING | Identification of the global transaction |
| Branch | PREPARED | This locks the changes to the resource |
| Global | COMMITTING | **The decision to commit the transaction** |
| Branch | COMMITTED | Signifies the changes in the resource have been succesfully committed |
| Global (*)| COMMITTED | The transaction has completed succesfully by committing the changes |
| Global (\*\*) | ROLLING_BACK | **The decision to rollback the transaction** |
| Branch (\*\*) | ROLLED_BACK| Records the decision to rollback the transaction |
| Global (*) | ROLLED_BACK| The transaction has completed succesfully by rolling back the changes |
| Both  | COMMIT_FAILED | PROTOCOL ERROR: Always recorded, a resource could not be committed after succesfully preparing |
| Both  | ROLLBACK_FAILED | PROTOCOL ERROR: Always recorded, a resource could not be rolled back |
| (*) | | Actually removes all transaction information from the store |
| (\*\*) | | Only stored when the transaction had started preparing (on recovery an unknown transaction is always rolled back) |

*Note: The COMMIT_FAILED and ROLLBACK_FAILED should not happen as the XA protocol in theory does not allow it (a resource should always be able to commit after successfully preparing and a resource should always be able to rollback).*


When the transaction store is configured to record all states the following are stored:

| State | Global | Branch (per resource) |
|---|---|---|
| ACTIVE | A transaction is started | A resource is enlisted in the transaction |
| PREPARING | Before preparing resources | Before preparing the resource |
| PREPARED | After all resources have been prepared successfully | After the resource has been prepared successfully |
| COMMITTING | **The decision to commit the transaction** | Before committing the resource |
| COMMITTED | After all resources have been committed | After the resource has been committed successfully |
| COMMIT_FAILED | PROTOCOL ERROR: Unexpected failure| PROTOCOL ERROR: a resource could not be committed after succesfully preparing |
| ROLLING_BACK | **The decision to rollback the transaction** | Before rolling back the resource |
| ROLLED_BACK | After all resources have been rolled back | After the resource has been rolled back successfully |
| ROLLBACK_FAILED | PROTOCOL ERROR: Unexpected failure | PROTOCOL ERROR: a resource could not be rolled back |

## Further reading
- [Distributed Transaction Processing: The XA Specification](http://pubs.opengroup.org/onlinepubs/009680699/toc.pdf)
- [Java(TM) Transaction API (JTA) Specification 1.0.1](http://download.oracle.com/otndocs/jcp/7286-jta-1.0.1-spec-oth-JSpec/)
