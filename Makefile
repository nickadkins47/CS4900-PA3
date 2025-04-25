
#################################################################

target = bin/microrts.jar

bot_name = lightari
bot_dir = lightari

bot_trg = lib/bots/$(bot_name).jar
bot_jar = $(bot_dir)/$(bot_name).jar
bot_cls = $(bot_dir)/$(bot_name)*.class
bot_src = $(bot_dir)/$(bot_name).java

.PHONY: default
default: rs

#################################################################
# Run Game

.PHONY: rf run_full rs run_single

ARG = 

rf run_full: $(target) $(bot_trg) # Full Game
	@echo "Running Full Game ..."
	@cd bin && java -cp microrts.jar rts.MicroRTS

rs run_single: $(target) $(bot_trg) # Single Round
	@echo "Running Round ..."
	@java -cp "bin/*" $(bot_dir)/test_run.java $(ARG)

rsn run_single_norecomp: # Single Round; Dont Recompile anything
	@echo "Running Round ..."
	@java -cp "bin/*" $(bot_dir)/test_run.java $(ARG)

#################################################################
# Tournament Results

.PHONY: results

res_dir = $(bot_dir)/results
res_src = $(res_dir)/avgs.cc
res_out = $(res_dir)/avgs.out

results: $(res_out)
	@./$(res_dir)/results.sh
	@cd $(res_dir) && ./avgs.out

$(res_out): $(res_src)
	@g++ -Wall $< -o $@

#################################################################
# Compile Game

.PHONY: comp

# 3: create a single JAR file with sources and dependencies
comp $(target): bin/ai bin/$(bot_dir)
	@echo "Creating $(target) ..."
	@./make_game.sh
	
# 2: extract the contents of the JAR dependencies
bin/$(bot_dir): lib $(bot_trg)
	@echo "Extracting Dependencies from ./lib ..."
	@mkdir -p $@
	@cd bin && find ../lib -name "*.jar" | xargs -n 1 jar xf

# 1: compile source files
bin/ai: src
	@echo "Compiling ./src ..."
	@javac -cp "lib/*:src" -d bin $(shell find src -name "*.java")

#################################################################
# Compile Bot

.PHONY: bot jar

bot $(bot_trg): $(bot_jar)
	@cp $(bot_jar) $(bot_trg)
	@mkdir -p bin/$(bot_dir)
	@cp $(bot_cls) bin/$(bot_dir)

jar $(bot_jar): $(bot_cls)
	@echo "Creating bot .jar file ..."
	@jar cf $(bot_jar) $^

$(bot_cls): $(bot_src)
	@echo "Compiling Bot ..."
	@javac -cp "lib/*:src" $<

#################################################################

.PHONY: clean clean_bin clean_bot

clean: clean_results clean_bin clean_bot

clean_results:
	@rm -rf $(res_dir)/tnmt_*.txt $(res_dir)/avgs.out

clean_bin:
	@rm -rf bin

clean_bot:
	@rm -rf $(bot_trg) $(bot_jar) $(bot_cls) bin/$(bot_dir)