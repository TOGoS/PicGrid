proguard_jar_urn         := urn:bitprint:MYGRCO7NRSUP6ZKWDXXJRAS2YDV753FF.LEQHGVMGMQFZDJDB3XHI7UFS2QWKWDFHBHSNMDA
scala_library_jar_urn    := urn:bitprint:YJEYYZSC2K2MYKICPTMBY42MDY46FEQD.YLSD3HUOKLUQPOUJ5ESYLW7KTPXKT2G7QMBSM7A
scala_everything_jar_urn := urn:bitprint:EO3DIVRTIU5WYR2CFWS72X6GU6CNRC2K.FXSDPXBZQZIYBJ4YC2M6PVUD5O2PNTDSOHHKOSA
rt_jar_urn               := urn:bitprint:QTSCSZTEFHMDN4HJVC55TBRGSP4IT44L.ODXTBENAOEIJ4RIR4YIDVTGB2JLCGFBP4TVGQUQ

src_dirs = java-src java-test scala-src scala-test

.PHONY: all clean

all: PicGrid.jar

clean:
	rm -rf bin ext-lib PicGrid.jar .*.touchfile .*.cmd

ext-lib/rt.jar:
	ccouch checkout -link ${rt_jar_urn}  $@
ext-lib/scala-library.jar:
	ccouch checkout -link ${scala_library_jar_urn} $@
ext-lib/scala-everything.jar:
	ccouch checkout -link ${scala_everything_jar_urn} $@
ext-lib/proguard.jar:
	ccouch checkout -link ${proguard_jar_urn} $@

.picgrid-javac.cmd:
	echo '' -classpath bin > $@
	echo '' -d bin -deprecation >> $@
	find java-src -name *.java >> $@

.picgrid-scalac.cmd:
	echo '' -classpath bin:ext-lib/scala-everything.jar:bin/rt.jar > $@
	echo '' -d bin -deprecation >> $@
	find java-src java-test scala-src -name *.java -o -name *.scala >> $@

bin: .picgrid-javac.cmd .picgrid-scalac.cmd ext-lib/scala-everything.jar
	mkdir -p bin
	javac @.picgrid-javac.cmd
	java -cp ext-lib/scala-everything.jar scala.tools.nsc.Main @.picgrid-scalac.cmd

PicGrid.jar: bin ext-lib/proguard.jar ext-lib/rt.jar ext-lib/scala-library.jar
	java -jar ext-lib/proguard.jar @PicGrid.pro
