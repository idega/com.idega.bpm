package com.idega.bpm.pdf.servlet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.FinderException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.process.business.CaseBusiness;
import com.idega.block.process.business.CaseCodeManager;
import com.idega.block.process.data.Case;
import com.idega.block.process.data.CaseHome;
import com.idega.block.process.data.CaseLog;
import com.idega.bpm.BPMConstants;
import com.idega.business.IBOLookup;
import com.idega.business.IBORuntimeException;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.data.IDOLookup;
import com.idega.graphics.generator.business.PDFGenerator;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.io.DownloadWriter;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Table2;
import com.idega.presentation.TableCell2;
import com.idega.presentation.TableRow;
import com.idega.presentation.TableRowGroup;
import com.idega.presentation.text.Heading1;
import com.idega.presentation.text.Text;
import com.idega.user.data.User;
import com.idega.util.FileUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.idega.util.text.Name;


/**
 * Writes Case Logs to PDF
 * @author <a href="mailto:aleksandras@idega.com>Aleksandras sivkovas</a>
 * Created: 2011.04.29
 */

public class CaseLogsToPDFWriter extends DownloadWriter {

	public static final String CASE_ID_PARAMETER = "caseLogIdToDownload";

	private static final Logger LOGGER = Logger.getLogger(CaseLogsToPDFWriter.class.getName());

	private Case theCase;

	@Autowired
	private PDFGenerator pdfGenerator;

	private byte[] pdfBytes = null;

	@Override
	public String getMimeType() {
		return MimeTypeUtil.MIME_TYPE_PDF_1;
	}

	private boolean hasAccess(String userId, Collection<User> users) {
		if (StringUtil.isEmpty(userId) || ListUtil.isEmpty(users)) {
			return false;
		}

		for (User user: users) {
			if (userId.equals(user.getId())) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void init(HttpServletRequest req, IWContext iwc) {
		try {
			ELUtil.getInstance().autowire(this);

			String caseUniqueId = iwc.getParameter(CASE_ID_PARAMETER);

			if (StringUtil.isEmpty(caseUniqueId)) {
				LOGGER.log(Level.SEVERE, "Do not know what to download: caseId is null");
				return;
			}

			if (iwc == null || !iwc.isLoggedOn()) {
				return;
			}

			CaseHome caseHome = (CaseHome) IDOLookup.getHome(Case.class);
			theCase = caseHome.findCaseByUniqueId(caseUniqueId);
			if (theCase == null) {
				return;
			}

			boolean canAccess = iwc.isSuperAdmin();
			com.idega.user.data.bean.User user = null;
			if (!canAccess) {
				user = iwc.getLoggedInUser();
				String userId = user.getId().toString();

				//	Owner or creator
				Collection<User> users = new ArrayList<>();
				User caseOwner = theCase.getOwner();
				if (caseOwner != null) {
					users.add(caseOwner);
				}
				User caseCreator = theCase.getCreator();
				if (caseCreator != null) {
					users.add(caseCreator);
				}
				canAccess = hasAccess(userId, users);

				//	Subscribers
				if (!canAccess) {
					canAccess = hasAccess(userId, theCase.getSubscribers());
				}

				//	Admins, managers and handlers
				if (!canAccess) {
					Set<String> userRoles = iwc.getAccessController().getAllRolesForUser(user);
					if (!ListUtil.isEmpty(userRoles)) {
						for (Iterator<String> iter = userRoles.iterator(); (!canAccess && iter.hasNext());) {
							String role = iter.next();
							if (StringUtil.isEmpty(role)) {
								continue;
							}

							role = role.toLowerCase();
							if (role.indexOf("admin") != -1 || role.indexOf("manager") != -1 || role.indexOf("handler") != -1) {
								canAccess = true;
							}
						}
					}
				}
			}
			if (!canAccess) {
				LOGGER.warning(user + " has no access to case " + theCase);
				return;
			}

			CaseBusiness caseBusiness = CaseCodeManager.getInstance().getCaseBusinessOrDefault(theCase.getCaseCode(), iwc);

			IWBundle bundle = iwc.getIWMainApplication().getBundle(BPMConstants.IW_BUNDLE_STARTER);
			IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
			Layer container = new Layer();
			container.add("<link href=\"" + bundle.getVirtualPathWithFileNameString("style/case_logs_pdf_style.css") +"\" type=\"text/css\" />");
			container.add(new Heading1(theCase.getCaseIdentifier() + " - " + theCase.getSubject()));

			Table2 table = new Table2();
			table.setWidth("100%");
			container.add(table);

			TableRowGroup group = table.createHeaderRowGroup();
			TableRow row = group.createRow();

			TableCell2 cell = row.createHeaderCell();
			cell.add(new Text(iwrb.getLocalizedString("case.performer", "Performer")));

			cell = row.createHeaderCell();
			cell.add(new Text(iwrb.getLocalizedString("case.action", "Action")));

			cell = row.createHeaderCell();
			cell.add(new Text(iwrb.getLocalizedString("case.status_after", "Status after")));

			cell = row.createHeaderCell();
			cell.add(new Text(iwrb.getLocalizedString("case.timestamp", "Timestamp")));

			cell = row.createHeaderCell();
			cell.add(new Text(iwrb.getLocalizedString("case.comment", "Comment")));

			group = table.createBodyRowGroup();

			List<CaseLog> logs = new ArrayList<>(this.getLogs(iwc));
			if(ListUtil.isEmpty(logs)){
				container.add(iwrb.getLocalizedString("no_logs_found", "There are no logs"));
			} else {
				Collections.reverse(logs);
				for (CaseLog log: logs) {
					row = group.createRow();
					User performer = log.getPerformer();

					String action = "";
					String comment = log.getComment() != null ? log.getComment() : "";

					if (comment.indexOf(": ") != -1) {
						action = comment.substring(0, comment.indexOf(": "));
						comment = action.length() < comment.length() ? comment.substring(comment.indexOf(": ") + 2) : "";
					}

					cell = row.createCell();
					cell.add(new Text(performer != null ? new Name(performer.getFirstName(), performer.getMiddleName(), performer.getLastName()).getName(iwc.getCurrentLocale()) : ""));

					cell = row.createCell();
					cell.add(new Text(action));

					cell = row.createCell();
					cell.add(new Text(caseBusiness.getLocalizedCaseStatusDescription(theCase, log.getCaseStatusAfter(), iwc.getCurrentLocale())));

					cell = row.createCell();
					cell.add(new Text(new IWTimestamp(log.getTimeStamp()).getLocaleDateAndTime(iwc.getCurrentLocale(), IWTimestamp.SHORT, IWTimestamp.SHORT)));

					cell = row.createCell();
					cell.add(new Text(comment));
				}
			}

			pdfBytes = this.pdfGenerator.getBytesOfGeneratedPDF(iwc, container, true, true);

			setAsDownload(iwc, this.getFileName(), this.pdfBytes.length);
		}
		catch (RemoteException re) {
			throw new IBORuntimeException(re);
		}
		catch (FinderException fe) {
			fe.printStackTrace();
		}
	}

	private Collection<CaseLog> getLogs(IWContext iwc) {
		try {
			CaseBusiness caseBusiness = IBOLookup.getServiceInstance(iwc, CaseBusiness.class);
			return caseBusiness.getCaseLogsByCase(theCase);
		}
		catch (FinderException fe) {
			LOGGER.log(Level.SEVERE, fe.getMessage(), fe);
		}
		catch (RemoteException re) {
			LOGGER.log(Level.SEVERE, re.getMessage(), re);
		}
		return Collections.emptyList();
	}

	@Override
	public void writeTo(IWContext iwc, OutputStream out) throws IOException {
		InputStream streamIn = new ByteArrayInputStream(pdfBytes);
		FileUtil.streamToOutputStream(streamIn, out);

		out.flush();
		out.close();
		out.close();
		streamIn.close();
	}

	@Override
	public String getFileName() {
		return "Case_" + theCase.getCaseIdentifier() + "_logs.pdf";
	}
}