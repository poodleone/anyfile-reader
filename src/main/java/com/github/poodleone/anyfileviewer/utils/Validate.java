package com.github.poodleone.anyfileviewer.utils;

import java.util.function.Supplier;

/**
 * バリデーションを行うユーティリティ
 */
public class Validate {
	/**
	 * 指定された条件expressionを満たすか確認します. 条件を満たさない場合、exceptionSupplierで生成した例外をスローします.
	 * 
	 * @param expression        確認する条件式
	 * @param exceptionSupplier 例外を生成するSupplier
	 */
	public static void isTrue(boolean expression, Supplier<RuntimeException> exceptionSupplier) {
		if (!expression) {
			throw exceptionSupplier.get();
		}
	}

	/**
	 * 指定されたobjectがnullでないか確認します. 条件を満たさない場合、exceptionSupplierで生成した例外をスローします.
	 * 
	 * @param object            nullでないか確認するオブジェクト
	 * @param exceptionSupplier 例外を生成するSupplier
	 */
	public static void notNull(Object object, Supplier<RuntimeException> exceptionSupplier) {
		if (object == null) {
			throw exceptionSupplier.get();
		}
	}

}
