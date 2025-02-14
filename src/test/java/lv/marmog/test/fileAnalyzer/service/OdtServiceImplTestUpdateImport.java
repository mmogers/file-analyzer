package lv.marmog.test.fileAnalyzer.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static lv.marmog.test.fileAnalyzer.service.utils.Utils.initLinkMap;
import lv.marmog.test.fileAnalyzer.exception.FolderProcessingException;
import lv.marmog.test.fileAnalyzer.exception.InvalidFolderException;
import lv.marmog.test.fileAnalyzer.model.OdtFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;
@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = "directory.path=/test/directory")
class OdtServiceImplTestUpdateImport {

	@InjectMocks
	private OdtServiceImpl odtService;
	@Value("${directory.path}")
	private String directoryPath;

	@Test
	void test_UpdateImport_Success() throws Exception {
		// Given

		String sourceFile = "template_aa02_editable.odt";
		String existingBlock = "block_1a.odt";
		String newBlock = "block_1.odt";

		Field field = OdtServiceImpl.class.getDeclaredField("directoryPath");
		field.setAccessible(true);
		field.set(odtService, "src/test/resources/root-test-folder");  // Manually inject the value

		assertEquals("src/test/resources/root-test-folder", field.get(odtService));

		OdtFile result1 = odtService.updateImport(sourceFile, existingBlock, newBlock);
		assertNotNull(result1);
		assertEquals(sourceFile, result1.getName());
		assertFalse(result1.getImportFiles().isEmpty());
		assertEquals(newBlock, result1.getImportFiles().get(0).getName());

		OdtFile result2 = odtService.updateImport(sourceFile, newBlock, existingBlock); //change back
		assertFalse(result2.getImportFiles().isEmpty());
		assertEquals(existingBlock, result2.getImportFiles().get(0).getName());
	}

	/*@Test
	void test_UpdateImport_FileNotFound() {
		// Given
		String sourceFile = "nonexistent.odt";
		String existingBlock = "oldBlock";
		String newBlock = "newBlock";

		when(linkMap.containsKey(sourceFile)).thenReturn(false);

		assertThrows(FileNotFoundInFolderException.class, () ->
				odtService.updateImport(sourceFile, existingBlock, newBlock));
	}*/

	/*@Test
	void test_UpdateImport_UnzipFailure() throws Exception {
		// Given
		String sourceFile = "corrupt.odt";
		String existingBlock = "oldBlock";
		String newBlock = "newBlock";

		when(linkMap.containsKey(sourceFile)).thenReturn(true);
		when(linkMap.containsKey(existingBlock)).thenReturn(true);
		when(linkMap.containsKey(newBlock)).thenReturn(true);
		when(linkMap.get(sourceFile)).thenReturn(tempDir.getAbsolutePath());

		doThrow(new OdtProcessingException("Failed to unzip"))
				.when(odtService).unzipFile(eq(sourceFile), anyMap());

		assertThrows(OdtProcessingException.class, () ->
				odtService.updateImport(sourceFile, existingBlock, newBlock));
	}*/

	/*@Test
	void test_UpdateImport_ZipFailure() throws Exception {
		// Given
		String sourceFile = "test.odt";
		String existingBlock = "oldBlock";
		String newBlock = "newBlock";

		when(linkMap.containsKey(sourceFile)).thenReturn(true);
		when(linkMap.containsKey(existingBlock)).thenReturn(true);
		when(linkMap.containsKey(newBlock)).thenReturn(true);
		when(linkMap.get(sourceFile)).thenReturn(tempDir.getAbsolutePath());

		doThrow(new OdtProcessingException("Failed to zip"))
				.when(odtService).zipFile(eq(sourceFile), any(File.class), anyMap());

		assertThrows(OdtProcessingException.class, () ->
				odtService.updateImport(sourceFile, existingBlock, newBlock));
	}*/
}