package com.github.poodleone.anyfileviewer.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import com.github.poodleone.anyfileviewer.FileTypeConfiguration;
import com.github.poodleone.anyfileviewer.itemdefinition.MetaItemDefinition;
import com.github.poodleone.anyfileviewer.record.Record;

/**
 * 一覧に表示する列を選択するダイアログ.
 */
public class ColumnCustomDialog extends JDialog {
	private static final long serialVersionUID = 1L;

	private JList<String> fromList = new JList<>(new DefaultListModel<>());
	private JList<String> toList = new JList<>(new DefaultListModel<>());

	private JButton insertButton = new JButton("挿入>");
	private JButton removeButton = new JButton("<戻す");
	private JButton deleteButton = new JButton("削除");
	private JButton editButton = new JButton("編集");
	private JButton topButton = new JButton("先頭");
	private JButton upButton = new JButton("上へ");
	private JButton downButton = new JButton("下へ");
	private JButton tailButton = new JButton("最終");

	private JTextField itemNameText = new JTextField();
	private JTextArea itemExpressionText = new JTextArea(10, 80);
	private JButton newItemButton = new JButton("項目作成/変更");

	private JButton applyButton = new JButton("適用");
	private JButton cancelButton = new JButton("キャンセル");

	private GridBagLayout gbl = new GridBagLayout();
	private JPanel panel = new JPanel();

	private int returnValue = JOptionPane.CANCEL_OPTION;

	/**
	 * 一覧の列をカスタマイズするダイアログを生成します.
	 * 
	 * @param config          ビューワの設定
	 * @param columns         カラム名のリスト
	 * @param record          カラム名選択に使用するレコード
	 * @param additionalItems 追加項目のリスト
	 */
	public ColumnCustomDialog(FileTypeConfiguration config, List<String> columns, Record record,
			List<MetaItemDefinition> additionalItems) {
		setTitle("一覧の列のカスタマイズ");

		// コントロールを配置
		panel.setLayout(gbl);

		addComponent(new JScrollPane(fromList), 0, 0, 2, 5, 1.0d, 1.0d, GridBagConstraints.BOTH);
		addComponent(new JScrollPane(toList), 3, 0, 1, 5, 1.0d, 1.0d, GridBagConstraints.BOTH);
		addComponent(Box.createVerticalStrut(100), 4, 4, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);

		addComponent(insertButton, 2, 0, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(removeButton, 2, 1, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(deleteButton, 2, 2, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(editButton, 2, 3, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);

		addComponent(topButton, 4, 0, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(upButton, 4, 1, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(downButton, 4, 2, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(tailButton, 4, 3, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);

		addComponent(Box.createHorizontalStrut(350), 0, 5, 2, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(Box.createHorizontalStrut(350), 3, 5, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);

		addComponent(Box.createVerticalStrut(10), 0, 6, 4, 1, 0.0d, 0.0d, GridBagConstraints.NONE,
				GridBagConstraints.WEST);
		addComponent(new JLabel("名前:"), 0, 7, 1, 1, 0.0d, 0.0d, GridBagConstraints.NONE, GridBagConstraints.EAST);
		addComponent(itemNameText, 1, 7, 1, 1, 0.0d, 0.0d, GridBagConstraints.HORIZONTAL);
		addComponent(new JLabel("式:"), 0, 8, 1, 1, 0.0d, 0.0d, GridBagConstraints.NONE, GridBagConstraints.NORTHEAST);
		addComponent(new JScrollPane(itemExpressionText), 1, 8, 4, 1, 0.0d, 0.0d, GridBagConstraints.BOTH);
		addComponent(newItemButton, 3, 9, 2, 1, 0.0d, 0.0d, GridBagConstraints.NONE, GridBagConstraints.EAST);

		addComponent(Box.createVerticalStrut(10), 0, 10, 4, 1, 0.0d, 0.0d, GridBagConstraints.NONE,
				GridBagConstraints.WEST);
		JPanel bottom = new JPanel();
		bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
		bottom.add(applyButton);
		bottom.add(cancelButton);
		bottom.setAlignmentX(Component.LEFT_ALIGNMENT);
		applyButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		cancelButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		addComponent(bottom, 0, 11, 5, 1, 0.0d, 0.0d, GridBagConstraints.NONE, GridBagConstraints.EAST);

		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panel, BorderLayout.CENTER);

		// リストの選択項目を設定
		DefaultListModel<String> fromListModel = (DefaultListModel<String>) fromList.getModel();
		DefaultListModel<String> toListModel = (DefaultListModel<String>) toList.getModel();
		List<String> allItems = new ArrayList<>();
		allItems.addAll(columns);
		additionalItems.stream().filter(e -> !allItems.contains(e.getName())).map(MetaItemDefinition::getName).forEach(allItems::add);
		if (record != null) {
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

		// コントロールの挙動を設定
		Map<String, MetaItemDefinition> newAdditionalItems = additionalItems.stream()
				.collect(Collectors.toMap(e -> e.getName(), e -> e, (k, v) -> v, LinkedHashMap::new));
		Runnable updateButton = () -> {
			insertButton.setEnabled(fromList.getSelectedIndex() != -1);
			removeButton.setEnabled(toList.getSelectedIndex() != -1);
			deleteButton.setEnabled(newAdditionalItems.containsKey(fromList.getSelectedValue()));
			editButton.setEnabled(newAdditionalItems.containsKey(fromList.getSelectedValue()));
			topButton.setEnabled(toList.getSelectedIndex() != -1);
			upButton.setEnabled(toList.getSelectedIndex() != -1);
			downButton.setEnabled(toList.getSelectedIndex() != -1);
			tailButton.setEnabled(toList.getSelectedIndex() != -1);
		};
		updateButton.run();
		// - 項目を作成ボタン
		newItemButton.addActionListener(e -> {
			boolean hasItemName = !itemNameText.getText().isEmpty();
			boolean hasItemExpression = !itemExpressionText.getText().isEmpty();

			itemNameText.setBackground(hasItemName ? Color.WHITE : Color.YELLOW);
			itemExpressionText.setBackground(hasItemExpression ? Color.WHITE : Color.YELLOW);

			if (hasItemName && hasItemExpression) {
				String newItemName = itemNameText.getText();
				newAdditionalItems.put(newItemName, new MetaItemDefinition(newItemName, itemExpressionText.getText()));

				if (!allItems.contains(newItemName)) {
					allItems.add(newItemName);
					fromListModel.addElement(newItemName);
				}
			}
		});
		fromList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if ((e.getClickCount() % 2) == 0) {
					int index = fromList.locationToIndex(e.getPoint());
					String itemName = fromListModel.getElementAt(index);
					if (newAdditionalItems.containsKey(itemName)) {
						itemNameText.setText(itemName);
						itemExpressionText.setText(newAdditionalItems.get(itemName).getValueExpression());
					}
				}
			}
		});
		toList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if ((e.getClickCount() % 2) == 0) {
					int index = toList.locationToIndex(e.getPoint());
					String itemName = toListModel.getElementAt(index);
					if (newAdditionalItems.containsKey(itemName)) {
						itemNameText.setText(itemName);
						itemExpressionText.setText(newAdditionalItems.get(itemName).getValueExpression());
					}
				}
			}
		});
		fromList.addListSelectionListener(e -> updateButton.run());
		toList.addListSelectionListener(e -> updateButton.run());
		// - 挿入ボタン
		insertButton.addActionListener(e -> {
			int fromIndex = fromList.getSelectedIndex();
			if (fromIndex != -1) {
				String itemName = fromListModel.getElementAt(fromIndex);
				fromListModel.removeElement(itemName);

				int toIndex = toList.getSelectedIndex() + 1;
				toListModel.insertElementAt(itemName, toIndex);
				toList.setSelectedIndex(toIndex);

				fromIndex = Integer.min(fromIndex, fromList.getLastVisibleIndex());
				fromList.setSelectedIndex(fromIndex);
				updateButton.run();
			}
		});
		// - 戻すボタン
		removeButton.addActionListener(e -> {
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1) {
				String itemName = toListModel.getElementAt(toIndex);
				toListModel.removeElement(itemName);
				fromListModel.removeAllElements();

				allItems.forEach(e2 -> {
					if (!toListModel.contains(e2)) {
						fromListModel.addElement(e2);
					}
				});
				fromList.setSelectedValue(itemName, true);

				toIndex = Integer.min(toIndex, toList.getLastVisibleIndex());
				toList.setSelectedIndex(toIndex);
				updateButton.run();
			}
		});
		// - 削除ボタン
		deleteButton.addActionListener(e -> {
			int fromIndex = fromList.getSelectedIndex();
			if (fromIndex != -1) {
				String itemName = fromList.getSelectedValue();
				fromListModel.removeElement(itemName);
				newAdditionalItems.remove(itemName);

				fromIndex = Integer.min(fromIndex, fromList.getLastVisibleIndex());
				fromList.setSelectedIndex(fromIndex);
				updateButton.run();
			}
		});
		// - 編集ボタン
		editButton.addActionListener(e -> {
			if (newAdditionalItems.containsKey(fromList.getSelectedValue())) {
				 MetaItemDefinition item = newAdditionalItems.get(fromList.getSelectedValue());
				itemNameText.setText(item.getName());
				itemExpressionText.setText(item.getValueExpression());
				updateButton.run();
			}
		});
		// - 先頭ボタン
		topButton.addActionListener(e -> {
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1) {
				toListModel.insertElementAt(toListModel.remove(toIndex), 0);
				toList.setSelectedIndex(0);
			}
		});
		// - 上へボタン
		upButton.addActionListener(e -> {
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1 && toIndex != 0) {
				int newToIndex = toIndex - 1;
				toListModel.insertElementAt(toListModel.remove(toIndex), newToIndex);
				toList.setSelectedIndex(newToIndex);
			}
		});
		// - 下へボタン
		downButton.addActionListener(e -> {
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1 && toIndex != toList.getLastVisibleIndex()) {
				int newToIndex = toIndex + 1;
				toListModel.insertElementAt(toListModel.remove(toIndex), newToIndex);
				toList.setSelectedIndex(newToIndex);
			}
		});
		// - 最終ボタン
		tailButton.addActionListener(e -> {
			int toIndex = toList.getSelectedIndex();
			if (toIndex != -1) {
				toListModel.addElement(toListModel.remove(toIndex));
				toList.setSelectedIndex(toList.getLastVisibleIndex());
			}
		});
		// - 適用ボタン
		applyButton.addActionListener(e -> {
			columns.clear();
			columns.addAll(Collections.list(toListModel.elements()));
			additionalItems.clear();
			additionalItems.addAll(newAdditionalItems.values());
			setVisible(false);
			returnValue = JOptionPane.OK_OPTION;
		});
		// - キャンセルボタン
		cancelButton.addActionListener(e -> setVisible(false));

		pack();
		setModal(true);
	}

	/**
	 * 一覧の列をカスタマイズするダイアログを表示します.
	 * 
	 * @return 選択された次の値が返されます.
	 *         <ul>
	 *         <li>JOptionPane.CANCEL_OPTION
	 *         <li>JOptionPane.OK_OPTION
	 *         </ul>
	 */
	public int showDialog() {
		setVisible(true);
		dispose();
		return returnValue;
	}

	private void addComponent(Component c, int x, int y, int w, int h, double wx, double wy, int fill) {
		addComponent(c, x, y, w, h, wx, wy, fill, GridBagConstraints.CENTER);
	}

	private void addComponent(Component c, int x, int y, int w, int h, double wx, double wy, int fill, int anchor) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = w;
		gbc.gridheight = h;
		gbc.weightx = wx;
		gbc.weighty = wy;
		gbc.fill = fill;
		gbc.anchor = anchor;
		gbl.setConstraints(c, gbc);
		panel.add(c);
	}

}
