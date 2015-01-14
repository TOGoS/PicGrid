proguard_jar_urn         := urn:bitprint:MYGRCO7NRSUP6ZKWDXXJRAS2YDV753FF.LEQHGVMGMQFZDJDB3XHI7UFS2QWKWDFHBHSNMDA
scala_library_jar_urn    := urn:bitprint:YJEYYZSC2K2MYKICPTMBY42MDY46FEQD.YLSD3HUOKLUQPOUJ5ESYLW7KTPXKT2G7QMBSM7A
scala_everything_jar_urn := urn:bitprint:EO3DIVRTIU5WYR2CFWS72X6GU6CNRC2K.FXSDPXBZQZIYBJ4YC2M6PVUD5O2PNTDSOHHKOSA
rt_jar_urn               := urn:bitprint:QTSCSZTEFHMDN4HJVC55TBRGSP4IT44L.ODXTBENAOEIJ4RIR4YIDVTGB2JLCGFBP4TVGQUQ
junit_jar_urn            := urn:bitprint:TEJJ6FSEFBCPNJFBDLRC7O7OICYU252P.XZMJNNSDGM456CHQCJI22DUDWYK6ZASNPLJ26GA

src_dirs = java-src java-test scala-src scala-test

fetch = java -jar util/TJFetcher.jar \
	-debug \
	-repo robert.nuke24.net \
	-repo fs.marvin.nuke24.net \
	-repo pvps1.nuke24.net \
	-repo localhost

.PHONY: default clean

default: PicGrid.jar.urn

clean:
	rm -rf bin ext-lib PicGrid.jar .*.touchfile .*.cmd

ext-lib/rt.jar:
	${fetch} ${rt_jar_urn} -o $@
ext-lib/scala-library.jar:
	${fetch} ${scala_library_jar_urn} -o $@
ext-lib/scala-everything.jar:
	${fetch} ${scala_everything_jar_urn} -o $@
ext-lib/proguard.jar:
	${fetch} ${proguard_jar_urn} -o $@
ext-lib/junit-3.8.1.jar:
	${fetch} ${junit_jar_urn} -o $@

.picgrid-javac.cmd: $(shell find java-src -name *.java)
	echo '' -classpath bin:ext-lib/junit-3.8.1.jar > $@
	echo '' -d bin -deprecation >> $@
	find java-src java-test -name *.java >> $@

.picgrid-scalac.cmd: $(shell find java-src scala-src -name *.java -o -name *.scala)
	echo '' -classpath bin:ext-lib/scala-everything.jar:bin/rt.jar > $@
	echo '' -d bin -deprecation >> $@
	find java-src java-test scala-src -name *.java -o -name *.scala >> $@

bin: .picgrid-javac.cmd .picgrid-scalac.cmd ext-lib/junit-3.8.1.jar ext-lib/scala-everything.jar
	mkdir -p bin
	javac @.picgrid-javac.cmd
	java -cp ext-lib/scala-everything.jar scala.tools.nsc.Main @.picgrid-scalac.cmd
	touch "$@"

PicGrid.jar: bin ext-lib/proguard.jar ext-lib/rt.jar ext-lib/scala-library.jar PicGrid.pro
	java -jar ext-lib/proguard.jar @PicGrid.pro

%.urn: % PicGrid.jar
	java -jar PicGrid.jar id "$<" >"$@"
