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
 * 固定長バイナリ形式のファイルのReaderです.
 *
 */
public class FixedBytesReader implements RecordReader {

	@Override
	public List<String> getOptionNames() {
		return Arrays.asList("recordSize", "dumpLayouts");
	}

	@Override
	public void load(Path path, RecordFormat format, Consumer<Record> consumer) {
		int recordSize = getRecordSize(format);
		byte[] buffer = new byte[recordSize];
		try (InputStream is = Files.newInputStream(path)) {
			while (true) {
				if (recordSize != is.read(buffer)) {
					break;
				}
				consumer.accept(readRecord(buffer, format));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public RecordSet load(Path path, RecordFormat format, int offset, int maxRecords) {
		int recordSize = getRecordSize(format);
		byte[] buffer = new byte[recordSize];
		try {
			long size = Files.size(path);
			if (offset == -1 || size < offset * recordSize) {
				// offsetが-1かファイルの範囲外の場合はファイル末尾まで読むように調整
				offset = Integer.max((int) (size / recordSize) - maxRecords, 0);
			}
		
			try (InputStream is = Files.newInputStream(path)) {
				RecordSet records = new RecordSet();
				is.skip(recordSize * offset);
				
				int index = offset;
				while (true) {
					if (recordSize != is.read(buffer)) {
						records.setEof(true);
						break;
					}
					try {
						if (0 < maxRecords && offset + maxRecords <= index) {
							break;
						}
						records.add(readRecord(buffer, format));
					} finally {
						index++;
					}
				}
				
				records.setPath(path);
				records.setFormat(format);
				records.setOffset(offset);
				return records;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
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

	private int getRecordSize(RecordFormat format) {
		try {
			return Integer.parseInt(format.getReaderOptions().get("recordSize"));
		} catch (NumberFormatException e) {
			throw new RuntimeException("BytesReaderのrecordSizeの指定が不正です。", e);
		}
	}
}
