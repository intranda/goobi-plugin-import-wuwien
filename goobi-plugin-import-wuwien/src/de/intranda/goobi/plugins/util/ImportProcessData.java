package de.intranda.goobi.plugins.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ImportProcessData {

    // generated process title, is used to match existing processes
    private String processTitle;

    // metadata docType="anchor"
    private Map<String, String> anchorMetadata = new HashMap<>();

    //docType="volume"
    private Map<String, String> volumeMetadata = new HashMap<>();

    // true if issue number is filled. If this is the case, each volume contains issues and the issue contains the article
    // otherwise the article is added to the volume
    private boolean createIssue = false;

    // contains the list of articles belonging to an issue
    // if no issue exist, "0" is used as number
    private Map<String, List<Map<String, String>>> articleMetadata = new HashMap<>();
}
