package com.github.poodleone.anyfileviewer.utils;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.function.Consumer;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

/**
 * GUI関連のユーティリティ.
 */
public class GUIUtils {
	/**
	 * UIフォントをまとめて設定します.
	 * 
	 * @param font フォント
	 */
	public static void setUIFont(javax.swing.plaf.FontUIResource font) {
		Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource)
				UIManager.put(key, font);
		}
	}

	/**
	 * コンポーネントの設定を行います.
	 * 
	 * @param c           コンポーネント
	 * @param initializer 設定を行うConsumer
	 * @return 設定されたコンポーネント
	 */
	public static <T> T setup(T c, Consumer<T> initializer) {
		initializer.accept(c);
		return c;
	}

	/**
	 * JMenuのインスタンスを生成し、メニュー項目を追加します.
	 * 
	 * @param text  メニューのテキスト("_"を含む場合、直後の文字をニーモニックに設定します)
	 * @param items 追加するメニュー項目
	 * @return 生成されたJMenuのインスタンス
	 */
	public static JMenu newJMenu(String text, JMenuItem... items) {
		JMenu menu = new JMenu();
		int offsetMnemonic = text.indexOf("_");
		if (offsetMnemonic != -1) {
			menu.setText(text.replaceFirst("_", ""));
			menu.setMnemonic(text.charAt(offsetMnemonic + 1));
		} else {
			menu.setText(text);
		}
		for (JMenuItem item : items) {
			menu.add(item);
		}
		return menu;
	}

	/**
	 * JMenuItemのインスタンスを生成し、ActionListenerを設定します.
	 * 
	 * @param text           メニューのテキスト("_"を含む場合、直後の文字をニーモニックに設定します)
	 * @param actionListener 設定するActionListener(使用しない場合はnullを指定します)
	 * @param keyStroke      アクセラレーションキー(使用しない場合はnullを指定します)
	 * @param items          子メニュー項目
	 * @return 生成されたJMenuItemのインスタンス
	 */
	public static JMenuItem newJMenuItem(String text, ActionListener actionListener, KeyStroke keyStroke,
			JMenuItem... items) {
		JMenuItem menu = new JMenuItem();
		int offsetMnemonic = text.indexOf("_");
		if (offsetMnemonic != -1) {
			menu.setText(text.replaceFirst("_", ""));
			menu.setMnemonic(text.charAt(offsetMnemonic + 1));
		} else {
			menu.setText(text);
		}
		if (actionListener != null) {
			menu.addActionListener(actionListener);
		}
		if (keyStroke != null) {
			menu.setAccelerator(keyStroke);
		}
		for (JMenuItem item : items) {
			menu.add(item);
		}
		return menu;
	}

	/**
	 * JMenuItemのインスタンスを生成し、ActionListenerを設定します.
	 * 
	 * @param text           メニューのテキスト("_"を含む場合、直後の文字をニーモニックに設定します)
	 * @param actionListener 設定するActionListener(使用しない場合はnullを指定します)
	 * @param items          子メニュー項目
	 * @return 生成されたJMenuItemのインスタンス
	 * @see #newJMenuItem(String, ActionListener, KeyStroke, JMenuItem ...)
	 */
	public static JMenuItem newJMenuItem(String text, ActionListener actionListener, JMenuItem... items) {
		return newJMenuItem(text, actionListener, null, items);
	}

	/**
	 * JMenuItemのインスタンスを生成します.
	 * 
	 * @param text  メニューのテキスト("_"を含む場合、直後の文字をニーモニックに設定します)
	 * @param items 子メニュー項目
	 * @return 生成されたJMenuItemのインスタンス
	 * @see #newJMenuItem(String, ActionListener, KeyStroke, JMenuItem ...)
	 */
	public static JMenuItem newJMenuItem(String text, JMenuItem... items) {
		return newJMenuItem(text, null, null, items);
	}

	/**
	 * ファイル選択ダイアログを表示します.
	 * 
	 * @param parent       親コンポーネント
	 * @param title        ダイアログのタイトル
	 * @param selectedItem 初期選択のファイル(初期選択なしの場合はnullを指定します)
	 * @param accessory    アクセサリコンポーネント(使用しない場合はnullを指定します)
	 * @return 選択されたファイルのパス(キャンセルされた場合はnullを返します)
	 */
	public static Path showFileOpenDialog(Component parent, String title, Path selectedItem, JComponent accessory) {
		JFileChooser filechooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			@Override
			public void approveSelection() {
				File file = getSelectedFile();
				if (!file.exists()) {
					String m = String.format("%s\nファイルが見つかりません。ファイル名を確認してください。", file.getName());
					JOptionPane.showConfirmDialog(this, m, getDialogTitle(), JOptionPane.DEFAULT_OPTION,
							JOptionPane.WARNING_MESSAGE);
					return;
				}
				super.approveSelection();
			}
		};
		if (accessory != null) {
			JPanel panel = (JPanel) ((JPanel) filechooser.getComponent(2)).getComponent(2);
			if (!accessory.getName().isEmpty()) {
				JPanel labels = (JPanel) panel.getComponent(0);
				labels.add(Box.createRigidArea(new Dimension(1, 12)));
				labels.add(new JLabel(accessory.getName() + ":"));
			}
			JPanel fields = (JPanel) panel.getComponent(2);
			fields.add(Box.createRigidArea(new Dimension(1, 8)));
			fields.add(accessory);
		}
		if (selectedItem != null) {
			filechooser.setCurrentDirectory(selectedItem.toAbsolutePath().getParent().toFile());
			filechooser.setSelectedFile(selectedItem.toFile());
		}
		filechooser.setDialogTitle(title);
		int selected = filechooser.showOpenDialog(parent);
		if (selected == JFileChooser.APPROVE_OPTION) {
			return filechooser.getSelectedFile().toPath();
		} else {
			return null;
		}
	}

	/**
	 * ディレクトリ選択ダイアログを表示します.
	 * 
	 * @param parent       親コンポーネント
	 * @param title        ダイアログのタイトル
	 * @param selectedItem 初期選択のディレクトリ(初期選択なしの場合はnullを指定します)
	 * @return 選択されたディレクトリのパス(キャンセルされた場合はnullを返します)
	 */
	public static Path showDirectoryChooserDialog(Component parent, String title, Path selectedItem) {
		JFileChooser filechooser = new JFileChooser();
		if (selectedItem != null) {
			filechooser.setCurrentDirectory(selectedItem.toAbsolutePath().toFile());
		}
		filechooser.setDialogTitle(title);
		filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int selected = filechooser.showOpenDialog(parent);
		if (selected == JFileChooser.APPROVE_OPTION) {
			return filechooser.getSelectedFile().toPath();
		} else {
			return null;
		}
	}

	/**
	 * ファイル保存ダイアログを表示します.
	 * 
	 * @param parent       親コンポーネント
	 * @param title        ダイアログのタイトル
	 * @param selectedItem 初期選択のファイル(初期選択なしの場合はnullを指定します)
	 * @return 選択されたファイルのパス(キャンセルされた場合はnullを返します)
	 */
	public static Path showFileSaveDialog(Component parent, String title, Path selectedItem) {
		JFileChooser filechooser = new JFileChooser() {
			private static final long serialVersionUID = 1L;

			@Override
			public void approveSelection() {
				File file = getSelectedFile();
				if (file.exists()) {
					String m = String.format("%s は既に存在します。\n上書きしますか？", file.getAbsolutePath());
					int selection = JOptionPane.showConfirmDialog(this, m, getDialogTitle(), JOptionPane.YES_NO_OPTION);
					if (selection != JOptionPane.YES_OPTION) {
						return;
					}
				}
				super.approveSelection();
			}
		};
		if (selectedItem != null) {
			filechooser.setCurrentDirectory(selectedItem.toAbsolutePath().getParent().toFile());
		}
		filechooser.setDialogTitle(title);
		int selected = filechooser.showSaveDialog(parent);
		if (selected == JFileChooser.APPROVE_OPTION) {
			return filechooser.getSelectedFile().toPath();
		} else {
			return null;
		}
	}

	/**
	 * メッセージダイアログを表示します.
	 * 
	 * @param parent    親コンポーネント
	 * @param title     ダイアログのタイトル
	 * @param message   メッセージ
	 * @param throwable 例外
	 */
	public static void showMessageDialog(Component parent, String title, String message, Throwable throwable) {
		StringBuilder text = new StringBuilder(message);
		if (throwable != null) {
			if (text.length() != 0) {
				text.append("\n");
			}

			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			throwable.printStackTrace(pw);
			pw.flush();
			text.append(sw.toString());
		}
		JTextArea textArea = new JTextArea(text.toString());
		textArea.setColumns(120);
		textArea.setRows(15);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setSize(textArea.getPreferredSize().width, 1);
		JOptionPane.showMessageDialog(parent, new JScrollPane(textArea), title, JOptionPane.PLAIN_MESSAGE);
	}
}
