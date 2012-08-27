package togos.picgrid

import java.security.MessageDigest
import scala.collection.mutable.ListBuffer
import org.bitpedia.util.Base32
import togos.picgrid.BlobConversions._
import togos.picgrid.image.CompoundImage
import togos.picgrid.image.CompoundImageComponent
import togos.picgrid.image.ImageInfoExtractor
import togos.blob.ByteChunk
import togos.ccouch2.store.Store
import java.lang.Integer

@serializable
@SerialVersionUID(1)
class ImageInfo(
	val uri:String, val sourceUri:String,
	val width:Int, val height:Int,
	val totalImageCount:Int
)

@serializable
@SerialVersionUID(1)
class ImageEntry( val name:String, val info:ImageInfo )

trait GridificationMethod
{
	def configString:String
	def gridify( images:Seq[ImageEntry] ):List[CompoundImageComponent]
}

class RowlyGridificationMethod extends GridificationMethod
{
	def configString = "rowly-default"
	
	def gridifyRows( images:Seq[ImageEntry], imagesPerRow:Int ):List[CompoundImageComponent] = {
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
			val rowHeight = (imageSpaceAvailable / imageWidthRatio)
			var x = 0f
			for( e <- row ) {
				val i = e.info
				val cellWidth = i.width * rowHeight / i.height
				components += new CompoundImageComponent( x.toInt, y, cellWidth.toInt, rowHeight.toInt, i.uri, e.name )
				x += cellWidth + spacing
				totalImageCount += i.totalImageCount
			}
			y += rowHeight.toInt + spacing
		}
		
		components.toList
	}
	
	def gridifyColumns( images:Seq[ImageEntry], imagesPerColumn:Int ):List[CompoundImageComponent] = {
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
			val rowWidth = (imageSpaceAvailable / imageHeightRatio)
			var y = 0f
			for( e <- column ) {
				val i = e.info
				val cellHeight = i.height * rowWidth / i.width
				components += new CompoundImageComponent( x, y.toInt, rowWidth.toInt, cellHeight.toInt, i.uri, e.name )
				y += cellHeight + spacing
				totalImageCount += i.totalImageCount
			}
			x += rowWidth.toInt + spacing
		}
		
		components.toList
	}
	
	def aspectRatio( components:Seq[CompoundImageComponent] ):Double = {
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
	
	def aspectRatioFitness( components:Seq[CompoundImageComponent] ):Double = {
		val ar = aspectRatio( components )
		var dist = 1.2 / ar;
		if( dist < 1 ) dist = 1 / dist
		- math.pow( dist, aspectRatioPower ) * aspectRatioWeight
		
		/*
		if( ar > 2.0 ) return (1.6 - ar)/1.0
		if( ar > 1.6 ) return (1.6 - ar)/1.6
		if( ar < 0.7 ) return (ar - 0.7)/0.7
		if( ar < 0.5 ) return (ar - 0.7)/0.1
		return 0
		*/
	}
	
	def componentAreaRatioFitness( components:Seq[CompoundImageComponent] ):Double = {
		var smallestArea = Integer.MAX_VALUE
		var largestArea = 0
		for( c <- components ) {
			val area = c.width * c.height
			if( area < smallestArea ) smallestArea = area
			if( area > largestArea ) largestArea = area
		}
		- math.pow( largestArea.toDouble / smallestArea, componentAreaRatioPower ) * componentAreaRatioWeight
	}
	
	def fitness( components:Seq[CompoundImageComponent] ):Double = {
		aspectRatioFitness( components ) + componentAreaRatioFitness( components )
	}
	
	def gridify( images:Seq[ImageEntry] ):List[CompoundImageComponent] = {
		var bestFitness = Double.NegativeInfinity
		var bestResult:List[CompoundImageComponent] = null
		var numRows = math.sqrt(images.length).toInt - 3
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

class MigratingSource[K,V]( val old:Function[K,V], val neu:Store[K,V]) extends Store[K,V]
{
	def apply( key:K ):V = {
		var v = neu(key)
		if( v != null ) return v
		v = old(key)
		if( v != null ) neu(key) = v
		return v
	}
	
	def update( key:K, value:V ) {
		neu(key) = value
	}
}

class BitmapGridificationMethod extends GridificationMethod
{
	def configString = "bitmap-default"
	
	class Bitmap( val width:Int, val height:Int ) {
		val data = new Array[Boolean]( width*height )
		
		def apply( x:Int, y:Int ):Boolean = data(x + y*width)
		def update( x:Int, y:Int, v:Boolean ) { data(x + y*width) = v }
		
		def spotIsOpen( x:Int, y:Int, w:Int, h:Int ):Boolean = {
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
		
		def markSpotUsed( x:Int, y:Int, w:Int, h:Int ) {
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
		
		def findOpenSpot( w:Int, h:Int ):(Int,Int) = {
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
	
	def quantize( i:ImageInfo, scale:Double ):(Int,Int) = {
		if( i.width >= i.height ) {
			var w = math.round(i.width.toFloat / i.height * scale * 3).toInt
			if( w < 1 ) w = 1
			var h = math.round(scale).toInt
			if( h < 1 ) h = 1
			return (w,h)
		} else {
			var h = math.round(i.height.toFloat / i.width * scale).toInt
			if( h < 1 ) h = 1
			var w = math.round(scale * 3).toInt
			if( w < 1 ) w = 1
			return (w,h)
		}
	}
	
	def computeScale( i:ImageInfo ):Double = {
		var scale = math.log( i.totalImageCount.toDouble )/3
		if( scale < 1 ) scale = 1
		scale
	}
	
	val cellWidth = 32
	val cellHeight = 104
	val cellSpacing = 4

	def fitAll( images:Seq[ImageEntry], bitmapWidth:Int, bitmapHeight:Int ):List[CompoundImageComponent] = {
		val bitmap = new Bitmap( bitmapWidth, bitmapHeight )
		val components = new ListBuffer[CompoundImageComponent]
		for( e <- images ) {
			val i = e.info
			val (cellsWide,cellsTall) = quantize(i, computeScale(i))
			val loc = bitmap.findOpenSpot( cellsWide, cellsTall )
			if( loc == null ) return null
			bitmap.markSpotUsed( loc._1, loc._2, cellsWide, cellsTall )
			val cx:Int = (cellWidth + cellSpacing) * loc._1
			val cw = cellWidth * cellsWide + cellSpacing * (cellsWide - 1)
			val cy = (cellHeight + cellSpacing) * loc._2
			val ch = cellHeight * cellsTall + cellSpacing * (cellsTall - 1)
			components += new CompoundImageComponent( cx, cy, cw, ch, i.uri, e.name )
		}
		return components.toList
	}
		
	def gridify( images:Seq[ImageEntry] ):List[CompoundImageComponent] = {
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
		
		val outerHeight = math.sqrt( totalArea / averageAspectRatio )
		val outerWidth = outerHeight * averageAspectRatio
		
		// Bitmap format:
		// 1x3 image = | (normal scale images can be no smaller than this)
		// 2x3 image = []
		// 3x3 image = [#]
		// 4x3 image = [##]
		// double scale image takes 2 lines, etc
		// bitmap is arranged rows-first
		
		var bitmapWidth:Int = math.round(outerWidth)*3 toInt
		var bitmapHeight:Int = math.round(outerHeight) toInt
		
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
	val datastore:BlobAutoStore,
	val infoExtractor:ImageInfoExtractor,
	val gridificationMethod:GridificationMethod
) {
	val xRdfSubjectRegex = """x-rdf-subject:(.*)""".r
	val imageFilenameRegex = """(?i).*\.(?:png|jpe?g|gif|bmp)$""".r
	
	def getDirEntries( uri:String ):List[DirectoryEntry] = {
		val xRdfSubjectRegex(blobUri) = uri
		val rdfBlob = datastore(blobUri)
		if( rdfBlob == null ) {
			throw new Exception("Cannot find blob "+uri+" to parse directory entries from.");
		}
		RDFDirectoryParser.parseDirectory( rdfBlob )
	}
	
	def gridifyRasterImage( uri:String, name:String ):ImageEntry = {
		val dims = infoExtractor.getImageDimensions( uri )
		if( dims == null ) return null
		val (w,h) = dims
		new ImageEntry( name, new ImageInfo( uri, uri, w, h, 1 ) )
	}
	
	def gridify( e:DirectoryEntry ):ImageEntry = {
		e.targetClass match {
			case DirectoryObjectClass.Blob => e.name match {
				case imageFilenameRegex() => gridifyRasterImage(e.targetUri, e.name)
				case _ => null
			}
			case DirectoryObjectClass.Directory =>
				gridifyDir( e.targetUri, e.name )
		}
	}
	
	def gridify( images:Seq[ImageEntry], name:String, generatedFromUri:String ):ImageEntry = {
		if( images.length == 0 ) return null
		if( images.length == 1 ) return images.head
		
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
		new ImageEntry( name, new ImageInfo( uri, generatedFromUri, ci.width, ci.height, ci.totalImageCount ) )
	}
	
	
	def gridifyDir( dir:Seq[DirectoryEntry], name:String, generatedFromUri:String ):ImageEntry = {
		gridify( dir.map( e => gridify(e) ).filter( e => e != null ), name, generatedFromUri )
	}
	
	def hashString( s:String ):String = {
		val sha1 = MessageDigest.getInstance("SHA-1")
		val sha1Hash = sha1.digest( s )
		Base32.encode(sha1Hash)
	}
	
	lazy val configHash = gridificationMethod.configString
	
	def gridifyDir( uri:String, name:String ):ImageEntry = {
		val cacheKey = configHash+":"+uri
		val cachedData:ByteChunk = functionCache( cacheKey )
		if( cachedData != null ) {
			SerializationUtil.unserialize(cachedData).asInstanceOf[ImageEntry]
		} else {
			val res:ImageEntry = gridifyDir( getDirEntries( uri ), name, uri )
			functionCache( cacheKey ) = SerializationUtil.serialize( res )
			res
		}
	}
}
