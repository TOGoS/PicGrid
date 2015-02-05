package togos.blob;

import java.io.IOException;
import java.io.InputStream;

public interface InputStreamable
{
	/**
	 * Return a new InputStream
	 */
	public InputStream inputStream() throws IOException;
}
