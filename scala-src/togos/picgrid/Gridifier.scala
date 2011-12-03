package togos.picgrid

import image.ImageMagickResizer
import togos.mf.value.ByteBlob
import scala.collection.JavaConversions.asScalaIterator
import java.io.ByteArrayOutputStream
import togos.mf.value.ByteChunk
import togos.picgrid.BlobConversions.byteBlobAsString
import togos.picgrid.image.CompoundImage
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.image.CompoundImageComponent
import scala.collection.mutable.ListBuffer
import java.io.File
import scala.collection.mutable.HashMap
import togos.picgrid.image.ImageMagickCropResizer
import togos.picgrid.image.ImageMagickCommands

class ImageInfo(
	val uri:String, val sourceUri:String,
	val width:Integer, val height:Integer,
	val totalImageCount:Integer
)

class ImageEntry( val name:String, val info:ImageInfo )

class Gridifier(
	val functionCache:FunctionCache,
	val datastore:Datastore,
	val infoExtractor:ImageInfoExtractor
) {
	val xRdfSubjectRegex = """x-rdf-subject:(.*)""".r
	val imageFilenameRegex = """(?i).*\.(?:png|jpe?g|gif|bmp)$""".r
	
	def getDirEntries( uri:String ):List[DirectoryEntry] = {
		val xRdfSubjectRegex(blobUri) = uri
		val rdfBlob = datastore(blobUri)
		RDFDirectoryParser.parseDirectory( rdfBlob )
	}
	
	def gridifySingleImage( uri:String ):ImageInfo = {
		val dims = infoExtractor.getImageDimensions( uri )
		if( dims == null ) return null
		val (w,h) = dims
		new ImageInfo( uri, uri, w, h, 1 )
	}
	
	def gridify( e:DirectoryEntry ):ImageEntry = {
		val info = e.targetClass match {
			case DirectoryObjectClass.Blob => e.name match {
				case imageFilenameRegex() => gridifySingleImage(e.targetUri)
				case _ => null
			}
			case DirectoryObjectClass.Directory =>
				gridifyDir( e.targetUri )
		}
		new ImageEntry( e.name, info )
	}
	
	def gridify( images:List[ImageEntry], imagesPerRow:Integer, generatedFromUri:String ):CompoundImage = {
		var rows = ListBuffer[List[ImageEntry]]()
		var row = ListBuffer[ImageEntry]()
		for( i <- images ) {
			if(row.length > imagesPerRow ) {
				rows += row.toList
				row = ListBuffer[ImageEntry]()
			}
			row += i
		}
		if( row.length > 0 ) rows += row.toList
		
		val components = ListBuffer[CompoundImageComponent]()
		var totalWidth = 1024
		var totalImageCount = 0
		var spacing = 4
		var y = 0
		for( row <- rows ) {
			var imageSpaceAvailable = totalWidth - (row.length - 1) * spacing
			var imageWidthRatio = 0f // sum(w/h)
			for( e <- row ) {
				val i = e.info
				imageWidthRatio += i.width.toFloat / i.height
			}
			val rowHeight = (imageSpaceAvailable / imageWidthRatio).toInt
			var x = 0
			for( e <- row ) {
				val i = e.info
				val cellWidth = i.width * rowHeight / i.height
				components += new CompoundImageComponent( x, y, cellWidth, rowHeight, i.uri, e.name )
				x += cellWidth + spacing
				totalImageCount += i.totalImageCount
			}
			y += rowHeight + spacing
		}
		
		new CompoundImage(
			totalWidth, y - spacing, components, null, null,
			totalImageCount, generatedFromUri )
	}
	
	def gridify( images:List[ImageEntry], generatedFromUri:String ):ImageInfo = {
		if( images.length == 0 ) return null
		if( images.length == 1 ) return images.head.info
		
		// TODO use promoted images somehow
		
		val ci = gridify( images, Math.sqrt(images.length).toInt, generatedFromUri:String )
		val uri = datastore.store( ci.serialize() )
		new ImageInfo( uri, generatedFromUri, ci.width, ci.height, ci.totalImageCount )
	}
	
	def gridifyDir( dir:List[DirectoryEntry], generatedFromUri:String ):ImageInfo = {
		gridify( dir.map( e => gridify(e) ).filter( i => i != null ), generatedFromUri )
	}
	
	def gridifyDir( uri:String ):ImageInfo = {
		gridifyDir( getDirEntries( uri ), uri )
	}
}
object Gridifier
{
	def main( args:Array[String] ) {
		var datastoreDir:String = null
		var datasources:ListBuffer[String] = new ListBuffer[String]()
		var i = 0
		var target:String = null
		while( i < args.length ) {
			args(i) match {
				case "-datastore" =>
					i += 1
					datastoreDir = args(i) 
				case "-datasource" =>
					i += 1
					datasources += args(i)
				case arg if !arg.startsWith("-") =>
					target = arg
				case arg => throw new RuntimeException("Unrecognised argument: "+arg)
			}
			i += 1
		}
		
		if( datastoreDir == null ) {
			throw new RuntimeException("No -datastore directory specified")
		}
		val datastore = new FSSHA1Datastore(new File(datastoreDir), datasources.toList)
		if( target == null ) {
			throw new RuntimeException("Must specify a target")
		}
		
		class KoolCache {
			val maps = new HashMap[String,HashMap[String,String]]
			
			protected def map( name:String ):HashMap[String,String] = {
				var m = maps.getOrElse(name,null)
				if( m == null ) {
					m = new HashMap()
					maps(name) = m
				} 
				m
			}
			
			def apply( k:(String,String) ):String = {
				map(k._1).getOrElse(k._2, null)
			}
			
			def update( k:(String,String), v:String ):Unit = {
				map(k._1)(k._2) = v
			}
		}
		
		val functionCache:FunctionCache = new KoolCache //HashMap[(String,String),String]()
		val imageInfoExtractor = new ImageInfoExtractor( functionCache, datastore )
		val resizer = new ImageMagickCropResizer( functionCache, datastore, ImageMagickCommands.convert )
		val gridifier = new Gridifier( functionCache, datastore, imageInfoExtractor )
		val gridRenderer = new GridRenderer( functionCache, datastore, imageInfoExtractor, resizer, ImageMagickCommands.convert )
		
		val cimg = gridifier.gridifyDir( target )
		if( cimg == null ) {
			System.err.println("No images found!")
			return
		}
		
		System.out.println( "rasterize("+cimg.uri+") = " + gridRenderer.rasterize( cimg.uri ) )
	}
}
