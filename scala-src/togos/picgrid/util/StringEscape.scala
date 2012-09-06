package togos.picgrid.util

/**
 * Encodes/decodes strings using good old backslash-encoding
 * for certain characters. 
 */
object StringEscape
{
	def escape( str:String ):String = {
		assert( str != null )
		
		val rez = new StringBuilder()
		var i = 0
		while( i < str.length() ) {
			rez.append( str.charAt(i) match {
				case '\n' => "\\n"
				case '\r' => "\\r"
				case '\t' => "\\t"
				case '\'' => "\\'"
				case '\"' => "\\\""
				case '\\' => "\\\\"
				case x    => x
			})
			i += 1
		}
		rez.toString()
	}
	
	def unescape( str:String ):String = {
		if( !str.contains("\\") ) return str
		
		val rez = new StringBuilder()
		var i = 0
		while( i < str.length() ) {
			val c = str.charAt(i)
			rez.append(
				if( c == '\\' && i+1 < str.length() ) {
					i += 1
					str.charAt(i) match {
						case 'n'  => '\n'
						case 'r'  => '\r'
						case 't'  => '\t'
						case x   => x
					}
				} else {
					c
				}
			)
			i += 1
		}
		rez.toString()
	}
	
	def apply( str:String ) = escape(str)
	def unapply( str:String ) = Some(unescape(str))
}
