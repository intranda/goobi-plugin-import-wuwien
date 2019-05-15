package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.goobi.production.enums.ImportType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.importer.DocstructElement;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.goobi.production.plugin.interfaces.IImportPluginVersion2;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.properties.ImportProperty;

import de.intranda.goobi.plugins.util.Config;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.forms.MassImportForm;
import de.sub.goobi.helper.exceptions.ImportPluginException;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.Fileformat;
import ugh.dl.Prefs;

@Data
@PluginImplementation
@Log4j
public class EndnoteExcelImport implements IImportPluginVersion2, IPlugin {

    private Prefs prefs;
    private Record data;
    private String importFolder;
    private String processTitle;
    private MassImportForm form;
    private List<ImportType> importTypes = new ArrayList<>();
    private String title = "intranda_import_endnote";
    private File file;
    private String workflowTitle;

    private Config config;

    private Map<String, Integer> headerOrder;


    public EndnoteExcelImport() {
        importTypes.add(ImportType.FILE);
    }

    public Config getConfig() {
        if (config == null) {
            config = loadConfig(workflowTitle);
        }
        return config;
    }

    @Override
    public Fileformat convertData() throws ImportPluginException {
        return null;
    }
    @Override
    public List<ImportObject> generateFiles(List<Record> records) {
        List<ImportObject> answer = new ArrayList<>();

        //        for (Record record : records) {
        //            ImportObject io = new ImportObject();

        //            try {
        //
        //                Object tempObject = record.getObject();
        //                Map<Integer, String> rowMap = (Map<Integer, String>) tempObject;
        //
        //                // generate a mets file
        //                DigitalDocument digitalDocument = null;
        //                Fileformat ff = null;
        //                DocStruct logical = null;
        //                DocStruct anchor = null;
        // TODO

        //                if (!config.isUseOpac()) {
        //                    ff = new MetsMods(prefs);
        //                    digitalDocument = new DigitalDocument();
        //                    ff.setDigitalDocument(digitalDocument);
        //                    String publicationType = getConfig().getPublicationType();
        //                    DocStructType logicalType = prefs.getDocStrctTypeByName(publicationType);
        //                    logical = digitalDocument.createDocStruct(logicalType);
        //                    digitalDocument.setLogicalDocStruct(logical);
        //                    answer.add(io);
        //                } else {
        //                    try {
        //                        if (StringUtils.isBlank(config.getIdentifierHeaderName())) {
        //                            Helper.setFehlerMeldung("Cannot request catalogue, no identifier column defined");
        //                            return Collections.emptyList();
        //                        }
        //
        //                        String catalogueIdentifier = rowMap.get(headerOrder.get(config.getIdentifierHeaderName()));
        //                        if (StringUtils.isBlank(catalogueIdentifier)) {
        //                            continue;
        //                        }
        //                        ff = getRecordFromCatalogue(catalogueIdentifier);
        //                        digitalDocument = ff.getDigitalDocument();
        //                        logical = digitalDocument.getLogicalDocStruct();
        //                        if (logical.getType().isAnchor()) {
        //                            anchor = logical;
        //                            logical = anchor.getAllChildren().get(0);
        //                        }
        //                        answer.add(io);
        //                    } catch (ImportPluginException e) {
        //                        log.error(e);
        //                        io.setErrorMessage(e.getMessage());
        //                        io.setImportReturnValue(ImportReturnValue.NoData);
        //                        continue;
        //                    }
        //                }

        //                DocStructType physicalType = prefs.getDocStrctTypeByName("BoundBook");
        //                DocStruct physical = digitalDocument.createDocStruct(physicalType);
        //                digitalDocument.setPhysicalDocStruct(physical);
        //                Metadata imagePath = new Metadata(prefs.getMetadataTypeByName("pathimagefiles"));
        //                imagePath.setValue("./images/");
        //                physical.addMetadata(imagePath);
        //
        //                // add collections if configured
        //                String col = getConfig().getCollection();
        //                if (StringUtils.isNotBlank(col)) {
        //                    Metadata mdColl = new Metadata(prefs.getMetadataTypeByName("singleDigCollection"));
        //                    mdColl.setValue(col);
        //                    logical.addMetadata(mdColl);
        //                }
        //                // and add all collections that where selected
        //                for (String colItem : form.getDigitalCollections()) {
        //                    if (!colItem.equals(col.trim())) {
        //                        Metadata mdColl = new Metadata(prefs.getMetadataTypeByName("singleDigCollection"));
        //                        mdColl.setValue(colItem);
        //                        logical.addMetadata(mdColl);
        //                    }
        //                }
        //                // create file name for mets file
        //                String fileName = null;
        //
        //                // create importobject for massimport
        //                io.setProcessTitle(record.getId());
        //                io.setImportReturnValue(ImportReturnValue.ExportFinished);
        //
        //                for (MetadataMappingObject mmo : getConfig().getMetadataList()) {
        //
        //                    String value = rowMap.get(headerOrder.get(mmo.getHeaderName()));
        //                    String identifier = null;
        //                    if (mmo.getNormdataHeaderName() != null) {
        //                        identifier = rowMap.get(headerOrder.get(mmo.getNormdataHeaderName()));
        //                    }
        //                    if (StringUtils.isNotBlank(mmo.getRulesetName()) && StringUtils.isNotBlank(value)) {
        //                        try {
        //                            Metadata md = new Metadata(prefs.getMetadataTypeByName(mmo.getRulesetName()));
        //                            md.setValue(value);
        //                            if (identifier != null) {
        //                                md.setAutorityFile("gnd", "http://d-nb.info/gnd/", identifier);
        //
        //                            }
        //                            if (anchor != null && "anchor".equals(mmo.getDocType())) {
        //                                anchor.addMetadata(md);
        //                            } else {
        //                                logical.addMetadata(md);
        //                            }
        //                        } catch (MetadataTypeNotAllowedException e) {
        //                            log.info(e);
        //                            // Metadata is not known or not allowed
        //                        }
        //
        //                        if (mmo.getRulesetName().equalsIgnoreCase("CatalogIDDigital") && !"anchor".equals(mmo.getDocType())) {
        //                            fileName = getImportFolder() + File.separator + value + ".xml";
        //                            io.setProcessTitle(value);
        //                            io.setMetsFilename(fileName);
        //                        }
        //                    }
        //
        //                    if (StringUtils.isNotBlank(mmo.getPropertyName())) {
        //                        Processproperty p = new Processproperty();
        //                        p.setTitel(mmo.getPropertyName());
        //                        p.setWert(value);
        //                        io.getProcessProperties().add(p);
        //                    }
        //                }
        //
        //                for (PersonMappingObject mmo : getConfig().getPersonList()) {
        //                    String firstname = "";
        //                    String lastname = "";
        //                    if (mmo.isSplitName()) {
        //                        String name = rowMap.get(headerOrder.get(mmo.getHeaderName()));
        //                        if (StringUtils.isNotBlank(name)) {
        //                            if (name.contains(mmo.getSplitChar())) {
        //                                if (mmo.isFirstNameIsFirst()) {
        //                                    firstname = name.substring(0, name.lastIndexOf(mmo.getSplitChar()));
        //                                    lastname = name.substring(name.lastIndexOf(mmo.getSplitChar()));
        //                                } else {
        //                                    lastname = name.substring(0, name.lastIndexOf(mmo.getSplitChar())).trim();
        //                                    firstname = name.substring(name.lastIndexOf(mmo.getSplitChar()) + 1).trim();
        //                                }
        //                            } else {
        //                                lastname = name;
        //                            }
        //                        }
        //                    } else {
        //                        firstname = rowMap.get(headerOrder.get(mmo.getFirstnameHeaderName()));
        //                        lastname = rowMap.get(headerOrder.get(mmo.getLastnameHeaderName()));
        //                    }
        //
        //                    String identifier = null;
        //                    if (mmo.getNormdataHeaderName() != null) {
        //                        identifier = rowMap.get(headerOrder.get(mmo.getNormdataHeaderName()));
        //                    }
        //                    if (StringUtils.isNotBlank(mmo.getRulesetName())) {
        //                        try {
        //                            Person p = new Person(prefs.getMetadataTypeByName(mmo.getRulesetName()));
        //                            p.setFirstname(firstname);
        //                            p.setLastname(lastname);
        //
        //                            if (identifier != null) {
        //                                p.setAutorityFile("gnd", "http://d-nb.info/gnd/", identifier);
        //                            }
        //                            if (anchor != null && "anchor".equals(mmo.getDocType())) {
        //                                anchor.addPerson(p);
        //                            } else {
        //                                logical.addPerson(p);
        //                            }
        //
        //                            //                            logical.addPerson(p);
        //                        } catch (MetadataTypeNotAllowedException e) {
        //                            log.info(e);
        //                            // Metadata is not known or not allowed
        //                        }
        //                    }
        //                }
        //
        //                for (GroupMappingObject gmo : getConfig().getGroupList()) {
        //                    try {
        //                        MetadataGroup group = new MetadataGroup(prefs.getMetadataGroupTypeByName(gmo.getRulesetName()));
        //                        for (MetadataMappingObject mmo : gmo.getMetadataList()) {
        //                            String value = rowMap.get(headerOrder.get(mmo.getHeaderName()));
        //                            Metadata md = new Metadata(prefs.getMetadataTypeByName(mmo.getRulesetName()));
        //                            md.setValue(value);
        //                            if (mmo.getNormdataHeaderName() != null) {
        //                                md.setAutorityFile("gnd", "http://d-nb.info/gnd/", rowMap.get(headerOrder.get(mmo.getNormdataHeaderName())));
        //                            }
        //                            group.addMetadata(md);
        //                        }
        //                        for (PersonMappingObject pmo : gmo.getPersonList()) {
        //                            Person p = new Person(prefs.getMetadataTypeByName(pmo.getRulesetName()));
        //                            String firstname = "";
        //                            String lastname = "";
        //                            if (pmo.isSplitName()) {
        //                                String name = rowMap.get(headerOrder.get(pmo.getHeaderName()));
        //                                if (StringUtils.isNotBlank(name)) {
        //                                    if (name.contains(pmo.getSplitChar())) {
        //                                        if (pmo.isFirstNameIsFirst()) {
        //                                            firstname = name.substring(0, name.lastIndexOf(pmo.getSplitChar()));
        //                                            lastname = name.substring(name.lastIndexOf(pmo.getSplitChar()));
        //                                        } else {
        //                                            lastname = name.substring(0, name.lastIndexOf(pmo.getSplitChar()));
        //                                            firstname = name.substring(name.lastIndexOf(pmo.getSplitChar()));
        //                                        }
        //                                    } else {
        //                                        lastname = name;
        //                                    }
        //                                }
        //                            } else {
        //                                firstname = rowMap.get(headerOrder.get(pmo.getFirstnameHeaderName()));
        //                                lastname = rowMap.get(headerOrder.get(pmo.getLastnameHeaderName()));
        //                            }
        //
        //                            p.setFirstname(firstname);
        //                            p.setLastname(lastname);
        //
        //                            if (pmo.getNormdataHeaderName() != null) {
        //                                p.setAutorityFile("gnd", "http://d-nb.info/gnd/", rowMap.get(headerOrder.get(pmo.getNormdataHeaderName())));
        //                            }
        //                            group.addMetadata(p);
        //                        }
        //                        if (anchor != null && "anchor".equals(gmo.getDocType())) {
        //                            anchor.addMetadataGroup(group);
        //                        } else {
        //                            logical.addMetadataGroup(group);
        //                        }
        //
        //                        //                        logical.addMetadataGroup(group);
        //
        //                    } catch (MetadataTypeNotAllowedException e) {
        //                        log.info(e);
        //                        // Metadata is not known or not allowed
        //                    }
        //                }
        //
        //                // write mets file into import folder
        //                ff.write(fileName);
        //            } catch (WriteException | PreferencesException | MetadataTypeNotAllowedException | TypeNotAllowedForParentException e) {
        //                io.setImportReturnValue(ImportReturnValue.WriteError);
        //                io.setErrorMessage(e.getMessage());
        //            }

        //        }
        // end of all excel rows
        return answer;
    }


    @Override
    public List<Record> generateRecordsFromFile() {

        if (StringUtils.isBlank(workflowTitle)) {
            workflowTitle = form.getTemplate().getTitel();
        }

        List<Record> recordList = new ArrayList<>();
        String idColumn = getConfig().getIdentifierHeaderName();
        headerOrder = new HashMap<>();

        InputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(file);

            BOMInputStream in = new BOMInputStream(fileInputStream, false);

            Workbook wb = WorkbookFactory.create(in);

            Sheet sheet = wb.getSheetAt(0);

            Iterator<Row> rowIterator = sheet.rowIterator();
            //  read and validate first row
            Row headerRow = rowIterator.next();

            int numberOfCells = headerRow.getLastCellNum();
            for (int i = 0; i < numberOfCells; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    cell.setCellType(CellType.STRING);
                    String value = cell.getStringCellValue();
                    headerOrder.put(value, i);
                }
            }

            while (rowIterator.hasNext()) {
                Map<Integer, String> map = new HashMap<>();
                Row row = rowIterator.next();
                int lastColumn = row.getLastCellNum();
                if (lastColumn == -1) {
                    continue;
                }
                for (int cn = 0; cn < lastColumn; cn++) {
                    //                while (cellIterator.hasNext()) {
                    //                    Cell cell = cellIterator.next();
                    Cell cell = row.getCell(cn, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String value = "";
                    switch (cell.getCellTypeEnum()) {
                        case BOOLEAN:
                            value = cell.getBooleanCellValue() ? "true" : "false";
                            break;
                        case FORMULA:
                            //                            value = cell.getCellFormula();
                            value = cell.getRichStringCellValue().getString();
                            break;
                        case NUMERIC:
                            value = String.valueOf((int) cell.getNumericCellValue());
                            break;
                        case STRING:
                            value = cell.getStringCellValue();
                            break;
                        default:
                            // none, error, blank
                            value = "";
                            break;
                    }
                    map.put(cn, value);

                }

                // just add the record if the conditional column contains a value

                Record r = new Record();
                r.setId(map.get(headerOrder.get(idColumn)));
                r.setObject(map);
                recordList.add(r);

            }

        } catch (Exception e) {
            log.error(e);
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }

        return recordList;
    }
    @Override
    public List<Record> splitRecords(String records) {
        return null;
    }

    @Override
    public List<Record> generateRecordsFromFilenames(List<String> filenames) {
        return null;
    }

    @Override
    public void setFile(File importFile) {
        this.file = importFile;

    }

    @Override
    public List<String> splitIds(String ids) {
        return null;
    }

    @Override
    public List<ImportProperty> getProperties() {
        return null;
    }

    @Override
    public List<String> getAllFilenames() {
        return null;
    }

    @Override
    public void deleteFiles(List<String> selectedFilenames) {
    }

    @Override
    public List<? extends DocstructElement> getCurrentDocStructs() {
        return null;
    }

    @Override
    public String deleteDocstruct() {
        return null;
    }

    @Override
    public String addDocstruct() {
        return null;
    }

    @Override
    public List<String> getPossibleDocstructs() {
        return null;
    }

    @Override
    public DocstructElement getDocstruct() {
        return null;
    }

    @Override
    public void setDocstruct(DocstructElement dse) {
    }

    @Override
    public PluginType getType() {
        return PluginType.Import;
    }

    @Override
    public boolean isRunnableAsGoobiScript() {
        return true;
    }

    /**
     * Loads the configuration for the selected template or the default configuration, if the template was not specified.
     * 
     * The configuration is stored in a {@link Config} object
     * 
     * @param workflowTitle
     * @return
     */

    private Config loadConfig(String workflowTitle) {
        XMLConfiguration xmlConfig = ConfigPlugins.getPluginConfig(title);
        xmlConfig.setExpressionEngine(new XPathExpressionEngine());
        xmlConfig.setReloadingStrategy(new FileChangedReloadingStrategy());

        SubnodeConfiguration myconfig = null;
        try {

            myconfig = xmlConfig.configurationAt("//config[./template = '" + workflowTitle + "']");
        } catch (IllegalArgumentException e) {
            myconfig = xmlConfig.configurationAt("//config[./template = '*']");
        }
        Config config = new Config(myconfig);

        return config;
    }
}
