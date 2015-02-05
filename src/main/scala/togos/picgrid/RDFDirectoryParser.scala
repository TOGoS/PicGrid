package togos.picgrid
import scala.collection.mutable.ListBuffer

object RDFDirectoryParser
{
	val entryRegex = """(?s)<DirectoryEntry>(.*?)</DirectoryEntry>""".r
	val nameRegex = """(?s).*<name>([^<]*)</name>.*""".r
	val targetRegex = """(?s).*<(\w+) rdf:about="([^"]+)".*""".r
	
	def parseDirectory( rdf:String ):List[DirectoryEntry] = {
		val lb = new ListBuffer[DirectoryEntry]()
		for( m <- entryRegex.findAllIn(rdf) ) {
			val nameRegex(name) = m
			val targetRegex(targetTypeName,targetUri) = m
			val targetClass = DirectoryObjectClass.byName(targetTypeName)
			lb += new DirectoryEntry( name, targetUri, targetClass )
		}
		lb.toList
	}
}
