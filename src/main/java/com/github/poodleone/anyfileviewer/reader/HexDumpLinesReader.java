package com.github.poodleone.anyfileviewer.reader;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.poodleone.anyfileviewer.DataParser;
import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.record.AbstractRecord;
import com.github.poodleone.anyfileviewer.record.ErrorRecord;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.utils.ByteUtils;

/**
 * HEX表記のダンプデータを含む1レコード1行形式のファイルのReaderです.
 */
public class HexDumpLinesReader extends LinesReader {
	@Override
	public List<String> getOptionNames() {
		return Arrays.asList("charset", "recordPattern", "dumpLayouts");
	}

	@Override
	protected Record readRecord(String line, RecordFormat format) {
		Matcher m = Pattern.compile(format.getReaderOptions().get("recordPattern")).matcher(line);
		if (m.find()) {
			// レコード生成
			Record record = new AbstractRecord(ByteUtils.parseHexBinary(m.group("DUMP"))) {
				@Override
				public int getLength() {
					return ((byte[]) getRawData()).length;
				}
			};
			record.getInnerItems().put("[format]", format.getName());

			// メタデータの取得
			format.getMetaDataNames().stream().filter(e -> !e.equals("DUMP"))
					.forEach(e -> record.getMetaItems().put(e, m.group(e)));

			// HEX表記のダンプデータをパース
			DataParser.parseRecord(record, format);
			return record;
		} else {
			return new ErrorRecord("不明なレコード形式(適用可能なrecordPatternが見つからない)");
		}
	}

}
