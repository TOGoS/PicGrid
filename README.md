# PicGrid

Packs directories full of pictures such that each directory is
represented as an a view of all its contents.

Example output:
http://pix.nuke24.net/uri-res/raw/urn:bitprint:ZUSSZNX5QOYJZM4VPBUNYJ5WBZH67TBT.NCYZ74FYVFHXXIWLMWAQ5ZQOA4LBENMLSI3ULVA/Travel.html

It takes as input RDF+XML representations of directories as created by
ContentCouch and also stores the results into a ContentCouch repository.

ContentCouch (the program) is not required for PicGrid to run, though
it is useful for generating input.

PicGrid will cache the output from each directory so that re-running
after e.g. adding one picture to one subdirectory of a 10GB photo
collection will not use any additional time or disk space for branches
of the directory tree that have not changed.
