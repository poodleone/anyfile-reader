package com.github.poodleone.anyfileviewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.poodleone.anyfileviewer.itemdefinition.ItemGroupDefinition;
import com.github.poodleone.anyfileviewer.reader.RecordReader;

/**
 * ビューワが読み込むファイルのレコード形式を定義するクラスです.<br>
 * fileDefinitionsディレクトリ下のプロパティファイルの内容をもとにインスタスが生成されます.
 */
public class RecordFormat {
	/** レコード形式の名前. */
	private String name;

	/** 一覧に出力する項目. */
	private List<String> listItems;

	/** ダンプのレイアウト定義リスト. */
	private List<ItemGroupDefinition> dumpLayoutDefinitions;

	/** ファイル読み込みに使用するRecordReaderクラス. */
	private Class<RecordReader> readerClass;

	/** RecordReaderクラスのオプション. */
	private Map<String, String> readerOptions;
	
	/** 式で評価するメタデータの定義. */
	private Map<String, String> metaItemExpressions;

	/**
	 * レコード形式の定義を生成します.
	 * 
	 * @param name                  レコード形式の名前
	 * @param listItems             一覧に出力する項目
	 * @param dumpLayoutDefinitions ダンプのレイアウト定義リスト
	 * @param readerClass           ファイル読み込みに使用するRecordReaderクラス
	 * @param readerOptions         RecordReaderクラスのオプション
	 * @param metaItemExpressions   式で評価するメタデータの定義.
	 */
	public RecordFormat(String name, List<String> listItems, List<ItemGroupDefinition> dumpLayoutDefinitions,
			Class<RecordReader> readerClass, Map<String, String> readerOptions, Map<String, String> metaItemExpressions) {
		this.name = name;
		this.listItems = listItems;
		this.dumpLayoutDefinitions = Collections.unmodifiableList(dumpLayoutDefinitions);
		this.readerClass = readerClass;
		this.readerOptions = readerOptions;
		this.metaItemExpressions = metaItemExpressions;
	}

	/**
	 * @return レコード形式の名前
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return 一覧に出力する項目.
	 */
	public List<String> getListItems() {
		return listItems;
	}

	/**
	 * @return ダンプのレイアウト定義リスト
	 */
	public List<ItemGroupDefinition> getDumpLayoutDefinitions() {
		return dumpLayoutDefinitions;
	}

	/**
	 * @return ファイル読み込みに使用するRecordReaderクラス
	 */
	public Class<RecordReader> getReaderClass() {
		return readerClass;
	}

	/**
	 * @return RecordReaderクラスのオプション
	 */
	public Map<String, String> getReaderOptions() {
		return readerOptions;
	}

	/**
	 * @return 式で評価するメタデータの定義
	 */
	public Map<String, String> getMetaItemExpressions() {
		return metaItemExpressions;
	}
	
	/**
	 * @return レコードのメタデータの項目名のリスト
	 */
	public List<String> getMetaDataNames() {
		List<String> items = new ArrayList<>();
		Matcher m = Pattern.compile("\\(\\?\\<([A-Za-z][A-Za-z0-9]*)\\>").matcher(readerOptions.get("recordPattern"));
		while (m.find()) {
			items.add(m.group(1));
		}
		return items;
	}

	@Override
	public String toString() {
		return name;
	}

}
