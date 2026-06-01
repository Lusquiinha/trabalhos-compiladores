ROOT      := $(CURDIR)
PARENT    := $(abspath $(ROOT)/..)
CORRETOR  := $(PARENT)/compiladores-corretor-automatico/target/compiladores-corretor-automatico-1.0-SNAPSHOT-jar-with-dependencies.jar
CASOS     := $(PARENT)/casos-de-teste
RA        := 811943
GCC       := gcc
TMP       := $(ROOT)/.corretor-tmp

T1_JAR    := $(ROOT)/T1/target/compilador.jar
T2_JAR    := $(ROOT)/T2/target/compilador.jar
T3_JAR    := $(ROOT)/T3/target/compilador.jar
T4_JAR    := $(ROOT)/T4/target/compilador.jar

.PHONY: all build build-t1 build-t2 build-t3 build-t4 test test-t1 test-t2 test-t3 test-t4 clean

all: test

build: build-t1 build-t2 build-t3 build-t4

build-t1:
	cd $(ROOT)/T1 && mvn -q clean package

build-t2:
	cd $(ROOT)/T2 && mvn -q clean package

build-t3:
	cd $(ROOT)/T3 && mvn -q clean package

build-t4:
	cd $(ROOT)/T4 && mvn -q clean package

test: test-t1 test-t2 test-t3 test-t4

test-t1: build-t1
	@mkdir -p $(TMP)
	java -jar "$(CORRETOR)" "java -jar $(T1_JAR)" $(GCC) "$(TMP)" "$(CASOS)" "$(RA)" t1

test-t2: build-t2
	@mkdir -p $(TMP)
	java -jar "$(CORRETOR)" "java -jar $(T2_JAR)" $(GCC) "$(TMP)" "$(CASOS)" "$(RA)" t2

test-t3: build-t3
	@mkdir -p $(TMP)
	java -jar "$(CORRETOR)" "java -jar $(T3_JAR)" $(GCC) "$(TMP)" "$(CASOS)" "$(RA)" t3

test-t4: build-t4
	@mkdir -p $(TMP)
	java -jar "$(CORRETOR)" "java -jar $(T4_JAR)" $(GCC) "$(TMP)" "$(CASOS)" "$(RA)" t4

clean:
	cd $(ROOT)/T1 && mvn -q clean
	cd $(ROOT)/T2 && mvn -q clean
	cd $(ROOT)/T3 && mvn -q clean
	cd $(ROOT)/T4 && mvn -q clean
	rm -rf $(TMP)
