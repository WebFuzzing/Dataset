# WFD

[![DOI](https://zenodo.org/badge/94008854.svg)](https://zenodo.org/badge/latestdoi/94008854)




Web Fuzzing Dataset (WFD):
a set of web/enterprise applications for scientific research in Software Engineering.

We collected several different systems running on the JVM, in different programming languages such as  Java and Kotlin.
In this documentation, we will refer to these projects as System Under Test (SUT).
Currently, the SUTs are either _REST_,  _GraphQL_ or _RPC_ APIs.

> This dataset was previously known as EMB. It was rebranded into WFD since version 4.0.0.

This collection of SUTs was originally assembled for easing experimentation with the fuzzer called [EvoMaster](http://evomaster.org).
However, finding this type of application is not trivial among open-source projects.
Furthermore, it is not simple to sort out all the technical details on how to set these applications up and start them in a simple, uniform approach.
Therefore, this repository provides the important contribution of providing all these necessary scripts for researchers that need this kind of case study.

__Black-box Testing__. For each SUT, we provide Docker Compose scripts (under the [dockerfiles](dockerfiles) folder) to start the APIs with all their needed dependencies (e.g., databases). APIs are configured with _mitmproxy_ and _JaCoCo_ to collect information on the fuzzing results.   

__White-box Testing__. For each SUT, we implemented _driver_ classes for _EvoMaster_ (currently the only existing white-box fuzzer for the JVM), which can programmatically _start_, _stop_ and _reset_ the state of SUT (e.g., data in SQL databases).
As well as enable setting up different properties in a _uniform_ way, like choosing TCP port numbers for the HTTP servers.
If a SUT uses any external services (e.g., a SQL database), these will be automatically started via Docker in these driver classes.

**NOTE**: version 1.6.1 was last one in which we still updated drivers for JavaScript and C\#. Those SUTs are not built anymore by default, and latest versions of *EvoMaster* might not work on those old drivers. Updating drivers for different programming languages (and re-implement white-box heuristics) is a massive amount of work, which unfortunately has little to no value for the scientific community (based on our experience). Those SUTs are still here in WFD to be able to replicate old experiments, but unfortunately not for *white-box* testing with latest versions of *EvoMaster*.



An old video (2023) providing some high level overview of EMB can be found [here](https://youtu.be/wJs34ATgLEw).

[![EMB YouTube Video](https://img.youtube.com/vi/wJs34ATgLEw/0.jpg)](https://www.youtube.com/watch?v=wJs34ATgLEw)



## License
All the code that is new for this repository (e.g., the driver classes) is released under Apache 2.0 license.
However, this repository contains as well sources from different open-source
projects, each one with its own license, as clarified in more details beneath.

## Example

To see an example of using these drivers with EvoMaster to generate test cases, you can look at this [short video](https://youtu.be/3mYxjgnhLEo) (5 minutes).

## Citation

If you are using WFD in an academic work, you can cite the following:

> O. Sahin, M. Zhang, A. Arcuri.
**WFC/WFD: Web Fuzzing Commons, Dataset and Guidelines to Support Experimentation in REST API Fuzzing**.
[arxiv 2509.01612](https://arxiv.org/abs/2509.01612)

For the old version still called EMB, you can refer to:

> A. Arcuri, M. Zhang, A. Golmohammadi, A. Belhadi, J. P. Galeotti, B. Marculescu, S. Seran.
 **EMB: A Curated Corpus of Web/Enterprise Applications And Library Support for Software Testing Research**.
In *IEEE International Conference on Software Testing, Validation and Verification (ICST)*, 2023.


## Current Case Studies

The projects were selected based on searches using keywords on GitHub APIs, using convenience sampling.
Several SUTs were looked at, in which we discarded the ones that would not compile, would crash at startup, would use obscure/unpopular libraries with no documentation to get them started, are too trivial, student projects, etc.
Where possible, we tried to prioritize/sort based on number of _stars_ on GitHub.
When authors of other fuzzers used some other open-source JVM APIs in their studies, we included them here into WFD.  


Note that some of these open-source projects might be no longer supported, whereas others are still developed and updated.
Once a system is added to WFD, we do not modify nor keep it updated with its current version under development.
The reason is that we want to keep an easy to use, constant set of case studies for experimentation that can be reliably used throughout the years.

The SUTs called _NCS_ (Numerical Case Study) and _SCS_ (String Case study) are artificial, developed by us.
They are based on numerical and string-based functions previously used in the literature of unit test generation.
We just re-implemented in different languages, and put them behind a web service.

For the RESTful APIs, each API has an endpoint where the OpenAPI/Swagger schemas can be downloaded from.
For simplicity, all schemas are also available as JSON/YML files under the folder [openapi-swagger](./openapi-swagger).

> **IMPORTANT**: More details (e.g., #LOCs and used databases) on these APIs can be found [in this table](statistics/table_emb.md).

Real-world APIs require authentication. 
How to setup authentication information, based on the current content of the initialized databases, is expressed in [Web Fuzzing Commons (WFC)](https://github.com/WebFuzzing/Commons) format. 
Auth configuration files can found in the [auth](auth) folder. 


### REST: Java/Kotlin (36)

* **Bibliothek** (MIT), [jdk_17_gradle/cs/rest/bibliothek](jdk_17_gradle/cs/rest/bibliothek), from [https://github.com/PaperMC/bibliothek](https://github.com/PaperMC/bibliothek)

* **Blog** (AGPL), [jdk_8_maven/cs/rest/original/blogapi](jdk_8_maven/cs/rest/original/blogapi), from [https://github.com/osopromadze/Spring-Boot-Blog-REST-API](https://github.com/osopromadze/Spring-Boot-Blog-REST-API)

* **CatWatch** (Apache), [jdk_8_maven/cs/rest/original/catwatch](jdk_8_maven/cs/rest/original/catwatch), from [https://github.com/zalando-incubator/catwatch](https://github.com/zalando-incubator/catwatch)

* **CWA-Verification-Server** (Apache), [jdk_11_maven/cs/rest/cwa-verification-server](jdk_11_maven/cs/rest/cwa-verification-server), from [https://github.com/corona-warn-app/cwa-verification-server](https://github.com/corona-warn-app/cwa-verification-server)

* **ERC20 Rest Service** (not-known license), [jdk_8_gradle/cs/rest/erc20-rest-service](jdk_8_gradle/cs/rest/erc20-rest-service), from [https://github.com/web3labs/erc20-rest-service](https://github.com/web3labs/erc20-rest-service)

* **Familie Ba Sak** (MIT), [jdk_17_maven/cs/rest/familie-ba-sak](jdk_17_maven/cs/rest/familie-ba-sak), from [https://github.com/navikt/familie-ba-sak](https://github.com/navikt/familie-ba-sak)

* **Features-Service** (Apache), [jdk_8_maven/cs/rest/original/features-service](jdk_8_maven/cs/rest/original/features-service), from [https://github.com/JavierMF/features-service](https://github.com/JavierMF/features-service)

* **Genome Nexus** (MIT), [jdk_8_maven/cs/rest-gui/genome-nexus](jdk_8_maven/cs/rest-gui/genome-nexus), from [https://github.com/genome-nexus/genome-nexus](https://github.com/genome-nexus/genome-nexus)

* **Gestao Hospital** (not-known license), [jdk_8_maven/cs/rest-gui/gestaohospital](jdk_8_maven/cs/rest-gui/gestaohospital), from [https://github.com/ValchanOficial/GestaoHospital](https://github.com/ValchanOficial/GestaoHospital)

* **HTTP Patch Spring** (MIT), [jdk_11_maven/cs/rest/http-patch-spring](jdk_11_maven/cs/rest/http-patch-spring), from [https://github.com/cassiomolin/http-patch-spring](https://github.com/cassiomolin/http-patch-spring)

* **Languagetool** (LGPL), [jdk_8_maven/cs/rest/original/languagetool](jdk_8_maven/cs/rest/original/languagetool), from [https://github.com/languagetool-org/languagetool](https://github.com/languagetool-org/languagetool)

* **Market** (MIT), [jdk_11_maven/cs/rest-gui/market](jdk_11_maven/cs/rest-gui/market), from [https://github.com/aleksey-lukyanets/market](https://github.com/aleksey-lukyanets/market)

* **Microcks** (Apache), [jdk_21_maven/cs/rest-gui/microcks](jdk_21_maven/cs/rest-gui/microcks), from [https://github.com/microcks/microcks](https://github.com/microcks/microcks)

* **NCS**, [jdk_8_maven/cs/rest/artificial/ncs](jdk_8_maven/cs/rest/artificial/ncs), (not-known license, artificial numerical examples coming from different sources)

* **News** (LGPL), [jdk_8_maven/cs/rest/artificial/news](jdk_8_maven/cs/rest/artificial/news), from [https://github.com/arcuri82/testing_security_development_enterprise_systems](https://github.com/arcuri82/testing_security_development_enterprise_systems)

* **OCVN** (MIT), [jdk_8_maven/cs/rest-gui/ocvn](jdk_8_maven/cs/rest-gui/ocvn), from [https://github.com/devgateway/ocvn](https://github.com/devgateway/ocvn)

* **Ohsome API** (AGPL-3.0), [jdk_17_maven/cs/rest/ohsome-api](jdk_17_maven/cs/rest/ohsome-api), from [https://github.com/GIScience/ohsome-api](https://github.com/GIScience/ohsome-api)

* **Payments Public API** (MIT), [jdk_11_maven/cs/rest/pay-publicapi](jdk_11_maven/cs/rest/pay-publicapi), from [https://github.com/alphagov/pay-publicapi](https://github.com/alphagov/pay-publicapi)

* **Person Controller** (Apache), [jdk_21_maven/cs/rest/person-controller](jdk_21_maven/cs/rest/person-controller), from [https://github.com/mongodb-developer/java-spring-boot-mongodb-starter](https://github.com/mongodb-developer/java-spring-boot-mongodb-starter)

* **Project Tracking System** (not-known license), [jdk_11_maven/cs/rest/tracking-system](jdk_11_maven/cs/rest/tracking-system), from [https://github.com/SelimHorri/project-tracking-system-backend-app](https://github.com/SelimHorri/project-tracking-system-backend-app)

* **ProxyPrint** (Apache), [jdk_8_maven/cs/rest/original/proxyprint](jdk_8_maven/cs/rest/original/proxyprint), from [https://github.com/ProxyPrint/proxyprint-kitchen](https://github.com/ProxyPrint/proxyprint-kitchen)

* **Quartz Manager** (Apache), [jdk_11_maven/cs/rest-gui/quartz-manager](jdk_11_maven/cs/rest-gui/quartz-manager), from [https://github.com/fabioformosa/quartz-manager](https://github.com/fabioformosa/quartz-manager)

* **Reservations API** (not-known license), [jdk_11_gradle/cs/rest/reservations-api](jdk_11_gradle/cs/rest/reservations-api), from [https://github.com/cyrilgavala/reservations-api](https://github.com/cyrilgavala/reservations-api)

* **Restcountries** (MPL), [jdk_8_maven/cs/rest/original/restcountries](jdk_8_maven/cs/rest/original/restcountries), from [https://github.com/apilayer/restcountries](https://github.com/apilayer/restcountries)

* **Scout-API** (MIT), [jdk_8_maven/cs/rest/original/scout-api](jdk_8_maven/cs/rest/original/scout-api), from [https://github.com/mikaelsvensson/scout-api](https://github.com/mikaelsvensson/scout-api)

* **SCS**, [jdk_8_maven/cs/rest/artificial/scs](jdk_8_maven/cs/rest/artificial/scs), (not-known license, artificial string examples coming from different sources)

* **Session Service** (not-known license), [jdk_8_maven/cs/rest/original/session-service](jdk_8_maven/cs/rest/original/session-service), from [https://github.com/cBioPortal/session-service](https://github.com/cBioPortal/session-service)

* **Spring-actuator-demo** (not-known license), [jdk_8_maven/cs/rest/original/spring-actuator-demo](jdk_8_maven/cs/rest/original/spring-actuator-demo), from [https://github.com/callicoder/spring-boot-actuator-demo](https://github.com/callicoder/spring-boot-actuator-demo)

* **Spring-batch-rest** (Apache), [jdk_8_maven/cs/rest/original/spring-batch-rest](jdk_8_maven/cs/rest/original/spring-batch-rest), from [https://github.com/chrisgleissner/spring-batch-rest](https://github.com/chrisgleissner/spring-batch-rest)

* **Spring Boot Restful API Example** (MIT), [jdk_17_maven/cs/rest/spring-rest-example](jdk_17_maven/cs/rest/spring-rest-example), from [https://github.com/phantasmicmeans/spring-boot-restful-api-example](https://github.com/phantasmicmeans/spring-boot-restful-api-example)

* **Spring ECommerce** (not-known license), [jdk_8_maven/cs/rest/original/spring-ecommerce](jdk_8_maven/cs/rest/original/spring-ecommerce), from [https://github.com/SaiUpadhyayula/SpringAngularEcommerce](https://github.com/SaiUpadhyayula/SpringAngularEcommerce)

* **Swagger Petstore** (Apache), [jdk_8_maven/cs/rest/original/swagger-petstore](jdk_8_maven/cs/rest/original/swagger-petstore), from [https://github.com/swagger-api/swagger-petstore](https://github.com/swagger-api/swagger-petstore)

* **Tiltaksgjennomføring** (MIT), [jdk_17_maven/cs/rest/tiltaksgjennomforing](jdk_17_maven/cs/rest/tiltaksgjennomforing), from [https://github.com/navikt/tiltaksgjennomforing-api](https://github.com/navikt/tiltaksgjennomforing-api)

* **User Management** (MIT), [jdk_8_maven/cs/rest/original/user-management](jdk_8_maven/cs/rest/original/user-management), from [https://github.com/andreagiassi/microservice-rbac-user-management](https://github.com/andreagiassi/microservice-rbac-user-management)

* **WebGoat** (GPL), [jdk_21_maven/cs/rest-gui/webgoat](jdk_21_maven/cs/rest-gui/webgoat), from [https://github.com/WebGoat/WebGoat](https://github.com/WebGoat/WebGoat)

* **YouTubeMock** (not-known license), [jdk_8_maven/cs/rest/original/youtube-mock](jdk_8_maven/cs/rest/original/youtube-mock), from [https://github.com/opensourcingapis/YouTubeMock](https://github.com/opensourcingapis/YouTubeMock)


### GraphQL: Java/Kotlin (5)

* **NCS**, [jdk_8_maven/cs/graphql/graphql-ncs](jdk_8_maven/cs/graphql/graphql-ncs), (not-known license, artificial numerical examples coming from different sources)

* **Patio-Api** (GPL), [jdk_11_gradle/cs/graphql/patio-api](jdk_11_gradle/cs/graphql/patio-api), from [https://github.com/patio-team/patio-api]()

* **SCS**, [jdk_8_maven/cs/graphql/graphql-scs](jdk_8_maven/cs/graphql/graphql-scs), (not-known license, artificial string examples coming from different sources)

* **Spring-Pet-Clinic** (Apache), [jdk_8_maven/cs/graphql/petclinic-graphql](jdk_8_maven/cs/graphql/petclinic-graphql), from [https://github.com/spring-petclinic/spring-petclinic-graphql]()

* **Timbuctoo** (GPL), [jdk_11_maven/cs/graphql/timbuctoo](jdk_11_maven/cs/graphql/timbuctoo), from [https://github.com/HuygensING/timbuctoo]()


### RPC (e.g.,Thrift and gRPC): Java

* **Signal-Registration** (not-known license), [jdk_17_maven/cs/grpc/signal-registration](jdk_17_maven/cs/grpc/signal-registration), from [https://github.com/signalapp/registration-service]()

* **NCS** (not-known license, artificial numerical examples coming from different sources).
 Thrift: [jdk_8_maven/cs/rpc/thrift/artificial/thrift-ncs](jdk_8_maven/cs/rpc/thrift/artificial/thrift-ncs).
 gRPC: [jdk_8_maven/cs/rpc/grpc/artificial/grpc-ncs](jdk_8_maven/cs/rpc/grpc/artificial/grpc-ncs).

* **SCS** (not-known license, artificial string examples coming from different sources).
  Thrift: [jdk_8_maven/cs/rpc/thrift/artificial/thrift-scs](jdk_8_maven/cs/rpc/thrift/artificial/thrift-scs).
  gRPC: [jdk_8_maven/cs/rpc/grpc/artificial/grpc-scs](jdk_8_maven/cs/rpc/grpc/artificial/grpc-scs).



[//]: # (FIXME: temporarely removed until we fully support Web)
[//]: # (### WEB: backend in Java/Kotlin &#40;1&#41;)

[//]: # ()
[//]: # (* **Spring-PetClinic** &#40;Apache&#41;, [jdk_17_maven/cs/web/spring-petclinic]&#40;jdk_17_maven/cs/web/spring-petclinic&#41;, from [https://github.com/spring-projects/spring-petclinic]&#40;&#41;)


[//]: # (### REST: JavaScript/TypeScript)

[//]: # ()
[//]: # (* Disease-sh-API &#40;GPL&#41;, [js_npm/rest/disease-sh-api]&#40;js_npm/rest/disease-sh-api&#41;, from [https://github.com/disease-sh/API]&#40;https://github.com/disease-sh/API&#41;)

[//]: # ()
[//]: # (* Cyclotron &#40;MIT&#41;, [js_npm/rest/cyclotron]&#40;js_npm/rest/cyclotron&#41;, from [https://github.com/ExpediaInceCommercePlatform/cyclotron]&#40;https://github.com/ExpediaInceCommercePlatform/cyclotron&#41;)

[//]: # ()
[//]: # (* SpaceX-API &#40;Apache-2.0 License&#41;, [js_npm/rest/spacex-api]&#40;js_npm/rest/spacex-api&#41;, from [https://github.com/r-spacex/SpaceX-API]&#40;https://github.com/r-spacex/SpaceX-API&#41;)

[//]: # ()
[//]: # (* Realworld-App &#40;ISC&#41;, [js_npm/rest/realworld-app]&#40;js_npm/rest/realworld-app&#41;, from [https://github.com/lujakob/nestjs-realworld-example-app]&#40;https://github.com/lujakob/nestjs-realworld-example-app&#41;)

[//]: # ()
[//]: # (* NCS, [js_npm/rest/ncs]&#40;js_npm/rest/ncs&#41;, &#40;not-known license, artificial numerical examples coming from different sources&#41;)

[//]: # ()
[//]: # (* SCS, [js_npm/rest/scs]&#40;js_npm/rest/scs&#41;, &#40;not-known license, artificial string examples coming from different sources&#41;)


[//]: # (### REST: .NET/C#)

[//]: # ()
[//]: # (* Menu.API &#40;not-known license&#41;, from [https://github.com/chayxana/Restaurant-App]&#40;https://github.com/chayxana/Restaurant-App&#41;)

[//]: # ()
[//]: # (* SampleProject &#40;MIT&#41;, from [https://github.com/kgrzybek/sample-dotnet-core-cqrs-api]&#40;https://github.com/kgrzybek/sample-dotnet-core-cqrs-api&#41;)

[//]: # ()
[//]: # (* NCS &#40;not-known license, artificial numerical examples coming from different sources&#41;)

[//]: # ()
[//]: # (* SCS &#40;not-known license, artificial string examples coming from different sources&#41;)



[//]: # (### GraphQL: JavaScript/TypeScript)

[//]: # ()
[//]: # (* React-Finland &#40;not-known license&#41;, [js_npm/graphql/react-finland]&#40;js_npm/graphql/react-finland&#41;, from [https://github.com/ReactFinland/graphql-api]&#40;https://github.com/ReactFinland/graphql-api&#41;)

[//]: # ()
[//]: # (* E-Commerce Server &#40;MIT&#41;, [js_npm/graphql/ecommerce-server]&#40;js_npm/graphql/ecommerce-server&#41;, from [https://github.com/react-shop/react-ecommerce]&#40;https://github.com/react-shop/react-ecommerce&#41;)


## Using This Repository

Due to several reasons, the software in this repository is not published as a library (e.g., on Maven and NPM).
To use WFD, you need to clone this repository:

```
git clone https://github.com/WebFuzzing/Dataset.git
```

There are at least 2 main use cases for WFD:

* Run experiments black-box fuzzers

* Run experiments with white-box _EvoMaster_


Everything can be setup by running the script `scripts/dist.py`.
Note that you will need installed at least Maven, Gradle, JDK 8, JDK 11, JDK 17, JDK 21, NPM, as well as Docker.
Also, you will need to setup environment variables like `JAVA_HOME_8`, `JAVA_HOME_11`,  `JAVA_HOME_17` and `JAVA_HOME_21`.
The script will issue error messages if any prerequisite is missing.
Once the script is completed, all the SUTs will be available under the `dist` folder, and a `dist.zip` will be created as well (if `scripts/dist.py` is run with `True` as input).

Regarding Maven, most-third party dependencies are automatically downloaded from Maven Central.
However, some dependencies are from GitHub, which unfortunately require authentication to be able to download such dependencies.
Unfortunately, they have [no intention](https://github.com/orgs/community/discussions/26634) to fix this huge usability issue :(
In your home folder, you need to create a configuration file for Maven, in particular `.m2/settings.xml`, with the following configurations:

```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
<servers>
    <server>
        <id>github</id>
		<!-- Old pre Maven 3.9.0 version -->
        <username>YOURUSERNAME</username>
		<password>???</password>
		<!-- New post Maven 3.9.0 version -->
		<configuration>
			<httpHeaders>
			<property>
				<name>Authorization</name>
				<value>Bearer ???</value>
			</property>
			</httpHeaders>
		</configuration>
    </server>
</servers>
</settings>
```
Which configuration to use depends on the version of Maven (it was changed in version 3.9.0).
In latest versions of Maven, you need to create an authorization token in GitHub (see more info directly on [GitHub documentation pages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)), and put it instead of `???`.



[//]: # (There is also a Docker file to run `dist.py`, named `build.dockerfile`.)

[//]: # (It can be built with:)

[//]: # ()
[//]: # (```)

[//]: # (docker build -f build.dockerfile -t emb .)

[//]: # (```)

[//]: # ()
[//]: # (The `dist` folder with all SUTs will be under `/emb/dist`. )



In the built `dist` folder, the files will be organized as follows:
 `<name>-sut.jar` will be the non-instrumented SUTs, whereas their executable drivers for white-box testing will be called `<name>-evomaster-runner.jar`.
 Instrumentation can be done at runtime by attaching the `evomaster-agent.jar` JavaAgent. If you are running experiments with EvoMaster, this will be automatically attached when running experiments with experiment scripts (discussed in next section). Or it can be attached manually with JVM option `-Devomaster.instrumentation.jar.path=evomaster-agent.jar` when starting the driver.


You can also build (and install) each module separately, based on needs.
For example, a Maven module can be installed with:

``mvn clean install -DskipTests``

However, it is important to understand how this repository is structured, to be able to effectively navigate through it.
Each folder represents a set of SUTs (and drivers) that can be built using the same tools.
For example, the folder `jdk_8_maven` contains all the SUTs that need JDK 8 and are built with Maven.
On the other hand, the SUTs in the folder `jdk_11_gradle` require JDK 11 and Gradle.

For thr JVM, each module has 2 submodules, called `cs` (short for "Case Study") and `em` (short for "EvoMaster").
`cs` contains all the source code of the different SUTs, whereas `em` contains all the drivers.
Note: building a top-module will build as well all of its internal submodules.

The _EvoMaster_ driver classes for Java  are called `EmbeddedEvoMasterController`.
Note that Java also has a different kind of driver called `ExternalEvoMasterController`.
The difference is that in External the SUT is started on a separated process, and not running in the same JVM of the driver itself.


### Running Experiments 

To simplify the running of experiments, we provide different scripts under the [experiments](experiments) folder:

1) __bb-exp.py__: to set up black-box experiments, generating Bash scripts.
2) __wb-exp.py__: to set up white-box experiments, generating Bash scripts.
3) __schedule.py__: to enable running and scheduling Bash job scripts in parallel. 



For debugging/experimenting with EvoMaster in white-box testing, you can also "start" each driver directly from an IDE (e.g., IntelliJ).
Each of these drivers has a "main" method that is running a REST API (binding on default port 40100), where each operation (like start/stop/reset the SUT) can be called via an HTTP message by EvoMaster.



## Old Versions

The old releases of EMB are linked in version number with the release of EvoMaster, as EvoMaster's libraries are used in the drivers (e.g., to clean databases and configure auth info).
In the Git repository of EMB, we did tag the versions of EMB.
See the [releases](https://github.com/EMResearch/EMB/releases) page.
For example, to use version `X`, you can check out the Git commit
of EMB tagged with version `X`.
To see the current available tags, from a command-line you can execute:

`git tag`

Then, to switch to a specific tag X (e.g., `v1.0.0`), you can run:

`git checkout tags/v1.0.0`

Finally, if for any reason you need to switch back to the latest snapshot version, you can run:

`git checkout master`

There is an issue if you try to checkout an old version.
Not only Java broke backward compatibility with JDK 9, but also Maven...
If you try to build with Maven and get an error regarding
`maven-processor-plugin`, you might have to add manually
the following plugin dependency version:
```
<plugin>
    <groupId>org.bsc.maven</groupId>
    <artifactId>maven-processor-plugin</artifactId>
    <version>3.3.3</version>
</plugin>
```


### Build *develop* Branch

Branch *develop* is using the most recent SNAPSHOT version of _EvoMaster_.
As that is not published online, you need to clone its repository, and build
it locally (see its documentation on how to do it).
