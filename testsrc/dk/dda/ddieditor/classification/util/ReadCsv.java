package dk.dda.ddieditor.classification.util;

import java.io.FileReader;

import org.junit.Test;

import au.com.bytecode.opencsv.CSVReader;

/*
 * Copyright 2012 Danish Data Archive (http://www.dda.dk) 
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 * The full text of the license is also available on the Internet at 
 * http://www.gnu.org/copyleft/lesser.html
 */

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
