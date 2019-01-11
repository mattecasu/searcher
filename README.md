# searcher

To run the service: `./gradlew bootRun`

Visit `http://localhost:8080/swagger-ui.html` for the service documentation.
There are two endpoints:
- `/index`, to index a (compliant) file from S3 [**note**: pay attention not to have credentials set in ~/.aws/credentials]
- `/query`, to query with lucene syntax (by default searches in all fields)

Current config:
- "title", StandardAnalyzer
- "description", EnglishAnalyzer
- "merchant", StandardAnalyzer
- "catch_all" field (default) with the default (Standard) analyzer

When the result is empty, the header `x-did-you-mean` will contain a possible suggestion.