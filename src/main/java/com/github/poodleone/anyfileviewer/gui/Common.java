package com.github.poodleone.anyfileviewer.gui;

import java.awt.Component;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;

import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.utils.GUIUtils;

/**
 * 各画面で共通のコンポーネント.
 *
 */
public class Common {
	/**
	 * ファイルを開くダイアログを表示します.
	 * 
	 * @param parent        親コンポーネント
	 * @param comboBoxModel ファイルの種類のモデル 
	 * @param selectedItem  初期選択のファイルパス
	 * @param consumer      ファイル選択時の処理
	 */
	public static void showFileOpenDialog(Component parent, ComboBoxModel<RecordFormat> comboBoxModel, Path selectedItem, Consumer<FormatAndPath> consumer) {
		JComboBox<RecordFormat> fileTypeCombo = new JComboBox<>(comboBoxModel);
		fileTypeCombo.setName("ファイルの種類");
		Path path = GUIUtils.showFileOpenDialog(parent, "ファイルを開く", selectedItem, fileTypeCombo);
		if (path != null) {
			consumer.accept(new FormatAndPath((RecordFormat) fileTypeCombo.getSelectedItem(), path));
		}
	}

	/**
	 * ファイル選択ダイアログの選択情報を格納するクラスです.
	 *
	 */
	public static class FormatAndPath {
		private RecordFormat recordFormat;
		private Path path;
		
		/**
		 * ファイル選択ダイアログの選択情報を生成します.
		 * 
		 * @param recordFormat ファイルの種類
		 * @param path         ファイルパス
		 */
		public FormatAndPath(RecordFormat recordFormat, Path path) {
			this.recordFormat = recordFormat;
			this.path = path;
		}
		
		/**
		 * @return ファイルの種類
		 */
		public RecordFormat getRecordFormat() {
			return recordFormat;
		}
		
		/**
		 * @return ファイルパス
		 */
		public Path getPath() {
			return path;
		}
	}
}
