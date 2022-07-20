package com.github.poodleone.anyfileviewer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.record.RecordItemImpl;

/**
 * ファイルの解析結果をエクスポートするクラスです.
 */
public class Exporter {
	/**
	 * 一覧をエクスポートします.
	 *
	 * @param recordFormat      ファイルのレコード形式
	 * @param inputPath         入力ファイルパス
	 * @param outputPath        出力ファイルパス
	 * @param columns           一覧に出力するカラム名のリスト
	 * @param delimiter         レコードの項目のセパレータ
	 * @param filter            フィルタ
	 * @param useExtendedFilter 高度なフィルタを使用するかどうか. trueの場合高度なフィルタを使用します.
	 */
	public static void exportList(RecordFormat recordFormat, Path inputPath, Path outputPath, List<String> columns,
			String delimiter, String filter, boolean useExtendedFilter) {
		try (BufferedWriter br = Files.newBufferedWriter(outputPath)) {
			br.write(String.join(delimiter, columns));
			br.write(System.lineSeparator());

			AtomicInteger i = new AtomicInteger();
			recordFormat.getReaderClass().newInstance().load(inputPath, recordFormat, record -> {
				record.getInnerItems().put("[No.]", Integer.valueOf(i.incrementAndGet()));
				if (!filter.isEmpty() && !testFilter(record, columns, filter, useExtendedFilter)) {
					return;
				}

				List<String> r = new ArrayList<>(columns.size());
				columns.forEach(column -> r.add(quote(record.getValue(column))));
				try {
					br.write(String.join(delimiter, r));
					br.write(System.lineSeparator());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * レコード詳細を1ファイルにまとめてエクスポートします.
	 *
	 * @param recordFormat      ファイルのレコード形式
	 * @param inputPath         入力ファイルパス
	 * @param outputPath        出力ファイルパス
	 * @param columns           一覧に出力するカラム名のリスト
	 * @param delimiter         レコードの項目のセパレータ
	 * @param filter            フィルタ
	 * @param useExtendedFilter 高度なフィルタを使用するかどうか. trueの場合高度なフィルタを使用します.
	 * @param headerExpression  レコードのヘッダをこの式から生成します
	 */
	public static void exportDetails(RecordFormat recordFormat, Path inputPath, Path outputPath, List<String> columns,
			String delimiter, String filter, boolean useExtendedFilter, String headerExpression) {
		try (BufferedWriter br = Files.newBufferedWriter(outputPath)) {

			AtomicInteger i = new AtomicInteger();
			recordFormat.getReaderClass().newInstance().load(inputPath, recordFormat, record -> {
				record.getInnerItems().put("[No.]", Integer.valueOf(i.incrementAndGet()));
				if (!filter.isEmpty() && !testFilter(record, columns, filter, useExtendedFilter)) {
					return;
				}
				try {
					br.write(formatDetail(record, headerExpression, delimiter));
					br.write(System.lineSeparator());
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * レコード詳細を1レコード1ファイルでエクスポートします.
	 *
	 * @param recordFormat       ファイルのレコード形式
	 * @param inputPath          入力ファイルパス
	 * @param outputDirPath      出力ディレクトリパス
	 * @param columns            一覧に出力するカラム名のリスト
	 * @param delimiter          レコードの項目のセパレータ
	 * @param filter             フィルタ
	 * @param useExtendedFilter  高度なフィルタを使用するかどうか. trueの場合高度なフィルタを使用します.
	 * @param headerExpression   レコードのヘッダをこの式から生成します
	 * @param fileNameExpression 出力ファイル名をこの式から生成します
	 */
	public static void exportDetailsFiles(RecordFormat recordFormat, Path inputPath, Path outputDirPath,
			List<String> columns, String delimiter, String filter, boolean useExtendedFilter, String headerExpression,
			String fileNameExpression) {

		try {
			AtomicInteger i = new AtomicInteger();
			recordFormat.getReaderClass().newInstance().load(inputPath, recordFormat, record -> {
				record.getInnerItems().put("[No.]", Integer.valueOf(i.incrementAndGet()));
				if (!filter.isEmpty() && !testFilter(record, columns, filter, useExtendedFilter)) {
					return;
				}
				try {
					String fileName = removeInvalidFileNameChars(DataParser.eval(record, fileNameExpression));
					Path outputPath = outputDirPath.resolve(fileName);
					Files.write(outputPath, Arrays.asList(formatDetail(record, headerExpression, delimiter)));
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean testFilter(Record record, List<String> columns, String filter, boolean useExtendedFilter) {
		if (useExtendedFilter) {
			return DataParser.evalAsBoolean(record, filter);
		} else {
			for (String column : columns) {
				if (!column.equals("[No.]") && record.getValue(column).contains(filter))
					return true;
			}
			return false;
		}
	}

	private static String formatDetail(Record record, String headerExpression, String delimiter) {
		StringBuilder sb = new StringBuilder(DataParser.eval(record, headerExpression)).append(System.lineSeparator());
		sb.append(String.join(",", "分類", "No.", "項目名", "位置", "長さ", "値", "値(HEX)")).append(System.lineSeparator());

		AtomicInteger no = new AtomicInteger();
		record.getMetaItems().entrySet().stream().forEach(e -> {
			sb.append(String.join(delimiter,
					"メタ情報",
					Integer.toString(no.incrementAndGet()),
					quote(e.getKey()),
					"",
					"",
					quote(e.getValue().toString()),
					"")).append(System.lineSeparator());
		});

		no.set(0);
		record.getItems().entrySet().stream().forEach(e -> {
			Object v = e.getValue();
			String offset = "", length = "", hex = "";
			if (v instanceof RecordItemImpl) {
				offset = Integer.toString(((RecordItemImpl) v).getOffset());
				length = Integer.toString(((RecordItemImpl) v).getLength());
				hex = ((RecordItemImpl) v).toHexString();
			}
			sb.append(String.join(delimiter,
					"データ",
					Integer.toString(no.incrementAndGet()),
					quote(e.getKey()),
					offset,
					length,
					quote(v.toString()),
					hex)).append(System.lineSeparator());
			;
		});

		return sb.toString();
	}

	private static String quote(String text) {
		return '"' + text.replaceAll("\0", "").replaceAll("\"", "\"\"") + '"';
	}

	private static String removeInvalidFileNameChars(String fileName) {
		return fileName.replaceAll("[\u0001-\u001f<>:\"/\\\\|?*\u007f]+", "");
	}
}
