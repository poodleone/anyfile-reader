package com.github.poodleone.anyfileviewer.itemdefinition;

import java.util.Collections;
import java.util.List;

import com.github.poodleone.anyfileviewer.DataParser;
import com.github.poodleone.anyfileviewer.record.Record;

/**
 * メタデータ用のレコード項目の定義です.<br>
 * 式の評価結果を項目の値に使用します.
 */
public class MetaItemDefinition implements ItemDefinition {
	/** 項目名. */
	private String name;

	/** 項目の値の算出式. */
	private String valueExpression;

	/**
	 * レコードの項目の定義を生成します.
	 * 
	 * @param name             項目名
	 * @param valueExpression  項目値の計算式
	 */
	public MetaItemDefinition(String name, String valueExpression) {
		this.name = name;
		this.valueExpression = valueExpression;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getLength(Record record, int offset) {
		return 0;
	}

	@Override
	public String toStringValue(Record record, int offset) {
		return DataParser.eval(record, valueExpression);
	}

	@Override
	public String toHexValue(Record record, int offset) {
		return "";
	}

	@Override
	public List<ItemDefinition> getChildren() {
		return Collections.emptyList();
	}
	
	/**
	 * @return 項目の値の算出式
	 */
	public String getValueExpression() {
		return valueExpression;
	}
}
