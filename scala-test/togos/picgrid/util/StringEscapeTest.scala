package togos.picgrid.util

import junit.framework.Assert.assertEquals
import junit.framework.TestCase

class StringEscapeTest extends TestCase
{
	def testEscape() {
		assertEquals( "Hello\\'\\\"\\r\\n\\t\\\\Goodbye", StringEscape("Hello'\"\r\n\t\\Goodbye") )
	}

	def testUnescape() {
		assertEquals( "Hello'\"\r\n\t\\Goodbye", StringEscape.unescape("Hello\\'\"\\r\\n\\t\\\\Goodbye") )
	}
	
	val quotedStringRegex = """^'((?:[^'\\]|\\.)*)'$""".r
	
	def testRegex() {
		val quotedStringRegex(foo) = "'foo'";
		assertEquals( "foo", foo )

		val quotedStringRegex(bar) = "'ba\\'r'";
		assertEquals( "ba\\'r", bar )
		
		val quotedStringRegex(StringEscape(barn)) = """'ba\r\n\'\"'""";
		assertEquals( "ba\r\n'\"", barn )
	}
}
