package com.github.poodleone.anyfileviewer.record;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * エラー処理用のレコードです.<br>
 * レコードの生成エラーの場合に使用します.
 */
public class ErrorRecord implements Record {
	/** 内部処理用の項目のMap. */
	private Map<String, Object> innerItemMap = new HashMap<>();

	/** メタデータのMap. */
	private Map<String, Object> metaItemMap = new LinkedHashMap<>();

	/**
	 * エラーレコードを生成します.
	 * 
	 * @param message エラーメッセージ
	 */
	public ErrorRecord(String message) {
		metaItemMap.put("[エラー]", message);
	}

	@Override
	public Map<String, Object> getInnerItems() {
		return innerItemMap;
	}

	@Override
	public Map<String, Object> getMetaItems() {
		return metaItemMap;
	}

	/**
	 * 項目のMapを取得します. エラーレコードは項目が存在しないため、空のMapを返します.
	 */
	@Override
	public Map<String, Object> getItems() {
		return Collections.emptyMap();
	}

	/**
	 * レコードの長さを取得します. エラーレコードは長さが不明なため、0を返します.
	 */
	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public <T> T getRawData() {
		return null;
	}

	@Override
	public String getValue(String name) {
		return metaItemMap.getOrDefault(name, innerItemMap.getOrDefault(name, "")).toString();
	}
	
	@Override
	public String getHexValue(String name) {
		return "";
	}

	@Override
	public String getRawStringValue(String name) {
		return "";
	}

}
