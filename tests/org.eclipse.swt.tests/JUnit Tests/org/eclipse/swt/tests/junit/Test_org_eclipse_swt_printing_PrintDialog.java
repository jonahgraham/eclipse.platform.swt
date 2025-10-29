/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.tests.junit;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.tests.WithTestLifecycle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Automated Test Suite for class org.eclipse.swt.printing.PrintDialog
 *
 * @see org.eclipse.swt.printing.PrintDialog
 */
@WithTestLifecycle
public class Test_org_eclipse_swt_printing_PrintDialog extends Test_org_eclipse_swt_widgets_Dialog {

@Override
@BeforeEach
public void setUp() {
	super.setUp();
	printDialog = new PrintDialog(shell, SWT.NONE);
	setDialog(printDialog);
}

@Test
public void test_ConstructorLorg_eclipse_swt_widgets_Shell() {
	new PrintDialog(shell);

	assertThrows(IllegalArgumentException.class, ()	-> new PrintDialog(null),"No exception thrown for parent == null");
}

@Test
public void test_ConstructorLorg_eclipse_swt_widgets_ShellI() {
	new PrintDialog(shell, SWT.NONE);

	assertThrows(IllegalArgumentException.class, ()	-> new PrintDialog(null, SWT.NONE),"No exception thrown for parent == null");
}

/* custom */
	PrintDialog printDialog;
}
