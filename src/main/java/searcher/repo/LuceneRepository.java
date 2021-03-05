package searcher.repo;

import lombok.extern.slf4j.Slf4j;
import model.Product;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Lock;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

@Slf4j
public class LuceneRepository {

    private final Analyzer analyzer;
    private final FSDirectory dir;
    SpellChecker spellchecker;

    public static final String titleFieldName = "title";
    public static final String descriptionFieldName = "description";
    public static final String merchantFieldName = "merchant";
    public static final String catchAllFieldName = "catch_all";

    public LuceneRepository(Analyzer analyzer) throws IOException {
        this.analyzer = analyzer;
        dir = FSDirectory.open(Files.createTempDirectory(null));
        spellchecker = new SpellChecker(dir);
    }

    // note: the in-memory solution won't make sense with multiple requests!
    public void indexProducts(Collection<Product> products) {
        try {
            Lock lock = dir.obtainLock(Thread.currentThread().getName());
            lock.ensureValid();

            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            indexWriterConfig.setOpenMode(OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(dir, indexWriterConfig);

            log.info("Starting indexing..");

            products.parallelStream()
                    .forEach(doc -> {
                        Document document = new Document();

                        document.add(new TextField(titleFieldName, doc.getTitle(), Field.Store.YES));
                        document
                                .add(new TextField(descriptionFieldName, doc.getDescription(), Field.Store.YES));
                        document.add(new TextField(merchantFieldName, doc.getMerchant(), Field.Store.YES));

                        document.add(new TextField(catchAllFieldName, doc.getTitle(), Field.Store.NO));
                        document.add(new TextField(catchAllFieldName, doc.getDescription(), Field.Store.NO));
                        document.add(new TextField(catchAllFieldName, doc.getMerchant(), Field.Store.NO));

                        try {
                            writer.addDocument(document);
                        } catch (IOException e) {
                            log.error(e.getMessage());
                        }

                    });

            log.info("Indexed " + writer.numRamDocs() + " documents.");
            writer.close();

            log.info("Spellchecker initialisation..");

            DirectoryReader reader = DirectoryReader.open(dir);
            LuceneDictionary spellCheckerDictionary = new LuceneDictionary(reader, catchAllFieldName);
            spellchecker
                    .indexDictionary(spellCheckerDictionary, new IndexWriterConfig(new StandardAnalyzer()),
                            true);
            reader.close();
            lock.close();
            log.info("Indexing phase done.");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public List<Product> searchProducts(String queryString, int limit)
            throws ParseException, IOException {

        DirectoryReader indexReader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(indexReader);

        Query query = new QueryParser(catchAllFieldName, analyzer)
                .parse(queryString);
        TopDocs topDocs = searcher.search(query, limit);
        List<Document> documents = newArrayList();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            documents.add(searcher.doc(scoreDoc.doc));
        }
        indexReader.close();
        return documents.stream()
                .map(doc -> getProductFromDocument(doc))
                .collect(toList());
    }

    public Optional<String> didYouMean(String queryString) throws IOException {
        List<String> suggestions = asList(spellchecker.suggestSimilar(queryString, 1));
        return suggestions.size() > 0 ? Optional.of(suggestions.get(0)) : Optional.empty();
    }

    private Product getProductFromDocument(Document doc) {
        return new Product()
                .setTitle(doc.get(titleFieldName))
                .setDescription(doc.get(descriptionFieldName))
                .setMerchant(doc.get(merchantFieldName));
    }

}
