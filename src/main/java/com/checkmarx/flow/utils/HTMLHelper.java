package com.checkmarx.flow.utils;

import com.checkmarx.flow.config.FlowProperties;
import com.checkmarx.flow.config.RepoProperties;
import com.checkmarx.flow.constants.SCATicketingConstants;
import com.checkmarx.flow.dto.ScanRequest;
import com.checkmarx.sdk.config.Constants;
import com.checkmarx.sdk.dto.Filter;
import com.checkmarx.sdk.dto.ScanResults;
import com.checkmarx.sdk.dto.ast.SCAResults;
import com.checkmarx.sdk.dto.cx.CxScanSummary;
import com.cx.restclient.ast.dto.sca.report.Finding;
import com.cx.restclient.ast.dto.sca.report.Package;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByKey;


@Slf4j
public class HTMLHelper {

    public static final String MD_H3 = "###";
    public static final String MD_H4 = "####";
    public static final String VERSION = "Version: ";
    public static final String DESCRIPTION = "Description: ";
    public static final String RECOMMENDATION = "Recommendation: ";
    public static final String RECOMMENDED_FIX = "recommendedFix";
    public static final String URL = "URL: ";
    public static final String DETAILS = "Details - ";
    public static final String SEVERITY = "Severity: ";
    public static final String DIV_CLOSING_TAG = "</div>";
    public static final String CRLF = "\r\n";
    public static final String WEB_HOOK_PAYLOAD = "web-hook-payload";
    private static final String ITALIC_OPENING_DIV = "<div><i>";
    private static final String ITALIC_CLOSING_DIV = "</i></div>";
    private static final String LINE_BREAK = "<br>";
    private static final String NVD_URL_PREFIX = "https://nvd.nist.gov/vuln/detail/";

    public static final String ISSUE_BODY = "**%s** issue exists @ **%s** in branch **%s**";
    public static final String ISSUE_BODY_TEXT = "%s issue exists @ %s in branch %s";

    private static final String DIV_A_HREF = "<div><a href='";
    
    private HTMLHelper(){}
    
    public static String getMergeCommentMD(ScanRequest request, ScanResults results, 
                                           RepoProperties properties) {
        StringBuilder body = new StringBuilder();

        if (results.isSastRestuls() || results.isAstResults()) {
            log.debug("Building merge comment MD for SAST scanner");

            if(results.isAstResults()){
                ScanUtils.setASTXIssuesInScanResults(results);
            }
            
            addScanSummarySection(request, results, properties, body);
            addFlowSummarySection(results, properties, body);
            addDetailsSection(request, results, properties, body);
        }

        addScaBody(results, body);
        return body.toString();
    }

    /**
     * = Generates an HTML message describing the discovered issue.
     *
     * @param issue The issue to add the comment too
     * @return string with the HTML message
     */
    public static String getHTMLBody(ScanResults.XIssue issue, ScanRequest request, FlowProperties flowProperties) {
        String branch = request.getBranch();
        StringBuilder body = new StringBuilder();
        body.append("<div>");

        if (Optional.ofNullable(issue.getScaDetails()).isPresent()) {
            setSCAHtmlBody(issue, request, body);

        } else {
            setSASTHtmlBody(issue, flowProperties, branch, body);
        }
        body.append(DIV_CLOSING_TAG);
        return body.toString();
    }
    
    private static void addFlowSummarySection(ScanResults results, RepoProperties properties, StringBuilder body) {
        if (properties.isFlowSummary()) {
            if (!ScanUtils.empty(properties.getFlowSummaryHeader())) {
                body.append(MD_H4).append(" ").append(properties.getFlowSummaryHeader()).append(CRLF);
            }
            body.append("Severity|Count").append(CRLF);
            body.append("---|---").append(CRLF);
            Map<String, Integer> flow = (Map<String, Integer>) results.getAdditionalDetails().get(Constants.SUMMARY_KEY);
            if (flow != null) {
                for (Map.Entry<String, Integer> severity : flow.entrySet()) {
                    body.append(severity.getKey()).append("|").append(severity.getValue().toString()).append(CRLF);
                }
            }
            body.append(CRLF);
        }
    }

    private static void addScaBody(ScanResults results, StringBuilder body) {

        Optional.ofNullable(results.getScaResults()).ifPresent(r -> {
            log.debug("Building merge comment MD for SCA scanner");
            if (body.length() > 0) {
                body.append("***").append(CRLF);
            }

            body.append(MD_H3).append(" Checkmarx Dependency (CxSCA) Scan Summary").append(CRLF)
                    .append("[Full Scan Details](").append(r.getWebReportLink()).append(")  ").append(CRLF)
                    .append(MD_H4).append(" Summary  ").append(CRLF)
                    .append("| Total Packages Identified | ").append(r.getSummary().getTotalPackages()).append("| ").append(CRLF)
                    .append("-|-").append(CRLF);

            Arrays.asList("High", "Medium", "Low").forEach(v ->
                    body.append(v).append(" severity vulnerabilities | ")
                            .append(r.getSummary().getFindingCounts().get(Filter.Severity.valueOf(v.toUpperCase()))).append(" ").append(CRLF));
            body.append("Scan risk score | ").append(String.format("%.2f", r.getSummary().getRiskScore())).append(" |").append(CRLF).append(CRLF);

            body.append(MD_H4).append(" CxSCA vulnerability result overview").append(CRLF);
            List<String> headlines = Arrays.asList(
                    "Vulnerability ID",
                    "Package",
                    "Severity",
//                    "CWE / Category",
                    "CVSS score",
                    "Publish date",
                    "Current version",
                    "Recommended version",
                    "Link in CxSCA",
                    "Reference – NVD link"
            );
            headlines.forEach(h -> body.append("| ").append(h));
            body.append("|").append(CRLF);

            headlines.forEach(h -> body.append("|-"));
            body.append("|").append(CRLF);

            r.getFindings().stream()
                    .sorted(Comparator.comparingDouble(o -> -o.getScore()))
                    .sorted(Comparator.comparingInt(o -> -o.getSeverity().ordinal()))
                    .forEach(f -> {

                        Arrays.asList(
                                '`'+f.getId()+'`',
                                extractPackageNameFromFindings(r, f),
                                f.getSeverity().name(),
//                                "N\\A",
                                f.getScore(),
                                f.getPublishDate(),
                                extractPackageVersionFromFindings(r, f),
                                Optional.ofNullable(f.getRecommendations()).orElse(""),
                                " [Vulnerability Link](" + ScanUtils.constructVulnerabilityUrl(r.getWebReportLink(), f) + ") | "
                        ).forEach(v -> body.append("| ").append(v));

                        if (!StringUtils.isEmpty(f.getCveName())) {
                            body.append("[").append(f.getCveName()).append("](https://nvd.nist.gov/vuln/detail/").append(f.getCveName()).append(")");
                        } else {
                            body.append("N\\A");
                        }
                        body.append("|" + CRLF);
                    });

        });
    }


    public static String getMDBody(ScanResults.XIssue issue, String branch, String fileUrl, FlowProperties flowProperties) {
        StringBuilder body = new StringBuilder();

        List<ScanResults.ScaDetails> scaDetails = issue.getScaDetails();
        if (!ScanUtils.empty(scaDetails)) {
            setSCAMDBody(branch, body, scaDetails);

        } else {
            setSASTMDBody(issue, branch, fileUrl, flowProperties, body);
        }

        return body.toString();
    }

    
    private static String extractPackageNameFromFindings(SCAResults r, Finding f) {
        return r.getPackages().stream().filter(p -> p.getId().equals(f.getPackageId())).map(Package::getName).findFirst().orElse("");
    }

    private static String extractPackageVersionFromFindings(SCAResults r, Finding f) {
        return r.getPackages().stream().filter(p -> p.getId().equals(f.getPackageId())).map(Package::getVersion).findFirst().orElse("");
    }
    
    private static void setSCAMDBody(String branch, StringBuilder body, List<ScanResults.ScaDetails> scaDetails) {
        log.debug("Building MD body for SCA scanner");
        scaDetails.stream().findAny().ifPresent(any -> {
            body.append("**Description**").append(CRLF).append(CRLF);
            body.append(any.getFinding().getDescription()).append(CRLF).append(CRLF);
            body.append(String.format(SCATicketingConstants.SCA_CUSTOM_ISSUE_BODY, any.getFinding().getSeverity(),
                    any.getVulnerabilityPackage().getName(), branch)).append(CRLF).append(CRLF);

            Map<String, String> scaDetailsMap = new LinkedHashMap<>();
            scaDetailsMap.put("**Vulnerability ID", any.getFinding().getId());
            scaDetailsMap.put("**Package Name", any.getVulnerabilityPackage().getName());
            scaDetailsMap.put("**Severity", any.getFinding().getSeverity().name());
            scaDetailsMap.put("**CVSS Score", String.valueOf(any.getFinding().getScore()));
            scaDetailsMap.put("**Publish Date", any.getFinding().getPublishDate());
            scaDetailsMap.put("**Current Package Version", any.getVulnerabilityPackage().getVersion());
            Optional.ofNullable(any.getFinding().getFixResolutionText()).ifPresent(f ->
                    scaDetailsMap.put("**Remediation Upgrade Recommendation", f)

            );

            scaDetailsMap.forEach((key, value) ->
                    body.append(key).append(":** ").append(value).append(CRLF).append(CRLF)
            );
            String findingLink = ScanUtils.constructVulnerabilityUrl(any.getVulnerabilityLink(), any.getFinding());
            body.append("[Link To SCA](").append(findingLink).append(")").append(CRLF).append(CRLF);

            String cveName = any.getFinding().getCveName();
            if (!ScanUtils.empty(cveName)) {
                body.append("[Reference – NVD link](").append(NVD_URL_PREFIX).append(cveName).append(")").append(CRLF).append(CRLF);
            }
        });
    }


    private static void setSASTHtmlBody(ScanResults.XIssue issue, FlowProperties flowProperties, String branch, StringBuilder body) {
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF);

        if(!ScanUtils.empty(issue.getDescription())) {
            body.append(ITALIC_OPENING_DIV).append(issue.getDescription().trim()).append(ITALIC_CLOSING_DIV);
        }
        body.append(CRLF);
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append("<div><b>Severity:</b> ").append(issue.getSeverity()).append(DIV_CLOSING_TAG);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("<div><b>CWE:</b>").append(issue.getCwe()).append(DIV_CLOSING_TAG);
            if(!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append(DIV_A_HREF).append(
                        String.format(
                                flowProperties.getMitreUrl(),
                                issue.getCwe()
                        )
                ).append("\'>Vulnerability details and guidance</a></div>");
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append(DIV_A_HREF).append(flowProperties.getWikiUrl()).append("\'>Internal Guidance</a></div>");
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append(DIV_A_HREF).append(issue.getLink()).append("\'>Checkmarx</a></div>");
        }
        Map<String, Object> additionalDetails = issue.getAdditionalDetails();
        if (MapUtils.isNotEmpty(additionalDetails) && additionalDetails.containsKey(ScanUtils.RECOMMENDED_FIX)) {
            body.append(DIV_A_HREF).append(additionalDetails.get(ScanUtils.RECOMMENDED_FIX)).append("\'>Recommended Fix</a></div>");
        }

        appendsSastAstDetails(issue, flowProperties, body);
        appendOsaDetailsHTML(issue, body);
    }

    private static void appendsSastAstDetails(ScanResults.XIssue issue, FlowProperties flowProperties, StringBuilder body) {
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            
            appendLinesHTML(body, trueIssues);
            appendNotExploitableHTML(flowProperties, body, fpIssues);
            appendCodeSnippetHTML(body, trueIssues);
            body.append("<hr/>");
        }
    }

    private static void appendOsaDetailsHTML(ScanResults.XIssue issue, StringBuilder body) {
        if(issue.getOsaDetails()!=null){
            for(ScanResults.OsaDetails o: issue.getOsaDetails()){
                body.append(CRLF);
                if(!ScanUtils.empty(o.getCve())) {
                    body.append("<b>").append(o.getCve()).append("</b>").append(CRLF);
                }
                body.append("<pre><code><div>");
                appendOsaDetails(body, o);
                body.append("</div></code></pre><div>");
                body.append(CRLF);
            }
        }
    }

    private static void appendCodeSnippetHTML(StringBuilder body, Map<Integer, ScanResults.IssueDetails> trueIssues) {
        for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
            if (!ScanUtils.empty(entry.getValue().getCodeSnippet())) {
                body.append("<hr/>");
                body.append("<b>Line #").append(entry.getKey()).append("</b>");
                body.append("<pre><code><div>");
                String codeSnippet = entry.getValue().getCodeSnippet();
                body.append(StringEscapeUtils.escapeHtml4(codeSnippet));
                body.append("</div></code></pre><div>");
            }
        }
    }
    private static void appendLinesHTML(StringBuilder body, Map<Integer, ScanResults.IssueDetails> trueIssues) {
        if(!trueIssues.isEmpty()) {
            body.append("<div><b>Lines: </b>");
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                body.append(entry.getKey()).append(" ");
            }
            body.append(DIV_CLOSING_TAG);
        }
    }

    private static void appendNotExploitableHTML(FlowProperties flowProperties, StringBuilder body, Map<Integer, ScanResults.IssueDetails> fpIssues) {
        if(flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {//List the false positives / not exploitable
            body.append("<div><b>Lines Marked Not Exploitable: </b>");
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                body.append(entry.getKey()).append(" ");
            }
            body.append(DIV_CLOSING_TAG);
        }
    }

    

    private static void setSCAHtmlBody(ScanResults.XIssue issue, ScanRequest request, StringBuilder body) {
        log.debug("Building HTML body for SCA scanner");
        issue.getScaDetails().stream().findAny().ifPresent(any -> {
            body.append(ITALIC_OPENING_DIV).append(any.getFinding().getDescription())
                    .append(ITALIC_CLOSING_DIV).append(LINE_BREAK);
            body.append(String.format(SCATicketingConstants.SCA_HTML_ISSUE_BODY, any.getFinding().getSeverity(),
                    any.getVulnerabilityPackage().getName(), request.getBranch()))
                    .append(DIV_CLOSING_TAG).append(LINE_BREAK);
        });

        Map<String, String> scaDetailsMap = new LinkedHashMap<>();
        issue.getScaDetails().stream().findAny().ifPresent(any -> {
            scaDetailsMap.put("<b>Vulnerability ID", any.getFinding().getId());
            scaDetailsMap.put("<b>Package Name", any.getVulnerabilityPackage().getName());
            scaDetailsMap.put("<b>Severity", any.getFinding().getSeverity().name());
            scaDetailsMap.put("<b>CVSS Score", String.valueOf(any.getFinding().getScore()));
            scaDetailsMap.put("<b>Publish Date", any.getFinding().getPublishDate());
            scaDetailsMap.put("<b>Current Package Version", any.getVulnerabilityPackage().getVersion());
            Optional.ofNullable(any.getFinding().getFixResolutionText()).ifPresent(f ->
                    scaDetailsMap.put("<b>Remediation Upgrade Recommendation", f)

            );

            scaDetailsMap.forEach((key, value) ->
                    body.append(key).append(":</b> ").append(value).append(LINE_BREAK)
            );

            String findingLink = ScanUtils.constructVulnerabilityUrl(any.getVulnerabilityLink(), any.getFinding());
            body.append(DIV_A_HREF).append(findingLink).append("\'>Link To SCA</a></div>");

            String cveName = any.getFinding().getCveName();
            if (!ScanUtils.empty(cveName)) {
                body.append(DIV_A_HREF).append(NVD_URL_PREFIX).append(cveName).append("\'>Reference – NVD link</a></div>");
            }
        });
    }


    private static void setSASTMDBody(ScanResults.XIssue issue, String branch, String fileUrl, FlowProperties flowProperties, StringBuilder body) {
        log.debug("Building MD body for SAST scanner");
        body.append(String.format(ISSUE_BODY, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF).append(CRLF);
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append("*").append(issue.getDescription().trim()).append("*").append(CRLF).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append(SEVERITY).append(issue.getSeverity()).append(CRLF).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("CWE:").append(issue.getCwe()).append(CRLF).append(CRLF);
            if(!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append("[Vulnerability details and guidance](").append(String.format(flowProperties.getMitreUrl(), issue.getCwe())).append(")").append(CRLF).append(CRLF);
            }
        }
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append("[Internal Guidance](").append(flowProperties.getWikiUrl()).append(")").append(CRLF).append(CRLF);
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append("[Checkmarx](").append(issue.getLink()).append(")").append(CRLF).append(CRLF);
        }
        Map<String, Object> additionalDetails = issue.getAdditionalDetails();
        if (MapUtils.isNotEmpty(additionalDetails) && additionalDetails.containsKey(RECOMMENDED_FIX)) {
            body.append("[Recommended Fix](").append(additionalDetails.get(ScanUtils.RECOMMENDED_FIX)).append(")").append(CRLF).append(CRLF);
        }

        appendSastAstDetails(issue, fileUrl, flowProperties, body);
        appendOsaDetails(issue, body);
    }

    private static void appendSastAstDetails(ScanResults.XIssue issue, String fileUrl, FlowProperties flowProperties, StringBuilder body) {
        if (issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .sorted(comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey() != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .sorted(comparingByKey())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            
            appendLines(fileUrl, body, trueIssues);
            appendNotExploitable(fileUrl, flowProperties, body, fpIssues);
            appendCodeSnippet(fileUrl, body, trueIssues);
            body.append("---").append(CRLF);
        }
    }

    private static void appendLines(String fileUrl, StringBuilder body, Map<Integer, ScanResults.IssueDetails> trueIssues) {
        if (!trueIssues.isEmpty()) {
            body.append("Lines: ");
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                if (fileUrl != null) {  //[<line>](<url>)
                    body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#L").append(entry.getKey()).append(") ");
                } else { //if the fileUrl is not provided, simply putting the line number (no link) - ADO for example
                    body.append(entry.getKey()).append(" ");
                }
            }
            body.append(CRLF).append(CRLF);
        }
    }

    private static void appendCodeSnippet(String fileUrl, StringBuilder body, Map<Integer, ScanResults.IssueDetails> trueIssues) {
        for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
            if (entry.getValue() != null && entry.getValue().getCodeSnippet() != null) {
                body.append("---").append(CRLF);
                body.append("[Code (Line #").append(entry.getKey()).append("):](").append(fileUrl).append("#L").append(entry.getKey()).append(")").append(CRLF);
                body.append("```").append(CRLF);
                body.append(entry.getValue().getCodeSnippet()).append(CRLF);
                body.append("```").append(CRLF);
            }
        }
    }

    private static void appendNotExploitable(String fileUrl, FlowProperties flowProperties, StringBuilder body, Map<Integer, ScanResults.IssueDetails> fpIssues) {
        if (flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {//List the false positives / not exploitable
            body.append(CRLF);
            body.append("Lines Marked Not Exploitable: ");
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                if (fileUrl != null) {  //[<line>](<url>)
                    body.append("[").append(entry.getKey()).append("](").append(fileUrl).append("#L").append(entry.getKey()).append(") ");
                } else { //if the fileUrl is not provided, simply putting the line number (no link) - ADO for example
                    body.append(entry.getKey()).append(" ");
                }
            }
            body.append(CRLF).append(CRLF);
        }
    }

    private static void appendOsaDetails(ScanResults.XIssue issue, StringBuilder body) {
        if(issue.getOsaDetails()!=null){
            for(ScanResults.OsaDetails o: issue.getOsaDetails()){
                body.append(CRLF);
                if(!ScanUtils.empty(o.getCve())) {
                    body.append("*").append(o.getCve()).append("*").append(CRLF);
                }
                body.append("```");
                appendOsaDetails(body, o);
                body.append("```");
                body.append(CRLF);
            }
        }
    }


    /**
     * = Generates an Text message describing the discovered issue.
     *
     * @param issue The issue to add the comment too
     * @return string with the HTML message
     */
    public static String getTextBody(ScanResults.XIssue issue, ScanRequest request, FlowProperties flowProperties) {
        String branch = request.getBranch();
        StringBuilder body = new StringBuilder();
        body.append(String.format(ISSUE_BODY_TEXT, issue.getVulnerability(), issue.getFilename(), branch)).append(CRLF);
        if(!ScanUtils.empty(issue.getDescription())) {
            body.append(issue.getDescription().trim());
        }
        body.append(CRLF);
        if(!ScanUtils.empty(issue.getSeverity())) {
            body.append(SEVERITY).append(issue.getSeverity()).append(CRLF);
        }
        appendCWE(issue, flowProperties, body);
        
        if(!ScanUtils.empty(flowProperties.getWikiUrl())) {
            body.append(DETAILS).append(flowProperties.getWikiUrl()).append(" - Internal Guidance ").append(CRLF);
        }
        if(!ScanUtils.empty(issue.getLink())){
            body.append(DETAILS).append(issue.getLink()).append(" - Checkmarx").append(CRLF);
        }
        Map<String, Object> additionalDetails = issue.getAdditionalDetails();
        if (MapUtils.isNotEmpty(additionalDetails) && additionalDetails.containsKey(ScanUtils.RECOMMENDED_FIX)) {
            body.append(DETAILS).append(additionalDetails.get(ScanUtils.RECOMMENDED_FIX)).append(" - Recommended Fix").append(CRLF);
        }

        appendSastAstDetials(issue, flowProperties, body);
        
        if(issue.getOsaDetails()!=null){
            for(ScanResults.OsaDetails o: issue.getOsaDetails()){
                body.append(CRLF);
                if(!ScanUtils.empty(o.getCve())) {
                    body.append(o.getCve()).append(CRLF);
                }
                appendOsaDetails(body, o);
                body.append(CRLF);
            }
        }
        return body.toString();
    }

    private static void appendCWE(ScanResults.XIssue issue, FlowProperties flowProperties, StringBuilder body) {
        if(!ScanUtils.empty(issue.getCwe())) {
            body.append("CWE: ").append(issue.getCwe()).append(CRLF);
            if(!ScanUtils.empty(flowProperties.getMitreUrl())) {
                body.append(DETAILS)
                        .append(
                                String.format(
                                        flowProperties.getMitreUrl(),
                                        issue.getCwe()
                                )
                        ).append(" - Vulnerability details and guidance").append(CRLF);
            }
        }
    }

    private static void appendSastAstDetials(ScanResults.XIssue issue, FlowProperties flowProperties, StringBuilder body) {
        if(issue.getDetails() != null && !issue.getDetails().isEmpty()) {
            Map<Integer, ScanResults.IssueDetails> trueIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && !x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            Map<Integer, ScanResults.IssueDetails> fpIssues = issue.getDetails().entrySet().stream()
                    .filter(x -> x.getKey( ) != null && x.getValue() != null && x.getValue().isFalsePositive())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            if(!trueIssues.isEmpty()) {
                body.append("Lines: ");
                for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                    body.append(entry.getKey()).append(" ");
                }
            }
            if(flowProperties.isListFalsePositives() && !fpIssues.isEmpty()) {//List the false positives / not exploitable
                body.append("Lines Marked Not Exploitable: ");
                for (Map.Entry<Integer, ScanResults.IssueDetails> entry : fpIssues.entrySet()) {
                    body.append(entry.getKey()).append(" ");
                }
            }
            for (Map.Entry<Integer, ScanResults.IssueDetails> entry : trueIssues.entrySet()) {
                if (!ScanUtils.empty(entry.getValue().getCodeSnippet())) {
                    body.append("Line # ").append(entry.getKey());
                    String codeSnippet = entry.getValue().getCodeSnippet();
                    body.append(StringEscapeUtils.escapeHtml4(codeSnippet)).append(CRLF);
                }
            }
        }
    }


    private static void appendOsaDetails(StringBuilder body, ScanResults.OsaDetails o) {
        if (!ScanUtils.empty(o.getSeverity())) {
            body.append(SEVERITY).append(o.getSeverity()).append(CRLF);
        }
        if (!ScanUtils.empty(o.getVersion())) {
            body.append(VERSION).append(o.getVersion()).append(CRLF);
        }
        if (!ScanUtils.empty(o.getDescription())) {
            body.append(DESCRIPTION).append(o.getDescription()).append(CRLF);
        }
        if (!ScanUtils.empty(o.getRecommendation())) {
            body.append(RECOMMENDATION).append(o.getRecommendation()).append(CRLF);
        }
        if (!ScanUtils.empty(o.getUrl())) {
            body.append(URL).append(o.getUrl());
        }
    }
    
    private static void addSastAstDetailsBody(ScanRequest request, StringBuilder body, Map<String, ScanResults.XIssue> xMap, Comparator<ScanResults.XIssue> issueComparator) {
        xMap.entrySet().stream()
                .filter(x -> x.getValue() != null && x.getValue().getDetails() != null)
                .sorted(Map.Entry.comparingByValue(issueComparator))
                .forEach(xIssue -> {
                    ScanResults.XIssue currentIssue = xIssue.getValue();
                    String fileUrl = ScanUtils.getFileUrl(request, currentIssue.getFilename());
                    currentIssue.getDetails().entrySet().stream()
                            .filter(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())
                            .sorted(Map.Entry.comparingByKey())
                            .forEach(entry -> {
                                //[<line>](<url>)
                                //Azure DevOps direct repo line url is unknown at this time.
                                if (request.getRepoType().equals(ScanRequest.Repository.ADO)) {
                                    body.append(entry.getKey()).append(" ");
                                } else {
                                    body.append("[").append(entry.getKey()).append("](").append(fileUrl);
                                    if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKET)) {
                                        body.append("#lines-").append(entry.getKey()).append(") ");
                                    } else if (request.getRepoType().equals(ScanRequest.Repository.BITBUCKETSERVER)) {
                                        body.append("#").append(entry.getKey()).append(") ");
                                    } else {
                                        body.append("#L").append(entry.getKey()).append(") ");
                                    }
                                }
                            });
                    if (currentIssue.getDetails().entrySet().stream().anyMatch(x -> x.getKey() != null && x.getValue() != null && !x.getValue().isFalsePositive())) {
                        body.append("|");
                        body.append(currentIssue.getSeverity()).append("|");
                        body.append(currentIssue.getVulnerability()).append("|");
                        body.append(currentIssue.getFilename()).append("|");
                        body.append("[Checkmarx](").append(currentIssue.getLink()).append(")");
                        body.append(CRLF);
                    }
                });
    }

    private static void addOsaDetailesBody(ScanResults results, StringBuilder body, Map<String, ScanResults.XIssue> xMap, Comparator<ScanResults.XIssue> issueComparator) {
        if (results.getOsa() != null && results.getOsa()) {
            log.debug("Building merge comment MD for OSA scanner");
            body.append(CRLF);
            body.append("|Library|Severity|CVE|").append(CRLF);
            body.append("---|---|---").append(CRLF);

            //OSA
            xMap.entrySet().stream()
                    .filter(x -> x.getValue() != null && x.getValue().getOsaDetails() != null)
                    .sorted(Map.Entry.comparingByValue(issueComparator))
                    .forEach(xIssue -> {
                        ScanResults.XIssue currentIssue = xIssue.getValue();
                        body.append("|");
                        body.append(currentIssue.getFilename()).append("|");
                        body.append(currentIssue.getSeverity()).append("|");
                        for (ScanResults.OsaDetails o : currentIssue.getOsaDetails()) {
                            body.append("[").append(o.getCve()).append("](")
                                    .append("https://cve.mitre.org/cgi-bin/cvename.cgi?name=").append(o.getCve()).append(") ");
                        }
                        body.append("|");
                        body.append(CRLF);
                        //body.append("```").append(currentIssue.getDescription()).append("```").append(CRLF); Description is too long
                    });
        }
    }

    
    private static void addScanSummarySection(ScanRequest request, ScanResults results, RepoProperties properties, StringBuilder body) {
        CxScanSummary summary = results.getScanSummary();
        body.append(MD_H3).append(" Checkmarx SAST Scan Summary").append(CRLF);
        body.append("[Full Scan Details](").append(results.getLink()).append(")").append(CRLF);
        if (properties.isCxSummary() && !request.getProduct().equals(ScanRequest.Product.CXOSA)) {
            if (!ScanUtils.empty(properties.getCxSummaryHeader())) {
                body.append(MD_H4).append(" ").append(properties.getCxSummaryHeader()).append(CRLF);
            }
            body.append("Severity|Count").append(CRLF);
            body.append("---|---").append(CRLF);
            body.append("High|").append(summary.getHighSeverity().toString()).append(CRLF);
            body.append("Medium|").append(summary.getMediumSeverity().toString()).append(CRLF);
            body.append("Low|").append(summary.getLowSeverity().toString()).append(CRLF);
            body.append("Informational|").append(summary.getInfoSeverity().toString()).append(CRLF).append(CRLF);
        }
    }

    private static void addDetailsSection(ScanRequest request, ScanResults results, RepoProperties properties, StringBuilder body) {
        if (properties.isDetailed()) {
            if (!ScanUtils.empty(properties.getDetailHeader())) {
                body.append(MD_H4).append(" ").append(properties.getDetailHeader()).append(CRLF);
            }
            body.append("|Lines|Severity|Category|File|Link|").append(CRLF);
            body.append("---|---|---|---|---").append(CRLF);

            Map<String, ScanResults.XIssue> xMap;
            xMap = ScanUtils.getXIssueMap(results.getXIssues(), request);
            log.info("Creating Merge/Pull Request Markdown comment");

            Comparator<ScanResults.XIssue> issueComparator = Comparator
                    .comparing(ScanResults.XIssue::getSeverity)
                    .thenComparing(ScanResults.XIssue::getVulnerability);

            addSastAstDetailsBody(request, body, xMap, issueComparator);

            addOsaDetailesBody(results, body, xMap, issueComparator);
        }
    }
}
