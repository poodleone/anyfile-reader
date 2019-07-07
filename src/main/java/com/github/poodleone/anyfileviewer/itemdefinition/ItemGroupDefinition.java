package com.github.poodleone.anyfileviewer.itemdefinition;

import java.util.Collections;
import java.util.List;

import com.github.poodleone.anyfileviewer.record.Record;

/**
 * 子項目を持つレコード項目(グループ)の定義です.<br>
 * データグループの設定でif/elsif/elseを使用した場合、条件式つきのグループとして扱われます.
 */
public class ItemGroupDefinition implements ItemDefinition {
	/** グループ名. */
	private String groupName;

	/** グループ内の子項目のリスト. */
	private List<ItemDefinition> children;

	/** このグループを使用するための条件式. */
	private String condition;

	/** 条件式の種類. */
	private ConditionType conditionType;

	/**
	 * 項目グループの定義を生成します.
	 * 
	 * @param groupName     グループ名
	 * @param children      グループ内の子項目のリスト
	 * @param condition     このグループを使用するための条件式(条件なしの場合は空文字列を指定します)
	 * @param conditionType 条件式の種類(条件なしの場合はnullを指定します)
	 */
	public ItemGroupDefinition(String groupName, List<ItemDefinition> children, String condition,
			ConditionType conditionType) {
		this.groupName = groupName;
		this.children = Collections.unmodifiableList(children);
		this.condition = condition;
		this.conditionType = conditionType;
	}

	@Override
	public String getName() {
		return groupName;
	}

	/**
	 * 項目のデータ長を返します. グループには長さはないため、0を返します.
	 */
	@Override
	public int getLength(Record record, int offset) {
		return 0;
	}

	/**
	 * 項目の値を文字列で取得します. グループには値はないため、空文字列を返します.
	 */
	@Override
	public String toStringValue(Record record, int offset) {
		return "";
	}

	/**
	 * 項目の値をHEX形式で取得します. グループには値はないため、空文字列を返します.
	 */
	@Override
	public String toHexValue(Record record, int offset) {
		return "";
	}

	@Override
	public List<ItemDefinition> getChildren() {
		return children;
	}

	/**
	 * このグループを使用するための条件式を取得します.
	 * 
	 * @return 条件式
	 */
	public String getCondition() {
		return condition;
	}

	/**
	 * このグループの条件式の種類(IF/ELSIF/ELSE)を取得します.
	 * 
	 * @return 条件式の種類
	 */
	public ConditionType getConditionType() {
		return conditionType;
	}

	/**
	 * 条件式の種類(IF/ELSIF/ELSE)
	 */
	public static enum ConditionType {
		/** IF */
		IF,
		/** ELSIF */
		ELSIF,
		/** ELSE */
		ELSE
	}
}
