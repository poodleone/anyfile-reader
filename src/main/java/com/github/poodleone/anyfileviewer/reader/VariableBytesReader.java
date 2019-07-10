package com.github.poodleone.anyfileviewer.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import com.github.poodleone.anyfileviewer.DataParser;
import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.record.AbstractRecord;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.record.RecordSet;

/**
 * 可変長バイナリ形式のファイルのReaderです.
 *
 */
public class VariableBytesReader implements RecordReader {

	@Override
	public List<String> getOptionNames() {
		return Arrays.asList("readProcess", "dumpLayouts");
	}

	@Override
	public void load(Path path, RecordFormat format, Consumer<Record> consumer) {
		String readProcess = format.getReaderOptions().get("readProcess");
		try (InputStream is = Files.newInputStream(path)) {
			while (true) {
				byte[] data = DataParser.eval(null, readProcess, byte[].class, new DataParser.Param("inputStream", is));
				if (data == null) {
					break;
				}
				consumer.accept(readRecord(data, format));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public RecordSet load(Path path, RecordFormat format, int offset, int maxRecords) {
		String readProcess = format.getReaderOptions().get("readProcess");
		try {
			if (offset == -1) {
				offset = getLastPageOffset(path, readProcess, maxRecords);
			}
		
			try (InputStream is = Files.newInputStream(path)) {
				RecordSet records = new RecordSet();

				int index = 0;
				while (true) {
					byte[] data = DataParser.eval(null, readProcess, byte[].class, new DataParser.Param("inputStream", is));
					if (data == null) {
						records.setEof(true);
						break;
					}
					try {
						if (index < offset) {
							continue;
						}
						if (0 < maxRecords && offset + maxRecords <= index) {
							break;
						}
						records.add(readRecord(data, format));
					} finally {
						index++;
					}
				}
				
				records.setPath(path);
				if (records.eof() && index != 0 && records.size() == 0) {
					// offsetがファイルの範囲外の場合はファイル末尾までを再読み込み
					return load(path, format, -1, maxRecords);
				}				records.setFormat(format);
				records.setOffset(offset);
				return records;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private int getLastPageOffset(Path path, String readProcess, int maxRecords) {
		int count = 0;
		try (InputStream is = Files.newInputStream(path)) {
			while (true) {
				byte[] data = DataParser.eval(null, readProcess, byte[].class, new DataParser.Param("inputStream", is));
				if (data == null) {
					break;
				}
				count++;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return (count / maxRecords) * maxRecords;
	}
	protected Record readRecord(byte[] data, RecordFormat format) {
		// レコード生成
		Record record = new AbstractRecord(data) {
			@Override
			public int getLength() {
				return ((byte[]) getRawData()).length;
			}
		};
		// データをパース
		DataParser.parseRecord(record, format);
		return record;
	}
}
