/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.swt.snippets;

import org.eclipse.swt.internal.gtk.*;
import org.eclipse.swt.widgets.*;

public class Snippet1 {


	public static void main(String[] args) {
		System.out.println("GTK4: " + GTK.GTK4);
		int i = 0;
		final Display display = new Display();
		while (i++ < 2500000) {
			System.out.println(i);
			Shell shell = new Shell(display);
			shell.open(); // only needed on wayland
			while (display.readAndDispatch()) {
			}
			shell.dispose();
			System.gc();
		}
	}
}
