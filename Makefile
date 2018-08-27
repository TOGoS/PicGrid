proguard_jar_urn         := urn:bitprint:MYGRCO7NRSUP6ZKWDXXJRAS2YDV753FF.LEQHGVMGMQFZDJDB3XHI7UFS2QWKWDFHBHSNMDA
scala_library_jar_urn    := urn:bitprint:YJEYYZSC2K2MYKICPTMBY42MDY46FEQD.YLSD3HUOKLUQPOUJ5ESYLW7KTPXKT2G7QMBSM7A
scala_everything_jar_urn := urn:bitprint:EO3DIVRTIU5WYR2CFWS72X6GU6CNRC2K.FXSDPXBZQZIYBJ4YC2M6PVUD5O2PNTDSOHHKOSA
rt_jar_urn               := urn:bitprint:QTSCSZTEFHMDN4HJVC55TBRGSP4IT44L.ODXTBENAOEIJ4RIR4YIDVTGB2JLCGFBP4TVGQUQ
junit_jar_urn            := urn:bitprint:TEJJ6FSEFBCPNJFBDLRC7O7OICYU252P.XZMJNNSDGM456CHQCJI22DUDWYK6ZASNPLJ26GA

java_src_dirs = src/*/java
scala_src_dirs = src/*/scala
src_dirs = ${java_src_dirs} ${scala_src_dirs}

fetch = java -jar util/TJFetcher.jar \
	-debug \
	-repo localhost \
	-repo wherever-files.nuke24.net \
	-repo fs.marvin.nuke24.net

class_dest_dir = target/scala-2.10/classes

.PHONY: default
default: PicGrid.jar.urn

.PHONY: clean
clean:
	rm -rf bin ${build_target_dir} ext-lib PicGrid.jar .*.touchfile .*.cmd

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
external_libs := ext-lib/rt.jar ext-lib/scala-library.jar ext-lib/scala-everything.jar ext-lib/proguard.jar ext-lib/junit-3.8.1.jar

.PHONY: download-external-libs
download-external-libs: ${external_libs}

.picgrid-javac.cmd: Makefile $(shell find ${java_src_dirs} -name *.java)
	echo '' -classpath ${class_dest_dir}:ext-lib/junit-3.8.1.jar > $@
	echo '' -d ${class_dest_dir} -deprecation >> $@
	find ${java_src_dirs} -name *.java >> $@

.picgrid-scalac.cmd: Makefile $(shell find ${src_dirs} -name *.java -o -name *.scala)
	echo '' -classpath ${class_dest_dir}:ext-lib/scala-everything.jar:ext-lib/junit-3.8.1.jar:ext-lib/rt.jar > $@
	echo '' -d ${class_dest_dir} -deprecation >> $@
	find ${src_dirs} -name *.java -o -name *.scala >> $@

${class_dest_dir}: .picgrid-javac.cmd .picgrid-scalac.cmd ext-lib/junit-3.8.1.jar ext-lib/scala-everything.jar
	mkdir -p ${class_dest_dir}
	javac @.picgrid-javac.cmd
	java -cp ext-lib/scala-everything.jar scala.tools.nsc.Main @.picgrid-scalac.cmd
	touch "$@"

PicGrid.jar: ${class_dest_dir} ext-lib/proguard.jar ext-lib/rt.jar ext-lib/scala-library.jar PicGrid.pro
	java -jar ext-lib/proguard.jar @PicGrid.pro

%.urn: % PicGrid.jar
	java -jar PicGrid.jar id "$<" >"$@"
