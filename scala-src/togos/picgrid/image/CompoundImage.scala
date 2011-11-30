package togos.picgrid.image

import togos.mf.value.ByteBlob
import togos.mf.base.SimpleByteChunk
import java.io.BufferedReader
import java.io.InputStreamReader
import togos.picgrid.io.ByteBlobInputStream
import togos.picgrid.SimpleByteBlob
import scala.collection.mutable.LinkedList
import java.util.ArrayList
import scala.collection.mutable.ListBuffer

class CompoundImageComponent( val x:Integer, val y:Integer, val w:Integer, val h:Integer,
	val uri:String )
{
	override def toString():String = {
		x+","+y+","+w+","+h+" "+uri
	}
}

class CompoundImage( val width:Integer, val height:Integer,
	val components:Seq[CompoundImageComponent], val promotedImageUri:String )
{
	def serialize():ByteBlob = {
		val sb = new StringBuilder()
		sb.append("COMPOUND-IMAGE "+width+","+height+"\n")
		if( promotedImageUri != null ) {
			sb.append("PROMOTE "+promotedImageUri)
		}
		for( c <- components ) {
			sb.append("COMPONENT "+c+"\n")
		}
		
		new SimpleByteBlob(new SimpleByteChunk(sb.toString().getBytes()))
	}
}
object CompoundImage
{
	val CI_LINE        = """^COMPOUND-IMAGE (\d+),(\d+)$""".r
	val PROMOTE_LINE   = """^PROMOTE (\S+)$""".r
	val COMPONENT_LINE = """^COMPONENT (\d+),(\d+),(\d+),(\d+) (\S+)$""".r
	
	def unserialize( b:ByteBlob ):CompoundImage = {
		val br = new BufferedReader( new InputStreamReader( new ByteBlobInputStream(b.chunkIterator()) ) )
		var line = br.readLine()
		var width,height = 0
		var components = new ListBuffer[CompoundImageComponent]()
		var promotedImageUri:String = null
		while( line != null ) {
			line match {
				case CI_LINE(w,h) =>
					width = w.toInt
					height = h.toInt
				case PROMOTE_LINE(uri) =>
					promotedImageUri = uri
				case COMPONENT_LINE(x,y,w,h,uri) =>
					components += new CompoundImageComponent(x.toInt,y.toInt,w.toInt,h.toInt,uri)
			}
			line = br.readLine()
		}
		new CompoundImage( width, height, components.toList, promotedImageUri )
	}
}
