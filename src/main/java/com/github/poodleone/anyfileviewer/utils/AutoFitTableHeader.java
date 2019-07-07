package com.github.poodleone.anyfileviewer.utils;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * 列幅が自動調整できるJTableHeader.
 */
public class AutoFitTableHeader extends JTableHeader {
	private static final long serialVersionUID = -5284198419983223628L;

	/**
	 * テーブルヘッダのインスタンスを生成します.
	 * 
	 * @param columnModel
	 */
	public AutoFitTableHeader(TableColumnModel columnModel) {
		super(columnModel);
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		if (e.getID() == MouseEvent.MOUSE_CLICKED && SwingUtilities.isLeftMouseButton(e)) {
			if (super.getCursor().getType() == Cursor.E_RESIZE_CURSOR) {
				if (e.getClickCount() % 2 == 1) {
					// カーソルが列の間にあるときシングルクリックされたらキャンセルする(ソートをとめるため)
					return;
				} else {
					// カーソルが列の間にあるときダブルクリックされたら列幅調整
					Point pt = new Point(e.getX() - 3, e.getY());
					int vc = super.columnAtPoint(pt);
					if (vc >= 0) {
						sizeWidthToFitData(vc);
						e.consume();
						return;
					}
				}
			}
		}
		super.processMouseEvent(e);
	}

	/**
	 * 列幅をデータの幅に合わせます.
	 *
	 * @param vc 表示列番号
	 */
	public void sizeWidthToFitData(int vc) {
		TableColumn tc = columnModel.getColumn(vc);

		// ヘッダの幅を取得
		TableCellRenderer hr = table.getTableHeader().getDefaultRenderer();
		Object headerText = tc.getHeaderValue();
		int max = hr.getTableCellRendererComponent(table, headerText, false, false, 0, vc).getPreferredSize().width;

		// 各行の幅で一番大きいサイズを取得
		for (int i = 0; i < table.getRowCount(); i++) {
			TableCellRenderer cr = table.getCellRenderer(i, vc);
			Object value = table.getValueAt(i, vc);
			Component c = cr.getTableCellRendererComponent(table, value, false, false, i, vc);
			int w = c.getPreferredSize().width;
			if (max < w) {
				max = w;
			}
		}

		tc.setPreferredWidth(max + 5);
	}

	/**
	 * 列幅をデータの幅に合わせます.
	 */
	public void sizeWidthToFitData() {
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			sizeWidthToFitData(i);
		}
	}
}
