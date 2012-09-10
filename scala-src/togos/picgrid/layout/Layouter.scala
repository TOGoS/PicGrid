package togos.picgrid.layout

import togos.picgrid.ImageEntry
import togos.picgrid.image.CompoundImageComponent

trait Layouter
{
	/**
	 * Return a string that can be used to identify this instance for caching its results.
	 * Should include the class, a version, and any configuration.
	 * e.g. bob-v5-640x480  
	 */
	def cacheString:String
	def gridify( images:Seq[ImageEntry] ):Seq[CompoundImageComponent]
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
			case "rowly" => new RowlyLayouter()
			case "borce" => new BorceLayouter(w,h)
			case _ => throw new Exception("Unrecognised layouter: '"+parts(0)+"'")
		}
	}
}
