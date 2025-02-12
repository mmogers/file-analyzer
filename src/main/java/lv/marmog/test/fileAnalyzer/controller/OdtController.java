package lv.marmog.test.fileAnalyzer.controller;

import lombok.extern.slf4j.Slf4j;
import lv.marmog.test.fileAnalyzer.model.OdtFile;
import lv.marmog.test.fileAnalyzer.service.OdtServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/odt")
@Slf4j
public class OdtController {

	@Value("${directory.path}")
	String directoryPath;

	private final OdtServiceImpl odtService;

	OdtController(OdtServiceImpl odtService){
		this.odtService = odtService;
	}
	/**
	 * lists all import files (name and link) for each such document directoryPath = path to root-folder, set in
	 * application.yml
	 *
	 * @return JSON of all .odt documents which have import files
	 */
	@GetMapping(value = "/imports")
	public ResponseEntity<List<OdtFile>> getFileImports() {
		return ResponseEntity.ok(odtService.getFileImports(new File(directoryPath)));
	}
	//TODO change to @QueryParams
	@PutMapping("/imports")
	public ResponseEntity<OdtFile> updateLink(String sourceFile, String existingLink, String newLink) throws Exception {
		return ResponseEntity.ok(odtService.updateImport("template_bb01.odt", "block_2.odt","block_1a.odt"));
	}

}
