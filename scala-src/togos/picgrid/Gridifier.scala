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
import togos.picgrid.file.FSSHA1Datastore
import togos.picgrid.file.SLFFunctionCache

class ImageInfo(
	val uri:String, val sourceUri:String,
	val width:Integer, val height:Integer,
	val totalImageCount:Integer
)

class ImageEntry( val name:String, val info:ImageInfo )

trait GridificationMethod
{
	def gridify( images:List[ImageEntry] ):List[CompoundImageComponent]
}

class RowlyGridificationMethod extends GridificationMethod
{
	def gridifyRows( images:List[ImageEntry], imagesPerRow:Integer ):List[CompoundImageComponent] = {
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
		
		components.toList
	}
	
	def gridifyColumns( images:List[ImageEntry], imagesPerColumn:Integer ):List[CompoundImageComponent] = {
		var columns = ListBuffer[List[ImageEntry]]()
		var column = ListBuffer[ImageEntry]()
		for( i <- images ) {
			if(column.length > imagesPerColumn ) {
				columns += column.toList
				column = ListBuffer[ImageEntry]()
			}
			column += i
		}
		if( column.length > 0 ) columns += column.toList
		
		val components = ListBuffer[CompoundImageComponent]()
		var totalHeight = 1024
		var totalImageCount = 0
		var spacing = 4
		var x = 0
		for( column <- columns ) {
			var imageSpaceAvailable = totalHeight - (column.length - 1) * spacing
			var imageHeightRatio = 0f // sum(h/w)
			for( e <- column ) {
				val i = e.info
				imageHeightRatio += i.height.toFloat / i.width
			}
			val rowWidth = (imageSpaceAvailable / imageHeightRatio).toInt
			var y = 0
			for( e <- column ) {
				val i = e.info
				val cellHeight = i.height * rowWidth / i.width
				components += new CompoundImageComponent( x, y, rowWidth, cellHeight, i.uri, e.name )
				y += cellHeight + spacing
				totalImageCount += i.totalImageCount
			}
			x += rowWidth + spacing
		}
		
		components.toList
	}
	
	def aspectRatio( components:List[CompoundImageComponent] ):Double = {
		var width, height = 0
		for( c <- components ) {
			if( c.x + c.width  > width  ) width  = c.x + c.width
			if( c.y + c.height > height ) height = c.y + c.height
		}
		width.toDouble / height
	}
	
	val aspectRatioPower = 2
	val aspectRatioWeight = 2
	val componentAreaRatioPower = 2
	val componentAreaRatioWeight = 1
	
	def aspectRatioFitness( components:List[CompoundImageComponent] ):Double = {
		val ar = aspectRatio( components )
		var dist = 1.2 / ar;
		if( dist < 1 ) dist = 1 / dist
		- Math.pow( dist, aspectRatioPower ) * aspectRatioWeight
		
		/*
		if( ar > 2.0 ) return (1.6 - ar)/1.0
		if( ar > 1.6 ) return (1.6 - ar)/1.6
		if( ar < 0.7 ) return (ar - 0.7)/0.7
		if( ar < 0.5 ) return (ar - 0.7)/0.1
		return 0
		*/
	}
	
	def componentAreaRatioFitness( components:List[CompoundImageComponent] ):Double = {
		var smallestArea = Integer.MAX_VALUE
		var largestArea = 0
		for( c <- components ) {
			val area = c.width * c.height
			if( area < smallestArea ) smallestArea = area
			if( area > largestArea ) largestArea = area
		}
		- Math.pow( largestArea.toDouble / smallestArea, componentAreaRatioPower ) * componentAreaRatioWeight
	}
	
	def fitness( components:List[CompoundImageComponent] ):Double = {
		aspectRatioFitness( components ) + componentAreaRatioFitness( components )
	}
	
	def gridify( images:List[ImageEntry] ):List[CompoundImageComponent] = {
		var bestFitness = Double.NegativeInfinity
		var bestResult:List[CompoundImageComponent] = null
		var numRows = Math.sqrt(images.length).toInt - 3
		if( numRows < 1 ) numRows = 1
		var i = 0
		while( i < 6 ) {
			val results = List(gridifyRows( images, numRows + i ), gridifyColumns( images, numRows + i ))
			for( result <- results ) {
				val fit = fitness( result )
				if( fit > bestFitness ) {
					bestResult = result
					bestFitness = fit
				}
			}
			i += 1
		}
		bestResult
	}
}

class BitmapGridificationMethod extends GridificationMethod
{
	class Bitmap( val width:Integer, val height:Integer ) {
		val data = new Array[Boolean]( width*height )
		
		def apply( x:Integer, y:Integer ):Boolean = data(x + y*width)
		def update( x:Integer, y:Integer, v:Boolean ) { data(x + y*width) = v }
		
		def spotIsOpen( x:Integer, y:Integer, w:Integer, h:Integer ):Boolean = {
			if( x+w >= width || y+h >= height ) return false
			
			var cy = 0
			while( cy < h ) {
				var cx = 0
				while( cx < w ) {
					if( this(x+cx,y+cy) ) return false
					cx += 1
				}
				cy += 1
			}
			return true
		}
		
		def markSpotUsed( x:Integer, y:Integer, w:Integer, h:Integer ) {
			var cy = 0
			while( cy < h ) {
				var cx = 0
				while( cx < w ) {
					this(x+cx,y+cy) = true
					cx += 1
				}
				cy += 1
			}
		}
		
		def findOpenSpot( w:Integer, h:Integer ):(Integer,Integer) = {
			var y = 0
			while( y < height ) {
				var x = 0
				while( x < width ) {
					if( spotIsOpen(x,y,w,h) ) return (x,y)
					x += 1
				}
				y += 1
			}
			return null
		}
		
		override def toString():String = {
			val sb:StringBuilder = new StringBuilder()
			var y = 0
			while( y < height ) {
				var x = 0
				while( x < width ) {
					sb.append( if( this(x,y) ) "X" else "." )
					x += 1
				}
				sb.append("\n")
				y += 1
			}
			return sb.toString()
		}
	}
	
	def quantize( i:ImageInfo, scale:Double ):(Integer,Integer) = {
		if( i.width >= i.height ) {
			var w = Math.round(i.width.toFloat / i.height * scale * 3).toInt
			if( w < 1 ) w = 1
			var h = Math.round(scale).toInt
			if( h < 1 ) h = 1
			return (w,h)
		} else {
			var h = Math.round(i.height.toFloat / i.width * scale).toInt
			if( h < 1 ) h = 1
			var w = Math.round(scale * 3).toInt
			if( w < 1 ) w = 1
			return (w,h)
		}
	}
	
	def computeScale( i:ImageInfo ):Double = {
		var scale = Math.log( i.totalImageCount.toDouble )/3
		if( scale < 1 ) scale = 1
		scale
	}
	
	val cellWidth = 32
	val cellHeight = 104
	val cellSpacing = 4

	def fitAll( images:List[ImageEntry], bitmapWidth:Integer, bitmapHeight:Integer ):List[CompoundImageComponent] = {
		val bitmap = new Bitmap( bitmapWidth, bitmapHeight )
		val components = new ListBuffer[CompoundImageComponent]
		for( e <- images ) {
			val i = e.info
			val (cellsWide,cellsTall) = quantize(i, computeScale(i))
			val loc = bitmap.findOpenSpot( cellsWide, cellsTall )
			if( loc == null ) return null
			bitmap.markSpotUsed( loc._1, loc._2, cellsWide, cellsTall )
			val cx = (cellWidth + cellSpacing) * loc._1
			val cw = cellWidth * cellsWide + cellSpacing * (cellsWide - 1)
			val cy = (cellHeight + cellSpacing) * loc._2
			val ch = cellHeight * cellsTall + cellSpacing * (cellsTall - 1)
			components += new CompoundImageComponent( cx, cy, cw, ch, i.uri, e.name )
		}
		return components.toList
	}
		
	def gridify( images:List[ImageEntry] ):List[CompoundImageComponent] = {
		var totalWidth :Double = 0
		var totalHeight:Double = 0
		var totalArea  :Double = 0
		var totalImageCount:Int = 0
		for( e <- images ) {
			val i = e.info
			val scale = computeScale(i)
			totalWidth += i.width * scale
			totalHeight += i.height * scale
			totalArea += scale * scale
			totalImageCount += i.totalImageCount
		}
		
		val averageAspectRatio = totalWidth / totalHeight
		System.err.println("Average aspect ratio = "+averageAspectRatio)
		System.err.println("Total area = "+totalArea)
		
		val outerHeight = Math.sqrt( totalArea / averageAspectRatio )
		val outerWidth = outerHeight * averageAspectRatio
		
		// Bitmap format:
		// 1x3 image = | (normal scale images can be no smaller than this)
		// 2x3 image = []
		// 3x3 image = [#]
		// 4x3 image = [##]
		// double scale image takes 2 lines, etc
		// bitmap is arranged rows-first
		
		var bitmapWidth:Integer = Math.round(outerWidth)*3 toInt
		var bitmapHeight:Integer = Math.round(outerHeight) toInt
		
		var components = fitAll( images, bitmapWidth, bitmapHeight )
		while( components == null ) {
			bitmapHeight += 1
			components = fitAll( images, bitmapWidth, bitmapHeight )
		}
		
		val resultWidth = bitmapWidth * cellWidth + (bitmapWidth-1) * cellSpacing
		val resultHeight = bitmapHeight * cellHeight + (bitmapHeight-1) * cellSpacing
		
		components
	}	
}

class Gridifier(
	val functionCache:FunctionCache,
	val datastore:Datastore,
	val infoExtractor:ImageInfoExtractor,
	val gridificationMethod:GridificationMethod
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
	
	def gridify( images:List[ImageEntry], generatedFromUri:String ):ImageInfo = {
		if( images.length == 0 ) return null
		if( images.length == 1 ) return images.head.info
		
		val components = gridificationMethod.gridify( images )
		
		var totalImageCount = 0
		for( image <- images ) totalImageCount += image.info.totalImageCount
		
		var width, height = 0
		for( c <- components ) {
			if( c.x + c.width  > width  ) width  = c.x + c.width
			if( c.y + c.height > height ) height = c.y + c.height
		}
		
		val ci = new CompoundImage( width, height, components, null, null, totalImageCount, generatedFromUri )
		
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
		var functionCacheDir:String = null
		var i = 0
		var target:String = null
		while( i < args.length ) {
			args(i) match {
				case "-convert-path" =>
					i += 1
					ImageMagickCommands.convertPath = args(i)
				case "-function-cache-dir" =>
					i += 1
					functionCacheDir = args(i)
				case "-datastore" =>
					i += 1
					datastoreDir = args(i) 
				case "-datasource" =>
					i += 1
					datasources += args(i)
				case "-ms-datasource" => // Multi-sector datasource (e.g. ccouch/data)
					i += 1
					val msd = new File(args(i))
					for( f <- msd.listFiles() ) if( f.isDirectory() ) {
						datasources += f.getPath();
					}
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
		
		val functionCache:FunctionCache =
			if( functionCacheDir != null ) {
				new SLFFunctionCache( new File(functionCacheDir) )
			} else {
				new MemoryFunctionCache()
			}
		val imageInfoExtractor = new ImageInfoExtractor( functionCache, datastore )
		val resizer = new ImageMagickCropResizer( datastore, ImageMagickCommands.convert )
		val gridificationMethod = new RowlyGridificationMethod
		val gridifier = new Gridifier( functionCache, datastore, imageInfoExtractor, gridificationMethod )
		val rasterizer = new CompoundImageRasterizer( functionCache, datastore, imageInfoExtractor, resizer, ImageMagickCommands.convert )
		val htmlizer = new CompoundImageHTMLizer( datastore, imageInfoExtractor, rasterizer )
		
		val cimg = gridifier.gridifyDir( target )
		if( cimg == null ) {
			System.err.println("No images found!")
			return
		}
		
		System.out.println( "Compound image URI = "+cimg.uri )
		
		System.out.println( "rasterize("+cimg.uri+") = " + rasterizer.rasterize( cimg.uri ) )
		
		System.out.println( "pagify("+cimg.uri+") = " + htmlizer.pagify( cimg.uri ) )
	}
}
