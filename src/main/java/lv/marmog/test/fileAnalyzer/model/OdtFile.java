package lv.marmog.test.fileAnalyzer.model;

import lombok.Data;

import java.util.List;

@Data
public class OdtFile {

    List<OdtFile> importFiles;
}
