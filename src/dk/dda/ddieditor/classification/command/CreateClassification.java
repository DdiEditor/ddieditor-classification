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
