

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup.studentmodel;

public class Student 
{
    private int id;
    private String firstName;
    private String lastName;
    private String password;
    
    public Student (final int id, final String firstName, final String lastName, final String password)
    {
    	this.id = id;
    	this.firstName = firstName;
    	this.lastName = lastName;
    	this.password = password;
    }
    
    public Student (final String firstName, final String lastName, final String password)
    {
    	this.firstName = firstName;
    	this.lastName = lastName;
    	this.password = password;
    }
    
    public Student(final int id, final String firstName, final String lastName) 
    {
    	this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() 
    {
        return firstName;
    }

    public void setFirstName(String firstName) 
    {
        this.firstName = firstName;
    }

    public int getId() 
    {
        return id;
    }

    public void setId(int id) 
    {
        this.id = id;
    }

    public String getLastName() 
    {
        return lastName;
    }

    public void setLastName(String lastName) 
    {
        this.lastName = lastName;
    }
    
    public String getPassword()
    {
    	return password;
    }

    @Override
    public String toString() 
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("StudentID:"+this.getId());
       // .append(", ")
       // .append(this.getLastName())
       // .append(", ")
       // .append(this.getFirstName());
        return builder.toString();
    }
    
}
