

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package studentmodel;

public class Registration 
{
    private final Student student;
    private final Course course;
    
    public Registration(final Student student, final Course course) 
    {
        this.student = student;
        this.course = course;
    }

    public Course getCourse() 
    {
        return course;
    }

    public Student getStudent() 
    {
        return student;
    }
    
    public String toString() 
    {
        final StringBuilder builder = new StringBuilder();
        builder.append(student.toString())
                .append(" is in ")
                .append(course.toString());
        return builder.toString();
    }

   
}
