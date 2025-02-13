package lv.marmog.test.fileAnalyzer.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import lv.marmog.test.fileAnalyzer.model.OdtFile;
import lv.marmog.test.fileAnalyzer.service.OdtServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/odt")
@Slf4j
@Tag(name = "ODT file Imports", description = "Endpoints for managing ODT file imports")
public class OdtController {

	@Value("${directory.path}")
	String directoryPath;

	private final OdtServiceImpl odtService;

	OdtController(OdtServiceImpl odtService) {
		this.odtService = odtService;
	}

	@Operation(summary = "Get ODT file imports", description = "Retrieves a list of ODT files imports from a specified directory and its subdirectories")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully retrieved list of odt files with their imports",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "500", description = "Internal server error",
					content = @Content)
	})
	@GetMapping(value = "/imports")
	public ResponseEntity<List<OdtFile>> getFileImports() {
		return ResponseEntity.ok(odtService.getFileImports(new File(directoryPath)));
	}

	@Operation(
			summary = "Update an import link in a given source ODT file",
			description = "Updates a specified import block reference inside an ODT file."
	)
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully updated the import link",
					content = @Content(mediaType = "application/json")),
			@ApiResponse(responseCode = "400", description = "Invalid input data",
					content = @Content),
			@ApiResponse(responseCode = "500", description = "Internal server error",
					content = @Content)
	})
	@PutMapping("/imports")
	public ResponseEntity<OdtFile> updateLink(
			@Parameter(description = "File name", example = "template_bb01.odt")
			@RequestParam(required = true) String sourceFile,
			@Parameter(description = "Import block name to replace", example = "block_2.odt")
			@RequestParam(required = true) String existingLink,
			@Parameter(description = "New import block name", example = "block_1a.odt")
			@RequestParam(required = true) String newLink)
			throws Exception {
		return ResponseEntity.ok(odtService.updateImport(sourceFile, existingLink, newLink));
	}

}
