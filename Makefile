proguard_jar_urn         := urn:bitprint:MYGRCO7NRSUP6ZKWDXXJRAS2YDV753FF.LEQHGVMGMQFZDJDB3XHI7UFS2QWKWDFHBHSNMDA
scala_library_jar_urn    := urn:bitprint:YJEYYZSC2K2MYKICPTMBY42MDY46FEQD.YLSD3HUOKLUQPOUJ5ESYLW7KTPXKT2G7QMBSM7A
scala_everything_jar_urn := urn:bitprint:EO3DIVRTIU5WYR2CFWS72X6GU6CNRC2K.FXSDPXBZQZIYBJ4YC2M6PVUD5O2PNTDSOHHKOSA
rt_jar_urn               := urn:bitprint:QTSCSZTEFHMDN4HJVC55TBRGSP4IT44L.ODXTBENAOEIJ4RIR4YIDVTGB2JLCGFBP4TVGQUQ
junit_jar_urn            := urn:bitprint:TEJJ6FSEFBCPNJFBDLRC7O7OICYU252P.XZMJNNSDGM456CHQCJI22DUDWYK6ZASNPLJ26GA

src_dirs = java-src java-test scala-src scala-test

cc_checkout = ccouch checkout

.PHONY: all clean

all: PicGrid.jar

clean:
	rm -rf bin ext-lib PicGrid.jar .*.touchfile .*.cmd

ext-lib/rt.jar:
	${cc_checkout} ${rt_jar_urn}  $@
ext-lib/scala-library.jar:
	${cc_checkout} ${scala_library_jar_urn} $@
ext-lib/scala-everything.jar:
	${cc_checkout} ${scala_everything_jar_urn} $@
ext-lib/proguard.jar:
	${cc_checkout} ${proguard_jar_urn} $@
ext-lib/junit-3.8.1.jar:
	${cc_checkout} ${junit_jar_urn} $@

.picgrid-javac.cmd:
	echo '' -classpath bin:ext-lib/junit-3.8.1.jar > $@
	echo '' -d bin -deprecation >> $@
	find java-src java-test -name *.java >> $@

.picgrid-scalac.cmd:
	echo '' -classpath bin:ext-lib/scala-everything.jar:bin/rt.jar > $@
	echo '' -d bin -deprecation >> $@
	find java-src java-test scala-src -name *.java -o -name *.scala >> $@

bin: .picgrid-javac.cmd .picgrid-scalac.cmd ext-lib/junit-3.8.1.jar ext-lib/scala-everything.jar
	mkdir -p bin
	javac @.picgrid-javac.cmd
	java -cp ext-lib/scala-everything.jar scala.tools.nsc.Main @.picgrid-scalac.cmd

PicGrid.jar: bin ext-lib/proguard.jar ext-lib/rt.jar ext-lib/scala-library.jar PicGrid.pro
	java -jar ext-lib/proguard.jar @PicGrid.pro
