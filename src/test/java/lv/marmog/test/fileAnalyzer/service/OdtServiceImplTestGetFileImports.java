package lv.marmog.test.fileAnalyzer.service;

import lv.marmog.test.fileAnalyzer.exception.InvalidFolderException;
import lv.marmog.test.fileAnalyzer.model.OdtFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static lv.marmog.test.fileAnalyzer.service.utils.Utils.initLinkMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OdtServiceImplTestGetFileImports {

	private OdtServiceImpl odtService;

	@BeforeEach
	void setUp() {
		odtService = new OdtServiceImpl(); // No dependencies, so we instantiate directly
	}

	@Test
	void test_GetFileImports_ValidFolder() throws Exception {
		// Given: valid folder with a sample ODT file
		File validFolder = new File("src/test/resources/root-test-folder");

		// When
		List<OdtFile> result = odtService.getFileImports(validFolder);

		// Then
		assertNotNull(result);
		assertFalse(result.isEmpty());
	}

	@Test
	void test_GetFileImports_EmptyFolder() throws Exception {
		// Given: a valid folder with a sample ODT file
		File emptyFolder = new File("src/test/resources/empty-folder");

		// When
		List<OdtFile> result = odtService.getFileImports(emptyFolder);

		// Then
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
		//TODO can be a parametrized test?
	void test_GetFileImports_InvalidFolder_ShouldThrowException() {
		// Given: non-existent folder
		File invalidFolderName = new File("src/test/resources/empty-folder/non_existent_folder");
		File invalidFolderNull = null;
		File invalidFolderNotFolder = new File("src/test/resources/folder-with-other-files/txt_file.txt");

		// When & Then
		InvalidFolderException exception1 = assertThrows(InvalidFolderException.class,
				() -> odtService.getFileImports(invalidFolderName));
		InvalidFolderException exception2 = assertThrows(InvalidFolderException.class,
				() -> odtService.getFileImports(invalidFolderNull));
		InvalidFolderException exception3 = assertThrows(InvalidFolderException.class,
				() -> odtService.getFileImports(invalidFolderNotFolder));

		assertTrue(exception1.getMessage().contains("Invalid folder"));
		assertTrue(exception2.getMessage().contains("Invalid folder"));
		assertTrue(exception3.getMessage().contains("Invalid folder"));
	}

	@Test
	void test_getLinkMap_ValidFolder_ShouldReturnOdtFiles() throws Exception {
		// Given: valid folder with ODT files
		File folder = new File("src/test/resources/root-test-folder");

		// When:
		Map<String, String> result = OdtServiceImpl.getLinkMap(folder);

		// Then: all ODT files should be found
		assertEquals(9, result.size()); //2 headers, 2 footers, 2 templates ; 3 blocks
		assertTrue(result.containsKey("template_aa01.odt"));
		assertTrue(result.containsKey("template_aa02_editable.odt"));
	}

	@Test
	void test_getLinkMap_EmptyFolder_ShouldReturnEmptyMap() throws Exception {
		// Given: empty folder
		File emptyFolder = new File("src/test/resources/empty-folder");

		// When: Calling getLinkMap()
		Map<String, String> result = OdtServiceImpl.getLinkMap(emptyFolder);

		// Then: empty map
		assertEquals(0, result.size());
		assertTrue(result.isEmpty());
	}

	@Test
	void test_GetLinkMap_NullFolder_ShouldThrowException() {
		File invalidFolderName = new File("src/test/resources/empty-folder/non_existent_folder");
		File invalidFolderNotFolder = new File("src/test/resources/folder-with-other-files/txt_file.txt");
		// When & Then
		assertThrows(InvalidFolderException.class, () -> OdtServiceImpl.getLinkMap(invalidFolderName));
		assertThrows(InvalidFolderException.class, () -> OdtServiceImpl.getLinkMap(invalidFolderNotFolder));
	}

	@Test
	void test_getLinkMap_ValidFolder_ShouldReturnOdtFiles_AndNotReturnCorruptedOrOtherFiles() throws Exception {
		// Given: valid folder with ODT files, folder, corrupted file and txt file
		File folder = new File("src/test/resources/root-test-folder");

		// When:
		Map<String, String> result = OdtServiceImpl.getLinkMap(folder);

		// Then: all ODT files should be found and other ignored
		assertEquals(9, result.size()); //2 headers, 2 footers, 2 templates.3 blocks
		assertNotEquals(6, folder.listFiles().length); //5 odt files, 1 folder and some non odt or corrupted files
		assertTrue(result.containsKey("template_aa01.odt"));
		assertTrue(result.containsKey("template_aa02_editable.odt"));
	}

	@Test
	void test_GetLinkMap_RecursiveTraversal_ShouldFindNestedFolder() throws Exception {
		// Given: A folder with subfolders and ODT files in it
		File folder = new File("src/test/resources/root-test-folder");
//		File subFolder = new File("src/test/resources/root-test-folder/common");

		// When:
		Map<String, String> result = OdtServiceImpl.getLinkMap(folder);

		// Then: The nested ODT file should be included
		assertTrue(result.containsKey("footer_1.odt"));
	}

	@Test
	void testGetLinkMap_FileProcessingException_ShouldLogError() throws Exception {
		// Given: a folder with a file that causes an exception
		File folder = new File("src/test/resources/root-test-folder");
		File errorFile = spy(new File(folder, "~$mplate_bb01.odt"));
		doThrow(new RuntimeException("Simulated Error")).when(errorFile).getName();

		// Spy on tempFolder to return our files
		File tempSpy = spy(folder);
		doReturn(new File[] { errorFile, new File(folder, "template_aa01.odt") }).when(tempSpy).listFiles();

		// When:
		Map<String, String> result = OdtServiceImpl.getLinkMap(tempSpy);

		// Then:
		assertTrue(result.containsKey("template_aa01.odt"));
		assertFalse(result.containsKey("~$mplate_bb01.odt")); // Should be ignored
	}

	@Test
	void test_ScanDirectory_EmptyDirectory() throws Exception {
		// Given: An empty directory
		File folder = mock(File.class);
		when(folder.exists()).thenReturn(true);
		when(folder.isDirectory()).thenReturn(true);
		when(folder.listFiles()).thenReturn(new File[0]);

		// When:call scanDirectory with an empty directory
		Map<String, List<String>> result = scanDirectoryTest(folder, new HashMap<>());

		// Then: The result map should be empty
		assertTrue(result.isEmpty());
	}

	@Test
	void test_ScanDirectory_ValidFiles() throws Exception {
		// Given:
		File folder = new File("src/test/resources/root-test-folder");
		Map<String, String> linkMap = initLinkMap();

		// When: Call scanDirectory
		Map<String, List<String>> result = OdtServiceImpl.scanDirectory(folder, linkMap);  // Adjust as needed

		// Then:
		assertNotNull(result, "Result map should not be null");
		assertEquals(2, result.size()); //2 files with inputs
		assertTrue(result.containsKey("template_aa01.odt"));
		assertTrue(result.containsKey("template_aa02_editable.odt"));
	}

	@Test
	void test_FindImportsInOdt_ReturnsImportList() throws Exception {
		File file = new File("src/test/resources/root-test-folder", "template_aa01.odt"); //contains 4 imports
		Map<String, String> linkMap = initLinkMap();
		List<String> imports;

		imports = odtService.findImportsInOdt(file, linkMap);

		assertTrue(imports.contains("block_1.odt"));
		assertEquals(4, imports.size());
	}

	@Test
	void test_GetImportFromFile_XmlDoesVotExist() throws Exception {

		File file = new File("src/test/resources/root-test-folder", "template_aa01.odt");
//		File tempDir = new File("src/test/resources/template_aa01_test");
		String fileType = "fakeName.xml";

//		File xmlFile = new File(tempDir, fileType);

		List<String> importList;
		importList = odtService.getImportFromFile(file, fileType, initLinkMap());

		assertEquals(0, importList.size());

	}

	// Helper method to test the scanDirectory method directly
	public Map<String, List<String>> scanDirectoryTest(File folder, Map<String, String> linkMap) throws Exception {

		return odtService.scanDirectory(folder, linkMap);
	}
}