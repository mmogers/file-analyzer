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
    private static final Map<String, String> linkMap = new HashMap<>();

    /**
     * searches for all the odt files in the root folder and its sub folders
     * @param folder - root folder
     * @return the list ao odt files that have imports and all their imported files
     */
    @Override
    public List<OdtFile> getFileImports(File folder) {
        Map<String, List<String>> importMap;
        List <OdtFile> odtFiles = new ArrayList<>();

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

    /**
     * scans the specified directory for odt files
     * @param folder root folder
     * @return
     */
    private   Map<String,List<String>> scanDirectory(File folder) {
        Map<String,List<String>> map = new HashMap<>();
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("Invalid directory: {}",folder);
            //TODO throw invalidDirectory
            return map ;
        }

        File[] directoryFiles = folder.listFiles();
        if (directoryFiles == null || directoryFiles.length == 0) {
            //TODO throw noFilesInDirectoryFound
            log.info("No files found in directory: {} ", folder);
            return map;
        }
        for(File file: directoryFiles){
            if (file.isDirectory()) {
                map = scanDirectory(file);
            } else if(file.getName().endsWith(".odt")) { //check if the file is an odt file
                linkMap.put(file.getName(), file.toString().replace("\\", "/"));
                List<String> imports = findImportsInOdt(file);
                if (!imports.isEmpty()) {
                    map.put(file.getName(), imports);
                }
            } else{
                log.info("The file is not .odt file {} ", file);
            }
        }
        return map;
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
