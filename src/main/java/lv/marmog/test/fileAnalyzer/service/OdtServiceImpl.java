package lv.marmog.test.fileAnalyzer.service;

import lv.marmog.test.fileAnalyzer.model.OdtFile;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class OdtServiceImpl implements OdtService{

    @Override
    public List<OdtFile> getFileImports(Path folder) {
        return null;
    }
}
