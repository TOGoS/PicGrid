package togos.picgrid.layout

import togos.ccouch2.store.Store
import togos.picgrid.ImageEntry
import scala.collection.mutable.ListBuffer
import togos.picgrid.image.CompoundImageComponent

class RowlyLayouter( w:Int, h:Int ) extends AutoSpacingLayouter(w,h)
{
	def cacheString = "rowly-v2"
	
	def gridifyRows( images:Seq[ImageEntry], imagesPerRow:Int ):List[LayoutCell] = {
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
		
		val cells = ListBuffer[LayoutCell]()
		var totalWidth = 1024
		var totalImageCount = 0
		var y = 0f
		for( row <- rows ) {
			var imageSpaceAvailable = totalWidth
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
				cells += new LayoutCell( e, x, y, cellWidth, rowHeight )
				x += cellWidth
				totalImageCount += i.totalImageCount
			}
			y += rowHeight
		}
		
		cells.toList
	}
	
	def gridifyColumns( images:Seq[ImageEntry], imagesPerColumn:Int ):List[LayoutCell] = {
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
		
		val cells = ListBuffer[LayoutCell]()
		var totalHeight = 1024
		var totalImageCount = 0
		var x = 0f
		for( column <- columns ) {
			var imageSpaceAvailable = totalHeight - (column.length - 1)
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
				cells += new LayoutCell( e, x, y, rowWidth, cellHeight )
				y += cellHeight
				totalImageCount += i.totalImageCount
			}
			x += rowWidth
		}
		
		cells.toList
	}
	
	def aspectRatio( cells:Seq[LayoutCell] ):Float = {
		var width, height = 0f
		for( c <- cells ) {
			if( c.x + c.w  > width  ) width  = c.x + c.w
			if( c.y + c.h > height  ) height = c.y + c.h
		}
		width / height
	}
	
	val aspectRatioPower = 2
	val aspectRatioWeight = 2
	val componentAreaRatioPower = 2
	val componentAreaRatioWeight = 1
	
	def aspectRatioFitness( cells:Seq[LayoutCell] ):Double = {
		val ar = aspectRatio( cells )
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
	
	def componentAreaRatioFitness( cells:Seq[LayoutCell] ):Double = {
		var smallestArea = Float.MaxValue
		var largestArea = Float.MinValue
		for( c <- cells ) {
			val area = c.w * c.h
			if( area < smallestArea ) smallestArea = area
			if( area > largestArea ) largestArea = area
		}
		- math.pow( largestArea.toDouble / smallestArea, componentAreaRatioPower ) * componentAreaRatioWeight
	}
	
	def fitness( cells:Seq[LayoutCell] ):Double = {
		aspectRatioFitness( cells ) + componentAreaRatioFitness( cells )
	}
	
	def _gridify( images:Seq[ImageEntry] ):List[LayoutCell] = {
		var bestFitness = Double.NegativeInfinity
		var bestResult:List[LayoutCell] = null
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
