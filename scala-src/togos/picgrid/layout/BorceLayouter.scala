package togos.picgrid.layout

import togos.ccouch2.store.Store
import togos.picgrid.ImageEntry
import scala.collection.mutable.ListBuffer
import togos.picgrid.image.CompoundImageComponent

class BorceLayouter(val maxWidth:Int, val maxHeight:Int) extends Layouter
{
	def configString = "borce:%dx%d".format(maxWidth,maxHeight)
	
	class ImageInfo( val w:Int, val h:Int, val weight:Float, val urn:String, val name:String )
	class CellTree( val subTrees:Seq[CellTree], val image:ImageInfo ) {
		def this( image:ImageInfo ) = this( List.empty, image )
		def this( subTree:Seq[CellTree] ) = this(subTree, null)
	}
	
	def adjustWeight( w:Float ) = math.sqrt(w).toFloat
	
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
			nextWeight = prevWeight + adjustWeight(images(midWeightIndex).weight)
			if( target - prevWeight < nextWeight - target ) {
				return midWeightIndex
			}
			prevWeight = nextWeight
			midWeightIndex += 1
		}
		
		assert( midWeightIndex > 0, "Somehow mid-weight index is > 0; totalWeight = "+totalWeight+", weights = "+images.map(i => i.weight) )
		assert( midWeightIndex < images.length, "Somehow mid-weight index is at end; totalWeight = "+totalWeight+", weights = "+images.map(i => i.weight) )
		
		//if( midWeightIndex == 0 ) return 1;
		//if( midWeightIndex == images.length ) return images.length-1;
		
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
			totalWeight += adjustWeight(image.weight)
		}
		
		//val midWeightIndex = math.round( (images.length / 2f + findSplitPoint(images, totalWeight)) / 2f )
		val midWeightIndex = findSplitPoint(images, totalWeight)
		
		// System.out.println( weightSplitDiagram(for(i<-images) yield i.weight, midWeightIndex) )
		
		return new CellTree( List( partition(images.slice(0,midWeightIndex)), partition(images.slice(midWeightIndex,images.size)) ) )
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
	def bordify( c:CompoundImageComponent, s:Int ) =
		new CompoundImageComponent( c.x + s, c.y + s, c.width - s*2, c.height - s*2, c.uri, c.name )
	
	def gridify( entries:Seq[ImageEntry], maxWidth:Int, maxHeight:Int ):List[CompoundImageComponent] = {
		val l = layout( partition( imageEntriesToInfo(entries) ) )
		val (w,h) = fitInside( l.w, l.h, maxWidth, maxHeight )
		var cells = List[CompoundImageComponent]()
		val spacing = 4
		val halfSpacing = spacing/2
		
		/*
		layoutToCells( l, 0, 0, w, h,
			(c:CompoundImageComponent) => cells = c :: cells
		)
		*/
		layoutToCells( l, -halfSpacing, -halfSpacing, w+spacing, h+spacing,
			(c:CompoundImageComponent) => cells = bordify(c,halfSpacing) :: cells
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
