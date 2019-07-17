package com.github.poodleone.anyfileviewer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.github.poodleone.anyfileviewer.itemdefinition.HexItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.InnerItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemGroupDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemGroupDefinition.ConditionType;
import com.github.poodleone.anyfileviewer.itemdefinition.MetaItemDefinition;
import com.github.poodleone.anyfileviewer.record.RecordItemImpl;
import com.github.poodleone.anyfileviewer.record.RecordSet;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.record.RecordExpressionItem;

/**
 * レコードデータのパーサ.
 */
public class DataParser {
	private static ItemDefinition paddingDefinition = new HexItemDefinition("[パディング]", "-1", null);
	private static Scriptable scope;

	/**
	 * パーサを初期化します.
	 */
	public static void initialize() {
		Context cx = Context.enter();
		scope = cx.initStandardObjects();
		Context.exit();
	}

	/**
	 * スクリプトを読み込みます.
	 * 
	 * @param path スクリプトのパス
	 * @throws IOException スクリプトの読み込みで異常が発生した場合
	 */
	public static void loadScript(Path path) throws IOException {
		Context cx = Context.enter();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			cx.evaluateReader(scope, reader, path.toString(), 1, null);
		} finally {
			Context.exit();
		}
	}

	/**
	 * レコードをパースします.
	 * 
	 * @param record レコード
	 * @param format 反映するレコード形式
	 */
	public static void parseRecord(Record record, RecordFormat format) {
		try {
			Optional<ItemGroupDefinition> definitions = format.getDumpLayoutDefinitions().stream() //
					.filter(e -> evalAsBoolean(record, e.getCondition())).findFirst();
			if (definitions.isPresent()) {
				ParserStatus parserStatus = new ParserStatus();
				parseItems("", definitions.get(), record, parserStatus);
				if (parserStatus.offset < record.getLength()) {
					record.getItems().put(paddingDefinition.getName(),
							new RecordItemImpl(record, paddingDefinition, parserStatus.offset));
				}
			} else {
				record.getMetaItems().put("[エラー]", "不明なレコード形式(適用可能なdumpLayoutsが見つからない)");
			}

			format.getMetaItemExpressions().entrySet().forEach(e -> {
				record.getMetaItems().put(e.getKey(), new RecordExpressionItem(record, e.getValue()));
			});
		} catch (RuntimeException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean parseItems(String groupName, ItemDefinition itemDefinition, Record record,
			ParserStatus parserStatus) {

		if (itemDefinition instanceof ItemGroupDefinition) {
			// グループの場合、条件を評価して子項目のパースを行う
			if (!evalAsBoolean(record, ((ItemGroupDefinition) itemDefinition).getCondition())) {
				return false;
			}
			boolean isAdded = false;
			for (ItemDefinition child : itemDefinition.getChildren()) {
				if (child instanceof ItemGroupDefinition) {
					ConditionType type = ((ItemGroupDefinition) child).getConditionType();
					if (isAdded && (type == ConditionType.ELSIF || type == ConditionType.ELSE)) {
						// ELSIF/ELSEブロックの場合、前のIF/ELSIFブロックの条件が満たされていたらこのブロックは評価しない
						continue;
					}
				}
				String itemName = itemDefinition.getName();
				String name;
				if (!groupName.isEmpty() && !itemName.isEmpty()) {
					name = groupName + "." + itemDefinition.getName();
				} else if (itemName.isEmpty()) {
					name = groupName;
				} else {
					name = itemName;
				}
				isAdded = parseItems(name, child, record, parserStatus);
			}
		} else if (itemDefinition instanceof MetaItemDefinition) {
			// meta項目の場合、レコードに内部処理用項目を追加
			String name = itemDefinition.getName();
			record.getMetaItems().put(name,
					new RecordExpressionItem(record, ((MetaItemDefinition) itemDefinition).getValueExpression()));

		} else if (itemDefinition instanceof InnerItemDefinition) {
			// hidden項目の場合、レコードに内部処理用項目を追加
			String name = itemDefinition.getName();
			record.getInnerItems().put(name,
					new RecordExpressionItem(record, ((InnerItemDefinition) itemDefinition).getValueExpression()));

		} else {
			// 項目の場合、レコードに項目を追加
			String name = groupName + "." + itemDefinition.getName();
			record.getItems().put(name, new RecordItemImpl(record, itemDefinition, parserStatus.offset));
			parserStatus.offset += itemDefinition.getLength(record, parserStatus.offset);
		}
		return true;
	}

	/**
	 * メタデータ項目の追加・削除を行います.
	 * 
	 * @param records 対象レコードセット
	 * @param additionalItems   新しいメタデータ項目のリスト
	 */
	public static void updateMetaItems(RecordSet records, List<MetaItemDefinition> additionalItems) {
		for (Record record : records) {
			record.getMetaItems().entrySet().removeIf(e -> {
				return e.getValue() instanceof RecordExpressionItem
						&& ((RecordExpressionItem) e.getValue()).isRemovable();
			});
			additionalItems.forEach(e -> {
				record.getMetaItems().put(e.getName(), new RecordExpressionItem(record, e.getValueExpression(), true));
			});
		}
	}

	/**
	 * 式を真偽値で評価します.
	 * 
	 * @param record     評価対象のレコード
	 * @param expression 式
	 * @param params     パラメータ
	 * @return 評価結果
	 */
	public static boolean evalAsBoolean(Record record, String expression, Param... params) {
		try {
			return expression == null || expression.isEmpty() || !"false".equals(eval(record, expression, params));
		} catch (RuntimeException e) {
			return true;
		}
	}

	/**
	 * 式を評価します.
	 * 
	 * @param record     評価対象のレコード
	 * @param expression 式
	 * @param params     パラメータ
	 * @return 評価結果
	 */
	public static String eval(Record record, String expression, Param... params) {
		Context cx = Context.enter();
		try {
			ScriptableObject.putProperty(scope, "rec", Context.javaToJS(record, scope));
			for (Param param : params) {
				ScriptableObject.putProperty(scope, param.key, Context.javaToJS(param.value, scope));
			}
			cx.evaluateString(scope, "var $ = function(name) { return rec.getValue(name); }", "", 1, null);
			cx.evaluateString(scope, "var $hex = function(name) { return rec.getHexValue(name) }", "", 1, null);

			Object result = cx.evaluateString(scope, expression, "", 1, null);
			return Context.toString(result);
		} finally {
			Context.exit();
		}
	}

	/**
	 * 式を評価します.
	 * 
	 * @param record     評価対象のレコード
	 * @param expression 式
	 * @param returnType 戻り値の型
	 * @param params     パラメータ
	 * @return 評価結果
	 */
	@SuppressWarnings("unchecked")
	public static <T> T eval(Record record, String expression, Class<T> returnType, Param... params) {
		Context cx = Context.enter();
		try {
			Scriptable scope = cx.initStandardObjects();
			ScriptableObject.putProperty(scope, "rec", Context.javaToJS(record, scope));
			for (Param param : params) {
				ScriptableObject.putProperty(scope, param.key, Context.javaToJS(param.value, scope));
			}
			cx.evaluateString(scope, "var $ = function(name) { return rec.getValue(name); }", "", 1, null);
			cx.evaluateString(scope, "var $hex = function(name) { return rec.getHexValue(name) }", "", 1, null);

			Object result = cx.evaluateString(scope, expression, "", 1, null);
			return (T) Context.jsToJava(result, returnType);
		} finally {
			Context.exit();
		}

	}

	private static class ParserStatus {
		public int offset = 0;

		public ParserStatus() {
			offset = 0;
		}
	}

	/**
	 * 式のパラメータ
	 */
	public static class Param {
		private String key;
		private Object value;

		/**
		 * パラメータを生成します.
		 * 
		 * @param key   パラメータ名
		 * @param value パラメータの値
		 */
		public Param(String key, Object value) {
			this.key = key;
			this.value = value;
		}
	}
}
