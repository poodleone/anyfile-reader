package com.github.poodleone.anyfileviewer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.poodleone.anyfileviewer.reader.RecordReader;
import com.github.poodleone.anyfileviewer.utils.Validate;
import com.github.poodleone.anyfileviewer.itemdefinition.HexItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemGroupDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.StringItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemGroupDefinition.ConditionType;

/**
 * ファイル種類の設定を管理するクラス.
 *
 */
public class FileTypeConfiguration {
	/** 設定プロパティファイルのパス. */
	private Path path;

	/** ファイルのレコード形式のMap(key: ファイル種類名, value: レコード形式). */
	private Map<String, RecordFormat> recordFormatMap = new LinkedHashMap<>();

	/** 各ファイルのデータグループ定義のMap(key: グループ名(拡張子抜きのプロパティファイル名), value: データグループ定義) */
	private Map<String, ItemGroupDefinition> dataGroupDefinitionMap = new LinkedHashMap<>();

	/** CSVデータのセパレータ(要素のクォートに対応). */
	private static String CSV_SEPARATOR = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

	/**
	 * ファイル種類の設定を読み込みます.
	 * 
	 * @param path 読込対象の設定プロパティファイルのパス
	 */
	public FileTypeConfiguration(Path path) {
		this.path = path;
		Properties properties = loadProperties(path);

		// レコード形式/データグループ形式定義の読み込み
		loadDataGroupDefinitions(getStringValue(path, properties, "dataGroupFormats.dir"));

		// ファイル形式の設定
		getValues(properties, "(?<group>fileType\\d+)\\.(?<name>name)").forEach(keyValue -> {
			// RecordReaderクラスの設定を取得
			String key = keyValue.keyGroup + ".readerClass";
			String readerClassName = getStringValue(path, properties, key);
			Class<RecordReader> readerClass;
			try {
				readerClass = forName(readerClassName);
			} catch (ClassNotFoundException | ClassCastException e) {
				throw new InvalidFileTypeConfigurationException(path, key, "RecordReaderのクラス" + readerClassName + "は存在しません。");
			}
			
			// Readerのオプション設定を取得
			Map<String, String> readerOptions = new HashMap<>();
			try {
				readerClass.newInstance().getOptionNames().forEach(option -> readerOptions.put(option,
						getStringValue(path, properties, keyValue.keyGroup + "." + option)));
			} catch (InstantiationException | IllegalAccessException e) {
				throw new AssertionError(e);
			}

			// ファイル種類名と形式を取得
			String name = keyValue.value;
			List<String> listItems = Arrays
					.asList(getStringValue(path, properties, keyValue.keyGroup + ".listItems").split(CSV_SEPARATOR));

			// レコード形式定義のリストを取得
			String dumpLayouts = (String) readerOptions.get("dumpLayouts");
			List<ItemGroupDefinition> dumpLayoutDefinitions;
			if (dumpLayouts != null) {
				dumpLayoutDefinitions = Arrays.stream(dumpLayouts.split(CSV_SEPARATOR)).map(e -> {
					return dataGroupDefinitionMap.get(e.trim());
				}).collect(Collectors.toList());
			} else {
				dumpLayoutDefinitions = Collections.emptyList();
			}

			recordFormatMap.put(name,
					new RecordFormat(name, listItems, dumpLayoutDefinitions, readerClass, readerOptions));
		});
	}

	/**
	 * ファイル種類の設定プロパティファイルをfileDefinitionsディレクトリから検索します.
	 * 
	 * @return 設定プロパティファイルのパスリスト
	 */
	public static List<Path> findPropertyFiles() {
		URL url = FileTypeConfiguration.class.getClassLoader().getResource("fileDefinitions");
		try (Stream<Path> stream = Files.list(Paths.get(url.toURI()))) {
			List<Path> files = stream.filter(path -> path.toString().endsWith(".properties")).sorted()
					.collect(Collectors.toList());
			if (files.size() == 0) {
				throw new RuntimeException("fileDefinitionsディレクトリにファイル設定(*.properties)がありません。");
			}
			return files;
		} catch (IOException | URISyntaxException e) {
			throw new AssertionError(e);
		}
	}

	/**
	 * @return この設定のプロパティファイルのパス
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * @return ファイルのレコード形式のMap(key: ファイル種類名, value: レコード形式)
	 */
	public Map<String, RecordFormat> getRecordFormatMap() {
		return recordFormatMap;
	}

	@SuppressWarnings("unchecked")
	private <T> T forName(String className) throws ClassNotFoundException {
		return (T) Class.forName(className);
	}

	/**
	 * データグループ定義を指定のディレクトリから検索し、読み込みます.<br>
	 * 読み込んだ定義はdataGroupDefinitionMapに追加します.
	 * 
	 * @param dirName データグループ定義の配置ディレクトリ名
	 */
	private void loadDataGroupDefinitions(String dirName) {
		dataGroupDefinitionMap.clear();
		URL url = getClass().getClassLoader().getResource(dirName);
		if (url == null) {
			throw new RuntimeException(dirName + "ディレクトリが見つかりません。");
		} else {
			try (Stream<Path> stream = Files.list(Paths.get(url.toURI()))) {
				stream.filter(path -> path.toString().endsWith(".properties"))
						.forEach(path -> loadDataGroupDefinition(path));
			} catch (IOException | URISyntaxException e) {
				throw new AssertionError(e);
			}
		}
	}

	/**
	 * 個別のデータグループ定義を読み込みます.
	 * 
	 * @param path データグループ定義の配置ディレクトリ名
	 */
	private void loadDataGroupDefinition(Path path) {
		String mapKey = path.getFileName().toString().replaceFirst("\\.properties$", "");
		if (dataGroupDefinitionMap.containsKey(mapKey)) {
			return;
		}

		Properties properties = loadProperties(path);
		String groupName = getStringValue(path, properties, "name", "");
		String condition = getStringValue(path, properties, "condition", null);

		List<ItemDefinition> children = new ArrayList<>();
		List<ItemDefinition> subGroup = null;
		ConditionType conditionType = null;
		String subGroupCondition = null;
		for (GroupedKeyValue keyValue : getValues(properties, "(?<group>item\\d+)(?<name>)")
				.collect(Collectors.toList())) {
			if (keyValue.value.matches("^if\\b.*")) {
				subGroup = new ArrayList<>();
				subGroupCondition = keyValue.value.substring(2);
				conditionType = ConditionType.IF;

			} else if (keyValue.value.matches("elsif\\b")) {
				Validate.notNull(conditionType,
						() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "elsifに対応するifが見つかりません。"));

				children.add(new ItemGroupDefinition(groupName, subGroup, subGroupCondition, conditionType));
				subGroup = new ArrayList<>();
				subGroupCondition = keyValue.value.substring(5);
				conditionType = ConditionType.ELSIF;

			} else if (keyValue.value.matches("else\\b.*")) {
				Validate.notNull(conditionType,
						() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "elseに対応するifが見つかりません。"));

				children.add(new ItemGroupDefinition(groupName, subGroup, subGroupCondition, conditionType));
				subGroup = new ArrayList<>();
				subGroupCondition = null;
				conditionType = ConditionType.ELSE;

			} else if (keyValue.value.matches("endif\\b.*")) {
				Validate.notNull(conditionType,
						() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "endifに対応するifが見つかりません。"));

				children.add(new ItemGroupDefinition(groupName, subGroup, subGroupCondition, conditionType));
				subGroup = null;
				subGroupCondition = null;
				conditionType = null;

			} else {
				List<ItemDefinition> target = subGroup == null ? children : subGroup;

				String[] values = keyValue.value.split(CSV_SEPARATOR, 2);
				String type = get(values, 0);
				Validate.isTrue(values.length == 2,
						() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "形式が不正です。"));

				if (type.trim().equals("group")) {
					Arrays.stream(values[1].split(CSV_SEPARATOR)).skip(1).forEach(e -> {
						String name = e.trim();
						loadDataGroupDefinition(path.getParent().resolve(name + ".properties"));
						target.add(dataGroupDefinitionMap.get(name));
					});

				} else if (type.equals("string")) {
					String[] tmp = values[1].split(CSV_SEPARATOR);
					Validate.isTrue(tmp.length >= 3, () -> new InvalidFileTypeConfigurationException(path, keyValue.key,
							"形式が不正です。項目タイプstringの場合、項目名・文字セット・項目長の指定が必要です。"));

					String name = get(tmp, 0);
					String charset = get(tmp, 1);
					String lengthExpression = get(tmp, 2);
					String valueExpression = get(tmp, 3);
					target.add(new StringItemDefinition(name, lengthExpression, Charset.forName(charset),
							valueExpression));

				} else if (type.equals("hex")) {
					String[] tmp = values[1].split(CSV_SEPARATOR);
					Validate.isTrue(tmp.length >= 2, () -> new InvalidFileTypeConfigurationException(path, keyValue.key,
							"形式が不正です。項目タイプhexの場合、項目名・項目長の指定が必要です。"));

					String name = get(tmp, 0);
					String lengthExpression = get(tmp, 1);
					String valueExpression = get(tmp, 2);
					target.add(new HexItemDefinition(name, lengthExpression, valueExpression));

				} else {
					throw new InvalidFileTypeConfigurationException(path, keyValue.key,
							"形式が不正です。項目タイプはgroup,string,hexのいずれかを指定してください。");
				}
			}
		}

		dataGroupDefinitionMap.put(mapKey, new ItemGroupDefinition(groupName, children, condition, null));
	}

	private String get(String[] array, int index) {
		if (index < array.length) {
			String value = array[index].trim();
			return value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
		} else {
			return null;
		}
	}

	/**
	 * プロパティファイルを読み込みます.
	 * 
	 * @param path プロパティファイル名
	 * @return プロパティ
	 */
	private Properties loadProperties(Path path) {
		Properties properties = new Properties();
		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(br);
			return properties;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private String getStringValue(Path path, Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value == null || value.isEmpty()) {
			throw new InvalidFileTypeConfigurationException(path, key, "値が未設定です。");
		}
		return value;
	}

	private String getStringValue(Path path, Properties properties, String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	private Stream<GroupedKeyValue> getValues(Properties properties, String keyPattern) {
		return properties.keySet().stream().filter(key -> key.toString().matches(keyPattern)).sorted().map(key -> {
			Matcher m = Pattern.compile(keyPattern).matcher(key.toString());
			m.find();
			return new GroupedKeyValue(key.toString(), m.group("group"), m.group("name"),
					properties.get(key).toString());
		});
	}

	/**
	 * グループ化されたプロパティのキーと値.
	 */
	private static class GroupedKeyValue {
		public String key;
		public String keyGroup;
		@SuppressWarnings("unused")
		public String keyName;
		public String value;

		public GroupedKeyValue(String key, String group, String name, String value) {
			this.key = key;
			this.keyGroup = group;
			this.keyName = name;
			this.value = value;
		}
	}

	/**
	 * ファイル種類の設定に問題があったときにスローされる例外です.
	 */
	public static class InvalidFileTypeConfigurationException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		/**
		 * 指定されたエラー詳細メッセージと原因を持つInvalidFileTypeConfigurationExceptionを構築します.<br>
		 * pathとpropertyKeyはエラー詳細メッセージに埋め込まれます.
		 * 
		 * @param path        問題あったファイル種類設定のパス
		 * @param propertyKey 問題のあったプロパティのキー
		 * @param message     詳細メッセージ
		 * @param cause       原因
		 */
		public InvalidFileTypeConfigurationException(Path path, String propertyKey, String message, Throwable cause) {
			super(propertyKey + "の設定が不正です。" + message + " 入力ファイル: " + path.toAbsolutePath(), cause);
		}

		/**
		 * 指定されたエラー詳細メッセージと持つInvalidFileTypeConfigurationExceptionを構築します.<br>
		 * pathとpropertyKeyはエラー詳細メッセージに埋め込まれます.
		 * 
		 * @param path        不正な内容のあったファイル種類設定のパス
		 * @param propertyKey 問題のあったプロパティのキー
		 * @param message     詳細メッセージ
		 */
		public InvalidFileTypeConfigurationException(Path path, String propertyKey, String message) {
			this(path, propertyKey, message, null);
		}

	}
}
