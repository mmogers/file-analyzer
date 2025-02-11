package lv.marmog.test.fileAnalyzer.service;


import lv.marmog.test.fileAnalyzer.model.OdtFile;

import java.io.File;
import java.util.List;

public interface OdtService {

   List<OdtFile> getFileImports(File dir);
}
