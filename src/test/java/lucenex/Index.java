package lucenex;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Trivial tests for indexing and search in Lucene
 */
public class Index {
	private static String string_path_index = "C:/Users/fsali/Desktop/Ing_Dati/HW-2/index";


	public static String getString_path_index() {
		return string_path_index;
	}


	private static String string_path_doc = "C:/Users/fsali/Desktop/Ing_Dati/HW-2/doc";


	public void indexDocs(Directory directory_index, Directory directory_doc, Codec codec) throws IOException {
		Analyzer defaultAnalyzer = new StandardAnalyzer();
		Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
		perFieldAnalyzers.put("contenuto", new StandardAnalyzer()); // uso questo analyzer per i contenuti
		perFieldAnalyzers.put("titolo", new WhitespaceAnalyzer()); // uso questo analyzer per i titoli

		Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		if (codec != null) {
			config.setCodec(codec);
		}
		IndexWriter writer = new IndexWriter(directory_index, config);
		writer.deleteAll();


		// Ottieni tutti i file dalla directory_doc
		Path docPath = ((FSDirectory) directory_doc).getDirectory(); // salvo in docPath il percorso di directory_doc
		int contatore_file_indicizzati = 0;

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(docPath, "*.txt")) { // per iterare sui file contenuti su una directory, solo su file .txt
			for (Path entry : stream) { // itero su tutti i file

				long file_Start_Time = System.currentTimeMillis(); // faccio partire il tempo
				String nome_file = entry.getFileName().toString().replaceFirst("[.][^.]+$", ""); // Questo è il titolo senza l'estensione
				String contenuto_file = new String(Files.readAllBytes(entry)); // Questo è il contenuto

				// Creazione di un documento Lucene
				Document documento = new Document();
				documento.add(new TextField("titolo", nome_file, Field.Store.YES));
				documento.add(new TextField("contenuto", contenuto_file, Field.Store.YES));
				writer.addDocument(documento);
				long fileEndTime = System.currentTimeMillis();
				System.out.println("Tempo di indicizzazione file " + nome_file + ": " + (fileEndTime - file_Start_Time) + "ms");
				contatore_file_indicizzati++;
			}
		} catch (IOException e) {
			// Gestisci l'eccezione
			e.printStackTrace();
		}
		System.out.println("\nTotale file indicizzati = " + contatore_file_indicizzati);
		writer.commit();
		writer.close();
	}
	public void read_from_index(Directory directory_index, String stringa_input) throws IOException, ParseException {
		read_from_index(directory_index, stringa_input, false);
	}

	public void read_from_index(Directory directory_index, String stringa_input, boolean explain) throws IOException, ParseException {
		IndexReader reader = DirectoryReader.open(directory_index);
		IndexSearcher searcher = new IndexSearcher(reader);

		String[] parts = stringa_input.split(":", 2);

		if (parts.length != 2) {
			throw new IllegalArgumentException("L'input deve essere nel formato 'campo:query'");
		}

		String campo = parts[0].trim(); // essa è titolo o contenuto
		String stringaQuery = parts[1].trim();

		if (!campo.equals("titolo") && !campo.equals("contenuto")) {
			throw new IllegalArgumentException("Il campo deve essere 'titolo' o 'contenuto'");
		}

		String[] termini = stringaQuery.split(" "); // uso il whitespace per separare i singoli termini
		List<String> listaTermini = Arrays.asList(termini);
		PhraseQuery.Builder builder = new PhraseQuery.Builder();
		for (String termine : listaTermini) {
			Term term = new Term(campo, termine);
			builder.add(term);
		}
		PhraseQuery phraseQuery = builder.build();



		// cerca tra i primi 10 risultati
		TopDocs hits = searcher.search(phraseQuery, 10);
		for (int i = 0; i < hits.scoreDocs.length; i++) {
			ScoreDoc scoreDoc = hits.scoreDocs[i];
			Document doc = searcher.doc(scoreDoc.doc);
			System.out.println("doc"+scoreDoc.doc + ":"+ doc.get("titolo") + " (" + scoreDoc.score +")");
			if (explain) {
				Explanation explanation = searcher.explain(phraseQuery, scoreDoc.doc);
				System.out.println(explanation);
			}
		}
	}


	public static void main(String[] args) throws IOException, ParseException {
		Path path_index = Paths.get(string_path_index);
		Path path_doc = Paths.get(string_path_doc);
		Directory directory_index = FSDirectory.open(path_index);
		Directory directory_doc = FSDirectory.open(path_doc);

		Index index = new Index();
		index.indexDocs(directory_index, directory_doc, new SimpleTextCodec());
		while(true) {
			System.out.println("\nPer cercare un termine o una frase nell'indice devi immetere\n"
					+ "\"titolo\":\"testo da cercare\" --> se vuoi cercare tra i nomi dei titoli\n "
					+ "oppure\n\"contenuto\": \"testo da cercare\" --> se vuoi cercare tra i contenuti\n\n"
					+ "Digita \"exit\" per uscire");
			Scanner scanner = new Scanner(System.in);
			String stringa_utente = scanner.nextLine();

			if("exit".equalsIgnoreCase(stringa_utente)) {
				System.out.println("Arrivederci!");
				scanner.close();
				break;
			}
			index.read_from_index(directory_index, stringa_utente, true);

		}
		directory_index.close();
		directory_doc.close();

	}

}


