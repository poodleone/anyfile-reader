package com.github.poodleone.anyfileviewer.itemdefinition;

import com.github.poodleone.anyfileviewer.DataParser;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.utils.ByteUtils;

/**
 * レコード項目の定義です.<br>
 * この項目はレコードから取得したバイナリ値をHEX形式で表現します.
 */
public class HexItemDefinition extends AbstractItemDefinition {
	/** 項目の値の算出式. */
	private String valueExpression;

	/**
	 * レコードの項目の定義を生成します.
	 * 
	 * @param name             項目名
	 * @param lengthExpression 項目長の計算式
	 * @param valueExpression  項目値の計算式(式を使用しない場合はnullを指定します)
	 */
	public HexItemDefinition(String name, String lengthExpression, String valueExpression) {
		super(name, lengthExpression);
		this.valueExpression = valueExpression;
	}

	/**
	 * 項目の値を文字列で取得します.<br>
	 * 項目値の計算式が指定されている場合は、式の評価結果を使います. 式内ではレコードの値を{@code value}変数で参照できます.
	 */
	@Override
	public String toStringValue(Record record, int offset) {
		String value = toHexValue(record, offset);
		if (valueExpression == null) {
			return value;
		} else {
			try {
				return DataParser.eval(record, valueExpression, new DataParser.Param("value", value));
			} catch (RuntimeException e) {
				record.getMetaItems().put("[項目値の計算エラー: " + getName() + "]",
						"計算元値: " + value + ", エラー内容: " + e.toString());
				return "[N/A]";
			}
		}
	}

	@Override
	public String toHexValue(Record record, int offset) {
		Object rawData = record.getRawData();
		if (rawData instanceof byte[]) {
			return ByteUtils.printHexBinary((byte[]) rawData, offset, getLength(record, offset));
		} else {
			return "";
		}
	}
}
