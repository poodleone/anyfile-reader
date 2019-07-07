package com.github.poodleone.anyfileviewer.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.record.RecordSet;

/**
 * ファイルからレコードを読み込むためのクラスのインタフェース.
 */
public interface RecordReader {
	/**
	 * @return 読込処理のオプション名のリスト
	 */
	List<String> getOptionNames();

	/**
	 * ファイルからレコードを読み込みます.
	 * 
	 * @param path       読み込むファイルのパス
	 * @param format     ファイルのレコード形式
	 * @param offset     読み込み開始位置へのオフセット(レコード数)
	 * @param maxRecords 読み込む最大レコード数(0で全件読込)
	 * @return 読み込んだレコード
	 */
	RecordSet load(Path path, RecordFormat format, int offset, int maxRecords);

	/**
	 * ファイルからレコードを読み込み、レコードごとにconsumerを実行します.
	 * 
	 * @param path     読み込むファイルのパス
	 * @param format   ファイルのレコード形式
	 * @param consumer レコードごとに実施する処理
	 */
	void load(Path path, RecordFormat format, Consumer<Record> consumer);
}
