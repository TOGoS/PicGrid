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
	val components:Seq[CompoundImageComponent],
	val promotedImage1Uri:String, val promotedImage2Uri:String )
{
	def aspectRatio = width.toFloat / height
	
	def serialize():ByteBlob = {
		val sb = new StringBuilder()
		sb.append("COMPOUND-IMAGE "+width+","+height+"\n")
		if( promotedImage1Uri != null ) {
			sb.append("PROMOTE1 "+promotedImage1Uri+"\n")
		}
		if( promotedImage2Uri != null ) {
			sb.append("PROMOTE2 "+promotedImage2Uri+"\n")
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
	val PROMOTE1_LINE   = """^PROMOTE1 (\S+)$""".r
	val PROMOTE2_LINE   = """^PROMOTE2 (\S+)$""".r
	val COMPONENT_LINE = """^COMPONENT (\d+),(\d+),(\d+),(\d+) (\S+)$""".r
	
	def unserialize( b:ByteBlob ):CompoundImage = {
		val br = new BufferedReader( new InputStreamReader( new ByteBlobInputStream(b.chunkIterator()) ) )
		var line = br.readLine()
		var width,height = 0
		var components = new ListBuffer[CompoundImageComponent]()
		var promotedImage1Uri, promotedImage2Uri : String = null
		while( line != null ) {
			line match {
				case CI_LINE(w,h) =>
					width = w.toInt
					height = h.toInt
				case PROMOTE1_LINE(uri) =>
					promotedImage1Uri = uri
				case PROMOTE2_LINE(uri) =>
					promotedImage2Uri = uri
				case COMPONENT_LINE(x,y,w,h,uri) =>
					components += new CompoundImageComponent(x.toInt,y.toInt,w.toInt,h.toInt,uri)
			}
			line = br.readLine()
		}
		new CompoundImage( width, height, components.toList, promotedImage1Uri, promotedImage2Uri )
	}
	
	implicit def compoundImageAsBlob( i:CompoundImage ):ByteBlob = i.serialize()
	implicit def blobAsCompoundImage( b:ByteBlob ):CompoundImage = unserialize(b)
}
