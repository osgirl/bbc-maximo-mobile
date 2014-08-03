package com.mro.mobile.app.mobilewo;

import com.mro.mobile.MobileApplicationException;
import com.mro.mobile.app.DefaultEventHandler;
import com.mro.mobile.ui.MobileMboDataBean;
import com.mro.mobile.ui.UIHandlerManager;
import com.mro.mobile.ui.event.UIEvent;
import com.mro.mobile.ui.event.UIEventHandler;
import com.mro.mobile.ui.res.UIUtil;
import com.mro.mobile.ui.res.controls.AbstractMobileControl;
import com.mro.mobileapp.WOApp;


/**
 * Custom Default Handler for the BBC Maximo Mobile implementation
 *
 */
public class CustomWOHandler extends DefaultEventHandler {
    private static final String mobileStatusList = "DISPATCH,TRAVEL,ONSITE,RETURN,START,ONHOLD,WOCOMP,COMP";
    private static final String dispatchList = "TRAVEL";
    private static final String travelList = "ONSITE,RETURN";
    private static final String onsiteList = "START,RETURN";
    private static final String startList = "ONHOLD,RETURN,WOCOMP";
    private static final String onholdList = "START";
    private static final String wocompList = "COMP";

	public boolean performEvent(UIEvent event) throws MobileApplicationException {
		if (event != null) {
			String eventId = event.getEventName();
			if (eventId.equalsIgnoreCase("filterstatusvalues")) {
				return filterstatusvalues(event);
			}
			if (eventId.equalsIgnoreCase("filterreturncodevalues")) {
				return filterreturncodevalues(event);
			}
	        if(eventId.equalsIgnoreCase("capturesignature")) {
	            return interceptCaptureSignature(event);
	        } 
	        if(eventId.equalsIgnoreCase("validateStatusChange")) {
	        	return interceptValidateChangeStatus(event);
	        } 
	        if(eventId.equals("validatepage")) {
	        	return interceptValidateChangeStatusPage(event);
	        } 
	        if(eventId.equals("mitigationcodechanged")) {
	        	return interceptMitigationCodeChanged(event);
	        } 
	        if(eventId.equals("hideIfNotRETURN")) {
	        	return hideIfNotRETURN(event);
	        }
	        if(eventId.equals("hideIfNotCOMP")) {
	        	return hideIfNotCOMP(event);
	        }
	        if(eventId.equals("readOnlyIfNotNew")) {
	        	return readOnlyIfNotNew(event);
	        }
		    if(eventId.equalsIgnoreCase("createPrimaryMultiEntry")) {
	            return interceptCreatePrimaryMultiEntry(event);
	        }
		}
		
		return UIUtil.getAppEventHandler().performEvent(event);
	}
	
	/**
	 * This method handles the event when the user clicks OK on the WO change status page.
	 * 
	 * It sets the actual start and finish dates based on the status the work order has been change to.  If the status has been
	 * changed to RETURN and work log is created.
	 * 
	 * @param event
	 * @return
	 * @throws MobileApplicationException
	 */
	public boolean interceptValidateChangeStatusPage(UIEvent event) throws MobileApplicationException {
		UIHandlerManager hMan = UIHandlerManager.getInstance();
		UIEventHandler wochangestatushandler = hMan.getUIHandler("wochangestatushandler");
		if(wochangestatushandler != null){
			wochangestatushandler.performEvent(event);
			MobileMboDataBean wodatabean = ((AbstractMobileControl) event.getCreatingObject()).getDataBean();
			String status = (wodatabean.getValue("STATUS") == null) ? "" : wodatabean.getValue("STATUS");
			if (status != null) {
				// When the work order’s status is changed to START the mobile work manager will automatically set the actual start date 
				if(status.equalsIgnoreCase("START") && wodatabean.getValue("IRVOLDSTATUS").equals("ONSITE")) {
					wodatabean.getMobileMbo().setDateValue("ACTSTART",wodatabean.getCurrentTime());
				} 
				// When the work order’s status is changed to WOCOMP the mobile work manager will automatically set the actual finish date 
				else if(status.equalsIgnoreCase("WOCOMP")) {
					wodatabean.getMobileMbo().setDateValue("ACTFINISH",wodatabean.getCurrentTime());
				} 
				else if(status.equalsIgnoreCase("RETURN")) {
					// Need a Return reason code.  
					if(wodatabean.getValue("IRV_RETURNSTATUSCODE").length()==0) {
						throw new MobileApplicationException("irvReturnStatusCode");
					}
					// If there is a return reason code create a work log entry and continue
					MobileMboDataBean worklogBean = wodatabean.getDataBean("WORKLOG");
					worklogBean.insert();
					worklogBean.setValue("SITEID", wodatabean.getValue("SITEID"));
					worklogBean.setValue("RECORDKEY", wodatabean.getValue("WONUM"));
					worklogBean.setValue("CLASS", "WORKORDER");
					worklogBean.setValue("CREATEBY", UIUtil.getApplication().getCurrentUser().toUpperCase());
					worklogBean.getMobileMbo().setDateValue("CREATEDATE", wodatabean.getCurrentTime());
					worklogBean.setValue("LOGTYPE", "WORK");
					worklogBean.setValue("CLIENTVIEWABLE", "1");
					worklogBean.setValue("DESCRIPTION", "The work order was returned.");
					worklogBean.setValue("DESCRIPTION_LONGDESCRIPTION", "The work order was returned with the reason: " + wodatabean.getValue("IRV_RETURNSTATUSCODE"));

					worklogBean.getDataBeanManager().save();
				}
				else if(status.equalsIgnoreCase("COMP")) {
					MobileMboDataBean attachments = wodatabean.getDataBean("WOATTACHMENTS");
					int attachmentCount = attachments.count();
					if (attachmentCount > 0) {
						boolean signatureFound=false;
						for(int i=0;i<attachmentCount;i++) {
							if(attachments.getMobileMbo(i).getBooleanValue("IRVISSIGNATURE")) {
								signatureFound=true;
								break;
							}
						}
						if (!signatureFound) {
							if(wodatabean.getValue("IRV_NOSIGNATURECODE").length()==0) {
								throw new MobileApplicationException("irvNoSignature");
							}
						}
					} else {
						if(wodatabean.getValue("IRV_NOSIGNATURECODE").length()==0) {
							throw new MobileApplicationException("irvNoSignature");
						}
					}
					// Commented out as failure code reference data will not be in for go-live			
//					// A failure code is required before a work order can be completed.
//					String failureclass = (wodatabean.getValue("FAILURECODE") == null) ? "" : wodatabean.getValue("FAILURECODE");
//					String problem = (wodatabean.getValue("FAILURECODE1") == null) ? "" : wodatabean.getValue("FAILURECODE1");
//					String cause = (wodatabean.getValue("FAILURECODE2") == null) ? "" : wodatabean.getValue("FAILURECODE2");
//					String remedy = (wodatabean.getValue("FAILURECODE3") == null) ? "" : wodatabean.getValue("FAILURECODE3");
//
//					if (failureclass.trim().length() <= 0) {
//						throw new MobileApplicationException("irvFailValidation", new Object[] { "Failure Class" });
//					} else if (problem.trim().length() <= 0) {
//						throw new MobileApplicationException("irvFailValidation", new Object[] { "Problem" });
//					} else if (cause.trim().length() <= 0) {
//						throw new MobileApplicationException("irvFailValidation", new Object[] { "Cause" });
//					} else if (remedy.trim().length() <= 0) {
//						throw new MobileApplicationException("irvFailValidation", new Object[] { "Remedy" });
//					}
					
				} 
				else if (status.equalsIgnoreCase("START")) {
					// Validate that a risk assessment has been performed
					String riskAssessmentFlag = wodatabean.getValue("IRVRISKASSESSMENT");
			
					if (riskAssessmentFlag == null || riskAssessmentFlag.trim().length() <=0 || riskAssessmentFlag.trim().equalsIgnoreCase("NO")) {
						throw new MobileApplicationException("irvRAFReqd");
					}
				}
				
				wodatabean.getMobileMbo().setDateValue("NEWSTATUSASOFDATE",wodatabean.getCurrentTime());
			}
		}
		return EVENT_HANDLED;
	}
	
	/**
	 * Validation rules for work order status changes
	 * 
	 * @param event
	 * @return
	 * @throws MobileApplicationException
	 */
	public boolean interceptValidateChangeStatus(UIEvent event) throws MobileApplicationException {
		String status = (String) event.getValue();
		MobileMboDataBean wodatabean = ((AbstractMobileControl) event.getCreatingObject()).getDataBean();
		wodatabean.setValue("IRVOLDSTATUS", wodatabean.getValue("STATUS"));
			
		if (status!= null && status.equalsIgnoreCase("START")) {
			// Validate that a risk assessment has been performed
			String riskAssessmentFlag = wodatabean.getValue("IRVRISKASSESSMENT");
	
			if (riskAssessmentFlag == null || riskAssessmentFlag.trim().length() <=0 || riskAssessmentFlag.trim().equalsIgnoreCase("NO")) {
				throw new MobileApplicationException("irvRAFReqd");
			}
		}
		
		wodatabean.getMobileMbo().setDateValue("NEWSTATUSASOFDATE",wodatabean.getCurrentTime());
		
		return EVENT_HANDLED;
	}
	
	
    /**
     * Make the doclinks record as a signature attachment
     * @param event
     * @return
     * @throws MobileApplicationException
     */
	public boolean interceptCaptureSignature(UIEvent event) throws MobileApplicationException {
		if(UIUtil.getAppEventHandler().performEvent(event)){
			MobileMboDataBean databean = ((AbstractMobileControl)event.getCreatingObject()).getDataBean();
	        if(databean != null)
	        {
	            databean.getMobileMbo().setDateValue("CHANGEDATE", databean.getCurrentTime());
	            databean.getMobileMbo().setBooleanValue("ISSIGNATURE", true);
	            databean.getMobileMbo().setBooleanValue("IRVISSIGNATURE", true);
	        }
		}
        return true;
    }	

	
	/**
	 * Filters the available next status values based on the current status.
	 * 
	 * @param event
	 * @return
	 * @throws MobileApplicationException
	 */
	public boolean filterstatusvalues(UIEvent event) throws MobileApplicationException {
    	MobileMboDataBean wodatabean = UIUtil.getCurrentScreen().getDataBean();
		String status = (wodatabean.getValue("STATUS") == null) ? "" : wodatabean.getValue("STATUS");

        if(wodatabean != null) {
        	MobileMboDataBean dropdownbean = (MobileMboDataBean)event.getValue();
            for(int i = dropdownbean.count() - 1; i >= 0; i--) {
                System.out.println(dropdownbean.getValue(i, "VALUE"));
                
                String ddValue = dropdownbean.getValue(i, "VALUE");
                if(status.equals("DISPATCH") && dispatchList.indexOf(ddValue)== -1) {
                	dropdownbean.remove(i);
                } else if(status.equals("TRAVEL") && travelList.indexOf(ddValue)== -1) {
	            	dropdownbean.remove(i);
                } else if(status.equals("ONSITE") && onsiteList.indexOf(ddValue)== -1) {
	            	dropdownbean.remove(i);
                } else if(status.equals("START") && ((startList.indexOf(ddValue)== -1) || ddValue.equals("COMP"))) {
	            	dropdownbean.remove(i);
                } else if(status.equals("ONHOLD") && onholdList.indexOf(ddValue)== -1) {
	            	dropdownbean.remove(i);
                } else if(status.equals("WOCOMP") && wocompList.indexOf(ddValue)== -1) {
	            	dropdownbean.remove(i);
	            } else if (mobileStatusList.indexOf(ddValue) == -1) {
	            	dropdownbean.remove(i);
	            }
            
            }
        }
        return true;
    }

	/**
	 * Filters the available return status code values based on the existing status.
	 * 
	 * @param event
	 * @return
	 * @throws MobileApplicationException
	 */
	public boolean filterreturncodevalues(UIEvent event) throws MobileApplicationException {
    	MobileMboDataBean wodatabean = UIUtil.getCurrentScreen().getDataBean();
		String status = (wodatabean.getValue("STATUS") == null) ? "" : wodatabean.getValue("STATUS");

		// Note that the reason code for abandon can only be applied if the status is being changed from TRAVEL.
        if(wodatabean != null) {
        	MobileMboDataBean dropdownbean = (MobileMboDataBean)event.getValue();
            for(int i = dropdownbean.count() - 1; i >= 0; i--) {            	
                System.out.println(dropdownbean.getValue(i, "VALUE"));                
                String ddValue = dropdownbean.getValue(i, "VALUE");
                if(!status.equals("TRAVEL") && ddValue.equalsIgnoreCase("Abandon")) {
                	dropdownbean.remove(i);
	            }
            
            }
        }
        return true;
    }


	/**
	 * Hides the drop down for Return Status Code on the Change Status page if the status is not RETURN
	 * 
	 * @param event
	 * @return
	 * @throws MobileApplicationException
	 */
	public boolean hideIfNotRETURN(UIEvent event) throws MobileApplicationException {
		MobileMboDataBean databean = UIUtil.getCurrentScreen().getDataBean();
		String status = databean.getValue("NEWSTATUSDISPLAY");

		if (status.equals("RETURN")) {
			((AbstractMobileControl)event.getCreatingObject()).setVisibility(true);
		} else {
			((AbstractMobileControl)event.getCreatingObject()).setVisibility(false);
		}
		return EVENT_HANDLED;
	}
	
	public boolean readOnlyIfNotNew(UIEvent event) throws MobileApplicationException{
			    MobileMboDataBean wodatabean = ((AbstractMobileControl)event.getCreatingObject()).getDataBean();
			    if (wodatabean != null)
			    {
			      WOApp app = (WOApp)UIUtil.getApplication();
			      String status = app.getInternalValue(wodatabean, "WOSTATUS", wodatabean.getValue("STATUS"));
			     
			      if (status.equals("WAPPR")) {
			        ((AbstractMobileControl)event.getCreatingObject()).setReadonly(false);
			      } else {
			        ((AbstractMobileControl)event.getCreatingObject()).setReadonly(true);
			      }
			    }
			    return EVENT_HANDLED;
			  }

	/**
	 * Hides the drop down for No Signature Code on the Change Status page if the status is not COMP
	 * 
	 * @param event
	 * @return
	 * @throws MobileApplicationException
	 */
	public boolean hideIfNotCOMP(UIEvent event) throws MobileApplicationException {
		MobileMboDataBean databean = UIUtil.getCurrentScreen().getDataBean();
		String status = databean.getValue("NEWSTATUSDISPLAY");

		if (status.equals("COMP")) {
			((AbstractMobileControl)event.getCreatingObject()).setVisibility(true);
		} else {
			((AbstractMobileControl)event.getCreatingObject()).setVisibility(false);
		}
		return EVENT_HANDLED;
	}

	/**
	 * Default the mitigation status on the work order to requested.
	 * 
	 * @param event
	 * @return
	 * @throws MobileApplicationException
	 */
	public boolean interceptMitigationCodeChanged(UIEvent event) throws MobileApplicationException {
		String mitigationCode = (String) event.getValue();
		MobileMboDataBean wodatabean = ((AbstractMobileControl) event.getCreatingObject()).getDataBean();

		String excusingStatus = (wodatabean.getValue("IRV_EXCSTATUS") == null) ? "" : wodatabean.getValue("IRV_EXCSTATUS");
		
		if (mitigationCode.trim().length() > 0) {
			if(excusingStatus.trim().length()<=0) {
				wodatabean.setValue("IRV_EXCSTATUS", "REQUESTED");
			}
		} 
		
		return EVENT_HANDLED;
	}

	/**
	 * When creating a follow-up SR and the assetnum was not specified the SR was not created when synchonised
	 * with Maximo.  The error was BMXAA4198E - The GL attribute GLACCOUNT on object {1} requires an organization. Specify a value in the Organization field.
	 * 
	 * @param event
	 * @return
	 * @throws MobileApplicationException
	 */
	public boolean interceptCreatePrimaryMultiEntry(UIEvent event) throws MobileApplicationException {
		MobileMboDataBean wodatabean = ((AbstractMobileControl) event.getCreatingObject()).getDataBean();
		
		if (wodatabean.getValue("ASSETSITEID") == "") {
			wodatabean.setValue("ASSETSITEID",wodatabean.getValue("SITEID"));
			wodatabean.setValue("ASSETORGID",wodatabean.getValue("ORGID"));
		}

		// Default the owner group to the NSC service Desk if there are no other owner assignments
		if (wodatabean.getValue("OWNERGROUP") == "" && wodatabean.getValue("OWNER")== "") {
			wodatabean.setValue("NEWOWNERGROUP","NSCSD");
			wodatabean.setValue("OWNERGROUP","NSCSD");
		}

		return EVENT_HANDLED;
	}
	
//	Stay with core product for this.  i.e. should not allow modification of location and asset when INPRG
//	private boolean isReadOnlyBasedOnStatus(UIEvent event) throws MobileApplicationException {
//		MobileMboDataBean wodatabean = ((AbstractMobileControl) event.getCreatingObject()).getDataBean();
//		if (wodatabean != null) {
//			String status = ((WOApp) UIUtil.getApplication()).getInternalValue(wodatabean, "WOSTATUS", wodatabean.getValue("STATUS"));
//			if ("WMATL,COMP,CLOSE,CAN".indexOf(status) != -1) {
//				((AbstractMobileControl) event.getCreatingObject()).setReadonly(true);
//			} else {
//				((AbstractMobileControl) event.getCreatingObject()).setReadonly(false);
//			}
//		}
//		return true;
//	}
}
