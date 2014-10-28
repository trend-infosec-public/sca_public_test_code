

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package studentmodel;

import java.util.HashSet;
import java.util.Set;

public class Course 
{
    private int id;
    private String name;
    private int limit;
    private Teacher teacher;
    private Set<Course> prereqs = new HashSet<Course>();
    private String courseNumber;
    private boolean offered = Boolean.TRUE;
    private int totalEnrolled;
    public Course(){
    	
    }
    
    public Course(final int id, final String name, final int limit, final String courseNumber) 
    {
        this.id = id;
        this.name = name;
        this.limit = limit;
        this.courseNumber = courseNumber;
    }

    public Course(final String name, final int limit, final String courseNumber) {
        this.name = name;
        this.limit = limit;
        this.courseNumber = courseNumber;
    }

    public int getId() 
    {
        return id;
    }

    public void setId(int id) 
    {
        this.id = id;
    }

    public int getLimit() 
    {
        return limit;
    }

    public void setLimit(int limit)
    {
        this.limit = limit;
    }

    public String getName() 
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Teacher getTeacher() 
    {
        return teacher;
    }

    public void setTeacher(Teacher teacher)
    {
        this.teacher = teacher;
    }

    public Set<Course> getPrereqs() 
    {
        return prereqs;
    }

    public void setPrereqs(Set<Course> prereqs) 
    {
        this.prereqs = prereqs;
    }

    public String getCourseNumber() 
    {
        return courseNumber;
    }

    public void setCourseNumber(String courseNumber) 
    {
        this.courseNumber = courseNumber;
    }

    public boolean isOffered() 
    {
        return offered;
    }

    public void setOffered(boolean offered) 
    {
        this.offered = offered;
    }

    public int getTotalEnrolled() 
    {
        return totalEnrolled;
    }

    public void setTotalEnrolled(int totalEnrolled) 
    {
        this.totalEnrolled = totalEnrolled;
    }

    public boolean isFull() 
    {
        return getTotalEnrolled() >= getLimit();
    }
    @Override
    public String toString() 
    {
        final StringBuilder builder = new StringBuilder();
        builder.append("cID:"+this.getId());
        return builder.toString();
    }    
    
	public void printCourses(final Set<Course> list) 
    {
		boolean listExists = false;
        final StringBuilder builder = new StringBuilder();
        for (final Course course : list) 
        {     	
            if (course.isOffered()) 
            {
                builder.append(course.toString());
                
                final Teacher teacher = course.getTeacher();
                if(teacher != null) 
                {
                    builder.append(" "+teacher.toString()+"\n");
                }
               /* 
                builder.append(", Seats Open ")
                        .append(course.getLimit() - course.getTotalEnrolled())
                        .append(" - prerequisites: ");
                
                for (final Course prereq : course.getPrereqs()) 
                {
                    builder.append(prereq.getId());
                }
                
                builder.append("\n");
                */
            }
            listExists = true;
        }
        if (listExists){
        	System.out.println("Courses:\n"+builder.toString());
        }
 }    
}
