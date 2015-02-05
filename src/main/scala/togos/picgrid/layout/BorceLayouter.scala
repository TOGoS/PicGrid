package togos.picgrid.layout

import togos.ccouch2.store.Store
import togos.picgrid.ImageEntry
import scala.collection.mutable.ListBuffer
import togos.picgrid.image.CompoundImageComponent

class BorceLayouter(maxWidth:Int, maxHeight:Int) extends AutoSpacingLayouter( maxWidth, maxHeight )
{
	def cacheString = "borce-v3-%dx%d".format(maxWidth,maxHeight)
	
	class CellTree( val subTrees:Seq[CellTree], val entry:ImageEntry, val weight:Float ) {
		def this( entry:ImageEntry ) = this( List.empty, entry, entry.info.totalByteCount )
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
		
		def findSplitPoint( images:Seq[ImageEntry], totalWeight:Float ):Int = {
			var midWeightIndex = 0
			var prevWeight, nextWeight = 0f
			val target = totalWeight / 2
			while( midWeightIndex < images.size ) {
				nextWeight = prevWeight + weightFunction(images(midWeightIndex).info.totalByteCount)
				if( target - prevWeight < nextWeight - target ) {
					return midWeightIndex
				}
				prevWeight = nextWeight
				midWeightIndex += 1
			}
			
			assert( midWeightIndex > 0, "Somehow mid-weight index is > 0; totalWeight = "+totalWeight+", weights = "+images.map(i => i.info.totalByteCount) )
			assert( midWeightIndex < images.length, "Somehow mid-weight index is at end; totalWeight = "+totalWeight+", weights = "+images.map(i => i.info.totalByteCount) )
			
			return midWeightIndex
		}
		
		def partition( images:Seq[ImageEntry] ):CellTree = {
			if( images.size == 0 ) {
				throw new Exception("Image list is empty!  Can't build CellTree from it.")
			} else if( images.size == 1 ) {
				return new CellTree( images(0) )
			} else if( images.size <= 2 ) {
				return new CellTree( for(i<-images) yield new CellTree(i) )
			}
			
			var totalWeight = 0f
			for( image <- images ) {
				totalWeight += weightFunction(image.info.totalByteCount)
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
		if( ct.entry != null ) return 0
		
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
		val expectedSubWeight = totalWeight / l
		var fit = 0f
		for( t <- ct.subTrees ) {
			// If it takes up 1/3 of the tree, we'd like it to have 1/3 of the weight:
			fit -= ratio(expectedSubWeight, t.weight) / l
			fit += fitness(t) / l
		}
		return fit
	}
	
	def fitness( ct:CellTree ):Float = fitness2(ct)/2 - maxDepth(ct)
	
	def partition( images:Seq[ImageEntry] ):CellTree = {
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
	class Layout( val w:Int, val h:Int, val components:Seq[LayoutComponent], val entry:ImageEntry ) {
		def this( w:Int, h:Int, components:Seq[LayoutComponent] ) = this(w,h,components,null)
		def this( w:Int, h:Int, entry:ImageEntry ) = this(w,h,List.empty,entry)
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
		if( ct.entry != null ) return new Layout(ct.entry.info.width, ct.entry.info.height, ct.entry)
		
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
	
	def layoutToCells( l:Layout, x:Float, y:Float, w:Float, h:Float, cellDest:(LayoutCell => Unit) ) {
		if( l.entry != null ) {
			cellDest( new LayoutCell( l.entry, x, y, w, h ) )
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
	
	def layoutToCells( l:Layout, x:Float, y:Float, w:Float, h:Float ):List[LayoutCell] = {
		var list = List[LayoutCell]()
		layoutToCells(l,x,y,w,h, (i => list = i :: list ) )
		list
	}
	
	def _gridify( entries:Seq[ImageEntry] ) = {
		val l = layout( partition( entries ) )
		layoutToCells( l, 0, 0, l.w, l.h )
	}
}
