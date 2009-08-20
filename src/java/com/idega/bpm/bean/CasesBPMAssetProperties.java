package com.idega.bpm.bean;

public class CasesBPMAssetProperties {
	
	private String caseId;
	private String processorType;
	private String commentsPersistenceManagerIdentifier;
	
	private boolean usePDFDownloadColumn = true;
	private boolean allowPDFSigning = true;
	private boolean hideEmptySection;
	private boolean showAttachmentStatistics;
	private boolean showOnlyCreatorInContacts;
	private boolean autoShowComments;
	
	public String getCaseId() {
		return caseId;
	}
	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}
	public String getProcessorType() {
		return processorType;
	}
	public void setProcessorType(String processorType) {
		this.processorType = processorType;
	}
	public boolean isUsePDFDownloadColumn() {
		return usePDFDownloadColumn;
	}
	public void setUsePDFDownloadColumn(boolean usePDFDownloadColumn) {
		this.usePDFDownloadColumn = usePDFDownloadColumn;
	}
	public boolean isAllowPDFSigning() {
		return allowPDFSigning;
	}
	public void setAllowPDFSigning(boolean allowPDFSigning) {
		this.allowPDFSigning = allowPDFSigning;
	}
	public boolean isHideEmptySection() {
		return hideEmptySection;
	}
	public void setHideEmptySection(boolean hideEmptySection) {
		this.hideEmptySection = hideEmptySection;
	}
	public String getCommentsPersistenceManagerIdentifier() {
		return commentsPersistenceManagerIdentifier;
	}
	public void setCommentsPersistenceManagerIdentifier(String commentsPersistenceManagerIdentifier) {
		this.commentsPersistenceManagerIdentifier = commentsPersistenceManagerIdentifier;
	}
	public boolean isShowAttachmentStatistics() {
		return showAttachmentStatistics;
	}
	public void setShowAttachmentStatistics(boolean showAttachmentStatistics) {
		this.showAttachmentStatistics = showAttachmentStatistics;
	}
	public boolean isShowOnlyCreatorInContacts() {
		return showOnlyCreatorInContacts;
	}
	public void setShowOnlyCreatorInContacts(boolean showOnlyCreatorInContacts) {
		this.showOnlyCreatorInContacts = showOnlyCreatorInContacts;
	}
	public boolean isAutoShowComments() {
		return autoShowComments;
	}
	public void setAutoShowComments(boolean autoShowComments) {
		this.autoShowComments = autoShowComments;
	}

}
