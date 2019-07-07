package com.github.poodleone.anyfileviewer.record;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * レコードを表すクラスです.
 */
public abstract class AbstractRecord implements Record {
	/** 内部処理用の項目のMap. */
	private Map<String, Object> innerItemMap = new HashMap<>();

	/** メタデータのMap. */
	private Map<String, Object> metaItemMap = new LinkedHashMap<>();

	/** 項目のMap. */
	private Map<String, Object> itemMap = new LinkedHashMap<>();

	/** レコードの元となるデータ. */
	private Object rawData;

	/**
	 * レコードを生成します.
	 * 
	 * @param rawData レコードの元データとなるデータ
	 */
	public AbstractRecord(Object rawData) {
		this.rawData = rawData;
	}

	@Override
	public Map<String, Object> getInnerItems() {
		return innerItemMap;
	}

	@Override
	public Map<String, Object> getMetaItems() {
		return metaItemMap;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getRawData() {
		return (T) rawData;
	}

	@Override
	public Map<String, Object> getItems() {
		return itemMap;
	}

	@Override
	public String getValue(String name) {
		return itemMap.getOrDefault(name, metaItemMap.getOrDefault(name, innerItemMap.getOrDefault(name, "")))
				.toString();
	}

	@Override
	public String getHexValue(String name) {
		Object item = itemMap.get(name);
		return item instanceof RecordItem ? ((RecordItem) item).toHexString() : "";
	}
}
