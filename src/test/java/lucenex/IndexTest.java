package lucenex;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IndexTest {

	private Directory directory_index;
	private IndexSearcher searcher;
	
	/* UTILITY PER I TEST */

	@Before
	public void setup() throws IOException {
		// Apri l'indice e crea un searcher
		this.directory_index = FSDirectory.open(Paths.get(Index.getString_path_index()));
		this.searcher = new IndexSearcher(DirectoryReader.open(directory_index));
		
	}

	@After
	public void end_setup() throws IOException {
		directory_index.close();
	}

	/* TEST PER I CONTENUTI*/



	@Test // il termine "riunione" matcha tutti i documenti
	public void testTermQueryInContentMatchAllDocuments() throws Exception {

		// Costruisci una query per il termine "riunione"
		TermQuery query = new TermQuery(new Term("contenuto", "riunione"));

		// Esegui la query
		TopDocs hits = this.searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che il numero di hit sia uguale al numero totale di documenti nell'indice
		assertEquals(DirectoryReader.open(directory_index).maxDoc(), hits.scoreDocs.length);		
	}

	@Test // il termine "pomeriggio" matcha solo un documento
	public void testTermQueryInContentMatchSingleDocument() throws Exception {
		
		// Costruisci una query per il termine "riunione"
		TermQuery query = new TermQuery(new Term("contenuto", "pomeriggio"));

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che ci sia solo un documento che matcha la query
		assertEquals(1, hits.scoreDocs.length);

		// Ottieni il documento matchato
		Document matchedDoc = searcher.doc(hits.scoreDocs[0].doc);

		// Verifica che il titolo del documento matchato sia "Mattinata Movimentata"
		assertEquals("Mattinata Movimentata", matchedDoc.get("titolo"));
	}


	@Test // la frase "il direttore ha sottolineato" è presente nei documenti: "Dimenticanze e Priorità
	// e "Rinnovo e Organizzazione".
	public void testPhraseQueryInContentMatchDoubleDocuments() throws Exception {

		// Costruisci una Phrasequery per la frase "il direttore ha sottolineato"
		PhraseQuery query = new PhraseQuery.Builder()
				.add(new Term("contenuto", "il"))
				.add(new Term("contenuto", "direttore"))
				.add(new Term("contenuto", "ha"))
				.add(new Term("contenuto", "sottolineato"))
				.build();

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che ci siano solo due documenti che matchano la query
		assertEquals(2, hits.scoreDocs.length);


		// Ottieni i documenti matchati
		Document matchedDoc1 = searcher.doc(hits.scoreDocs[0].doc);
		Document matchedDoc2 = searcher.doc(hits.scoreDocs[1].doc);


		// Verifica che il titolo del documento matchato1 sia "Dimenticanze e Priorità"
		assertEquals("Dimenticanze e Priorità", matchedDoc1.get("titolo"));

		// Verifica che il titolo del documento matchato2 sia "Rinnovo e Riorganizzazione"
		assertEquals("Rinnovo e Riorganizzazione", matchedDoc2.get("titolo"));
	}


	@Test // la frase "Il direttore ha sottolineato" non viene trovata nei documenti perchè lo standardAnalyzer
	// dei contenuti trasforma tutti i termini maiuscoli in minuscoli.
	public void testPhraseQueryInContentNotMatchCapitalizedTerms() throws Exception {

		// Costruisci una Phrasequery per la frase "il direttore ha sottolineato"
		PhraseQuery query = new PhraseQuery.Builder()
				.add(new Term("contenuto", "Il")) // termine in maiuscolo !
				.add(new Term("contenuto", "direttore"))
				.add(new Term("contenuto", "ha"))
				.add(new Term("contenuto", "sottolineato"))
				.build();

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che non ci sia nessun documento che matcha la query
		assertEquals(0, hits.scoreDocs.length);
	}

	@Test // Il termine "divano" non matcha nessun documento
	public void testTermQueryInContentNotMatchDocuments() throws Exception {

		// Costruisci un TermQuery per il termine "divano"
		TermQuery query = new TermQuery(new Term("contenuto", "divano"));

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che non ci sia nessun documento che matcha la query
		assertEquals(0, hits.scoreDocs.length);
	}



	/* TEST PER I TITOLI */



	@Test // il termine "e" matcha 3 titoli di documenti
	public void testTermQueryInTitleMatchTripleDocuments() throws Exception {

		// Costruisci una query per il termine "riunione"
		TermQuery query = new TermQuery(new Term("titolo", "e"));

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che ci siano solo 3 i documenti che matchano la query
		assertEquals(3, hits.scoreDocs.length);


		// Ottieni i documenti matchati
		Document matchedDoc1 = searcher.doc(hits.scoreDocs[0].doc);
		Document matchedDoc2 = searcher.doc(hits.scoreDocs[1].doc);
		Document matchedDoc3 = searcher.doc(hits.scoreDocs[2].doc);


		// Verifica che il titolo del documento matchato1 sia "Caldo e Affari"
		assertEquals("Caldo e Affari", matchedDoc1.get("titolo"));

		// Verifica che il titolo del documento matchato2 sia "Dimenticanze e Priorità"
		assertEquals("Dimenticanze e Priorità", matchedDoc2.get("titolo"));

		// Verifica che il titolo del documento matchato3 sia "Rinnovo e Riorganizzazione"
		assertEquals("Rinnovo e Riorganizzazione", matchedDoc3.get("titolo"));
	}

	@Test // il termine "Caldo" matcha solo un documento perché presente nel titolo "Caldo e Affari"
	public void testTermQueryInTitleMatchSingleDocument() throws Exception {

		// Costruisci una query per il termine "riunione"
		TermQuery query = new TermQuery(new Term("titolo", "Caldo"));

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che ci sia solo un documento che matcha la query
		assertEquals(1, hits.scoreDocs.length);

		// Ottieni il documento matchato
		Document matchedDoc = searcher.doc(hits.scoreDocs[0].doc);

		// Verifica che il titolo del documento matchato sia "Mattinata Movimentata"
		assertEquals("Caldo e Affari", matchedDoc.get("titolo"));
	}


	@Test // la frase "Dimenticanze e Priorità" è presente nel titolo del documento: "Dimenticanze e Priorità
	public void testPhraseQueryInTitleMatchSingleDocuments() throws Exception {

		// Costruisci una Phrasequery per la frase "Dimenticanze e Priorità"
		PhraseQuery query = new PhraseQuery.Builder()
				.add(new Term("titolo", "Dimenticanze"))
				.add(new Term("titolo", "e"))
				.add(new Term("titolo", "Priorità"))
				.build();

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che ci sia solo un documento che matcha la query
		assertEquals(1, hits.scoreDocs.length);


		// Ottieni il documenti matchato
		Document matchedDoc = searcher.doc(hits.scoreDocs[0].doc);


		// Verifica che il titolo del documento matchato sia "Dimenticanze e Priorità"
		assertEquals("Dimenticanze e Priorità", matchedDoc.get("titolo"));
	}


	@Test // nei titoli si usa il WhiteSpaceAnalyzer che non converte i termini da maiuscoli in minuscoli
	// ma lascia il font originale dei caratteri
	public void testPhraseQueryInTitleNotMatchDiversityOfFontSize() throws Exception {

		// Costruisci una Phrasequery per la frase "dimenticanze e priorità" --> tutto in minuscolo
		PhraseQuery query = new PhraseQuery.Builder()
				.add(new Term("titolo", "dimenticanze"))
				.add(new Term("titolo", "e"))
				.add(new Term("titolo", "priorità"))
				.build();

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che non ci sia nessun documento che matcha la query
		assertEquals(0, hits.scoreDocs.length);
	}

	@Test // Il termine "Computer" non matcha nessun titolo di documenti
	public void testTermQueryInTitleNotMatchDocuments() throws Exception {

		// Costruisci un TermQuery per il termine "Computer"
		TermQuery query = new TermQuery(new Term("titolo", "Computer"));

		// Esegui la query
		TopDocs hits = searcher.search(query, 10); // assumiamo che ci siano al massimo 10 documenti

		// Assicuriamoci che non ci sia nessun documento che matcha la query
		assertEquals(0, hits.scoreDocs.length);
	}

}



