//TODO add junit tests
//TODO check pom dependencies
//TODO check SOLID
//TODO check if link exists
//TODO check all the cases - empty directory, no link, no odt file, some other files, corrupted files
//TODO what to do if we want to show all odt files even without imports
package lv.marmog.test.fileAnalyzer.service;

import lv.marmog.test.fileAnalyzer.exception.DirectoryScanException;
import lv.marmog.test.fileAnalyzer.exception.FileCleanupException;
import lv.marmog.test.fileAnalyzer.exception.FileCompressionException;
import lv.marmog.test.fileAnalyzer.exception.ExtractionException;
import lv.marmog.test.fileAnalyzer.exception.FileNotFoundInFolderException;
import lv.marmog.test.fileAnalyzer.exception.InvalidFolderException;
import lv.marmog.test.fileAnalyzer.exception.OdtProcessingException;
import lv.marmog.test.fileAnalyzer.exception.XmlProcessingException;
import lv.marmog.test.fileAnalyzer.exception.XmlUpdateException;
import lv.marmog.test.fileAnalyzer.model.OdtFile;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static lv.marmog.test.fileAnalyzer.constants.Constants.CONTENT_XML;
import static lv.marmog.test.fileAnalyzer.constants.Constants.IMPORT_PATTERN;
import static lv.marmog.test.fileAnalyzer.constants.Constants.STYLES_XML;
import static org.apache.tomcat.util.http.fileupload.FileUtils.deleteDirectory;

@Service
public class OdtServiceImpl implements OdtService {

	private static final Logger log = LoggerFactory.getLogger(OdtServiceImpl.class);
	@Value("${directory.path}")
	String directoryPath;

	@Override
	public List<OdtFile> getFileImports(File folder)
			throws InvalidFolderException, OdtProcessingException, DirectoryScanException {

		if (folder == null || !folder.exists() || !folder.isDirectory()) {
			log.error("Invalid folder: {}", folder);
			throw new InvalidFolderException(
					"Invalid folder provided: " + (folder != null ? folder.getAbsolutePath() : "null"));
		}
		try {
			//list of all .odt files and path to them in folder and subfolders
			Map<String, String> linkMap = getLinkMap(folder);
			Map<String, List<String>> importMap = scanDirectory(folder, linkMap);

			if (importMap.isEmpty()) {
				log.warn("No files with imports found in directory: {}", folder);
			}
			// odt files that have imports
			List<OdtFile> odtFiles = new ArrayList<>();

			importMap.forEach((fileName, imports) -> {
				OdtFile newFile = new OdtFile(fileName, linkMap.get(fileName));
				List<OdtFile> newImportFiles = new LinkedList<>();
				imports.forEach(importStr -> {
					if (linkMap.containsKey(importStr)) {
						newImportFiles.add(new OdtFile(importStr, linkMap.get(importStr)));
					} else {
						log.warn("Import link not found in linkMap for file {}", importStr);
					}

				});
				newFile.setImportFiles(newImportFiles);
				odtFiles.add(newFile);

			});
			log.info("Successfully found {} ODT files with links", odtFiles.size());
			return odtFiles;

		} catch (DirectoryScanException e) {
			throw new DirectoryScanException("Failed to scan directory: " + folder + ". " + e.getMessage(), e);
		}

	}

	@Override
	public OdtFile updateImport(String sourceFile, String existingBlockName, String newBlockName) throws Exception {
		log.info("Starting import update for file: {}, replacing '{}' with '{}'", sourceFile, existingBlockName,
				newBlockName);

		Map<String, String> linkMap = getLinkMap(new File(directoryPath));

		if (!linkMap.containsKey(sourceFile)) {
			log.error("ODT file '{}' not found in directory {}", sourceFile, directoryPath);
			throw new FileNotFoundInFolderException("File not found: " + sourceFile);
		}

		File tempDir;

		tempDir = unzipFile(sourceFile, linkMap); // Unzip the .odt file (a ZIP archive)

		updateXmlFiles(sourceFile, existingBlockName, newBlockName,
				tempDir); // update the XML files (content.xml and styles.xml)

		zipFile(sourceFile, tempDir, linkMap); // zip .odt file back

		deleteTempDirectory(sourceFile, tempDir); //  clean up

		// Return an updated OdtFile object with new import information
		OdtFile updatedFile = new OdtFile(sourceFile, linkMap.get(sourceFile));
		log.info("Returning ODT file with updated input file : {}", updatedFile.getName());

		OdtFile importBlock = new OdtFile(newBlockName, linkMap.get(newBlockName));
		List<OdtFile> importList = new ArrayList<>();
		importList.add(importBlock);
		updatedFile.setImportFiles(importList);

		return updatedFile;
	}

	private static void deleteTempDirectory(String sourceFile, File tempDir) throws FileCleanupException {
		try {
			log.info("deleting temp directory {} ", tempDir.getAbsolutePath());
			deleteDirectory(tempDir);
		} catch (IOException e) {
			throw new FileCleanupException("Failed to delete temporary files for: " + sourceFile, e);
		}
	}

	private static void zipFile(String sourceFile, File tempDir, Map<String, String> linkMap)
			throws FileCompressionException {
		try {
			zipOdt(tempDir, linkMap.get(sourceFile)); // zip directory back into an .odt file
		} catch (IOException e) {
			throw new FileCompressionException("Failed to compress ODT file: " + sourceFile, e);
		}
	}

	private static void zipOdt(File tempDir, String outputOdtPath) throws IOException {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputOdtPath))) {
			addFilesToZip(tempDir, tempDir, zos);    // add files to ZIP recursively
		}
	}

	private static void updateXmlFiles(String sourceFile, String existingBlockName, String newBlockName, File tempDir)
			throws XmlUpdateException {
		try {
			updateImportInXmlFiles(tempDir, existingBlockName, newBlockName, CONTENT_XML);
			updateImportInXmlFiles(tempDir, existingBlockName, newBlockName, STYLES_XML);
		} catch (Exception e) {
			throw new XmlUpdateException("Error updating XML files in: " + sourceFile, e);
		}
	}

	private static File unzipFile(String sourceFile, Map<String, String> linkMap) throws ExtractionException {
		File tempDir;

		try {
			tempDir = unzipOdt(linkMap.get(sourceFile));
		} catch (IOException e) {
			throw new ExtractionException("Failed to unzip ODT file: " + sourceFile, e);
		}
		return tempDir;
	}

	private static void addFilesToZip(File rootDir, File currentDir, ZipOutputStream zos) throws IOException {
		if (currentDir == null || !currentDir.exists()) {
			log.warn("Skipping ZIP process: directory '{}' does not exist or is null", currentDir);
			return;
		}
		for (File file : currentDir.listFiles()) {
			if (file.isDirectory()) {
				addFilesToZip(rootDir, file, zos);    // add files to ZIP recursively
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

	private static void updateImportInXmlFiles(File tempDir, String oldFileName, String newFileName, String fileType)
			throws XmlProcessingException {
		File xmlFile = new File(tempDir, fileType);

		if (!xmlFile.exists()) {
			log.warn("{} not found!", fileType);
			return;
		}

		Document doc = getXmlDocument(xmlFile);

		if (!exchangeImportBlock(oldFileName, newFileName, fileType, doc)) {
			log.info("No matching import blocks found in {}", fileType);
			return;
		}

		writeModifiedContentBack(xmlFile, doc);
		log.info("Successfully updated {} in {}", fileType, xmlFile.getAbsolutePath());

	}

	private static Document getXmlDocument(File xmlFile) throws XmlProcessingException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(xmlFile);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new XmlProcessingException("Error parsing XML file: " + xmlFile.getAbsolutePath(), e);
		}
	}

	private static boolean exchangeImportBlock(String oldFileName, String newFileName, String fileType, Document doc) {
		boolean updated = false;
		NodeList importNodes = doc.getElementsByTagName("text:text-input");

		for (int i = 0; i < importNodes.getLength(); i++) {
			Element element = (Element) importNodes.item(i);
			String content = element.getTextContent();

			if (content.contains("[import " + oldFileName + "]")) {
				element.setTextContent(content.replace(oldFileName, newFileName));
				log.info("Updated: {}  → {} ", content, element.getTextContent());
				updated = true;
			}
		}
		if (!updated) {
			log.warn("No  '[import {}]' found in '{}'", oldFileName, fileType);
			return false;
		}
		return true;
	}

	private static void writeModifiedContentBack(File xmlFile, Document doc) throws XmlProcessingException {
		try (FileOutputStream fos = new FileOutputStream(xmlFile)) {
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(new DOMSource(doc), new StreamResult(fos));
		} catch (TransformerException | IOException e) {
			throw new XmlProcessingException("Error writing updated XML file: " + xmlFile.getAbsolutePath(), e);
		}
	}

	private static File unzipOdt(String odtFilePath) throws IOException, ExtractionException {
		File tempDir = new File("temp_odt_dir");
		if (!tempDir.exists() && !tempDir.mkdir()) {
			log.error("Failed to create temporary directory '{}'", tempDir.getAbsolutePath());
			throw new ExtractionException("Could not create temp directory: " + tempDir.getAbsolutePath());
		}

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(odtFilePath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File newFile = new File(tempDir, entry.getName());

				File parentDir = newFile.getParentFile();
				if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
					throw new ExtractionException("Could not create directory: " + parentDir.getAbsolutePath());
				}

				try (FileOutputStream fos = new FileOutputStream(newFile)) {
					IOUtils.copy(zis, fos);
				} catch (IOException e) {
					throw new ExtractionException("Error writing file: " + newFile.getAbsolutePath(), e);
				}
			}
		} catch (IOException e) {
			throw new ExtractionException("Failed to extract ODT file: " + odtFilePath, e);
		}
		log.info("Successfully extracted ODT file to '{}'", tempDir.getAbsolutePath());
		return tempDir;
	}

	private Map<String, List<String>> scanDirectory(File folder, Map<String, String> linkMap)
			throws InvalidFolderException, DirectoryScanException, OdtProcessingException {

		checkValidFolder(folder);

		File[] directoryFiles = folder.listFiles();
		Map<String, List<String>> map = new HashMap<>();

		if (directoryFiles == null || directoryFiles.length == 0) {
			log.info("No files found in directory: {}", folder);
			return map;
		}

		for (File file : directoryFiles) {
			processFileOrDirectory(file, linkMap, map);
		}

		return map;
	}

	private void processFileOrDirectory(File file, Map<String, String> linkMap, Map<String, List<String>> map)
			throws DirectoryScanException, OdtProcessingException {

		if (file.isDirectory()) {
			scanSubDirectory(file, linkMap, map);
		} else if (file.getName().startsWith("~$")) { //check if the file is not corrupted
			log.info("The file {} is corrupted ", file.getName());
		} else if ((file.getName().endsWith(".odt")) && !(file.getName()
				.startsWith("~$"))) { //check if the file is an odt file
			processOdfFile(file, linkMap, map);
		} else {
			log.info("The file is not .odt file {} ", file.getName());
		}
	}

	/**
	 * Processes an ODT file and extracts import links.
	 */
	private static void processOdfFile(File file, Map<String, String> linkMap, Map<String, List<String>> map)
			throws OdtProcessingException {

		try {
			List<String> imports = findImportsInOdt(file, linkMap);

			if (!imports.isEmpty()) {
				map.put(file.getName(), imports);
				log.info("Found imports in file: {} with imports: {}", file.getName(), imports);
			}
		} catch (IOException | ParserConfigurationException | SAXException | ExtractionException |
				XmlProcessingException e) {
			throw new OdtProcessingException("Error processing .odt file: " + file, e);
		}
	}

	private void scanSubDirectory(File file, Map<String, String> linkMap, Map<String, List<String>> map)
			throws DirectoryScanException {

		try {
			map.putAll(scanDirectory(file, linkMap)); //recursion
		} catch (Exception e) {
			throw new DirectoryScanException("Error scanning sub-directory: " + file, e);
		}
	}

	private static void checkValidFolder(File folder) throws InvalidFolderException {
		if (!folder.exists() || !folder.isDirectory()) {
			log.error("Invalid directory: {}", folder);
			throw new InvalidFolderException("Invalid directory: " + folder);
		}
	}

	public static List<String> findImportsInOdt(File odtFile, Map<String, String> linkMap)
			throws IOException, ParserConfigurationException, SAXException, ExtractionException,
			XmlProcessingException {
		List<String> imports = getImportFromFile(odtFile, "styles.xml", linkMap);
		imports.addAll(getImportFromFile(odtFile, "content.xml", linkMap));
		return imports;
	}

	private static List<String> getImportFromFile(File odtFile, String fileType, Map<String, String> linkMap)
			throws IOException, ExtractionException, XmlProcessingException {
		List<String> importList = new ArrayList<>();

		// Unzip the .odt file (a ZIP archive)
		File tempDir = unzipOdt(linkMap.get(odtFile.getName()));

		File xmlFile = new File(tempDir, fileType);
		if (!xmlFile.exists()) {
			log.warn("{} not found!", fileType);
			return importList;
		}

		Document doc = getXmlDocument(xmlFile);

		// find and replace the import placeholder
		NodeList importNodes = doc.getElementsByTagName("text:text-input");
		for (int i = 0; i < importNodes.getLength(); i++) {
			Element element = (Element) importNodes.item(i);
			String content = element.getTextContent();

			Matcher matcher = IMPORT_PATTERN.matcher(content);

			while (matcher.find()) {
				String extractedFilename = matcher.group(1);
				importList.add(extractedFilename);
			}
		}

		deleteDirectory(tempDir);

		return importList;
	}

	private static Document getXmlDocument(DocumentBuilderFactory factory, File xmlFile) throws XmlProcessingException {

		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			return builder.parse(xmlFile);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new XmlProcessingException("Error parsing XML file: " + xmlFile.getAbsolutePath(), e);
		}
	}

	/**
	 * @param folder The folder to scan
	 * @return A Map of odt file names to file paths
	 */
	public static Map<String, String> getLinkMap(File folder) throws InvalidFolderException {
		Map<String, String> linkMap = new HashMap<>();

		checkValidFolder(folder);

		File[] directoryFiles = folder.listFiles();

		if (directoryFiles == null || directoryFiles.length == 0) {
			log.info("No files found in directory: {}", folder);
			return linkMap;
		}
		//recursion
		for (File file : directoryFiles) {
			try {
				if (file.isDirectory()) {
					linkMap.putAll(getLinkMap(file));
				} else if (file.getName().startsWith("~$")) {
					log.warn("the file is corrupted: {} ", file.getName());
				} else if (file.getName().endsWith(".odt")) {
					linkMap.put(file.getName(), file.toString().replace("\\", "/"));
					log.info("Added .odt file to map: {} ", file.getName());

				} else {
					log.info("The file is not .odt file {} ", file.getName());

				}
			} catch (Exception e) {
				log.error("Error processing file: {}", file.getAbsolutePath(), e);

			}
		}
		log.info("Processed {} ODT files from directory: {}", linkMap.size(), folder);
		return linkMap;
	}
}
