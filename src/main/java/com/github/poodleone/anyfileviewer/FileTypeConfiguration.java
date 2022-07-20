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
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.poodleone.anyfileviewer.itemdefinition.HexItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.InnerItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemGroupDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.ItemGroupDefinition.ConditionType;
import com.github.poodleone.anyfileviewer.itemdefinition.MetaItemDefinition;
import com.github.poodleone.anyfileviewer.itemdefinition.StringItemDefinition;
import com.github.poodleone.anyfileviewer.reader.RecordReader;
import com.github.poodleone.anyfileviewer.utils.Validate;

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

	/** データグループ定義ファイルの配置ディレクトリ */
	private List<String> dataGroupFormatsDirs;

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

		// スクリプトの読み込み
		DataParser.initialize();
		getValues(properties, "(?<group>scriptPath\\d+)(?<name>)").forEach(keyValue -> {
			try {
				Path scriptPath = Paths.get(getClass().getClassLoader().getResource(keyValue.value).toURI());
				DataParser.loadScript(scriptPath);
			} catch (URISyntaxException | IOException e) {
				throw new InvalidFileTypeConfigurationException(path, keyValue.key, "スクリプト" + path + "の読み込みで異常が発生しました。", e);
			}
		});

		// レコード形式/データグループ形式定義の読み込み
		dataGroupFormatsDirs = properties.entrySet().stream()
				.filter(p -> p.getKey().toString().matches("dataGroupFormats\\.dir.*"))
				.map(p -> p.getValue().toString())
				.collect(Collectors.toList());
		loadDataGroupDefinitions(dataGroupFormatsDirs);

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

			// 式で評価するメタデータの定義を取得
			Map<String, String> metaItemExpressions = new HashMap<>();
			getValues(properties, "(?<group>" + keyValue.keyGroup + "\\.metaItemExpression)\\.(?<name>.+)").forEach(keyValue2 -> {
				metaItemExpressions.put(keyValue2.keyName, keyValue2.value);
			});

			recordFormatMap.put(name,
					new RecordFormat(name, listItems, dumpLayoutDefinitions, readerClass, readerOptions, metaItemExpressions));
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
		} catch (NullPointerException e) {
			throw new RuntimeException("fileDefinitionsディレクトリが見つかりません。", e);
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
	 * @param dirNames データグループ定義の配置ディレクトリ名
	 */
	private void loadDataGroupDefinitions(List<String> dirNames) {
		dataGroupDefinitionMap.clear();

		dirNames.forEach(dirName -> {
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
		});

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

		Deque<Pair> stack = new LinkedList<>();
		stack.addLast(new Pair("", new ItemGroupDefinition(groupName, new ArrayList<>(), condition, null)));

		for (GroupedKeyValue keyValue : getValues(properties, "(?<group>item\\d+)(?<name>)")
				.collect(Collectors.toList())) {
			if (keyValue.value.matches("^if\\b.*")) {
				String subGroupCondition = keyValue.value.substring(2);
				stack.addLast(new Pair(keyValue.key, new ItemGroupDefinition("", new ArrayList<>(), subGroupCondition, ConditionType.IF)));

			} else if (keyValue.value.matches("elsif\\b.*")) {
				Validate.notNull(stack.peekLast().itemDefinition.getConditionType(),
						() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "elsifに対応するifが見つかりません。"));

				ItemGroupDefinition prevGroup = stack.removeLast().itemDefinition;
				stack.getLast().itemDefinition.getChildren().add(prevGroup);

				String subGroupCondition = keyValue.value.substring(5);
				stack.addLast(new Pair(keyValue.key, new ItemGroupDefinition("", new ArrayList<>(), subGroupCondition, ConditionType.ELSIF)));

			} else if (keyValue.value.matches("else\\b.*")) {
				Validate.notNull(stack.peekLast().itemDefinition.getConditionType(),
						() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "elseに対応するifが見つかりません。"));

				ItemGroupDefinition prevGroup = stack.removeLast().itemDefinition;
				stack.getLast().itemDefinition.getChildren().add(prevGroup);

				stack.addLast(new Pair(keyValue.key, new ItemGroupDefinition("", new ArrayList<>(), null, ConditionType.ELSE)));

			} else if (keyValue.value.matches("endif\\b.*")) {
				Validate.notNull(stack.peekLast().itemDefinition.getConditionType(),
						() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "endifに対応するifが見つかりません。"));

				ItemGroupDefinition prevGroup = stack.removeLast().itemDefinition;
				stack.getLast().itemDefinition.getChildren().add(prevGroup);

			} else {
				List<ItemDefinition> target = stack.getLast().itemDefinition.getChildren();

				String[] values = keyValue.value.split(CSV_SEPARATOR, 2);
				String type = get(values, 0);
				Validate.isTrue(values.length == 2,
						() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "形式が不正です。"));

				if (type.trim().equals("group")) {
					Arrays.stream(values[1].split(CSV_SEPARATOR)).forEach(e -> {
						String name = e.trim();
						URL url = dataGroupFormatsDirs.stream()
								.map(dirName -> getClass().getClassLoader().getResource(dirName + "/" + name + ".properties"))
								.filter(Objects::nonNull)
								.findFirst()
								.orElseThrow(() -> new InvalidFileTypeConfigurationException(path, keyValue.key, name + ".properties が見つかりません。"));
						try {
							loadDataGroupDefinition(Paths.get(url.toURI()));
							target.add(dataGroupDefinitionMap.get(name));
						} catch (URISyntaxException ignore) {
						}
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

				} else if (type.equals("meta")) {
					String[] tmp = values[1].split(CSV_SEPARATOR);
					Validate.isTrue(tmp.length == 2,
							() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "形式が不正です。項目タイプmetaの場合、項目名、項目値の算出式の指定が必要です。"));

					String name = get(tmp, 0);
					String valueExpression = get(tmp, 1);
					target.add(new MetaItemDefinition(name, valueExpression));

				} else if (type.equals("hidden")) {
					String[] tmp = values[1].split(CSV_SEPARATOR);
					Validate.isTrue(tmp.length == 2,
							() -> new InvalidFileTypeConfigurationException(path, keyValue.key, "形式が不正です。項目タイプhiddenの場合、項目名、項目値の算出式の指定が必要です。"));

					String name = get(tmp, 0);
					String valueExpression = get(tmp, 1);
					target.add(new InnerItemDefinition(name, valueExpression));

				} else {
					throw new InvalidFileTypeConfigurationException(path, keyValue.key,
							"形式が不正です。項目タイプはgroup,string,hex,meta,hiddenのいずれかを指定してください。");
				}
			}
		}

		Validate.isTrue(stack.size() == 1,
				() -> new InvalidFileTypeConfigurationException(path, stack.getLast().key, "if/elsifに対応するendifが見つかりません。"));
		dataGroupDefinitionMap.put(mapKey, stack.getLast().itemDefinition);
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

	private class Pair {
		public String key;
		public ItemGroupDefinition itemDefinition;
		public Pair(String key, ItemGroupDefinition itemDefinition) {
			this.key = key;
			this.itemDefinition = itemDefinition;
		}
	}
}
