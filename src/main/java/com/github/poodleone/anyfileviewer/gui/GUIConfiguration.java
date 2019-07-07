package com.github.poodleone.anyfileviewer.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * GUIの設定を管理するクラス.
 *
 */
public class GUIConfiguration {
	private static GUIConfiguration instance = new GUIConfiguration();
	private List<Path> recentlyUsedFiles = new ArrayList<>(17);
	private List<String> filters = new ArrayList<>();
	private Map<Booleans, Boolean> booleansMap = new EnumMap<>(Booleans.class);
	private Path path = Paths.get("gui.properties");

	/** boolean型のプロパティ名 */
	public enum Booleans {
		/** 高度なフィルタを使用するかどうか */
		extendedFilterEnabled
	};

	/**
	 * プログラム終了時にプロパティファイルを更新する.
	 */
	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			getInstance().save();
		}));
	}

	/**
	 * @return GUIの設定管理クラスのインスタンス
	 */
	public static GUIConfiguration getInstance() {
		return instance;
	}

	/**
	 * 最近使ったファイルのリストを取得します.
	 * 
	 * @return 最近使ったファイルのリスト
	 */
	public List<Path> getRecentlyUsedFiles() {
		return recentlyUsedFiles;
	}

	/**
	 * 最近使ったファイルを追加します.
	 * 
	 * @param path 追加するファイルパス
	 */
	public void addRecentlyUsedFile(Path path) {
		recentlyUsedFiles.remove(path);
		recentlyUsedFiles.add(0, path);
		for (int i = 15; i < recentlyUsedFiles.size(); i++) {
			recentlyUsedFiles.remove(i);
		}
	}

	/**
	 * 保存された検索フィルタのリストを取得します.
	 * 
	 * @return 検索フィルタのリスト
	 */
	public List<String> getFilters() {
		return filters;
	}

	/**
	 * 保存する検索フィルタを追加します.
	 * 
	 * @param filter 追加するフィルタ
	 */
	public void addFilter(String filter) {
		String trimed = filter.trim();
		filters.removeIf(e -> e.equals(trimed));
		filters.add(0, trimed);
	}

	/**
	 * boolean型のプロパティを取得します.
	 * 
	 * @param key プロパティキー
	 * @return プロパティの値
	 */
	public boolean getBoolean(Booleans key) {
		return booleansMap.get(key);
	}

	/**
	 * boolean型のプロパティを設定します.
	 * 
	 * @param key   プロパティキー
	 * @param value プロパティの値
	 */
	public void setBoolean(Booleans key, boolean value) {
		booleansMap.put(key, value);
	}

	/**
	 * GUIの設定を読み込みます.
	 */
	private GUIConfiguration() {
		Properties properties = loadProperties();

		// 最近使用したファイル
		getValues(properties, "(?<group>)(?<name>recentlyUsedFiles\\d+)").forEach(keyValue -> {
			recentlyUsedFiles.add(Paths.get(keyValue.value));
		});
		// フィルタ
		getValues(properties, "(?<group>)(?<name>filters\\d+)").forEach(keyValue -> {
			filters.add(keyValue.value);
		});
		// boolean型プロパティ
		Arrays.stream(Booleans.values()).forEach(key -> {
			booleansMap.put(key, Boolean.parseBoolean(properties.getProperty(key.toString())));
		});
	}

	/**
	 * GUIの設定を保存します.
	 */
	public void save() {
		Properties properties = loadProperties();

		AtomicInteger i = new AtomicInteger();
		// 最近使用したファイル
		recentlyUsedFiles.forEach(
				e -> properties.setProperty(String.format("recentlyUsedFiles%02d", i.getAndIncrement()), e.toString()));
		i.set(0);
		// フィルタ
		filters.forEach(e -> properties.setProperty(String.format("filters%04d", i.getAndIncrement()), e.toString()));
		i.set(0);
		// boolean型プロパティ
		Arrays.stream(Booleans.values()).forEach(key -> {
			properties.setProperty(key.toString(), booleansMap.getOrDefault(key, Boolean.FALSE).toString());
		});

		try {
			properties.store(Files.newBufferedWriter(path, StandardCharsets.UTF_8), "");
		} catch (IOException ignore) {
		}
	}

	/**
	 * プロパティファイルを読み込みます.
	 * 
	 * @return プロパティ
	 */
	private Properties loadProperties() {
		Properties properties = new Properties();
		try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			properties.load(br);
			return properties;
		} catch (NullPointerException e) {
			throw new RuntimeException("プロパティファイル" + path + "が見つかりませんでした。");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@SuppressWarnings("unused")
	private String getStringValue(Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value == null || value.isEmpty()) {
			throw new IllegalArgumentException("プロパティ" + key + "が未設定です。");
		}
		return value;
	}

	@SuppressWarnings("unused")
	private String getStringValue(Properties properties, String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	private Stream<GroupedKeyValue> getValues(Properties properties, String keyPattern) {
		return properties.keySet().stream().filter(key -> key.toString().matches(keyPattern)).sorted().map(key -> {
			Matcher m = Pattern.compile(keyPattern).matcher(key.toString());
			m.find();
			return new GroupedKeyValue(m.group("group"), m.group("name"), properties.get(key).toString());
		});
	}

	private static class GroupedKeyValue {
		@SuppressWarnings("unused")
		public String keyGroup;
		@SuppressWarnings("unused")
		public String keyName;
		public String value;

		public GroupedKeyValue(String group, String name, String value) {
			this.keyGroup = group;
			this.keyName = name;
			this.value = value;
		}
	}
}
