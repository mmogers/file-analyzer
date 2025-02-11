//add logs
package lv.marmog.test.fileAnalyzer.service;

import lv.marmog.test.fileAnalyzer.model.OdtFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static lv.marmog.test.fileAnalyzer.constants.Constants.IMPORT_PATTERN;

@Service
public class OdtServiceImpl implements OdtService{

    private static final Logger log = LoggerFactory.getLogger(OdtServiceImpl.class);

    static Map<String, List<String>> importMap = new HashMap<>();
    static List <OdtFile> odtFiles = new ArrayList<>();

    @Override
    public List<OdtFile> getFileImports(File folder) {
        scanDirectory(folder, importMap);

        importMap.forEach((file, imports) -> {
            OdtFile newFile = new OdtFile(file);
            List<OdtFile> newImportFiles = new LinkedList<>();
            imports.forEach(importStr -> {
                newImportFiles.add(new OdtFile(importStr));
            });
            newFile.setImportFiles(newImportFiles);
            odtFiles.add(newFile);
        });

        return odtFiles;
    }

    private void scanDirectory(File dir,  Map<String, List<String>> importMap) {
        if (!dir.exists() || !dir.isDirectory()) {
            log.error("Invalid directory: {}",dir);
            //TODO throw invalidDirectory
            return;
        }

        File[] directoryFiles = dir.listFiles();
        if (directoryFiles == null || directoryFiles.length == 0) {
            //TODO throw noFilesInDirectoryFound
            log.info("No files found in directory: {} ", dir);
            return;
        }
        for(File file: directoryFiles){
            if (file.isDirectory()) {
                scanDirectory(file, importMap);
            } else {
                List<String> imports = findImportsInOdt(file);
                if (!imports.isEmpty()) {
                    importMap.put(file.getName(), imports);
                }
            }
        }

    }

    public static List<String> findImportsInOdt(File odtFile) {
        List<String> imports = getImportFromFile(odtFile,"styles.xml");
        imports.addAll(getImportFromFile(odtFile, "content.xml"));
        return imports;
    }

    private static List<String> getImportFromFile(File odtFile, String fileType) {
        List<String> importList = new ArrayList<>();
        try (ZipFile zipFile = new ZipFile(odtFile)) {
            ZipEntry contentEntry = zipFile.getEntry(fileType);
            if (contentEntry == null) {
                //TODO noContextXmlFoundInOdtFile
                log.warn("{} not found in {} ", fileType,odtFile.getName());
                return importList;
            }

            try (InputStream is = zipFile.getInputStream(contentEntry);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }

                Matcher matcher = IMPORT_PATTERN.matcher(content.toString());
                while (matcher.find()) {
                    importList.add(matcher.group(1)); // Extracts file name from [import blabla.odt]
                }
            }

        } catch (IOException e) {
            log.error("Error reading {} :  {} ",odtFile.getName(),e.getMessage());
            //TODO exception
        }
        return importList;
    }
}
