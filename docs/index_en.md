---
title: Import for journal articles from an Endnote Export
identifier: intranda_import_endnote
published: true
description: This is a technical documentation for the plugin to import newspaper articles including merging with existing processes.
keywords:
    - Goobi workflow
    - Plugin
    - Import Plugin
---
## Introduction
This documentation describes the installation, configuration and use of the plugin to import journal articles from an Excel file exported from Endnote.


## Installation
The plugin must be installed in the following folder:

```bash
/opt/digiverso/goobi/plugins/import/plugin_intranda_import_endnote-base.jar
```

There is also a configuration file, which must be located at the following location:

```bash
/opt/digiverso/goobi/config/plugin_intranda_import_endnote.xml
```


## Overview and functionality
To use the import, the mass import area must be opened in the process templates and the `intranda_import_endnote` plugin must be selected in the `File upload import` tab. An Excel file can then be uploaded and imported.

![Selection of the plugin for performing the import](screen1_en.png)

The import takes place line by line. For each line, the process title is generated from the configured fields and checked to see whether the volume already exists in Goobi. If this is not the case, the process is created again and the configured metadata for `anchor` and `volume` is imported.

Now it is checked whether an issue should be created. This is done on the basis of the `Issue` column. If the field is empty, the article is appended directly to the year, otherwise the correct issue is searched for. If it does not exist yet, it will also be created. The sorting of the issues is based on the number of the `Issue` column.

The article is then created and added to the issue or volume. If several articles exist, the sorting is done by specifying the start page from the `Pages` column.


## Configuration
The configuration is done via the configuration file `plugin_intranda_import_endnote.xml` and can be adapted during operation.

```xml
<config_plugin>
    <config>
        <template>*</template>
        <processTitleGeneration>TSL+'_'+ISSN+'_'+Volume</processTitleGeneration>
        <anchorDocType>Periodical</anchorDocType>
        <volumeDocType>PeriodicalVolume</volumeDocType>
        <issueDocType>PeriodicalIssue</issueDocType>
        <articleDocType>Article</articleDocType>

        <metadata ugh="CatalogIDDigital" headerName="Key" docType="child"/>
        <metadata ugh="PublicationYear" headerName="Publication Year" docType="volume"/>
        <metadata ugh="Author" headerName="Author" docType="child" />
        <metadata ugh="TitleDocMain" headerName="Title" docType="child"/>
        <metadata ugh="TitleDocMain" headerName="Publication Title" docType="anchor"/>
        <metadata ugh="ISSN" headerName="ISSN" docType="anchor"/>
        <metadata ugh="DOI" headerName="DOI" docType="child"/>
        <metadata ugh="URL" headerName="Url" docType="child"/>
        <metadata ugh="Abstract" headerName="Abstract Note" docType="child"/>
        <metadata ugh="Pages" headerName="Pages" docType="child"/>
        <metadata ugh="CurrentNo" headerName="Issue" docType="child"/>
        <metadata ugh="CurrentNo" headerName="Volume" docType="volume"/>
        <metadata ugh="DocLanguage" headerName="Language" docType="child"/>
        <metadata ugh="Note" headerName="Manual Tags" docType="volume"/>
    </config>
</config_plugin>
```

The configuration basically allows different configurations for different process templates. For this purpose, only the name of the desired template must be entered in the `template` field. The entry with the value `*` is used for all templates for which no separate configuration exists.

The `processTitleGeneration` element defines the rules with which the process title is to be generated. The same conventions apply as in `goobi_projects.xml`. The two values `ATS` (author-title-key) and `TSL` (title-key) are automatically generated from the available metadata, for the use of further metadata the column names from the Excel file can be used.

The elements `anchorDocType`, `volumeDocType`, `issueDocType` and `articleDocType` define the structural elements to be used for the elements journal, volume, issue and article. They must exist in the ruleset.

This is followed by the mapping of the metadata. The `metadata` element is used for this purpose. Three attributes are allowed, in `ugh` the metadata name from the ruleset is stored, in `headerName` the heading of the column from the Excel file and in docType it is defined whether the metadata should be added in the journal title (`anchor`), `volume` or article (`child`).
