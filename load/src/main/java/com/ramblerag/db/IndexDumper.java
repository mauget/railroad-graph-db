package com.ramblerag.db;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

@SuppressWarnings("restriction")
public class IndexDumper {

	public static void main(String[] args) {
		if (args.length == 0){
			args = new String[]{"var/graphdb/index"};
		}
		try {
			new IndexDumper().dump(args[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void dump(String dir) throws XMLStreamException,
			FactoryConfigurationError, CorruptIndexException, IOException {

		XMLStreamWriter out = XMLOutputFactory.newInstance()
				.createXMLStreamWriter(System.out);

		IndexReader reader = IndexReader.open(FSDirectory.open(new File(dir)),
				true);
		out.writeStartDocument();

		out.writeStartElement("dir");
		out.writeCharacters(dir);
		out.writeEndElement();
		
		out.writeStartElement("document-count");
		out.writeCharacters(String.format("%d", reader.numDocs()));
		out.writeEndElement();
		
		out.writeStartElement("documents");
		for (int i = 0; i < reader.numDocs(); i++) {
			dumpDocument(reader.document(i), out);
		}
		out.writeEndElement();
		out.writeEndDocument();

		out.flush();
		reader.close();
	}

	@SuppressWarnings({ })
	private void dumpDocument(Document document, XMLStreamWriter out)
			throws XMLStreamException {
		out.writeStartElement("document");
		for (Fieldable fieldable : (List<Fieldable>) document.getFields()) {
			out.writeStartElement("fieldable");
			out.writeAttribute("name", fieldable.name());
			out.writeAttribute("value", fieldable.stringValue());
			out.writeEndElement();
		}
		out.writeEndElement();
	}
}