Notes on the OSGI tryout

- Converted mule-core, module-annotations and module-spring-config to bundles
- Created two test bundles that expose two versions of the same class (v1 & v2)

Using Karaf 3.0.1 (OSGI Container)


Dependencies configuration
--------------------------
Some dependencies are available as bundles directly. We can install those using karaf's maven support.
For the rest, we start with a fairly naive approach, which consists of wrapping the jar as a bundle (exporting 
all the packages and importing all the dependency packages needed). We may refine this with bnd recipes in the future.
Karaf allows to define a set of bundles as a feature. We may eventually group our dependencies or modules in a feature.
In the meanwhile, some libraries, such as spring are easily instaleld as features.
There's no order to install dependencies. Those will not be started, just registered.

List of dependencies so far:
bundle:install mvn:com.google.guava/guava/16.0.1
bundle:install 'wrap:mvn:com.github.stephenc.eaio-uuid/uuid/3.4.0$Bundle-SymbolicName=eaio-uuid&Bundle-Version=3.4'

feature:install transaction   
bundle:install  mvn:org.apache.geronimo.specs/geronimo-j2ee-connector_1.5_spec/1.1
bundle:install 'wrap:mvn:commons-beanutils/commons-beanutils/1.8.0$Bundle-SymbolicName=commons-beanutils&Bundle-Version=1.8.0'
bundle:install 'wrap:mvn:commons-collections/commons-collections/3.2.1$Bundle-SymbolicName=commons-collections&Bundle-Version=3.2.1'
bundle:install 'wrap:mvn:commons-cli/commons-cli/1.2$Bundle-SymbolicName=commons-cli&Bundle-Version=1.2'
bundle:install 'wrap:mvn:commons-io/commons-io/1.4$Bundle-SymbolicName=commons-io&Bundle-Version=1.4'

bundle:install -s 'wrap:mvn:commons-lang/commons-lang/2.4$Bundle-SymbolicName=commons-lang&Bundle-Version=2.4'
bundle:install -s 'wrap:mvn:commons-pool/commons-pool/1.6$Bundle-SymbolicName=commons-pool&Bundle-Version=1.6'
bundle:install -s 'wrap:mvn:org.jgrapht/jgrapht-jdk1.5/0.7.3$Bundle-SymbolicName=jgrapht&Bundle-Version=0.7.3'
bundle:install -s 'wrap:mvn:org.mule.mvel/mule-mvel2/2.1.9-MULE-004$Bundle-SymbolicName=mule-mvel&Bundle-Version=2.1.9.MULE-003'

bundle:install 'wrap:mvn:org.antlr/antlr-runtime/3.5$Bundle-SymbolicName=antlr-runtime&Bundle-Version=3.5'

bundle:install 'wrap:mvn:cglib/cglib-nodep/2.2$Bundle-SymbolicName=cglib&Bundle-Version=2.2'

install 'wrap:mvn:dom4j/dom4j/1.6.1$Bundle-SymbolicName=dom4j&Bundle-Version=1.6.1'

feature:install spring/3.1.4.RELEASE

bundle:install mvn:org.mule.modules/mule-module-spring-config/4.0-SNAPSHOT
bundle:install mvn:org.mule.modules/mule-module-annotations/4.0-SNAPSHOT
bundle:install mvn:org.mule/mule-core/4.0-SNAPSHOT


In order to activate a bundle, you can use start <bundle name or number>.
To diagnose problems (i.e. missing dependencies): diag

