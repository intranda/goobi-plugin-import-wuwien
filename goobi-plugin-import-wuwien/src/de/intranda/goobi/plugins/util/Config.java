package de.intranda.goobi.plugins.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;

import lombok.Data;

@Data
public class Config {

    private String publicationType;
    private String collection;
    private int identifierColumn;
    private List<MetadataMappingObject> metadataList = new ArrayList<>();

    private String publicationTypeColumnName;

    private String processTitleRule;

    private String anchorDocType;
    private String volumeDocType;
    private String issueDocType;
    private String articleDocType;

    /**
     * loads the &lt;config&gt; block from xml file
     * 
     * @param xmlConfig
     */

    public Config(SubnodeConfiguration xmlConfig) {

        publicationType = xmlConfig.getString("/publicationType", "Monograph");
        collection = xmlConfig.getString("/collection");
        identifierColumn = xmlConfig.getInt("/identifierColumn", 1);
        publicationTypeColumnName = xmlConfig.getString("/publicationType", null);

        processTitleRule = xmlConfig.getString("/processTitleGeneration", null);

        anchorDocType = xmlConfig.getString("/anchorDocType", "Periodical");
        volumeDocType = xmlConfig.getString("/volumeDocType", "PeriodicalVolume");
        issueDocType = xmlConfig.getString("/issueDocType", "Issue");
        articleDocType  = xmlConfig.getString("/articleDocType", "Article");

        @SuppressWarnings("unchecked")
        List<HierarchicalConfiguration> mml = xmlConfig.configurationsAt("//metadata");
        for (HierarchicalConfiguration md : mml) {
            metadataList.add(getMetadata(md));
        }

    }

    private MetadataMappingObject getMetadata(HierarchicalConfiguration md) {
        String rulesetName = md.getString("@ugh");
        String propertyName = md.getString("@name");
        Integer columnNumber = md.getInteger("@column", null);
        Integer identifierColumn = md.getInteger("@identifier", null);
        String headerName = md.getString("@headerName", null);
        String normdataHeaderName = md.getString("@normdataHeaderName", null);
        String docType = md.getString("@docType", "child");

        MetadataMappingObject mmo = new MetadataMappingObject();
        mmo.setExcelColumn(columnNumber);
        mmo.setIdentifierColumn(identifierColumn);
        mmo.setPropertyName(propertyName);
        mmo.setRulesetName(rulesetName);
        mmo.setHeaderName(headerName);
        mmo.setNormdataHeaderName(normdataHeaderName);
        mmo.setDocType(docType);
        return mmo;
    }

}
