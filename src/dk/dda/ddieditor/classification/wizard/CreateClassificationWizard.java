package dk.dda.ddieditor.classification.wizard;

import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.preference.PreferenceUtil;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import au.com.bytecode.opencsv.CSVReader;

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
 * RCP wizard for classification creation
 */
public class CreateClassificationWizard extends Wizard {
	private List<DDIResourceType> resources = null;

	public DDIResourceType selectedResource = null;
	public String cvsFile = null;
	public String labelTxt = "";
	public String descriptionTxt = "";
	public int codeImpl = 1; // use default nested
	int levels = 0;

	@Override
	public boolean performFinish() {
		return true;
	}

	@Override
	public void addPages() {
		SelectPage rangePage = new SelectPage();
		addPage(rangePage);
	}

	class SelectPage extends WizardPage {
		public static final String PAGE_NAME = "select";
		Spinner spinner = null;
		Label spinnerLabel = null;

		public SelectPage() {
			super(PAGE_NAME, Translator.trans("classcification.wizard.title"),
					null);
		}

		void pageComplete() {
			if (cvsFile != null && selectedResource != null) {
				setPageComplete(true);
			}
		}

		@Override
		public void createControl(Composite parent) {
			final Editor editor = new Editor();
			Group group = editor.createGroup(parent,
					Translator.trans("classcification.wizard.title"));
			// label
			editor.createLabel(group, Translator.trans("classcification.label"));
			Text label = editor.createText(group, "", false);
			label.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					labelTxt = ((Text) event.getSource()).getText();
				}
			});

			// description
			StyledText description = editor.createTextAreaInput(group,
					Translator.trans("classcification.description"), "", false);
			description.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					descriptionTxt = ((StyledText) event.getSource()).getText();
				}
			});

			// csv file
			editor.createLabel(group,
					Translator.trans("classcification.filechooser.title"));
			final Text pathText = editor.createText(group, "");
			pathText.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(KeyEvent e) {
					// on a CR - check if file exist and read it
					if (e.keyCode == SWT.CR) {
						cvsFile = readFile(pathText);
						pageComplete();
					}
				}
			});
			pathText.addTraverseListener(new TraverseListener() {
				public void keyTraversed(TraverseEvent e) {
					// on a TAB - check if file exist and read it
					switch (e.detail) {
					case SWT.TRAVERSE_TAB_NEXT:
					case SWT.TRAVERSE_TAB_PREVIOUS: {
						cvsFile = readFile(pathText);
						if (cvsFile == null) {
							e.doit = false;
						}
					}
					}
				}
			});

			Button pathBrowse = editor.createButton(group,
					Translator.trans("classcification.filechooser.browse"));
			pathBrowse.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					FileDialog fileChooser = new FileDialog(PlatformUI
							.getWorkbench().getDisplay().getActiveShell());
					fileChooser.setText(Translator
							.trans("classcification.filechooser.title"));
					fileChooser.setFilterExtensions(new String[] { "*.csv",
							"*.*" });
					fileChooser.setFilterNames(new String[] {
							Translator
									.trans("classcification.filternames.csvfile"),
							Translator
									.trans("classcification.filternames.anyfile") });

					PreferenceUtil.setPathFilter(fileChooser);
					cvsFile = fileChooser.open();
					PreferenceUtil.setLastBrowsedPath(cvsFile);

					pathText.setText(cvsFile);

					// 20120903 levels comment out
					// try {
					// readLevels(new File(pathText.getText()));
					// } catch (Exception ex) {
					// levels = 1;
					// return;
					// }

					pageComplete();
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});

			// 20120903 comment out
			// // create nested codes
			// editor.createLabel(group,
			// Translator.trans("classcification.codenested.label"));
			// Combo codcombo = editor.createCombo(group, new String[] {
			// Translator.trans("classcification.codenested.seperate"),
			// Translator.trans("classcification.codenested.levels"),
			// Translator.trans("classcification.codenested.nonest") });
			// codcombo.select(1);
			// codcombo.setVisible(false);
			//
			// // level spinner
			// spinnerLabel = editor.createLabel(group, Translator
			// .trans("classcification.codenested.subseperatefrom"));
			// spinnerLabel.setVisible(false);
			// spinner = new Spinner(group, SWT.BORDER);
			// spinner.setMinimum(0);
			// spinner.setSelection(0);
			// spinner.setIncrement(1);
			// spinner.setPageIncrement(1);
			// spinner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
			// false, 1, 1));
			// spinner.setVisible(false);
			//
			// codcombo.addSelectionListener(new SelectionListener() {
			// @Override
			// public void widgetSelected(SelectionEvent e) {
			// codeImpl = ((Combo) e.getSource()).getSelectionIndex();
			// setLevelInput();
			// }
			//
			// @Override
			// public void widgetDefaultSelected(SelectionEvent e) {
			// // do nothing
			// }
			// });

			// loaded resources
			try {
				resources = PersistenceManager.getInstance().getResources();
			} catch (DDIFtpException e) {
				MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
						.getActiveShell(), Translator.trans("ErrorTitle"),
						e.getMessage());
			}

			String[] options = new String[resources.size()];
			int count = 0;
			for (DDIResourceType resource : resources) {
				options[count] = resource.getOrgName();
				count++;
			}
			editor.createLabel(group,
					Translator.trans("classcification.resource.select"));
			Combo combo = editor.createCombo(group, options);
			if (options.length == 1) {
				combo.select(0);
				selectedResource = resources.get(0);
			} else {
				combo.addSelectionListener(new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						Combo c = (Combo) event.getSource();
						selectedResource = resources.get(c.getSelectionIndex());
						pageComplete();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent event) {
						// do nothing
					}
				});
			}

			// finalize
			setControl(group);
			setPageComplete(false);
		}

		private String readFile(Text pathText) {
			if (!new File(pathText.getText()).exists()) {
				MessageDialog
						.openError(PlatformUI.getWorkbench().getDisplay()
								.getActiveShell(), Translator
								.trans("ErrorTitle"), Translator.trans(
								"classcification.filenotfound.message",
								pathText.getText()));
				setPageComplete(false);
				return null;
			}

			setPageComplete(true);
			return pathText.getText();
		}

		private void setLevelInput() {
			if (codeImpl == 0 && levels > 1) {
				spinner.setMaximum(levels);
				spinner.setSelection(0);
				spinner.setVisible(true);
				spinnerLabel.setVisible(true);
			} else {
				spinner.setVisible(false);
				spinnerLabel.setVisible(false);
			}
		}

		private void readLevels(File file) throws Exception {
			CSVReader reader = new CSVReader(new FileReader(file));
			String[] cells;
			String empty = "";
			boolean emptyLine = true;

			levels = 0;
			while ((cells = reader.readNext()) != null) {
				// test for end of level definition
				for (int i = 0; i < cells.length; i++) {
					if (!cells[i].equals(empty)) {
						emptyLine = false;
					}
				}
				if (emptyLine) {
					break;
				}
				levels++;
				emptyLine = true;
			}
			setLevelInput();
		}
	}
}
