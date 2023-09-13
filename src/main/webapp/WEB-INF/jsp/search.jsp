<%-- 
    Document   : DOE CODE web API documentation template
    Created on : Jun 29, 2017, 1:02:39 PM
    Author     : ensornl
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
     <head>
          <meta charset='utf-8'>
          <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
          <meta name="viewport" content="width=device-width">

          <title>DOE CODE Search API Documentation</title>

          <!-- Flatdoc -->
          <script src="//ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
          <script src='${pageContext.request.contextPath}/js/legacy.js'></script>
          <script src='${pageContext.request.contextPath}/js/flatdoc-0.90-jq3.js'></script>

          <!-- Flatdoc theme -->
          <link href="${pageContext.request.contextPath}/css/flatdoc-theme.css" rel="stylesheet">
          <link href="${pageContext.request.contextPath}/css/doecodeapi.css" rel="stylesheet">
          <script src="${pageContext.request.contextPath}/js/flatdoc-theme.js"></script>

          <!-- Meta -->
          <meta content="DOE CODE Services API" property="og:title">
          <meta content="Back-end services for DOE CODE application." name="description">

          <!-- Initializer -->
          <script>
              Flatdoc.run({
                 fetcher: Flatdoc.file('${pageContext.request.contextPath}/markdown/Search.md')
              });
          </script>
     </head>
     <body role='flatdoc'>

          <div class='header'>
               <div class='left'>
                    <h1>DOE CODE API</h1>
                    <ul>
                         <li><a href='/doecodeapi/services/docs'>API Services Documentation</a></li>
                    </ul>
               </div>
          </div>

          <div class='content-root'>
               <div class='menubar'>
                    <div class='menu section' role='flatdoc-menu'></div>
               </div>
               <div role='flatdoc-content' class='content'></div>
          </div>

     </body>
</html>
