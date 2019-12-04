package tw.com.isecurity.mg.admin.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;

import tw.com.isecurity.mg.commons.Constants;
import tw.com.isecurity.mg.commons.MenuUtil;
import tw.com.isecurity.mg.hibernate.MgAdmin;
import tw.com.isecurity.mg.hibernate.MgAdminFunctions;
import tw.com.isecurity.mg.register.action.RegisterKeyAction;
import tw.com.isecurity.mg.service.AdminFunctionsService;
import tw.com.isecurity.mg.service.AdminService;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionSupport;

@SuppressWarnings({"serial", "rawtypes"})
public class AdminAction extends ActionSupport {
	
	@Resource
    private AdminService adminService;
	
	@Resource
    private AdminFunctionsService adminFunctionsService;
	
	private static Logger logger = Logger.getLogger(AdminAction.class);
	
	private String adminId;
	private String password;
	private String isAdmin;
	
	private MgAdmin mgAdmin;
	private List adminList;
	
	private boolean append;
	
	private String[] functions;
	private String[] editFunctions;
	
	private String redirectURL;
	
	// For read-only/editable privilege
	private static String PAGE_NAME = "Administrator List";
	private static boolean allowUserEdit = false;
	  	
	/**
	 * go to login page
	 */
	public String execute() {
		return "Login.Page";
	}
	
	/**
	 * go to admin form page
	 * @return
	 */
	public String go2AdminForm() {
		mgAdmin = new MgAdmin();
		return "Admin.Form";
	}
	public String auth() throws Exception{
    	ActionContext actionContext = ActionContext.getContext();
		Map sessionMap = actionContext.getSession();
    	MgAdmin userSession =  (MgAdmin) sessionMap.get("mgadmin");
    	redirectURL = MenuUtil.fetchFirstAction(userSession);
    	if (redirectURL == null) redirectURL = "../Admin/AdminAction.action";
    	
		if (Constants.DEBUG_URL == null || !Constants.DEBUG_URL.equals("1")) {
			try {
				if (redirectURL != null && redirectURL.startsWith("../")) {
					String urlScheme = (Constants.USE_SSL != null && Constants.USE_SSL.equals("1")) ? "https:" : "http:";
					String httpPrefix = urlScheme + Constants.WEB_URL_NO_SCHEME;
					logger.info("AdminAction():auth(): httpPrefix=" + httpPrefix);
					if (!httpPrefix.endsWith("/")) {
						httpPrefix += "/";
					}
					redirectURL = redirectURL.replaceFirst("../", httpPrefix);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		logger.info("AdminAction():auth(): redirectURL=" + redirectURL);
		return "success";
	}
	/** 
	 * get one admin data to edit
	 * @return
	 * @throws Exception
	 */
	public String getAdmin2Edit() throws Exception {
    	mgAdmin = adminService.getAdmin(adminId);
    	mgAdmin.setFunctions(getAdminFunctions(mgAdmin));
    	mgAdmin.setEditFunctions(getEditableAdminFunctions(mgAdmin));
    	return "Admin.Form";
    }
	
	/**
	 * create or update admin data to save
	 * @return
	 * @throws Exception
	 */
	public String saveAdmin() throws Exception {
		ActionContext actionContext = ActionContext.getContext();
		Map sessionMap = actionContext.getSession();
		MgAdmin userSession =  (MgAdmin) sessionMap.get("mgadmin");
		if (mgAdmin.getSupervisor() == null || mgAdmin.getSupervisor().equals("")) mgAdmin.setSupervisor("N");
    	int result = adminService.saveAdmin(mgAdmin, append, userSession.getAdminId());
    	if (result == -1) {
    		addActionMessage("admin user is already exist");
    	} else {
    		addActionMessage("admin user append success");
    	}

    	List<MgAdminFunctions> fs = new ArrayList<MgAdminFunctions>();
    	List<String> editFunctionList = null;
    	List<String> trimmedEditFunctionList = null;
    	if (editFunctions != null) {
    		editFunctionList = Arrays.asList(editFunctions);
    		trimmedEditFunctionList = new ArrayList<String>();
    		if (editFunctionList != null) {
    			for (String efn : editFunctionList) {
    				String trimEfn = efn.split("#")[0];
    				trimmedEditFunctionList.add(trimEfn);
    			}
    		}
    	}
    	
    	if (functions != null){
    		for (String function: functions){
    			boolean isAllowEdit = true;
    			String name = function.split("#")[0];
    			int id = Integer.parseInt(function.split("#")[1]);
    			if (trimmedEditFunctionList != null) {
    				if (!trimmedEditFunctionList.contains(name)) {
    					isAllowEdit = false;
    				}
    			}
    			MgAdminFunctions mgFunc = new MgAdminFunctions();
    			mgFunc.setAdminId(mgAdmin.getAdminId());
    			mgFunc.setFunctionName(name);
    			if (id > 0){
    				mgFunc.setId(id);
    			}
    			if (isAllowEdit) {
    				mgFunc.setMode(AdminFunctionsService.MODE_EDITABLE);
    			} else {
    				mgFunc.setMode(AdminFunctionsService.MODE_READONLY);
    			}
    			mgFunc.setCreateDate(new Date());
    			fs.add(mgFunc);
    		}
    	}
    	adminFunctionsService.saveAdminFunctions(mgAdmin.getAdminId(), fs);
    	if (mgAdmin.getAdminId().equals(userSession.getAdminId())){
			setLoginUser(mgAdmin.getAdminId());
    	}
    	return "showMessages";
    }
	
	/**
	 * delete one admin data
	 * @return
	 * @throws Exception
	 */
	public String deleteAdmin() throws Exception {
		
    	ActionContext actionContext = ActionContext.getContext();
		Map sessionMap = actionContext.getSession();
    	MgAdmin userSession =  (MgAdmin) sessionMap.get("mgadmin");
    	//String adminId = userSession.getAdminId();
    	
    	adminService.deleteAdmin(adminId, userSession.getAdminId());
    	
    	return "success";
    }
	
	/**
	 * query all admin user list
	 * @return
	 * @throws Exception
	 */
	public String queryAdmins() throws Exception {
		try {
			ActionContext actionContext = ActionContext.getContext();
			Map<String, Object> sessionMap = actionContext.getSession();
			MgAdmin userSession =  (MgAdmin) sessionMap.get("mgadmin");
			if (userSession != null) {
				PAGE_NAME = "Administrator List";
				allowUserEdit = adminFunctionsService.isAdminAllowEdit(userSession.getAdminId(), PAGE_NAME);
			}
		} catch (Exception e) {
		}
		
		adminList = adminService.queryAdminList();
		return "Admin.List";
	}
	
	/**
	 * check login user id / password
	 * @return
	 * @throws Exception
	 */
	public String checkAdmin() throws Exception {
		if (adminService.checkAdmin(adminId, password) == 1) {
			MgAdmin loginUser = setLoginUser(adminId);
			setRedirectURL(MenuUtil.fetchFirstAction(loginUser));
			
			if (Constants.DEBUG_URL == null || !Constants.DEBUG_URL.equals("1")) {
				try {
					if (redirectURL != null && redirectURL.startsWith("../")) {
						String urlScheme = (Constants.USE_SSL != null && Constants.USE_SSL.equals("1")) ? "https:" : "http:";
						String httpPrefix = urlScheme + Constants.WEB_URL_NO_SCHEME;
						logger.info("AdminAction():checkAdmin(): httpPrefix=" + httpPrefix);
						if (!httpPrefix.endsWith("/")) {
							httpPrefix += "/";
						}
						redirectURL = redirectURL.replaceFirst("../", httpPrefix);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.info("AdminAction():checkAdmin(): redirectURL=" + redirectURL);
			return "success";
		} else {
			addActionError("Login failed, please try again!");
			return "input";
		}
	}
	@SuppressWarnings("unchecked")
	private MgAdmin setLoginUser(String adminId) throws Exception{
		MgAdmin loginUser = adminService.getAdmin(adminId);
		loginUser.setFunctions(getAdminFunctions(loginUser));	
		loginUser.setEditFunctions(getEditableAdminFunctions(loginUser));
		ActionContext actionContext = ActionContext.getContext();
		Map session = actionContext.getSession();
	    session.put("mgadmin", loginUser);
	    return loginUser;
	}
	private List<MgAdminFunctions> getAdminFunctions(MgAdmin admin) throws Exception{
		List<MgAdminFunctions> fs = adminFunctionsService.queryAdminFunctions(admin.getAdminId());
		if (fs == null) fs = new ArrayList<MgAdminFunctions>();
		if (admin.getSupervisor().equalsIgnoreCase("Y")) fs.add(new MgAdminFunctions(adminId, "Administrator List"));
		int indexGroupFunc = fs.indexOf(new MgAdminFunctions(adminId, "Group List"));
		if (Constants.ONLY_ONE_GROUP != null && Constants.ONLY_ONE_GROUP.equals("1") && indexGroupFunc != -1) {
			fs.remove(fs.get(indexGroupFunc));
		}
		return fs;
	}
	private List<MgAdminFunctions> getEditableAdminFunctions(MgAdmin admin) throws Exception{
		List<MgAdminFunctions> editFs = adminFunctionsService.queryAdminEditableFunctions(admin.getAdminId());
		if (editFs == null) editFs = new ArrayList<MgAdminFunctions>();
		if (admin.getSupervisor().equalsIgnoreCase("Y")) editFs.add(new MgAdminFunctions(adminId, "Administrator List"));
		int indexGroupFunc = editFs.indexOf(new MgAdminFunctions(adminId, "Group List"));
		if (Constants.ONLY_ONE_GROUP != null && Constants.ONLY_ONE_GROUP.equals("1") && indexGroupFunc != -1) {
			editFs.remove(editFs.get(indexGroupFunc));
		}
		return editFs;
	}
	
	/**
	 * user logout
	 * @return
	 */
	public String logout() {
		ActionContext actionContext = ActionContext.getContext();
		Map session = actionContext.getSession();
		session.remove("mgadmin");
		return "Login.Page";
	}
	
	public String getPassword() {
		return password;
	}
 
	public void setPassword(String password) {
		this.password = password;
	}
 
	public String getAdminId() {
		return adminId;
	}
 
	public void setAdminId(String adminId) {
		this.adminId = adminId;
	}

	public MgAdmin getMgAdmin() {
		return mgAdmin;
	}

	public void setMgAdmin(MgAdmin mgAdmin) {
		this.mgAdmin = mgAdmin;
	}

	public List getAdminList() {
		return adminList;
	}
	
	public void setAdminList(List adminList) {
		this.adminList = adminList;
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	public String getIsAdmin() {
		return isAdmin;
	}

	public void setIsAdmin(String isAdmin) {
		this.isAdmin = isAdmin;
	}

	public String[] getFunctions() {
		return functions;
	}

	public void setFunctions(String[] functions) {
		this.functions = functions;
	}

	public String[] getEditFunctions() {
		return editFunctions;
	}

	public void setEditFunctions(String[] editFunctions) {
		this.editFunctions = editFunctions;
	}

	public String getRedirectURL() {
		return redirectURL;
	}

	public void setRedirectURL(String redirectURL) {
		this.redirectURL = redirectURL;
	}

	public boolean getAllowUserEdit() {
		return allowUserEdit;
	}
	
}
