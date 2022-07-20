package com.github.poodleone.anyfileviewer.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import com.github.poodleone.anyfileviewer.Exporter;
import com.github.poodleone.anyfileviewer.FileTypeConfiguration;
import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.utils.GUIUtils;

/**
 * レコード詳細をエクスポートするダイアログ.
 */
public class DetailExporterDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private JTextField inputPathText = new JTextField("", 60);
	private JButton openButton1 = new JButton("開く");

	private JTextField outputPathText = new JTextField("", 60);
	private JButton openButton2 = new JButton("開く");

	private JComboBox<RecordFormat> fileTypeCombo = new JComboBox<>();
	private JTextField delimiterText = new JTextField(",", 3);
	private JTextField filterText = new JTextField();
	private JTextField headerText = new JTextField("\"[No.\" + $(\"[No.]\") + \" \" + $(\"DateTime\") + \"]\"");
	private JTextField fileNameText = new JTextField("\"No.\" + $(\"[No.]\") + \"_\" + $(\"DateTime\") + \".txt\"");

	private JCheckBox separateFilesCheckBox = new JCheckBox("レコードごとにファイルを分ける", true);
	private JCheckBox useExtendedFilterCheckBox = new JCheckBox("高度なフィルタを使用する", false);
	private JButton exportButton = new JButton("エクスポート");

	/**
	 * レコード詳細をエクスポートするダイアログを生成します.
	 *
	 * @param config            ビューワの設定
	 * @param recordFormat      ファイルのレコード形式の初期選択値
	 * @param path              入力元ファイル名の初期選択値
	 * @param columns           簡易フィルタに使用するカラム
	 * @param filter            フィルタの初期選択値
	 * @param useExtendedFilter 高度なフィルタを使用するかどうかの初期選択値
	 */
	public DetailExporterDialog(FileTypeConfiguration config, RecordFormat recordFormat, Path path,
			List<String> columns, String filter, boolean useExtendedFilter) {
		setTitle("詳細をエクスポート");

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
		line4.add(new JLabel("ヘッダの内容:"));
		line4.add(headerText);

		JPanel line5 = new JPanel();
		line5.setLayout(new BoxLayout(line5, BoxLayout.Y_AXIS));
		JPanel line5sub1 = new JPanel();
		line5sub1.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		line5sub1.add(separateFilesCheckBox);
		line5.add(line5sub1);
		JPanel line5sub2 = new JPanel();
		line5sub2.setLayout(new BoxLayout(line5sub2, BoxLayout.X_AXIS));
		line5sub2.add(new JLabel("レコードごとのファイル名:"));
		line5sub2.add(fileNameText);
		line5sub2.setBorder(new EmptyBorder(0, 10, 0, 0));
		line5.add(line5sub2);
		line5.setBorder(new EmptyBorder(10, 0, 10, 0));

		JPanel line6 = new JPanel();
		line6.setLayout(new BoxLayout(line6, BoxLayout.X_AXIS));
		line6.add(new JLabel("フィルタ:"));
		line6.add(filterText);
		filterText.setText(filter);

		JPanel line7 = new JPanel();
		line7.add(useExtendedFilterCheckBox);
		line7.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		line7.add(exportButton);

		JPanel lines = new JPanel();
		lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
		lines.add(line1);
		lines.add(line2);
		lines.add(line3);
		lines.add(line4);
		lines.add(line5);
		lines.add(line6);
		lines.add(line7);
		lines.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(lines, BorderLayout.NORTH);

		// コントロールの挙動を設定
		inputPathText.setText(Objects.toString(path, ""));
		separateFilesCheckBox.addChangeListener(e -> fileNameText.setEnabled(separateFilesCheckBox.isSelected()));
		// - 入力元ファイルパスの開くボタン
		openButton1.addActionListener(e -> {
			// ファイル選択ダイアログを表示(現在の入力値を初期ディレクトリに指定)
			Path oldPath = toPath(inputPathText.getText());
			Common.showFileOpenDialog(this, fileTypeCombo.getModel(), oldPath, e2 -> {
				fileTypeCombo.setSelectedItem(e2.getRecordFormat());
				inputPathText.setText(e2.getPath().toString());
			});
		});
		// - 出力先ファイルパスの開くボタン
		openButton2.addActionListener(e -> {
			// ファイル選択ダイアログを表示(現在の入力値を初期ディレクトリに指定)
			Path oldPath = toPath(outputPathText.getText());
			if (oldPath == null) {
				oldPath = toPath(inputPathText.getText()); // 出力先が未指定なら入力元パスを初期ディレクトリにする
			}
			Path newPath = separateFilesCheckBox.isSelected()
					? GUIUtils.showDirectoryChooserDialog(this, "エクスポート先のディレクトリを指定する", oldPath)
					: GUIUtils.showFileSaveDialog(this, "エクスポート先を指定する", oldPath);
			if (newPath != null) {
				outputPathText.setText(newPath.toString());
			}
		});
		// - エクスポートボタン
		exportButton.addActionListener(e -> {
			Path inputPath = toPath(inputPathText.getText());
			Path outputPath = toPath(outputPathText.getText());
			if (inputPath == null || outputPath == null) {
				JOptionPane.showMessageDialog(this, "入力元または出力先の指定が不正です。", getTitle(), JOptionPane.OK_OPTION);
				return;
			}
			if (separateFilesCheckBox.isSelected()) {
				// レコードごとにファイルを分ける場合
				if (!Files.isDirectory(outputPath)) {
					JOptionPane.showMessageDialog(this,
							"出力先にディレクトリが指定されていません。\nレコードごとにファイルを分ける場合は、出力先にディレクトリを指定してください。", getTitle(),
							JOptionPane.OK_OPTION);
					return;
				} else {
					Exporter.exportDetailsFiles((RecordFormat) fileTypeCombo.getSelectedItem(), inputPath, outputPath,
							columns, delimiterText.getText(), filterText.getText(),
							useExtendedFilterCheckBox.isSelected(), headerText.getText(), fileNameText.getText());
				}
			} else {
				// 全レコードを1ファイルにまとめる場合
				Exporter.exportDetails((RecordFormat) fileTypeCombo.getSelectedItem(), inputPath, outputPath, columns,
						delimiterText.getText(), filterText.getText(), useExtendedFilterCheckBox.isSelected(),
						headerText.getText());
			}
			JOptionPane.showMessageDialog(this, "詳細のエクスポートが完了しました。", getTitle(), JOptionPane.DEFAULT_OPTION, null);
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
