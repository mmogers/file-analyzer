package lv.marmog.test.fileAnalyzer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class OdtFile {

	@JsonProperty("name")
	private String name;

	@JsonProperty("link")
	private String link;

	@JsonProperty("importFiles")
	private List<OdtFile> importFiles;

	public String getName() {
		return name;
	}
	public List<OdtFile> getImportFiles() {
		return importFiles;
	}

	public OdtFile(String name, String link) {
		this.name = name;
		this.link = link;
	}

	public void setImportFiles(List<OdtFile> importFiles) {
		this.importFiles = importFiles;
	}
}
