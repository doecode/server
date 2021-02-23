/*
 */
package gov.osti.doi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import gov.osti.entity.Award;
import gov.osti.entity.Contributor;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.Developer;
import gov.osti.entity.RelatedIdentifier;
import gov.osti.entity.SponsoringOrganization;
import gov.osti.listeners.DoeServletContextListener;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.TimeZone;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.HttpHeaders;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

/**
 * Interface for DataCite registration of software DOI values.
 * 
 * @author ensornl
 */
public class DataCite {
    // logger
    private static Logger log = LoggerFactory.getLogger(DataCite.class);
    // DataCite API base request URL
    private static final String DATACITE_URL = DoeServletContextListener.getConfigurationProperty("datacite.url");
    private static String DATACITE_LOGIN = DoeServletContextListener.getConfigurationProperty("datacite.user");
    private static String DATACITE_PASSWORD = DoeServletContextListener.getConfigurationProperty("datacite.password");
    private static String DATACITE_BASE_URL = DoeServletContextListener.getConfigurationProperty("datacite.baseurl");
    private static String DATACITE_PREFIX = DoeServletContextListener.getConfigurationProperty("datacite.prefix");

    // Jackson object mapper
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setTimeZone(TimeZone.getDefault());
    
    /**
     * Convert a List of Developers into something DataCite can understand.
     * 
     * @param sw the XMLStreamWriter to write to
     * @param developers a List of Developer objects
     * @throws XMLStreamException on XML output errors
     */
    private static void writeDevelopers(XMLStreamWriter sw, List<Developer> developers) throws XMLStreamException {
        sw.writeStartElement("creators");
        
        for ( Developer developer : developers ) {
            String given_name = (StringUtils.isEmpty(developer.getFirstName())) ? "" : developer.getFirstName(),
                   family_name = (StringUtils.isEmpty(developer.getLastName())) ? "" : developer.getLastName();
            sw.writeStartElement("creator");
            
            sw.writeStartElement("creatorName");
            sw.writeCharacters(family_name + ", " + given_name);
            sw.writeEndElement();
            
            sw.writeStartElement("givenName");
            sw.writeCharacters(given_name);
            sw.writeEndElement();
            
            sw.writeStartElement("familyName");
            sw.writeCharacters(family_name);
            sw.writeEndElement();
            
            if (StringUtils.isNotEmpty(developer.getOrcid())) {
                sw.writeStartElement("nameIdentifier");
                sw.writeAttribute("schemeURI", "http://orcid.org/");
                sw.writeAttribute("nameIdentifierScheme", "ORCID");
                sw.writeCharacters(developer.getOrcid());
                sw.writeEndElement();
            }
            List<String> affiliations = developer.getAffiliations();
            if ( null!=affiliations && !affiliations.isEmpty() ) {
                for ( String affiliation : affiliations ) {
                    if (!StringUtils.isEmpty(affiliation)) {
                        sw.writeStartElement("affiliation");
                        sw.writeCharacters(affiliation);
                        sw.writeEndElement();
                    }
                }
            }
            
            sw.writeEndElement();
        }
        
        sw.writeEndElement();
    }
    
    /**
     * Write a List of Contributors to DataCite format.
     * 
     * @param sw the XMLStreamWriter to write on
     * @param contributors a List of Contributors (possibly empty)
     * @throws XMLStreamException on XML output errors
     */
    private static void writeContributors(XMLStreamWriter sw, List<Contributor> contributors) throws XMLStreamException {
        if (null==contributors || contributors.isEmpty())
            return;
        
        sw.writeStartElement("contributors");
        
        for ( Contributor contributor : contributors ) {
            String given_name = (StringUtils.isEmpty(contributor.getFirstName())) ? "" : contributor.getFirstName(),
                   family_name = (StringUtils.isEmpty(contributor.getLastName())) ? "" : contributor.getLastName();
            sw.writeStartElement("contributor");
            sw.writeAttribute("contributorType", contributor.getContributorType().name());
            
            sw.writeStartElement("contributorName");
            sw.writeCharacters(family_name + ", " + given_name);
            sw.writeEndElement();
            
            if (!StringUtils.isEmpty(contributor.getOrcid())) {
                sw.writeStartElement("nameIdentifier");
                sw.writeAttribute("schemeURI", "http://orcid.org/");
                sw.writeAttribute("nameIdentifierScheme", "ORCID");
                sw.writeCharacters(contributor.getOrcid());
                sw.writeEndElement();
            }
            List<String> affiliations = contributor.getAffiliations();
            if ( null!=affiliations && !affiliations.isEmpty() ) {
                for ( String affiliation : affiliations ) {
                    if (!StringUtils.isEmpty(affiliation)) {
                        sw.writeStartElement("affiliation");
                        sw.writeCharacters(affiliation);
                        sw.writeEndElement();
                    }
                }
            }
            
            sw.writeEndElement();
        }
        
        sw.writeEndElement();
    }
    
    /**
     * Write KEYWORDS as a set of SUBJECTS for DataCite (semi-colon-delimited)
     * @param sw the XMLStreamWriter to output on
     * @param keywords a set of keywords
     * @throws XMLStreamException on XML output errors
     */
    private static void writeKeywords(XMLStreamWriter sw, String keywords) throws XMLStreamException {
        if (StringUtils.isEmpty(keywords))
            return;
        
        sw.writeStartElement("subjects");
        
        for ( String keyword : keywords.split(";")) {
            sw.writeStartElement("subject");
            sw.writeCharacters(keyword);
            sw.writeEndElement();
        }
        
        sw.writeEndElement();
    }
    
    /**
     * Write a set of FUNDING IDENTIFIERS from SPONSORS (including required PRIMARY AWARDS)
     * to DataCite as "fundingReferences".  Currently schema only supports ONE
     * (the PRIMARY) award number per Sponsor.
     * 
     * @param sw the XMLStreamWriter to output on
     * @param sponsors a List of Sponsoring Organizations
     * @throws XMLStreamException on XML output errors
     */
    private static void writeFundingIdentifiers(XMLStreamWriter sw, List<SponsoringOrganization> sponsors, List<Award> awards) throws XMLStreamException {
        if ((null==sponsors || sponsors.isEmpty()) && (null==awards || awards.isEmpty()))
            return;
        
        sw.writeStartElement("fundingReferences");
        
        if (!(null==awards || awards.isEmpty())) {
            for ( Award award : awards ) {
                // Sponsor name first
                sw.writeStartElement("fundingReference");
                
                sw.writeStartElement("funderName");
                sw.writeCharacters(award.getFunderName());
                sw.writeEndElement();
                
                sw.writeStartElement("awardNumber");
                sw.writeAttribute("awardURI", award.getAwardDoi());
                //sw.writeCharacters(); // award number here?
                sw.writeEndElement();
                
                sw.writeEndElement();
            }
        }
        
        if (!(null==sponsors || sponsors.isEmpty())) {
            for ( SponsoringOrganization sponsor : sponsors ) {
                // Sponsor name first
                sw.writeStartElement("fundingReference");
                
                sw.writeStartElement("funderName");
                sw.writeCharacters(sponsor.getOrganizationName());
                sw.writeEndElement();
                
                sw.writeStartElement("awardNumber");
                sw.writeCharacters(sponsor.getPrimaryAward());
                sw.writeEndElement();
                
                sw.writeEndElement();
            }
        }
        
        sw.writeEndElement();
    }
    
    /**
     * Write out RELATED IDENTIFIERS to DataCite XML format.
     * 
     * @param sw the XMLStreamWriter to write on 
     * @param identifiers a set of RelatedIdentifier Objects, possibly empty
     * @throws XMLStreamException on XML output errors
     */
    private static void writeRelatedIdentifiers(XMLStreamWriter sw, List<RelatedIdentifier> identifiers) throws XMLStreamException {
        
        if (null==identifiers || identifiers.isEmpty())
            return;
        
        sw.writeStartElement("relatedIdentifiers");
        
        for ( RelatedIdentifier identifier : identifiers ) {
            sw.writeStartElement("relatedIdentifier");
            sw.writeAttribute("relatedIdentifierType", identifier.getIdentifierType().name());
            sw.writeAttribute("relationType", identifier.getRelationType().name());
            sw.writeCharacters(identifier.getIdentifierValue());
            sw.writeEndElement();
        }
        
        sw.writeEndElement();
    }
    
    
    
    /**
     * Write a Metadata DOI request. (Now supporting schema 4.3)
     * 
     * SET THE DOI TO THE DESIRED VALUE FIRST!
     * 
     * @param m the Metadata object
     * @throws IOException on write errors
     * @throws XMLStreamException on XML output errors
     * @return a String of DataCite formatted XML for this Metadata
     */
    protected static String writeMetadata(DOECodeMetadata m) throws IOException, XMLStreamException {
        // if no DOI is supplied, we will create one
        if (null==m.getDoi()) {
            throw new IOException ("DOI not set properly.");
        }
        
        // the Date Year only
        SimpleDateFormat date_only = new SimpleDateFormat("yyyy");
        // Write via STaX output streams
        XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
        StringWriter out = new StringWriter();
        XMLStreamWriter sw = xmlOutputFactory.createXMLStreamWriter(out);

        sw.writeStartDocument("1.0");
        sw.writeStartElement("resource");
        sw.writeDefaultNamespace("http://datacite.org/schema/kernel-4");
        sw.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        sw.writeAttribute("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
                "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.3/metadata.xsd"); 
        
        sw.writeStartElement("identifier");
        sw.writeAttribute("identifierType", "DOI");
        sw.writeCharacters(m.getDoi());
        sw.writeEndElement();
        
        writeDevelopers(sw, m.getDevelopers());
        writeContributors(sw, m.getContributors());
        
        sw.writeStartElement("titles");
        sw.writeStartElement("title");
        sw.writeCharacters(m.getSoftwareTitle());
        sw.writeEndElement();
        sw.writeEndElement();
        
        sw.writeStartElement("publisher");
        
        if (m.getResearchOrganizations().isEmpty()) {
            sw.writeCharacters("Not Available");
        } else {
            sw.writeCharacters(m.getResearchOrganizations().get(0).getOrganizationName());
        }
        sw.writeEndElement();
        
        sw.writeStartElement("publicationYear");
        sw.writeCharacters( (null==m.getReleaseDate()) ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy")) :
                date_only.format(m.getReleaseDate()));
        sw.writeEndElement();
        
        sw.writeStartElement("language");
        sw.writeCharacters("en");
        sw.writeEndElement();
        
        writeKeywords(sw, m.getKeywords());
        
        // send the "code ID" as the alternate identifier value
        sw.writeStartElement("alternateIdentifiers");
        sw.writeStartElement("alternateIdentifier");
        sw.writeAttribute("alternateIdentifierType", "DOE CODE ID");
        sw.writeCharacters(String.valueOf(m.getCodeId()));
        sw.writeEndElement();
        sw.writeEndElement();
        
        writeFundingIdentifiers(sw, m.getSponsoringOrganizations(), m.getAwardDois());
        
        sw.writeStartElement("resourceType");
        sw.writeAttribute("resourceTypeGeneral", "Software");
        sw.writeCharacters(m.getSoftwareTitle());
        sw.writeEndElement();
        
        writeRelatedIdentifiers(sw, m.getRelatedIdentifiers());
        
        sw.writeStartElement("descriptions");
        
        sw.writeStartElement("description");
        sw.writeAttribute("descriptionType", "Abstract");
        sw.writeCharacters(m.getDescription());
        sw.writeEndElement();
        
        sw.writeEndElement();
        
        sw.writeEndDocument();
        return out.toString();
    }
    
    /**
     * Send a request to DataCite to register DOI metadata information.
     * 
     * @param m the DOECodeMetadata object to register with DataCite
     * @throws IOException on metadata registration errors
     */
    private static void registerMetadata(DOECodeMetadata m) throws IOException {
        // set some reasonable default timeouts
        // create an HTTP client to request through
        CloseableHttpClient hc = 
                HttpClientBuilder
                .create()
                .setDefaultRequestConfig(RequestConfig
                    .custom()
                    .setSocketTimeout(60000)
                    .setConnectTimeout(60000)
                    .setConnectionRequestTimeout(60000)
                    .build())
                .build();
        
        try {
            // create an API authenticated request to send METADATA
            HttpPost request = new HttpPost(DATACITE_URL + "/metadata");
            String authentication = DATACITE_LOGIN + ":" + DATACITE_PASSWORD;
            byte[] encoded = Base64.encodeBase64(authentication.getBytes(Charset.forName("ISO-8859-1")));
            request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encoded));
            request.setHeader(HttpHeaders.ACCEPT, "application/xml");
            request.setHeader(HttpHeaders.CONTENT_TYPE, "application/xml; charset=UTF-8");
            
            request.setEntity(new StringEntity(writeMetadata(m), "UTF-8"));
            
            // 201 CREATED is the only successful API response
            HttpResponse response = hc.execute(request);
            int status_code = response.getStatusLine().getStatusCode();
            if ( HttpStatus.SC_CREATED!=status_code ) {
                // otherwise, read the reason why
                String text_response = EntityUtils.toString(response.getEntity());
                log.warn("DOI request failed, response code=" + status_code);

                throw new IOException ("Metadata failed: " + text_response);
            }
        } catch ( XMLStreamException e ) {
            log.warn("XML metadata error: " + e.getMessage());
            throw new IOException ("XML parser error: " + e.getMessage());
        } finally {
            hc.close();
        }
    }
    
    /**
     * Send a request to DataCite to translate a DOI value to a URL to resolve.
     * 
     * @param m the DOECodeMetadata Object to register with DataCite
     * @throws IOException on HTTP transmission errors, or failed to register DOI
     */
    private static void registerDoi(DOECodeMetadata m) throws IOException {
        // set some reasonable default timeouts
        // create an HTTP client to request through
        CloseableHttpClient hc = 
                HttpClientBuilder
                .create()
                .setDefaultRequestConfig(RequestConfig
                        .custom()
                        .setSocketTimeout(60000)
                        .setConnectTimeout(60000)
                        .setConnectionRequestTimeout(60000)
                        .build())
                .build();
        
        try {
            // send a DOI registration request
            HttpPost request = new HttpPost(DATACITE_URL + "/doi");
            String authentication = DATACITE_LOGIN + ":" + DATACITE_PASSWORD;
            byte[] encoded = Base64.encodeBase64(authentication.getBytes(Charset.forName("ISO-8859-1")));
            request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(encoded));
            request.setHeader(HttpHeaders.ACCEPT, "text/plain");
            request.setHeader(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
            
            request.setEntity(new StringEntity("doi=" + m.getDoi() + "\nurl=" + DATACITE_BASE_URL + m.getCodeId() + "\n"));
            
            HttpResponse response = hc.execute(request);
            int status_code = response.getStatusLine().getStatusCode();
            
            // success if SC_CREATED (201) returned, otherwise throw error
            if ( HttpStatus.SC_CREATED!=status_code ) {
                String text_response = EntityUtils.toString(response.getEntity());
                log.warn("DOI URL request failure, response code=" + status_code);
                throw new IOException ("DOI registration failed: " + text_response);
            }
        } finally {
            hc.close();
        }
    }
    
    /**
     * Determine whether or not the indicated METADATA record is registered to
     * that DOI.
     * 
     * If there's no DOI registered, consider this OPEN and available to be taken
     * by this CODE ID.  If a DOI IS registered, it MUST be the same CODE ID
     * in order to be accepted.
     * 
     * @param m the METADATA record
     * @return true if DOI is available OR owned by this METADATA, false if not
     */
    public static final boolean verifyDoiOwnership(DOECodeMetadata m) {
        EntityManager em = DoeServletContextListener.createEntityManager();
        
        try {
            TypedQuery<DOECodeMetadata> query = em.createNamedQuery("DOECodeMetadata.findByDoi", DOECodeMetadata.class)
                    .setParameter("doi", m.getDoi());
            List<DOECodeMetadata> results = query.getResultList();
            
            // DOI is NOT currently registered to anyone, OPEN
            if (results.isEmpty())
                return true;
            
            // get to whom it IS registered
            DOECodeMetadata result = results.get(0);
            
            // if this CODE ID belongs to the REGISTERED OWNER, we are OK
            // otherwise, return FALSE
            return (result.getCodeId().equals(m.getCodeId()));
        } finally {
            em.close();
        }
    }
    
    /**
     * Register DOI information with DataCite.
     * 
     * If DataCite information is not configured, or the register contains no
     * DOI value, this call is skipped.
     * 
     * Throws IOException on registration or DataCite errors, otherwise you
     * may assume success.
     * 
     * @param m the DOECodeMetadata object to register
     * @throws IOException on DOI registration errors
     */
    public static void register(DOECodeMetadata m) throws IOException {
        // if not configured, ignore this call
        if ("".equals(DATACITE_LOGIN) || StringUtils.isEmpty(m.getDoi()))
            return;
        
        // ensure the DOI to be registered is recognized and valid for this record
        // do we know this prefix?
        if (!m.getDoi().startsWith(DATACITE_PREFIX))
            return;
        
        // check to make sure we can proceed ( SHOULD NOT BE REGISTERED ELSEWHERE )
        if (!verifyDoiOwnership(m))
            throw new IOException ("DOI " + m.getDoi() + " is not exclusive to code ID " + m.getCodeId());
        
        // try the registration, throws IOException if failed
        registerMetadata(m);
        registerDoi(m);
    }
}
