# To-do

- Make sure Docker version *actually works*
  - Can PicGrid a directory
  - Can webserve that griddified directory
  - Make sure Docker container has imagemagick in it
- How come sometimes pictures turn into a diagonally-subdivided gray rectangle?
  - e.g. in http://picture-files.nuke24.net/uri-res/raw/urn:bitprint:IQXSR7HBOJNPWVAOXUCHZVKAKXPJNO4O.YZPCHLTNS4I3A4SAHJZC7ALQZH5EMD6B5X6YPUQ/Game21.html
- New layouter method (see below)
- Compose should allow filesystem objects to be passed on the command line
  and store them itself (should be able to steal directory serialization code from ccouch3)
- Include a utility for recursively scanning emitted HTML files for URNs
  and writing them all to STDOUT.

### Zero-width crash

```
$ picgrid-compose -layouter borce -page-style hover x-rdf-subject:urn:bitprint:YRLZROF2ZKR5GCAUZ7OBMFWG2NCIO3MB.5ILYDI7K3FLR2YBENH477HYHAINKTMNTWLTGDFA
Exception in thread "main" java.lang.AssertionError: assertion failed: Cell width is <= 0: togos.picgrid.layout.LayoutCell@1ae53e9
        at scala.Predef$.assert(Predef.scala:179)
        at togos.picgrid.layout.AutoSpacingLayouter$$anonfun$layout$1.apply(Layouter.scala:99)
        at togos.picgrid.layout.AutoSpacingLayouter$$anonfun$layout$1.apply(Layouter.scala:98)
        at scala.collection.immutable.List.foreach(List.scala:318)
        at togos.picgrid.layout.AutoSpacingLayouter.layout$4348ff2d(Layouter.scala:98)
        at togos.picgrid.Gridifier.gridify$35eef3cf(Gridifier.scala:57)
        at togos.picgrid.Gridifier.gridifyDir$35eef3cf(Gridifier.scala:84)
        at togos.picgrid.Gridifier.gridifyDir(Gridifier.scala:106)
        at togos.picgrid.app.ComposeCommand$.main(ComposeCommand.scala:175)
        at togos.picgrid.app.PicGridCommand$.main(PicGridCommand.scala:166)
        at togos.picgrid.app.PicGridCommand.main(PicGridCommand.scala)
```

This seems to be a trouble with the 'borce' layouter, because when using 'rowly'...

```sh
$ picgrid-compose -layouter rowly -page-style hover x-rdf-subject:urn:bitprint:YRLZROF2ZKR5GCAUZ7OBMFWG2NCIO3MB.5ILYDI7K3FLR2YBENH477HYHAINKTMNTWLTGDFA
urn:bitprint:6W4PVKPFENWMOKGXCTD5I7WVTFE7NDIG.BTJTX7P4CC4UXLEZH3CL6K5PAW37NLUICWYMZUY
```

### New layouter

For a given set of images with weights (these weights can
be assigned arbitrarily), create a single row.
Find the average area / weight.
Create the row again, this time adjusting images that are
too large by attempting to fit them into a cell with
neighboring images that are also too large.

Next, take the width/height of the single row
and calculate how many rows the image set should be split into
to match the target ratio.  Break the single row up into
that many rows, that many -1, -2, +1, and +2, and
hold onto those configurations and their fitness values.

Repeat the 2 steps but making columns instead of rows.

# Done

* Layouter that runs both borce and rowly (and any new ones you come up with)
  and picks the most fit layout from all the results.
* Allow selection of layout from command-line
* Break program into smaller parts that can be run separately.
  (compose can accept directories or compound image files and
  can emit compound images, raster images, or HTML)
* Pretty picture for the README (2018-08-27)
  - Maybe this one: http://picture-files.nuke24.net/uri-res/raw/urn:bitprint:32B7UZ4SORCYNVM7ZRPG5UBJA3DDR4TB.2EALXNOSI7JNBPIIM7TN6672WVGVGXAXM25OHNA/Mushroom.html
