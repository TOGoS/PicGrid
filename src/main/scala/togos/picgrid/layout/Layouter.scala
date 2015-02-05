package togos.picgrid.layout

import togos.picgrid.ImageEntry
import togos.picgrid.image.CompoundImageComponent
import togos.picgrid.ImageInfo

class LayoutCell( val entry:ImageEntry, val x:Float, val y:Float, val w:Float, val h:Float )

class Layout( val layouter:Layouter, val cells:Seq[LayoutCell] )

trait Layouter
{
	/**
	 * Return a string that can be used to identify this instance for caching its results.
	 * Should include the class, a version, and any configuration.
	 * e.g. bob-v5-640x480  
	 */
	def cacheString:String
	def layout( images:Seq[ImageEntry] ):Layout
}

abstract class AutoSpacingLayouter( val maxWidth:Int, val maxHeight:Int ) extends Layouter
{
	def _gridify( images:Seq[ImageEntry] ):Seq[LayoutCell]
	
	/**
	 * Squishes them into the allowed area and adds space
	 * between cells.
	 */
	def squish( cells:Seq[LayoutCell] ):Seq[LayoutCell] = {
		if( cells.size == 0 ) return List()
		
		var minX, minY = Float.MaxValue
		var maxX, maxY = Float.MinValue
		for( c <- cells ) {
			if( c.x < minX ) minX = c.x
			if( c.y < minY ) minY = c.y
			if( c.x + c.w > maxX ) maxX = c.x + c.w
			if( c.y + c.h > maxY ) maxY = c.y + c.h
		}
		
		val inWidth  = maxX - minX
		val inHeight = maxY - minY
		
		var scale = 1f
		if( scale * inWidth  > maxWidth  ) scale = maxWidth  / inWidth
		if( scale * inHeight > maxHeight ) scale = maxHeight / inHeight
		val outWidth  = inWidth  * scale
		val outHeight = inHeight * scale
		
		var minSize = Float.MaxValue
		for( c <- cells ) {
			minSize = math.min( minSize, scale*math.min(c.w, c.h) )
		}
		
		val spacing:Int =
			if(       minSize >= 13 )
				4
			else if( minSize >= 9 )
				3
			else if( minSize >= 6 )
				2
			else if( minSize >= 4 )
				1
			else
				0
		
		val spacedDX = 0 - minX
		val spacedDY = 0 - minY
		val spacedSX = (outWidth +spacing)/(inWidth )
		val spacedSY = (outHeight+spacing)/(inHeight)
		
		//System.err.println( "%f, %f to %f, %f ; target = %f, %f".format(minX,minY,maxX,maxY,outWidth,outHeight) )
		//System.err.println( "x * %f + %f ; y * %f + %f".format(spacedSX,spacedDX,spacedSY,spacedDY) )
		
		return for( c <- cells ) yield new LayoutCell(
			c.entry,
			(c.x-minX) * spacedSX + spacedDX,
			(c.y-minY) * spacedSY + spacedDY,
			c.w * spacedSX - spacing,
			c.h * spacedSY - spacing
		)
	}
	
	def quantize( c:LayoutCell ):LayoutCell = new LayoutCell(
		c.entry,
		math.round(c.x), math.round(c.y),
		math.round(c.w + c.x) - math.round(c.x),
		math.round(c.h + c.y) - math.round(c.y)
	)
	
	def quantize( cells:Seq[LayoutCell] ):Seq[LayoutCell] = for( c <- cells ) yield quantize(c)
	
	def layout( images:Seq[ImageEntry] ):Layout = {
		val cells = quantize(squish(_gridify( images )))
		
		// Sanity checks
		for( c <- cells ) {
			assert( c.w > 0               , "Cell width is <= 0: "+c )
			assert( c.h > 0               , "Cell height is <= 0: "+c )
			assert( c.x >= 0              , "Cell X pos is < 0: "+c )
			assert( c.y >= 0              , "Cell Y pos is < 0: "+c )
			assert( c.x + c.w <= maxWidth , "Cell X+width is > "+maxWidth+": "+c )
			assert( c.y + c.h <= maxHeight, "Cell Y+height is > "+maxHeight+": "+c )
		}
		
		return new Layout( this, cells )
	}
}

object Layouter
{
	val DEFAULT_DIMS = (1280,800)
	
	val DIMS = """^(\d+)x(\d+)$""".r
	
	def parseDims( s:String ):(Int,Int) = s match {
		case DIMS(w,h) => (w.toInt,h.toInt)
		case _ => throw new Exception("Failed to parse '"+s+"' as dimensions")
	}
	
	def fromString( str:String ):Layouter = {
		val parts = str.split(":")
		
		val (w,h) = if( parts.size >= 2 ) parseDims(parts(1)) else DEFAULT_DIMS 
		
		return parts(0) match {
			case "rowly" => new RowlyLayouter(w,h)
			case "borce" => new BorceLayouter(w,h)
			case "multifit" => new MultiFitLayouter( List(
				new RowlyLayouter(w,h),
				new BorceLayouter(w,h)
			) )
			case _ => throw new Exception("Unrecognised layouter: '"+parts(0)+"'")
		}
	}
}
