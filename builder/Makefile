.PHONY: all clean

all: PicGridBuilder.jar

clean:
	rm -rf bin .javac.cmd PicGridBuilder.jar

.javac.cmd:
	echo '' -d bin -deprecation > $@
	find src -name *.java >> $@

bin: .javac.cmd
	mkdir -p bin/META-INF
	echo 'Manifest-Version: 1.0' > bin/META-INF/MANIFEST.MF
	echo 'Main-Class: togos.picgrid.build.PicGridBuilder' >> bin/META-INF/MANIFEST.MF
	javac @.javac.cmd

PicGridBuilder.jar: bin
	cd bin ; zip -r ../PicGridBuilder.jar . ; cd ..
