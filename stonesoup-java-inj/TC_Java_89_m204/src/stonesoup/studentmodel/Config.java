

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup.studentmodel;
import java.util.Set;

/**
 * Holds the configuration information for the application.  
 * This comes from the command line arguments. 
 * Note:  Not all fields will be used at any one time. 
 */

//SQL INJECTION - good place for INPUTVALIDATION / WHITE & BLACK LISTS
public class Config {

    //for most actions
    private String studentId;
    private String courseNumber;
    private int teacherId;
    private String command;
    private String className;
    private String instructions;
    private String password;

    //for student/teacher actions
    private String firstName;
    private String lastName;

    //for course actions
    private int sizeLimit;
    private Set<Integer> prereqIds;
    
    
    public String getInstructions()
    {
    	return instructions;
    }
    
    public void setInstructions(String instructions)
    {
    	this.instructions = instructions;
    }
    public String getPassword()
    {
    	return password;
    }
    
    public void setPassword(String password)
    {
    	this.password = password;
    }
    
    public String getCourseName()
    {
    	return className;
    }
    
    public void setCourseName(String className)
    {
    	this.className = className;
    }

    public String getCourseNumber() 
    {
        return courseNumber;
    }

    public void setCourseNumber(String courseNumber) 
    {
        this.courseNumber= courseNumber;
    }

    public String getFirstName() 
    {
        return firstName;
    }

    public void setFirstName(String firstName) 
    {
        this.firstName = firstName;
    }

    public String getLastName() 
    {
        return lastName;
    }

    public void setLastName(String lastName) 
    {
        this.lastName = lastName;
    }

    public Set<Integer> getPrereqIds() 
    {
        return prereqIds;
    }

    public void setPrereqIds(Set<Integer> prereqIds)
    {
        this.prereqIds = prereqIds;
    }

    public int getSizeLimit() 
    {
        return sizeLimit;
    }

    public void setSizeLimit(int sizeLimit) 
    {
        this.sizeLimit = sizeLimit;
    }

    public String getStudentId()
    {
        return studentId;
    }

    public void setStudentId(String studentId) 
    {
        this.studentId = studentId;
    }

    public int getTeacherId() 
    {
        return teacherId;
    }

    public void setTeacherId(int teacherId) 
    {
        this.teacherId = teacherId;
    }

    public String getCommand() 
    {
        return command;
    }

    public void setCommand(String command) 
    {
        this.command = command;
    }

	@Override
	public String toString() {
		return "Config [studentId=" + studentId + ", courseNumber="
				+ courseNumber + ", teacherId=" + teacherId + ", command="
				+ command + ", className=" + className + ", instructions="
				+ instructions + ", password=" + password + ", firstName="
				+ firstName + ", lastName=" + lastName + ", sizeLimit="
				+ sizeLimit + ", prereqIds=" + prereqIds + "]";
	}
    
    
}
