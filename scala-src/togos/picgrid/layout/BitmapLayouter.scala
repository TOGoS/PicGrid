package togos.picgrid.layout

import scala.collection.mutable.ListBuffer
import togos.picgrid.image.CompoundImageComponent
import togos.picgrid.ImageEntry
import togos.picgrid.ImageInfo
import togos.picgrid.BlobAutoStore
import togos.picgrid.image.ImageInfoExtractor
import togos.picgrid.image.CompoundImage
import togos.picgrid.image.CompoundImageComponent
import togos.blob.ByteChunk
import java.security.MessageDigest
import org.bitpedia.util.Base32
import togos.picgrid.FunctionCache
import togos.picgrid.SerializationUtil

class BitmapLayouter extends Layouter
{
	def cacheString = "bitmap-v1"
	
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
		for( e <- images) yield {
			val i = e.info
			val (cellsWide, cellsTall) = quantize(i, computeScale(i))
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
