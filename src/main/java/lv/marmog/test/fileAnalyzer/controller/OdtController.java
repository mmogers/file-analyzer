package lv.marmog.test.fileAnalyzer.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.marmog.test.fileAnalyzer.model.OdtFile;
import lv.marmog.test.fileAnalyzer.service.OdtServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/odt")
@Slf4j
public class OdtController {

    @Value("${directory.path}")
    String directoryPath;

    @Autowired
    OdtServiceImpl odtService;

    /**
     * returns JSON of all .odt documents which have import files,
     * lists all import files (name and link) for each such document
     * directoryPath = path to root-folder, set in application.yml
     * @return
     */
    @GetMapping(value = "/imports")
    public ResponseEntity<List<OdtFile>> getFileImports() {
        return ResponseEntity.ok(odtService.getFileImports(new File(directoryPath)));
    }
    //add PUT for updating the file
}
