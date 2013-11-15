package com.idega.bpm.bean;

public class CasesBPMAssetProperties {

	private String caseId, processorType, commentsPersistenceManagerIdentifier,specialBackPage;

	private boolean usePDFDownloadColumn = true,
					allowPDFSigning = true,
					hideEmptySection,
					showAttachmentStatistics,
					showOnlyCreatorInContacts,
					autoShowComments,
					showLogExportButton,
					showComments = true,
					showContacts = true,
					isNameFromExternalEntity = false,
					showUserProfilePicture = Boolean.TRUE,
					addExportContacts;

	public boolean isNameFromExternalEntity() {
		return isNameFromExternalEntity;
	}

	public void setNameFromExternalEntity(boolean isNameFromExternalEntity) {
		this.isNameFromExternalEntity = isNameFromExternalEntity;
	}

	public boolean isShowLogExportButton() {
		return showLogExportButton;
	}

	public void setShowLogExportButton(boolean showLogExportButton) {
		this.showLogExportButton = showLogExportButton;
	}

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

	public String getSpecialBackPage() {
		return specialBackPage;
	}

	public void setSpecialBackPage(String specialBackPage) {
		this.specialBackPage = specialBackPage;
	}

	public boolean isShowComments() {
		return showComments;
	}

	public void setShowComments(boolean showComments) {
		this.showComments = showComments;
	}

	public boolean isShowContacts() {
		return showContacts;
	}

	public void setShowContacts(boolean showContacts) {
		this.showContacts = showContacts;
	}

	public boolean isShowUserProfilePicture() {
		return showUserProfilePicture;
	}

	public void setShowUserProfilePicture(boolean showUserProfilePicture) {
		this.showUserProfilePicture = showUserProfilePicture;
	}

	public boolean isAddExportContacts() {
		return addExportContacts;
	}

	public void setAddExportContacts(boolean addExportContacts) {
		this.addExportContacts = addExportContacts;
	}

}