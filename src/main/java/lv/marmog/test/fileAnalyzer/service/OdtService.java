package lv.marmog.test.fileAnalyzer.service;

import lv.marmog.test.fileAnalyzer.exception.FolderProcessingException;
import lv.marmog.test.fileAnalyzer.exception.InvalidFolderException;
import lv.marmog.test.fileAnalyzer.exception.OdtProcessingException;
import lv.marmog.test.fileAnalyzer.model.OdtFile;

import java.io.File;
import java.util.List;

public interface OdtService {

	List<OdtFile> getFileImports(File dir)
			throws InvalidFolderException, OdtProcessingException, FolderProcessingException;

	OdtFile updateImport(String sourceFile, String existingLink, String newLink)
			throws Exception; //TODO take care of exception
}
