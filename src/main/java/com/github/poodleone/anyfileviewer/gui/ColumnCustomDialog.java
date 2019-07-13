package com.github.poodleone.anyfileviewer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import com.github.poodleone.anyfileviewer.FileTypeConfiguration;
import com.github.poodleone.anyfileviewer.record.Record;

/**
 * 一覧に表示する列を選択するダイアログ.
 */
public class ColumnCustomDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private JList<String> fromList = new JList<>(new DefaultListModel<>());
	private JList<String> toList = new JList<>(new DefaultListModel<>());

	private JTextField columnNameText = new JTextField("", 60);
	private JButton insertButton = new JButton("挿入→");
	private JButton removeButton = new JButton("←削除");
	private JButton topButton = new JButton("先頭");
	private JButton upButton = new JButton("上へ");
	private JButton downButton = new JButton("下へ");
	private JButton tailButton = new JButton("最終");
	
	private JButton applyButton = new JButton("適用");
	private JButton cancelButton = new JButton("キャンセル");

	private GridBagLayout gbl = new GridBagLayout();
	private JPanel panel = new JPanel();

	private int returnValue = JOptionPane.CANCEL_OPTION;

	/**
	 * 一覧の列をカスタマイズするダイアログを生成します.
	 * 
	 * @param config  ビューワの設定
	 * @param columns カラム名のリスト
	 * @param record  カラム名選択に使用するレコード
	 */
	public ColumnCustomDialog(FileTypeConfiguration config, List<String> columns, Record record) {
		setTitle("一覧の列をカスタマイズ");

		// コントロールを配置
		panel.setLayout(gbl);
		addComponent(columnNameText, 0, 0, 4, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);

		addComponent(fromList, 0, 1, 1, 4, 1.0d, 1.0d, GridBagConstraints.BOTH);
		
		addComponent(insertButton, 1, 1, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(removeButton, 1, 2, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		
		addComponent(toList, 2, 1, 1, 4, 1.0d, 1.0d, GridBagConstraints.BOTH);
		
		addComponent(topButton, 3, 1, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(upButton, 3, 2, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(downButton, 3, 3, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(tailButton, 3, 4, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		
		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		bottom.add(applyButton);
		bottom.add(cancelButton);
		applyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		cancelButton.setAlignmentX(Component.CENTER_ALIGNMENT);
		bottom.setBorder(new LineBorder(Color.BLACK));
		addComponent(bottom, 0, 6, 4, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panel, BorderLayout.CENTER);

		// リストの選択項目を設定
		fromList.setPreferredSize(new Dimension(200, fromList.getSize().height * 30));
		toList.setPreferredSize(new Dimension(200, toList.getSize().height * 30));
		DefaultListModel<String> fromListModel = (DefaultListModel<String>) fromList.getModel();
		DefaultListModel<String> toListModel = (DefaultListModel<String>) toList.getModel();
		List<String> allItems = new ArrayList<>();
		if (record != null) {
			allItems.addAll(columns);
			record.getMetaItems().keySet().stream().filter(e -> !allItems.contains(e)).forEach(allItems::add);
			record.getItems().keySet().stream().filter(e -> !allItems.contains(e)).forEach(allItems::add);
		}
		allItems.forEach(e -> {
			if (!columns.contains(e)) {
				fromListModel.addElement(e);
			}
		});
		columns.forEach(e -> {
			toListModel.addElement(e);
		});
		insertButton.setEnabled(!fromListModel.isEmpty());
		removeButton.setEnabled(!toListModel.isEmpty());
		fromList.setSelectedIndex(0);
		toList.setSelectedIndex(toListModel.getSize() - 1);

		Runnable updateButton = () -> {
			insertButton.setEnabled(!fromListModel.isEmpty());
			removeButton.setEnabled(!toListModel.isEmpty());
		};
		// コントロールの挙動を設定
		// - 挿入ボタン
		insertButton.addActionListener(e -> {
			int fromIndex = fromList.getSelectedIndex();
			if (fromIndex != -1) {
				String item = fromListModel.getElementAt(fromIndex);
				fromListModel.removeElement(item);
				
				int toIndex = toList.getSelectedIndex() + 1;
				toListModel.insertElementAt(item, toIndex);
				toList.setSelectedIndex(toIndex);
				
				fromIndex = Integer.min(fromIndex, fromList.getLastVisibleIndex());
				fromList.setSelectedIndex(fromIndex);
				updateButton.run();
			}
		});
		// - 削除ボタン
		removeButton.addActionListener(e -> {
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1) {
				String item = toListModel.getElementAt(toIndex);
				toListModel.removeElement(item);
				fromListModel.removeAllElements();
				
				allItems.forEach(e2 -> {
					if (!toListModel.contains(e2)) {
						fromListModel.addElement(e2);
					}
				});
				fromList.setSelectedValue(item, true);
				
				toIndex = Integer.min(toIndex, toList.getLastVisibleIndex());
				toList.setSelectedIndex(toIndex);
				updateButton.run();
			}
		});
		topButton.addActionListener(e ->{
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1) {
				toListModel.insertElementAt(toListModel.remove(toIndex), 0);
				toList.setSelectedIndex(0);
			}
		});
		upButton.addActionListener(e ->{
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1 && toIndex != 0) {
				int newToIndex = toIndex - 1;
				toListModel.insertElementAt(toListModel.remove(toIndex), newToIndex);
				toList.setSelectedIndex(newToIndex);
			}
		});
		downButton.addActionListener(e ->{
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1 && toIndex != toList.getLastVisibleIndex()) {
				int newToIndex = toIndex + 1;
				toListModel.insertElementAt(toListModel.remove(toIndex), newToIndex);
				toList.setSelectedIndex(newToIndex);
			}
		});
		tailButton.addActionListener(e ->{
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1) {
				toListModel.addElement(toListModel.remove(toIndex));
				toList.setSelectedIndex(toList.getLastVisibleIndex());
			}
		});
		applyButton.addActionListener(e -> {
			columns.clear();
			columns.addAll(Collections.list(toListModel.elements()));
			setVisible(false);
			returnValue = JOptionPane.OK_OPTION;
		});
		cancelButton.addActionListener(e -> setVisible(false));
		pack();
		setModal(true);
	}
	
	/**
	 * 一覧の列をカスタマイズするダイアログを表示します.
	 * 
	 * @return 選択された次の値が返されます.
     * <ul>
     * <li>JOptionPane.CANCEL_OPTION
     * <li>JOptionPane.OK_OPTION
     * </ul>
     */
	public int showDialog() {
		setVisible(true);
		dispose();
		return returnValue;
	}
	
	private void addComponent(Component c, int x, int y, int w, int h, double wx, double wy, int fill) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = w;
        gbc.gridheight = h;
        gbc.weightx = wx;
        gbc.weighty = wy;
        gbc.fill = fill;
        gbl.setConstraints(c, gbc);
        panel.add(c);
    }

}
