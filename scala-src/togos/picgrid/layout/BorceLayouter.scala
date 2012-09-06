package togos.picgrid.layout

import togos.ccouch2.store.Store
import togos.picgrid.ImageEntry
import scala.collection.mutable.ListBuffer
import togos.picgrid.image.CompoundImageComponent

class BorceLayouter(val maxWidth:Int, val maxHeight:Int) extends Layouter
{
	def configString = "borce:%dx%d".format(maxWidth,maxHeight)
	
	class ImageInfo( val w:Int, val h:Int, val weight:Float, val urn:String, val name:String )
	class CellTree( val subTrees:Seq[CellTree], val image:ImageInfo, val weight:Float ) {
		def this( image:ImageInfo ) = this( List.empty, image, image.weight )
		def this( subTrees:Seq[CellTree] ) = this(subTrees, null, subTrees.map(_.weight).sum)
	}
	
	class Partitioner( val weightFunction:(Float=>Float), val weightWeight:Int, val countWeight:Int ) {
		def weightSplitDiagram( weights:Seq[Float], splitPoint:Int ):String = {
			var total = 0f
			for( w <- weights ) total += w
			val scale = 75 / total
			val rWeights = for(w <- weights) yield math.round(w * scale)
			var rTotal = 0
			for( rWeight <- rWeights ) rTotal += rWeight
			var dia = ""
			var i = 0
			for( rWeight <- rWeights ) {
				dia += ((if(i % 2 == 0) "#" else "$") * rWeight)
				i += 1
			}
			val rScale = rTotal / total
			return (
				"Split at " + splitPoint + "/" + weights.length + ":\n" + 
				dia + "\n" + (" " * (rTotal / 2).toInt) + "^"
			)
		}
		
		def findSplitPoint( images:Seq[ImageInfo], totalWeight:Float ):Int = {
			var midWeightIndex = 0
			var prevWeight, nextWeight = 0f
			val target = totalWeight / 2
			while( midWeightIndex < images.size ) {
				nextWeight = prevWeight + weightFunction(images(midWeightIndex).weight)
				if( target - prevWeight < nextWeight - target ) {
					return midWeightIndex
				}
				prevWeight = nextWeight
				midWeightIndex += 1
			}
			
			assert( midWeightIndex > 0, "Somehow mid-weight index is > 0; totalWeight = "+totalWeight+", weights = "+images.map(i => i.weight) )
			assert( midWeightIndex < images.length, "Somehow mid-weight index is at end; totalWeight = "+totalWeight+", weights = "+images.map(i => i.weight) )
			
			return midWeightIndex
		}
		
		def partition( images:Seq[ImageInfo] ):CellTree = {
			if( images.size == 0 ) {
				throw new Exception("Image list is empty!  Can't build CellTree from it.")
			} else if( images.size == 1 ) {
				return new CellTree( images(0) )
			} else if( images.size == 2 ) {
				return new CellTree( List(new CellTree(images(0)), new CellTree(images(1))) )
			}
			
			var totalWeight = 0f
			for( image <- images ) {
				totalWeight += weightFunction(image.weight)
			}
			
			val midWeightIndex = math.round( (weightWeight * (images.length / 2f) + countWeight * findSplitPoint(images, totalWeight)) / (weightWeight + countWeight) )
			
			//System.err.println( weightSplitDiagram(for(i<-images) yield i.weight, midWeightIndex) )
			
			return new CellTree( List( partition(images.slice(0,midWeightIndex)), partition(images.slice(midWeightIndex,images.size)) ) )
		}
	}
	
	val partitioners = List(
		new Partitioner( i => math.sqrt(i).toFloat, 0, 1 ),
		new Partitioner( i => math.sqrt(i).toFloat, 1, 2 ),
		new Partitioner( i => math.sqrt(i).toFloat, 2, 2 ),
		new Partitioner( i => math.sqrt(i).toFloat, 2, 1 ),
		new Partitioner( i => math.sqrt(i).toFloat, 1, 0 ),
		new Partitioner( i => i.toFloat, 0, 1 ),
		new Partitioner( i => i.toFloat, 0, 2 ),
		new Partitioner( i => i.toFloat, 2, 2 ),
		new Partitioner( i => i.toFloat, 2, 1 ),
		new Partitioner( i => i.toFloat, 1, 0 ),
		new Partitioner( i => i.toFloat, 0, 1 ),
		new Partitioner( i => i * i.toFloat, 0, 2 ),
		new Partitioner( i => i * i.toFloat, 2, 2 ),
		new Partitioner( i => i * i.toFloat, 2, 1 ),
		new Partitioner( i => i * i.toFloat, 1, 0 )
	)
	
	def minDepth( ct:CellTree ):Int = {
		if( ct.image != null ) return 0
		
		var min = Int.MaxValue
		for( s <- ct.subTrees ) {
			min = math.min( min, 1+minDepth(s) )
		}
		return min
	}
	
	def maxDepth( ct:CellTree ):Int = {
		var max = 0
		for( s <- ct.subTrees ) {
			max = math.max( max, 1+maxDepth(s) )
		}
		return max
	}
	
	def fitness2( ct:CellTree ):Float = {
		val l = ct.subTrees.length
		val totalWeight = ct.weight
		val expectedSubWeight = ct.weight / l
		var fit = 0f
		for( t <- ct.subTrees ) {
			fit -= ratio(expectedSubWeight, t.weight) / l
			fit += fitness(t) / l
		}
		return fit
	}
	
	def fitness( ct:CellTree ):Float = fitness2(ct)/2 - maxDepth(ct)
	
	def partition( images:Seq[ImageInfo] ):CellTree = {
		var minFit = Float.MaxValue
		var maxFit = Float.MinValue
		var mostFit:CellTree = null
		for( p <- partitioners ) {
			val ct = p.partition(images)
			val fit = fitness(ct)
			// System.err.println("Fitness = "+fit)
			if( fit < minFit ) {
				minFit = fit
			}
			if( fit > maxFit ) {
				mostFit = ct
				maxFit = fit
			}
		}
		// System.err.println("Min fit = "+minFit+", max fit = "+maxFit)
		return mostFit
	}
	
	class LayoutComponent( val x:Int,val y:Int,val w:Int, val h:Int, val l:Layout )
	class Layout( val w:Int, val h:Int, val components:Seq[LayoutComponent], val image:ImageInfo ) {
		def this( w:Int, h:Int, components:Seq[LayoutComponent] ) = this(w,h,components,null)
		def this( w:Int, h:Int, image:ImageInfo ) = this(w,h,List.empty,image)
	}
	
	def swap( x:Int, y:Int, really:Boolean ):(Int,Int) = if( really ) (x,y) else (y,x)
	
	def _layout( subLayouts:Seq[Layout], maxSB:Int, vertical:Boolean ):Layout = {
		var a, b = 0
		
		var components = List[LayoutComponent]()
		
		for( subLayout <- subLayouts ) {
			/*
			val scaledWidth = (subLayout.w * maxSB.toFloat / subLayout.h).toInt
			components = new LayoutComponent( a, b, scaledWidth, maxSB, subLayout ) :: components
			a += scaledWidth
			*/
			
			val (slSA,slSB) = swap(subLayout.w, subLayout.h, vertical)
			val scaledSA = (slSA * (maxSB.toFloat/slSB)).toInt // remove toInt?
			val (slX,slY) = swap(a, b, vertical)
			val (slW,slH) = swap(scaledSA,maxSB, vertical)
			components = new LayoutComponent( slX, slY, slW, slH, subLayout ) :: components
			a += scaledSA
		}
		
		val (w,h) = swap(a,maxSB,vertical)
		return new Layout( w, h, components )
		//return new Layout( a, maxSB, components )
	}
	
	def ratio( x:Float, y:Float ):Float = if(x>y) x / y else y / x
	
	def layout( ct:CellTree ):Layout = {
		if( ct.image != null ) return new Layout(ct.image.w, ct.image.h, ct.image)
		
		var subLayouts = List[Layout]()
		var maxWidth, maxHeight = 0
		for( subTree <- ct.subTrees ) {
			val l = layout(subTree)
			maxWidth = math.max( maxWidth, l.w )
			maxHeight = math.max( maxHeight, l.h )
			subLayouts = l :: subLayouts
		}
		subLayouts = subLayouts.reverse
		
		// return _layout( subLayouts,  maxHeight, false )
		
		val h = _layout( subLayouts, maxHeight, false )
		val v = _layout( subLayouts, maxWidth,  true  )
		if( ratio(h.w,h.h) < ratio(v.w,v.h) ) h else v
	}
	
	def roundBounds( x:Float, y:Float, w:Float, h:Float ):(Int,Int,Int,Int) = {
		val endX = math.round(x+w); val endY = math.round(y+h)
		val beginX = math.round(x); val beginY = math.round(y);
		return (beginX,beginY,endX-beginX,endY-beginY)
	}
	
	def layoutToCells( l:Layout, x:Float, y:Float, w:Float, h:Float, cellDest:(CompoundImageComponent => Unit) ) {
		if( l.image != null ) {
			val (cx,cy,cw,ch) = roundBounds(x,y,w,h)
			cellDest( new CompoundImageComponent(cx, cy, cw, ch, l.image.urn, l.image.name) )
		} else {
			val scaleX = w / l.w; val scaleY = h / l.h
			for( lc <- l.components ) {
				layoutToCells( lc.l,
					x + scaleX*lc.x, y + scaleY*lc.y,
					    scaleX*lc.w,     scaleY*lc.h,
				cellDest )
			}
		}
	}
	
	def layoutToCells( l:Layout, x:Float, y:Float, w:Float, h:Float ):List[CompoundImageComponent] = {
		var list = List[CompoundImageComponent]()
		layoutToCells(l,x,y,w,h, (i => list = i :: list ) )
		list
	}
	
	def fitInside( w:Float, h:Float, maxWidth:Float, maxHeight:Float ):(Float,Float) = {
		if( w <= maxWidth && h <= maxHeight ) return (w,h)
		val ratio = math.min( maxWidth / w, maxHeight / h )
		return (w * ratio, h * ratio)
	}
	
	def imageEntriesToInfo( entries:Seq[ImageEntry] ):Seq[ImageInfo] = {
		for( e <- entries ) yield new ImageInfo( e.info.width, e.info.height, e.info.totalByteCount, e.info.uri, e.name )
	}
	
	/**
	 * Shrinks the component by s pixels on each side.
	 */
	def bordify( c:CompoundImageComponent, s1:Int, s2:Int ) =
		new CompoundImageComponent( c.x + s1, c.y + s1, c.width - s1 - s2, c.height - s1 - s2, c.uri, c.name )
	
	def gridify( entries:Seq[ImageEntry], maxWidth:Int, maxHeight:Int ):List[CompoundImageComponent] = {
		val l = layout( partition( imageEntriesToInfo(entries) ) )
		val (w,h) = fitInside( l.w, l.h, maxWidth, maxHeight )
		
		val rawCells = layoutToCells( l, 0, 0, w, h )
		var minSize:Int = Int.MaxValue
		for( c <- rawCells ) {
			minSize = math.min( minSize, math.min(c.height, c.width) )
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
		
		val spacing1:Int = math.floor(spacing/2f).toInt
		val spacing2:Int = math.ceil( spacing/2f).toInt
		
		var cells = List[CompoundImageComponent]()
		layoutToCells( l, -spacing1, -spacing1, w+spacing1+spacing2, h+spacing1+spacing2,
			(c:CompoundImageComponent) => cells = bordify(c,spacing1,spacing2) :: cells
		)
		
		// Sanity checks
		for( c <- cells ) {
			assert( c.width > 0                , "Cell width is <= 0: "+c )
			assert( c.height > 0               , "Cell height is <= 0: "+c )
			assert( c.x >= 0                   , "Cell X pos is < 0: "+c )
			assert( c.y >= 0                   , "Cell Y pos is < 0: "+c )
			assert( c.x + c.width  <= maxWidth , "Cell X+width is > "+maxWidth+": "+c )
			assert( c.y + c.height <= maxHeight, "Cell Y+height is > "+maxHeight+": "+c )
		}
		return cells
	}
	
	def gridify( images:Seq[ImageEntry] ) = gridify(images, maxWidth, maxHeight)
}
