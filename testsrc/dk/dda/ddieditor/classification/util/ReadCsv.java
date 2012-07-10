package dk.dda.ddieditor.classification.util;

import java.io.FileReader;

import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

public class ReadCsv {
	@Test
	public void test() throws Exception {
		CSVReader reader = new CSVReader(new FileReader("resources/test.csv"));
		String[] nextLine;
		int count = 1;
		while ((nextLine = reader.readNext()) != null) {
			System.out.println("Line:" + count);
			for (int i = 0; i < nextLine.length; i++) {
				System.out.println(i + ": '" + nextLine[i] + "'");
			}
			count++;
		}

		// List myEntries = reader.readAll();
		// System.out.println("break");
	}
}
