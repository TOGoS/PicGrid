package togos.picgrid

class DirectoryObjectClass( val name:String )

object DirectoryObjectClass
{
	val Blob      = new DirectoryObjectClass("Blob")
	val Directory = new DirectoryObjectClass("Directory")
	
	def byName( name:String ) = name match {
		case "Blob" => Blob
		case "Directory" => Directory
	}
}
