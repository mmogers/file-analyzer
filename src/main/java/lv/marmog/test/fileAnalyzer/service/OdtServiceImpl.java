//TODO add more logs
//TODO add junit tests
//TODO place all the methods one after another
//TODO check SOLID
//TODO check if link exists
//TODO check all the cases - empty directory, no link, no odt file, some other files, corrupted files
//TODO what to do if we want to show all odt files even without imports
package lv.marmog.test.fileAnalyzer.service;

import lv.marmog.test.fileAnalyzer.exception.DirectoryScanException;
import lv.marmog.test.fileAnalyzer.exception.InvalidFolderException;
import lv.marmog.test.fileAnalyzer.exception.OdtProcessingException;
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
			throw new InvalidFolderException ("Invalid folder provided: " + (folder != null ? folder.getAbsolutePath() : "null"));
		}
		try{
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
					if(linkMap.containsKey(importStr)){
						newImportFiles.add(new OdtFile(importStr, linkMap.get(importStr)));
					} else{
						log.warn("Import link not found in linkMap for file {}", importStr);
					}

				});
				newFile.setImportFiles(newImportFiles);
				odtFiles.add(newFile);

			});
			log.info("Successfully found {} ODT files with links", odtFiles.size());
			return odtFiles;

		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new OdtProcessingException("Failed to process ODT files: " + e.getMessage(), e);
		} catch (DirectoryScanException e) {
			throw new DirectoryScanException("Failed to scan directory: " + folder + ". " + e.getMessage(), e);
		}

	}

	@Override
	public OdtFile updateImport(String odtFilePath, String oldFileName , String newFileName ) throws Exception {
		Map<String, String> linkMap = getLinkMap(new File(directoryPath));

		// Unzip the .odt file (a ZIP archive)
		File tempDir = unzipOdt(linkMap.get(odtFilePath));

		// update the XML files (content.xml and styles.xml)
		//TODO: maybe we can check where is the source file, and check only one
		updateImportInXmlFiles(tempDir, oldFileName, newFileName, CONTENT_XML);
		updateImportInXmlFiles(tempDir, oldFileName, newFileName, STYLES_XML);

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
		for (File file : currentDir.listFiles()) { //TODO check for NULL POINTER
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

	private static void updateImportInXmlFiles(File tempDir, String oldFileName, String newFileName, String fileType) throws Exception { //TODO check Exception
		File xmlFile = new File(tempDir, fileType);
		if (!xmlFile.exists()) {
			log.warn("{} not found!", fileType);
			return;
		}

		// pasre the content.xml
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xmlFile);

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
		try (FileOutputStream fos = new FileOutputStream(xmlFile)) {
			// save the updated XML
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.transform(new DOMSource(doc), new StreamResult(fos));
		}
	}

	private static File unzipOdt(String odtFilePath) throws IOException {
		File tempDir = new File("temp_odt_dir");
		if (!tempDir.exists()) {
			tempDir.mkdir();
		}

		try (ZipInputStream zis = new ZipInputStream(new FileInputStream(odtFilePath))) {
			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File newFile = new File(tempDir, entry.getName());
				new File(newFile.getParent()).mkdirs();//TODO check if uts ignored

				try (FileOutputStream fos = new FileOutputStream(newFile)) {
					IOUtils.copy(zis, fos);
				}
			}
		}
		return tempDir;
	}

	//TODO reduce complexity
	private Map<String, List<String>> scanDirectory(File folder, Map<String, String> linkMap )
			throws IOException, ParserConfigurationException, SAXException, InvalidFolderException,
			DirectoryScanException, OdtProcessingException {
		Map<String, List<String>> map = new HashMap<>();

		checkValidFolder(folder);

		File[] directoryFiles = folder.listFiles();

		if (directoryFiles == null || directoryFiles.length == 0) {
			log.info("No files found in directory: {}", folder);
			return map;
		}

		for (File file : directoryFiles) {
				if (file.isDirectory()) {
					try {
						map.putAll(scanDirectory(file, linkMap));
					} catch (Exception e){
						log.error("Error scanning directory: {} ", file);
						throw new DirectoryScanException("Error scanning sub-directory: " + file, e);
					}
				} else if (file.getName().endsWith(".odt")) { //check if the file is an odt file
					try{
						List<String> imports = findImportsInOdt(file, linkMap);
						if (!imports.isEmpty()) {
							map.put(file.getName(), imports);
							log.info("Found imports in file: {} with imports: {}", file.getName(), imports);
						}
					} catch (IOException | ParserConfigurationException | SAXException e){
						log.error("Error processing .odt file: {}", file.getName());
						throw new OdtProcessingException( "Error processing .odt file: " + file, e);
					}
				} else {
					log.info("The file is not .odt file {} ", file.getName());
				}
		}

		return map;
	}

	private static void checkValidFolder(File folder) throws InvalidFolderException {
		if (!folder.exists() || !folder.isDirectory()) {
			log.error("Invalid directory: {}", folder);
			throw new InvalidFolderException("Invalid directory: " + folder);
		}
	}

	public static List<String> findImportsInOdt(File odtFile, Map<String, String> linkMap)
			throws IOException, ParserConfigurationException, SAXException {
		List<String> imports = getImportFromFile(odtFile, "styles.xml", linkMap);
		imports.addAll(getImportFromFile(odtFile, "content.xml", linkMap));
		return imports;
	}

	private static List<String> getImportFromFile(File odtFile, String fileType, Map<String, String> linkMap)
			throws IOException, ParserConfigurationException, SAXException {
		List<String> importList = new ArrayList<>();

		// Unzip the .odt file (a ZIP archive)
		File tempDir = unzipOdt(linkMap.get(odtFile.getName()));

		File xmlFile = new File(tempDir, fileType);
		if (!xmlFile.exists()) {
			log.warn("{} not found!", fileType);
			return importList;
		}

		// pasre the content.xml
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(xmlFile);

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

	/**
	 *
	 * @param folder The folder to scan
	 * @return A Map of odt file names to file paths
	 */
	public static  Map<String, String> getLinkMap(File folder) throws InvalidFolderException {
		Map<String, String> linkMap = new HashMap<>();

		checkValidFolder(folder);

		File[] directoryFiles = folder.listFiles();

		if (directoryFiles == null || directoryFiles.length == 0) {
			log.info("No files found in directory: {}", folder);
			return linkMap;
		}
			//recursion
			for (File file : directoryFiles) {
				try{
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
				} catch (Exception e){
					log.error("Error processing file: {}", file.getAbsolutePath(), e);

				}
			}
		log.info("Processed {} ODT files from directory: {}", linkMap.size(), folder);
		return linkMap;
	}
}
