This is an exact clone of the https://github.com/MorphiaOrg/morphia/ repo, with deprecation removed from a few methods
so that our projects are not swamped with warnings. The maintainer of morphia deprecated these methods as a signal that
they'll be changing in the 2.0 release; however, he did not provide alternatives, and the migration to 2.x is larger
than we'd like to tackle at this point.

This also adds a fix for ICL-25846 to keep `QueryImpl.first()` from returning too much data.

And it adds compatibility for Java 17 (see SetAccessibleHelper and where it's used).

```
# Update version
mvn versions:set

# Build & install locally 
mvn -DskipTests source:jar install

# Deploy artifacts to maven repo:
mvn deploy:deploy-file -Dfile=pom.xml -DartifactId=morphia-parent -DgroupId=dev.morphia.morphia -Dversion=1.6.1-dna3 -Dpackaging=pom -DpomFile=pom.xml -DrepositoryId=itrader-internal -Durl=http://build1.dev1.dynamicaction.com:8081/archiva/repository/internal/

mvn deploy:deploy-file -Dfile=morphia/target/core-1.6.1-dna3.jar -Dsources=morphia/target/core-1.6.1-dna3-sources.jar -DartifactId=core -DgroupId=dev.morphia.morphia -Dversion=1.6.1-dna3 -Dpackaging=jar -DpomFile=morphia/pom.xml -DrepositoryId=itrader-internal -Durl=http://build1.dev1.dynamicaction.com:8081/archiva/repository/internal/
```