package searcher.config;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searcher.repo.LuceneRepository;
import searcher.repo.S3Repository;

@Configuration
public class Config {

  @Value("${s3.region}")
  private String s3Region;

  // https://s3-eu-west-1.amazonaws.com/pricesearcher-code-tests/software-engineer/products.json.gz
  public AmazonS3 s3Client() {
    return AmazonS3ClientBuilder.standard()
        .withRegion(s3Region)
        .build();
  }

  @Bean
  public S3Repository s3Repository() {
    return new S3Repository(s3Client());
  }

  @Bean
  public LuceneRepository luceneRepository() throws IOException {
    PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(
        new StandardAnalyzer(),
        ImmutableMap.of(
            "title", new StandardAnalyzer(),
            "description", new EnglishAnalyzer(),
            "merchant", new StandardAnalyzer()
        ));
    return new LuceneRepository(analyzer);
  }


}
