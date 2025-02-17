package lv.marmog.test.fileAnalyzer.service;

import lv.marmog.test.fileAnalyzer.exception.FileNotFoundInFolderException;
import lv.marmog.test.fileAnalyzer.exception.FolderProcessingException;
import lv.marmog.test.fileAnalyzer.exception.OdtProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import lv.marmog.test.fileAnalyzer.model.OdtFile;
import org.springframework.test.context.TestPropertySource;

import static lv.marmog.test.fileAnalyzer.service.utils.Utils.initLinkMap;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = "directory.path=/test/directory")
class OdtServiceImplTestUpdateImport {

	@InjectMocks
	private OdtServiceImpl odtService;

	private Map<String, String> linkMap;
	private Map<String, List<String>> map;

	@BeforeEach
	void setUp() throws Exception {
		linkMap = initLinkMap();
		map = new HashMap<>();

		// Inject the directory path into the OdtProcessor
		Field field = OdtServiceImpl.class.getDeclaredField("directoryPath");
		field.setAccessible(true);
		field.set(odtService, "src/test/resources/root-test-folder");
	}

	@Test
	void test_UpdateImport_Success() throws Exception {
		// Given

		String sourceFile = "template_aa02_editable.odt";
		String existingBlock = "block_1a.odt";
		String newBlock = "block_1.odt";

		OdtFile result1 = odtService.updateImport(sourceFile, existingBlock, newBlock);
		assertNotNull(result1);
		assertEquals(sourceFile, result1.getName());
		assertFalse(result1.getImportFiles().isEmpty());
		assertEquals(newBlock, result1.getImportFiles().get(0).getName());

		OdtFile result2 = odtService.updateImport(sourceFile, newBlock, existingBlock); //change back
		assertFalse(result2.getImportFiles().isEmpty());
		assertEquals(existingBlock, result2.getImportFiles().get(0).getName());
	}

	@Test
	void test_UpdateImport_FileNotFound() throws NoSuchFieldException, IllegalAccessException {
		// Given
		String sourceFile = "nonexistent.odt";
		String existingBlock = "block_1a.odt";
		String newBlock = "block_1.odt";
 		//When and then
		assertThrows(FileNotFoundInFolderException.class, () ->
				odtService.updateImport(sourceFile, existingBlock, newBlock));
	}

	@Test
	void test_ProcessOdfFile_Success() throws Exception {
		// Given
		File file = new File("src/test/resources/root-test-folder/template_aa01.odt");

		// When
		odtService.processOdfFile(file, linkMap, map);

		// Then
		assertTrue(map.containsKey(file.getName()));
		assertNotNull(map.get(file.getName()));
		assertFalse(map.get(file.getName()).isEmpty());
	}

	@Test
	void test_ProcessOdfFile_FileNotFound() {
		// Given
		String fileName = "nonexistent.odt";
		File file = new File("src/test/resources/root-test-folder/" + fileName);

		// When and Then
		assertThrows(OdtProcessingException.class, () -> {
			odtService.processOdfFile(file, linkMap, map);
		});
	}

	@Test
	void test_ProcessOdfFile_FolderProcessingException() throws Exception {
		// Given
		String fileName = "invalid_folder.odt"; // A file that triggers folder processing errors
		File file = new File("src/test/resources/root-test-folder/" + fileName);

		// When , Then
		OdtProcessingException exception = assertThrows(OdtProcessingException.class, () -> {
			odtService.processOdfFile(file, linkMap, map);
		});

		assertEquals("Error processing .odt file: " + file, exception.getMessage());
		assertTrue(exception.getCause() instanceof FolderProcessingException);
	}

}