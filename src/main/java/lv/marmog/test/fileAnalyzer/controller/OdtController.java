package lv.marmog.test.fileAnalyzer.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lv.marmog.test.fileAnalyzer.log.InjectLog;
import lv.marmog.test.fileAnalyzer.model.OdtFile;
import lv.marmog.test.fileAnalyzer.service.OdtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/odt")
@Slf4j
public class OdtController {


    private final OdtService odtService;

    @GetMapping(value = "/imports")
    public ResponseEntity<List<OdtFile>> getFileImports(@RequestParam Path folder) {
        return ResponseEntity.ok(odtService.getFileImports(folder));
    }
}
