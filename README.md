# PicGrid

Transforms directories full of pictures into recursive structures
where each directory is represented by a picture of its contents
packed into a rectangle.

## Examples

[![A collection of mushroom photos](http://picture-files.nuke24.net/uri-res/raw/urn:bitprint:TL6JFRX2TOGYQQFNU7S7OEKZ5E7V5IX7.MO4VQTEUYYTT4JCZ5VWXAY2QJ4MYXRYA7XNJ4LI/Mushrooms.jpg)](http://picture-files.nuke24.net/uri-res/raw/urn:bitprint:32B7UZ4SORCYNVM7ZRPG5UBJA3DDR4TB.2EALXNOSI7JNBPIIM7TN6672WVGVGXAXM25OHNA/Mushroom.html)

[![A subset of my 'travel' photos](http://picture-files.nuke24.net/uri-res/raw/urn:bitprint:FTNY4PJ5FSFLMQSSPQ6RFJAJELIB2JQG.2ZTKMUA4MMIIOCX7BC2MWU242AEWCPRFO2IDFUI/Travel.jpg)](http://picture-files.nuke24.net/uri-res/raw/urn:bitprint:ZUSSZNX5QOYJZM4VPBUNYJ5WBZH67TBT.NCYZ74FYVFHXXIWLMWAQ5ZQOA4LBENMLSI3ULVA/Travel.html)

## Operation

PicGrid is a command-line Java program takes as input RDF+XML representations of directories as created by
[ContentCouch](https://github.com/TOGoS/ContentCouch) and also stores the results into a ContentCouch repository.
Basically, inputs and outputs are stored in
[a 'repository' directory](https://github.com/TOGoS/ContentCouchRepositoryFormat#directory-structure)
based on their base32-encoded SHA-1 hash.

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

See [```docker/```](./docker) for building using a Dockerized build environment
and/or building a PicGrid docker container.
```make -C docker``` should do the job.

## To use the latest Docker container

```docker run --rm togos/picgrid:latest```.
