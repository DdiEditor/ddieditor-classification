package dk.dda.ddieditor.classification.command;

import org.ddialliance.ddieditor.ui.editor.category.CategorySchemeEditor;
import org.ddialliance.ddieditor.ui.editor.code.CodeSchemeEditor;
import org.ddialliance.ddieditor.ui.view.ViewManager;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.PlatformUI;

import dk.dda.ddieditor.classification.wizard.CreateClassificationWizard;

/*
 * Copyright 2012 Danish Data Archive (http://www.dda.dk) 
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either Version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library; if not, write to the 
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, 
 * Boston, MA  02110-1301  USA
 * The full text of the license is also available on the Internet at 
 * http://www.gnu.org/copyleft/lesser.html
 */

/**
 * RCP command for classification creation
 */
public class CreateClassification extends
		org.eclipse.core.commands.AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// open dialog
		CreateClassificationWizard wizard = new CreateClassificationWizard();
		WizardDialog dialog = new WizardDialog(PlatformUI.getWorkbench()
				.getDisplay().getActiveShell(), wizard);

		int returnCode = dialog.open();
		if (returnCode != Window.CANCEL) {
			// import
			CreateClassificationJob longJob = new CreateClassificationJob(
					wizard.selectedResource, wizard.cvsFile, wizard.labelTxt,
					wizard.descriptionTxt, wizard.codeImpl);
			BusyIndicator.showWhile(PlatformUI.getWorkbench().getDisplay(),
					longJob);

			// refresh
			ViewManager.getInstance().addViewsToRefresh(
					new String[] { CodeSchemeEditor.ID,
							CategorySchemeEditor.ID});
			ViewManager.getInstance().refesh();
		}
		return null;
	}
}
