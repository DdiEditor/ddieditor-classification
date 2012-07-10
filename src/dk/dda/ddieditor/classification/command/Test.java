package dk.dda.ddieditor.classification.command;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;

public class Test {

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		Spinner spinner = new Spinner (shell, SWT.BORDER);
		spinner.setMinimum(0);
		spinner.setMaximum(5);
		spinner.setSelection(0);
		spinner.setIncrement(1);
		spinner.setPageIncrement(1);
		Rectangle clientArea = shell.getClientArea();
		spinner.setLocation(clientArea.x, clientArea.y);
		spinner.pack();
		shell.pack();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		display.dispose();
	}
}