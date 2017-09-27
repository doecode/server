/*
     Service class for the google sitemap, as it currently stands -WAS 9/26/2017
 */
package gov.osti.services;

import gov.osti.entity.DOECodeMetadata;
import gov.osti.listeners.DoeServletContextListener;
import gov.osti.search.SearchResponse;
import gov.osti.search.SolrDocument;
import gov.osti.search.SolrResult;
import static gov.osti.services.SearchService.JSON_MAPPER;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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
 *
 * @author smithwa
 */
@Path("/sitemap/")
public class GoogleSitemapService {

     protected static final int MAX_RECORDS_PER_SITEMAP_PAGE = 20000;
     protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
     private static final String SEARCH_URL = DoeServletContextListener.getConfigurationProperty("search.url");
     private static final String SITE_URL = DoeServletContextListener.getConfigurationProperty("site.url");

     @Context
     ServletContext context;
     @Context
     UriInfo uri;

     // Logger
     private static final Logger log = LoggerFactory.getLogger(SearchService.class);

     /**
      * Return the list of pages of records
      *
      * @return
      */
     @GET
     @Produces(MediaType.TEXT_XML)
     @Path("xml")
     public Response getSitemapList() {
          //Get the number of pages based on teh number of records
          long numOfRecords = getNumberOfRecordsInIndex();
          double numOfPages = (float) numOfRecords / MAX_RECORDS_PER_SITEMAP_PAGE;

          //Get the date we want to use
          LocalDate firstOfMonth = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
          String moddedDate = firstOfMonth.format(DATE_FORMATTER);

          //Get the opennet sitemap string we will be using
          String sitemapURL = uri.getBaseUri().toString();

          //Wirte out the xml
          StringBuilder xml_string = new StringBuilder();
          xml_string.append("<sitemapindex xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
          for (int i = 0; i < numOfPages; i++) {
               xml_string.append("<sitemap>");
               xml_string.append("<loc>" + sitemapURL + (i + 1) + "</loc>");
               xml_string.append("<lastmod>" + moddedDate + "</lastmod>");
               xml_string.append("</sitemap>");
          }
          xml_string.append("</sitemapindex>");

          return Response.ok(xml_string.toString(), MediaType.TEXT_XML).build();
     }

     @GET
     @Produces(MediaType.TEXT_XML)
     @Path("xml/{pageNum}")
     public Response getSitemapPage(@PathParam("pageNum") Long pageNum) {
          long startNum = (pageNum - 1) * MAX_RECORDS_PER_SITEMAP_PAGE;
          StringBuilder xml_string = new StringBuilder();

          try {
               CloseableHttpClient hc = HttpClientBuilder.create().build();
               URIBuilder builder = new URIBuilder(SEARCH_URL).addParameter("q", "*:*").addParameter("rows", Integer.toString(MAX_RECORDS_PER_SITEMAP_PAGE))
                       .addParameter("omitHeader", "true").addParameter("fl", "json").addParameter("fl", "codeId").addParameter("sort", "codeId asc").addParameter("start", Long.toString(startNum));
               HttpGet get = new HttpGet(builder.build());
               HttpResponse response = hc.execute(get);

               if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                    //Create a result object
                    SolrResult result = JSON_MAPPER.readValue(EntityUtils.toString(response.getEntity()), SolrResult.class);
                    SearchResponse query = new SearchResponse();
                    query.setStart(result.getSearchResponse().getStart());
                    query.setNumFound(result.getSearchResponse().getNumFound());

                    if (null != result.getSearchResponse().getDocuments()) {
                         for (SolrDocument doc : result.getSearchResponse().getDocuments()) {
                              query.add(JSON_MAPPER.readValue(doc.getJson(), DOECodeMetadata.class));
                         }
                    }
                    String right_now_string = LocalDate.now().format(DATE_FORMATTER);
                    xml_string.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");
                    query.getDocs().forEach((record) -> {
                         xml_string.append("<url>");
                         xml_string.append("<loc>" + SITE_URL + "/biblio" + "/" + record.getCodeId() + "</loc>");
                         xml_string.append("<lastmod>" + right_now_string + "</lastmod>");
                         xml_string.append("<changefreq>monthly</changefreq>");
                         xml_string.append("<priority>0.5</priority>");
                         xml_string.append("</url>");
                    });
                    xml_string.append("</urlset>");
               }
          } catch (URISyntaxException ex) {
               log.error("Error in getting solr count: " + ex.getMessage());
          } catch (IOException ex) {
               log.error("Error in getting solr count: " + ex.getMessage());
          }
          return Response.ok(xml_string.toString(), MediaType.TEXT_XML).build();
     }

     public long getNumberOfRecordsInIndex() {
          long totalCount = 0;
          try {
               CloseableHttpClient hc = HttpClientBuilder.create().build();
               URIBuilder builder = new URIBuilder(SEARCH_URL).addParameter("q", "*:*").addParameter("rows", "0").addParameter("omitHeader", "true");
               HttpGet get = new HttpGet(builder.build());
               HttpResponse response = hc.execute(get);

               if (HttpStatus.SC_OK == response.getStatusLine().getStatusCode()) {
                    SolrResult result = JSON_MAPPER.readValue(EntityUtils.toString(response.getEntity()), SolrResult.class);
                    totalCount = result.getSearchResponse().getNumFound();
               }
          } catch (URISyntaxException ex) {
               log.error("Error in getting solr count: " + ex.getMessage());
          } catch (IOException ex) {
               log.error("Error in getting solr count: " + ex.getMessage());
          }
          return totalCount;
     }
}
