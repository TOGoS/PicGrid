rm -rf bin
mkdir bin

find java-src -name *.java >java-src.lst
"C:\Program Files\Java\jdk1.7.0_07\bin\javac.exe" -source 1.6 -target 1.6 -d bin @java-src.lst

find java-src scala-src -name *.java -o -name *.scala >scala-src.lst
: If we don't give fsc -cp bin, it will silently skip compiling classes
: that depend on already-compiled java classes

"C:\apps\scala-2.10.0-M7\bin\scalac.bat" -cp bin -d bin -deprecation -feature @scala-src.lst
goto:eof
call "C:\apps\scala-2.9.1.final\bin\fsc.bat" -cp bin -d bin -make:all @scala-src.lst
call "C:\apps\scala-2.9.1.final\bin\fsc.bat" -cp bin -d bin -make:all @scala-src.lst
call "C:\apps\scala-2.9.1.final\bin\fsc.bat" -cp bin -d bin -make:all @scala-src.lst
call "C:\apps\scala-2.9.1.final\bin\fsc.bat" -cp bin -d bin -make:all @scala-src.lst
call "C:\apps\scala-2.9.1.final\bin\fsc.bat" -cp bin -d bin -make:all @scala-src.lst
