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

    private String identifierHeaderName;
    private String publicationTypeColumnName;

    /**
     * loads the &lt;config&gt; block from xml file
     * 
     * @param xmlConfig
     */

    public Config(SubnodeConfiguration xmlConfig) {

        publicationType = xmlConfig.getString("/publicationType", "Monograph");
        collection = xmlConfig.getString("/collection");
        identifierColumn = xmlConfig.getInt("/identifierColumn", 1);
        identifierHeaderName = xmlConfig.getString("/identifierHeaderName", null);
        publicationTypeColumnName = xmlConfig.getString("/publicationType", null);

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
