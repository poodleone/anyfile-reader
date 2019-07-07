package com.github.poodleone.anyfileviewer.utils;

import java.awt.Toolkit;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

/**
 * テキストフィールドの数値チェックを行うInputVerifierです.<br>
 * 入力値が数値であり、指定されたmin～maxの範囲内であることを確認します.
 *
 */
public class IntegerInputVerifier extends InputVerifier {
	private int min;
	private int max;

	/**
	 * テキストフィールドの値がmin～maxの範囲内の数値であることを確認するInputVerifierを生成します.
	 * 
	 * @param min 入力可能な最小値
	 * @param max 入力可能な最大値
	 */
	public IntegerInputVerifier(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public boolean verify(JComponent c) {
		boolean verified = false;
		JTextComponent textField = (JTextComponent) c;
		try {
			int value = Integer.parseInt(textField.getText());
			if (value < min) {
				textField.setText(Integer.toString(min));
			} else if (max < value) {
				textField.setText(Integer.toString(max));
			}
			verified = true;
		} catch (NumberFormatException e) {
			UIManager.getLookAndFeel().provideErrorFeedback(c);
			Toolkit.getDefaultToolkit().beep();
		}
		return verified;
	}
}