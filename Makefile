
#################################################################

target = bin/microrts.jar

bot_name = mayaripp
bot_dir = mayaripp

bot_bin = $(bot_dir)/$(bot_dir)
bot_trg = lib/bots/$(bot_name).jar
bot_jar = $(bot_bin)/$(bot_name).jar
bot_cls = $(bot_bin)/$(bot_name)*.class
bot_src = $(bot_dir)/$(bot_name).java

.PHONY: default
default: rs

#################################################################
# Run Game

.PHONY: rf run_full rs run_single

rf run_full: $(target) # Run MicroRTS
	@echo "Running Full Game ..."
	@cd bin && java -cp microrts.jar rts.MicroRTS

rs run_single: bin
	@echo "Running Round ..."
	@java -cp "bin/*:lib/*" mayaripp/test_run.java

#################################################################
# Compile Game

.PHONY: comp

# 3: create a single JAR file with sources and dependencies
comp $(target): bin
	@echo "Creating $(target) ..."
	@./make_game.sh

# 1: compile source files
# 2: extract the contents of the JAR dependencies
bin: lib src | $(bot_trg)
	@echo "Compiling ./src ..."
	@javac -cp "lib/*:src" -d bin $(shell find . -name "*.java")
	@echo "Extracting Dependencies from ./lib ..."
	@cd bin && find ../lib -name "*.jar" | xargs -n 1 jar xf

#################################################################
# My fork of the Mayari Bot, Mayari++

.PHONY: bot jar

bot $(bot_trg): $(bot_jar)
	@cp $(bot_jar) $(bot_trg)

jar $(bot_jar): $(bot_cls)
	@jar cf $(bot_jar) $^

$(bot_cls): $(bot_src)
	@javac -cp "lib/*:src" -d $(bot_dir) $<

#################################################################

.PHONY: clean clean_bin clean_bot

clean: clean_bin clean_bot

clean_bin:
	@rm -rf bin

clean_bot:
	@rm -rf $(bot_trg) $(bot_jar) $(bot_cls)