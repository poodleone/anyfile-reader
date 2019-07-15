package com.github.poodleone.anyfileviewer.gui;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import com.github.poodleone.anyfileviewer.FileTypeConfiguration;
import com.github.poodleone.anyfileviewer.RecordFormat;
import com.github.poodleone.anyfileviewer.DataParser;
import com.github.poodleone.anyfileviewer.gui.GUIConfiguration.Booleans;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.record.RecordSet;
import com.github.poodleone.anyfileviewer.utils.AutoFitTableHeader;
import com.github.poodleone.anyfileviewer.utils.DropDownButton;
import com.github.poodleone.anyfileviewer.utils.GUIUtils;
import com.github.poodleone.anyfileviewer.utils.IntegerInputVerifier;

/**
 * メインウィンドウ.
 */
public class MainWindow extends JFrame {
	private static final long serialVersionUID = 1L;

	private FileTypeConfiguration config;
	private RecordSet records = new RecordSet();

	private JTable table;
	private TableModel tableModel = new TableModel();
	private AutoFitTableHeader tableHeader;
	private TableRowSorter<TableModel> sorter;

	private JMenuItem recentlyUsedFilesMenu = GUIUtils.newJMenu("最近使ったファイル(_F)");
	private JMenuItem propertiesSelectorMenu = GUIUtils.newJMenu("ファイル設定を読込");
	private JTextField maxRowsText = GUIUtils.setup(new JTextField("100", 4),
			c -> c.setInputVerifier(new IntegerInputVerifier(1, Integer.MAX_VALUE)));
	private JTextField offsetFromText = GUIUtils.setup(new JTextField(4),
			c -> c.setInputVerifier(new IntegerInputVerifier(1, Integer.MAX_VALUE)));
	private JTextField offsetToText = GUIUtils.setup(new JTextField(4), c -> c.setEditable(false));
	private JButton topButton = new JButton("|<");
	private JButton prevButton = new JButton("<<");
	private JButton nextButton = new JButton(">>");
	private JButton tailButton = new JButton(">|");

	private JComboBox<RecordFormat> fileTypeCombo = new JComboBox<>();

	private JComboBox<String> filterText = new JComboBox<>();
	private JPopupMenu filterMenu = new JPopupMenu();
	private DropDownButton filterMenuButton = new DropDownButton("フィルタ設定", filterMenu);
	private JCheckBoxMenuItem filterModeCheckBoxMenuItem = new JCheckBoxMenuItem("高度なフィルタを有効にする", false);

	/**
	 * メインウィンドウを生成します.
	 */
	public MainWindow() {
		super();
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			GUIUtils.showMessageDialog(this, "AnyfileViewer", "例外が発生しました。", e);
			e.printStackTrace();
		});
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		initialize();
		pack();

		updatePropertiesSelectorMenu();
	}

	private void reloadFile() {
		if (records.getPath() != null) {
			openFile(null, records.getPath());
		}
	}

	private void openFile() {
		Common.showFileOpenDialog(this, fileTypeCombo.getModel(), records.getPath(), e -> {
			openFile(e.getRecordFormat(), e.getPath());
		});
	}

	private void openFile(RecordFormat recordFormat, Path path) {
		try {
			Path oldPath = records.getPath();
			RecordFormat oldFormat = records.getFormat();
			
			if (recordFormat == null) {
				recordFormat = (RecordFormat) fileTypeCombo.getSelectedItem();
			}
			int recordOffset = offsetFromText.getText().isEmpty() ? 0 : Integer.parseInt(offsetFromText.getText()) - 1;
			int maxRows = Integer.parseInt(maxRowsText.getText());
			records = recordFormat.getReaderClass().newInstance().load(path, recordFormat, recordOffset, maxRows);

			GUIConfiguration.getInstance().addRecentlyUsedFile(records.getPath());
			if (oldFormat == records.getFormat()) {
				tableModel.fireTableDataChanged();

			} else {
				tableModel.columnNames.clear();
				tableModel.columnNames.add("[No.]");
				tableModel.columnNames.addAll(records.getFormat().getListItems());
				tableModel.fireTableStructureChanged();
				sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
			}
			if (oldPath != path || oldFormat != records.getFormat()) {
				tableHeader.sizeWidthToFitData();
			}
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException(e);
		} finally {
			updateControls();
		}
	}

	private void exportList() {
		RecordFormat recordFormat = (RecordFormat) fileTypeCombo.getSelectedItem();
		new ListExporterDialog(config, recordFormat, records.getPath(), tableModel.columnNames,
				filterText.getEditor().getItem().toString(), filterModeCheckBoxMenuItem.isSelected()).setVisible(true);
	}

	private void exportDetails() {
		RecordFormat recordFormat = (RecordFormat) fileTypeCombo.getSelectedItem();
		new DetailExporterDialog(config, recordFormat, records.getPath(), tableModel.columnNames,
				filterText.getEditor().getItem().toString(), filterModeCheckBoxMenuItem.isSelected()).setVisible(true);
	}

	private void showDetailWindow(int index) {
		int modelIndex = table.convertRowIndexToModel(index);
		Record record = records.get(modelIndex);
		new DetailWindow(records.getPath(), modelIndex, record).setVisible(true);
	}

	private void showDetailWindows() {
		Arrays.stream(table.getSelectedRows()).forEach(index -> showDetailWindow(index));
	}
	
	private void showColumnCustomDialog() {
		Record record = null;
		int selectedRow = table.getSelectedRow();
		if (selectedRow != -1) {
			record = records.get(table.convertRowIndexToModel(selectedRow));
		} else if (!records.isEmpty()) {
			record = records.get(0);
		}
		if (new ColumnCustomDialog(config, tableModel.columnNames, record).showDialog() == JOptionPane.OK_OPTION) {
			tableModel.fireTableStructureChanged();
			tableHeader.sizeWidthToFitData();
		}
	}
	
	private void showExpressionTestDialog() {
		Record record = null;
		int selectedRow = table.getSelectedRow();
		if (selectedRow != -1) {
			record = records.get(table.convertRowIndexToModel(selectedRow));
		} else if (!records.isEmpty()) {
			record = records.get(0);
		}
		String result = DataParser.eval(record, Objects.toString(filterText.getEditor().getItem(), ""));
		GUIUtils.showMessageDialog(this, "フィルタの式をテスト", result, null);
	}

	private void initialize() {
		// メニューバーの設定
		JMenuBar menubar = new JMenuBar();
		menubar.add(GUIUtils.newJMenu("ファイル(_F)" //
				, GUIUtils.newJMenuItem("開く(_O)", e -> openFile(), KeyStroke.getKeyStroke("ctrl O")) //
				, GUIUtils.newJMenuItem("一覧をエクスポート", e -> exportList()) //
				, GUIUtils.newJMenuItem("詳細をエクスポート", e -> exportDetails()) //
				, recentlyUsedFilesMenu));
		menubar.add(GUIUtils.newJMenu("一覧(_L)" //
				, GUIUtils.newJMenuItem("再読込(_R)", e -> reloadFile(), KeyStroke.getKeyStroke("F5")),
				GUIUtils.newJMenuItem("詳細を開く", e -> showDetailWindows()),
				GUIUtils.newJMenuItem("列のカスタマイズ", e -> showColumnCustomDialog()),
				GUIUtils.newJMenuItem("列幅を調整", e -> tableHeader.sizeWidthToFitData())));
		menubar.add(GUIUtils.newJMenu("その他(_O)" //
				, GUIUtils.newJMenuItem("フィルタの式をテスト(_T)", e -> showExpressionTestDialog(), KeyStroke.getKeyStroke("ctrl T"))
				, propertiesSelectorMenu));
		setJMenuBar(menubar);

		// ヘッダの設定
		// - ヘッダ1
		JPanel header1 = new JPanel();
		header1.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		header1.add(new JLabel("ファイルの種類:"));
		header1.add(fileTypeCombo);

		// - ヘッダ2
		JPanel header2 = new JPanel();
		header2.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		header2.add(new JLabel("表示行数:"));
		header2.add(maxRowsText);
		header2.add(new JLabel(" 表示範囲:"));
		header2.add(offsetFromText);
		header2.add(new JLabel("-"));
		header2.add(offsetToText);
		header2.add(new JLabel(" "));
		header2.add(topButton);
		header2.add(prevButton);
		header2.add(nextButton);
		header2.add(tailButton);

		// - ヘッダ3
		JPanel header3 = new JPanel();
		header3.setLayout(new BoxLayout(header3, BoxLayout.X_AXIS));
		header3.add(new JLabel("フィルタ:"));
		header3.add(filterText);
		header3.add(filterMenuButton);

		// - 
		JPanel headers = new JPanel();
		headers.setLayout(new BoxLayout(headers, BoxLayout.Y_AXIS));
		headers.add(header1);
		headers.add(header2);
		headers.add(header3);
		headers.setBorder(new EmptyBorder(5, 5, 5, 5));

		// -
		getContentPane().add(headers, BorderLayout.NORTH);

		filterModeCheckBoxMenuItem.setToolTipText("高度なフィルタ(式を使用できるフィルタ)を有効にする。");
		filterMenu.add(GUIUtils.newJMenuItem("このフィルタを保存する", e -> saveFilter()));
		filterMenu.add(GUIUtils.newJMenuItem("このフィルタを削除する", e -> removeFilter()));
		filterMenu.add(GUIUtils.newJMenuItem("フィルタの式をテスト", e -> showExpressionTestDialog()));
		filterMenu.add(filterModeCheckBoxMenuItem);
		filterMenuButton.setComponentPopupMenu(filterMenu);
		filterMenuButton.addActionListener(e -> filterMenu.show(filterMenuButton, 0, filterMenuButton.getHeight()));

		// テーブル初期化
		table = new JTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);

		// - テーブルのフィルタ/ソート設定
		sorter = new TableRowSorter<>(tableModel);
		table.setRowSorter(sorter);
		filterText.setEditable(true);
		filterText.addActionListener((e) -> {
			if ("comboBoxChanged".equals(e.getActionCommand())) {
				applyTableFilter();
			}
		});
		@SuppressWarnings("serial")
		Action filterAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				applyTableFilter();
			}
		};
		JComponent filterEditor = (JComponent) filterText.getEditor().getEditorComponent();
		filterEditor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), filterAction);
		filterEditor.getActionMap().put(filterAction, filterAction);
		updateFilters();
		filterText.getEditor().setItem("");
		filterModeCheckBoxMenuItem
				.setSelected(GUIConfiguration.getInstance().getBoolean(Booleans.extendedFilterEnabled));
		filterModeCheckBoxMenuItem.addChangeListener(e -> {
			GUIConfiguration.getInstance().setBoolean(Booleans.extendedFilterEnabled,
					filterModeCheckBoxMenuItem.isSelected());
		});

		// - テーブルのヘッダ設定
		tableHeader = new AutoFitTableHeader(table.getColumnModel());
		table.setTableHeader(tableHeader);

		// - テーブルのページング
		maxRowsText.addActionListener(e -> updatePage(0));
		offsetFromText.addActionListener(e -> updatePage(0));
		prevButton.addActionListener(e -> updatePage(-Integer.parseInt(maxRowsText.getText())));
		nextButton.addActionListener(e -> updatePage(Integer.parseInt(maxRowsText.getText())));
		topButton.addActionListener(e -> updatePage(Integer.MIN_VALUE));
		tailButton.addActionListener(e -> updatePage(Integer.MAX_VALUE));

		// -
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent mouseEvent) {
				JTable table = (JTable) mouseEvent.getSource();
				Point point = mouseEvent.getPoint();
				int row = table.rowAtPoint(point);
				if (mouseEvent.getClickCount() == 2 && table.getSelectedRow() != -1) {
					showDetailWindow(row);
				}
			}
		});

		// ウィンドウイベントの設定
		addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				updateWindowTitle();
			}

			@Override
			public void windowLostFocus(WindowEvent e) {
				updateWindowTitle();
			}
		});

		updateControls();
	}

	private void applyTableFilter() {
		String filter = Objects.toString(filterText.getEditor().getItem(), "");
		if (filter.isEmpty()) {
			sorter.setRowFilter(null);
		} else {
			sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
				@Override
				public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
					if (filterModeCheckBoxMenuItem.isSelected()) {
						return DataParser.evalAsBoolean(records.get(entry.getIdentifier()), filter);
					} else {
						for (int i = 1; i < entry.getValueCount(); i++) {
							if (entry.getStringValue(i).contains(filter))
								return true;
						}
						return false;
					}
				}
			});
		}
	}

	private void saveFilter() {
		String filter = Objects.toString(filterText.getEditor().getItem(), "");
		if (filter != null && !filter.trim().isEmpty()) {
			GUIConfiguration.getInstance().addFilter(filter);
			updateFilters();
		}
	}

	private void removeFilter() {
		String filter = Objects.toString(filterText.getEditor().getItem(), "");
		GUIConfiguration.getInstance().getFilters().removeIf(e -> e.equals(filter));
		updateFilters();
		filterText.getEditor().setItem("");
	}

	private void updatePage(int add) {
		int recordOffset;
		if (add == Integer.MAX_VALUE) {
			recordOffset = -1;
		} else if (add == Integer.MIN_VALUE) {
			recordOffset = 0;
		} else {
			try {
				recordOffset = Integer.valueOf(offsetFromText.getText()) + add;
				if (recordOffset < 0) {
					recordOffset = 0;
				}
			} catch (NumberFormatException e) {
				return;
			}
		}

		offsetFromText.setText(Integer.toString(recordOffset));
		reloadFile();
	}

	private void updatePropertiesSelectorMenu() {
		if (config == null) {
			config = new FileTypeConfiguration(FileTypeConfiguration.findPropertyFiles().get(0));
		}
		propertiesSelectorMenu.removeAll();
		FileTypeConfiguration.findPropertyFiles().forEach(path -> {
			JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(path.getFileName().toString());
			menuItem.setSelected(config.getPath().equals(path));
			menuItem.addActionListener(e -> {
				config = new FileTypeConfiguration(path);
				updatePropertiesSelectorMenu();
			});
			propertiesSelectorMenu.add(menuItem);
		});
		fileTypeCombo.removeAllItems();
		config.getRecordFormatMap().entrySet().forEach(e -> fileTypeCombo.addItem(e.getValue()));
	}

	private void updateControls() {
		updateWindowTitle();

		int recordOffset = records.getOffeset();
		if (records.isEmpty()) {
			offsetFromText.setText("");
			offsetToText.setText("");
		} else {
			offsetFromText.setText(Integer.toString(recordOffset + 1));
			offsetToText.setText(Integer.toString(recordOffset + records.size()));
		}
		offsetFromText.setEditable(!records.isEmpty());

		prevButton.setEnabled(recordOffset != 0);
		nextButton.setEnabled(!records.eof());
		topButton.setEnabled(recordOffset != 0);
		tailButton.setEnabled(!records.eof());

		recentlyUsedFilesMenu.removeAll();
		AtomicInteger i = new AtomicInteger();
		GUIConfiguration.getInstance().getRecentlyUsedFiles().forEach(e -> {
			String mnemonic = "_" + Integer.toString(i.getAndIncrement(), 32);
			recentlyUsedFilesMenu.add(GUIUtils.newJMenuItem(mnemonic + " " + e.toString(), path -> openFile(null, e)));
		});
	}

	private void updateWindowTitle() {
		if (records.getPath() != null) {
			setTitle((isFocused() ? records.getPath() : records.getPath().getFileName()) + " - AnyfileViewer");
		} else {
			setTitle("AnyfileViewer");
		}
	}

	private void updateFilters() {
		filterText.removeAllItems();
		GUIConfiguration.getInstance().getFilters().forEach(e -> filterText.addItem(e));
	}

	private class TableModel extends AbstractTableModel {
		private static final long serialVersionUID = 6751453527834701108L;

		private List<String> columnNames = new ArrayList<>();

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames.get(columnIndex);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return "[No.]".equals(getColumnName(columnIndex)) ? Integer.class : Object.class;
		}

		@Override
		public int getRowCount() {
			return records.size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if ("[No.]".equals(getColumnName(columnIndex))) {
				return Integer.valueOf(records.getOffeset() + rowIndex + 1);
			} else {
				Record record = records.get(rowIndex);
				if (!record.getMetaItems().containsKey("[エラー]")) {
					return record.getValue(columnNames.get(columnIndex));
				} else if (columnIndex == 1) {
					return record.getValue("[エラー]");
				} else {
					return "";
				}
			}
		}
	}
}
