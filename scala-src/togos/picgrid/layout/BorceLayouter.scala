package togos.picgrid.layout

import togos.ccouch2.store.Store
import togos.picgrid.ImageEntry
import scala.collection.mutable.ListBuffer
import togos.picgrid.image.CompoundImageComponent

class BorceLayouter extends Layouter
{
	class ImageInfo( val w:Int, val h:Int, val weight:Float, val urn:String, val name:String )
	class CellTree( val subTrees:Seq[CellTree], val image:ImageInfo ) {
		def this( image:ImageInfo ) = this( List.empty, image )
		def this( subTree:Seq[CellTree] ) = this(subTree, null)
	}
	
	def adjustWeight( w:Float ) = w * w
	
	def findSplitPoint( images:Seq[ImageInfo], totalWeight:Float ):Int = {
		var midWeightIndex = 0
		var prevWeight, nextWeight = 0f
		while( midWeightIndex < images.size ) {
			nextWeight = prevWeight + adjustWeight(images(midWeightIndex).weight)
			if( totalWeight / 2 - prevWeight < nextWeight - totalWeight / 2 ) {
				return midWeightIndex
			}
			midWeightIndex += 1
		}
		midWeightIndex
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
		
		val midWeightIndex = findSplitPoint(images, totalWeight)
		
		return new CellTree( List( partition(images.slice(0,midWeightIndex)), partition(images.slice(midWeightIndex,images.size)) ) )
	}
	
	class LayoutComponent( val x:Int,val y:Int,val w:Int, val h:Int, l:Layout )
	class Layout( val w:Int, val h:Int, val components:Seq[LayoutComponent], val image:ImageInfo ) {
		def this( w:Int, h:Int, components:Seq[LayoutComponent] ) = this(w,h,components,null)
		def this( w:Int, h:Int, image:ImageInfo ) = this(w,h,List.empty,image)
	}
	
	def swap( x:Int, y:Int, really:Boolean ):(Int,Int) = if( really ) (x,y) else (y,x)
	
	def _layout( subLayouts:Seq[Layout], maxWidth:Int, maxHeight:Int, vertical:Boolean ):Layout = {
		val (maxA, maxB) = swap(maxWidth, maxHeight, vertical)
		var a, b = 0
		
		var components = List[LayoutComponent]()
		
		for( subLayout <- subLayouts ) {
			val (slSA,slSB) = swap(subLayout.w, subLayout.h, vertical)
			val scaledSA = (slSA * (maxB.toFloat/slSB)).toInt
			val (slX,slY) = swap(a, b, vertical)
			val (slW,slH) = swap(slSA,slSB, vertical)
			components = new LayoutComponent( slX, slY, slW, slH, subLayout ) :: components
		}
		
		val (w,h) = swap(a,maxB,vertical)
		return new Layout( w, h, components )
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
		
		val h = _layout( subLayouts, maxWidth, maxHeight, false )
		val v = _layout( subLayouts, maxWidth, maxHeight, true  )
		if( ratio(h.w,h.h) < ratio(v.w,v.h) ) h else v
	}
	
	def roundBounds( x:Float, y:Float, w:Float, h:Float ):(Int,Int,Int,Int) = {
		val endX = math.round(x+w); val endY = math.round(y+h)
		val beginX = math.round(x); val beginY = math.round(y);
		return (beginX,beginY,endX-beginX,endY-beginY)
	}
	
	def layoutToCells( l:Layout, x:Float, y:Float, w:Float, h:Float ):Seq[CompoundImageComponent] = {
		if( l.image.urn != null ) {
			val (cx,cy,cw,ch) = roundBounds(x,y,w,h)
			return List(new CompoundImageComponent(cx, cy, cw, ch, l.image.urn, l.image.name))
		}
		null
	}
	
	
	
	
	
	
	//// Blah old stuffs
	
	def configString = "borce-default"
	
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
