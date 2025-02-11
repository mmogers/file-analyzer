package lv.marmog.test.fileAnalyzer.constants;

import java.util.regex.Pattern;

public class Constants {
	public static final Pattern IMPORT_PATTERN = Pattern.compile("\\[import\\s+([^\\]]+\\.odt)\\]");

}