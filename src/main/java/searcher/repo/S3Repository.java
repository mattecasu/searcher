package searcher.repo;

import static java.util.stream.Collectors.joining;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.S3Object;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.GZIPInputStream;

public class S3Repository {

  private final AmazonS3 s3Client;

  public S3Repository(AmazonS3 s3Client) {
    this.s3Client = s3Client;
  }

  public String getFileContent(String fileUrl) throws IOException, URISyntaxException {
    AmazonS3URI s3URI = new AmazonS3URI(new URI(fileUrl));
    S3Object s3Object = s3Client.getObject(s3URI.getBucket(), s3URI.getKey());
    return new BufferedReader(new InputStreamReader(
        new GZIPInputStream(s3Object.getObjectContent())))
        .lines()
        .collect(joining());
  }

}
