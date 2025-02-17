# Processing imports in odt files

## Description

1. Given the root-folder (can be changed in application.yml. example: src/main/resources/static/root-folder/ ) 
    scans the folder and it sub-folders, searches for odt files with import block/s (with odt file). 
    Returns the list of odt files with their imports with paths. 
    API: http://localhost:8080/odt/imports 
   or can be tried using swagger - http://localhost:8080/swagger-ui/index.html
   response for example: 
    ```json
    [
        {
            "name": "template_bb01.odt",
            "link": "src/main/resources/static/root-folder/templates/template_bb01.odt",
            "importFiles": [
                {
                    "name": "footer_2.odt",
                    "link": "src/main/resources/static/root-folder/common/footer_2.odt"
                },
                {
                    "name": "header_2.odt",
                    "link": "src/main/resources/static/root-folder/common/header_2.odt"
                },
                {
                    "name": "block_2.odt",
                    "link": "src/main/resources/static/root-folder/templates/block_2.odt"
                }
            ]
        },
        {
            "name": "template_aa01.odt",
            "link": "src/main/resources/static/root-folder/templates/template_aa01.odt",
            "importFiles": [
                {
                    "name": "footer_1.odt",
                    "link": "src/main/resources/static/root-folder/common/footer_1.odt"
                },
                {
                    "name": "header_1.odt",
                    "link": "src/main/resources/static/root-folder/common/header_1.odt"
                }
            ]
        }
    ]
    
2. Given 3 odt file names - 1 - the file in which we want to change an import block, 2 - existing import file name, 
   3 - new file name; The app will change the import block in the given file.
    API for example: http://localhost:8080/odt/imports?sourceFile=template_bb01.odt&existingBlockName=block_1.odt&newBlockName=block_2.odt 
    or using swagger http://localhost:8080/swagger-ui/index.html
   response for example:
   ```json
    {
        "name": "template_bb01.odt",
        "link": "src/main/resources/static/root-folder/templates/template_bb01.odt",
        "importFiles": [
             {
                "name": "block_2.odt",
                "link": "src/main/resources/static/root-folder/templates/block_2.odt"
            }
        ]
    }

## Technologies Used
- Java 17
- Spring Boot
- Maven
- JUnit
- Swagger

## Running Instructions
1. 