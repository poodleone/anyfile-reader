package com.github.poodleone.anyfileviewer.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.record.AbstractRecord;
import com.github.poodleone.anyfileviewer.record.ErrorRecord;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.record.RecordSet;

/**
 * 1レコード1行形式のファイルのReaderです.
 *
 */
public class LinesReader implements RecordReader {

	@Override
	public List<String> getOptionNames() {
		return Arrays.asList("charset", "recordPattern");
	}

	@Override
	public void load(Path path, RecordFormat format, Consumer<Record> consumer) {
		Charset charset = Charset.forName(format.getReaderOptions().get("charset"));
		try (BufferedReader br = Files.newBufferedReader(path, charset)) {
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				consumer.accept(readRecord(line, format));
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public RecordSet load(Path path, RecordFormat format, int offset, int maxRecords) {
		Charset charset = Charset.forName(format.getReaderOptions().get("charset"));
		if (offset == -1) {
			offset = getLastPageOffset(path, charset, maxRecords);
		}
		try (BufferedReader br = Files.newBufferedReader(path, charset)) {
			RecordSet records = new RecordSet();
			int index = 0;
			while (true) {
				String line = br.readLine();
				if (line == null) {
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
					records.add(readRecord(line, format));
				} finally {
					index++;
				}

			}
			records.setPath(path);
			if (records.eof() && index != 0 && records.size() == 0) {
				// offsetがファイルの範囲外の場合はファイル末尾までを再読み込み
				return load(path, format, -1, maxRecords);
			}
			records.setFormat(format);
			records.setOffset(offset);
			return records;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private int getLastPageOffset(Path path, Charset charset, int maxRecords) {
		int count = 0;
		try (BufferedReader br = Files.newBufferedReader(path, charset)) {
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				count++;
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return (count / maxRecords) * maxRecords;
	}

	protected Record readRecord(String line, RecordFormat format) {
		Matcher m = Pattern.compile(format.getReaderOptions().get("recordPattern")).matcher(line);
		if (m.find()) {
			// レコード生成
			Record record = new AbstractRecord(null) {
				private int length = line.length();

				@Override
				public int getLength() {
					return length;
				}
			};
			// 項目をレコードにセット
			record.getInnerItems().put("[format]", format.getName());
			format.getMetaDataNames().stream().forEach(e -> record.getMetaItems().put(e, m.group(e)));
			return record;
		} else {
			return new ErrorRecord("不明なレコード形式(適用可能なrecordPatternが見つからない)");
		}
	}

}
