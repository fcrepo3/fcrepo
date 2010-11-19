Fedora 3.4 Spring Configuration
==== Notes ====================
Fedora will attempt to parse all files with a .xml extension in this directory
as Spring bean definitions.  The application context for these definitions
should run basic lifecycle, annotation, & context-aware bean post-processors.

It should be noted that Fedora expects org.fcrepo.server.Module beans to have
their role indicated in their id/name attributes.  Also, any configured module
is expected to have a corresponding configuration bean of a class that
is/extends org.fcrepo.server.config.ModuleConfiguration.

To support these definitions, some convenience methods have been added- See
the examples below.  It is also worth noting that if the default
ResourceIndexModule is confgured without a "datastore" parameter, it will
attempt to load an org.trippi.TriplestoreConnector from a bean of the same
name.

==== Module Example ============

<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="http://www.springframework.org/schema/beans 
        http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
        http://www.springframework.org/schema/context 
        http://www.springframework.org/schema/context/spring-context-3.0.xsd">
<bean id="org.fcrepo.server.storage.DOManagerConfiguration"
 name="org.fcrepo.server.storage.DOManagerConfiguration"
 class="org.fcrepo.server.config.ModuleConfiguration"
 autowire-candidate="true">
<constructor-arg index="0">
<list>
<bean class="org.fcrepo.server.config.Parameter">
<constructor-arg type="java.lang.String" value="pidNamespace" />
<property name="value" value="changeme" />
</bean>
<bean class="org.fcrepo.server.config.Parameter">
<constructor-arg type="java.lang.String" value="storagePool" />
<property name="value" value="localDerbyPool" />
</bean>
<bean class="org.fcrepo.server.config.Parameter">
<constructor-arg type="java.lang.String" value="storageCharacterEncoding" />
<property name="value" value="UTF-8" />
</bean>
<bean class="org.fcrepo.server.config.Parameter">
<constructor-arg type="java.lang.String" value="defaultExportFormat" />
<property name="value" value="info:fedora/fedora-system:FOXML-1.1" />
</bean>
<bean class="org.fcrepo.server.config.Parameter">
<constructor-arg type="java.lang.String" value="gSearchRESTURL" />
<property name="value" value="http://localhost:8080/fedoragsearch/rest" />
</bean>
<bean class="org.fcrepo.server.config.Parameter">
<constructor-arg type="java.lang.String" value="gSearchUsername" />
<property name="value" value="exampleUsername" />
</bean>
<bean class="org.fcrepo.server.config.Parameter">
<constructor-arg type="java.lang.String" value="gSearchPassword" />
<property name="value" value="examplePassword" />
</bean>
</list>
</constructor-arg>
<constructor-arg index="1" type="java.lang.String" value="org.fcrepo.server.storage.DOManager" />
<constructor-arg index="2" type="java.lang.String" value="org.fcrepo.server.storage.DefaultDOManager" />
<constructor-arg index="3" type="java.lang.String" value="The interface to the storage subsystem." />
</bean>

<bean id="org.fcrepo.server.storage.DOManager"
 name="org.fcrepo.server.storage.DOManager"
 class="org.fcrepo.server.storage.DefaultDOManager"
 destroy-method="shutdownModule" init-method="initModule" autowire-candidate="true" lazy-init="true">
<constructor-arg index="0">
  <bean factory-bean="org.fcrepo.server.storage.DOManagerConfiguration" factory-method="getParameters" />
</constructor-arg>
<constructor-arg index="1" ref="org.fcrepo.server.Server" />
<constructor-arg index="2">
  <bean factory-bean="org.fcrepo.server.storage.DOManagerConfiguration" factory-method="getRole" />
</constructor-arg>
</bean>
</beans>

==== Triplestore Example =======

<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="http://www.springframework.org/schema/beans 
        http://www.springframework.org/schema/beans/spring-beans-3.0.xsd
        http://www.springframework.org/schema/context 
        http://www.springframework.org/schema/context/spring-context-3.0.xsd">
<bean id="org.trippi.TriplestoreConnector"
 name="org.trippi.TriplestoreConnector"
 class="org.trippi.impl.mulgara.MulgaraConnector"
 destroy-method="close" autowire-candidate="true">
<property name="configuration">
    <map>
      <entry>
        <key><value>remote</value></key>
        <!-- Tells the connector to communicate with Mulgara in remote or 
			local mode. If true, the host parameter must be defined. If false,
			the path parameter must be defined.
         -->
        <value>false</value>
      </entry>
      <entry>
        <key><value>path</value></key>
        <!-- The local path to the main triplestore directory. -->
        <value>/C:/fedora-3.3/data/resourceIndex</value>
      </entry>
      <entry>
        <key><value>serverName</value></key>
        <!-- The server name for rmi binding. -->
        <value>fedora</value>
      </entry>
      <entry>
        <key><value>modelName</value></key>
        <!-- The name of the model to use. -->
        <value>ri</value>
      </entry>
      <entry>
        <key><value>poolInitialSize</value></key>
        <!-- The initial size of the session pool used for queries. 
			Note: A value of 0 will cause the Resource Index to operate in 
			synchronized mode: concurrent read/write requests are put in a queue 
			and handled in FIFO order; this will severely impair performance and 
			is only intended for debugging. -->
        <value>3</value>
      </entry>
      <entry>
        <key><value>poolMaxGrowth</value></key>
        <!-- Maximum number of additional sessions the pool may add. If 
			specified as -1, no limit will be placed on pool growth. -->
        <value>-1</value>
      </entry>
      <entry>
        <key><value>readOnly</value></key>
        <!-- Whether the triplestore should be read-only. Most Fedora 
  			repositories will set this to false.-->
        <value>false</value>
      </entry>
      <entry>
        <key><value>autoCreate</value></key>
        <!-- Create the model if it doesn&apos;t already exist. 
  			At startup, the model will be automatically created. In addition, an 
  			XML schema datatyped model named &quot;xsd&quot; will also be automatically 
  			created.-->
        <value>true</value>
      </entry>
      <entry>
        <key><value>autoTextIndex</value></key>
        <!-- Whether to propagate adds/deletes to a full-text 
  			[Full-Text] model automatically. While a very useful feature, 
  			enabling full-text indexing adds significantly to object ingest 
  			times. If true, the text model will be named modelName-fullText. 
  			Note that if this is true and autoCreate is true, the text model 
  			will also be created if it doesn&apos;t already exist.-->
        <value>false</value>
      </entry>
      <entry>
        <key><value>autoFlushDormantSeconds</value></key>
        <!-- Seconds of buffer inactivity that will trigger an 
  			auto-flush. If this threshold is reached, flushing will occur in the 
  			background, during which time the buffer is still available for 
  			writing.-->
        <value>5</value>
      </entry>
      <entry>
        <key><value>autoFlushBufferSize</value></key>
        <!-- The size at which the buffer should be auto-flushed. If 
  			this threshold is reached, flushing will occur in the background, 
  			during which time the buffer is still available for 
  			writing.-->
        <value>20000</value>
      </entry>
      <entry>
        <key><value>bufferFlushBatchSize</value></key>
        <!-- The number of updates to send to the triplestore at a time. 
  			This should be the same size as, or smaller than 
  			autoFlushBufferSize.-->
        <value>20000</value>
      </entry>
      <entry>
        <key><value>bufferSafeCapacity</value></key>
        <!-- The maximum size the buffer can reach before being forcibly 
  			flushed. If this threshold is reached, flushing will occur in the 
  			foreground and the buffer will be locked for writing until it is 
  			finished. This should be larger than autoFlushBufferSize.-->
        <value>40000</value>
      </entry>
    </map>
</property>
</bean>
</beans>
