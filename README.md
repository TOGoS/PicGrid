# PicGrid

Transforms directories full of pictures into recursive structures
where each directory is represented by a picture of its contents
packed into a rectangle.

![For example, this directory of mushroom pictures](http://picture-files.nuke24.net/uri-res/raw/urn:bitprint:TL6JFRX2TOGYQQFNU7S7OEKZ5E7V5IX7.MO4VQTEUYYTT4JCZ5VWXAY2QJ4MYXRYA7XNJ4LI/Mushrooms.jpg)

Example output:
http://picture-files.nuke24.net/uri-res/raw/urn:bitprint:ZUSSZNX5QOYJZM4VPBUNYJ5WBZH67TBT.NCYZ74FYVFHXXIWLMWAQ5ZQOA4LBENMLSI3ULVA/Travel.html

It takes as input RDF+XML representations of directories as created by
ContentCouch and also stores the results into a ContentCouch repository.

ContentCouch (the program) is not required for PicGrid to run, though
it is useful for generating input.

PicGrid will cache the output from each directory so that re-running
after e.g. adding one picture to one subdirectory of a 10GB photo
collection will not use any additional time or disk space for branches
of the directory tree that have not changed.

## Building

The build process for this project is mostly self-contained
(see [the Makefile](./Makefile)),
but requires a relatively old version of Java (such as 1.7.0)
to support the old version of Scala that's used.

See ```[docker](./docker)/``` for building using a Dockerized build environment
and/or building a PicGrid docker container.

Or just ```docker run togos/picgrid:latest```.
