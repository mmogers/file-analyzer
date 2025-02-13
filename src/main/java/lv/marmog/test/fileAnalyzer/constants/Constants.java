package lv.marmog.test.fileAnalyzer.constants;

import java.util.regex.Pattern;

public class Constants {
	public static final Pattern IMPORT_PATTERN = Pattern.compile("\\[import\\s+([^\\]]+\\.odt)\\]");
	public static final String CONTENT_XML = "content.xml";
	public static final String STYLES_XML = "styles.xml";
}