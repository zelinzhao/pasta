pasta: modules
	mvn package
	bash ./modules/library/createLib.sh

modules:
	ant -f ./modules/developer-interface/build.xml
	ant -f ./modules/dpg/build.xml
	mvn -f ./modules/xstream/xstream/pom.xml package dependency:copy-dependencies -DoutputDirectory=./alllib -Dmaven.test.skip=true
	mvn -f ./modules/agent/pom.xml package

clean:
	ant -f ./modules/developer-interface/build.xml clean
	ant -f ./modules/dpg/build.xml clean
	mvn -f ./modules/xstream/xstream/pom.xml clean
	mvn -f ./modules/agent/pom.xml clean
	mvn clean

.PHONY: pasta modules clean
