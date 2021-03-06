package togos.picgrid.layout

import togos.picgrid.ImageEntry
import togos.picgrid.image.CompoundImageComponent

class MultiFitLayouter( val subLayouters:Seq[Layouter] ) extends Layouter
{
	def cacheString:String = {
		var str = "multifit-v1"
		for( l <- subLayouters ) str += "+("+l.cacheString+")"
		return str
	}
	
	def size( cs:Seq[LayoutCell] ):(Float,Float) = {
		if( cs.size == 0 ) return (0,0)
		
		var minX, minY = Float.MaxValue
		var maxX, maxY = Float.MinValue
		for( c <- cs ) {
			if( c.x < minX ) minX = c.x
			if( c.y < minY ) minY = c.y
			if( c.x + c.w > maxX ) maxX = c.x + c.w
			if( c.y + c.h > maxY ) maxY = c.y + c.h
		}
		return (maxX-minX,maxY-minY)
	}
	
	def ratio( x:Float, y:Float ) = if(x > y) x/y else y/x
	
	// TODO: Take weight / size ratio into account
	// (will be easier when CICs are replaced with LayoutCells)
	
	def fitness( cs:Seq[LayoutCell] ):Float = {
		// -1 for more than 12x variation in area
		// -1 for ratio more diff than 2:1
		// -2 for width or height of a component < 16 
		// -size ratio
		
		if( cs.size == 0 ) return 0
		
		var minArea = Float.MaxValue
		var maxArea = Float.MinValue
		var hasTinyImages = false 
		for( c <- cs ) {
			val a = c.w * c.h
			if( a < minArea ) minArea = a
			if( a > maxArea ) maxArea = a
			if( c.w < 16 || c.h < 16 ) hasTinyImages = true
		}
		val (w,h) = size(cs)
		
		var fitness = 0f
		if( maxArea / minArea > 12 ) fitness -= 1
		if( hasTinyImages ) fitness -= 2
		fitness -= ratio(w,h)
		if( ratio(w,h) > 2 ) fitness -= 1
		return fitness
	}
	
	def layout( images:Seq[ImageEntry], context:String ):Layout = {
		var bestFitness = Float.MinValue
		var mostFit:Layout = null
		for( l <- subLayouters ) {
			val cs = l.layout( images, context )
			val fit = fitness(cs.cells)
			if( fit > bestFitness ) {
				mostFit = cs
				bestFitness = fit
			}
		}
		return mostFit
	}
}
