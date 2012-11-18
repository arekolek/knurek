
Importing to IntelliJ:

Just import it from external model (Maven).

Building:

With maven:

mvn clean package android:deploy android:run

With IntelliJ:

Because IntelliJ sucks you need to manually add compatibility-v4 as a dependency for actionbarsherlock in intellij module settings.
