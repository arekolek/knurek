
Importing to Eclipse:

* Install ADT plugin (available in Marketplace)
* Install m2e plugin (available in Marketplace)
* Install m2e-apt plugin (as in https://github.com/excilys/androidannotations/wiki/Building-Project-Maven-Eclipse)
* Import... Existing Maven Project

Importing to IntelliJ:

* Just import it from external model (Maven).

Building:

With maven:

* mvn clean package android:deploy android:run

With Eclipse:

* Just run as Android Application

With IntelliJ:

* Because IntelliJ sucks you need to manually add compatibility-v4 as a dependency for actionbarsherlock in intellij module settings.
