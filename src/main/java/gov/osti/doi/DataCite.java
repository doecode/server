/*
 */
package gov.osti.doi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import gov.osti.entity.Contributor;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.entity.Developer;
import gov.osti.entity.RelatedIdentifier;
import gov.osti.entity.SponsoringOrganization;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface for DataCite registration of software DOI values.
 * 
 * @author ensornl
 */
public class DataCite {
    // logger
    private static Logger log = LoggerFactory.getLogger(DataCite.class);

    
    // Jackson object mapper
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    
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
            sw.writeStartElement("creator");
            
            sw.writeStartElement("creatorName");
            sw.writeCharacters(developer.getLastName() + ", " + developer.getFirstName());
            sw.writeEndElement();
            
            sw.writeStartElement("givenName");
            sw.writeCharacters(developer.getFirstName());
            sw.writeEndElement();
            
            sw.writeStartElement("familyName");
            sw.writeCharacters(developer.getLastName());
            sw.writeEndElement();
            
            if (!"".equals(developer.getOrcid())) {
                sw.writeStartElement("nameIdentifier");
                sw.writeAttribute("schemeURI", "http://orcid.org/");
                sw.writeAttribute("nameIdentifierScheme", "ORCID");
                sw.writeCharacters(developer.getOrcid());
                sw.writeEndElement();
            }
            if (!"".equals(developer.getAffiliations())) {
                sw.writeStartElement("affiliation");
                sw.writeCharacters(developer.getAffiliations());
                sw.writeEndElement();
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
        if (contributors.isEmpty())
            return;
        
        sw.writeStartElement("contributors");
        
        for ( Contributor contributor : contributors ) {
            sw.writeStartElement("contributor");
            sw.writeAttribute("contributorType", contributor.getContributorType().name());
            
            sw.writeStartElement("contributorName");
            sw.writeCharacters(contributor.getLastName() + ", " + contributor.getFirstName());
            sw.writeEndElement();
            
            if (!"".equals(contributor.getOrcid())) {
                sw.writeStartElement("nameIdentifier");
                sw.writeAttribute("schemeURI", "http://orcid.org/");
                sw.writeAttribute("nameIdentifierScheme", "ORCID");
                sw.writeCharacters(contributor.getOrcid());
                sw.writeEndElement();
            }
            if (!"".equals(contributor.getAffiliations())) {
                sw.writeStartElement("affiliation");
                sw.writeCharacters(contributor.getAffiliations());
                sw.writeEndElement();
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
        if (null==keywords)
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
    private static void writeFundingIdentifiers(XMLStreamWriter sw, List<SponsoringOrganization> sponsors) throws XMLStreamException {
        if (sponsors.isEmpty())
            return;
        
        sw.writeStartElement("fundingReferences");
        
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
        
        if (identifiers.isEmpty())
            return;
        
        sw.writeStartElement("relatedIdentifiers");
        
        for ( RelatedIdentifier identifier : identifiers ) {
            sw.writeStartElement("relatedIdentifier");
            sw.writeAttribute("relatedIdentifierType", identifier.getIdentifierType().getName());
            sw.writeAttribute("relationType", identifier.getRelationType().getName());
            sw.writeCharacters(identifier.getIdentifierValue());
            sw.writeEndElement();
        }
        
        sw.writeEndElement();
    }
    
    
    
    /**
     * Write a Metadata DOI request. (Now supporting schema 4.0)
     * 
     * SET THE DOI TO THE DESIRED VALUE FIRST!
     * 
     * @param m the Metadata object
     * @throws IOException on write errors
     * @throws XMLStreamException on XML output errors
     * @return a String of DataCite formatted XML for this Metadata
     */
    public static String writeMetadata(DOECodeMetadata m) throws IOException, XMLStreamException {
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
                "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4/metadata.xsd");
        
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
        sw.writeCharacters( (null==m.getDateOfIssuance()) ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyy")) :
                date_only.format(m.getDateOfIssuance()));
        sw.writeEndElement();
        
        sw.writeStartElement("language");
        sw.writeCharacters("en");
        sw.writeEndElement();
        
        writeKeywords(sw, m.getKeywords());
        
        // send the "code ID" as the alternate identifier value
        sw.writeStartElement("alternateIdentifiers");
        sw.writeStartElement("alternateIdentifier");
        sw.writeAttribute("alternateIdentifierType", "DOECode ID");
        sw.writeCharacters(String.valueOf(m.getCodeId()));
        sw.writeEndElement();
        sw.writeEndElement();
        
        writeFundingIdentifiers(sw, m.getSponsoringOrganizations());
        
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
    
}
