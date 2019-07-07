package com.github.poodleone.anyfileviewer.itemdefinition;

import java.util.List;

import com.github.poodleone.anyfileviewer.record.Record;

/**
 * レコード項目の定義を表すインターフェース.
 */
public interface ItemDefinition {

	/**
	 * 項目名を返します.
	 * 
	 * @return 項目名
	 */
	String getName();

	/**
	 * 項目のデータ長を返します.
	 * 
	 * @param record この項目が含まれるレコード
	 * @param offset レコードデータ中の項目のオフセット
	 * @return 項目のデータ長
	 */
	int getLength(Record record, int offset);

	/**
	 * 項目の値を文字列で取得します.
	 * 
	 * @param record この項目が含まれるレコード
	 * @param offset レコードデータ中の項目のオフセット
	 * @return 項目の値
	 */
	String toStringValue(Record record, int offset);

	/**
	 * 項目の値をHEX形式で取得します.
	 * 
	 * @param record この項目が含まれるレコード
	 * @param offset レコードデータ中の項目のオフセット
	 * @return 項目の値
	 */
	String toHexValue(Record record, int offset);

	/**
	 * 子項目を取得します.
	 * 
	 * @return 子項目のリスト(子項目がない場合は空リストを返します)
	 */
	List<ItemDefinition> getChildren();
}