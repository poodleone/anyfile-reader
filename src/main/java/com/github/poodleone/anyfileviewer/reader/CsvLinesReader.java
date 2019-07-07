package com.github.poodleone.anyfileviewer.reader;

import java.util.Arrays;
import java.util.List;

import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.record.AbstractRecord;
import com.github.poodleone.anyfileviewer.record.Record;

/**
 * CSV形式のファイルのReaderです.
 */
public class CsvLinesReader extends LinesReader {
	@Override
	public List<String> getOptionNames() {
		return Arrays.asList("charset");
	}

	@Override
	protected Record readRecord(String line, RecordFormat format) {
		Record record = new AbstractRecord(null) {
			private int length = line.length();

			@Override
			public int getLength() {
				return length;
			}
		};
		String[] values = line.split(",");
		for (int i = 0; i < values.length; i++) {
			if (i < format.getListItems().size()) {
				record.getItems().put(format.getListItems().get(i), values[i]);
			} else {
				record.getItems().put("[" + i + "]", values[i]);
			}
		}
		return record;
	}

}
