package com.objectcode.GanttProjectAPI;

 /**
   * Handles GanttDiagram-Resources (Users)
   *    
   * license: LGPL v3
   * 
   * @author heyduk
   */
  public class Resource {
    String fId;           // internal unique ID of Resource in GanttProject
    String fName;         // Shortname of User (e.g. JPA, ...)
    private String fFunction;     // function / Rolename (predefined from GanttProject or userdefined in diagram per roles-tag! 
    private String fFunctionCode; // function-code
    private String fcontacts;
    private String fPhone;
    
    public Resource(String anId, String aName, String aFunction, String aFunctionCode, String aPhoneNr) {
      fId = anId;
      fName = aName;
      fFunction = aFunction;
      fFunctionCode = aFunctionCode;
      fPhone = aPhoneNr;
    }
  
    public String toString() {
      return "Resource[Id="+fId+", Name="+fName+", Function="+fFunction+", FunctionCode="+fFunctionCode+", Phone="+fPhone+"]";
    }
  
    public String getFunction() {
      return fFunction;
    }

    public void setFunction(String function) {
      fFunction = function;
    }

    public String getId() {
      return fId;
    }
  
    public void setId(String id) {
      fId = id;
    }
  
    public String getName() {
      return fName;
    }
  
    public void setName(String name) {
      fName = name;
    }

    public String getFunctionCode() {
      return fFunctionCode;
    }

    public void setFunctionCode(String functionCode) {
      fFunctionCode = functionCode;
    }

	public void setPhone(String Phone) {
		fPhone = Phone;
	}

	public String getPhone() {
		return fPhone;
	}

	public String getcontacts() {
		return fcontacts;
	}

	public void setcontacts(String contacts) {
		fcontacts = contacts;
	}
  }