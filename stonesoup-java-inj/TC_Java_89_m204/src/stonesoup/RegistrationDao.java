

/********************************************************************
 ** This software (or technical data) was produced for the U. S.
 ** Government under contract 2009-0917826-016 and is subject to 
 ** the Rights in Data-General Clause 52.227-14. Alt. IV (DEC 2007).
 ** 
 ** (c) Copyright 2012 The MITRE Corporation. All Rights Reserved.
 ********************************************************************/

package stonesoup;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import stonesoup.studentmodel.Config;
import stonesoup.studentmodel.Course;
import stonesoup.studentmodel.DataConnection;
import stonesoup.studentmodel.Registration;
import stonesoup.studentmodel.Student;
import stonesoup.studentmodel.Teacher;

// A simple DAO for accessing the data for the registration application.
public class RegistrationDao {

	// Constructor
	public RegistrationDao() {
	}


    /**
	 * Adds a new Registration to the db. This expects both a Student and a
	 * Course object associated with the Registration or it will thrown an
	 * IllegalArgumentException as an unchecked error.
     * The registration will only be allowed if the student and the course both
     * exist and there is no enrollment conflict.
     *
     * @param registration The registration
     * @throws SQLException on error
	 */
	public void addRegistration(final Config config) throws SQLException {
        final Student regstudent = getStudent(config.getStudentId());
        final Course intocourse = getCourseByCourseNumber(config.getCourseNumber());
        final Registration reg = new Registration(regstudent, intocourse);

		// first confirm that it is OK to enroll
		final Student confirmStudent = this.getStudent(regstudent.getId());
		if (confirmStudent == null) {
			throw new IllegalArgumentException("Student doesn't exist");
		}

		final Course confirmCourse = this.getCourse(intocourse.getId());
		if (confirmCourse == null) {
			throw new IllegalArgumentException("Course doesn't exist");
		}

		if (confirmCourse.isFull()) {
			throw new IllegalStateException("Course is full");
		}
		Connection conn = null;
		try {
			conn = new DataConnection().initialize();
			PreparedStatement stmt = null;
			try {
				stmt = conn.prepareStatement("INSERT INTO registration (student_id, course_id) VALUES (?, ?)");
				stmt.setInt(1, Integer.parseInt(reg.getStudent().getId()));
				stmt.setInt(2, reg.getCourse().getId());
				stmt.executeUpdate();
			} finally {
				closeStatement(stmt);
			}
		} finally {
			conn.close();
		}
		regstudent.toString();
	}

    /**
     * Lists the Courses student is registered for.
     *
     * @param config with student, password set
     * @return the list of courses
     * @throws SQLException on error
     */
	public Set<Course> getRegistrationsForStudent(final Config config) throws SQLException {

		final Student studentreg = getStudent(config.getStudentId());
		final Set<Course> courses = new HashSet<Course>();

		Connection conn = null;
		PreparedStatement stmt = null, stmt2 = null, stmt3 = null;
		ResultSet rs = null, rs2 = null, rs3 = null;

		try {
			// faulty password check
			if (!passwordCheck(studentreg, config.getPassword())) {
				System.out.println("Incorrect Password");
			} else {

		    conn = new DataConnection().initialize();
			stmt = conn
					.prepareStatement("SELECT * FROM course c "
							+ "JOIN registration r on "
							+ "r.course_id = c.id WHERE r.student_id = ?");
			stmt.setInt(1, Integer.parseInt(studentreg.getId().substring(0,1)));
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int id = rs.getInt(1);
				final String name = rs.getString(2);
				final int limit = rs.getInt(3);
				final String courseNumber = rs.getString(4);
				final boolean isOffered = rs.getBoolean(5);
				final int tid = rs.getInt(6);


				String fname = null;
				String lname = null;
				stmt3=conn.prepareStatement("Select firstname, lastname from teacher where id = ?");
				stmt3.setInt(1, tid);
				rs3 = stmt3.executeQuery();
				while (rs3.next()) {
					fname = rs3.getString(1);
					lname = rs3.getString(2);
				}

				final Course course = new Course(id, name, limit, courseNumber);
				course.setOffered(isOffered);
				course.setTeacher(new Teacher(tid,fname,lname));

				// Need to calculate the total number of students
				// enrolled in the course as well. Probably could do this
				// as part of the first query, but this works.
				stmt2 = conn
						.prepareStatement("SELECT count(*) FROM registration WHERE course_id = ?");
				stmt2.setInt(1, id);
				rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					final int totalEnrolled = rs2.getInt(1);
					course.setTotalEnrolled(totalEnrolled);
				}
				courses.add(course);
			}
			}
		} finally {
			closeResultSet(rs);
			closeResultSet(rs2);
			closeStatement(stmt);
			closeStatement(stmt2);
			conn.close();
		}
		return courses;
	}

	/**
	 * Deletes the registration
	 * @param config
	 * @throws SQLException on error
	 */
	public void deleteRegistration(final Config config)	throws SQLException {
    	final Student unregstudent = getStudent(config.getStudentId());
    	final Course outofcourse = getCourseByCourseNumber(config.getCourseNumber());

		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = new DataConnection().initialize();
			if (!passwordCheck(unregstudent, config.getPassword())) {
				System.out.println("Incorrect Password");
			} else {
				stmt = conn.prepareStatement("DELETE FROM registration WHERE "
						+"student_id = ? AND course_id = ?");
				stmt.setInt(1, Integer.parseInt(unregstudent.getId()));
				stmt.setInt(2, outofcourse.getId());
				stmt.executeUpdate();
		        System.out.println(unregstudent.toString()+" has been deleted from "+outofcourse.toString());
			}
		} finally {
			closeStatement(stmt);
			conn.close();
		}
	}

    /**
     * Gets a student by id.
     * Throws unchecked error when invalid student ID is used.
     *
     * @param id The id to search for
     * @return The student
     * @throws SQLException on error
     */
	private Student getStudent(final String studentID) throws SQLException {
		Student s = null;
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = new DataConnection().initialize();
			stmt = conn.prepareStatement("SELECT * FROM student WHERE id = ?");
			stmt.setInt(1, Integer.parseInt(studentID.substring(0,1)));
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String first = rs.getString("firstname");
				final String last = rs.getString("lastname");
				s =  new Student(studentID, first, last, "");
			}
		} finally {
			closeResultSet(rs);
			closeStatement(stmt);
			conn.close();
		}
		return s;
	}

	/**
     * Gets a Course by id.
     * Throws an unchecked error when an invalid course ID is used.
     *
     * @param id The id to search for
     * @return The course
     * @throws SQLException on error
     */
	private Course getCourse(final int courseid) throws SQLException {
		Course course = null;

		Connection conn = null;
		PreparedStatement stmt = null, stmt2 = null;
		ResultSet rs = null, rs2 = null;
		try {
			conn = new DataConnection().initialize();
			stmt = conn.prepareStatement("SELECT * FROM course WHERE id = ?");
			stmt.setInt(1, courseid);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final String name = rs.getString("name");
				final int limit = rs.getInt("size_limit");
				final String courseNumber = rs.getString("course_number");
				final boolean isOffered = rs.getBoolean("is_offered");
				course = new Course(courseid, name, limit, courseNumber);
				course.setOffered(isOffered);

				// Need to calculate the total number of students
				// enrolled in the course as well. Probably could do this
				// as part of the first query, but this works.
				stmt2 = conn
						.prepareStatement("SELECT count(*) FROM registration WHERE course_id = ?");
				stmt2.setInt(1, courseid);
				rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					final int totalEnrolled = rs2.getInt(1);
					course.setTotalEnrolled(totalEnrolled);
				}
			}
		} finally {
			closeResultSet(rs);
			closeResultSet(rs2);
			closeStatement(stmt);
			closeStatement(stmt2);
			conn.close();
		}

		return course;
	}

	/**
	 * Gets a Course by course NAME.
	 * Throws an unchecked error when an invalid
	 * course number is used.
	 *
	 * @param string courseNumber The courseNumber to search for
	 * @return The course
	 * @throws SQLException  on error
	 */
	private Course getCourseByCourseNumber(final String courseNumber) throws SQLException {
		Course course = null;
		Connection conn = null;
		PreparedStatement stmt = null, stmt2 = null;
		ResultSet rs = null, rs2 = null;
		try {
			conn = new DataConnection().initialize();
			stmt = conn.prepareStatement("SELECT * FROM course WHERE course_number = ?");
			stmt.setString(1, courseNumber);
			rs = stmt.executeQuery();
			while (rs.next()) {
				final int id = rs.getInt("id");
				final String name = rs.getString("name");
				final int limit = rs.getInt("size_limit");
				final boolean isOffered = rs.getBoolean("is_offered");
				course = new Course(id, name, limit, courseNumber);
				course.setOffered(isOffered);

				// Need to calculate the total number of students
				// enrolled in the course as well. Probably could do this
				// as part of the first query, but this works.
				stmt2 = conn.prepareStatement("SELECT count(*) FROM registration WHERE course_id = ?");
				stmt2.setInt(1, id);
				rs2 = stmt2.executeQuery();
				while (rs2.next()) {
					final int totalEnrolled = rs2.getInt(1);
					course.setTotalEnrolled(totalEnrolled);
				}
			}
		} finally {
			closeResultSet(rs);
			closeResultSet(rs2);
			closeStatement(stmt);
			closeStatement(stmt2);
			conn.close();
		}

		return course;
	}

	// Vulnerable password check
	private boolean passwordCheck(Student unregstudent, String password) throws SQLException {
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = new DataConnection().initialize();
			//expoit- passing concatd str to incorrectly parameterized query
			stmt = conn.prepareStatement("SELECT * FROM student WHERE id=" + unregstudent.getId() + " AND password='" + password +"'");	//STONESOUP:TRIGGER_POINT
			//System.out.println("stmt-"+stmt.toString());
			rs = stmt.executeQuery();

			if (rs.next()) {
				return true;
			} else {
				return false;
			}
		} catch (SQLException e) {
			System.err.println("sql error");
		} finally {
			closeStatement(stmt);
			conn.close();
		}
		return false;
	}

	// List a students registrations
	public void printRegistrations(final Set<Course> list)
    {
    	boolean listExists = false;
        final StringBuilder builder = new StringBuilder();
        for (final Course course : list)
        {
            builder.append("CourseID:" + course.getId() + " ");
		    final Teacher teacher = course.getTeacher();
	        builder.append(teacher.toString() + "\n");
		    listExists = true;
        }
        if (listExists){
        	System.out.println("Student Enrolled in - " + builder.toString());
        }
    }

	/**
	 * Closes the prepared statement
	 * @param stmt The prepared statement to close
	 */
	private void closeStatement(PreparedStatement stmt) throws SQLException {
		if (stmt != null) {
			try {
				stmt.close();
			} catch (SQLException e) {
				System.err.println("Failure closing Statement");
			}
		}
	}

	/**
	 * Closes the result set
	 * @param rs The result set to close
	 */
	private void closeResultSet(ResultSet rs) throws SQLException {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				System.err.println("Failure closing ResultSet");
			}
		}
	}


}
