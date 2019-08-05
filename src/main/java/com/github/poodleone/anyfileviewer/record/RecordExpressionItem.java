package com.github.poodleone.anyfileviewer.record;

import com.github.poodleone.anyfileviewer.DataParser;

/**
 * 式で評価する項目.
 */
public class RecordExpressionItem {
	/** この項目が含まれるレコード. */
	private Record record;

	/** 項目の値の算出式. */
	private String valueExpression;
	
	/** 項目の開始位置までのオフセット. */
	private int offset;
	
	/** 項目を削除可能かどうか. */
	private boolean isRemovable;

	/**
	 * 式で評価する項目を生成します.
	 * 
	 * @param record          この項目が含まれるレコード
	 * @param valueExpression 項目値の計算式
	 * @param offset          項目の開始位置までのオフセット
	 * @param isRemovable     項目を削除可能かどうか
	 */
	public RecordExpressionItem(Record record, String valueExpression, int offset, boolean isRemovable) {
		this.record = record;
		this.valueExpression = valueExpression;
		this.offset = offset;
		this.isRemovable = isRemovable;
	}

	@Override
	public String toString() {
		return DataParser.eval(record, valueExpression, new DataParser.Param("offset", offset));
	}
	
	/**
	 * @return 項目を削除可能かどうか
	 */
	public boolean isRemovable() {
		return isRemovable;
	}
}
