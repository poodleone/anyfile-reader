package com.github.poodleone.anyfileviewer.gui;

import java.util.List;
import java.awt.BorderLayout;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import com.github.poodleone.anyfileviewer.record.RecordItemImpl;
import com.github.poodleone.anyfileviewer.record.RecordItem;
import com.github.poodleone.anyfileviewer.record.Record;
import com.github.poodleone.anyfileviewer.utils.AutoFitTableHeader;

/**
 * 詳細ウィンドウ.
 */
public class DetailWindow extends JFrame {
	private static final long serialVersionUID = -1L;

	private Path path;
	private int recordIndex;
	private Record record;

	private TableModel tableModel;
	private AutoFitTableHeader tableHeader;
	private TableRowSorter<TableModel> sorter;

	private JTextField filterText = new JTextField();

	/**
	 * 詳細ウィンドウを生成します.
	 * 
	 * @param path        表示レコードが含まれるファイルのパス
	 * @param recordIndex 表示レコードのindex
	 * @param record      表示レコード
	 */
	public DetailWindow(Path path, int recordIndex, Record record) {
		super();
		this.path = path;
		this.recordIndex = recordIndex;
		this.record = record;
		initialize();
		pack();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}

	/**
	 * ウィンドウを初期化します.
	 */
	private void initialize() {
		// ヘッダの設定
		JPanel header1 = new JPanel();
		header1.setLayout(new BoxLayout(header1, BoxLayout.X_AXIS));
		header1.add(new JLabel("フィルタ:"));
		header1.add(filterText);

		JPanel headers = new JPanel();
		headers.setLayout(new BoxLayout(headers, BoxLayout.Y_AXIS));
		headers.add(header1);
		getContentPane().add(headers, BorderLayout.NORTH);

		// テーブル初期化
		tableModel = new TableModel();
		JTable table = new JTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		// - テーブルのフィルタ/ソート設定
		sorter = new TableRowSorter<>(tableModel);
		table.setRowSorter(sorter);
		filterText.setEditable(true);
		filterText.addActionListener(e -> applyTableFilter());

		// - テーブルのヘッダ設定
		tableHeader = new AutoFitTableHeader(table.getColumnModel());
		table.setTableHeader(tableHeader);
		tableHeader.sizeWidthToFitData();
		sorter.setSortKeys(Arrays.asList(new RowSorter.SortKey(1, SortOrder.ASCENDING)));

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

		getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
	}

	/**
	 * テーブルにフィルタを適用します.
	 */
	private void applyTableFilter() {
		String filter = filterText.getText();
		if (filter.isEmpty()) {
			sorter.setRowFilter(null);
		} else {
			sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
				@Override
				public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
					for (Columns c : new Columns[] { Columns.NAME, Columns.VALUE, Columns.VALUE_HEX }) {
						if (entry.getStringValue(c.ordinal()).contains(filter))
							return true;
					}
					return false;
				}
			});
		}
	}

	/**
	 * ウィンドウタイトルを更新します.
	 * <li>フォーカスがあるとき: No.nnn ファイルのフルパス - アプリ名</li>
	 * <li>フォーカスがないとき: No.nnn ファイルのファイル名 - アプリ名</li>
	 */
	private void updateWindowTitle() {
		setTitle("No." + (recordIndex + 1) + " " + (isFocused() ? path : path.getFileName()) + " - AnyfileViewer");
	}

	private enum Columns {
		CLASS, NO, NAME, OFFSET, LENGTH, VALUE, VALUE_HEX
	};

	/**
	 * TableModel.
	 */
	private class TableModel extends AbstractTableModel {
		private static final long serialVersionUID = -1L;

		private String[] columnNames = { "分類", "No.", "項目名", "位置", "長さ", "値", "値(HEX)" };
		private List<String> itemNames;

		public TableModel() {
			super();
			itemNames = new ArrayList<>(record.getMetaItems().size() + record.getItems().size());
			itemNames.addAll(record.getMetaItems().keySet());
			itemNames.addAll(record.getItems().keySet());
		}

		@Override
		public String getColumnName(int columnIndex) {
			return columnNames[columnIndex];
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			Columns c = Columns.values()[columnIndex];
			if (c == Columns.NO) {
				return ItemNumber.class;
			} else if (c == Columns.OFFSET || c == Columns.LENGTH) {
				return Number.class;
			} else {
				return Object.class;
			}
		}

		@Override
		public int getRowCount() {
			return record.getMetaItems().size() + record.getItems().size();
		}

		@Override
		public int getColumnCount() {
			return columnNames.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			Columns c = Columns.values()[columnIndex];
			boolean isMetaData = rowIndex < record.getMetaItems().size();

			if (c == Columns.CLASS) {
				return isMetaData ? "メタ情報" : "データ";
			} else if (c == Columns.NO) {
				if (isMetaData) {
					return new ItemNumber(0, rowIndex + 1);
				} else {
					return new ItemNumber(1, rowIndex - record.getMetaItems().size() + 1);
				}
			}

			String itemName = itemNames.get(rowIndex);
			Object item = (isMetaData ? record.getMetaItems() : record.getItems()).get(itemName);
			if (c == Columns.NAME) {
				return itemName;

			} else if (c == Columns.OFFSET) {
				return item instanceof RecordItemImpl ? ((RecordItemImpl) item).getOffset() : "";

			} else if (c == Columns.LENGTH) {
				return item instanceof RecordItemImpl ? ((RecordItemImpl) item).getLength() : "";

			} else if (c == Columns.VALUE) {
				return item.toString();

			} else if (c == Columns.VALUE_HEX) {
				return item instanceof RecordItem ? ((RecordItem) item).toHexString() : "";

			}
			throw new IllegalArgumentException();
		}
	}

	/**
	 * 項目No.
	 */
	private class ItemNumber extends Number implements Comparable<ItemNumber> {
		private static final long serialVersionUID = 1L;
		private int no;
		private int subNo;

		public ItemNumber(int no, int subNo) {
			this.no = no;
			this.subNo = subNo;
		}

		@Override
		public int compareTo(ItemNumber o) {
			int compare = Integer.compare(no, o.no);
			return compare != 0 ? compare : Integer.compare(subNo, o.subNo);
		}

		@Override
		public int intValue() {
			return no * record.getMetaItems().size() + subNo;
		}

		@Override
		public long longValue() {
			return no * record.getMetaItems().size() + subNo;
		}

		@Override
		public float floatValue() {
			return no * record.getMetaItems().size() + subNo;
		}

		@Override
		public double doubleValue() {
			return no * record.getMetaItems().size() + subNo;
		}

		@Override
		public String toString() {
			return Integer.toString(subNo);
		}

	}
}
