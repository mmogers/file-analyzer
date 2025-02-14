package lv.marmog.test.fileAnalyzer.service.utils;

import lv.marmog.test.fileAnalyzer.service.OdtServiceImpl;

import java.util.HashMap;
import java.util.Map;

public class Utils {

	private OdtServiceImpl odtService;

	public Utils(OdtServiceImpl odtService) {
		this.odtService = odtService;
	}

	public static Map<String, String> initLinkMap() {
		Map<String, String> linkMap = new HashMap<>();
		linkMap.put("footer_1.odt", "src/test/resources/root-test-folder/common/footer_1.odt");
		linkMap.put("footer_2.odt", "src/test/resources/root-test-folder/common/footer_2.odt");
		linkMap.put("header_1.odt", "src/test/resources/root-test-folder/common/header_1.odt");
		linkMap.put("header_2.odt", "src/test/resources/root-test-folder/common/header_2.odt");
		linkMap.put("block_1.odt", "src/test/resources/root-test-folder/block_1.odt");
		linkMap.put("block_1a.odt", "src/test/resources/root-test-folder/block_1a.odt");
		linkMap.put("block_2.odt", "src/test/resources/root-test-folder/block_2.odt");
		linkMap.put("template_aa01.odt", "src/test/resources/root-test-folder/template_aa01.odt");
		linkMap.put("template_aa02_editable.odt", "src/test/resources/root-test-folder/template_aa02_editable.odt");
		return linkMap;
	}
}
