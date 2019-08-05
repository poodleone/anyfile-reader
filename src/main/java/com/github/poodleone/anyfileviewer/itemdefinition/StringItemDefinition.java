package com.github.poodleone.anyfileviewer.itemdefinition;

import java.nio.charset.Charset;

import com.github.poodleone.anyfileviewer.DataParser;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.utils.ByteUtils;

/**
 * レコード項目の定義です.<br>
 * この項目はレコードから取得した固定長文字列を項目の値として扱います.<br>
 * 項目値の計算式(valueExpression)が指定されている場合は、式の評価結果を項目の値に使用します.
 */
public class StringItemDefinition extends AbstractItemDefinition {
	/** 項目の値の文字セット. */
	private Charset charset;

	/** 項目の値の算出式. */
	private String valueExpression;

	/**
	 * レコードの項目の定義を生成します.
	 * 
	 * @param name             項目名
	 * @param lengthExpression 項目長の計算式
	 * @param charset          項目の値の文字セット
	 * @param valueExpression  項目値の計算式(式を使用しない場合はnullを指定します)
	 */
	public StringItemDefinition(String name, String lengthExpression, Charset charset, String valueExpression) {
		super(name, lengthExpression);
		this.charset = charset;
		this.valueExpression = valueExpression;
	}

	/**
	 * @return 項目の値の文字セット
	 */
	public Charset getCharset() {
		return charset;
	}
	
	@Override
	public String toRawStringValue(Record record, int offset) {
		Object rawData = record.getRawData();
		try {
			// 項目長を取得
			int length = getLength(record, offset);
			int remain = ((byte[]) rawData).length - offset;
			if (length > remain) {
				// 長さが足りない場合は補正
				length = Integer.max(remain, 0);
			}

			// 項目の値を取得
			if (rawData instanceof byte[]) {
				return new String((byte[]) rawData, offset, length, charset);
			} else {
				return rawData.toString().substring(offset, offset + length);
			}
		} catch (IndexOutOfBoundsException | NullPointerException e) {
			return "";
		}
	}

	/**
	 * 項目の値を文字列で取得します.<br>
	 * 項目値の計算式が指定されている場合は、式の評価結果を使います. 式内ではレコードの値を{@code value}変数で参照できます.
	 */
	@Override
	public String toStringValue(Record record, int offset) {
		String value = toRawStringValue(record, offset);
		if (valueExpression == null) {
			return value;
		} else {
			try {
				return DataParser.eval(record, valueExpression, new DataParser.Param("value", value), new DataParser.Param("offset", offset));
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
		try {
			if (rawData instanceof byte[]) {
				return ByteUtils.printHexBinary((byte[]) rawData, offset, getLength(record, offset));
			} else {
				byte[] bytes = toRawStringValue(record, offset).getBytes(charset);
				return ByteUtils.printHexBinary(bytes, 0, bytes.length);
			}
			// NOTE: printHexBinaryは長さの補正は不要(IndexOutOfBoundsExceptionにならない)
		} catch (IndexOutOfBoundsException | NullPointerException e) {
			return "";
		}
	}
}
