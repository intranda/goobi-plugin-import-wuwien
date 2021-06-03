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
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.goobi.beans.Process;
import org.goobi.production.enums.ImportReturnValue;
import org.goobi.production.enums.ImportType;
import org.goobi.production.enums.PluginType;
import org.goobi.production.importer.DocstructElement;
import org.goobi.production.importer.ImportObject;
import org.goobi.production.importer.Record;
import org.goobi.production.plugin.interfaces.IImportPluginVersion2;
import org.goobi.production.plugin.interfaces.IPlugin;
import org.goobi.production.properties.ImportProperty;

import de.intranda.goobi.plugins.util.Config;
import de.intranda.goobi.plugins.util.ImportProcessData;
import de.intranda.goobi.plugins.util.MetadataMappingObject;
import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.forms.MassImportForm;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.UghHelper;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.ImportPluginException;
import de.sub.goobi.helper.exceptions.SwapException;
import de.sub.goobi.persistence.managers.ProcessManager;
import lombok.Data;
import lombok.extern.log4j.Log4j;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.MetadataTypeNotAllowedException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedAsChildException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.UGHException;
import ugh.exceptions.WriteException;
import ugh.fileformats.mets.MetsMods;

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

    private int numberOfNewRecords;
    private int numberOfUpdatedRecords;

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

        /*
         * Suche nach existierenden Vorgängen in Goobi
         * Öffnen der mets datei oder neuen vorgang erstellen
         *  suche nach issue
         * erstellen des neuen issues oder nutzen von existierendem
         * hinzufügen von article
         * 
         */

        for (Record record : records) {
            String processTitle = record.getId();
            ImportProcessData ipd = (ImportProcessData) record.getObject();

            // check if process already exists
            Process process = ProcessManager.getProcessByExactTitle(processTitle);
            if (process == null) {
                //  create new process, create metadata
                ImportObject io = new ImportObject();
                String fileName = getImportFolder() + processTitle + ".xml";

                try {
                    Fileformat ff = new MetsMods(prefs);
                    DigitalDocument digitalDocument = new DigitalDocument();
                    ff.setDigitalDocument(digitalDocument);

                    DocStruct logical = digitalDocument.createDocStruct(prefs.getDocStrctTypeByName(config.getAnchorDocType()));
                    digitalDocument.setLogicalDocStruct(logical);
                    DocStruct volume = digitalDocument.createDocStruct(prefs.getDocStrctTypeByName(config.getVolumeDocType()));
                    logical.addChild(volume);

                    // add all anchor metadata
                    for (String metadata : ipd.getAnchorMetadata().keySet()) {
                        Metadata md = new Metadata(prefs.getMetadataTypeByName(metadata));
                        md.setValue(ipd.getAnchorMetadata().get(metadata));
                        logical.addMetadata(md);
                    }

                    // add all volume metadata
                    for (String metadata : ipd.getVolumeMetadata().keySet()) {
                        Metadata md = new Metadata(prefs.getMetadataTypeByName(metadata));
                        md.setValue(ipd.getVolumeMetadata().get(metadata));
                        volume.addMetadata(md);
                    }
                    String identifier;
                    if (StringUtils.isBlank(ipd.getAnchorMetadata().get("ISSN"))) {
                        identifier = String.valueOf(System.currentTimeMillis());
                    } else {
                        identifier = ipd.getAnchorMetadata().get("ISSN");
                    }
                    // create identifier
                    Metadata anchorIdentifier = new Metadata(prefs.getMetadataTypeByName("CatalogIDDigital"));
                    anchorIdentifier.setValue(identifier);
                    logical.addMetadata(anchorIdentifier);
                    Metadata volumeIdentifier = new Metadata(prefs.getMetadataTypeByName("CatalogIDDigital"));
                    volumeIdentifier.setValue(identifier + "_" + ipd.getVolumeMetadata().get("PublicationYear"));
                    volume.addMetadata(volumeIdentifier);

                    // physical docstruct
                    DocStructType physicalType = prefs.getDocStrctTypeByName("BoundBook");
                    DocStruct physical = digitalDocument.createDocStruct(physicalType);
                    digitalDocument.setPhysicalDocStruct(physical);
                    Metadata imagePath = new Metadata(prefs.getMetadataTypeByName("pathimagefiles"));
                    imagePath.setValue("./images/");
                    physical.addMetadata(imagePath);

                    // create issues and articles
                    if (ipd.isCreateIssue()) {
                        for (String issueNumber : ipd.getArticleMetadata().keySet()) {
                            DocStruct issue = digitalDocument.createDocStruct(prefs.getDocStrctTypeByName(config.getIssueDocType()));
                            volume.addChild(issue);
                            Metadata md = new Metadata(prefs.getMetadataTypeByName("CurrentNo"));
                            md.setValue(issueNumber);
                            issue.addMetadata(md);
                            addArticlesToDocstruct(issue, ipd.getArticleMetadata().get(issueNumber), digitalDocument);
                        }
                    } else {
                        addArticlesToDocstruct(volume, ipd.getArticleMetadata().get("0"), digitalDocument);
                    }

                    io.setMetsFilename(fileName);
                    io.setImportReturnValue(ImportReturnValue.ExportFinished);
                    io.setProcessTitle(processTitle);
                    ff.write(fileName);
                } catch (UGHException | DocStructHasNoTypeException e) {
                    log.error(e);
                    io.setImportReturnValue(ImportReturnValue.InvalidData);
                }

                answer.add(io);

                numberOfNewRecords = numberOfNewRecords + 1;
            } else {
                try {
                    numberOfUpdatedRecords = numberOfUpdatedRecords + 1;
                    prefs = process.getRegelsatz().getPreferences();
                    Fileformat fileformat = process.readMetadataFile();
                    // insert new metadata
                    DocStruct anchor = fileformat.getDigitalDocument().getLogicalDocStruct();

                    DocStruct volume = anchor.getAllChildren().get(0);
                    if (!ipd.isCreateIssue()) {
                        // insert new articles on volume
                        // try to sort them based on value in 'Pages'

                        addArticlesToDocstruct(volume, ipd.getArticleMetadata().get("0"), fileformat.getDigitalDocument());

                    } else {
                        for (Entry<String, List<Map<String, String>>> entry : ipd.getArticleMetadata().entrySet()) {
                            DocStruct matchedIssue = null;
                            String issueNumber = entry.getKey();
                            int positionToInsert = 0;
                            if (StringUtils.isNotBlank(issueNumber) && volume.getAllChildren() != null) {
                                // try to find existing issue
                                for (DocStruct currentIssue : volume.getAllChildren()) {
                                    String issueNumberOfVolume = getMetadataValue(currentIssue, prefs, "CurrentNo");
                                    if (issueNumber.equals(issueNumberOfVolume)) {
                                        matchedIssue = currentIssue;
                                        break;
                                    }
                                }

                                // if this didn't exit, try to find correct position of new issue
                                if (matchedIssue == null) {

                                    int issueNumberOfCurrentIssue = Integer.parseInt(issueNumber.trim());
                                    for (DocStruct currentIssue : volume.getAllChildren()) {
                                        String issueNumberOfVolume = getMetadataValue(currentIssue, prefs, "CurrentNo");

                                        if (StringUtils.isNumeric((issueNumberOfVolume))) {
                                            int numberOfVolume = Integer.parseInt(issueNumberOfVolume.trim());
                                            if (issueNumberOfCurrentIssue < numberOfVolume) {
                                                positionToInsert = positionToInsert + 1;
                                            } else {
                                                break;
                                            }

                                        }
                                    }

                                    // if no existing issue was found, add new issue at the right position
                                    matchedIssue = fileformat.getDigitalDocument().createDocStruct(prefs.getDocStrctTypeByName(config
                                            .getIssueDocType()));
                                    Metadata issueNumberMetadata = new Metadata(prefs.getMetadataTypeByName("CurrentNo"));
                                    issueNumberMetadata.setValue(issueNumber);
                                    matchedIssue.addMetadata(issueNumberMetadata);

                                    List<DocStruct> docStructToMove = new ArrayList<>();

                                    for (int i = 0; i < volume.getAllChildren().size(); i++) {
                                        if (i > positionToInsert) {
                                            DocStruct child = volume.getAllChildren().get(i);
                                            docStructToMove.add(child);
                                            volume.removeChild(child);
                                        }
                                    }
                                    volume.addChild(matchedIssue);
                                    for (DocStruct ds : docStructToMove) {
                                        volume.addChild(ds);
                                    }
                                }
                            } else {
                                volume.addChild(matchedIssue);
                            }
                            addArticlesToDocstruct(matchedIssue, entry.getValue(), fileformat.getDigitalDocument());
                        }
                    }

                    process.writeMetadataFile(fileformat);
                } catch (TypeNotAllowedAsChildException | TypeNotAllowedForParentException | MetadataTypeNotAllowedException | ReadException
                        | PreferencesException | WriteException | IOException | InterruptedException | SwapException | DAOException e) {
                    log.error(e);
                }
            }

        }

        Helper.setMeldung("Created " + numberOfNewRecords + " new process(es) and updated " + numberOfUpdatedRecords + " process(es).");

        return answer;
    }

    /**
     * Add a new articles to the given docstruct. Add all metadata from the list to each new article. If possible, try to find the correct order based
     * on the value of the 'Pages' field
     * 
     * @param docstruct
     * @param values
     * @param digDoc
     */

    private void addArticlesToDocstruct(DocStruct docstruct, List<Map<String, String>> values, DigitalDocument digDoc) {

        for (Map<String, String> articleMetadata : values) {
            try {
                // create docstruct and add metadata
                String pageNumbers = articleMetadata.get("Pages");
                DocStruct article = digDoc.createDocStruct(prefs.getDocStrctTypeByName(config.getArticleDocType()));
                for (String metadataName : articleMetadata.keySet()) {
                    if (metadataName.equals("Author") && StringUtils.isNotBlank(articleMetadata.get(metadataName))) {
                        String[] splittedPersons = articleMetadata.get(metadataName).split(";");
                        for (String personname : splittedPersons) {
                            Person person = new Person(prefs.getMetadataTypeByName(metadataName));
                            if (personname.contains(",")) {
                                String lastname = personname.substring(0, personname.lastIndexOf(",")).trim();
                                String firstname = personname.substring(personname.lastIndexOf(",") + 1).trim();
                                person.setFirstname(firstname);
                                person.setLastname(lastname);
                            } else {
                                person.setLastname(personname);
                            }
                            article.addPerson(person);
                        }
                    } else {
                        Metadata md = new Metadata(prefs.getMetadataTypeByName(metadataName));
                        md.setValue(articleMetadata.get(metadataName));
                        article.addMetadata(md);
                    }
                }

                // find correct position
                pageNumbers = pageNumbers.replaceAll("\\D.*", "");
                if (StringUtils.isNotBlank(pageNumbers) && docstruct.getAllChildren() != null) {
                    int positionToInsert = 0;

                    int startPageNoOfArticleToInsert = Integer.parseInt(pageNumbers);
                    for (DocStruct currentIssue : docstruct.getAllChildren()) {
                        String pageNoOfArticle = getMetadataValue(currentIssue, prefs, "Pages");
                        if (pageNoOfArticle != null && StringUtils.isNotBlank(pageNoOfArticle.replaceAll("\\D.*", ""))) {
                            int startPageNoOfCurrentArticle = Integer.parseInt(pageNumbers.replaceAll("\\D.*", ""));
                            if (startPageNoOfArticleToInsert < startPageNoOfCurrentArticle) {
                                positionToInsert = positionToInsert + 1;
                            } else {
                                break;
                            }
                        }
                    }
                    List<DocStruct> docStructToMove = new ArrayList<>();

                    for (int i = 0; i < docstruct.getAllChildren().size(); i++) {
                        if (i > positionToInsert) {
                            DocStruct child = docstruct.getAllChildren().get(i);
                            docStructToMove.add(child);
                            docstruct.removeChild(child);
                        }
                    }
                    docstruct.addChild(article);
                    for (DocStruct ds : docStructToMove) {
                        docstruct.addChild(ds);
                    }

                } else {
                    // add new article to the end
                    docstruct.addChild(article);
                }

            } catch (TypeNotAllowedAsChildException | MetadataTypeNotAllowedException | TypeNotAllowedForParentException e) {
                log.error(e);
                return;
            }
        }
    }

    /**
     * get metadata value from a docstruct or return null if metadata doesn't exist
     * 
     * @param currentIssue
     * @param prefs
     * @param metadataName
     * @return
     */

    private String getMetadataValue(DocStruct currentIssue, Prefs prefs, String metadataName) {
        List<? extends Metadata> metadataList = currentIssue.getAllMetadataByType(prefs.getMetadataTypeByName(metadataName));
        if (metadataList != null && !metadataList.isEmpty()) {
            return metadataList.get(0).getValue();
        }
        return null;
    }

    public static void main(String[] args) {
        EndnoteExcelImport eei = new EndnoteExcelImport();
        eei.setWorkflowTitle("*");
        eei.getConfig();
        eei.setFile(new File("/home/robert/Downloads/POP_EndNote_Library_Liste_fuer_BIB_zsArt_1.xlsx"));
        List<Record> records = eei.generateRecordsFromFile();
        System.out.println(records.size());
    }

    @Override
    public List<Record> generateRecordsFromFile() {
        numberOfNewRecords = 0;
        numberOfUpdatedRecords = 0;

        Map<String, ImportProcessData> metadataFromExcelfile = new HashMap<>();
        if (StringUtils.isBlank(workflowTitle)) {
            workflowTitle = form.getTemplate().getTitel();
        }

        List<Record> recordList = new ArrayList<>();
        Map<Integer, String> headerOrder = new HashMap<>();

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
                    headerOrder.put(i, value);
                }
            }

            while (rowIterator.hasNext()) {
                Map<String, String> map = new HashMap<>();
                Row row = rowIterator.next();
                int lastColumn = row.getLastCellNum();
                if (lastColumn == -1) {
                    continue;
                }
                for (int cn = 0; cn < lastColumn; cn++) {

                    Cell cell = row.getCell(cn, MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String value = "";
                    switch (cell.getCellType()) {
                        case BOOLEAN:
                            value = cell.getBooleanCellValue() ? "true" : "false";
                            break;
                        case FORMULA:
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
                    map.put(headerOrder.get(cn), value);
                }
                // create process title based on rule
                String rule = config.getProcessTitleRule();
                StringBuilder generatedTitle = new StringBuilder();
                StringTokenizer tokenizer = new StringTokenizer(rule, "+");

                while (tokenizer.hasMoreTokens()) {
                    String namepart = tokenizer.nextToken();
                    /*
                     * wenn der String mit ' anfängt und mit ' endet, dann den Inhalt so übernehmen
                     */
                    if (namepart.startsWith("'") && namepart.endsWith("'")) {
                        generatedTitle.append(namepart.substring(1, namepart.length() - 1));
                    } else {
                        if ("TSL".equals(namepart)) {
                            String title = map.get("Publication Title");
                            String author = null;
                            generatedTitle.append(createAtstsl(title, author));
                        } else if ("ATS".equals(namepart)) {
                            String title = map.get("Publication Title");
                            String author = map.get("Author");
                            generatedTitle.append(createAtstsl(title, author));
                        } else {
                            String metadataValue = map.get(namepart);
                            if (StringUtils.isNotBlank(metadataValue)) {
                                generatedTitle.append(metadataValue);
                            }
                        }
                    }
                }
                String newTitle = generatedTitle.toString();
                if (newTitle.endsWith("_")) {
                    newTitle = newTitle.substring(0, newTitle.length() - 1);
                }
                // remove non-ascii characters for the sake of TIFF header limits
                String regex = ConfigurationHelper.getInstance().getProcessTitleReplacementRegex();

                String filteredTitle = newTitle.replaceAll(regex, "");

                /*
                 * 1. Vorgangstitel bilden
                 * 2. prüfen, ob titel bereits in Liste vorhanden ist
                 * 3. issue + article in liste hinzufügen oder neuer vorgang, issue + article erstellen
                 * 
                 */
                ImportProcessData data = null;
                if (metadataFromExcelfile.containsKey(filteredTitle)) {
                    data = metadataFromExcelfile.get(filteredTitle);
                } else {
                    data = new ImportProcessData();
                    data.setProcessTitle(filteredTitle);
                    metadataFromExcelfile.put(filteredTitle, data);
                    // anchor and volume data
                    for (MetadataMappingObject mmo : config.getMetadataList()) {
                        String metadataValue = map.get(mmo.getHeaderName());
                        if ("anchor".equals(mmo.getDocType())) {
                            data.getAnchorMetadata().put(mmo.getRulesetName(), metadataValue);
                        } else if ("volume".equals(mmo.getDocType())) {
                            data.getVolumeMetadata().put(mmo.getRulesetName(), metadataValue);
                        }
                    }
                }
                List<Map<String, String>> articleData = null;
                if (StringUtils.isBlank(map.get("Issue"))) {
                    data.setCreateIssue(false);
                    if (data.getArticleMetadata().get("0") == null) {
                        articleData = new ArrayList<>();
                        data.getArticleMetadata().put("0", articleData);
                    } else {
                        articleData = data.getArticleMetadata().get("0");
                    }
                } else {
                    data.setCreateIssue(true);
                    if (data.getArticleMetadata().get(map.get("Issue")) == null) {
                        articleData = new ArrayList<>();
                        data.getArticleMetadata().put(map.get("Issue"), articleData);
                    } else {
                        articleData = data.getArticleMetadata().get(map.get("Issue"));
                    }
                }

                Map<String, String> articleMetadata = new HashMap<>();
                for (MetadataMappingObject mmo : config.getMetadataList()) {
                    String metadataValue = map.get(mmo.getHeaderName());
                    if (StringUtils.isBlank(mmo.getDocType()) || "child".equals(mmo.getDocType())) {
                        articleMetadata.put(mmo.getRulesetName(), metadataValue);
                    }
                }
                articleData.add(articleMetadata);

            }
            for (ImportProcessData ipd : metadataFromExcelfile.values()) {
                Record r = new Record();
                r.setId(ipd.getProcessTitle());
                r.setObject(ipd);
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
        return false;
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

    private String createAtstsl(String title, String author) {
        StringBuilder result = new StringBuilder(8);
        if (author != null && author.trim().length() > 0) {
            result.append(author.length() > 4 ? author.substring(0, 4) : author);
            result.append(title.length() > 4 ? title.substring(0, 4) : title);
        } else {
            StringTokenizer titleWords = new StringTokenizer(title);
            int wordNo = 1;
            while (titleWords.hasMoreTokens() && wordNo < 5) {
                String word = titleWords.nextToken();
                switch (wordNo) {
                    case 1:
                        result.append(word.length() > 4 ? word.substring(0, 4) : word);
                        break;
                    case 2:
                    case 3:
                        result.append(word.length() > 2 ? word.substring(0, 2) : word);
                        break;
                    case 4:
                        result.append(word.length() > 1 ? word.substring(0, 1) : word);
                        break;
                }
                wordNo++;
            }
        }
        String res = UghHelper.convertUmlaut(result.toString()).toLowerCase();
        return res.replaceAll("[\\W]", ""); // delete umlauts etc.
    }

    public static Iterable<MatchResult> findRegexMatches(String pattern, CharSequence s) {
        List<MatchResult> results = new ArrayList<>();
        for (Matcher m = Pattern.compile(pattern).matcher(s); m.find();) {
            results.add(m.toMatchResult());
        }
        return results;
    }
}
