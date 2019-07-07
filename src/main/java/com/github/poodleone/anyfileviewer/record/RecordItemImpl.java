package com.github.poodleone.anyfileviewer.record;

import com.github.poodleone.anyfileviewer.itemdefinition.ItemDefinition;

/**
 * レコード項目の実装です.
 */
public class RecordItemImpl implements RecordItem {
	/** この項目が含まれるレコード. */
	private Record record;

	/** 項目定義. */
	private ItemDefinition itemDefine;

	/** 項目の開始位置までのオフセット. */
	private int offset;

	/**
	 * レコード項目を生成します.
	 * 
	 * @param record     この項目が含まれるレコード
	 * @param itemDefine 使用する項目定義
	 * @param offset     レコード項目の開始位置までのオフセット
	 */
	public RecordItemImpl(Record record, ItemDefinition itemDefine, int offset) {
		this.record = record;
		this.itemDefine = itemDefine;
		this.offset = offset;
	}

	/**
	 * @return 項目定義
	 */
	public ItemDefinition getItemDefine() {
		return itemDefine;
	}

	/**
	 * @return 項目の開始位置までのオフセット
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * @return 項目の長さ
	 */
	public int getLength() {
		return itemDefine.getLength(record, offset);
	}

	@Override
	public String toHexString() {
		return itemDefine.toHexValue(record, offset);
	}

	@Override
	public String toString() {
		return itemDefine.toStringValue(record, offset);
	}
}