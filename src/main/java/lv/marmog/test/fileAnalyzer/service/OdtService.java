package lv.marmog.test.fileAnalyzer.service;


import lv.marmog.test.fileAnalyzer.model.OdtFile;

import java.nio.file.Path;
import java.util.List;

public interface OdtService {

   List<OdtFile> getFileImports(Path folder);
}
