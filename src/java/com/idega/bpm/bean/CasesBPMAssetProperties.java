package com.idega.bpm.bean;

public class CasesBPMAssetProperties {

	private String caseId, processorType, commentsPersistenceManagerIdentifier,specialBackPage, inactiveTasksToShow;

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
					addExportContacts = false,
					showUserCompany = false,
					showLastLoginDate = false,
					allowToReloadCaseView = true,
					showSettingsButton = true;

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

	public boolean isShowUserCompany() {
		return showUserCompany;
	}

	public void setShowUserCompany(boolean showUserCompany) {
		this.showUserCompany = showUserCompany;
	}

	public boolean isShowLastLoginDate() {
		return showLastLoginDate;
	}

	public void setShowLastLoginDate(boolean showLastLoginDate) {
		this.showLastLoginDate = showLastLoginDate;
	}

	public boolean isAllowToReloadCaseView() {
		return allowToReloadCaseView;
	}

	public void setAllowToReloadCaseView(boolean allowToReloadCaseView) {
		this.allowToReloadCaseView = allowToReloadCaseView;
	}

	public boolean isShowSettingsButton() {
		return showSettingsButton;
	}

	public void setShowSettingsButton(boolean showSettingsButton) {
		this.showSettingsButton = showSettingsButton;
	}

	public String getInactiveTasksToShow() {
		return inactiveTasksToShow;
	}

	public void setInactiveTasksToShow(String inactiveTasksToShow) {
		this.inactiveTasksToShow = inactiveTasksToShow;
	}

}