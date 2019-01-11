package searcher.repo;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import model.Product;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Lock;

@Slf4j
public class LuceneRepository {

  private final Analyzer analyzer;
  private final FSDirectory dir;

  public LuceneRepository(Analyzer analyzer) throws IOException {
    this.analyzer = analyzer;
    dir = FSDirectory.open(Files.createTempDirectory(null));
  }

  // note: the in-memory solution won't make sense with multiple requests!
  public void indexProducts(Collection<Product> products) {

    try {
      Lock lock = dir.obtainLock(Thread.currentThread().getName());
      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
      indexWriterConfig.setOpenMode(OpenMode.CREATE);
      IndexWriter writer = new IndexWriter(dir, indexWriterConfig);

      products.parallelStream()
          .forEach(doc -> {
            Document document = new Document();

            document.add(new TextField("title", doc.getTitle(), Field.Store.YES));
            document.add(new TextField("description", doc.getDescription(), Field.Store.YES));
            document.add(new TextField("merchant", doc.getMerchant(), Field.Store.YES));

            document.add(new TextField("catch_all", doc.getTitle(), Field.Store.NO));
            document.add(new TextField("catch_all", doc.getDescription(), Field.Store.NO));
            document.add(new TextField("catch_all", doc.getMerchant(), Field.Store.NO));

            try {
              writer.addDocument(document);
            } catch (IOException e) {
              log.error(e.getMessage());
            }

          });

      writer.close();
      lock.close();
    } catch (IOException e) {
      log.error(e.getMessage());
    }
  }

  public List<Product> searchProducts(String queryString, int limit)
      throws ParseException, IOException {

    DirectoryReader indexReader = DirectoryReader.open(dir);
    IndexSearcher searcher = new IndexSearcher(indexReader);

    Query query = new QueryParser("catch_all", analyzer)
        .parse(queryString);
    TopDocs topDocs = searcher.search(query, limit);
    List<Document> documents = newArrayList();
    for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
      documents.add(searcher.doc(scoreDoc.doc));
    }
    return documents.stream()
        .map(doc -> getProductFromDocument(doc))
        .collect(toList());
  }

  private Product getProductFromDocument(Document doc) {
    return new Product()
        .setTitle(doc.get("title"))
        .setDescription(doc.get("description"))
        .setMerchant(doc.get("merchant"));
  }

}
