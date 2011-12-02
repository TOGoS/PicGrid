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

class Gridifier(
	val functionCache:FunctionCache,
	val datastore:Datastore,
	val resizer:ImageMagickResizer,
	val infoExtractor:ImageInfoExtractor
) {
	val xRdfSubjectRegex = """x-rdf-subject:(.*)""".r
	val imageFilenameRegex = """(?i).*\.(?:png|jpe?g|gif|bmp)$""".r
	
	def getDirEntries( uri:String ):List[DirectoryEntry] = {
		val xRdfSubjectRegex(blobUri) = uri
		val rdfBlob = datastore(blobUri)
		RDFDirectoryParser.parseDirectory( rdfBlob )
	}
	
	def gridifySingleImage( uri:String ):CompoundImage = {
		val dims = infoExtractor.getImageDimensions( uri )
		if( dims == null ) return null
		val (w,h) = dims
		val component = new CompoundImageComponent(0,0,w,h,uri)
		new CompoundImage( w, h, List(component), null, uri )
	}
	
	def gridify( e:DirectoryEntry ):CompoundImage = {
		e.targetClass match {
			case DirectoryObjectClass.Blob => e.name match {
				case imageFilenameRegex() => gridifySingleImage(e.targetUri)
				case _ => null
			}
			case DirectoryObjectClass.Directory =>
				gridify( getDirEntries( e.targetUri ) )
		}
	}
	
	def gridify( dir:List[DirectoryEntry] ):CompoundImage = {
		val sortedImages = dir.map( e => gridify(e) ).filter( i => i != null )
		if( sortedImages.length == 0 ) return null
		if( sortedImages.length == 1 ) return sortedImages.head
		// TODO use promoted images somehow
		var rows = ListBuffer[List[CompoundImage]]()
		val imagesPerRow = Math.sqrt(sortedImages.length) * 4 / 3
		var row = ListBuffer[CompoundImage]()
		for( i <- sortedImages ) {
			if(row.length > imagesPerRow ) {
				rows += row.toList
				row = ListBuffer[CompoundImage]()
			}
			row += i
		}
		if( row.length > 0 ) rows += row.toList
		
		val components = ListBuffer[CompoundImageComponent]()
		var totalWidth = 1024
		var spacing = 4
		var y = 0
		for( row <- rows ) {
			var imageSpaceAvailable = totalWidth - (row.length - 1) * spacing
			var imageWidthRatio = 0f // sum(w/h)
			for( i <- row ) {
				imageWidthRatio += i.width.toFloat / i.height
			}
			val rowHeight = (imageSpaceAvailable / imageWidthRatio).toInt
			var x = 0
			for( i <- row ) {
				val imageUri = datastore.store( i.serialize() )
				val cellWidth = i.width * rowHeight / i.height
				components += new CompoundImageComponent( x, y, cellWidth, rowHeight, imageUri )
				x += cellWidth + spacing
			}
			y += rowHeight + spacing
		}
		
		new CompoundImage( totalWidth, y - spacing, components, null, null )
	}
	
	def gridify( uri:String ):CompoundImage = {
		gridify( getDirEntries( uri ) )
	}
	
	def gridifyAndStore( uri:String ):String = {
		datastore.store( gridify(uri).serialize() )
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
		val resizer = new ImageMagickResizer( functionCache, datastore, "/usr/bin/convert" )
		val gridifier = new Gridifier( functionCache, datastore, resizer, imageInfoExtractor )
		val gridRenderer = new GridRenderer( functionCache, datastore, imageInfoExtractor, "/usr/bin/convert" )
		
		val cimg = gridifier.gridifyAndStore( target )
		if( cimg == null ) {
			System.err.println("No images found!")
			return
		}
		
		System.out.println( "rasterize("+cimg + ") = " + gridRenderer.rasterize( cimg ) )
	}
}