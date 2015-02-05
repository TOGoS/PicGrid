package togos.picgrid.util;

public class HTMLUtil
{
    public static String htmlEscape( String text ) {
	return text.replace("&","&amp;").replace(">","&gt;").replace("<","&lt;");
    }
}
