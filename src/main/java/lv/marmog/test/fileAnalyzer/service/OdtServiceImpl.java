//TODO add more logs
//TODO add junit tests
//TODO place all the methods one after another
//TODO extract a method for creating linkMap
//TODO check SOLID
package lv.marmog.test.fileAnalyzer.service;

import lv.marmog.test.fileAnalyzer.model.OdtFile;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static lv.marmog.test.fileAnalyzer.constants.Constants.IMPORT_PATTERN;
import static org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory;

@Service
public class OdtServiceImpl implements OdtService {

	private static final Logger log = LoggerFactory.getLogger(OdtServiceImpl.class);
	private static final Map<String, String> linkMap = new HashMap<>();

	@Value("${directory.path}")
	String directoryPath;

	@Override
	public List<OdtFile> getFileImports(File folder) {

		Map<String, List<String>> importMap;
		List<OdtFile> odtFiles = new ArrayList<>();

		importMap = scanDirectory(folder);

		importMap.forEach((file, imports) -> {
			OdtFile newFile = new OdtFile(file, linkMap.get(file));
			List<OdtFile> newImportFiles = new LinkedList<>();
			imports.forEach(importStr -> {
				newImportFiles.add(new OdtFile(importStr, linkMap.get(importStr)));
			});
			newFile.setImportFiles(newImportFiles);
			odtFiles.add(newFile);
		});

		return odtFiles;
	}

	@Override
	public OdtFile updateImport(String odtFilePath, String oldFileName , String newFileName ) throws Exception {

		getFileImports(new File(directoryPath));
		// Unzip the .odt file (a ZIP archive)
		File tempDir = unzipOdt(linkMap.get(odtFilePath));

		// update the XML files (content.xml and/or styles.xml)
		updateImportInContentXml(tempDir, oldFileName, newFileName);
		//TODO update import in styles as well

		// zip .odt file back
		zipOdt(tempDir, linkMap.get(odtFilePath));

		//  clean up
		deleteDirectory(tempDir); //TODO check this method  org.apache.tomcat.util.http.fileupload.FileUtils

		return null; //make it return some nice thing
	}

	// zip directory back into an .odt file
	private static void zipOdt(File tempDir, String outputOdtPath) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputOdtPath))) {
			addFilesToZip(tempDir, tempDir, zos);
		}
	}

	// add files to ZIP recursively
	private static void addFilesToZip(File rootDir, File currentDir, ZipOutputStream zos) throws IOException {
		for (File file : currentDir.listFiles()) {
			if (file.isDirectory()) {
				addFilesToZip(rootDir, file, zos);
			} else {
				try (FileInputStream fis = new FileInputStream(file)) {
					String zipEntryName = rootDir.toURI().relativize(file.toURI()).getPath();
					zos.putNextEntry(new ZipEntry(zipEntryName));
					IOUtils.copy(fis, zos);
					zos.closeEntry();
				}
			}
		}
	}

	private static void updateImportInContentXml(File tempDir, String oldFileName, String newFileName) throws Exception { //TODO check Exception
		File contentXmlFile = new File(tempDir, "content.xml"); //TODO add constants for styles.xml and for content.xml
		if (!contentXmlFile.exists()) {
			log.warn("content.xml not found!");
			return;
		}

		// pasre the content.xml
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(contentXmlFile);

		// find and replace the import placeholder
		NodeList importNodes = doc.getElementsByTagName("text:text-input");
		for (int i = 0; i < importNodes.getLength(); i++) {
			Element element = (Element) importNodes.item(i);
			String content = element.getTextContent();

			if (content.contains("[import " + oldFileName + "]")) {
				element.setTextContent(content.replace(oldFileName, newFileName));
				log.info("Updated: {}  → {} ", content , element.getTextContent());
			}
		}

		// write modified content.xml back
		try (FileOutputStream fos = new FileOutputStream(contentXmlFile)) {
			// save the updated XML
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(new DOMSource(doc), new StreamResult(fos));
		}
	}

	private File unzipOdt(String odtFilePath) throws IOException {
		File tempDir = new File("temp_odt_dir");
		if (!tempDir.exists())
			tempDir.mkdir();

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(odtFilePath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File newFile = new File(tempDir, entry.getName());
				new File(newFile.getParent()).mkdirs();

				try (FileOutputStream fos = new FileOutputStream(newFile)) {
					IOUtils.copy(zis, fos);
				}
			}
		}
		return tempDir;
	}

	private Map<String, List<String>> scanDirectory(File folder) {
		Map<String, List<String>> map = new HashMap<>();
		if (!folder.exists() || !folder.isDirectory()) {
			log.error("Invalid directory: {}", folder);
			//TODO throw invalidDirectory
			return map;
		}

		File[] directoryFiles = folder.listFiles();
		if (directoryFiles == null || directoryFiles.length == 0) {
			//TODO throw noFilesInDirectoryFound
			log.info("No files found in directory: {} ", folder);
			return map;
		}
		for (File file : directoryFiles) {
			if (file.isDirectory()) {
				map = scanDirectory(file);
			} else if (file.getName().endsWith(".odt")) { //check if the file is an odt file
				linkMap.put(file.getName(), file.toString().replace("\\", "/"));
				List<String> imports = findImportsInOdt(file);
				if (!imports.isEmpty()) {
					map.put(file.getName(), imports);
				}
			} else {
				log.info("The file is not .odt file {} ", file);
			}
		}
		return map;
	}

	public static List<String> findImportsInOdt(File odtFile) {
		List<String> imports = getImportFromFile(odtFile, "styles.xml");
		imports.addAll(getImportFromFile(odtFile, "content.xml"));
		return imports;
	}

	private static List<String> getImportFromFile(File odtFile, String fileType) {
		List<String> importList = new ArrayList<>();
		try (ZipFile zipFile = new ZipFile(odtFile)) {
			ZipEntry contentEntry = zipFile.getEntry(fileType);
			if (contentEntry == null) {
				//TODO noContextXmlFoundInOdtFile
				log.warn("{} not found in {} ", fileType, odtFile.getName());
				return importList;
			}

			try (InputStream is = zipFile.getInputStream(contentEntry);
					BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
				StringBuilder content = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line);
				}

				Matcher matcher = IMPORT_PATTERN.matcher(content.toString()); //TODO ignore imports that are not in text:text-input tag
				while (matcher.find()) {
					importList.add(matcher.group(1)); // Extracts file name from [import blabla.odt]
				}
			}

		} catch (IOException e) {
			log.error("Error reading {} :  {} ", odtFile.getName(), e.getMessage());
			//TODO exception
		}
		return importList;
	}
}
