package cfml.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Version {
	/**
	 * Retrieves the version information from the pom.properties file.
	 *
	 * @return The version string, or an empty string if the version cannot be determined.
	 */
	public static String getVersion() {
		final InputStream is = Version.class
				.getResourceAsStream("/META-INF/maven/com.github.cfmleditor/cfml.parsing/pom.properties");
		try {
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line = reader.readLine();
			while (line != null && !line.startsWith("version=")) {
				line = reader.readLine();
			}
			if (line != null) {
				return line.replaceAll("version=", "");
			}
		} catch (final Exception e) {
			try {
				if (is != null) {
					is.close();
				}
			} catch (final IOException e1) {
				e1.printStackTrace();
			}
		}
		return "";
	}
}
