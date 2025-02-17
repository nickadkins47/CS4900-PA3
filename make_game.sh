
# Have to use a seperate shell script to handle a calculation for the Makefile

cd bin && jar cf microrts.jar $(find . -name '*.class' -type f)