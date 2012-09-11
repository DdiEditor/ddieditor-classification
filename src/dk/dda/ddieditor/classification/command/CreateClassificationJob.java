package dk.dda.ddieditor.classification.command;

import java.io.FileReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CategorySchemeDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CategorySchemeType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CategoryType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeSchemeDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeSchemeType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.CodeType;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.LevelDocument;
import org.ddialliance.ddi3.xml.xmlbeans.logicalproduct.LevelType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.AbstractMaintainableType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.AbstractVersionableType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.LabelType;
import org.ddialliance.ddi3.xml.xmlbeans.reusable.StructuredStringType;
import org.ddialliance.ddieditor.logic.identification.IdentificationManager;
import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.model.lightxmlobject.LightXmlObjectType;
import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.ui.dbxml.DaoHelper;
import org.ddialliance.ddieditor.ui.dbxml.code.CodeSchemeDao;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.model.AnyModel;
import org.ddialliance.ddieditor.ui.model.ElementType;
import org.ddialliance.ddieditor.ui.model.code.CodeScheme;
import org.ddialliance.ddieditor.ui.preference.PreferenceUtil;
import org.ddialliance.ddieditor.util.LightXmlObjectUtil;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.ddialliance.ddiftp.util.log.Log;
import org.ddialliance.ddiftp.util.log.LogFactory;
import org.ddialliance.ddiftp.util.log.LogType;
import org.ddialliance.ddiftp.util.xml.XmlBeansUtil;

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

/*
 * CreateClassificationJob is inspired by the project: Virgil UI by: Samuel Spencer, see: http://code.google.com/p/virgil-ui/
 */

/**
 * Parse CSV and store DDI-L
 */
public class CreateClassificationJob implements Runnable {
	private Log log = LogFactory.getLog(LogType.SYSTEM,
			CreateClassificationJob.class);

	DDIResourceType selectedResource = null;
	String csvFile = null;
	String label;
	String description;
	int codeImpl;

	public CodeSchemeDocument cods;
	public CategorySchemeDocument cats;

	List<LevelType> levels = new ArrayList<LevelType>();
	Map<Integer, CodeType> levelCodeMap = new HashMap<Integer, CodeType>();
	Map<Integer, CodeSchemeDocument> codeSchemeMap = new HashMap<Integer, CodeSchemeDocument>();
	Map<Integer, CategorySchemeDocument> catSchemeMap = new HashMap<Integer, CategorySchemeDocument>();

	public CreateClassificationJob(DDIResourceType selectedResource,
			String inCsvFile, String label, String description, int codeImpl) {
		this.selectedResource = selectedResource;
		this.csvFile = inCsvFile;
		this.label = label;
		this.description = description;
		this.codeImpl = codeImpl;
	}

	@Override
	public void run() {
		try {
			// code scheme init
			cods = CodeSchemeDocument.Factory.newInstance();
			cods.addNewCodeScheme();
			initCodeScheme(cods.getCodeScheme(), label, description);

			// cat scheme init
			cats = CategorySchemeDocument.Factory.newInstance();
			cats.addNewCategoryScheme();
			initCategoryScheme(cats.getCategoryScheme(), label, description);

			// parse csv
			try {
				parseCsv();
			} catch (Exception e) {
				DDIFtpException ex = new DDIFtpException(Translator.trans(
						"classcification.error.cvsfileparseerror", csvFile));
				ex.setRealThrowable(e);
				throw ex;
			}

			// post process
			postProcess();

			// store ddi
			storeDdi();
		} catch (Exception e) {
			Editor.showError(e, null);
		}
	}

	private void postProcess() throws DDIFtpException {
		// set levels
		cods.getCodeScheme().setLevelArray(levels.toArray(new LevelType[] {}));

		if (codeImpl > 0) {
			// cat scheme reference
			IdentificationManager.getInstance().addReferenceInformation(
					cods.getCodeScheme().addNewCategorySchemeReference(),
					LightXmlObjectUtil.createLightXmlObject(null, null, cats
							.getCategoryScheme().getId(), cats
							.getCategoryScheme().getVersion(),
							ElementType.CATEGORY_SCHEME.getElementName()));
		} else {
			// create references to level code schemes
			if (!codeSchemeMap.isEmpty()) {
				for (CodeSchemeDocument doc : codeSchemeMap.values()) {
					IdentificationManager.getInstance()
							.addReferenceInformation(
									cods.getCodeScheme()
											.addNewCodeSchemeReference(),
									LightXmlObjectUtil.createLightXmlObject(
											null, null, doc.getCodeScheme()
													.getId(), doc
													.getCodeScheme()
													.getVersion(),
											ElementType.CODE_SCHEME
													.getElementName()));
				}
			}
		}

		// log
		if (log.isDebugEnabled()) {
			log.debug(cats);
			log.debug(cods);
			if (!codeSchemeMap.isEmpty()) {
				for (CodeSchemeDocument doc : codeSchemeMap.values()) {
					log.debug(doc);
				}
			}
		}
	}

	private void storeDdi() throws Exception {
		PersistenceManager.getInstance().setWorkingResource(
				selectedResource.getOrgName());

		// parent
		LightXmlObjectType parent = null;
		String pId = null, pVer = null;
		List<LightXmlObjectType> parents = DdiManager.getInstance()
				.getLogicalProductsLight(null, null, null, null)
				.getLightXmlObjectList().getLightXmlObjectList();
		if (parents.isEmpty()) {
			parent = LightXmlObjectUtil.createLightXmlObject(null, null, null,
					null, "logicalproduct__LogicalProduct");
		} else {
			parent = parents.get(0);
			pId = parents.get(0).getId();
			pVer = parents.get(0).getVersion();
		}

		// create cat scheme
		if (!cats.getCategoryScheme().getCategoryList().isEmpty()) {
			AnyModel anyModelCats = new AnyModel(cats.getCategoryScheme()
					.getId(), cats.getCategoryScheme().getVersion(), null,
					null, null);
			anyModelCats.setDocument(cats);
			DaoHelper.createScheme(anyModelCats, parent);
		}

		// create level category schemes
		if (!catSchemeMap.isEmpty()) {
			for (CategorySchemeDocument cats : catSchemeMap.values()) {
				DdiManager.getInstance().createElement(cats, pId, pVer,
						"logicalproduct__LogicalProduct");
			}
		}

		// create code scheme
		CodeSchemeDao codsDao = new CodeSchemeDao();
		CodeScheme codsModel = new CodeScheme(cods, pId, pVer);
		codsDao.create(codsModel);

		// create level code schemes
		if (!codeSchemeMap.isEmpty()) {
			for (CodeSchemeDocument doc : codeSchemeMap.values()) {
				codsModel = new CodeScheme(doc, pId, pVer);
				codsDao.create(codsModel);
			}
		}
	}

	public void parseCsv() throws Exception {
		CSVReader reader = new CSVReader(new FileReader(csvFile));
		String[] cells;
		int count = 1;

		String empty = "";
		boolean emptyLine = true;
		boolean dataStart = false;
		int levelCount = 1;

		// TODO error report on csv file -care for users!!!
		while ((cells = reader.readNext()) != null) {
			if (log.isDebugEnabled()) {
				log.debug("Line:" + count);
				StringBuilder msg = new StringBuilder();
				for (int i = 0; i < cells.length; i++) {
					msg.append(cells[i] + ", ");
				}
				log.debug(msg.toString());
			}

			// test for code begin - aka empty line
			for (int i = 0; i < cells.length; i++) {
				if (!cells[i].equals(empty)) {
					emptyLine = false;
				}
			}
			if (emptyLine) {
				// skip empty line
				dataStart = true;
				continue;
			}

			// parse levels and code
			for (int i = 0; i < cells.length; i++) {
				// levels
				if (!dataStart) {
					levelCount = levels.size();

					// level 1
					if (levelCount == 0) {
						createLevel(levelCount + 1, cells[i], cells[i + 1]);
						break;
					}

					// level 2
					if (levelCount == 1 && !cells[levelCount + 1].equals(empty)) {
						createLevel(levelCount + 1, cells[(levelCount + 1)],
								cells[(levelCount + 2)]);
						break;
					}

					// level n
					if (levelCount > 0 && !cells[levelCount + 2].equals(empty)) {
						createLevel(levelCount + 1, cells[(levelCount + 2)],
								cells[(levelCount + 3)]);
						break;
					}
				} else {
					// codes
					if (!cells[i].equals(empty)) {
						createCode(i, cells[i], cells[i + 1]);
						break;
					}
				}
			}

			// increment
			emptyLine = true;
			count++;
		}
	}

	private void createLevel(int number, String label, String text)
			throws Exception {
		LevelType type = LevelDocument.Factory.newInstance().addNewLevel();
		type.setName(label);
		setText(type.addNewDescription(), text);

		type.setLevelNumber(BigInteger.valueOf(number));
		levels.add(type);
	}

	private void createCode(int position, String codeTxt, String category)
			throws Exception {
		// level number
		int levelNumber = position;
		try {
			// handle division by zero
			levelNumber = position / 2;
		} catch (Exception e) {
			// do nothing
		}

		// check level code
		CodeType levelCode = levelCodeMap.get(levelNumber);

		// create code
		levelNumber++;
		CodeType code = createCodeImpl(levelCode, levelNumber, codeTxt,
				category);
		levelCodeMap.put(levelNumber, code);
	}

	private CodeType createCodeImpl(CodeType parent, int levelNumber,
			String codeTxt, String category) throws Exception {
		// category
		CategoryType cat = createCategory(category, levelNumber);

		// code
		CodeType code = null;
		switch (codeImpl) {
		// one code scheme per level
		case 0:
			if (codeSchemeMap.get(levelNumber) == null) {
				CodeSchemeDocument doc = CodeSchemeDocument.Factory
						.newInstance();
				doc.addNewCodeScheme();
				initCodeScheme(
						doc.getCodeScheme(),
						levels.get(levelNumber - 1).getName() + "-"
								+ levelNumber,
						XmlBeansUtil.getTextOnMixedElement(levels
								.get(levelNumber - 1).getDescriptionList()
								.get(0)));

				// cat scheme reference
				IdentificationManager.getInstance().addReferenceInformation(
						doc.getCodeScheme().addNewCategorySchemeReference(),
						LightXmlObjectUtil.createLightXmlObject(null, null,
								catSchemeMap.get(levelNumber)
										.getCategoryScheme().getId(),
								catSchemeMap.get(levelNumber)
										.getCategoryScheme().getVersion(),
								ElementType.CATEGORY_SCHEME.getElementName()));

				// add to map
				codeSchemeMap.put(levelNumber, doc);

				// code
				code = doc.getCodeScheme().addNewCode();
			} else {
				code = codeSchemeMap.get(levelNumber).getCodeScheme()
						.addNewCode();
			}
			break;

		// nest codes by level
		case 1:
			if (parent != null) {
				code = parent.addNewCode();
			} else {
				code = cods.getCodeScheme().addNewCode();
			}
			break;

		// no nesting -all in one code scheme
		case 2:
			code = cods.getCodeScheme().addNewCode();
			break;

		default:
			break;
		}

		// code values
		code.setValue(codeTxt);
		code.setLevelNumber(BigInteger.valueOf(levelNumber));

		// category reference
		IdentificationManager.getInstance().addReferenceInformation(
				code.addNewCategoryReference(),
				LightXmlObjectUtil.createLightXmlObject(cats
						.getCategoryScheme().getId(), cats.getCategoryScheme()
						.getVersion(), cat.getId(), cat.getVersion(),
						ElementType.CATEGORY.getElementName()));
		return code;
	}

	private CategoryType createCategory(String text, int levelNumber)
			throws Exception {
		CategoryType cat = null;
		// create separate category scheme
		if (codeImpl < 1) {
			if (catSchemeMap.get(levelNumber) == null) {
				CategorySchemeDocument doc = CategorySchemeDocument.Factory
						.newInstance();
				doc.addNewCategoryScheme();
				initCategoryScheme(
						doc.getCategoryScheme(),
						levels.get(levelNumber - 1).getName() + "-"
								+ levelNumber,
						XmlBeansUtil.getTextOnMixedElement(levels
								.get(levelNumber - 1).getDescriptionList()
								.get(0)));
				catSchemeMap.put(levelNumber, doc);
				cat = doc.getCategoryScheme().addNewCategory();
			} else {
				cat = catSchemeMap.get(levelNumber).getCategoryScheme()
						.addNewCategory();
			}
		} else {
			cat = cats.getCategoryScheme().addNewCategory();
		}
		addIdAndVersion(cat, ElementType.CATEGORY.getIdPrefix(), null);
		setText(cat.addNewLabel(), text);
		return cat;
	}

	private void initCategoryScheme(CategorySchemeType categoryScheme,
			String label, String description) throws DDIFtpException {
		addIdAndVersion(categoryScheme,
				ElementType.CATEGORY_SCHEME.getIdPrefix(), null);
		if (!label.equals("")) {
			setText(categoryScheme.addNewLabel(), label);
		}
		if (!description.equals("")) {
			setText(categoryScheme.addNewDescription(), description);
		}
	}

	private void initCodeScheme(CodeSchemeType codeScheme, String label,
			String description) throws DDIFtpException {
		addIdAndVersion(codeScheme, ElementType.CODE_SCHEME.getIdPrefix(), null);
		if (!label.equals("")) {
			setText(codeScheme.addNewLabel(), label);
		}
		if (!description.equals("")) {
			setText(codeScheme.addNewDescription(), description);
		}
	}

	private void addIdAndVersion(AbstractVersionableType abstractVersionable,
			String prefix, String postfix) throws DDIFtpException {
		IdentificationManager.getInstance().addIdentification(
				abstractVersionable, prefix, postfix);

		IdentificationManager.getInstance().addVersionInformation(
				(AbstractVersionableType) abstractVersionable, null, null);
	}

	private void addIdAndVersion(AbstractMaintainableType abstractIdentifiable,
			String prefix, String postfix) throws DDIFtpException {
		IdentificationManager.getInstance().addIdentification(
				abstractIdentifiable, prefix, postfix);
		IdentificationManager.getInstance().addVersionInformation(
				(AbstractVersionableType) abstractIdentifiable, null, null);
		abstractIdentifiable.setAgency(PreferenceUtil.getDdiAgency());
	}

	private void setText(LabelType label, String text) throws DDIFtpException {
		XmlBeansUtil.setTextOnMixedElement(label, text);
		XmlBeansUtil.addTranslationAttributes(label,
				Translator.getLocaleLanguage(), false, true);
	}

	private void setText(StructuredStringType struct, String text)
			throws DDIFtpException {
		XmlBeansUtil.setTextOnMixedElement(struct, text);
		XmlBeansUtil.addTranslationAttributes(struct,
				Translator.getLocaleLanguage(), false, true);
	}
}
