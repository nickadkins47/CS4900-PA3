
.PHONY: default ex main mayari

default: main

ex: bin/microrts.jar
	cd bin && java -cp microrts.jar rts.MicroRTS

# 1: compile source files
# 2: extract the contents of the JAR dependencies
# 3: create a single JAR file with sources and dependencies
main bin/microrts.jar:
	javac -cp "lib/*:src" -d bin $(find . -name "*.java") && \
	cd bin && \
	find ../lib -name "*.jar" | xargs -n 1 jar xvf
	jar cvf microrts.jar $(find . -name '*.class' -type f) 

mayari :
	javac