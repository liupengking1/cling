Read the README.txt
=====================

https://github.com/4thline/cling/blob/master/distribution/src/dist/README.txt

Mods
---------------------
* Modification of UpnpServiceImpl to avoid shutdownRouter exception

* Addition of a ApplicationURLHeader as clone of LocationHeader

* Modification of RemoteDevice to store the Application URL information

* Modification of RetrieveRemoteDescriptors to get the info from the header and put it in the RemoteDevice

* Modification of UpnpHeader to take ApplicationURLHeader into account in the parsing


Building Cling
---------------------

* Install Maven 3.2.3 or newer.

* Install the Android SDK and set the ANDROID_HOME environment variable to the SDK install directory.

* Clone the Cling source:

````
git clone https://github.com/4thline/cling.git
````

* Change into the `cling/` directory.

* Install everything into your local `~/.m2` Maven repository (this will take a few minutes if all dependencies have to be downloaded for the first time).

````
mvn clean install
````

If your build fails with Android/dex packaging errors, you forgot the clean.

* Use Cling in your pom.xml with:

````
<dependencies>
  <dependency>
    <groupId>org.fourthline.cling</groupId>
    <artifactId>cling-core</artifactId>
    <version>2.0.1</version>
  </dependency>
</dependencies>
````
