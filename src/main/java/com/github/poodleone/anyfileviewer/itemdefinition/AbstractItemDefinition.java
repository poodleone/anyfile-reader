package com.github.poodleone.anyfileviewer.itemdefinition;

import java.util.Collections;
import java.util.List;

import com.github.poodleone.anyfileviewer.DataParser;
import com.github.poodleone.anyfileviewer.record.Record;

/**
 * レコード項目の定義を表す抽象クラス.<br>
 * レコード項目の基本的な挙動を設定しています.
 */
public abstract class AbstractItemDefinition implements ItemDefinition {
	/** 項目名. */
	protected String name;

	/** 項目長. */
	protected int length;

	/** 項目長の計算式(固定長項目の場合はnull). */
	protected String lengthExpression;

	/**
	 * レコードの項目の定義を生成します.
	 * 
	 * @param name             項目名
	 * @param lengthExpression 項目長の計算式("-1"を指定した場合、長さは項目のoffsetからレコード末尾までになります)
	 */
	public AbstractItemDefinition(String name, String lengthExpression) {
		this.name = name;
		if (lengthExpression.matches("^[+-]?\\d+$")) {
			// 計算式が数値だけなら固定長として扱う
			this.length = Integer.parseInt(lengthExpression);
		} else {
			this.lengthExpression = lengthExpression;
		}
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * 項目のデータ長を返します.<br>
	 * 可変長項目の場合、データ長は式の評価結果が使用されます.
	 */
	@Override
	public int getLength(Record record, int offset) {
		if (lengthExpression == null) {
			// 固定長の場合
			return length != -1 ? length : record.getLength() - offset;
		} else {
			// 可変長の場合
			try {
				return Integer.parseInt(DataParser.eval(record, lengthExpression));
			} catch (RuntimeException e) {
				record.getMetaItems().put("[項目長の計算エラー: " + getName() + "]", e.toString());
				return 0;
			}
		}
	}

	@Override
	public List<ItemDefinition> getChildren() {
		return Collections.emptyList();
	}
}
