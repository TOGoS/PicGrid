package togos.picgrid.layout

import togos.ccouch2.store.Store
import togos.picgrid.ImageEntry
import scala.collection.mutable.ListBuffer
import togos.picgrid.image.CompoundImageComponent

class RowlyLayouter extends Layouter
{
	def cacheString = "rowly-v1"
	
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
