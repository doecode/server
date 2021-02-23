/*
 */
package gov.osti.services;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.osti.entity.DOECodeMetadata;
import gov.osti.search.SearchData;
import gov.osti.search.SolrDocument;
import gov.osti.search.SolrResult;
import gov.osti.listeners.DoeServletContextListener;
import gov.osti.search.FacetCountsDeserializer;
import gov.osti.search.FacetDeserializer;
import gov.osti.search.SearchResponse;
import gov.osti.search.SolrFacet;
import gov.osti.search.SolrFacetCounts;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement a search interface with a SOLR backend.
 *
 * @author ensornl
 */
@Path("/search")
public class SearchService {
    @Context ServletContext context;

    // Logger
    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    /**
     * Implement a simple JSON filter to remove named properties.
     */
    @JsonFilter("filter properties by name")
    class PropertyFilterMixIn {}

    // filter out certain attribute names
    protected final static String[] ignoreProperties = {
        "recipientName",
        "recipient_name",
        "recipientEmail",
        "recipient_email",
        "recipientPhone",
        "recipient_phone",
        "owner",
        "workflowStatus",
        "workflow_status",
        "access_limitations",
        "accessLimitation",
        "access_limitation",
        "disclaimers",
        "licenseContactEmail",
        "license_contact_email",
        "awardDoi",
        "award_doi",
        "awardDois",
        "award_dois"
    };
    protected static FilterProvider filter = new SimpleFilterProvider()
            .addFilter("filter properties by name",
                    SimpleBeanPropertyFilter.serializeAllExcept(ignoreProperties));
    protected static FilterProvider filterExcludeFacets = new SimpleFilterProvider()
            .addFilter("filter properties by name",
                    SimpleBeanPropertyFilter.serializeAllExcept(ArrayUtils.addAll(ignoreProperties, new String[] {"facets","facet_counts"})));

    // specialized handler for SOLR date faceting
    protected static final SimpleModule moduleFacet = new SimpleModule()
            .addDeserializer(SolrFacet.class, new FacetDeserializer());
    protected static final SimpleModule moduleFacetCounts = new SimpleModule()
            .addDeserializer(SolrFacetCounts.class, new FacetCountsDeserializer());

    // Jackson ObjectMapper instance
    protected static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .addMixIn(Object.class, PropertyFilterMixIn.class)
            .setTimeZone(TimeZone.getDefault());
    protected static final ObjectMapper XML_MAPPER = new XmlMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT)
            .addMixIn(Object.class, PropertyFilterMixIn.class)
            .setTimeZone(TimeZone.getDefault());
    protected static final ObjectMapper JSON_MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(moduleFacet)
            .registerModule(moduleFacetCounts)
            .addMixIn(Object.class, PropertyFilterMixIn.class)
            .setTimeZone(TimeZone.getDefault());
    protected static final ObjectMapper BIBLIO_WRAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(SerializationFeature.WRAP_ROOT_VALUE, true)
            .addMixIn(Object.class, PropertyFilterMixIn.class)
            .setTimeZone(TimeZone.getDefault());

    // a JSON mapper
    protected static final ObjectMapper mapper = new ObjectMapper().setTimeZone(TimeZone.getDefault());

    // configured location of the search service endpoint
    private static final String SEARCH_URL = DoeServletContextListener.getConfigurationProperty("search.url");

    /**
     * Acquire information from the searching index if possible.  This endpoint
     * should ONLY return Approved records that have been indexed for searching.
     * Requires that searching be configured.
     *
     * Response Codes:
     * 200 - OK, record found, and returned in desired format
     * 206 - No content, searching is not configured/unavailable
     * 404 - Record was not found
     * 500 - IO error or search malformed
     *
     * @param codeId the CODE ID to find
     * @param format the desired FORMAT; may be "yaml" or "xml".  Default is JSON
     * unless specified
     * @return the record in the desired format, if found
     */
    @GET
    @Path("{codeId}")
    @Produces ({MediaType.APPLICATION_JSON, "text/yaml", MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN})
    public Response getSingleRecord(@PathParam("codeId") Long codeId, @QueryParam("format") String format, @QueryParam("export") boolean export) {
        // no search configured, you get nothing
        if ("".equals(SEARCH_URL))
            return Response
                    .status(Response.Status.NO_CONTENT)
                    .build();
        CloseableHttpClient hc = HttpClientBuilder
                .create()
                .build();

        try {
            // construct a Search for a single CODEID value
            URIBuilder builder = new URIBuilder(SEARCH_URL)
                .addParameter("q", "codeId:" + codeId)
                .addParameter("fl", "json")
                .addParameter("rows", "1");

            HttpGet get = new HttpGet(builder.build());

            HttpResponse response = hc.execute(get);

            if (HttpStatus.SC_OK==response.getStatusLine().getStatusCode()) {
                SolrResult result = JSON_MAPPER.readValue(EntityUtils.toString(response.getEntity()), SolrResult.class);

                if (result.getSearchResponse().isEmpty())
                    return ErrorResponse
                            .notFound("No records found.")
                            .build();
                // get the first result
                SolrDocument doc = result.getSearchResponse().getDocuments()[0];
                // convert it to a POJO
                DOECodeMetadata md = DOECodeMetadata.parseJson(new StringReader(doc.getJson()));
                if (export)
                    md.setDoi("https://doi.org/" + md.getDoi());

                // if no release date, don't return the DOI for display in search results.
                if (!StringUtils.isBlank(md.getDoi()) && md.getReleaseDate() == null)
                    md.setDoi(null);

                // if YAML is requested, return that; otherwise, default to JSON
                if ("yaml".equals(format)) {
                    // return the YAML
                    return
                        Response
                        .status(Response.Status.OK)
                        .header("Content-Type", "text/yaml")
                        .header("Content-Disposition", "attachment; filename = \"metadata.yml\"")
                        .entity(YAML_MAPPER
                                .writer(filter).writeValueAsString(md))
                        .build();
                } else if ("xml".equals(format)) {
                    return Response
                            .ok()
                            .header("Content-Type", MediaType.APPLICATION_XML)
                            .entity(XML_MAPPER
                                    .writer(filter).writeValueAsString(md))
                            .build();
                } else {
                    // send back the JSON (named object "metadata")
                    return Response
                        .ok()
                        .header("Content-Type", MediaType.APPLICATION_JSON)
                        .entity(BIBLIO_WRAPPER
                                .writer(filter)
                                .writeValueAsString(md))
                        .build();
                }
            } else {
                return ErrorResponse
                        .status(response.getStatusLine().getStatusCode())
                        .message(EntityUtils.toString(response.getEntity()))
                        .build();
            }
        } catch ( IOException | URISyntaxException e ) {
            log.warn("Searching Error.", e);
            return ErrorResponse.internalServerError("Search error encountered.").build();
        }
    }

    /**
     * Translate a SearchData parameter request to SOLR output search results.
     *
     * @param uriInfo the GET search parameters
     * @param format the optional output format (YAML/JSON/XML; JSON is default)
     * @return the output of the SOLR search results, if any
     */
    @GET
    @Produces ({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "text/yaml"})
    public Response searchGet(@Context UriInfo uriInfo, @QueryParam("format") String format) {
        // no search configured, you get nothing
        if ("".equals(SEARCH_URL))
            return Response
                    .status(Response.Status.NO_CONTENT)
                    .build();

        // get parameters
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

        // map parameters into JSON
        ObjectNode getParams = mapper.createObjectNode();

        SearchData so = new SearchData();
        List<String> arrayFields = new ArrayList<>();
        // determine string array parameters from the target object
        for (Field field : so.getClass().getDeclaredFields()) {
            if (field.getType().getSimpleName().equalsIgnoreCase("string[]"))
                arrayFields.add(field.getName());
        }

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().get(0);

            // if no key/value skip
            if (StringUtils.isBlank(key) || StringUtils.isBlank(value))
                continue;

            // convert snake_case input into camelCase for reflection matching
            String keyCamelCase =
                Arrays.stream(key.split("_"))
                    .map(String::toLowerCase)
                    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                    .collect(Collectors.joining());
            keyCamelCase = keyCamelCase.substring(0, 1).toLowerCase() + keyCamelCase.substring(1);

            // determine if field or value indicate an array
            boolean isArrayField = false;
            boolean isArrayValue = value.startsWith("[") && value.endsWith("]");
            if (arrayFields.contains(keyCamelCase))
                isArrayField = true;

            // convert input into JSON format
            if (isArrayField) {
                // array field requires special formatting for JSON mapper
                ArrayNode values = mapper.createArrayNode();

                // input is already in JSON array format, map it
                if (isArrayValue) {
                    try {
                        values = mapper.readValue(value, new TypeReference<ArrayNode>(){});
                    } catch (IOException ex) {
                        log.warn("Unable to process JSON ARRAY from GET parameter.");
                        log.warn("Message: " + ex.getMessage());
                        return ErrorResponse
                                .internalServerError("JSON ARRAY parameter formatting error.")
                                .build();
                    }
                }
                // input is a normal string, add it to array node
                else
                    values.add(value);

                // set key with string array value
                getParams.set(key, values);
            }
            else
                // set key with string value
                getParams.put(key, value);
        }

        // convert JSON to String, as expected for search()
        String parameters;
        try {
            parameters = mapper.writeValueAsString(getParams);
        } catch (JsonProcessingException e) {
            log.warn("Unable to process JSON from GET.");
            log.warn("Message: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("JSON formatting error.")
                    .build();
        }

        // call search
        return search(parameters, format);
    }

    /**
     * Translate a SearchData parameter request to SOLR output search results.
     *
     * @param parameters the JSON SearchData Object of search parameters
     * @param format the optional output format (YAML/JSON/XML; JSON is default)
     * @return the output of the SOLR search results, if any
     */
    @POST
    @Produces ({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "text/yaml"})
    @Consumes (MediaType.APPLICATION_JSON)
    public Response searchPost(String parameters, @QueryParam("format") String format) {
        // no search configured, you get nothing
        if ("".equals(SEARCH_URL))
            return Response
                    .status(Response.Status.NO_CONTENT)
                    .build();

        // call search
        return search(parameters, format);
    }

    private Response search(String parameters, String format) {
        try {
            // get a set of search parameters
            SearchData searchFor = SearchData.parseJson(new StringReader(parameters));
            boolean showFacets = searchFor.isShowFacets();

            CloseableHttpClient hc = HttpClientBuilder
                    .create()
                    .build();

            URIBuilder builder = new URIBuilder(SEARCH_URL)
                    .addParameter("q", searchFor.toQ())
                    .addParameter("fl", "json")
                    .addParameter("sort", searchFor.getSort());
            // if values are specified for rows and start, supply those.
            if (null!=searchFor.getRows())
                builder.addParameter("rows", String.valueOf(searchFor.getRows()));
            if (null!=searchFor.getStart())
                builder.addParameter("start", String.valueOf(searchFor.getStart()));
            // is show facets, add those
            if (showFacets) {
                    builder.addParameter("facet", "on");
                    builder.addParameter("facet.field", "fResearchOrganizations");
            }

            HttpGet get = new HttpGet(builder.build());

            HttpResponse response = hc.execute(get);

            if (HttpStatus.SC_OK==response.getStatusLine().getStatusCode()) {
                SolrResult result = JSON_MAPPER.readValue(EntityUtils.toString(response.getEntity()), SolrResult.class);
                // construct a search response object
                SearchResponse query = new SearchResponse();
                query.setStart(result.getSearchResponse().getStart());
                query.setNumFound(result.getSearchResponse().getNumFound());

                // if there are matched documents, load them in
                if ( null!=result.getSearchResponse().getDocuments() ) {
                    for ( SolrDocument doc : result.getSearchResponse().getDocuments() ) {
                        // convert it to a POJO
                        DOECodeMetadata md = JSON_MAPPER.readValue(doc.getJson(), DOECodeMetadata.class);

                        // if no release date, don't return the DOI for display in search results.
                        if (!StringUtils.isBlank(md.getDoi()) && md.getReleaseDate() == null)
                            md.setDoi(null);

                        query.add(md);
                    }
                    if (showFacets) {
                        // check out the FACETS
                        query.setFacets(result.getSolrFacet().getValues());
                        // check out the FACET COUNTS
                        query.setFacetFieldCounts(result.getSolrFacetCounts().getFields()); // fields
                    }
                }

                FilterProvider searchFilter = filter;
                if (!showFacets)
                    searchFilter = filterExcludeFacets;

                // respond with the appropriate format based on the input parameter
                if ("xml".equals(format)) {
                    return Response
                            .ok()
                            .header("Content-Type", MediaType.APPLICATION_XML)
                            .entity(XML_MAPPER
                                    .writer(searchFilter)
                                    .writeValueAsString(query))
                            .build();
                } else if ("yaml".equals(format)) {
                    return Response
                            .ok()
                            .header("Content-Type", "text/yaml")
                            .entity(YAML_MAPPER
                                    .writer(searchFilter)
                                    .writeValueAsString(query))
                            .build();
                } else {
                    return Response
                            .ok()
                            .header("Content-Type", MediaType.APPLICATION_JSON)
                            .entity(JSON_MAPPER
                                    .writer(searchFilter)
                                    .writeValueAsString(query))
                            .build();
                }
            } else {
                // let the user know something failed
                return ErrorResponse
                        .status(response.getStatusLine().getStatusCode())
                        .message(EntityUtils.toString(response.getEntity()))
                        .build();
            }
        } catch ( URISyntaxException e ) {
            log.warn("URI Error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("Unable to contact search provider.")
                    .build();
        } catch ( JsonProcessingException e ) {
            log.warn("Unable to process JSON from: " + parameters);
            log.warn("Message: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("JSON formatting error.")
                    .build();
        } catch ( IOException e ) {
            log.warn("Unhandled IO Error: " + e.getMessage());
            return ErrorResponse
                    .internalServerError("IO Error.")
                    .build();
        }
    }
}
