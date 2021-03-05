package searcher.web;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import model.Product;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import searcher.repo.LuceneRepository;
import searcher.repo.S3Repository;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Slf4j
@RestController
public class Controller {

    @Autowired
    private S3Repository s3Repository;
    @Autowired
    private LuceneRepository luceneRepository;

    private final ObjectMapper objectMapper;

    public Controller() {
        this.objectMapper = new ObjectMapper();
    }

    @RequestMapping(value = "/index", method = {POST})
    @ResponseBody
    public ResponseEntity<String> index(@RequestParam String fileUrl) throws URISyntaxException {

        try {
            String fileContent = s3Repository.getFileContent(fileUrl);
            List<Product> products = asList(objectMapper.readValue(fileContent, Product[].class));
            luceneRepository.indexProducts(products);
            return new ResponseEntity<>("Indexed file " + fileUrl, OK);
        } catch (IOException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>("The file cannot be parsed: " + fileUrl, BAD_REQUEST);
        }
    }

    @RequestMapping(value = "/query", method = {POST})
    @ResponseBody
    public ResponseEntity<List<Product>> query(@RequestParam String queryString,
                                               @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Product> products = luceneRepository.searchProducts(queryString, limit);
            if (products.isEmpty()) {
                String didYouMean = luceneRepository.didYouMean(queryString).orElse("");
                MultiValueMap<String, String> headers = new LinkedMultiValueMap();
                headers.put("X-did-you-mean", asList(didYouMean));
                return new ResponseEntity<>(products, headers, OK);
            } else {
                return new ResponseEntity<>(products, OK);
            }

        } catch (ParseException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(newArrayList(), BAD_REQUEST);
        } catch (IOException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(newArrayList(), INTERNAL_SERVER_ERROR);
        }
    }


}
