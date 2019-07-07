package com.github.poodleone.anyfileviewer.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import com.github.poodleone.anyfileviewer.FileTypeConfiguration;
import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.Exporter;
import com.github.poodleone.anyfileviewer.utils.GUIUtils;

/**
 * レコードの一覧をエクスポートするダイアログ.
 */
public class ListExporterDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private JTextField inputPathText = new JTextField("", 60);
	private JButton openButton1 = new JButton("開く");

	private JTextField outputPathText = new JTextField("", 60);
	private JButton openButton2 = new JButton("開く");

	private JComboBox<RecordFormat> fileTypeCombo = new JComboBox<>();
	private JTextField delimiterText = new JTextField(",", 3);
	private JTextField columnsText = new JTextField();
	private JTextField filterText = new JTextField();

	private JCheckBox useExtendedFilterCheckBox = new JCheckBox("高度なフィルタを使用する", false);
	private JButton exportButton = new JButton("エクスポート");

	/**
	 * レコード一覧をエクスポートするダイアログを生成します.
	 * 
	 * @param config            ビューワの設定
	 * @param recordFormat      ファイルのレコード形式の初期選択値
	 * @param path              入力元ファイル名の初期選択値
	 * @param columns           簡易フィルタに使用するカラム
	 * @param filter            フィルタの初期選択値
	 * @param useExtendedFilter 高度なフィルタを使用するかどうかの初期選択値
	 */
	public ListExporterDialog(FileTypeConfiguration config, RecordFormat recordFormat, Path path, List<String> columns,
			String filter, boolean useExtendedFilter) {
		setTitle("一覧をエクスポート");

		// コントロールを配置
		JPanel line1 = new JPanel();
		line1.setLayout(new BoxLayout(line1, BoxLayout.X_AXIS));
		line1.add(new JLabel("入力元:"));
		line1.add(openButton1);
		line1.add(inputPathText);

		JPanel line2 = new JPanel();
		line2.setLayout(new BoxLayout(line2, BoxLayout.X_AXIS));
		line2.add(new JLabel("出力先:"));
		line2.add(openButton2);
		line2.add(outputPathText);

		JPanel line3 = new JPanel();
		line3.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		line3.add(new JLabel("ファイルの種類:"));
		line3.add(fileTypeCombo);
		line3.add(new JLabel(" 区切文字:"));
		line3.add(delimiterText);
		config.getRecordFormatMap().entrySet().forEach(e -> fileTypeCombo.addItem(e.getValue()));
		fileTypeCombo.setSelectedItem(recordFormat);

		JPanel line4 = new JPanel();
		line4.setLayout(new BoxLayout(line4, BoxLayout.X_AXIS));
		line4.add(new JLabel("出力項目:"));
		line4.add(columnsText);
		columnsText.setText(String.join(",", columns));

		JPanel line5 = new JPanel();
		line5.setLayout(new BoxLayout(line5, BoxLayout.X_AXIS));
		line5.add(new JLabel("フィルタ:"));
		line5.add(filterText);
		filterText.setText(filter);

		JPanel line6 = new JPanel();
		line6.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		line6.add(useExtendedFilterCheckBox);
		line6.add(exportButton);

		JPanel lines = new JPanel();
		lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
		lines.add(line1);
		lines.add(line2);
		lines.add(line3);
		lines.add(line4);
		lines.add(line5);
		lines.add(line6);
		lines.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(lines, BorderLayout.NORTH);

		// コントロールの挙動を設定
		inputPathText.setText(Objects.toString(path, ""));
		// - 入力元ファイルパスの開くボタン
		openButton1.addActionListener(e -> {
			// ファイル選択ダイアログを表示(現在の入力値を初期ディレクトリに指定)
			Path oldPath = toPath(inputPathText.getText());
			Path newPath = GUIUtils.showFileOpenDialog(this, "ファイルを開く", oldPath);
			if (newPath != null) {
				inputPathText.setText(newPath.toString());
			}
		});
		// - 出力先ファイルパスの開くボタン
		openButton2.addActionListener(e -> {
			// ファイル選択ダイアログを表示(現在の入力値を初期ディレクトリに指定)
			Path oldPath = toPath(outputPathText.getText());
			if (oldPath == null) {
				oldPath = toPath(inputPathText.getText()); // 出力先が未指定なら入力元パスを初期ディレクトリにする
			}
			Path newPath = GUIUtils.showFileSaveDialog(this, "エクスポート先を指定する", oldPath);
			if (newPath != null) {
				outputPathText.setText(newPath.toString());
			}
		});
		// - エクスポートボタン
		exportButton.addActionListener(e -> {
			Path inputPath = toPath(inputPathText.getText());
			Path outputPath = toPath(outputPathText.getText());
			if (inputPath != null || outputPath != null) {
				JOptionPane.showMessageDialog(this, "出力先または出力先の指定が不正です。", getTitle(), JOptionPane.OK_OPTION);
				return;
			}
			Exporter.exportList((RecordFormat) fileTypeCombo.getSelectedItem(), inputPath, outputPath,
					Arrays.asList(columnsText.getText().split(",")), delimiterText.getText(), filterText.getText(),
					useExtendedFilterCheckBox.isSelected());
			JOptionPane.showMessageDialog(this, "一覧のエクスポートが完了しました。", getTitle(), JOptionPane.DEFAULT_OPTION, null);
		});
		pack();
		this.setModal(true);
	}

	private static Path toPath(String path) {
		try {
			return Paths.get(path);
		} catch (InvalidPathException e) {
			return null;
		}
	}
}
