package lv.marmog.test.fileAnalyzer.model;

import lombok.Data;
import lv.marmog.test.fileAnalyzer.controller.OdtController;

import java.util.List;

@Data
public class OdtFile {

    List<OdtFile> importFiles;
}
